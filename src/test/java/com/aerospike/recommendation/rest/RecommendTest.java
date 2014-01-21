package com.aerospike.recommendation.rest;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import com.aerospike.client.AerospikeClient;

public class RecommendTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception{
		// set properties
		Properties as = System.getProperties();
		as.put("seedHost", "192.168.51.200");
		as.put("port", "3000");
		as.put("namespace", "test");
		// start app
		ApplicationContext appCon = SpringApplication.run(AerospikeRecommendationService.class, new String[0]);
		appCon.getBean(AerospikeRecommendationService.class);
		RESTController controller = appCon.getBean(RESTController.class);
		
		controller.getRecommendationFor("893988");
	}

}
