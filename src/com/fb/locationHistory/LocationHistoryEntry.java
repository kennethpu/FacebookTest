package com.fb.locationHistory;
import java.util.Comparator;
import java.util.Date;
import com.restfb.types.*;

public class LocationHistoryEntry implements Comparator {
	public Date date;
	public Place place;
	public String name;
	public String city;
	public String state;
	public String country;
	public String time;
	
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
		time = date.toString();
	}
	
//	@Override
//	public int compare(LocationEntry a, LocationEntry b) {
//		return (b.date.compareTo(a.date));
//	}

	@Override
	public int compare(Object arg0, Object arg1) {
		LocationHistoryEntry a = (LocationHistoryEntry) arg0;
		LocationHistoryEntry b = (LocationHistoryEntry) arg1;
		return (b.date.compareTo(a.date));
	}
	 
}
