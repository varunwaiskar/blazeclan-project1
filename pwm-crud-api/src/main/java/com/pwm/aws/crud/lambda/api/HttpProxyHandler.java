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
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.google.gson.Gson;
import com.pwm.aws.crud.lambda.api.model.File;

public class HttpProxyHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
	
	//Dynamo table name
	private String DYNAMO_TABLE = "team4-files";

	//Method handling Lambda Request
	public ApiGatewayResponse handleRequest(Map<String, Object> request, Context context) {
		try {

			//Initialization for future use
			boolean flag = false;
			Item resItem = null;
			Gson gson = new Gson();
			String responseToken = null ;	//response token default set as invalid 
			List<File> fileList = new ArrayList<>();
			Map<String, String> responseHeaders = new HashMap<>();

			try {


				// Fetching Headers
				Map<String, String> rawHeaders = (Map<String, String>) request.get("headers");
				String jwtToken = rawHeaders.get("Authorization");

				//Authorizing verification
				isJWTAuthorized(jwtToken);
				
				//If verified then set responseToken
				responseToken = jwtToken;
				
				//Fetching parameters
				Map<String, String> parameters = (Map<String, String>) request.get("queryStringParameters");
				String searchItem = "";

				if (parameters != null) {
					searchItem = parameters.get("search");
				}

				//DynamoCLient
				AmazonDynamoDB dynamoClient = AmazonDynamoDBClientBuilder.defaultClient();
				DynamoDB dynamoDB = new DynamoDB(dynamoClient);

				ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMO_TABLE);
				ScanResult result = dynamoClient.scan(scanRequest);

				int count = result.getCount();

				for (int i = 1; i <= count; i++) {
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id", i);
					File file = new File(resItem.toJSON());

					if (parameters != null) {
						if (file.getName().contains(searchItem))
							fileList.add(file);
					} else
						fileList.add(file);
				}

			} 
			catch(InvalidClaimException e) {
				flag = true;
				responseHeaders.put("Authorization", "InvalidClaimException");
			}
			catch(AlgorithmMismatchException e) {
				flag = true;
				responseHeaders.put("Authorization", "AlgorithmMismatchException");
			}
			catch(TokenExpiredException e) {
				flag = true;
				responseHeaders.put("Authorization", "Token Expired");
			}
			catch(SignatureVerificationException e) {
				flag = true;
				responseHeaders.put("Authorization", "Signature is invalid");
			}
			catch(JWTVerificationException e) {
				flag = true;
				responseHeaders.put("Authorization", "JWTVerificationException");
			}
			catch(RuntimeException e) {
				flag = true;
				responseHeaders.put("Authorization", e.getMessage());
				context.getLogger().log("ERROR : " + e.getMessage());
			}
			catch (Exception e) {
				context.getLogger().log("ERROR : " + e.getMessage());
			}

			String fileStr = gson.toJson(fileList);

			//Setting Response Headers
			
			responseHeaders.put("Content-Type", "application/json");
			responseHeaders.put("Access-Control-Allow-Origin", "*");
			responseHeaders.put("Vary", "Origin");

			if(flag) {
				return ApiGatewayResponse.builder().setStatusCode(401).setRawBody(fileStr).setHeaders(responseHeaders)
						.build();
			}
			else {
				responseHeaders.put("Authorization", responseToken);
				return ApiGatewayResponse.builder().setStatusCode(200).setRawBody(fileStr).setHeaders(responseHeaders)
					.build();
			}
		} 
		catch (Exception e) {
			return error(500, "Fatal error occurred", e);
		}
	}

	
	//JWT Token Verification
	public void isJWTAuthorized(String token) {

		token = token.replace("Bearer ", "");

		String aws_cognito_region = "us-east-1"; // Replace this with your aws cognito region
		String aws_user_pools_id = "us-east-1_FdSehbTlZ"; // Replace this with your aws user pools id
		RSAKeyProvider keyProvider = new AwsCognitoRSAKeyProvider(aws_cognito_region, aws_user_pools_id);
		Algorithm algorithm = Algorithm.RSA256(keyProvider);
		JWTVerifier jwtVerifier = JWT.require(algorithm)
				// .withAudience("2qm9sgg2kh21masuas88vjc9se") // Validate your apps audience if
				// needed
				.build();

		jwtVerifier.verify(token);

	}

	//ApiGatewayResponse Builder
	private ApiGatewayResponse error(int code, String message, Exception e) {

		Map<String, String> responseHeaders = new HashMap<>();
		responseHeaders.put("Content-Type", "text/plain");

		return ApiGatewayResponse.builder().setStatusCode(code).setHeaders(responseHeaders).setRawBody(message).build();
	}
}