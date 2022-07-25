package com.pwm.aws.crud.lambda.api.model;

import com.google.gson.Gson;

public class File {

	int id;
	String name;
	String url;
	
	public File(int id, String name, String url) {
		this.id = id;
		this.name = name;
		this.url = url;
	}
	
	public File(String json) {
		Gson gson = new Gson();
		File file = gson.fromJson(json,  File.class);
		this.id = file.id;
		this.name = file.name;
		this.url = file.url;
	}
	
	public String toString() {
		return new Gson().toJson(this);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
}
