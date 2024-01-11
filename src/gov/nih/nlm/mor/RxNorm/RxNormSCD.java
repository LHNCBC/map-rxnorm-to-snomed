package gov.nih.nlm.mor.RxNorm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.Snomed.SnomedDefinitionTemplate;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

public class RxNormSCD implements java.io.Serializable {

	private static final long serialVersionUID = 2261607265667390359L;
	private Integer rxcui = null;
	private String name = null;
	private Long manufacturedDoseFormCode = null;
	private String manufacturedDoseFormName = null;	
	private Long unitOfPresentationCode = null;
	public SnomedDefinitionTemplate template = null;
	private String  unitOfPresentationName = null;
	private Vector<RxNormIngredient> vIngredient = new Vector<RxNormIngredient>();
	private Vector<RxNormDoseForm> vDoseForm = new Vector<RxNormDoseForm>();
	private Vector<RxNormBoss> vBoss = new Vector<RxNormBoss>();
	private Set<Integer> baseCuis = new HashSet<Integer>();
	private String quantityFactor = "";
	private String qualitativeDistinction = "";
	private Vector<Long> snomedCodes = new Vector<Long>();
	private Vector<RxNormSBD> brandDrugs = new Vector<RxNormSBD>();
	private String status = "";
	private Set<Property> properties = new HashSet<>();
	private Set<NDC> ndcSet = new HashSet<>();
	private ArrayList<Property> attributes = new ArrayList<Property>();
	private ArrayList<Property> codes = new ArrayList<Property>();
	private ArrayList<Property> names = new ArrayList<Property>();
	private ArrayList<Property> sources = new ArrayList<Property>();
	private boolean hasNDC = false;
	private boolean isVetOnly = false;
	private boolean isPrescribable = false;
	private boolean isVaccine = false;
	private boolean hasQd = false;	
	private ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	RestUrl restUrl = new RestUrl();
	
	//testing
	private String cvxCode = "";
	
