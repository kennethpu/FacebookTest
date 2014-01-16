package com.fb;


import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.restfb.exception.FacebookOAuthException;


public class ExecuteSearch {
	/**
	 * Program Entry Point
	 */
	public static void main(String[] args) {
		//Site to get auth_token https://developers.facebook.com/tools/explorer
		String MY_AUTH_TOKEN = "CAACEdEose0cBAEksh5jYBQ2SZAZC3bhKN53J1ZB3bad3FvRnIqWlPmZAn7MDpRk4d17paRQfaOrGU2IXPo4PKF6X45ETZBNg19hqTeZCDXgZAArssGvPc3sE2rh6DrVRRmBpqdQ9z0s31haMUvZCGa1Qp4VoW6UpQvAnEDdsCQr59ZC5XnDoP8qifZBYObVBL0HZBrK2lhj242zJAZDZD";
		FacebookSearch fbSearch = new FacebookSearch(MY_AUTH_TOKEN);
		BufferedReader br = new BufferedReader( new InputStreamReader(System.in));
		boolean loop = true;
		boolean inputValid = false;
		int sel = 0;
		
		while(loop) {
			System.out.println("Select search option:"); 
			System.out.println("  0: mostMutualFriends()"); 
			System.out.println("  1: getFriendEducation()"); 
			System.out.println("  2: getFriendStatuses()"); 
			System.out.println("  3: getLocationHistory()"); 
			System.out.println("  4: getFriendList()");
			
			while(!inputValid) {
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
				}
				break;
			case 1:
				try {
					fbSearch.getFriendEducation();
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
				}
				break;
			case 2:
				try {
					fbSearch.getFriendStatuses(3);
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
				}
				break;
			case 3:
				try {
					fbSearch.getLocationHistory();
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
				}
				break;
			case 4:
				try { 
					fbSearch.getFriendList();
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
				}
				break;
			default:
				try { 
					fbSearch.mostMutualFriends(0);
				} catch (FacebookOAuthException e) {
					System.out.println("ERROR: Authorization Token expired! Must request new token.");
				}
				break;
			}
			
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
