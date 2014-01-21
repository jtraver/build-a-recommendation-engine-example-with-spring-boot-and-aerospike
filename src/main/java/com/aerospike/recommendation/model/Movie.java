package com.aerospike.recommendation.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;

/**
 * The Movie class model the data stored about a movie
 * stored in Aerospike
 * @author peter
 *
 */
public class Movie implements IRecord {
	public static final String RATING = "rating";
	public static final String WATCHED_BY = "watchedBy";
	public static final String TITLE = "title";
	public static final String YEAR_OF_RELEASE = "yearOfRelease";
	public static final String MOVIE_ID = "movieId";
	String movieId;
	String yearOfRelease;
	String title;
	int rating;
	List<WatchedRated> watchedBy;

	public Movie(String movieId){
		super();
		this.movieId = movieId;
	}
	public Movie(String movieId, String yearOfRelease, String title) {
		this(movieId);
		this.yearOfRelease = yearOfRelease;
		this.title = title;
	}
	@SuppressWarnings("unchecked")
	public Movie(String movieId, Record movieRecord) {
		this(movieId);
		fromRecord(movieRecord);
	}

	public List<WatchedRated> getWatchedBy() {
		return watchedBy;
	}
	public void setWatchedBy(List<WatchedRated> watchedBy) {
		this.watchedBy = watchedBy;
	}
	public String getMovieId() {
		return movieId;
	}
	public String getYearOfRelease() {
		return yearOfRelease;
	}
	public String getTitle() {
		return title;
	}

	public void setYearOfRelease(String yearOfRelease) {
		this.yearOfRelease = yearOfRelease;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setRating(int rating) {
		this.rating = rating;
	}
	public int getRating() {
		return rating;
	}

	@SuppressWarnings("unchecked")
	public void fromRecord(Record record) {
		this.yearOfRelease = (String) record.getValue(YEAR_OF_RELEASE);
		this.title = (String) record.getValue(TITLE);
		this.watchedBy =  (List<WatchedRated>) record.getValue(WATCHED_BY);
		if (this.watchedBy != null){
			for (WatchedRated mr : this.watchedBy){
				rating += mr.getRating();
			}
			this.rating /= watchedBy.size();
		} else {
			this.rating = 0;
		}
	}
	/**
	 * Sort the watched movies so that the latest review
	 * is at the start of the list
	 * @return
	 */
	public List<WatchedRated> sortWatched(){
		Collections.sort(this.watchedBy, Collections.reverseOrder());
		return this.watchedBy;
	}
	/* (non-Javadoc)
	 * @see com.aerospike.recommendation.model.IRecord#asBins()
	 */
	@Override
	public Bin[] asBins(){
		Bin[] bins = new Bin[]{
				new Bin(MOVIE_ID, Value.get(movieId)),
				new Bin(YEAR_OF_RELEASE, Value.get(yearOfRelease)),
				new Bin(TITLE, Value.get(title)),
				new Bin(WATCHED_BY, Value.getAsList(watchedBy))};
		new Bin(RATING, Value.get(rating));
		return bins;
	}
	/* (non-Javadoc)
	 * @see com.aerospike.recommendation.model.IRecord#getKey(java.lang.String, java.lang.String)
	 */
	@Override
	public Key getKey(String namespace, String set) throws AerospikeException{
		return new Key(namespace, set, this.movieId);
	}

	@Override
	public String toString() {
		return ("MOVIE [ID=" + this.movieId 
				+ ", title=" + this.title);
	}

}
