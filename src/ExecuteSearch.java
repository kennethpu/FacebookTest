import com.restfb.exception.FacebookOAuthException;


public class ExecuteSearch {
	/**
	 * Program Entry Point
	 */
	public static void main(String[] args) {
		//Site to get auth_token https://developers.facebook.com/tools/explorer
		String MY_AUTH_TOKEN = "CAACEdEose0cBACMxFqEnmP2ykaMu21HFaqOUwdGlJNoHCD7d7GYdj8hoA26W5X5knMCMBSzzP2WLfY9RoSEQGGfwMg50k253Hj5WNgvg2HGkOZA2y1SSf0bcKJrvPnlvzpkPOxeLVcgQiDrzaJ3JnvTx8GpzEhZAYh1TIvKx2gyDyYkNZCFVzis4G3VuKlQeXflXXAfDQZDZD";
		FacebookSearch fbSearch = new FacebookSearch(MY_AUTH_TOKEN);
		try {
			fbSearch.mostMutualFriends(0);
			// fbSearch.getFriendEducation();
			// fbSearch.getFriendStatuses(3);
			//fbSearch.getLocationHistory();
			//fbSearch.getFriendList();
		} catch (FacebookOAuthException e) {
			System.out.println("ERROR: Authorization Token expired! Must request new token.");
		}
	}
}
