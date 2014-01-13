import com.restfb.exception.FacebookOAuthException;


public class ExecuteSearch {
	/**
	 * Program Entry Point
	 */
	public static void main(String[] args) {
		//Site to get auth_token https://developers.facebook.com/tools/explorer
		String MY_AUTH_TOKEN = "CAACEdEose0cBAPAKLH3WwVVwkoNm294ZBy9vh16vYFXEaMKNgZAl1Ak9C0yMJREfdVefKsZAN7xmC4F5ZAHRml20UR6soBP1ZC9bf4mbFT1tr5n9IfyNJPcxglEC4rNXV8k9eoqekMe6IboPnnTckYBbqb0otLiesqdOg39e5VKTNc6uIDeB2oRGJnZCXpgUbJ038DKfLAywZDZD";
		FacebookSearch fbSearch = new FacebookSearch(MY_AUTH_TOKEN);
		try {
			//fbSearch.mostMutualFriends(0);
			// fbSearch.getFriendEducation();
			// fbSearch.getFriendStatuses(3);
			//fbSearch.getLocationHistory();
			fbSearch.getFriendList();
		} catch (FacebookOAuthException e) {
			System.out.println("ERROR: Authorization Token expired! Must request new token.");
		}
	}
}
