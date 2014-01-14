package com.aerospike.recommendation.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.recommendation.model.MovieRating;

@Controller
public class RESTController {
	public static final String PRODUCT_SET = "MOVIE_TITLES";
	public static final String PRODUCT_HISTORY = "WATCHED_BY";
	public static final String USERS_SET = "MOVIE_CUSTOMERS";
	public static final String USER_HISTORY = "MOVIES_WATCHED";
	private static Logger log = Logger.getLogger(RESTController.class); 
	@Autowired
	AerospikeClient client;

	static final String nameSpace;
	static {
		Properties as = System.getProperties();
		nameSpace = (String) as.get("namespace");
	}
	/**
	 * get a recommendation for a specific user
	 * @param user a unique ID for a user
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/recommendation/{user}", method=RequestMethod.GET)
	public @ResponseBody JSONArray getRecommendationFor(@PathVariable("user") String user) throws Exception {
		log.debug("Finding recomendations for " + user);
		Policy policy = new Policy();

		// Get the user's purchase history as a list of ratings
		Key key = new Key(nameSpace, USERS_SET, Value.get(user));
		Record thisUser = client.get(policy, key, USER_HISTORY);
		if (thisUser == null){
			log.debug("Could not find user: " + user );
			throw new UserNotFound(user);
		}
		List<Map<String, Object>> thisUserProducts = (List<Map<String, Object>>)thisUser.getValue(USER_HISTORY);

		// For each product listed retrieve the product profile
		// Do this in a BATCH operation that retrieves multiple
		// records in 1 operation.
		Key[] productKeys = new Key[thisUserProducts.size()];
		int index = 0;
		for (Map<String, Object> rating : thisUserProducts){
			productKeys[index] = new Key(nameSpace, PRODUCT_SET, (String)rating.get(MovieRating.MOVIE_ID)); 
			index++;
		}
		
		// Get all the products' histories
		Record[] productProfiles = client.get(policy, productKeys, PRODUCT_HISTORY);

		// For each product profile retrieve the user
		// profile, excluding the target user

		Record bestMatchedUser = null;
		int bestScore = 0;

		Set<Key> possibleMatches = new HashSet<Key>();
		for (Record product : productProfiles){
			List<Map<String, Object>> userRatingList = (List<Map<String, Object>>) product.getValue(PRODUCT_HISTORY);
			if (!(userRatingList == null)){
				for (Map<String, Object> userRating : userRatingList){
					if (!userRating.get(MovieRating.CUSTOMER_ID).equals(user)) //exclude target user
						possibleMatches.add(new Key(nameSpace, USERS_SET, Value.get(userRating.get(MovieRating.CUSTOMER_ID))));
				}
			}
		}

		Record[] similarUsers = client.get(policy, possibleMatches.toArray(new Key[0]), USER_HISTORY);
		// find user with the highest similarity
		for (Record similarUser : similarUsers){
			List<Map<String, Object>> similarUserProduct = (List<Map<String, Object>>)similarUser.getValue(USER_HISTORY);
			int score = easySimilarity(thisUserProducts, similarUserProduct);
			if (score > bestScore){
				bestScore = score;
				bestMatchedUser = similarUser;
			}
		}
		// return the best matched user's purchases as the recommendation
		JSONArray recommendations = new JSONArray();
		Set<MovieRating> bestMatchedPurchases = new HashSet<MovieRating>() ;
		bestMatchedPurchases.addAll( (Collection<? extends MovieRating>) bestMatchedUser.getValue(USER_HISTORY));
		//Remove common products
		bestMatchedPurchases.removeAll(thisUserProducts);

		productKeys = new Key[bestMatchedPurchases.size()];
		index = 0;
		for (MovieRating product : bestMatchedPurchases){
			productKeys[index] = new Key("test", PRODUCT_SET, product.getMovie());
		}
		Record[] recommendedMovies = client.get(policy, productKeys, "Title", "YearOfRelease");
		log.debug("Found these recomendations: " + recommendations);
		for (Record rec: recommendedMovies){
			recommendations.add(new JSONRecord(rec));
		}
		return recommendations;
	}

	/*
	 * profile file upload
	 */
	@RequestMapping(value="/profileUpload", method=RequestMethod.GET)
	public @ResponseBody String provideUploadInfo() {
		return "You can upload a file by posting to this same URL.";
	}
	/**
	 * Upload a CSV file containing either user profiles OR product profiles
	 * @param what USER for user profile, PRODUCT for product profile
	 * @param name
	 * @param file
	 * @return
	 */
	@RequestMapping(value="/profileUpload/{what}", method=RequestMethod.POST)
	public @ResponseBody String handleFileUpload(@PathVariable("what") String what, @RequestParam("name") String name, 
			@RequestParam("file") MultipartFile file){
		
		if (!file.isEmpty() && (what.equalsIgnoreCase("USER") || what.equalsIgnoreCase("PRODUCT"))) {
			try {
				WritePolicy wp = new WritePolicy();
				String line =  "";
				String setName = what;
				String binName = (setName.equalsIgnoreCase("USER"))? USER_HISTORY : PRODUCT_HISTORY;
				BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
				while ((line = br.readLine()) != null) {


					// use comma as key and bins separator 
					String[] values = line.split(",");
					// use colon for value separator
					String[] history = values[1].trim().split(":");
					List<String> historyList = new ArrayList<String>();
					for (String entry : history){
						historyList.add(entry);
					}

					// write the record to Aerospike
					Key key = new Key(nameSpace, setName, values[0].trim() );
					Bin bin = new Bin(binName, Value.get(historyList));
					client.put(wp, key, bin);

					log.info(setName + " [ID=" + values[0].trim() 
							+ " , history=" + values[1].trim() + "]"); 

				}
				br.close();
				log.info("Successfully uploaded " + name);
				return "You successfully uploaded " + name;
			} catch (Exception e) {
				log.error("Failed to upload " + name, e);
				return "You failed to upload " + name + " => " + e.getMessage();
			}
		} else {
			log.error("Failed to upload " + name + " because the file was empty.");
			return "You failed to upload " + name + " because the file was empty.";
		}
	}
	/**
	 * This is a very rudimentary similarity algorithm
	 * @param thisUserProducts
	 * @param similarUserProduct
	 * @return
	 */
	private int easySimilarity(List<Map<String, Object>> thisUserProducts, List<Map<String, Object>> similarUserProduct){
		int incommon = 0;

		for (Map<String, Object> sourceRating : thisUserProducts){
			if (similarUserProduct.contains(sourceRating.get(MovieRating.MOVIE_ID))){
				incommon++;
			}
		}
		return incommon;
	}

//	private double cosineSimilarity(List<Integer> vec1, List<Integer> vec2) { 
//		double dp = dotProduct(vec1, vec2); 
//		double magnitudeA = magnitude(vec1); 
//		double magnitudeB = magnitude(vec2); 
//		return dp / magnitudeA * magnitudeB; 
//	} 
//
//	private double magnitude(List<Integer> vec) { 
//		double sum_mag = 0; 
//		for(Integer value : vec) { 
//			sum_mag += value * value; 
//		} 
//		return Math.sqrt(sum_mag); 
//	} 
//
//	private double dotProduct(List<Integer> vec1, List<Integer> vec2) { 
//		double sum = 0; 
//		for(int i = 0; i<vec1.size(); i++) { 
//			sum += vec1.get(i) * vec2.get(i); 
//		} 
//		return sum; 
//	} 

}
