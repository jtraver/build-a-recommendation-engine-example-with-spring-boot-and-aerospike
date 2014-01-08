package com.aerospike.recommendation.rest;

public class CosineSimilarity {


	public static void main(String[] args) { 

		int vec1[] = {1,2,5,0,2,3}; 
		int vec2[] = {2,1,3,2,0,1}; 

		double cos_sim = CosineSimilarity(vec1,vec2); 
		System.out.println("Cosine Similarity="+cos_sim); 
	} 

	private static double CosineSimilarity(int[] vec1, int[] vec2) { 
		double dp = dotProduct(vec1,vec2); 
		double magnitudeA = findMagnitude(vec1); 
		double magnitudeB = findMagnitude(vec2); 
		return (dp)/(magnitudeA*magnitudeB); 
	} 

	private static double findMagnitude(int[] vec) { 
		double sum_mag=0; 
		for(int i=0;i<vec.length;i++) 
		{ 
			sum_mag = sum_mag + vec[i]*vec[i]; 
		} 
		return Math.sqrt(sum_mag); 
	} 

	private static double dotProduct(int[] vec1, int[] vec2) { 
		double sum=0; 
		for(int i=0;i<vec1.length;i++) 
		{ 
			sum = sum + vec1[i]*vec2[i]; 
		} 
		return sum; 
	} 

}
