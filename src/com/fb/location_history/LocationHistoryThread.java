package com.fb.location_history;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fb.Friend;
import com.fb.common.SearchThread;
import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.batch.BatchRequest.BatchRequestBuilder;
import com.restfb.types.Checkin;
import com.restfb.types.Photo;
import com.restfb.types.User;

/**
 * Thread class to perform location search operations for a single user. Multiple instances of 
 * this class can be run simultaneously for multiple users.
 */
public class LocationHistoryThread extends SearchThread<LocationHistorySetEntry>{
	
	/*
	 * Constructor
	 */
	public LocationHistoryThread(User user, FacebookClient facebookClient, SortedSet<LocationHistorySetEntry> outputSet) {
		this.user = user;
		this.facebookClient = facebookClient;
		this.outputSet = outputSet;
	}
	
	/**
	 * 
	 */
	public void run() {
		// Build batch request
		if (verbose) {
			System.out.printf("[LocationHistory] Building batch request for User: %s...\n", user.getName());
		}
		try {
			batchList = buildBatchRequest();
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			return;
		}
		if (verbose) {
			System.out.printf("[LocationHistory] Building batch request for User: %s...DONE\n", user.getName());
		}
		
		if (verbose) {
			System.out.printf("[LocationHistory] Executing batch request for User: %s...\n", user.getName());
		}
		// Execute batch request
		List<BatchResponse> batchResponses =
				  facebookClient.executeBatch((BatchRequest[]) batchList.toArray(new BatchRequest[0]));
		if (verbose) {
			System.out.printf("[LocationHistory] Executing batch request for User: %s...DONE\n", user.getName());
		}
		
		if (verbose) {
			System.out.printf("[LocationHistory] Processing batch response for User: %s...\n", user.getName());
		}
		// Process batch responses
		processBatchResponse(batchResponses, outputSet);
		if (verbose) {
			System.out.printf("[LocationHistory] Processing batch response for User: %s...DONE\n", user.getName());
		}
	}
	
	/**
	 * Builds a batch request to query for a user's checkins and photos
	 * 
	 * Returns:
	 *   - List of BatchRequests to execute
	 */
	public List<BatchRequest> buildBatchRequest() throws Exception {
		// Initialize data structure for storing BatchRequests
		List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
		
		// Get friend's checkins
		BatchRequest locationRequest = new BatchRequestBuilder(user.getId()+"/locations").parameters(Parameter.with("limit", "100")).build();
		batchRequests.add(locationRequest);
		
		return batchRequests;
	}
	
	/**
	 * Processes batch responses received from facebook and adds them to 
	 * a set
	 * 
	 * Parameters:
	 *   - batchResponses : batch responses received from facebook
	 *   - outputSet      : set to add processed entries too 
	 */
	public void processBatchResponse(List<BatchResponse> batchResponses, SortedSet<LocationHistorySetEntry> outputSet) {
		
		// Retrieve Checkin and Photo connections from batch response
		Connection<Checkin> checkins = new Connection<Checkin>(facebookClient, batchResponses.get(0).getBody(), Checkin.class);
		//Connection<Photo> photos = new Connection<Photo>(facebookClient, batchResponses.get(1).getBody(), Photo.class);
		
		// If friend has no relevant data (no hometown, current location,
		// checkins, or photos) skip
		//if (checkins.getData().size() == 0 && user.getLocation() == null && user.getHometown() == null)
		//	return;
		
		// Initialize sorted data structure for storing friend's location
		// data
		TreeSet<LocationHistoryEntry> treeSet = new TreeSet<LocationHistoryEntry>();

		// List of previously accessed Checkin lists (to address an issue
		// where fetch would occasionally loop infinitely through the lists
		List<List<Checkin>> prev_checkins_list = new ArrayList<List<Checkin>>();

		// Iterate through friend's checkins and add valid locations to set
		for (List<Checkin> checkins_list : checkins) {
			if (prev_checkins_list.contains(checkins_list))
				break;
			prev_checkins_list.add(checkins_list);

			for (Checkin checkin : checkins_list) {
				if (checkin.getPlace() == null
						|| checkin.getPlace().getLocation() == null
						|| checkin.getPlace().getLocation().getCountry() == null)
					continue;

				treeSet.add(new LocationHistoryEntry(checkin.getCreatedTime(), checkin.getPlace()));
			}
		}
		
		// Save friend's current location or 'unknown' if not found
		String curLocation = (user.getLocation() != null) ? user.getLocation().getName() : "unknown";
		
		// Save friend's hometown or 'unknown' if not found
		String hometown = (user.getHometown() != null) ? user.getHometown().getName() : "unknown";
		
		// Add processed entry to output set
		outputSet.add(new LocationHistorySetEntry(user.getName(), curLocation, hometown, treeSet));
		
	}
}
