package gov.nih.nlm.mor.auditmap.definitionalFeaturesAnalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.OWLClass;

import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

public class GetRxNormCVX {
	
	public static RestUrl restUrl = new RestUrl();
	private static ExponentialBackoff exponentialBackoff = new ExponentialBackoff();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
//		OWLClass code=null;
//		System.out.println(getRxCodesCVXVaccine(code));
		
	}
	

	public static Boolean getRxCodesCVXVaccine(OWLClass code) {

	    Set<String> codes = new HashSet<>();
	    Boolean a= false;

	    JSONObject SourceForSCD = null;
	  
	    String RxNormCuiString = code.getIRI().getShortForm().replace("Rx", " ").trim();

	    try {
	    	//String urltest=restUrl.getRestUrl() + "/REST/rxcui/1099941/allProperties.json?prop=all";
	    	String urltest=restUrl.getRestUrl() + "/REST/rxcui/"+RxNormCuiString+"/allProperties.json?prop=all";
		  SourceForSCD = getresult(urltest, null);
	   // System.out.println(SourceForSCD);
	        }

	    catch(Exception e) {

	           System.out.println("Unable to fetch dose form codes for Rxcui: " + RxNormCuiString);

	    }

	   

	    if( !SourceForSCD.isNull("propConceptGroup") ) {

	           JSONObject propConceptGroup = (JSONObject) SourceForSCD.get("propConceptGroup");

	           if( !propConceptGroup.isNull("propConcept") ) {

	        	   JSONArray propConcept = (JSONArray) propConceptGroup.get("propConcept");
	        	   for(int i=0; i < propConcept.length(); i++ ) {
		        		  JSONObject listelent= (JSONObject) propConcept.get(i);
		        		 	  	if(!listelent.isNull("propCategory")) {
		        		 	  	
		        				  		String rxString = listelent.get("propCategory").toString();
		        				  		//System.out.println("rxString "+rxString);
		        				  		if(rxString.equals("SOURCES")) {
		        				  			String Source = listelent.get("propValue").toString();
		        				  			//System.out.println(Source);
		        				  			if(Source.equals("HL7 Clinical Vaccine Formulation")) {
		        				  				
		        				  				a=true;
		        				  				break;
		        				  			}
		        				  			
		        				  		}

		        				  	
		        				  		codes.add(rxString);
		        				  	}
		        			
		        			  
		        		  
		        	   }
             

	           }

	    }

	   

	    return a;

	}
//	public static JSONObject getresult(String URLtoRead) throws IOException {
//
//	    URL url;
//
//	    HttpsURLConnection connexion;
//
//	    BufferedReader reader;
//
//	   
//
//	    String line;
//
//	    String result="";
//
//	    url= new URL(URLtoRead);
//
//	    
//	    connexion= (HttpsURLConnection) url.openConnection();
//	    connexion.setRequestMethod("GET");
//	     reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));
//	    while ((line =reader.readLine())!=null) {
//
//	           result += line;
//
//	          
//
//	    }
//
//	  
//	    JSONObject json = new JSONObject(result);
//
//	    return json;
//
//	}
	
	public static JSONObject getresult(String URLtoRead, ExponentialBackoff b) throws IOException, InterruptedException {
		URL url;
		HttpURLConnection connexion;
		BufferedReader reader;
		 
		exponentialBackoff = b == null ? new ExponentialBackoff() : b; 
		
		String line;
		String result="";
		url= new URL(URLtoRead);

		connexion= (HttpURLConnection) url.openConnection();
		connexion.setRequestMethod("GET");
		try {
			reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));
		} catch(Exception e) {
			System.err.println("Network hiccup, retrying the call...");
			exponentialBackoff.multiply();
			if(exponentialBackoff.getTime() <= exponentialBackoff.getMax()) {
				Thread.sleep(exponentialBackoff.getTime());
				return getresult(URLtoRead, exponentialBackoff);
			} else {
				System.err.println("Tired of retrying the call. Throwing an error. (This may impact the final product. Is the API up? Waited more than" + exponentialBackoff.getTime() / 1000 + " seconds and haven't violated the ToS.)");
				throw e;
			}
		}
		while ((line =reader.readLine())!=null) {
			result += line;

		}

		JSONObject json = new JSONObject(result);
		return json;
	}



}
