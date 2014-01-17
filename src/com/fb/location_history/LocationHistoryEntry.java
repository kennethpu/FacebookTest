package com.fb.location_history;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;

import com.restfb.types.*;

public class LocationHistoryEntry implements Comparable<LocationHistoryEntry> {
	/*
	 * Instance Variables
	 */
	private Date date;
	private Place place;
	private String name;
	private String city;
	private String state;
	private String country;
	private String time;
	private double latitude;
	private double longitude;
	
	/*
	 * Constructors
	 */
	public LocationHistoryEntry(){}
	public LocationHistoryEntry(Date d, Place p) {
		date = d;
		place = p;
		Location location = p.getLocation();
		name = p.getName();
		city = (location.getCity() != null && !location.getCity().equals("")) ? " " + location.getCity() : "";
		state = (location.getState() != null && !location.getState().equals("")) ? " " + location.getState() : "";
		//country = (location.getCountry() != null && !location.getCountry().equals("")) ? " " + location.getCountry() : "";
		country = location.getCountry();
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		time = date.toString();
	}
	
	/*
	 *	Comparison Definition 
	 */
	public int compareTo(LocationHistoryEntry arg) {
		// LoactionHistoryEntries are sorted by date in reverse order (earlier -> 'greater')
		return arg.date.compareTo(this.date);
	}
	
	/*
	 * Getter Methods
	 */
	public Date getDate() {
		return date;
	}
	public String getName() {
		return name;
	}
	public String getCity() {
		return city;
	}
	public String getState() {
		return state;
	}
	public String getCountry() {
		return country;
	}
	public String getTime() {
		return time;
	}
	public double getLatitude() {
		return latitude;
	}
	public double getLongitude() {
		return longitude;
	}
}
