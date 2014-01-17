package com.fb.common;

import java.util.List;
import java.util.SortedSet;

import com.restfb.FacebookClient;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.types.User;

public abstract class SearchThread<T> extends Thread {
	public FacebookClient facebookClient;
	public User user;
	public boolean verbose = true; 
	public List<BatchRequest> batchList;
	public SortedSet<T> outputSet;
	
	public abstract List<BatchRequest> buildBatchRequest() throws Exception;
	
	public abstract void processBatchResponse(List<BatchResponse> batchResponses, SortedSet<T> outputSet);
		
}	
