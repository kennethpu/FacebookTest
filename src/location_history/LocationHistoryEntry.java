package location_history;
import java.util.Comparator;
import java.util.Date;
import com.restfb.types.*;

public class LocationHistoryEntry implements Comparable<LocationHistoryEntry> {
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

	//@Override
	//public int compare(LocationHistoryEntry arg0, LocationHistoryEntry arg1) {
	//	return (arg1.date.compareTo(arg0.date));
	//}
	@Override
	public int compareTo(LocationHistoryEntry arg) {
		return arg.date.compareTo(this.date);
	}
	 
}
