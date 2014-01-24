package com.aerospike.recommendation.dataimport.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WatchedRated implements Map, Comparable<WatchedRated>
{
	
	/**
	 * 
	 */
	public static final String DATE = "date";
	public static final String RATING = "rating";
	public static final String CUSTOMER_ID = "customer-id";
	public static final String MOVIE_ID = "movie-id";
	
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	public WatchedRated() {
		super();
	}
	public WatchedRated(Map<String, Object> map) {
		this();
		this.properties = map;
	}
	@SuppressWarnings("unchecked")
	public WatchedRated(String movie, String customerID, long rating, String date) {
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

	public long getRating() {
		return (Long) this.properties.get(RATING);
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
		if (!(other instanceof WatchedRated))
			return false;
		WatchedRated otherRating = (WatchedRated) other;
		return getMovie().equals(otherRating.getMovie());
	}
	
	public Map<String, Object> getAsMap(){
		return this.properties;
	}
	
	@Override
	public String toString() {
		return "WatchedRated [" +  this.getMovie() + "," 
				+ this.getCustomerID() + "," 
				+ this.getRating() + "," 
				+ this.getDate() +"]";
	}
	@Override
	public void clear() {
		this.properties.clear();
		
	}
	@Override
	public boolean containsKey(Object key) {
		return this.properties.containsKey(key);
	}
	@Override
	public boolean containsValue(Object value) {
		return this.properties.containsValue(value);
	}
	@Override
	public Set entrySet() {
		return this.properties.entrySet();
	}
	@Override
	public Object get(Object key) {
		return this.properties.get(key);
	}
	@Override
	public boolean isEmpty() {
		return this.properties.isEmpty();
	}
	@Override
	public Set keySet() {
		return this.properties.keySet();
	}
	@Override
	public Object put(Object key, Object value) {
		return this.properties.put((String) key, value);
	}
	@Override
	public void putAll(Map map) {
		this.properties.putAll(map);
		
	}
	@Override
	public Object remove(Object key) {
		return this.properties.remove(key);
	}
	@Override
	public int size() {
		return this.properties.size();
	}
	@Override
	public Collection values() {
		return this.properties.values();
	}
	@Override
	public int compareTo(WatchedRated other) {
		return this.getDate().compareTo(other.getDate());
	}
	
}
