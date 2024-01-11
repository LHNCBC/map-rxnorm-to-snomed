package gov.nih.nlm.mor.RxNorm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

public class RxNormSBD implements java.io.Serializable {
	
	private static final long serialVersionUID = 3938462378653449971L;
	
	private Integer rxcui = null;
	private String name = null;
	private String synonym = null;
	private String language = null;
	private String status = "\"unnamed\"";
	private Vector<RxNormBoss> vBoss = new Vector<RxNormBoss>();
	private ArrayList<String> scdRxcuis = new ArrayList<String>();
	private Set<RxNormSBD> remappedSbds = new HashSet<>();
	private Set<RxNormSBD> quantifiedSbds = new HashSet<>();
	private Set<Property> properties = new HashSet<>();	
	private Set<NDC> ndcSet = new HashSet<>();
	private Vector<RxNormDoseForm> vDoseForm = new Vector<RxNormDoseForm>();
	private ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	RestUrl restUrl = new RestUrl();
	
	public RxNormSBD(JSONObject jsonObject) {
//		"tty": "SBD",
//		"conceptProperties": [
//		{
//		"rxcui": "1486532",
//		"name": "sodium fluoride 0.5 MG/ML Mouthwash [CTX4 Rinse]",
//		"synonym": "CTX4 Rinse 0.05 % Mouthwash",
//		"tty": "SBD",
//		"language": "ENG",
//		"suppress": "N",
//		"umlscui": ""
//		},		
		this.rxcui = Integer.valueOf(jsonObject.getString("rxcui"));
		this.name = jsonObject.getString("name");
		this.synonym = jsonObject.getString("synonym");
		this.language = jsonObject.getString("language");				
	}
	
	public RxNormSBD(Integer rxcui, String name) {
		this.rxcui = Integer.valueOf(rxcui);
		this.name = name;
//		this.synonym = jsonObject.getString("synonym");
//		this.language = jsonObject.getString("language");				
	}
	
	public RxNormSBD(Integer rxcui, String name, RxNormSCD scd) {
		
	}
	
