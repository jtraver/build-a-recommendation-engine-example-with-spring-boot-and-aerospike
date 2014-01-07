#Building a simple recommendation engine use RESTful Web Service, Spring Boot and Aerospike
===========================================================================================
Spring Boot is a powerful jump start into Spring. It allows you to build powerful applications with production grade services with little effort on your part. 

Aerospike is a high available, low latency NoSQL data base that scales linearly. It is an in-memory database optimized to use both DRAM and native Flash. Aerospike boasts latencies of 1 to 3 ms consistently across throughput loads on a correctly sized cluster. Aerospike also has high reliability and is ACID compliant.  Their oldest customer has many terabytes of data and has never been offline, even during Hurricane Sandy in New York City.

##What you will build
This guide will take you through creating a simple recommendation engine. This engine will use Similarity Vectors to recommend products to a user. 
The algorithm for this is quite easy, but you do require a lot of data to make it work.

Time is of the essence. Your application may be a mobile app, a web application or a Real Time Bidding application for online advertising. 
In each case you will need to go to the database and retrieve your data within 2-5ms so your application can respond within 50ms with a recommendation. You could try this any database, but Aerospike is very fast ( 1- 5ms latency) and the latency remains flat as the transaction rate grows.

You will build a simple recommendation RESTful web service with Spring Boot and Aerospike. 
The recommendation service accepts an HTTP GET request:

    http://localhost:8080/recommendation/{user}

It responds with the following JSON:

    {
    }

