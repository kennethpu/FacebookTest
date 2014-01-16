package com.fb;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.batch.BatchRequest.BatchRequestBuilder;
import com.restfb.types.User;

/** 
 * Inner class that builds and executes batch requests in parallel to 
 * help speed up pulling data from facebook
 */
class BatchExecutor extends Thread {
	List<User> users;
	int startIndex;
	SortedSet<Friend> topFriends;
	List<BatchRequest> batchList;

	private FacebookClient facebookClient;      // restFB stuff to perform actual facebook queries
	private int numFriends;						// Number of user's facebook friends 
	private boolean verbose = false; 			// Variables for debug outputs


		/**
		 * Default constructor
		 * @param u : list of facebook friends 
		 * @param s : start index within u to build batch from
		 * @param tf : set of friends to store results in
		 */
		public BatchExecutor(List<User> users, int startIndex, SortedSet<Friend> topFriends, int numFriends, FacebookClient facebookClient) {
			this.users = users;
			this.startIndex = startIndex;
			this.topFriends = topFriends;
			this.numFriends = numFriends;
			this.facebookClient =  facebookClient;
		}
		
		/**
		 * Build and execute batch request, then process batch response
		 */
		public void run() {
			// endIndex is either startIndex+50 or the end of the list
			int endIndex = (numFriends-startIndex > 50) ? startIndex+50 : numFriends-1;
			
			// Build batch request
			try {
				batchList = buildUserBatchRequest(users.subList(startIndex,endIndex), startIndex, endIndex);
			} catch (Exception e) {
				System.out.println("ERROR: " + e.getMessage());
				return;
			}
			
			// Print statements for debugging purposes
			if (verbose) System.out.printf("[%03d-%03d]Executing batch request...\n", startIndex, endIndex);
			
			// Execute batch request
			List<BatchResponse> batchResponses =
					  facebookClient.executeBatch((BatchRequest[]) batchList.toArray(new BatchRequest[0]));
			
			// Print statements for debugging purposes
			if (verbose) System.out.printf("[%03d-%03d]Executing batch request...DONE\n", startIndex, endIndex);
			
			// Process batch responses
			processUserBatchResponse(batchResponses, users.subList(startIndex,endIndex), topFriends, startIndex, endIndex);			
		}
		
		/**
		 * Builds a batch request for mutual friends from a given list of facebook 
		 * friends 
		 * 
		 * Parameters:
		 *   - users : list of friends
		 *   - start : start index within full friend list [FOR DEBUGGING]
		 *   - end   : end index within full friend list [FOR DEBUGGING]
		 *  
		 * Returns:
		 *   - List of BatchRequests to execute
		 */
		private List<BatchRequest> buildUserBatchRequest(List<User> users, int start, int end) throws Exception {
			// Print statements for debugging purposes
			if (verbose) {
				System.out.printf("[%03d-%03d]Building batch request...\n", start, end);
			}
			
			// Initialize data structure for storing BatchRequests
			List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
			
			// Check that batch is not too large
			int batchSize = users.size();
			if (batchSize > 50) {
				throw new Exception("Too many requests, maximum batch size is 50.");
			}
			
			// Build BatchRequests and add them to list
			for (int i = 0; i < batchSize; i++) {
				BatchRequest request = new BatchRequestBuilder("me/mutualfriends/"+users.get(i).getId()).build();
				batchRequests.add(request);
			}
			
			// Print statements for debugging purposes
			if (verbose) System.out.printf("[%03d-%03d]Building batch request...DONE\n", start, end);
			
			return batchRequests;
		}
		
		/**
		 * Processes batch responses received from facebook and adds them to 
		 * a given set
		 * 
		 * Parameters:
		 *   - bachResponses : batch responses received from facebook
		 *   - friends       : list of friends
		 *   - friendSet     : set containing friend entries 
		 *   - start         : start index within full friend list [FOR DEBUGGING]
		 *   - end           : end index within full friend list [FOR DEBUGGING]
		 */
		private void processUserBatchResponse(List<BatchResponse> batchResponses, List<User> friends, SortedSet<Friend> friendSet, int start, int end) {
			// Print statements for debugging purposes
			if (verbose) System.out.printf("[%03d-%03d]Processing batch response...\n", start, end);
			
			// Process batch responses
			BatchResponse fResponse;
			int batchSize = batchResponses.size();
			for (int i=0; i<batchSize; i++) {
				fResponse = batchResponses.get(i);
				if (fResponse.getCode() != 200) {
					System.out.println("ERROR: Batch request failed: " + fResponse);
					continue;
				}
				Connection<User> mutualFriends = 
						new Connection<User>(facebookClient, fResponse.getBody(), User.class);
				friendSet.add(new Friend(friends.get(i).getName(), mutualFriends.getData().size()));
			}
			
			// Print statements for debugging purposes
			if (verbose) System.out.printf("[%03d-%03d]Processing batch response...DONE\n", start, end);
	}
}