	public RxNormSCD(PrintWriter pwVax, Integer cui, String scdName) {
		rxcui = cui;
		name = scdName;
		JSONObject allRelated = null;
		JSONObject allProperties = null;
		JSONObject rxHistory = null;
		JSONObject ndc = null;
		JSONObject humanProperty = null;
		
		try {
			allRelated = getresult(restUrl.getRestUrl() + "/REST/rxcui/" + rxcui + "/related.json?tty=IN+PIN+DF+SBD", null);
//MOR-39: transition to the new history API
//			rxHistory = getresult(restUrl.getRestUrl() + "/REST/rxcuihistory/concept.json?rxcui=" + rxcui );
			rxHistory = getresult(restUrl.getRestUrl() + "/REST/rxcui/" + rxcui + "/historystatus.json", null);			
			humanProperty = getresult(restUrl.getRestUrl() + "/REST/rxcui/" + rxcui + "/allProperties.json?prop=all", null);
			ndc = getresult(restUrl.getRestUrl() + "/REST/rxcui/" + rxcui + "/allhistoricalndcs.json?history=1", null); //most efficient			
		} catch (Exception e) {
			System.out.println("Unable to finish building SCD for rxcui " + rxcui);
			e.printStackTrace();
		}
		
		if( allRelated != null ) {
			JSONObject allRelatedGroup = (JSONObject) allRelated.get("relatedGroup");
			JSONArray conceptGroup = (JSONArray) allRelatedGroup.get("conceptGroup");
			for( int i=0; i < conceptGroup.length(); i++ ) {
				JSONObject element = (JSONObject) conceptGroup.get(i);
				String tty = element.getString("tty");
				if( tty.equals("IN") )  {
					JSONArray conceptProperties = null;
					if(element.has("conceptProperties")) {
					conceptProperties = (JSONArray) element.get("conceptProperties");
						if( conceptProperties != null ) {			
							for(int j=0; j < conceptProperties.length(); j++) {
								JSONObject b = (JSONObject) conceptProperties.get(j);
								RxNormIngredient ing = new RxNormIngredient(b);
								vIngredient.add(ing);
							}						
						}
					}
				}
				else if( tty.equals("PIN") )  {
					JSONArray conceptProperties = null;
					if(element.has("conceptProperties")) {
					conceptProperties = (JSONArray) element.get("conceptProperties");
						if( conceptProperties != null ) {			
							for(int j=0; j < conceptProperties.length(); j++) {
								JSONObject b = (JSONObject) conceptProperties.get(j);
								RxNormIngredient ing = new RxNormIngredient(b);
								ing.setPIN(true);
								vIngredient.add(ing);
							}						
						}
					}
				}
				else if( tty.equals("DF") ) {
					JSONArray conceptProperties = null;
					if(element.has("conceptProperties")) {					
						conceptProperties = (JSONArray) element.get("conceptProperties");
						if( conceptProperties != null ) {			
							for(int j=0; j < conceptProperties.length(); j++) {
								JSONObject b = (JSONObject) conceptProperties.get(j);
								RxNormDoseForm df = new RxNormDoseForm(b);
								vDoseForm.add(df);
							}						
						}
					}
				}
				else if( tty.equals("SBD")) {
					JSONArray conceptProperties = null;
					if(element.has("conceptProperties")) {
						conceptProperties = (JSONArray) element.get("conceptProperties");
						if(conceptProperties != null) {
							for(int j=0; j < conceptProperties.length(); j++) {
								JSONObject b = (JSONObject) conceptProperties.get(j);
								RxNormSBD sbd = new RxNormSBD(b);
								brandDrugs.add(sbd);
							}
						}
					}
				}
			}
		}
		if( rxHistory != null ) {
//			if( rxcui.equals(1801823) ) {
//				System.out.println("stop");
//			}
			JSONObject rxcuiStatusHistory = (JSONObject) rxHistory.get("rxcuiStatusHistory");
			JSONObject metaData = (JSONObject) rxcuiStatusHistory.get("metaData");
			if(metaData.has("status")) {
				this.status = metaData.getString("status");
			}
				JSONObject definitionalFeatures = (JSONObject) rxcuiStatusHistory.get("definitionalFeatures");
				if(definitionalFeatures.has("ingredientAndStrength")) {
					JSONArray arrBoss = (JSONArray) definitionalFeatures.get("ingredientAndStrength");
					for(int i=0; i < arrBoss.length(); i++) {
						JSONObject b = (JSONObject) arrBoss.get(i);
						RxNormBoss boss = buildBoss(rxcui, b);
						vBoss.add(boss);
					}											
				}	
		}
		if( ndc != null ) {
//	example, 204913 - Direct only for now		
//			{
//				"historicalNdcConcept": {
//				"historicalNdcTime": [
//				{
//				"status": "direct",
//				"rxcui": "204913",
//				"ndcTime": [
//				{
//				"ndc": [
//				"00096121000"
//				],
//				"startDate": "201510",
//				"endDate": "202212"
//				},
//				{
//				"ndc": [
//				"00395277516"
//				],
//				"startDate": "201205",
//				"endDate": "202212"
//				},	
			
			

			
			JSONObject historicalNdcConcept = null;
			if( !ndc.isNull("historicalNdcConcept") ) {
				historicalNdcConcept = (JSONObject) ndc.getJSONObject("historicalNdcConcept");
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
//				if( !historicalNdcConcept.isNull("historicalNdcTime") ) {
//					JSONArray historicalNdcTime = historicalNdcConcept.getJSONArray("historicalNdcTime");
//					String status = historicalNdcConcept.getString("status");
//					for(int i=0; i < historicalNdcTime.length(); i++) {
//						JSONArray ndcTime = historicalNdcTime.getJSONArray(i);					
//						for(int j=0; j < ndcTime.length(); j++) {
//						
//							JSONObject ndcTimeElement = ndcTime.getJSONObject(j);
//							
//							if(ndcTimeElement.has("ndc")) {
//								
//								JSONArray ndcArray =  ndcTimeElement.getJSONArray("ndc");
//								
//								if(ndcArray.length() == 1) {
//									NDC ndcObject = new NDC();
//									
//									ndcObject.setNdc(ndcArray.getString(0));
//									ndcObject.setStatus(status);
//									ndcObject.setStartDate(ndcTimeElement.getString("startDate"));
//									ndcObject.setEndDate(ndcTimeElement.getString("endDate"));
//								}
//								else {
//									System.out.println(rxcui + ": a direct ndcTime without ndc array of 1");
//								}
//	
//							}
//						}
//						
//					}				
//				}
			}
		}
		if( humanProperty != null ) {
			//TODO for JC: Obsolete and Not Active
			//Failed at rxcui: 1000026 ---> methenamine mandelate 250 MG Oral Tablet
			//org.json.JSONException: JSONObject["propConceptGroup"] not found.
			//skeleton model for SBDs only is fine but cannot build logical definitions without needed definitional features
			//this one appears to be an Obsolete concept on a method allowing only Active
			boolean vet = false;
			boolean human = false;
			boolean prescribable = false;
			boolean vaccine = false;
			JSONObject propConceptGroup = null;
			try {
				propConceptGroup = (JSONObject) humanProperty.get("propConceptGroup");
			} catch(Exception e) {
//				System.out.println("Failed at rxcui: " + rxcui + " ---> " + name);
//				e.printStackTrace();
			}
			if( propConceptGroup != null && !propConceptGroup.isNull("propConcept") ) {
				JSONArray propConceptArray = (JSONArray) propConceptGroup.get("propConcept");
				for(int i=0; i < propConceptArray.length(); i++) {
					JSONObject b = (JSONObject) propConceptArray.get(i);
					String category = b.getString("propCategory");					
					String propertyName = b.getString("propName");
					String propertyValue = b.getString("propValue");
					addProperty(propertyName, propertyValue, category);					
					switch(propertyName) { //some of these boolean values are used for the Analyzer
						case "CVX":
							vaccine = true;
							this.cvxCode = b.getString("propValue").trim();
							if(pwVax != null) {
								pwVax.println(this.name + "\t(" + this.rxcui + ")\tSCD\t" + this.cvxCode);
								pwVax.flush();
							}
							break;					
						case "HUMAN_DRUG":
							human = true;							
							break;
						case "PRESCRIBABLE":
							prescribable = true;							
							break;
						case "QUALITATIVE_DISTINCTION":
							this.hasQd = true;
							this.qualitativeDistinction = propertyValue;							
							break;							
						case "QUANTITY":
							this.quantityFactor = propertyValue;							
							break;
						case "VET_DRUG":
							vet = true;							
							break;
						default:
							//do nothing
							break;
					}
				}
			}
			if( vet && !human ) { 
				this.isVetOnly = true;
			}
			if( prescribable ) {
				this.isPrescribable = true;
			}
			if( vaccine ) {
				this.isVaccine = true;
			}			
		}
		
		if( vDoseForm.size() > 1 ) {
			System.out.println("More than 1 DF for " + name);
		}
		
//		System.out.println("Finished SCD in " + (System.currentTimeMillis() - start) + " milliseconds.");	
		
	}
	
