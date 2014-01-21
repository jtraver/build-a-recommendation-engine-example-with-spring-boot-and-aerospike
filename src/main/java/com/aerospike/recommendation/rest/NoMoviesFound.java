package com.aerospike.recommendation.rest;

public class NoMoviesFound extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4277831144559006904L;
	private String customerID;

	public NoMoviesFound() {
		super();
	}


	public NoMoviesFound(String user) {
		super("No movies found for customer: " + user);
		this.customerID = user;
	}


	public String getCustomerID() {
		return customerID;
	}

}
