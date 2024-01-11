package gov.nih.nlm.mor.RxNorm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;

import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.Snomed.SnomedUnitOfPresentation;
import gov.nih.nlm.mor.map.ConcentrationToPresentation;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

public class RxNormBoss implements java.io.Serializable {
	
	private static final long serialVersionUID = 6307466552548789850L;
	private Integer rxcui = null;	
	private Integer baseRxcui = null;
	private String baseName = null;	
	private Integer bossRxcui = null;
	private String bossName = null;
	private Double numeratorValue = null;
	private String numeratorUnit = null;
	private Double denominatorValue = null;
	private String denominatorUnit = null;
	private Integer actIngredRxcui = null;
	private String actIngredName = null;
//	private Vector<String> unitsNotForNormalization = new Vector<String>();
	private Vector<String> unitsForNormalization = new Vector<String>();	
	private String[] unitAbbreviations;	
	private String doseForm = null;
	private transient String forWhat = new String(); //debug purposes
	RestUrl restUrl = new RestUrl();
	
	//declare the snomed equivalencies and get all the math out of the way
	private String snomedNU = null;
	private Double snomedNV = null;
	private String snomedDU = null;
	private Double snomedDV = null;
	private ConcentrationToPresentation concentrationToPresentation = null;
	private transient int unitIndex = 0;
	//TODO: continue this...
	private SnomedUnitOfPresentation snomedUnitOfPresentation = null;
	
	//Introduced RxNav DU names Mar-06-2023
	//private HashMap<RxNormDoseForm, SnomedDFPair> rxNormDoseForm2SnomedDFPair = null;
	
	//TODO: Start here on implementation and normalization of strengths (constructor first, followed by recursive method
	private RxNormDoseForm rxNormDoseForm = null;
	private SnomedDFPair snomedDFPair = null;
	
	String closeTerm = new String(restUrl.getRestUrl() + "/REST/rxcui.json?name=");
	String closeTermSearchParam = new String("&search=2");
	private ExponentialBackoff exponentialBackoff = new ExponentialBackoff();	
	
//	public RxNormBoss(URL apiBaseUrl, Integer rxcui) {
//		this.allConceptsRxhistoryMethod = this.allConceptsRxhistoryMethod + rxcui.toString();
//		JSONObject result = gather();
//		if( result != null ) {
//
//			for(int i=0; i < bosses.length(); i++) {
//				JSONObject boss = bosses.getJSONObject(i);
//				RxNormBoss bossToAdd = buildBoss(rxcui, boss);
//				if( bossToAdd != null ) {
//					bossConcepts.add(bossToAdd);
//				}
//			}
//		}		
//	}
	
	/*
	 *                 "baseRxcui": "6837",
                "baseName": "Methionine",
                "bossRxcui": "6837",
                "bossName": "Methionine",
                "actIngredRxcui": "",
                "actIngredName": "",
                "moietyRxcui": "",
                "moietyName": "",
                "numeratorValue": "250",
                "numeratorUnit": "MG",
                "denominatorValue": "",
                "denominatorUnit": ""
	 */
	
	public RxNormBoss(Integer cui, JSONObject b, String doseFormCui) {
		JSONObject result = b;
		
		RxNormIngredient possibleAnhydrous = null;
		
		this.rxcui = cui;
		this.baseRxcui = new Integer(result.get("baseRxcui").toString());
		this.baseName = result.get("baseName").toString(); 
		this.bossRxcui = new Integer(result.get("bossRxcui").toString());
		this.bossName = result.get("bossName").toString();
		possibleAnhydrous = removeAnhydrous(this.bossRxcui, this.bossName);
		if(possibleAnhydrous != null && possibleAnhydrous.getRxcui() != this.bossRxcui) {
			this.bossRxcui = possibleAnhydrous.getRxcui();
			this.bossName = possibleAnhydrous.getName();
		}
		this.numeratorValue = result.get("numeratorValue").toString().isEmpty() ? Double.valueOf(1) : Double.valueOf(new String(result.get("numeratorValue").toString()));
		this.numeratorUnit = result.get("numeratorUnit").toString();
		this.denominatorValue = result.get("denominatorValue").toString().isEmpty() ? Double.valueOf(1) : Double.valueOf(new String(result.get("denominatorValue").toString())); 
		this.denominatorUnit = result.get("denominatorUnit").toString().isEmpty() ? "1" : result.get("denominatorUnit").toString();
		
		//implicit business rules as a result of dataprep changes made by pulling from SCDC RXNORM_BOSS_STRENGTH - right move? We shall see.
		//this.denominatorUnit = (result.get("denominatorUnit").toString().isEmpty() || result.get("denominatorUnit").toString().equalsIgnoreCase("EACH")) ? "1" : result.get("denominatorUnit").toString();		
		
		//same anhydrous detection?
		this.actIngredRxcui = result.get("actIngredRxcui").toString().isEmpty() ? Integer.valueOf(-1) : new Integer(result.get("actIngredRxcui").toString()); 
		this.actIngredName = result.get("actIngredName").toString().isEmpty() ? "null" : result.get("actIngredName").toString();  
		
		setUnitsForNormalization();		
		setUnitAbbreviations();
		calcSnomedValues();
		
//		if( !this.snomedNU.equals(numeratorUnit)) {			
//			System.out.println(this.bossName + "\t" + this.forWhat);
//			System.out.println("\trx NU => " + this.numeratorUnit + "\tSCT NU => " + this.snomedNU);
//			System.out.println("\trx NV => " + this.numeratorValue + "\tSCT NV => " + this.snomedNV);
//		}
//		if( !this.snomedDU.equals(this.denominatorUnit)) {		
//			System.out.println(this.bossName + "\t" + this.forWhat);
//			System.out.println("\trx DU => " + this.denominatorUnit + "\tSCT DU => " + this.snomedDU);
//			System.out.println("\trx DV => " + this.denominatorValue + "\tSCT DV => " + this.snomedDV);
//		}			
		
	}
	
