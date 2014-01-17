package com.fb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.User;

public class ExecuteSearch {
	
	public static HashMap<String, String> friendsList = new HashMap<String, String>();
	
	/**
	 * Program Entry Point
	 */
	public static void main(String[] args) {
		//Site to get auth_token https://developers.facebook.com/tools/explorer
		String MY_AUTH_TOKEN = "CAACEdEose0cBAFuJEuLQ5zknLzfucV36OEhzfe1bGwntJmd3bRJXGZABs55U0pq9cq3yvYFTrFXAsSP4rXN5QDdXx2a3RwFAOVSRFnZAfR9pOry267gXYaBA7Sf8VroodHqJcrcBGUnXrCkVldf1SnZCQyYgHHeCZBkOgtociuedS5g5IddGUkSuCc5ZBRpYZD";
		FacebookClient facebookClient = new DefaultFacebookClient(MY_AUTH_TOKEN);
		FacebookSearch fbSearch = new FacebookSearch(facebookClient);
		
		HashMap<String, String> tempFriendList= new HashMap<String, String>();
		
		Connection<User> myFriends = null;
		User me = null;
		// Get friends data
		try {
			myFriends = facebookClient.fetchConnection(
					"me/friends", User.class);
			me = facebookClient.fetchObject("me", User.class);
		} catch (FacebookOAuthException e) {
			System.out.println("ERROR: Authorization Token expired! Must request new token.");
			return;
		}
		
		tempFriendList.put(me.getName(), me.getId());
		
		int numFriends = myFriends.getData().size();
		// Print statements for debugging purposes
		System.out.println("Friend Count: " + numFriends);
		for (User f : myFriends.getData()) {
			tempFriendList.put(f.getName(), f.getId());
		}
		
		friendsList = tempFriendList;
		
		BufferedReader br = new BufferedReader( new InputStreamReader(System.in));
		boolean loop = true;
		boolean inputValid = false;
		int sel = 0;
		
		while(loop) {
			inputValid = false;

			while(!inputValid) {
				System.out.println("Select search option:"); 
				System.out.println("  0: mostMutualFriends()"); 
				System.out.println("  1: getFriendEducation()"); 
				System.out.println("  2: getFriendStatuses()"); 
				System.out.println("  3: getFriendLocationHistory()");
				try {
					sel = Integer.parseInt(br.readLine());
					inputValid = true;
				} catch ( Exception e) {
					System.out.println("Unrecognized input!"); 
				}
			}
			
			switch(sel) {
			case 0:
				try {
					fbSearch.mostMutualFriends(0);
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
					loop = false;
				}
				break;
			case 1:
				try {
					fbSearch.getFriendEducation();
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
					loop = false;
				}
				break;
			case 2:
				try {
					fbSearch.getFriendStatuses(3);
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
					loop = false;
				}
				break;
			case 3:
				try {
					fbSearch.getFriendLocationHistory();
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
					loop = false;
				}
				break;
			default:
				try { 
					fbSearch.mostMutualFriends(0);
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
					loop = false;
				}
				break;
			}
			if (loop) {
				System.out.println("Again? <y/n>"); 
				inputValid = false;
				String input = "n";
				while(!inputValid) {
					try {
						input = br.readLine();
						if (input.contains("y") || input.contains("n")) {
							inputValid = true;
						} else {
							System.out.println("Unrecognized input!");
						}
					} catch ( Exception e) {
						System.out.println("Unrecognized input!"); 
					}
				}
				if (input.contains("n")) {
					loop = false;
				}
			}
		}
	}
}
