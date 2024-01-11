package definitionalFeaturesAnalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.OWLClass;

public class GetRxNormCVX {

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
		  SourceForSCD = getresult(urltest);
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
	public static JSONObject getresult(String URLtoRead) throws IOException {

	    URL url;

	    HttpsURLConnection connexion;

	    BufferedReader reader;

	   

	    String line;

	    String result="";

	    url= new URL(URLtoRead);

	    
	    connexion= (HttpsURLConnection) url.openConnection();
	    connexion.setRequestMethod("GET");
	     reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));
	    while ((line =reader.readLine())!=null) {

	           result += line;

	          

	    }

	  
	    JSONObject json = new JSONObject(result);

	    return json;

	}



}
