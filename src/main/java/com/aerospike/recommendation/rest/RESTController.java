package com.aerospike.recommendation.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;

@Controller
public class RESTController {
	private static final int MOVIE_REVIEW_LIMIT = 20;
	public static final String NAME_SPACE = "test";
	public static final String PRODUCT_SET = "MOVIE_TITLES";
	public static final String USERS_SET = "MOVIE_CUSTOMERS";

	public static final String DATE = "date";
	public static final String RATING = "rating";
	public static final String CUSTOMER_ID = "customer-id";
	public static final String MOVIE_ID = "movie-id";
	public static final String WATCHED_BY = "watchedBy";
	public static final String TITLE = "title";
	public static final String YEAR_OF_RELEASE = "yearOfRelease";
	public static final String CUSTOMER_WATCHED = "watched";
	private static Logger log = Logger.getLogger(RESTController.class); 
	@Autowired
	AerospikeClient client;

	static final String nameSpace;
	static {
		Properties as = System.getProperties();
		nameSpace = (String) as.get("namespace");
	}
	/**
	 * get a recommendation for a specific customer
	 * @param user a unique ID for a customer
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/recommendation/{customer}", method=RequestMethod.GET)
	public @ResponseBody JSONArray getRecommendationFor(@PathVariable("customer") String customerID) throws Exception {
		log.debug("Finding recomendations for " + customerID);
		Policy policy = new Policy();

		/* 
		 * Get the customer's purchase history as a list of ratings
		 */
		Record thisUser = client.get(policy, new Key(NAME_SPACE, USERS_SET, customerID));
		if (thisUser == null){
			log.debug("Could not find user: " + customerID );
			throw new CustomerNotFound(customerID);
		}
		/*
		 * get the movies watched and rated
		 */
		List<Map<String, Object>> customerWatched = (List<Map<String, Object>>) thisUser.getValue(CUSTOMER_WATCHED);
		if (customerWatched == null || customerWatched.size()==0){
			// customer Hasen't Watched anything
			log.debug("No movies found for customer: " + customerID );
			throw new NoMoviesFound(customerID);
		}

		/*
		 * build a vector list of movies watched
		 */
		List<Integer> thisCustomerMovieVector = makeVector(customerWatched);


		Record bestMatchedCustomer = null;
		double bestScore = 0;
		/*
		 * for each movie this customer watched, iterate
		 * through the other customers that also watched
		 * the movie 
		 */
		for (Map<String, Object> wr : customerWatched){
			Key movieKey = new Key(NAME_SPACE, PRODUCT_SET, (String) wr.get(MOVIE_ID) );
			Record movieRecord = client.get(policy, movieKey);

			List<Map<String, Object>> whoWatched = (List<Map<String, Object>>) movieRecord.getValue(WATCHED_BY);

			if (!(whoWatched == null)){
				int end = Math.min(MOVIE_REVIEW_LIMIT, whoWatched.size()); 
				/* 
				 * Some movies are watched by >100k customers, only look at the last n movies, or the 
				 * number of customers, whichever is smaller
				 */
				for (int index = 0; index < end; index++){
					Map<String, Object> watchedBy = whoWatched.get(index);
					String similarCustomerId = (String) watchedBy.get(CUSTOMER_ID);
					if (!similarCustomerId.equals(customerID)) {
						// find user with the highest similarity

						Record similarCustomer = client.get(policy, new Key(NAME_SPACE, USERS_SET, similarCustomerId));

						List<Map<String, Object>> similarCustomerWatched = (List<Map<String, Object>>) similarCustomer.getValue(CUSTOMER_WATCHED);
						double score = easySimilarity(thisCustomerMovieVector, similarCustomerWatched);
						if (score > bestScore){
							bestScore = score;
							bestMatchedCustomer = similarCustomer;
						}
					}
				}
			}
		}
		log.debug("Best customer: " + bestMatchedCustomer);
		log.debug("Best score: " + bestScore);
		// return the best matched user's purchases as the recommendation
		List<Integer> bestMatchedPurchases = new ArrayList<Integer>();
		for (Map<String, Object> watched : (List<Map<String, Object>>)bestMatchedCustomer.getValue(CUSTOMER_WATCHED)){
			Integer movieID = Integer.parseInt((String) watched.get(MOVIE_ID));
			if ((!thisCustomerMovieVector.contains(movieID))&&(movieID != null)){
				bestMatchedPurchases.add(movieID);
			}
		}

