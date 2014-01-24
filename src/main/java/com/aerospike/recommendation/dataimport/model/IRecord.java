package com.aerospike.recommendation.dataimport.model;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;

public interface IRecord {

	public abstract Bin[] asBins();

	public abstract Key getKey(String namespace, String set)
			throws AerospikeException;
	
	public abstract void fromRecord(Record record);

}