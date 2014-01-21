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
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.recommendation.model.Customer;
import com.aerospike.recommendation.model.Movie;
import com.aerospike.recommendation.model.WatchedRated;

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
				Movie movie = new Movie(fields[0], 
						fields[1], 
						fields[2]);
				Key key = new Key(RESTController.NAME_SPACE, RESTController.PRODUCT_SET, fields[0]);
				// write the record to Aerospike
				client.put(writePolicy, movie.getKey("test", RESTController.PRODUCT_SET),
						movie.asBins());

				System.out.println(movie); 

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

		Movie movie = null;
		String movieID = null;
		int rating = 0;
		List<WatchedRated> watchedBy = null;
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		while ((line = br.readLine()) != null) {
			Customer customer = null;
			String customerID = null;
			String date = null;
			Record customerRecord = null;

			if (line.endsWith(":")){ // we are going to process a movie
				// update the last movie 
				if (movie != null){
					movie.setWatchedBy(watchedBy);
					saveMovieRecord(movie);
				}
				// get the next movie ID
				movieID = line.substring(0, line.length()-1);
				movie = new Movie(movieID);
				// fetch the movie record
				Record movieRecord = client.get(policy, movie.getKey(RESTController.NAME_SPACE, 
						RESTController.PRODUCT_SET));
				
				if (movieRecord != null){
					/*
					 * if we find a movie record in Aerospike
					 * get its watched by list
					 */
					movie.fromRecord(movieRecord);
					if (movie.getWatchedBy() == null)
						movie.setWatchedBy(new ArrayList<WatchedRated>());
					watchedBy = movie.getWatchedBy();
					
				} else {
					/*
					 * if we dont, null out the movie reference
					 */
					movie = null;
				}
			} else {
				String[] values = line.split(",");
				customerID = values[0];
				rating = Integer.parseInt(values[1]);
				date = values[2];

				WatchedRated newRating = new WatchedRated(movieID,customerID, rating, date);

				watchedBy.add(newRating);

				customer = new Customer(customerID);

				// get the movies watched
				List<WatchedRated> watched = null;
				customerRecord = client.get(policy, customer.getKey(RESTController.NAME_SPACE, RESTController.USERS_SET));
				if (customerRecord != null) {
					customer.fromRecord(customerRecord);
				}
				
				if (customer.getWatched() == null)
					customer.setWatched(new ArrayList<WatchedRated>());
				watched = customer.getWatched();

				watched.add(newRating);

				// update customer
				WritePolicy wp = new WritePolicy();
				client.put(wp, 
						customer.getKey(RESTController.NAME_SPACE, RESTController.USERS_SET), 
						customer.asBins());
				customer = null;
			}



		}
		br.close();
		if (movie != null){
			saveMovieRecord(movie);
			movie = null;
		}
		System.out.println("Successfully processed " + file.getName());

	}
	
	private void saveMovieRecord(Movie movie) throws AerospikeException {
		WritePolicy wp = new WritePolicy();
		// sort the WatchedRated records by date
		movie.sortWatched();
		client.put(wp, 
				movie.getKey(RESTController.NAME_SPACE, RESTController.PRODUCT_SET), 
				movie.asBins());
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