		// get the movies
		Key[] recomendedMovieKeys = new Key[bestMatchedPurchases.size()];
		int index = 0;
		for (int recomendedMovieID : bestMatchedPurchases){
			recomendedMovieKeys[index] = new Key(NAME_SPACE, PRODUCT_SET, String.valueOf(recomendedMovieID));
			log.debug("Added Movie key: " + recomendedMovieKeys[index]);
			index++;
		}
		Record[] recommendedMovies = client.get(policy, recomendedMovieKeys, TITLE, YEAR_OF_RELEASE);
		
		// This is a diagnostic step
		if (log.isDebugEnabled()){
			log.debug("Recomended Movies:");
			for (Record rec : recommendedMovies){
				log.debug(rec);
			}
		}
		
		// Turn the Aerospike records into a JSONArray
		JSONArray recommendations = new JSONArray();
		for (Record rec: recommendedMovies){
			if (rec != null)
				recommendations.add(new JSONRecord(rec));
		}
		log.debug("Found these recomendations: " + recommendations);
		return recommendations;
	}
	/**
	 * Produces a Integer vector from the movie IDs
	 * @param ratingList
	 * @return
	 */
	private List<Integer> makeVector(List<Map<String, Object>> ratingList){
		List<Integer> movieVector = new ArrayList<Integer>();
		for (Map<String, Object> one : ratingList){
			movieVector.add(Integer.parseInt((String)one.get(MOVIE_ID)));
		}
		return movieVector;
	}
	/**
	 * This is a very rudimentary algorithm using Cosine similarity
	 * @param customerWatched
	 * @param similarCustomerWatched
	 * @return
	 */
	private double easySimilarity(List<Integer> thisCustomerVector, List<Map<String, Object>> similarCustomerWatched){
		double incommon = 0;
		/*
		 * this is the place where you can create clever
		 * similarity score.
		 * 
		 * This algorithm simple returns how many movies these customers have in common.
		 * 
		 * You could use any similarity algorithm you wish
		 */
		List<Integer> similarCustomerVector = makeVector(similarCustomerWatched);
		
		return cosineSimilarity(thisCustomerVector, similarCustomerVector);
	}

	/**
	 * Cosing similarity
	 * @param vec1
	 * @param vec2
	 * @return
	 */
	private double cosineSimilarity(List<Integer> vec1, List<Integer> vec2) { 
		double dp = dotProduct(vec1, vec2); 
		double magnitudeA = magnitude(vec1); 
		double magnitudeB = magnitude(vec2); 
		return dp / magnitudeA * magnitudeB; 
	} 
	/**
	 * Magnitude
	 * @param vec
	 * @return
	 */
	private double magnitude(List<Integer> vec) { 
		double sum_mag = 0; 
		for(Integer value : vec) { 
			sum_mag += value * value; 
		} 
		return Math.sqrt(sum_mag); 
	} 
	/**
	 * Dot product
	 * @param vec1
	 * @param vec2
	 * @return
	 */
	private double dotProduct(List<Integer> vec1, List<Integer> vec2) { 
		double sum = 0; 
		if (vec1.size() > vec2.size()) {
			int diff = vec1.size() - vec2.size();
			for (int i = 0; i < diff; i++)
					vec2.add(0);
			
		} else if (vec1.size() < vec2.size()) {
			int diff = vec2.size() - vec1.size();
			for (int i = 0; i < diff; i++)
					vec1.add(0);
		}
		for(int i = 0; i<vec1.size(); i++) { 
			sum += vec1.get(i) * vec2.get(i); 
		} 
		return sum; 
	} 

}
