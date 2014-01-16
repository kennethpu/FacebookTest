package com.fb.common;

import java.util.List;

import com.restfb.FacebookClient;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.types.User;

public abstract class SearchThread extends Thread {
	public FacebookClient facebookClient;
	public User user;
	public boolean verbose = true; 
	public List<BatchRequest> batchList;
	
	public abstract List<BatchRequest> buildBatchRequest() throws Exception;
	
	public abstract void processBatchResponse(List<BatchResponse> batchResponses);
		
}	
