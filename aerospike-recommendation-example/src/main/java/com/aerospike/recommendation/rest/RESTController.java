package com.aerospike.recommendation.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

@Controller
public class RESTController {
	private static final String PRODUCT_SET = "PRODUCT";
	private static final String PRODUCT_HISTORY = "history";
	private static final String USERS_SET = "USER";
	private static final String USER_HISTORY = "history";
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

		Policy policy = new Policy();

		// Get the user's purchase history as a list of strings
		Key key = new Key(nameSpace, USERS_SET, Value.get(user));
		Record userProfile = client.get(policy, key, USER_HISTORY);
		List<String> receivedList = (List<String>)userProfile.getValue(USER_HISTORY);
		Set<String> sourcePurchaseList = new HashSet<String>() ;
		sourcePurchaseList.addAll(receivedList);


		// For each product listed retrieve the product profile
		// Do this in a BATCH operation that retrieves multiple
		// records in 1 operation.
		Key[] productKeys = new Key[sourcePurchaseList.size()];
		int index = 0;
		for (String product : sourcePurchaseList){
			productKeys[index] = new Key(nameSpace, PRODUCT_SET, product); 
			index++;
		}
		Record[] productProfiles = client.get(policy, productKeys, USER_HISTORY);

		// For each product profile retrieve the user
		// profile, excluding the target user
		Record bestMatchedUser = null;
		Set<Key> possibleMatches = new HashSet<Key>();
		for (Record product : productProfiles){
			List<String> userList = (List<String>) product.getValue(PRODUCT_HISTORY);
			for (String productUser : userList){
				if (!productUser.equals(user)) //exclude target user
					possibleMatches.add(new Key(nameSpace, USERS_SET, Value.get(productUser)));
			}
		}
		;
		Record[] similarUsers = client.get(policy, possibleMatches.toArray(new Key[0]), USER_HISTORY);
		// find user with the highest similarity
		for (Record similarUser : similarUsers){
			//TODO  Insert "similarity black magic" here
			bestMatchedUser = similarUser;
		}
		// return the best matched user's purchases as the recommendation
		JSONArray recommendations = new JSONArray();
		Set<String> bestMatchedPurchases = new HashSet<String>() ;
		bestMatchedPurchases.addAll( (Collection<? extends String>) bestMatchedUser.getValue(USER_HISTORY));
		//Remove common products
		bestMatchedPurchases.removeAll(sourcePurchaseList);

		for (String product : bestMatchedPurchases){
			recommendations.add(product);
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

					/*
					 * write the record to Aerospike
					 * NOTE: Bin names must not exceed 14 characters
					 */
					Key key = new Key(nameSpace, setName, values[0].trim() );
					Bin bin = new Bin(binName, Value.get(historyList));
					client.put(wp, key, bin);

					log.info(setName + " [ID= " + values[0].trim() 
							+ " , history=" + values[1].trim()); 

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

}
