package com.aerospike.recommendation.dataimport;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.recommendation.dataimport.model.Customer;
import com.aerospike.recommendation.dataimport.model.Movie;
import com.aerospike.recommendation.dataimport.model.WatchedRated;
import com.aerospike.recommendation.rest.RESTController;

public class MoviesUploader {
	private static Logger log = Logger.getLogger(MoviesUploader.class);

	public static final String MOVIE_DIR = "src/test/resources/movies";
	private static AerospikeClient client;
	private static WritePolicy writePolicy;


	public static void main(String[] args) throws Exception{
		Options options = new Options();
		options.addOption("h", "host", true, "Server hostname (default: localhost)");
		options.addOption("p", "port", true, "Server port (default: 3000)");
		options.addOption("n", "namespace", true, "Namespace (default: test)");
		options.addOption("l", "limit", true, "Limit the number of movies uploaded");
		options.addOption("u", "usage", false, "Print usage.");

		CommandLineParser parser = new PosixParser();
		CommandLine cl = parser.parse(options, args, false);

		if (args.length == 0 || cl.hasOption("u")) {
			logUsage(options);
			return;
		}
		int limit = 0;
		String host = cl.getOptionValue("h", "127.0.0.1");
		String portString = cl.getOptionValue("p", "3000");
		int port = Integer.parseInt(portString);
		String namespace = cl.getOptionValue("n","test");

		log.debug("Host: " + host);
		log.debug("Port: " + port);
		log.debug("Name space: " + namespace);
		if (cl.hasOption("l")){
			limit = Integer.parseInt(cl.getOptionValue("l", "0"));
		}
		log.debug("Limit: " + limit);

		client = new AerospikeClient(host, port);
		writePolicy = new WritePolicy();

		File ratingDir = new File(MOVIE_DIR);
		File[] ratingFiles = ratingDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File file) {
				return file.getName().startsWith("movie_000") && file.getName().endsWith(".json");
			}
		});
		// process each rating file
		int counter = 0;
		for (File ratingFile : ratingFiles){
			processRatingFile(ratingFile);
			counter++;
			if (limit != 0 && counter == limit)
				break;
		}

	}
	@SuppressWarnings("unchecked")
	private static void processRatingFile(File file) throws IOException, AerospikeException, ParseException {
		if (!checkFileExists(file)) return;
		Policy policy = new Policy();
		policy.timeout = 0;
		JSONParser parser = new JSONParser();
		
		Object obj = parser.parse(new FileReader(file));
		JSONObject jsonObject = (JSONObject) obj;
		 
		Movie movie = new Movie(jsonObject);
		
		saveMovieRecord(movie);
		
		for (WatchedRated wr : movie.getWatchedBy()) {
			Customer customer = null;
			String customerID = null;
			Record customerRecord = null;

				customerID = wr.getCustomerID();


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

				watched.add(wr);

				// update customer
				client.put(writePolicy, 
						customer.getKey(RESTController.NAME_SPACE, RESTController.USERS_SET), 
						customer.asBins());
				customer = null;



		}
		System.out.println("Successfully processed " + file.getName());

	}

	private static void saveMovieRecord(Movie movie) throws AerospikeException {
		WritePolicy wp = new WritePolicy();
		// sort the WatchedRated records by date
		movie.sortWatched();
		client.put(wp, 
				movie.getKey(RESTController.NAME_SPACE, RESTController.PRODUCT_SET), 
				movie.asBins());
	}

	private static boolean checkFileExists(File file){
		if (!file.exists()) {
			Assert.fail("File " + file.getName() + " does not extst");
			return false;
		}
		return true;

	}
	private static void logUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		String syntax = MoviesUploader.class.getName() + " [<options>]";
		formatter.printHelp(pw, 100, syntax, "options:", options, 0, 2, null);
		log.info(sw.toString());
	}

}
