package com.aerospike.recommendation.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.recommendation.model.MovieRating;

public class NetFlicksUploader {
	public static final String MOVIE_TITLES = "src/test/resources/net-flicks/movie_titles.txt";
	public static final String RATINGS = "src/test/resources/net-flicks/training_set";
	AerospikeClient client;
	WritePolicy writePolicy;

	@Before
	public void setUp() throws Exception {
		String seedHost = System.getenv("seedHost");
		String port = System.getenv("port");
		client = new AerospikeClient(seedHost, Integer.parseInt(port));
		writePolicy = new WritePolicy();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {

		// Movie title
		// MovieID,YearOfRelease,Title
		File tracks = new File(MOVIE_TITLES);
		processFile(tracks, new ILineProcessor() {

			@Override
			public void storeRecord(String[] fields) throws AerospikeException {
				Key key = new Key("test", RESTController.PRODUCT_SET, fields[0]);
				// write the record to Aerospike
				client.put(writePolicy, key,
						new Bin("YearOfRelease", Value.get(fields[1])),
						new Bin("Title", Value.get(fields[2]))
						);


				System.out.println("MOVIE_TITLE [ID=" + fields[0] 
						+ ", title=" + fields[2]); 

			}

			@Override
			public String[] parseLine(String line) {
				return line.split(",");
			}
		});

		// Movie rating
		//The first line of each file contains the movie id followed by a colon.  
		//Each subsequent line in the file corresponds to a rating from a customer
		//and its date in the following format:
		//
		//CustomerID,Rating,Date
		//
		//- MovieIDs range from 1 to 17770 sequentially.
		//- CustomerIDs range from 1 to 2649429, with gaps. There are 480189 users.
		//- Ratings are on a five star (integral) scale from 1 to 5.
		//- Dates have the format YYYY-MM-DD.

		File ratingDir = new File(RATINGS);
		File[] ratingFiles = ratingDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File file) {
				return file.getName().startsWith("mv_") && file.getName().endsWith(".txt");
			}
		});
		// process each rating file
		int counter = 0;
		for (File ratingFile : ratingFiles){
			processRatingFile(ratingFile);
			counter++;
			if (counter > 100)
				break;
		}

	}
	@SuppressWarnings("unchecked")
	private void processRatingFile(File file) throws IOException, AerospikeException {
		checkFileExists(file);
		String line =  "";
		Policy policy = new Policy();
		policy.timeout = 0;

		Key movieKey = null;
		Record movie = null;
		String movieID = null;
		int rating = 0;
		List<Map<String, Object>> watchedBy = null;
		boolean movieToUpdate = false;
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		while ((line = br.readLine()) != null) {
			String customerID = null;
			String date = null;
			Key customerKey = null;
			Record customerRecord = null;

			if (line.endsWith(":")){
				// update the last movie with the new rating
				if (movieToUpdate){
					for (Map<String, Object> mr : watchedBy){
						rating += (Integer) mr.get(MovieRating.RATING);
					}
					rating /= watchedBy.size();
					saveMovieRecord(movieID, watchedBy, rating);
				}
				// get the next movie ID
				movieID = line.substring(0, line.length()-1);
				movieKey = new Key("test", RESTController.PRODUCT_SET, movieID);
				// fetch the movie record
				movie = client.get(policy, movieKey, RESTController.PRODUCT_HISTORY);
				if (movie != null){
					watchedBy = (List<Map<String, Object>>) movie.bins.get(RESTController.PRODUCT_HISTORY);
					if (watchedBy == null)
						watchedBy = new ArrayList<Map<String, Object>>();
				} else {
					watchedBy = new ArrayList<Map<String, Object>>();
				}
				movieToUpdate = true;
			} else {
				String[] values = line.split(",");
				customerID = values[0];
				rating = Integer.parseInt(values[1]);
				date = values[2];

				Map<String, Object> newRating = new HashMap<String, Object>();
				newRating.put(MovieRating.MOVIE_ID, movieID);
				newRating.put(MovieRating.CUSTOMER_ID, customerID);
				newRating.put(MovieRating.RATING, rating);
				newRating.put(MovieRating.DATE, date);

				watchedBy.add(newRating);

				customerKey = new Key("test", RESTController.USERS_SET, customerID);

				// get the movies watched
				List<Map<String, Object>> watched = null;
				customerRecord = client.get(policy, customerKey, RESTController.USER_HISTORY);
				if (customerRecord != null) {
					watched = (List<Map<String, Object>>) customerRecord.getValue(RESTController.USER_HISTORY);
					if (watched == null)
						watched = new ArrayList<Map<String, Object>>();
				} else {
					watched = new ArrayList<Map<String, Object>>();
				}
				watched.add(newRating);

				// update customer
				WritePolicy wp = new WritePolicy();
				client.put(wp, customerKey, Bin.asList(RESTController.USER_HISTORY, watched));
			}



		}
		br.close();
		if (movieToUpdate){
			saveMovieRecord(movieID, watchedBy, rating);
		}
		System.out.println("Successfully processed " + file.getName());

	}
	
	private void saveMovieRecord(String movieID, List<Map<String, Object>> watchedBy, int rating) throws AerospikeException {
		WritePolicy wp = new WritePolicy();
		Key movieKey = new Key("test", RESTController.PRODUCT_SET, movieID);
		client.put(wp, movieKey, Bin.asList(RESTController.PRODUCT_HISTORY, watchedBy), new Bin("RATING", rating));
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
