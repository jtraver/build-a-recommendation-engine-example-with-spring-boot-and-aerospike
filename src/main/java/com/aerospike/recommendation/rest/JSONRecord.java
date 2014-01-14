package com.aerospike.recommendation.rest;

import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.aerospike.client.Record;
import com.aerospike.client.query.ResultSet;

/**
 * JSONRecord is used to convert an Aerospike Record
 * returned from the cluster to JSON format
 * @author peter
 *
 */
@SuppressWarnings("serial")
public class JSONRecord extends JSONObject {
	@SuppressWarnings("unchecked")
	public JSONRecord(Record record){
		put("generation", record.generation);
		put("expiration", record.expiration);
		put("bins", new JSONObject(record.bins));
		if (record.duplicates != null){
			JSONArray duplicates = new JSONArray();
			for (Map<String, Object> duplicate : record.duplicates){
				duplicates.add(new JSONObject(duplicate));
			}
			put("duplicates", duplicates);
		}
	}

}
