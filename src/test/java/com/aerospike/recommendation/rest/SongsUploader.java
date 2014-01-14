package com.aerospike.recommendation.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.client.policy.WritePolicy;

public class SongsUploader {

	AerospikeClient client;
	WritePolicy writePolicy;

	public static final String TRACKS = "src/test/resources/songs/subset_unique_tracks.txt";
	public static final String TERMS = "src/test/resources/songs/subset_unique_terms.txt";
	public static final String MBTAGS = "src/test/resources/songs/subset_unique_mbtags.txt";
	public static final String ARTISTS = "src/test/resources/songs/subset_unique_artists.txt";
	public static final String ARTIST_LOCATION = "src/test/resources/songs/subset_artist_location.txt";
	public static final String TRACKS_BY_YEAR = "src/test/resources/songs/subset_tracks_per_year.txt";

	@Before
	public void setUp() throws Exception {
		String seedHost = System.getenv("seedHost");
		String port = System.getenv("port");
		this.client = new AerospikeClient(seedHost, Integer.parseInt(port));
		writePolicy = new WritePolicy();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSongData() throws Exception{

		/*
		 * Load the tracks
		 */
		File tracks = new File(TRACKS);
		processFile(tracks, new ILineProcessor() {

			@Override
			public void storeRecord(String[] fields) throws AerospikeException {
				Key key = new Key("test", "TRACKS", fields[0]);
				// write the record to Aerospike
				client.put(writePolicy, key,
						new Bin("LABEL_ID", Value.get(fields[1])),
						new Bin("ARTIST_NAME", Value.get(fields[2])),
						new Bin("TRACK_NAME", Value.get(fields[3]))
						);


				System.out.println("TRACK [ID=" + fields[0] 
						+ ", name=" + fields[3]); 

			}

			@Override
			public String[] parseLine(String line) {
				return line.split(":");
			}
		});

		/*
		 * Load the artists
		 */
		File artists = new File(ARTISTS);
		processFile(artists, new ILineProcessor() {

			@Override
			public void storeRecord(String[] fields) throws AerospikeException {
				Key key = new Key("test", "ARTISTS", fields[0]);
				// write the record to Aerospike
				client.put(writePolicy, key,
						new Bin("UUID", Value.get(fields[1])),
						new Bin("TRACK_ID", Value.get(fields[2])),
						new Bin("ARTIST_NAME", Value.get(fields[3]))
						);


				System.out.println("ARTIST [ID=" + fields[0] 
						+ ", name=" + fields[3]); 

			}

			@Override
			public String[] parseLine(String line) {
				return line.split(":");
			}
		});


	}

	private void processFile(File file, ILineProcessor processor) throws IOException, AerospikeException {
		checkFileExists(file);
		String line =  "";
		String setName = file.getName();
		if (setName.lastIndexOf('.') > 0){ // Remove the file extension
			setName = setName.substring(0, setName.lastIndexOf('.'));
		}
		BufferedReader br = new BufferedReader(new FileReader(file));
		while ((line = br.readLine()) != null) {

			String[] values = processor.parseLine(line);
			processor.storeRecord(values);

		}
		br.close();
		System.out.println("Successfully processed " + setName);

	}

	private boolean checkFileExists(File file){
		if (!file.exists()) {
			Assert.fail("File " + file.getName() + " does not extst");
			return false;
		}
		return true;

	}
}
