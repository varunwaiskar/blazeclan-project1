package com.pwm.aws.crud.lambda.api;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class HttpProxyHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
    private static final Logger logger = LogManager.getLogger(HttpProxyHandler.class);

    private static final String[] headersBlacklist = {"CloudFront-", "X-Amzn-", "X-Amz-", "X-Forwarded-", "Host", "Via", "origin", "Referer"};
    private String ingoingWhitelist;
    private List<String> outgoingWhitelist;

    public HttpProxyHandler() {
        Properties props = new Properties();
        try {
            props.load(HttpProxyHandler.class.getClassLoader().getResourceAsStream("whitelist.properties"));
            ingoingWhitelist = props.getProperty("whitelist.ingoing");
            outgoingWhitelist = Arrays.asList(props.getProperty("whitelist.outgoing").split(","));
        } catch (IOException e) {
            logger.error("Error parsing properties file", e);
        }
    }

    public ApiGatewayResponse handleRequest(Map<String, Object> request, Context context) {
        try {
        /*
        request.forEach((k, v) -> {
            if (v != null)
                logger.info(k + " = " + v.toString() + " -- " + v.getClass());
            else logger.info(k + " = null");
        });
        */

            Map<String, String> rawHeaders = (Map<String, String>) request.get("headers");
            String method = (String) request.get("httpMethod");
            String body = (String) request.get("body");
            String url = ((Map<String, String>) request.get("pathParameters")).get("url");
            Map<String, String> parameters = (Map<String, String>) request.get("queryStringParameters");

            logger.debug("Request method = " + method);
            logger.debug("Request raw headers = " + rawHeaders);
            logger.debug("Request path = " + url);
            logger.debug("Request parameters = " + parameters);
            logger.debug("Request body = " + body);

            if (outgoingWhitelist.stream().noneMatch(url::contains)) {
                return error(403, "Domain not in whitelist", null);
            }

            Map<String, String> cleanHeaders = rawHeaders.entrySet()
                    .stream()
                    .filter(p -> isHeaderNotInBlacklist(p.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            logger.debug("Cleaned headers = " + cleanHeaders);

            URI uri;
            try {
                uri = constructURI(url, parameters);
                logger.debug("uri = " + uri.toString());
            } catch (URISyntaxException e) {
                return error(400, "Error constructing URI", e);
            }

            HttpClient client = HttpClientBuilder.create().build();

            HttpRequestBase requestBase;
            try {
                requestBase = constructRequest(uri, cleanHeaders, method, body);
            } catch (UnsupportedEncodingException e) {
                return error(500, "Error constructing request", e);
            } catch (MethodNotSupportedException e) {
                return error(405, "Method Not Allowed", null);
            }

            HttpResponse response;
            try {
                response = client.execute(requestBase);
            } catch (Exception e) {
                return error(500, "Error executing request", e);
            }

            String responseBody;
            try {
                responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            } catch (IOException e) {
                return error(500, "Error while parsing the response", e);
            }
            logger.info("responsebody = ", responseBody);

            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Content-Type", response.getEntity().getContentType().getValue());
            responseHeaders.put("Access-Control-Allow-Origin", ingoingWhitelist);
            responseHeaders.put("Vary", "Origin");

            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setRawBody(responseBody)
                    .setHeaders(responseHeaders)
                    .build();
        } catch (Exception e) {
            return error(500, "Fatal error occurred", e);
        }
    }

    private URI constructURI(String path, Map<String, String> params) throws URISyntaxException {
        String url = path;
        if (params != null) {
            url += "?" + params.entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));
        }
        return new URI(url);
    }

    private HttpRequestBase constructRequest(URI uri, Map<String, String> headers, String method, String body)
            throws UnsupportedEncodingException, MethodNotSupportedException {
        HttpRequestBase requestBase;
        switch (method) {
            case "GET":
                requestBase = new HttpGet(uri);
                break;
            case "POST":
                requestBase = new HttpPost(uri);
                if (body != null)
                    ((HttpPost)requestBase).setEntity(new StringEntity(body));
                break;
            default:
                throw new MethodNotSupportedException("Method " + method + " not supported");
        }

        headers.forEach(requestBase::addHeader);

        return requestBase;
    }

    private boolean isHeaderNotInBlacklist(String input) {
        return Arrays.stream(headersBlacklist).parallel().noneMatch(input::contains);
    }

    private ApiGatewayResponse error(int code, String message, Exception e) {
        logger.error(message, e);

        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "text/plain");

        return ApiGatewayResponse.builder()
                .setStatusCode(code)
                .setHeaders(responseHeaders)
                .setRawBody(message)
                .build();
    }
}