	public RxNormBoss(RxNormBoss b) {
		this.rxcui = b.getRxcui();	
		this.baseRxcui = b.getBaseRxcui();
		this.baseName = b.getBaseName();	
		this.bossRxcui = b.getBossRxCui();
		this.bossName = b.getBossName();
		this.numeratorValue = b.getNumeratorValue();
		this.numeratorUnit = b.getNumeratorUnit();
		this.denominatorValue = b.getDenominatorValue();
		this.denominatorUnit = b.getDenominatorUnit();
		this.actIngredRxcui = b.getActiveIngredientRxCui();
		this.actIngredName = b.getActiveIngredientName();		
		
	}
	
	public RxNormBoss(boolean all, Vector<RxNormDoseForm> dfV, Integer cui, JSONObject b, HashMap<RxNormDoseForm, SnomedDFPair> rxNormDoseForm2SnomedDFPair) {
		JSONObject result = b;
		
		RxNormIngredient possibleAnhydrous = null;
		
		if(dfV != null && dfV.size() > 0) {
			this.doseForm = dfV.get(0).getName();
			rxNormDoseForm = getRxNormDoseForm(doseForm, rxNormDoseForm2SnomedDFPair);
			snomedDFPair = getSnomedDoseFormPair(rxNormDoseForm, rxNormDoseForm2SnomedDFPair);
//			SnomedUnitofPresentation = snomedDFPair.getSnomedUnitOfPresentation();
//			String dfCui = dfV.get(0).getRxcui();
//			if(df2Preferred.containsKey(dfCui)) {
//				this.rxNavPreferredDU = df2Preferred.get(dfCui);
//			}
		}		
		
		this.rxcui = cui;
		this.baseRxcui = result.get("baseRxcui").toString().isEmpty() ? Integer.valueOf(-1) : new Integer(result.getString("baseRxcui").toString());  
		this.baseName = result.get("baseName").toString().isEmpty() ? "" : result.get("baseName").toString();
		this.bossRxcui = result.get("bossRxcui").toString().isEmpty() ? Integer.valueOf(-1) : new Integer(result.get("bossRxcui").toString());
		this.bossName = result.get("bossName").toString().isEmpty() ? "" : result.get("bossName").toString();
		possibleAnhydrous = removeAnhydrous(this.bossRxcui, this.bossName);
		if(possibleAnhydrous != null && possibleAnhydrous.getRxcui() != this.bossRxcui) {
			this.bossRxcui = possibleAnhydrous.getRxcui();
			this.bossName = possibleAnhydrous.getName();
		}
		this.numeratorValue = result.get("numeratorValue").toString().isEmpty() ? Double.valueOf(1) : !NumberUtils.isCreatable(result.get("numeratorValue").toString()) ? Double.valueOf(1) : Double.valueOf(new String(result.get("numeratorValue").toString()));
		this.numeratorUnit = result.get("numeratorUnit").toString();
		this.denominatorValue = result.get("denominatorValue").toString().isEmpty() ? Double.valueOf(1) : !NumberUtils.isCreatable(result.get("denominatorValue").toString()) ? Double.valueOf(1) : Double.valueOf(new String(result.get("denominatorValue").toString()));
		this.denominatorUnit = result.getString("denominatorUnit");
		
		//Introduced Mar-06-2023
//		try {
//			if(rxNormDoseForm != null && snomedDFPair != null) {
//				
//				//this.denominatorUnit = result.get("denominatorUnit").toString().isEmpty() ? "1" : result.get("denominatorUnit").toString();
////				this.denominatorUnit = !result.get("denominatorUnit").toString().equalsIgnoreCase(rxNormDoseForm.getRxnavPreferredDenominatorUnitName()) ? 
////												"1" : snomedDFPair.getSnomedUnitOfPresentation().toString();
//				
//				
//				if(!snomedDFPair.hasUP()) {
//					this.denominatorUnit = result.get("denominatorUnit").toString();
//				}
//				//an empty value (as before) would be equivalent to something unexpected				
//				else if(!result.get("denominatorUnit").toString().equalsIgnoreCase(rxNormDoseForm.getRxnavPreferredDenominatorUnitName())) {
//					this.denominatorUnit = "1";
//				}
//				//TODO: Think more about this :)
//				//does this need to be recognized as an owl:Class off the bat? 
//				//we calculate and could ignore a type OWLClass
//				else {
//					//What is an EACH?
//					//if you have .5mg/mg going to 500mcg/...EACH? - we will have to keep this on normalization with implicit rules
//					this.denominatorUnit = result.getString("denominatorUnit");
//				}
//			}
//			else {
//				this.denominatorUnit = result.getString("denominatorUnit");
//			}
//		} catch(Exception e) {
//			System.err.println(rxcui.toString() + "\tProblem superimposing a preferred RxNav DU - check config file for an entry on doseform " + this.doseForm);
//			e.printStackTrace();
//		}
		
		//same anhydrous detection?
		this.actIngredRxcui = result.get("activeIngredientRxcui").toString().isEmpty() ? Integer.valueOf(-1) : new Integer(result.get("activeIngredientRxcui").toString()); 
		this.actIngredName = result.get("activeIngredientName").toString().isEmpty() ? "" : result.get("activeIngredientName").toString();  
		
		setUnitsForNormalization();		
		setUnitAbbreviations();
//		if(dfV != null && dfV.size() > 0) {
//			this.doseForm = dfV.get(0).getName();
//		}
		if(this.doseForm != null && !this.doseForm.isEmpty()) {
			calcSnomedValues(); //with all concepts we might need to watch out for obsolete dfs (as strings but converted similarly?)
		}
	}	
	
