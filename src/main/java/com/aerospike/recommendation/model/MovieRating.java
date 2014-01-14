package com.aerospike.recommendation.model;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class MovieRating {
	
	public static final String DATE = "date";
	public static final String RATING = "rating";
	public static final String CUSTOMER_ID = "customer-id";
	public static final String MOVIE_ID = "movie-id";
	
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	public MovieRating() {
		super();
	}
	@SuppressWarnings("unchecked")
	public MovieRating(String movie, String customerID, int rating, String date) {
		this();
		this.properties.put(MOVIE_ID, movie);
		this.properties.put(CUSTOMER_ID, customerID);
		this.properties.put(RATING, rating);
		this.properties.put(DATE, date);
	}
	public String getMovie() {
		return (String) this.properties.get(MOVIE_ID);
	}
	@SuppressWarnings("unchecked")
	public void setMovie(String movie) {
		this.properties.put(MOVIE_ID, movie);
	}
	
	public String getCustomerID() {
		return (String) this.properties.get(CUSTOMER_ID);
	}
	@SuppressWarnings("unchecked")
	public void setCustomerID(String movie) {
		this.properties.put(CUSTOMER_ID, movie);
	}

	public int getRating() {
		return (Integer) this.properties.get(RATING);
	}
	
	/**
	 * @param rating 1-5, 0 means not rated
	 */
	@SuppressWarnings("unchecked")
	public void setRating(int rating) {
		
		this.properties.put(RATING, rating);
	}
	public String getDate() {
		return (String) this.properties.get(DATE);
	}
	/**
	 * @param date Format: 2004-07-04
	 */
	@SuppressWarnings("unchecked")
	public void setDate(String date) {
		this.properties.put(DATE, date);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof MovieRating))
			return false;
		MovieRating otherRating = (MovieRating) other;
		return getMovie().equals(otherRating.getMovie());
	}
	
	public Map<String, Object> getAsMap(){
		return this.properties;
	}
	
}
