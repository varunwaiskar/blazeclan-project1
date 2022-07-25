package com.pwm.aws.crud.lambda.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.pwm.aws.crud.lambda.api.model.File;

public class HttpProxyHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	public ApiGatewayResponse handleRequest(Map<String, Object> request, Context context) {
		try {

			List<File> fileList = new ArrayList<>();

			AmazonDynamoDB dynamoClient = AmazonDynamoDBClientBuilder.defaultClient();
			DynamoDB dynamoDB = new DynamoDB(dynamoClient);
			String DYNAMO_TABLE = "team4-files";

			Item resItem = null;

			try {
	            Map<String, String> parameters = (Map<String, String>) request.get("queryStringParameters");
	            String searchItem = "";
	            
				if(parameters != null) {
					searchItem = parameters.get("search");
				}

				System.out.println(searchItem);
				
				ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMO_TABLE);

				ScanResult result = dynamoClient.scan(scanRequest);

				int count = result.getCount();

				for (int i = 1; i <= count; i++) {
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id", i);
					File file = new File(resItem.toJSON());
					
					if(parameters != null) {
						if(file.getName().contains(searchItem))
							fileList.add(file);
					}
					else
						fileList.add(file);
				}

				// responseBody = fileList.toString();
			} catch (Exception e) {
				context.getLogger().log("ERROR : " + e.getMessage());
			}

			Gson gson = new Gson();
			String fileStr = gson.toJson(fileList);

			// responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");

			Map<String, String> responseHeaders = new HashMap<>();
			responseHeaders.put("Content-Type", "application/json");
			responseHeaders.put("Access-Control-Allow-Origin", "*");
			responseHeaders.put("Vary", "Origin");

			return ApiGatewayResponse.builder().setStatusCode(200).setRawBody(fileStr).setHeaders(responseHeaders)
					.build();
		} catch (Exception e) {
			return error(500, "Fatal error occurred", e);
		}
	}

	public ApiGatewayResponse handleSearchRequest(Map<String, Object> request, Context context) {
		try {
				
			System.out.println(request);
            Map<String, String> parameters = (Map<String, String>) request.get("queryStringParameters");
            String searchItem = parameters.get("search");
            
            
			List<File> fileList = new ArrayList<>();

			AmazonDynamoDB dynamoClient = AmazonDynamoDBClientBuilder.defaultClient();
			DynamoDB dynamoDB = new DynamoDB(dynamoClient);
			String DYNAMO_TABLE = "team4-files";

			Item resItem = null;

			try {

				ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMO_TABLE);

				ScanResult result = dynamoClient.scan(scanRequest);

				int count = result.getCount();

				for (int i = 1; i <= count; i++) {
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id", i);
					File file = new File(resItem.toJSON());
					
					if(file.getName().contains(searchItem))
						fileList.add(file);
				}

				// responseBody = fileList.toString();
			} catch (Exception e) {
				context.getLogger().log("ERROR : " + e.getMessage());
			}

			Gson gson = new Gson();
			String fileStr = gson.toJson(fileList);

			// responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");

			Map<String, String> responseHeaders = new HashMap<>();
			responseHeaders.put("Content-Type", "application/json");
			responseHeaders.put("Access-Control-Allow-Origin", "*");
			responseHeaders.put("Vary", "Origin");

			return ApiGatewayResponse.builder().setStatusCode(200).setRawBody(fileStr).setHeaders(responseHeaders)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			return error(500, "Fatal error occurred", e);
		}
	}

	private ApiGatewayResponse error(int code, String message, Exception e) {

		Map<String, String> responseHeaders = new HashMap<>();
		responseHeaders.put("Content-Type", "text/plain");

		return ApiGatewayResponse.builder().setStatusCode(code).setHeaders(responseHeaders).setRawBody(message).build();
	}
}