	private SnomedDFPair getSnomedDoseFormPair(RxNormDoseForm rxNormDF, HashMap<RxNormDoseForm, SnomedDFPair> rxNormDoseForm2SnomedDFPair) {
		if(rxNormDoseForm2SnomedDFPair.containsKey(rxNormDF)) {
			return rxNormDoseForm2SnomedDFPair.get(rxNormDF);
		}
		return null;
	}

	private RxNormDoseForm getRxNormDoseForm(String doseForm2, HashMap<RxNormDoseForm, SnomedDFPair> rxNormDoseForm2SnomedDFPair) {
		for(RxNormDoseForm df : rxNormDoseForm2SnomedDFPair.keySet()) {
			if(df.getName().equalsIgnoreCase(doseForm2)) {
				return df;
			}
		}
		return null;
	}

	public String getDoseForm() {
		return this.doseForm;
	}
	
	public RxNormBoss(Integer cui, JSONObject b) {
		JSONObject result = b;
		
		RxNormIngredient possibleAnhydrous = null;
		
		this.rxcui = cui;
		this.baseRxcui = new Integer(result.get("baseRxcui").toString());
		this.baseName = result.get("baseName").toString(); 
		this.bossRxcui = new Integer(result.get("bossRxcui").toString());
		this.bossName = result.get("bossName").toString();
		possibleAnhydrous = removeAnhydrous(this.bossRxcui, this.bossName);
		if(possibleAnhydrous != null && possibleAnhydrous.getRxcui() != this.bossRxcui) {
			this.bossRxcui = possibleAnhydrous.getRxcui();
			this.bossName = possibleAnhydrous.getName();
		}
		this.numeratorValue = result.get("numeratorValue").toString().isEmpty() ? Double.valueOf(1) : Double.valueOf(new String(result.get("numeratorValue").toString()));
		this.numeratorUnit = result.get("numeratorUnit").toString();
		this.denominatorValue = result.get("denominatorValue").toString().isEmpty() ? Double.valueOf(1) : Double.valueOf(new String(result.get("denominatorValue").toString())); 
		this.denominatorUnit = result.get("denominatorUnit").toString().isEmpty() ? "1" : result.get("denominatorUnit").toString();
		
		//same anhydrous detection?
		this.actIngredRxcui = result.get("actIngredRxcui").toString().isEmpty() ? Integer.valueOf(-1) : new Integer(result.get("actIngredRxcui").toString()); 
		this.actIngredName = result.get("actIngredName").toString().isEmpty() ? "null" : result.get("actIngredName").toString();  
		
		setUnitsForNormalization();		
		setUnitAbbreviations();
		calcSnomedValues();
	}
	
