package com.fb.common;

import java.util.List;
import java.util.SortedSet;

import com.restfb.FacebookClient;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.types.User;

/**
 * Template class that all future search threads should extend/follow
 */
public abstract class SearchThread<T> extends Thread {
	/* 
	 * Instance Variables
	 */
	public FacebookClient facebookClient;
	public User user;
	public boolean verbose = true; 
	public List<BatchRequest> batchList;
	public SortedSet<T> outputSet;
	
	/**
	 * Build a Batch Request to send to Facebook
	 */
	public abstract List<BatchRequest> buildBatchRequest() throws Exception;
	
	/**
	 * Process the Batch Response receieved from Facebook
	 */
	public abstract void processBatchResponse(List<BatchResponse> batchResponses, SortedSet<T> outputSet);
		
}	
