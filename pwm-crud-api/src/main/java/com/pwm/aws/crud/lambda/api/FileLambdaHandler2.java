package com.pwm.aws.crud.lambda.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.pwm.aws.crud.lambda.api.model.File;

public class FileLambdaHandler2 implements RequestStreamHandler {

	private String DYNAMO_TABLE = "team4-files";

	@SuppressWarnings("unchecked")
	@Override
	public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {

		OutputStreamWriter writer = new OutputStreamWriter(output);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		JSONParser parser = new JSONParser(); // this will help us parse the request object
		JSONObject responseObject = new JSONObject(); // we will add to this object for our api response
		JSONObject responseBody = new JSONObject();// we will add the item to this object

		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);

		int id;
		Item resItem = null;

		try {
			JSONObject reqObject = (JSONObject) parser.parse(reader);
			// pathParameters
			if (reqObject.get("pathParameters") != null) {
				JSONObject pps = (JSONObject) reqObject.get("pathParameters");
				if (pps.get("id") != null) {
					id = Integer.parseInt((String) pps.get("id"));
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id", id);

				}
			}

			if (resItem != null) {
				File file = new File(resItem.toJSON());
				responseBody.put("file", file);
				responseObject.put("statusCode", 200);
			} else {
				responseBody.put("message", "No Items Found");
				responseObject.put("statusCode", 404);
			}

			responseObject.put("body", responseBody.toString());

		} catch (Exception e) {
			context.getLogger().log("ERROR : " + e.getMessage());
		}
		writer.write(responseObject.toString());
		// writer.write(responseObject.toString().replaceAll("\\\\",""));
		reader.close();
		writer.close();

	}

	@SuppressWarnings("unchecked")
	public void handleAllFileRequest(InputStream input, OutputStream output, Context context) throws IOException {

		OutputStreamWriter writer = new OutputStreamWriter(output);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		JSONParser parser = new JSONParser(); // this will help us parse the request object
		JSONObject responseObject = new JSONObject(); // we will add to this object for our api response
		JSONObject responseBody = new JSONObject();// we will add the item to this object

		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);

		Item resItem = null;

		try {

			ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMO_TABLE);

			ScanResult result = client.scan(scanRequest);
			
			int count = result.getCount();
			
			System.out.println(count);
			
			List<File> fileList = new ArrayList<>();
			
			for(int i = 1; i <= count; i++) {
				resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id", i);
				File file = new File(resItem.toJSON());
				fileList.add(file);
			}
						
			System.out.println(fileList.toString());
			
			if (count == 0) {
				responseBody.put("message", "No Items Found");
				responseObject.put("statusCode", 200);
			} else {
				responseBody.put("fileList", fileList);
				responseObject.put("statusCode", 200);
			}

			responseObject.put("body", responseBody);

		} catch (Exception e) {
			context.getLogger().log("ERROR : " + e.getMessage());
		}
		writer.write(responseObject.toString());
		reader.close();
		writer.close();

	}

}