	public RxNormSBD(String cui, String name, String historycall, HashMap<RxNormDoseForm, SnomedDFPair> rxNormDoseForm2SnomedDFPair) {
		boolean active = false;
		this.name = name;
		this.rxcui = Integer.valueOf(cui);
		
		JSONObject historyObject = null;
		JSONObject allProperties = null;
		JSONObject ndc = null;		
		
		try {
			historyObject = getresult(historycall, null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			allProperties = getresult(restUrl.getRestUrl() + "/REST/rxcui/" + cui + "/allProperties.json?prop=all", null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		if(historyObject != null ) {
			JSONObject rxcuiStatusHistory = null;
			if(!historyObject.isNull("rxcuiStatusHistory")) {
				rxcuiStatusHistory = historyObject.getJSONObject("rxcuiStatusHistory");
				if(rxcuiStatusHistory.has("metaData")) {
					JSONObject metaData = rxcuiStatusHistory.getJSONObject("metaData");
					this.status = metaData.getString("status");
					active = status.equalsIgnoreCase("active") ? true : false;
				}
				if(!active) {
					if(rxcuiStatusHistory.has("attributes")) {
						JSONObject attributes = rxcuiStatusHistory.getJSONObject("attributes");
						//this isn't an array! we need to check each key-value pair.
						if(attributes.has("tty")) { //we need you the most!
							String ttyValue = attributes.getString("tty");
							addProperty("TTY", ttyValue, "ATTRIBUTES");							
						}
//We shall see.						
//						if(attributes.has("isMultipleIngredient")) {
//							addProperty(propertyName, propertyValue, "ATTRIBUTES");							
//						}
//						if(attributes.has("isBranded" )) {
//							addProperty(propertyName, propertyValue, "ATTRIBUTES");							
//						}
					}					
				}
				if(rxcuiStatusHistory.has("definitionalFeatures")) {
					JSONObject definitionalFeatures = rxcuiStatusHistory.getJSONObject("definitionalFeatures");
					if(definitionalFeatures.has("doseFormConcept")) {
						JSONArray doseFormConcept = definitionalFeatures.getJSONArray("doseFormConcept");
						for(int j=0; j < doseFormConcept.length(); j++) {
							JSONObject dfObj = doseFormConcept.getJSONObject(j);
							RxNormDoseForm df = new RxNormDoseForm(Integer.valueOf(dfObj.getString("doseFormRxcui")), dfObj.getString("doseFormName"));
							this.vDoseForm.add(df);
						}
					}					
					if(definitionalFeatures.has("ingredientAndStrength")) {
						JSONArray ingredientAndStrength = definitionalFeatures.getJSONArray("ingredientAndStrength");
						for(int i=0; i < ingredientAndStrength.length(); i++) {
							JSONObject b = ingredientAndStrength.getJSONObject(i);
							//RxNormBoss boss = new RxNormBoss(true, vDoseForm, Integer.valueOf(cui), b);
							//Mar-06-2023
							RxNormBoss boss = new RxNormBoss(true, vDoseForm, Integer.valueOf(cui), b, rxNormDoseForm2SnomedDFPair);							
							vBoss.add(boss);
						}
					}
				}
				boolean hasDerived = false;
 				if(rxcuiStatusHistory.has("derivedConcepts")) {
					JSONObject derivedConcepts = rxcuiStatusHistory.getJSONObject("derivedConcepts");
					if(derivedConcepts.has("scdConcept")) {
						JSONObject scdConcept = derivedConcepts.getJSONObject("scdConcept");
						String scdcui = scdConcept.getString("scdConceptRxcui");
					//	System.out.println("SBD " + this.name + " has SCD " + scdcui );							
						this.scdRxcuis.add(scdcui);		
						hasDerived = true;
					}
					if(derivedConcepts.has("remappedConcept")) {
						JSONArray remappedConcepts = derivedConcepts.getJSONArray("remappedConcept");
						for(int i=0; i < remappedConcepts.length(); i++) {
							JSONObject remappedConcept = remappedConcepts.getJSONObject(i);
							String remappedCui = remappedConcept.getString("remappedRxCui");
							String tty = remappedConcept.getString("remappedTTY");
							if(tty.equalsIgnoreCase("SCD"))	this.scdRxcuis.add(remappedCui);
							else if(tty.equalsIgnoreCase("SBD")) {
								String remappedName = remappedConcept.getString("remappedName");
								String remappedHistoryCall = historycall.replace("/" + cui + "/", "/" + remappedCui + "/");
								RxNormSBD remappedSBD = new RxNormSBD(remappedCui, remappedName, remappedHistoryCall, rxNormDoseForm2SnomedDFPair);
								remappedSbds.add(remappedSBD);
							}
							hasDerived = true;
						}
					}
					if(derivedConcepts.has("quantifiedConcept")) {
						JSONArray quantifiedConcepts = derivedConcepts.getJSONArray("quantifiedConcept");
						for(int i=0; i < quantifiedConcepts.length(); i++) {
							JSONObject quantifiedConcept = quantifiedConcepts.getJSONObject(i);
							String quantifiedRxCui = quantifiedConcept.getString("quantifiedRxcui");
							String quantifiedName = quantifiedConcept.getString("quantifiedName");
							String tty = quantifiedConcept.getString("quantifiedTTY");
							// String quantifiedActive = quantifiedConcept.getString("quantifiedActive"); - we're getting everything
							if(tty.equalsIgnoreCase("SCD"))	this.scdRxcuis.add(quantifiedRxCui);
							else if(tty.equalsIgnoreCase("SBD")) {
								String quantifiedHistoryCall = historycall.replace("/" + cui + "/", "/" + quantifiedRxCui + "/");
								RxNormSBD remappedSBD = new RxNormSBD(quantifiedRxCui, quantifiedName, quantifiedHistoryCall, rxNormDoseForm2SnomedDFPair);
								quantifiedSbds.add(remappedSBD);
							}
							hasDerived = true;
						}
					}					
					if(!hasDerived) {
						System.err.println(cui + "\t" + this.status + "\t" + this.name); // + "\thas no derivedConcept");
					}
				}
			}
		}
		
		
		if( allProperties != null) {
			if(allProperties.has("propConceptGroup")) {
				JSONObject propConceptGroup = allProperties.getJSONObject("propConceptGroup");		
	//this was to catch when studying anything that wasn't Active
	//I found anything Obsolete doesn't have Properties, though they have ATTRIBUTES (not visible in RxNav webapp but there nonetheless)
	//these were the definitionalFeatures as properties. We will probably want to squelch these as annotations since they reside in logical definitions
	//of the clinical drug classes.
	//
	//			try {
	//				propConceptGroup = (JSONObject) allProperties.get("propConceptGroup");
	//			} catch(Exception e) {
	////				System.out.println("Failed at rxcui: " + rxcui + " ---> " + name);
	////				e.printStackTrace();
	//			}		
				if( propConceptGroup != null && !propConceptGroup.isNull("propConcept") ) {
					JSONArray propConceptArray = (JSONArray) propConceptGroup.get("propConcept");
					for(int i=0; i < propConceptArray.length(); i++) {
						JSONObject b = (JSONObject) propConceptArray.get(i);
						String category = b.getString("propCategory");					
						String propertyName = b.getString("propName");
						String propertyValue = b.getString("propValue");
						addProperty(propertyName, propertyValue, category);
					}
				}
			}
		}
		
		try {
			ndc = getresult(restUrl.getRestUrl() + "/REST/rxcui/" + rxcui + "/allhistoricalndcs.json?history=1", null); //most efficient
		} catch(Exception e) {
			System.out.println("Failed to getresult ndcs: " + rxcui);
			e.printStackTrace();
		}
		
		if( ndc != null && ndc.has("historicalNdcConcept")) {
			//			example, 204913 - Direct only for now		
			//					{
			//						"historicalNdcConcept": {
			//						"historicalNdcTime": [
			//						{
			//						"status": "direct",
			//						"rxcui": "204913",
			//						"ndcTime": [
			//						{
			//						"ndc": [
			//						"00096121000"
			//						],
			//						"startDate": "201510",
			//						"endDate": "202212"
			//						},
			//						{
			//						"ndc": [
			//						"00395277516"
			//						],
			//						"startDate": "201205",
			//						"endDate": "202212"
			//						},			
			JSONObject historicalNdcConcept = (JSONObject) ndc.get("historicalNdcConcept");
			if( !historicalNdcConcept.isNull("historicalNdcTime") ) {
				JSONArray historicalNdcTime = historicalNdcConcept.getJSONArray("historicalNdcTime");
				JSONObject historicalNdcTimeDirect = historicalNdcTime.getJSONObject(0);
				String status = historicalNdcTimeDirect.getString("status");
				JSONArray ndcTime = historicalNdcTimeDirect.getJSONArray("ndcTime");
				for(int j=0; j < ndcTime.length(); j++) {

					JSONObject ndcTimeElement = ndcTime.getJSONObject(j);

					if(ndcTimeElement.has("ndc")) {
						JSONArray ndcArray =  ndcTimeElement.getJSONArray("ndc");
						if(ndcArray.length() == 1) {
							NDC ndcObject = new NDC();

							ndcObject.setNdc(ndcArray.getString(0));
							ndcObject.setStatus(status);
							ndcObject.setStartDate(ndcTimeElement.getString("startDate"));
							ndcObject.setEndDate(ndcTimeElement.getString("endDate"));
							ndcSet.add(ndcObject);
						}
						else {
							System.out.println(rxcui + ": a direct ndcTime without ndc array of 1");
						}
					}
				}				
			}
		}		
	}	
	
	public RxNormSBD() {
		
	}
	
	private void addProperty(String propertyName, String propertyValue, String category) {
		Property p = new Property(propertyName, propertyName, propertyValue, category); //unique identifiers will be declared during generation
		properties.add(p);		
	}
	
	public RxNormBoss buildBoss(Integer rxcui, JSONObject b) {
		JSONObject result = b;
		RxNormBoss boss = null;
		
		String baserxcuiString = result.get("baseRxcui").toString();
		String basename = result.get("baseName").toString();
		String bossrxcuiString = result.get("bossRxcui").toString();
		String bossname = result.get("bossName").toString();
		String nvString = result.get("numeratorValue").toString();
		String nuString = result.get("numeratorUnit").toString();
		String dvString = result.get("denominatorValue").toString();
		String duString = result.get("denominatorUnit").toString();	
		String actIngredRxcuiString = result.get("activeIngredientRxcui").toString().isEmpty() ? "-1" : result.get("activeIngredientRxcui").toString(); 
		String actIngredName = result.get("activeIngredientName").toString().isEmpty() ? "null" : result.get("activeIngredientName").toString();
		
		Integer baserxcui = null;
		if(!baserxcuiString.isEmpty()) {
			baserxcui = new Integer(baserxcuiString);			
		}
		
		Integer actIngredRxcui = null;
		if( !actIngredRxcuiString.isEmpty() ) {
			actIngredRxcui = new Integer(actIngredRxcuiString);
		}
		
		Integer bossrxcui = (!bossrxcuiString.isEmpty()) ? new Integer(bossrxcuiString) : new Integer(-1); 
		
		Double nv = null;
		Double dv = null;
		if(!nvString.isEmpty() && !NumberUtils.isParsable(nvString)) {
			System.err.println(rxcui + "\t" + this.name + "\t" + nvString + "\t" + status + "\tNaN" );
		} else {
//			nv = (!nvString.isEmpty()) ? Double.valueOf(nvString) : Double.valueOf("1");
			nv = Double.valueOf("1");
		}
		if(!dvString.isEmpty() && !NumberUtils.isParsable(dvString)) {
			System.err.println(rxcui + "\t" + this.name + "\t" + dvString + "\t" + status + "\tNaN" );
		} else {
//			dv = (!dvString.isEmpty()) ? Double.valueOf(dvString) : Double.valueOf("1"); //If the dv is empty, can we assume this is 1?
			dv = Double.valueOf("1");
		}
		
		String nu = (!nuString.isEmpty()) ? new String(nuString) : "1";

		// unit of presentation is set later in the program, so this is a bad thing to do
//		String du = (!duString.isEmpty()) ? new String(duString) : this.unitOfPresentationName;
		String du = (!duString.isEmpty()) ? new String(duString) : "1";
		
//TODO: The Active implementation requires allRelated to return the DF(s)
//Since they are not returned with Obsolete and Not Active apply from historystatus possibly 
		try {
			boss = new RxNormBoss(new Integer(rxcui), baserxcui, basename, bossrxcui, bossname, nv, nu, dv, du, actIngredRxcui, actIngredName, this.name, this.vDoseForm.get(0).getName());
		} catch(Exception e) {
//			System.out.println("Building BoSS for rxcui: " + this.rxcui.toString());
		}
		return boss; 
	}	

	public Integer getRxcui() {
		return rxcui;
	}

	public void setRxcui(Integer rxcui) {
		this.rxcui = rxcui;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSynonym() {
		return synonym;
	}

	public void setSynonym(String synonym) {
		this.synonym = synonym;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Vector<RxNormBoss> getvBoss() {
		return vBoss;
	}

	public void setvBoss(Vector<RxNormBoss> vBoss) {
		this.vBoss = vBoss;
	}

	public ArrayList<String> getScdRxcuis() {
		return scdRxcuis;
	}

	public void setScdRxcuis(ArrayList<String> scdRxcuis) {
		this.scdRxcuis = scdRxcuis;
	}

	public Vector<RxNormDoseForm> getvDoseForm() {
		return vDoseForm;
	}

	public void setvDoseForm(Vector<RxNormDoseForm> vDoseForm) {
		this.vDoseForm = vDoseForm;
	}
	
	public Set<Property> getProperties() {
		return this.properties;
	}

	public Set<NDC> getNdcSet() {
		return ndcSet;
	}

	public Set<RxNormSBD> getRemappedSbds() {
		return remappedSbds;
	}
	
	public Set<String> getRemappedSbdCuis() {
		Set<String> cuis = new HashSet<>();
		for(RxNormSBD sbd : remappedSbds) {
			String cui = sbd.getRxcui().toString();
			cuis.add(cui);
		}
		return cuis;
	}	
	
	public Set<RxNormSBD> getQuantifiedSbds() {
		return quantifiedSbds;
	}	
	
	public Set<String> getQuantifiedSbdCuis() {
		Set<String> cuis = new HashSet<>();
		for(RxNormSBD sbd : quantifiedSbds) {
			String cui = sbd.getRxcui().toString();
			cuis.add(cui);
		}
		return cuis;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
//	public static JSONObject getresult(String URLtoRead) throws IOException {
//		URL url;
//		HttpsURLConnection connexion;
//		BufferedReader reader;
//		
//		String line;
//		String result="";
//		url= new URL(URLtoRead);
//	
//		connexion= (HttpsURLConnection) url.openConnection();
//		connexion.setRequestMethod("GET");
//		reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));	
//		while ((line =reader.readLine())!=null) {
//			result += line;
//			
//		}
//		
//		JSONObject json = new JSONObject(result);
//		return json;
//	}	
	
//	public JSONObject getresult(String URLtoRead) throws IOException {
//		URL url;
//		HttpURLConnection connexion;
//		BufferedReader reader;
//
//		String line;
//		String result="";
//		url= new URL(URLtoRead);
//
//		connexion= (HttpURLConnection) url.openConnection();
//		connexion.setRequestMethod("GET");
//		reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));	
//		while ((line =reader.readLine())!=null) {
//			result += line;
//
//		}
//
//		JSONObject json = new JSONObject(result);
//		return json;
//	}	
	
	public JSONObject getresult(String URLtoRead, ExponentialBackoff b) throws IOException, InterruptedException {
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

	@Override
	public int hashCode() {
		return Objects.hash(language, name, rxcui, synonym);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RxNormSBD other = (RxNormSBD) obj;
		return Objects.equals(language, other.language) && Objects.equals(name, other.name)
				&& Objects.equals(rxcui, other.rxcui) && Objects.equals(synonym, other.synonym);
	}

}
