#Building a simple recommendation engine using RESTful Web Service, Spring Boot and Aerospike
---------------------------------------------------------------------------------------------
Recommendation engines are used in applications to personalize the user experience. For example, eCommerce applications recommend products to a customer that other customers, with similar profiles, have viewed or purchased.

Spring Boot is a powerful jump-start into Spring. It allows you to build powerful applications with production grade services with little effort on your part.

Aerospike is a high available, low latency NoSQL database that scales linearly. It is an in-memory database optimized to use both DRAM and native Flash. Aerospike boasts latencies of 1 to 3 ms consistently across throughput loads on a correctly sized cluster. Aerospike also has high reliability and is ACID compliant.  Their oldest customer has many terabytes of data and has never been offline, even during Hurricane Sandy in New York City.

##What you will build
This guide will take you through creating a simple recommendation engine. This engine will use Similarity Vectors to recommend products to a user. 
The algorithm for this is quite easy, but you do require a lot of data to make it work.

Time is of the essence. Your application may be a mobile app, a web application or a Real Time Bidding application for online advertising. 
In each case you will need to go to the database and retrieve your data within 2-5ms so your application can respond within 50ms with a recommendation. You could try this any database, but Aerospike is very fast ( 1- 5ms latency) and the latency remains flat as the transaction rate grows.

You will build a simple recommendation RESTful web service with Spring Boot and Aerospike. 
The recommendation service accepts an HTTP GET request:

    http://localhost:8080/recommendation/{customer}

It responds with the following JSON array of recomendations:

    [{"expiration":130019315,"bins":{"title":"Classic Albums: Meat Loaf: Bat Out of Hell","yearOfRelease":"1999"},"generation":4},{"expiration":130019337,"bins":{"title":"Rudolph the Red-Nosed Reindeer","yearOfRelease":"1964"},"generation":4},{"expiration":130019338,"bins":{"title":"The Bad and the Beautiful","yearOfRelease":"1952"},"generation":4},{"expiration":130019384,"bins":{"title":"Jingle All the Way","yearOfRelease":"1996"},"generation":4},{"expiration":130019386,"bins":{"title":"The Killing","yearOfRelease":"1956"},"generation":4},{"expiration":130019400,"bins":{"title":"Silkwood","yearOfRelease":"1983"},"generation":4},{"expiration":130019404,"bins":{"title":"Chain of Command","yearOfRelease":"2000"},"generation":4}]
    
There are also many features added to your application out-of-the-box for managing the service in a production (or other) environment. 

##Algorithm
People act on products. People view products, kick the tires, bounce on the bed, etc; and sometimes this leads to a purchase. So there two actions people have with products: 
* View
* Purchase
An individual person will have a history of Views and Purchases.

A simple recommendation algorithm is to find another user who is similar and recommend products that the other user has viewed/purchased to this user. It is a good idea to eliminate the duplicates so that this user is only recommended products that they have not seen.

How do you do this? You need to maintain a history of a user’s Views and Purchases, e.g:

####Movie Customers
|customerId|watched|
|----|---------|
|893988|List(Map("movie-id"->"1", "rating"->3, "customer-id"->"893988", "date"->"2005-11-17"), ...|
|712664|List(Map("movie-id"->"3", "rating"->5, "customer-id"->"712664", "date"->"2004-02-01"), ...|

You also maintain a list of who purchased a product e.g.

####Movie Titles
|movieId|yearOfRelease|title|watchBy|
|-------|-------------|-----|-------|
|89|2000|Chain of Command|List(Map("movie-id"->"89", "rating"->2, "customer-id"->"712664", "date"->"2001-08-02"), ...|
|83|1983|Silkwood|List(Map("movie-id"->"83", "rating"->3, "customer-id"->"716091", "date"->"2000-01-08"), ...|
|78|1996|Jingle All the Way|List(Map("movie-id"->"78", "rating"->3, "customer-id"->"1943087", "date"->"2001-09-14"), ...|

From this data, you can see that Jane Doe and John Smith have similar purchase histories, but Albert Citizen does not. 

If Jane Doe uses your application, you could recommend to her the same things that John Smith purchased, minus the products that are common to both John and Jane. You may also prioritize which products to recommend based on a category (a similarity weight) i.e. the “dog” related products may have more relevance to Jane than the Bose Headset.

###How do you find similarity?
Similarity can be found using several algorithms, e.g. Cosine Similarity. In this example, you will use a very simple algorithm using a simple score.

###Scenario
1.	Jane Doe accesses the application
2.	Retrieve Jane’s cusromer profile
3.	Retrieve the movie Profile for each of Jane’s views, this can be a batch operation in Aerospike that retrieves a list of records in one lump
4.	For each product
	a.	Retrieve the customer profile
	b.	See if this profile is similar to Jane’s by giving it a score (using Cosine similarity)
5.	Using the customers profile with the highest similarity score, recommend the products in this user profile to Jane.

##How to build
It is easy to build a single runnable Jar with Maven

	mvn package
	
##Loading the test data
Aerospike has the ability to backup and restore the data in an entire cluster. The test data for this application is stored as am Aerospike backup file. 

Download the file at https://drive.google.com/a/aerospike.com/folderview?id=0B8luCpttpeaAVWZkYl85a2ktaXc&usp=sharing 
and restore it to your Aerospike cluster using the following command:

	asrestore -h 127.0.0.1 -p 3000 -d <back directory name>

	
##Running the package
The package is a RESTful service using Spring Boot, packaged in a runnable jar

	java -jar aerospike-recommendation-example-<version>.jar
	   