	private void addProperty(String propertyName, String propertyValue, String category) {	
		Property p = new Property(propertyName, propertyName, propertyValue, category); //unique identifiers will be declared during generation
		properties.add(p);	
	}
	
	public RxNormSCD(String cui, String name, String historycall, HashMap<RxNormDoseForm, SnomedDFPair> rxNormDoseForm2SnomedDFPair) {
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
			}
		}
		
		try {
			allProperties = getresult(restUrl.getRestUrl() + "/REST/rxcui/" + rxcui + "/allProperties.json?prop=all", null);
		} catch(Exception e) {
			e.printStackTrace();
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
	
	public RxNormSCD() {
		
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
		}
		else {
			nv = (!nvString.isEmpty()) ? Double.valueOf(nvString) : Double.valueOf("1");
		}
		if(!dvString.isEmpty() && !NumberUtils.isParsable(dvString)) {
			System.err.println(rxcui + "\t" + this.name + "\t" + dvString + "\t" + status + "\tNaN" );
		}
		else {
			dv = (!dvString.isEmpty()) ? Double.valueOf(dvString) : Double.valueOf("1"); //If the dv is empty, can we assume this is 1?
		}
		
		String nu = (!nuString.isEmpty()) ? new String(nuString) : "1";

		// unit of presentation is set later in the program, so this is a bad thing to do
//		String du = (!duString.isEmpty()) ? new String(duString) : this.unitOfPresentationName;
		String du = (!duString.isEmpty()) ? new String(duString) : "1";
		
		try {
			if(rxcui == null) {System.out.println("rxcui");}
			if(baserxcui == null) {System.out.println("baserxcui");}
			if(basename == null) {System.out.println("basename");}
			if(bossrxcui == null) {System.out.println("bossrxcui");}
			if(bossname == null) {System.out.println("bossname");}
			if(nv == null) {System.out.println("nv null");}
			if(nu == null) {System.out.println("nu null");}
			if(dv == null) {System.out.println("dv null");}
			if(du == null) {System.out.println("du null");}
			if(actIngredRxcui == null) {System.out.println("actIngredRxcui null");}
			if(actIngredName == null) {System.out.println("actIngredName null");}
			if(this.name == null) {System.out.println("name null");}
			if(this.vDoseForm.get(0).getName() == null) {System.out.println("vDose null");}
			
			boss = new RxNormBoss(new Integer(rxcui), baserxcui, basename, bossrxcui, bossname, nv, nu, dv, du, actIngredRxcui, actIngredName, this.name, this.vDoseForm.get(0).getName());
		} catch(Exception e) {
//			System.out.println("Building BoSS for rxcui: " + this.rxcui.toString());
		}
		if(boss == null) {
			System.out.println(rxcui + " doesn't have a BoSS");
		}
		return boss; 
	}
	
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
	
	public boolean getIsVaccine() {
		return this.isVaccine;
	}
	
	public boolean getIsPrescribable() {
		return this.isPrescribable;
	}
	
	public void setIsPrescribable(boolean b) {
		this.isPrescribable = b;
	}
	
	public SnomedDefinitionTemplate getSnomedDefinitionTemplate() {
		return this.template;
	}
	
	public void setSnomedDefinitionTemplate(SnomedDefinitionTemplate t) {
		this.template = t;
	}
	
	public Vector<Long> getSnomedCodes() {
		return this.snomedCodes;
	}
	
	//we already know this scd is missing an active ingredient from the class its called from
