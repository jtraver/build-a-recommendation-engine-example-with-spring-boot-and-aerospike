package com.aerospike.recommendation.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class ProductUserUploader {

	private static final String USER_FILE = "src/test/resources/users.csv";
	private static final String PRODUCT_FILE = "src/test/resources/products.csv";

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void uploadProductsUsers() {
        RestTemplate template = new RestTemplate();
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
        parts.add("name", USER_FILE);
        parts.add("file", new FileSystemResource(USER_FILE));
        String response = template.postForObject("http://localhost:8080/profileUpload/USER", parts, String.class);
        System.out.println(response);

        parts = new LinkedMultiValueMap<String, Object>();
        parts.add("name", PRODUCT_FILE);
        parts.add("file", new FileSystemResource(PRODUCT_FILE));
        response = template.postForObject("http://localhost:8080/profileUpload/PRODUCT", parts, String.class);
        System.out.println(response);
	}
}
