package com.aerospike.recommendation.rest;

public class UserNotFound extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2689850285266341615L;
	private String userID;

	public UserNotFound() {
		super();
	}


	public UserNotFound(String user) {
		super("User not found: " + user);
		this.userID = user;
	}


	public String getUserID() {
		return userID;
	}

	
}
