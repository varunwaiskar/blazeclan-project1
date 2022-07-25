package com.pwm.aws.crud.lambda.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.pwm.aws.crud.lambda.api.model.File;
import com.pwm.aws.crud.lambda.api.model.Product;

public class FileLambdaHandler implements RequestStreamHandler {

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
		List<ArrayList<String>> filesList = new ArrayList<ArrayList<String>>();

		try {
			JSONObject reqObject = (JSONObject) parser.parse(reader);
			// pathParameters
			if (reqObject.get("pathParameters") != null) {
				JSONObject pps = (JSONObject) reqObject.get("pathParameters");
				if (pps.get("id") != null) {
					id = Integer.parseInt((String) pps.get("id"));
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id", id);

				}

				if (resItem != null) {
					File file = new File(resItem.toJSON());
					responseBody.put("file", file);
					responseObject.put("statusCode", 200);
				}

				else {
					responseBody.put("message", "No Items Found");
					responseObject.put("statusCode", 404);
				}
			}

			else {

				ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMO_TABLE);

				ScanResult result = client.scan(scanRequest);
				List<Map<String, AttributeValue>> files = result.getItems();

				for (Map<String, AttributeValue> item : files) {

					Collection<AttributeValue> valList = item.values();

					ArrayList<String> tempList = new ArrayList<>();

					for (AttributeValue value : valList)
						tempList.add(value.toString());

					filesList.add(tempList);
				}

				if (filesList.isEmpty()) {
					responseBody.put("message", "No Items Found");
					responseObject.put("statusCode", 404);
				} else {
					responseBody.put("fileList", filesList);
					responseObject.put("statusCode", 200);
				}
			}

			
			responseObject.put("body", responseBody.toString());

		} catch (Exception e) {
			context.getLogger().log("ERROR : " + e.getMessage());
		}
		writer.write(responseObject.toString());
		reader.close();
		writer.close();

	}

	/*
	 * public void handleSearchRequest(InputStream input, OutputStream output,
	 * Context context) throws IOException {
	 * 
	 * OutputStreamWriter writer = new OutputStreamWriter(output); BufferedReader
	 * reader = new BufferedReader(new InputStreamReader(input));
	 * 
	 * JSONParser parser = new JSONParser(); // this will help us parse the request
	 * object JSONObject responseObject = new JSONObject(); // we will add to this
	 * object for our api response JSONObject responseBody = new JSONObject();// we
	 * will add the item to this object
	 * 
	 * AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient(); DynamoDB
	 * dynamoDB = new DynamoDB(client);
	 * 
	 * String searchParam; Item resItem = null; List<ArrayList<String>> filesList =
	 * new ArrayList<ArrayList<String>>();
	 * 
	 * 
	 * 
	 * try { JSONObject reqObject = (JSONObject) parser.parse(reader);
	 * //pathParameters if (reqObject.get("pathParameters")!=null) { JSONObject pps
	 * = (JSONObject)reqObject.get("pathParameters"); if
	 * (pps.get("searchParam")!=null) { searchParam =
	 * (String)pps.get("searchParam");
	 * 
	 * ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMO_TABLE);
	 * 
	 * ScanResult result = client.scan(scanRequest); List<Map<String,
	 * AttributeValue>> files = result.getItems();
	 * 
	 * for (Map<String, AttributeValue> item : files) {
	 * 
	 * Collection<AttributeValue> valList = item.values();
	 * 
	 * ArrayList<String> tempList = new ArrayList<>();
	 * 
	 * for (AttributeValue value : valList) tempList.add(value.toString());
	 * 
	 * filesList.add(tempList); }
	 * 
	 * 
	 * 
	 * 
	 * }
	 * 
	 * 
	 * 
	 * 
	 * }
	 * 
	 * 
	 * if (resItem!=null) { Product product = new Product(resItem.toJSON());
	 * responseBody.put("product", product); responseObject.put("statusCode", 200);
	 * }else { responseBody.put("message", "No Items Found");
	 * responseObject.put("statusCode", 404); }
	 * 
	 * responseObject.put("body", responseBody.toString());
	 * 
	 * } catch (Exception e) { context.getLogger().log("ERROR : "+e.getMessage()); }
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * responseObject.put("body", responseBody.toString());
	 * 
	 * } catch (Exception e) { context.getLogger().log("ERROR : " + e.getMessage());
	 * } writer.write(responseObject.toString()); reader.close(); writer.close();
	 * 
	 * }
	 */

}