	private RxNormIngredient removeAnhydrous(Integer cui, String ing) {
		RxNormIngredient returnIngredient = null;
		if( cui == null || ing == null ) return returnIngredient;
		
		boolean set = false;
		String newIng = null;
		String newCui = null;
		String val = ing.toLowerCase();
		if( val.contains("anhydrous") ) {
			//brute force, not going to waste time with java regexs right now
			val = val.replace(",", "");			
			val = val.replace("(anhydrous)", "");			
			val = val.replace("anhydrous", "");
			val = val.replace("monobasic", "");
			val = val.replace("tribasic", "");
			val = val.replace("(obsolete)", "");
			val = val.trim();
			val = val.replace(" ", "%20");
			JSONObject result = null;
//			JSONObject scdResult = null;
//			JSONObject ingredientResult = null;
			try {
				result = getresult(closeTerm + val + closeTermSearchParam, null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("Unable to fetch closest term to: " + val);
				e.printStackTrace();
			}
			if( result != null && result.has("idGroup") && !result.isNull("idGroup")) {
					JSONObject idGroup = (JSONObject) result.get("idGroup");
					if(idGroup.has("name")) {
						newIng = idGroup.get("name").toString();
						if( idGroup.has("rxnormId") ) {
							JSONArray rxnormIdArray = (JSONArray) idGroup.get("rxnormId");
							newCui = rxnormIdArray.getString(0); //the best match is the name.. not many cases of more than one cui that i've seen
							returnIngredient = new RxNormIngredient(Integer.valueOf(newCui), newIng);
							set = true;
		//					System.out.println("Adding a new ingredient in BoSS from an anyhdrous form:");
		//					System.out.println("\t" + newCui + " : " + newIng);
						}
						else {
							System.out.println("Unable to find an rxcui for non-anhydrous text: " + newIng);
						}
					}
			}
		}
		if( !set ) {
			 returnIngredient = new RxNormIngredient(cui, ing);				
		}
		
		return returnIngredient;
	}	
	
	public RxNormBoss(Integer id, Integer bzrxcui, String bzname, Integer bsrxcui, String bsname, Double nv, String numU, Double dv, String denU, Integer actCui, String actName, String scdName, String rxDoseForm) {
		
		RxNormIngredient possibleAnhydrous = null;
		
		this.rxcui = id;
		this.baseRxcui = bzrxcui;
		this.baseName = bzname;
		this.bossRxcui = bsrxcui;
		this.bossName = bsname;
		possibleAnhydrous = removeAnhydrous(this.bossRxcui, this.bossName);
		if(possibleAnhydrous != null && possibleAnhydrous.getRxcui() != this.bossRxcui) {
			this.bossRxcui = possibleAnhydrous.getRxcui();
			this.bossName = possibleAnhydrous.getName();
		}
		this.numeratorValue = nv;
		this.numeratorUnit = numU;
		this.denominatorValue = dv;
		this.denominatorUnit = denU;
		this.actIngredRxcui = actCui;
		this.actIngredName = actName;
		this.forWhat = scdName;
		this.doseForm = rxDoseForm;

		setUnitsForNormalization();
		setUnitAbbreviations();
		calcSnomedValues();   //so now it will be necessary to figure out if the Rx CUI is a SCT product or CD
		
//		if( !this.snomedNU.equals(numeratorUnit)) {			
//			System.out.println(this.bossName + "\t" + this.forWhat);
//			System.out.println("\trx NU => " + this.numeratorUnit + "\tSCT NU => " + this.snomedNU);
//			System.out.println("\trx NV => " + this.numeratorValue + "\tSCT NV => " + this.snomedNV);
//		}
//		if( !this.snomedDU.equals(this.denominatorUnit)) {		
//			System.out.println(this.bossName + "\t" + this.forWhat);
//			System.out.println("\trx DU => " + this.denominatorUnit + "\tSCT DU => " + this.snomedDU);
//			System.out.println("\trx DV => " + this.denominatorValue + "\tSCT DV => " + this.snomedDV);
//		}			
		
	}
	
	private void setUnitsForNormalization() {
		//TODO: Put this in a config file
		
// It started as an inverse.. toooooo many, going with just mass in lieu
		//		// special case
//		this.unitsNotForNormalization.add("1");
//		
//		this.unitsNotForNormalization.add("%");		
//		this.unitsNotForNormalization.add("ACTUAT");
//		this.unitsNotForNormalization.add("Amba1U");
//		this.unitsNotForNormalization.add("Amb a 1-U");		
//		this.unitsNotForNormalization.add("AU");
//		this.unitsNotForNormalization.add("BAU");
//		this.unitsNotForNormalization.add("CELLS");
//		this.unitsNotForNormalization.add("HR");
//		this.unitsNotForNormalization.add("IR");
//		this.unitsNotForNormalization.add("MCI");
//		this.unitsNotForNormalization.add("MEQ");
//		this.unitsNotForNormalization.add("ML");
//		this.unitsNotForNormalization.add("L");		
//		this.unitsNotForNormalization.add("MMOL");
//		this.unitsNotForNormalization.add("PNU");
//		this.unitsNotForNormalization.add("Percentage");
//		this.unitsNotForNormalization.add("SQCM");
//		this.unitsNotForNormalization.add("SQHDM");
//		this.unitsNotForNormalization.add("SQ-HDM");		
//		this.unitsNotForNormalization.add("UNT");
//		this.unitsNotForNormalization.add("VECTORGENOMES");
//		this.unitsNotForNormalization.add("VECTOR-GENOMES");
//		this.unitsNotForNormalization.add("GM");
		
		this.unitsForNormalization.add("PG");
		this.unitsForNormalization.add("NG");
		this.unitsForNormalization.add("MCG");
		this.unitsForNormalization.add("MG");
		this.unitsForNormalization.add("G");
		this.unitsForNormalization.add("KG");
		
//Some of these are new...
//		ML
//		EACH
//		ACTUAT
//		MG
//		L
//		VIAL
//		1
//		AU
//		MEQ
//		SQCM
//		SQ-HDM

		
		
	}

	private void setUnitAbbreviations() {
		unitAbbreviations = new String[6];
		unitAbbreviations[0] = "PG";
		unitAbbreviations[1] = "NG";
		unitAbbreviations[2] = "MCG";
		unitAbbreviations[3] = "MG";
		unitAbbreviations[4] = "G";
		unitAbbreviations[5] = "KG";
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
	
//	public String getUnitForPredicate(String p) {
//		if( p.contains("denominator unit") ) {
//			return this.snomedDU;
//		}
//		else if( p.contains("numerator unit") ) {
//			return this.snomedNU;
//		}
//		else return null;
//	}
	
	public void setConcentrationToPresentation(ConcentrationToPresentation cp) {
		this.concentrationToPresentation = cp;
	}
	
	public ConcentrationToPresentation getConcentrationToPresentation() {
		return this.concentrationToPresentation;
	}
	
	public Integer getRxcui() {
		return rxcui;
	}
	
	public Integer getBaseRxcui() {
		return baseRxcui;
	}
	
	public String getBaseName() {
		return baseName;
	}
	
	public Integer getBossRxCui() {
		return bossRxcui;
	}
	
	public String getBossName() {
		return bossName;
	}
	
	public String getActiveIngredientName() {
		return actIngredName;
	}
	
	public Integer getActiveIngredientRxCui() {
		return actIngredRxcui;
	}	
	
	public Double getNumeratorValue() {
		return numeratorValue;
	}
	
	public String getNumeratorUnit() {
		return numeratorUnit;
	}
	
	public Double getDenominatorValue() {
		return denominatorValue;
	}
	
	public String getDenominatorUnit() {
		return denominatorUnit;
	}
	
	public String getSnomedNumeratorUnit() {
		return snomedNU;
	}
	
//	public Double getSnomedNumberatorValue() {
	public String getSnomedNumberatorValue() {
//		return snomedNV;
//        DecimalFormat df = new DecimalFormat("#");
//        df.setMaximumFractionDigits(3);
//        return df.format(snomedNV);	
		String val = "1";
//		System.out.println(this.forWhat + ": NV => " + this.snomedNV);		
		if(this.snomedNV != null ) {        
			DecimalFormat df = new DecimalFormat("#");
	        df.setMaximumFractionDigits(3);		  //should this be higher for the rxnorm values?
			val = df.format(snomedNV);
			if( val != null ) {
				return val;
			}
		}
		return val;
	}
	
	public String getSnomedDenominatorUnit() {
		return snomedDU;
	}
	
//	public Double getSnomedDenominatorValue() {
	public String getSnomedDenominatorValue() {
		String val = "1";
//		System.out.println(this.forWhat + ": DV => " + this.snomedDV);
		if(this.snomedDV != null ) {        
			DecimalFormat df = new DecimalFormat("#");
	        df.setMaximumFractionDigits(3);		
			val = df.format(snomedDV);
			if( val != null ) {
				return val;
			}
		}
		return val;
	}

	//"dimensional analysis" (the older term),
	//"factor label method" being the newer term
	public void calcSnomedValues() {
		//debug
		if(doseForm == null || doseForm.equals("")) {
			System.err.println("No DF for rxcui: " + rxcui + "\t" + this.forWhat);
			System.exit(-1);
		}
		if(this.denominatorUnit == null) {
			System.err.println("Denominator unit is null for rxcui: " + rxcui + "\t" + this.forWhat);
			System.exit(-1);			
		}
		try {
			this.unitsForNormalization.contains(numeratorUnit);
		} catch(Exception e) {
			System.err.println("Fail fast on an unexpected unitForNormalization: " + rxcui + "\t" + this.forWhat);
			e.printStackTrace();
		}
		//		System.out.println(this.forWhat);
		//		if( this.forWhat.equals("Lactobacillus acidophilus 2480000000 MG/ML Oral Suspension")) {
		//			System.out.println("BREAK");
		//		}
		//		if( this.forWhat.equals("Fluocinonide 0.0005 MG/MG Topical Gel")) {
		//		System.out.println("BREAK");
		//		}	
		//		if( this.forWhat.equals("Nitroglycerin 0.02 MG/MG Topical Ointment")) {
		//			System.out.println("BREAK");
		//		}
		//		System.out.println("Something is break on " + this.getRxcui());
		//		System.out.println("NV => " + this.numeratorValue);
		//		System.out.println("NU => " + this.numeratorUnit);
		//		System.out.println("DV => " + this.denominatorValue);
		//		System.out.println("DU => " + this.denominatorUnit);

		//many special rules for dose form pairs without a SCT unit of presentation
		//TODO: Can this be made configurable?  If the DF Map is auto generated, doubt it.
		if( doseForm.contains("Ophthalmic") ) {
			if( doseForm.contains("Gel") || doseForm.contains("Ointment") ) {
				//Feedback from JN: normalizeMGtoML();
				normalizeMGtoGrams();
			}
//Feedback from JN:			else if( doseForm.contains("Ointment")) {
//				normalizeMGtoGrams();
//			}
			else {
				//solution or suspension, normalize per usual
				defaultNormalize();
			}
		}
		else if(doseForm.contains("Topical Cream")) {
			normalizeMLtoGrams();
		}
		else if(doseForm.contains("Topical Lotion")) {
			defaultNormalize();
		}
		else if(doseForm.contains("Topical Foam")) {
			defaultNormalize();
		}
		else if(doseForm.contains("Topical Gel")) {
			normalizeMGtoGrams();
		}
		else if(doseForm.contains("Topical Powder")) {
			normalizeMGtoGrams();
		}
		else if(doseForm.contains("Topical Ointment")) {
			normalizeMGtoGrams();
		}
//		else if( doseForm.contains("Topical")) {
//			if( doseForm.contains("Cream") || doseForm.contains("Lotion") || 
//					doseForm.contains("Foam") ) {
//				// JN: normalizeMLtoGrams();
//				normalizeMGtoML();
//			}
//			else if(doseForm.contains("Gel") || doseForm.contains("Powder") || doseForm.contains("Ointment")) {
//				normalizeMGtoGrams();
//			}
//			else {
//				defaultNormalize();
//			}
//			return; 
//		}
		else if( doseForm.contains("Vaginal") && !doseForm.contains("Insert")) {
			if( doseForm.contains("Cream") ) {
				if( this.denominatorUnit.equalsIgnoreCase("mg")) {
					normalizeMGtoML();
				}
				else {
					normalizeMLtoGrams();
				}
			}
			else if( doseForm.contains("Gel") ) {
				normalizeMGtoGrams();
			}
			else {
				defaultNormalize();
			}
		}
		//		else if( (doseForm.contains("Topical Spray") || doseForm.contains("Cream")) && denominatorUnit.equals("ML") &&
		//			this.unitsForNormalization.contains(numeratorUnit) && !doseForm.contains("Ophthalmic")) {
		//			normalizeMLtoGrams();
		//			return;
		//		}
		else if( (doseForm.contains("Gel") || doseForm.contains("Ointment")) && denominatorUnit.equalsIgnoreCase("MG") && this.unitsForNormalization.contains(numeratorUnit) ) {
			try {
			normalizeMGtoGrams();
			} catch(Exception e) {
				System.err.println("Fail to normalizeMGtoGrams on rxcui: " + rxcui + "\t" + this.forWhat);
			}
		}
		else {
			defaultNormalize();
			return;
		}
		return;
	}
	
	// The following normalization methods look very similar.  To help understand why these are
	// necessary, refer to the RxN SCT assertions already made.  Patterns are visible where RxN
	// will be MG/MG whereas SCT may use mass over volume.
	
	private void defaultNormalize() {
		try {
			if( !this.numeratorUnit.isEmpty() && !this.numeratorValue.isNaN()) {
				normalize(this.numeratorUnit, Double.valueOf(this.numeratorValue), true);			
			}
			if( !this.denominatorUnit.isEmpty() && !this.denominatorValue.isNaN()) {
				normalize(this.denominatorUnit, Double.valueOf(this.denominatorValue), false);
			}	
		} catch(Exception e) {
			System.err.println("normalization failed on rxcui: " + rxcui + "\t" + this.forWhat + "\t(probably no configuration for DF of: " + this.doseForm + ")");
		}
	}
	
	private void normalizeMLtoGrams() {
		if( !this.numeratorUnit.isEmpty() && !this.numeratorValue.toString().isEmpty()) {
			normalize(this.numeratorUnit, Double.valueOf(this.numeratorValue), true);			
		}
		if( !this.denominatorUnit.isEmpty() && !this.denominatorValue.toString().isEmpty()) {
			if( this.denominatorValue == 1 && this.denominatorUnit.equals("ML")) {
				this.snomedDU = "G";
				this.snomedDV = Double.valueOf("1");
			}
		}
		else {
			this.snomedDU = this.denominatorUnit;
			this.snomedDV = this.denominatorValue;
		}
	}
	
	private void normalizeMGtoML() {
		if( !this.numeratorUnit.isEmpty() && !this.numeratorValue.toString().isEmpty()) {
			normalize(this.numeratorUnit, Double.valueOf(this.numeratorValue), true);
		}
		if( !this.denominatorUnit.isEmpty() && !this.denominatorValue.toString().isEmpty()) {
			unitIndex = getAbbreviationIndex(this.snomedNU);
			if( unitIndex != -1) {				
				this.snomedDU = "ML";
				this.snomedDV = Double.valueOf(1);
				multiply(true);
//				setUnit(true);
				//the value could be less than one or more than 1000- we don't know
				normalize(this.snomedNU, Double.valueOf(this.snomedNV), true);
			}			
			else {
				this.snomedDU = this.denominatorUnit;
				this.snomedDV = this.denominatorValue;
			}			
//			if( this.denominatorValue == 1 && this.denominatorUnit.equalsIgnoreCase("mg") ) {
//				this.snomedDU = "ML";
//				this.snomedDV = Double.valueOf(1);
//			}
		}
		
	}
	
	private void normalizeMGtoGrams() {
		if( !this.numeratorUnit.isEmpty() && !this.numeratorValue.toString().isEmpty()) {
			normalize(this.numeratorUnit, Double.valueOf(this.numeratorValue), true);			
		}		
		if( !this.denominatorUnit.isEmpty() && !this.denominatorValue.toString().isEmpty()) {
			if( this.denominatorValue == 1 && this.denominatorUnit.equals("MG")) {
				unitIndex = getAbbreviationIndex(this.snomedNU);
				if( unitIndex != -1) {				
					this.snomedDU = "G";
					this.snomedDV = Double.valueOf("1");
					multiply(true);
//					setUnit(true);
					//the value could be less than one or more than 1000- we don't know
					normalize(this.snomedNU, Double.valueOf(this.snomedNV), true);
				}
			}
			else {  //cannot do anything about ACTUAT?  See: '120 ACTUAT Testosterone 10MG/ACTUAT Topical Gel'
				this.snomedDU = this.denominatorUnit;
				this.snomedDV = this.denominatorValue;
			}			
		}
		else {  //cannot do anything about ACTUAT?  See: '120 ACTUAT Testosterone 10MG/ACTUAT Topical Gel'
			this.snomedDU = this.denominatorUnit;
			this.snomedDV = this.denominatorValue;
		}
	}
	
	//TODO: See how pairing the preferred DU to the UoP (if exists) affects this not-so-much-rocket-science
	//it could be the case the axiom-atic approach would surrender any calculated effort.
	private void normalize(String unit, Double val, boolean isNumerator) {
//		if( this.baseRxcui.equals(Integer.valueOf("103457")) ) {
//			System.out.println("Debug");
//		}	
		unitIndex = getAbbreviationIndex(unit);
		if( val.equals(1.0) ) {
			val = Double.valueOf("1");
		}		
		if(isNumerator) {
			this.snomedNU = unit;
			this.snomedNV = val;
		}
		else {
			this.snomedDU = unit;
			this.snomedDV = val;
		}			
		if( !unitsForNormalization.contains(unit) ) return;			
		boolean done = false;
		while( !done ) { 
			done = checkDone(isNumerator);
		}

		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.HALF_UP);
		this.snomedNV  = Double.valueOf(df.format(Double.valueOf(this.snomedNV)));

	}
	
	private int getAbbreviationIndex(String unit) {
		int index = -1;
		for( int i=0; i < this.unitAbbreviations.length-1; i++ ) {
			if( unitAbbreviations[i].equalsIgnoreCase(unit) ) {
				index = i;
				break;
			}
		}
		return index;
	}	
	
	private void multiply(boolean isNumerator) {
		if(isNumerator) this.snomedNV = this.snomedNV * 1000;
		else this.snomedDV = this.snomedDV * 1000;
		unitIndex--;
//		unitMovement--;
	}
		
	private void divide(boolean isNumerator) {
		if(isNumerator) this.snomedNV = this.snomedNV / 1000;
		else this.snomedDV = this.snomedDV / 1000;
		unitIndex++;
//		unitMovement++;
	}
	
	private boolean checkDone(boolean isNumerator) {
		Double n;
		if(isNumerator)	n = this.snomedNV;
		else n = this.snomedDV;
		boolean done = false;
		if( (n < 1000 && n >= 1) || (unitIndex == unitAbbreviations.length-1 && n >= 1000) || (unitIndex == 0 && n < 1) ) {
			setUnit(isNumerator);
			done = true;
		}
		else if( n >= 1000 ) {
			divide(isNumerator);
			done = checkDone(isNumerator);
		}
		else if( n < 1 ) {
			multiply(isNumerator);
			done = checkDone(isNumerator);			
		}
		return done;
	}	

	
// These methods aren't helpers to checkDone	
//	private String multiply(Double value) {
//		return String.valueOf(value * 1000);
//	}
//	
//	private String divide(Double value) {
//		return String.valueOf(value / 1000);
//	}	
	
	//TODO: Add the unit of presentation to this class or pull it from somewhere else,
	// add to constructor
	private void setUnit(boolean isNumerator) {
		if( isNumerator ) this.snomedNU = unitAbbreviations[unitIndex];
		else {
			String dfString = this.doseForm.toLowerCase();
			if(dfString.contains("tablet") || dfString.contains("capsule") || dfString.contains("suppository") ) this.snomedDU = "1";
			else this.snomedDU = unitAbbreviations[unitIndex];
		}
	}	
	
	//I'll just leave this here.  Currently not used.
	private boolean checkDone(String value) {
		Double n = new Double(value);
		boolean done = false;
		if( n < 1000 && n >= 1 ) {
			done = true;
		}
		else if( n < 1 ) {
			done = false;
		}
		return done;
	}
	
// The first attempt of an implementation, full of cascading fun -- bad given we only know of mass units
// and then somewhere else we need to ignore those that are untranslatable (at this time) such as MEQ and UNT.
// Do we want to convert UNT -> MU??
//	private void convertDenominator(String du, Double dv, boolean done) {
//		this.snomedDU = du;
//		this.snomedDV = String.valueOf(dv);
//		if( this.snomedDV.equals("1.0")) {
//			this.snomedDV = "1";
//		}
//		if( unitsNotForNormalization.contains(this.snomedDU) ) return; 
//		while( !done ) {
//			if( this.snomedDU.equalsIgnoreCase("kg") ) {
//				if( dv >= 1000 ) {
//					done = true;
//				}
//				else if( dv < 1 ) {
//					this.snomedDU = "G";
//					this.snomedDV = multiply(dv);
//				}
//			}
//			else if( this.snomedDU.equalsIgnoreCase("g") ) {
//				if( dv >= 1000 ) {
//					this.snomedDU = "KG";
//					this.snomedDV = divide(dv);
//				}
//				else if( dv < 1 ) {
//					this.snomedDU = "MG";
//					this.snomedDV = multiply(dv);
//				}
//			}
//			else if( this.snomedDU.equalsIgnoreCase("mg") ) {
//				if( dv >= 1000 ) {
//					this.snomedDU = "G";
//					this.snomedDV = divide(dv);
//				}
//				else if( dv < 1 ) {
//					this.snomedDU = "MCG";
//					this.snomedDV = multiply(dv);				
//				}					
//			}
//			else if( this.snomedDU.equalsIgnoreCase("mcg") ) {
//				if( dv >= 1000 ) {
//					this.snomedDU = "MG";
//					this.snomedDV = divide(dv);		
//				}
//				else if( dv < 1 ) {
//					this.snomedDU = "NG";
//					this.snomedDV = multiply(dv);
//				}			
//			}
//			else if( this.snomedDU.equalsIgnoreCase("ng") ) {
//				if( dv >= 1000 ) {
//					this.snomedDU = "MCG";
//					this.snomedDV = divide(dv);	
//				}
//				else if( dv < 1 ) {
//					this.snomedDU = "PG";
//					this.snomedDV = multiply(dv);				
//				}
//			}
//			else if( this.snomedDU.equalsIgnoreCase("pg") ) {
//				if( dv >= 1000 ) {
//					this.snomedDU = "NG";
//					this.snomedDV = divide(dv);
//				}
//				else if( dv < 1 ) {
//					done = true;
//				}
//			}
//			else {
//				done = true;
//			}
//
//			if( !done ) {
//				done = checkDone(this.snomedDV);				
//			}			
//
//		}
//
//		DecimalFormat df = new DecimalFormat("#.###");
//		df.setRoundingMode(RoundingMode.HALF_UP);
//		String stringValue = df.format(Double.valueOf(this.snomedDV));
//		this.snomedDV = stringValue;	
//		
////		if( !this.snomedDU.equals(du)) {		
////			System.out.println(this.bossName + "\t" + this.forWhat);
////			System.out.println("\trx DU => " + du + "\tSCT DU => " + this.snomedDU);
////			System.out.println("\trx DV => " + dv + "\tSCT DV => " + this.snomedDV);
////		}
//
//	}

}
