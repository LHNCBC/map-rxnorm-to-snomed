package gov.nih.nlm.mor.util;

public class RestUrl {
	public String restUrl = System.getProperty("restUrl");
	
	public RestUrl() {
		if(restUrl == null) {
			setRestUrl("https://rxnav.nlm.nih.gov");
		}
	}
	
	public void setRestUrl(String url) {
		this.restUrl = url;
	}
	
	public String getRestUrl() {
		return this.restUrl;
	}
	
}