//	public RxNormSCD findSimilarActiveDrug(TreeMap<String, RxNormSCD> map) {
//		RxNormSCD returnSCD = null;
//		
//		if( !this.status.equalsIgnoreCase("active") ) {
//			for(String code : map.keySet()) {
//				returnSCD = map.get(code);
//				if((this.getRxNormBoss().size() == returnSCD.getRxNormBoss().size())
//					&& //this should probably be dose form group in the future - let's see how many we pickup first
//					this.getRxNormDoseForm().get(0).equals(returnSCD.getRxNormDoseForm().get(0))) {
//					//proceed...
//					
//				}
//			}
//		}
//		
//		
//		return returnSCD;
//	}
	
//	public boolean missingActiveIngredient(Vector<RxNormBoss> bosses) {
//		boolean missing = false;
//		for(RxNormBoss boss : bosses) {
//			if(boss.)
//		}
//	}
	
	public void setSnomedCodes() {
		JSONObject allSnomedCodes = null;
		String cuiString = this.rxcui.toString();
		try {
			allSnomedCodes = getresult(restUrl.getRestUrl() + "/REST/rxcui/" + cuiString + "/property.json?propName=SNOMEDCT", null);			
		}
		catch(Exception e) {
			System.out.println("Unable to fetch snomed codes for rxcui: " + cuiString);
		}
		
		if( allSnomedCodes != null ) {
			if( !allSnomedCodes.isNull("propConceptGroup") ) {
				JSONObject propConceptGroup = (JSONObject) allSnomedCodes.get("propConceptGroup");
				JSONArray propConceptArr = (JSONArray) propConceptGroup.get("propConcept");
				for( int i=0; i < propConceptArr.length(); i++ ) {
					JSONObject conceptValue = (JSONObject) propConceptArr.get(i);
					Long codeToAdd = new Long(conceptValue.get("propValue").toString());
					this.snomedCodes.add(codeToAdd);
				}
			}
		}
	}
	
	public Long getManufacturedDoseFormCode() {
		return this.manufacturedDoseFormCode;
	}
	
	public String getManufacturedDoseFormName() {
		return this.manufacturedDoseFormName;
	}
	
	public Long getUnitOfPresentationCode() {
		return this.unitOfPresentationCode;
	}
	
	public String getUnitOfPresentationName() {
		return this.unitOfPresentationName;
	}
	
	public void setManufacturedDoseFormCode(Long code) {
		this.manufacturedDoseFormCode = code;
	}
	
	public void setManufacturedDoseFormName(String name) {
		this.manufacturedDoseFormName = name;
	}	
	
	public void setUnitOfPresentationCode(Long code) {
		this.unitOfPresentationCode = code;
	}
	
	public void setUnitOfPresentationName(String name) {
		this.unitOfPresentationName = name;
	}
	
	public Integer getCui() {
		return this.rxcui;
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean hasNDC() {
		return this.hasNDC;   //remember, it's false by default
							  //if not set on instantiation
							  //quit trying to find it elsewhere
	}
	
	public Vector<RxNormIngredient> getRxNormIngredient() {
		return this.vIngredient;
	}
	
	public Vector<RxNormDoseForm> getRxNormDoseForm() {
		return this.vDoseForm;
	}
	
	public Vector<RxNormBoss> getRxNormBoss() {
		return this.vBoss;
	}
	
	public boolean hasUnknownActiveIngredient() {
		for(RxNormBoss boss : this.vBoss) {
			if(boss.getActiveIngredientName().equals("null")) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasUnknownBoss() { 
		for(RxNormBoss boss : this.vBoss) {
			if(boss.getBossName().equals("null")) {
				return true;
			}
		}
		return false;
	}
	
	public String getRxNormQuantityFactor() {
		return this.quantityFactor;
	}
	
	public String getRxNormQualitativeDistinction() {
		return this.qualitativeDistinction;
	}
	
	public boolean hasRxNormQualitativeDistinction() {
		return this.hasQd;
	}	
	
	public void addBaseCui(Integer i) {
		this.baseCuis.add(i);
	}
	
	public int getBaseCuiCount() {
		int count = this.baseCuis.size();
		return count;
	}
	
	public boolean isVetOnly() {
		return this.isVetOnly;
	}
	
	public Vector<RxNormSBD> getBrandDrugs() {
		return brandDrugs;
	}

	public void setBrandDrugs(Vector<RxNormSBD> brandDrugs) {
		this.brandDrugs = brandDrugs;
	}
	
	public void print() {
		System.out.println(rxcui + ":\t" + name );
		
		System.out.println(" Ingredient(s):");
		for(RxNormIngredient i : vIngredient ) {
			System.out.print("\t" + i.getRxcui() + "\t=> " + i.getName());
			String eol = i.getPIN() ? " (is PIN)\n" : "\n";
			System.out.print(eol);
		}
		
		System.out.println(" Dose Form(s):");
		for(RxNormDoseForm d : vDoseForm) {
			System.out.println("\t" + d.getRxcui() + "\t=> " + d.getName());
		}
		
		System.out.println(" BoSS(es)");
		for(RxNormBoss b : vBoss) {
			System.out.println("\t" + b.getBossRxCui().toString() + "\t=> " + b.getBossName());
			if( b.getNumeratorUnit() != null) {
				System.out.println("\t\t" + "nu => " + b.getNumeratorUnit());
			}
			else {
				System.out.println("\t\t" + "nu => NOT FOUND");
			}
			if( b.getNumeratorValue() != null ) {
				System.out.println("\t\t" + "nv => " + b.getNumeratorValue().toString());
			}
			else {
				System.out.println("\t\t" + "nv => NOT FOUND");
			}			
			if( b.getDenominatorUnit() != null ) {
				System.out.println("\t\t" + "du => " + b.getDenominatorUnit()); //where this is empty assume 1?
			}
			else {
				System.out.println("\t\t" + "du => NOT FOUND");
			}
			if( b.getDenominatorValue() != null ) {
				System.out.println("\t\t" + "dv => " + b.getDenominatorValue().toString()); //where this is empty assume... ? dose form?
			}
			else {
				System.out.println("\t\t" + "dv => NOT FOUND");
			}	
			System.out.println();
		}
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public Set<Property> getProperties() {
		return this.properties;
	}

	public Set<NDC> getNdcSet() {
		return ndcSet;
	}
	
}