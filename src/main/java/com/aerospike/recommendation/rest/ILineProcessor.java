package com.aerospike.recommendation.rest;

import java.io.IOException;

import com.aerospike.client.AerospikeException;

public interface ILineProcessor {
	public String[] parseLine(String line);
	public void storeRecord(String[] fields) throws IOException, AerospikeException;
}
