/* 
 * 1. Get all the SCD(s)
 * 2. For each, populate the
 * 	a. IN/MIN
 *  b. PIN
 *  c. BN (there shouldn't be one, and we aren't really concerned with this
 *  d. SBDC
 *  e. SBD
 *  f. SBDG
 *  g. DFG - dose form group
 *  	i. for each of these guys build a DF
 *  h. SCDG
 *  i. SCD (it's only one, so we'll go with the rxcui as the identifier - also the key to the treemap
 *  j. SCDC
 *  	i. for each of these guys we'll use its rxcui to lookup the BoSS and Strength
 *          a. BoSS (Basis of Strength Substance)
 *  		a. Strength is composed of Denominator_Unit, Denominator_Value, Numerator_Units, Numerator_Value
 */

package gov.nih.nlm.mor.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import gov.nih.nlm.mor.RxNorm.RxNormDoseForm;
import gov.nih.nlm.mor.RxNorm.RxNormIngredient;
import gov.nih.nlm.mor.RxNorm.RxNormSCD;
import gov.nih.nlm.mor.RxNorm.RxNormSBD;
import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.Snomed.SnomedDefinitionTemplate;
import gov.nih.nlm.mor.Snomed.SnomedManufacturedDoseForm;
import gov.nih.nlm.mor.Snomed.SnomedUnitOfPresentation;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

public class FetchRxNormData implements java.io.Serializable {
	
	private static final long serialVersionUID = -4261607265667390359L;
	public TreeMap<Integer, RxNormSCD> rxcui2SCD = new TreeMap<Integer, RxNormSCD>();
	public TreeMap<Integer, RxNormSBD> rxcui2SBD = new TreeMap<Integer, RxNormSBD>();	
	public TreeMap<Integer, RxNormIngredient> rxcui2IN = new TreeMap<Integer, RxNormIngredient>();
	public TreeMap<Integer, RxNormIngredient> rxcui2PIN = new TreeMap<Integer, RxNormIngredient>();
	public TreeMap<Integer, RxNormIngredient> rxcui2MIN = new TreeMap<Integer, RxNormIngredient>();	
	public TreeMap<Integer, RxNormDoseForm> rxcui2DF = new TreeMap<Integer, RxNormDoseForm>();
	public TreeMap<Integer, SnomedDFPair> rxdf2SnomedDFPair = new TreeMap<Integer, SnomedDFPair>();
	public RestUrl restUrl = new RestUrl();
	HashMap<RxNormDoseForm, SnomedDFPair> rxNormDoseForm2SnomedDFPair = new HashMap<RxNormDoseForm, SnomedDFPair>();	
//	String allConceptsSCDMethod = restUrl.getRestUrl() + "/REST/allconcepts.json?tty=SCD";
//	String allIngredientsMethod = restUrl.getRestUrl() + "/REST/allconcepts.json?tty=IN+PIN";
	transient String allConcepts = restUrl.getRestUrl() + "/REST/allstatus.json";
	transient String allTTY = restUrl.getRestUrl() + "/REST/allconcepts.json?tty=SCD+DF+IN+MIN+PIN";
	transient String closeTerm = restUrl.getRestUrl() + "/REST/rxcui.json?name=";
	transient String closeTermSearchParam = "&search=2";
	boolean includeObsoleteAndNotActive = false;
	public PrintWriter pwVax;
	private ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	
	public FetchRxNormData(boolean includeAll) {
		//TODO: Thread this
		this.includeObsoleteAndNotActive = includeAll;
		this.gather();
	}	
	
	private void gather() {
		long start = System.currentTimeMillis();
		JSONObject result = null;
		
		try {
			pwVax = new PrintWriter(new File("cvx-concepts"));
		} catch(Exception e) {
			e.printStackTrace();
		}		
		
		try {
			if(!includeObsoleteAndNotActive) {
				result = getresult(allTTY, null);
			} else {
				System.out.println("Getting Active+Obsolete+NotActive I think...");
				result = getresult(allConcepts, null);
			}
		} catch (Exception e) {
			//System.out.println("Unable to fetch RxNorm data with TTY SCD+IN+PIN+DF");
			if(!includeObsoleteAndNotActive) {
				System.out.println("Unable to fetch RxNorm data with TTY SCD+IN+PIN+DF");
			} else {
				System.out.println("Unable to fetch all RxNorm data");
			}
			e.printStackTrace();
		}
		
		try {
			setDFPairMap("./config/doseFormMap.txt");

			for( Integer i : rxdf2SnomedDFPair.keySet()) {
				System.out.print(i);
				SnomedDFPair pair = rxdf2SnomedDFPair.get(i);
				if(pair.hasDF()) {
					System.out.print("\t" + pair.getSnomedManufacturedDoseForm().getCode() + "\t" + pair.getSnomedManufacturedDoseForm().getName() );
				}
				if(pair.hasUP()) {
					System.out.print("\t" + pair.getSnomedUnitOfPresentation().getCode() + "\t" + pair.getSnomedUnitOfPresentation().getName() );
				}
				if( pair.getSnomedDefinitionTemplate() != null ) {
					System.out.print("\t" + pair.getSnomedDefinitionTemplate().getTemplateType() );
				}
				System.out.println();
			}
		}
		catch (Exception e) {
			System.out.println("Unable to read dose form map configuration file. Check that doseFormMap.txt exists in this directory.");
			System.exit(0);
		}
		
		JSONObject group = null;
		JSONArray minConceptArray = null;		
		
		try {
		group = (JSONObject) result.get("minConceptGroup");
		} catch(Exception e) {
			e.printStackTrace();
		}
		minConceptArray = (JSONArray) group.get("minConcept");
		for(int i = 0; i < minConceptArray.length(); i++ ) {
//			long s = System.currentTimeMillis();
			if( (i != 0) && (i % 1000) == 0) {
				System.out.println(i + " RxNorm concepts read to memory.." + " in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
			}
			JSONObject minConcept = (JSONObject) minConceptArray.get(i);
			
			String cuiString = minConcept.get("rxcui").toString();
			Integer rxcui = new Integer(cuiString);
			String name = minConcept.get("name").toString();
			String type = minConcept.get("tty").toString();
			
//			if( cuiString.equals("1801823")) {
//				System.out.println("debug");
//			}
			
			if( type.equals("SCD") ) {
				RxNormSCD clinicalDrug = new RxNormSCD(pwVax, rxcui, name);
//				clinicalDrug.setSnomedCodes();
				rxcui2SCD.put(rxcui, clinicalDrug);		
			}
			else if(type.equals("SBD") ) {
				RxNormSBD brandedDrug = new RxNormSBD(rxcui, name);
				rxcui2SBD.put(rxcui, brandedDrug);
			}
			else if( type.equals("IN") ) {
//				RxNormIngredient rxnormIngredient = removeAnhydrous(rxcui, name);
				RxNormIngredient rxnormIngredient = new RxNormIngredient(rxcui, name);
				rxnormIngredient.setSnomedCodes();
				rxcui2IN.put(rxcui, rxnormIngredient);
//				if( rxcui.equals(3423)) {
//					rxnormIngredient.print();
//				}				
			}
			else if( type.equals("PIN") ) {
//				RxNormIngredient rxnormIngredient = removeAnhydrous(rxcui, name);				
				RxNormIngredient rxnormIngredient = new RxNormIngredient(rxcui, name);
				rxnormIngredient.setSnomedCodes();				
				rxcui2PIN.put(rxcui, rxnormIngredient);
//				if( rxcui.equals(203177)) {
//					rxnormIngredient.print();
//				}
			}
			else if( type.equals("MIN") ) {				
				RxNormIngredient rxnormIngredient = new RxNormIngredient(rxcui, name);
				rxnormIngredient.setSnomedCodes();
				rxcui2MIN.put(rxcui, rxnormIngredient);
			}			
			else if( type.equals("DF") ) {			
				RxNormDoseForm rxnormDF = new RxNormDoseForm(rxcui, name);
				rxcui2DF.put(rxcui, rxnormDF);
			}
//			System.out.println(rxcui + " " + type + " in " + (System.currentTimeMillis() - s) + " milliseconds.");
		}
		
		try {
			setAllergens("./config/allergenics.txt");
		}
		catch(Exception e) {
			e.printStackTrace();
		}		

		System.out.println(minConceptArray.length() + " RxNorm concepts in memory.");		

// Don't remove this in case we ever want to call ingredients separate from the one call above
//		group = (JSONObject) ingredientResult.getJSONObject("minConceptGroup");
//		minConceptArray = (JSONArray) group.getJSONArray("minConcept");
//		for(int i = 0; i < minConceptArray.length(); i++) {
//			if( (i != 0) && (i % 1000) == 0) {
//				System.out.println(i + " RxNormIngredient concepts built..");
//			}
//			
//			JSONObject minConcept = (JSONObject) minConceptArray.get(i);
//			
//			String cuiString = minConcept.get("rxcui").toString();
//			Integer rxcui = new Integer(cuiString);
//			String name = minConcept.getString("name").toString();
//			
//			RxNormIngredient rxnormIngredient = new RxNormIngredient(rxcui, name);
//			rxnormIngredient.setSnomedCodes(rxcui);
//			rxcui2IN.put(rxcui, rxnormIngredient);
//			
//		}
		

		
		System.out.println("Finished building RxNorm model in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
		System.out.println();
	}

	//utility method
	private RxNormIngredient removeAnhydrous(Integer cui, String ing) {
		RxNormIngredient returnIngredient = null;
		
		boolean set = false;
		String newIng = null;
		String newCui = null;
		String val = ing.toLowerCase();
		if( val.contains("hydrous") || val.contains("hydrate") || val.contains("basic") ) {
			//brute force
			val = val.replaceAll("(?i),", "");		
			val = val.replaceAll("(?i)(obsolete)", "");
			val = val.replaceAll("(?i)monobasic", "");
			val = val.replaceAll("(?i)dibasic", "");
			val = val.replaceAll("(?i)(tribasic)", "");				
			val = val.replaceAll("(?i)tribasic", "");			
			val = val.replaceAll("(?i)(anhydrous)", "");
			val = val.replaceAll("(?i)(monohydrate)", "");
			val = val.replaceAll("(?i)(anhydrous)", "");			
			val = val.replaceAll("(?i)anhydrous", "");
			val = val.replaceAll("(?i)(trihydrate)", "");			
			val = val.replaceAll("(?i)trihydrate", "");
			val = val.replaceAll("(?i)(dihydrate)", "");			
			val = val.replaceAll("(?i)dihydrate", "");
			val = val.replaceAll("(?i)(monohydrate)", "");			
			val = val.replaceAll("(?i)monohydrate", "");
			val = val.replaceAll("(?i)(hemihydrate)", "");			
			val = val.replaceAll("(?i)hemihydrate", "");
			val = val.replaceAll("(?i)(tetrahydrate)", "");			
			val = val.replaceAll("(?i)tetrahydrate", "");
			val = val.replaceAll("(?i)(tetradecahydrate)", "");			
			val = val.replaceAll("(?i)tetradecahydrate", "");			
			val = val.replaceAll("(?i)(pentahydrate)", "");			
			val = val.replaceAll("(?i)pentahydrate", "");
			val = val.replaceAll("(?i)(dodecahydrate)", "");			
			val = val.replaceAll("(?i)dodecahydrate", "");
			val = val.replaceAll("(?i)(sesquihydrate)", "");			
			val = val.replaceAll("(?i)sesquihydrate", "");
			val = val.replaceAll("(?i)(octahydrate)", "");			
			val = val.replaceAll("(?i)octahydrate", "");
			val = val.replaceAll("(?i)(hexahydrate)", "");			
			val = val.replaceAll("(?i)hexahydrate", "");
			val = val.replaceAll("(?i)(octahydrate)", "");			
			val = val.replaceAll("(?i)octahydrate", "");
			val = val.replaceAll("(?i)(nonahydrate)", "");			
			val = val.replaceAll("(?i)nonahydrate", "");
			val = val.replaceAll("(?i)(oxyhydrate)", "");			
			val = val.replaceAll("(?i)oxyhydrate", "");
			val = val.replaceAll("(?i)monobasic-dibasic", "");
			val = val.replaceAll("(?i)(dehydrate)", "");			
			val = val.replaceAll("(?i)dehydrate", "");
			val = val.replaceAll("(?i)(hydrate)", "");			
			val = val.replaceAll("(?i)hydrate", "");
			
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
			if( result != null ) {
				JSONObject idGroup = (JSONObject) result.get("idGroup");
				try {
					newIng = idGroup.get("name").toString();
				} catch(Exception e) {
					System.err.println("Skipping: " + closeTerm + val + closeTermSearchParam);
				}
				if( idGroup.has("rxnormId") ) {
					JSONArray rxnormIdArray = (JSONArray) idGroup.get("rxnormId");
					newCui = rxnormIdArray.getString(0); //the best match is the name.. not many cases of more than one cui that i've seen
					returnIngredient = new RxNormIngredient(Integer.valueOf(newCui), newIng);
					set = true;
//					System.out.println("Added to ing maps a new ingredient from an anyhdrous form:");
//					System.out.println("\t" + newCui + " : " + newIng);
				}
				else {
					System.out.println("Unable to find an rxcui for non-anhydrous text: " + newIng);
				}
			}
		}
		if( !set ) {
			 returnIngredient = new RxNormIngredient(cui, ing);				
		}
		
		return returnIngredient;
	}
	
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
	
	//write = true, serialize our calls into individual files
	//read = true, read from our serialized files
	public static JSONObject getresult(String URLtoRead, boolean write, boolean read) throws IOException {
		URL url;
		HttpURLConnection connexion;
		BufferedReader reader;
		FileReader fr = null;
		PrintWriter pw = null;
		
		String line;
		String result="";
		url= new URL(URLtoRead);
		
		String method;
		String parameter;
		String filename = null;
		File file;
		
		if(read || write ) {
			method = URLtoRead.substring(URLtoRead.lastIndexOf("/"), URLtoRead.lastIndexOf("."));
			parameter = URLtoRead.substring(URLtoRead.indexOf("?"));
			parameter = parameter.replace("=", "-").replace("&", "-");
			filename = method + "-" + parameter;		
		}
		
		if(filename == null) {
			file = new File(filename);
			connexion= (HttpURLConnection) url.openConnection();
			connexion.setRequestMethod("GET");			
			reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));				
		} else {
			fr = new FileReader(new File(filename));
			reader = new BufferedReader(fr);
		}
	
		while ((line =reader.readLine())!=null) {
			result += line;		
		}
		
		if(write) {
			try {
				pw = new PrintWriter(new File(filename));
				pw.println(result);
				pw.flush();
				pw.close();				
			} catch(Exception e) {
				System.out.println("Unable to serialize to local filesystem the call: " + URLtoRead);
				e.printStackTrace();
			}
		}			
		
		JSONObject json = new JSONObject(result);
		return json;
	}	
	
	public TreeMap<Integer, RxNormDoseForm> getDFMap() {
		return rxcui2DF;
	}	
	
	public TreeMap<Integer, RxNormSCD> getSCDMap() {
		return rxcui2SCD;
	}
	
	public TreeMap<Integer, RxNormSBD> getSBDMap() {
		return rxcui2SBD;
	}	
	
	public TreeMap<Integer, RxNormIngredient> getINMap() {
		return rxcui2IN;
	}
	
	public TreeMap<Integer, RxNormIngredient> getMINMap() {
		return rxcui2MIN;
	}	
	
	public TreeMap<Integer, RxNormIngredient> getPINMap() {
		return rxcui2PIN;
	}	
	
	public TreeMap<Integer, SnomedDFPair> getDFPairMap() {	
		return this.rxdf2SnomedDFPair;
	}
	
	public void setDFPairMap(String filename) {
		readTable(filename);		
	}
	
	public void setAllergens(String filename) {
		readAllergenTable(filename);
	}
	
	public void readAllergenTable(String filename) {
		FileReader file = null;
		BufferedReader buff = null;
		try {
			file = new FileReader(filename);
			buff = new BufferedReader(file);
			boolean eof = false;
			int colIndex = -1;
			while (!eof) {
				String line = buff.readLine();
				if (line == null)
					eof = true;
				else {	
					if( line != null && line.contains("\t") ) {
						String[] values = line.split("\t", -1);
						String cuiString = values[0].trim();
						String inString = values[1].trim();  //we have this through REST already
						if( cuiString != null ) {
							if( rxcui2IN.containsKey(Integer.valueOf(cuiString)) ) {
								RxNormIngredient in = rxcui2IN.get(Integer.valueOf(cuiString));
								in.setAllergenic(true);
								rxcui2IN.put(Integer.valueOf(cuiString), in);
							}
							if( rxcui2MIN.containsKey(Integer.valueOf(cuiString))) {
								RxNormIngredient in = rxcui2MIN.get(Integer.valueOf(cuiString));
								in.setAllergenic(true);
								rxcui2MIN.put(Integer.valueOf(cuiString), in);
							}
						}
					}
					else {
						System.err.println("No Property configured for configuration index " + colIndex);
						System.err.println("Exiting");
						System.exit(-1);
					}						
				}
			}
//			//			for( int i=0; i < columns.size(); i++ ) {
//			//				columns.elementAt(i).print();
//			//			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Closing the streams
			try {
				buff.close();
				file.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}

	public void readTable(String filename) {	
		FileReader file = null;
		BufferedReader buff = null;
		try {
			file = new FileReader(filename);
			buff = new BufferedReader(file);
			boolean eof = false;
			int colIndex = -1;
			while (!eof) {
				String line = buff.readLine();
				if (line == null)
					eof = true;
				else {	
					if( line != null && line.contains("\t") ) {  //suppress ambiguous dose forms found by JN (UpdateDoseForms.java:113)
						if(line.startsWith("#")) continue;
						String[] values = line.split("\t", -1);							
						String rxDf = values[0].trim();
						String rxDFName = values[1].trim();
						
						//Introduced Mar-06-2023
						String rxPreferredDu = values[2].trim();
						
						String sctDf = values[3].trim();
						String sctDfName = values[4].trim();
						String sctUp = values[5].trim();
						String sctUpName = values[6].trim();
						String templateName = values[7].trim();
						RxNormDoseForm rxdf = null;
						SnomedManufacturedDoseForm smdf = null;
						SnomedUnitOfPresentation suop = null;
						SnomedDefinitionTemplate sdt = null;
						SnomedDFPair sdfp = null;
						boolean rxValue = true;
						boolean snDFValue = true;
						boolean snUPValue = true;
						boolean template = true;
						boolean forceDuNecessary = false;
						for( int i=0; i < values.length; i++) {
							if( values[i].equals(""))  {
								if( i == 0 || i == 1 ) {
									rxValue = false;
								}
	//							if( i == 2 ) {
	//								df2PreferredDu.put(rxDf, rxPreferredDu);
	//							}
								if( i == 3 || i == 4 ) {
									snDFValue = false;
								}
								if( i == 5 || i == 6 ) {
									snUPValue = false;
								}
								if( i == 7 ) {
									template = false;
								}
	//							if(!snUPValue) {
	//								forceDuNecessary = true;
	//							}
							}
						}
	
	//					if( rxDf.equals("316949") ) {
	//						System.out.print("BREAK");
	//					}
						
						Integer rxCui = new Integer(rxDf);
								
						if( snDFValue ) {
							smdf = new SnomedManufacturedDoseForm(new Long(sctDf), sctDfName);
						}
						if( snUPValue ) {
							suop = new SnomedUnitOfPresentation(new Long(sctUp), sctUpName);
						}
						if( rxCui != null && template ) {
							sdt = new SnomedDefinitionTemplate(templateName);
							SnomedDFPair dfPair = new SnomedDFPair(smdf, suop, sdt);
							rxdf2SnomedDFPair.put(rxCui, dfPair);
							
							//Introduced with March-06-2023
							RxNormDoseForm rxNormDoseForm = new RxNormDoseForm(rxCui, rxDFName);
							rxNormDoseForm.setRxnavPreferredDenominatorUnitName(rxPreferredDu);
							rxNormDoseForm2SnomedDFPair.put(rxNormDoseForm, dfPair);
							
						}
						else if( rxCui != null && !template) {
							SnomedDFPair dfPair = null;
							//The injectables have no template in the config file.  We need to abide by RxNorm
							//rules on how to map.  They are: 
							//1. If a dose form is just an Injection, check if it is just mass (e.g., mass over 1 vial), if so template is a Presentation
							//2. If a dose form is just an Injection, check if it is mass over volume and has a Quantity Factor, if so template is a Concentration and Presentation
							//3. If a dose form is an Injectable solution or suspension, the concentration must be without a Quantity Factory to apply just the concentration strength, if so the template is a Concentration
							/*
							 *	1649574	Injection	385223009	Powder for conventional release solution for injection (dose form)	733026001	Vial (unit of presentation)
							 * 	316950	Injectable Suspension	385220007	Conventional release suspension for injection (dose form)	733026001	Vial (unit of presentation)	
								316949	Injectable Solution	385223009	Powder for conventional release solution for injection (dose form)	733026001	Vial (unit of presentation)	
	316950	Injectable Suspension	385220007	Conventional release suspension for injection (dose form)	733026001	Vial (unit of presentation)	
	316949	Injectable Solution	385219001	Conventional release solution for injection (dose form)			
	1649574	Injection	385223009	Powder for conventional release solution for injection (dose form)	733026001	Vial (unit of presentation)	
							 */
							
	//						//(1)
	//						if( smdf.getCode().equals(Long.valueOf("385223009")) && suop.getCode().equals(Long.valueOf("733026001")) ) {
	//							//we don't know which template an SCD will use
	//							dfPair = new SnomedDFPair(smdf, suop, null);
	//							rxdf2SnomedDFPair.put(rxCui,  dfPair);
	//						}
	//						//(2)
	//						else if( smdf.getCode().equals(Long.valueOf("385220007")) && suop.getCode().equals(Long.valueOf("733026001")) ) {
	//							dfPair = new SnomedDFPair(smdf, suop, null);								
	//							rxdf2SnomedDFPair.put(rxCui,  dfPair);								
	//						}
	//						//(3)
	//						else if( smdf.getCode().equals(Long.valueOf("385223009")) && suop.getCode().equals(Long.valueOf("733026001")) ) {
								dfPair = new SnomedDFPair(smdf, suop, null);								
								rxdf2SnomedDFPair.put(rxCui,  dfPair);								
						}
	
	//implicitly applied later
	//					if(forceDuNecessary) {
	//						df2PreferredDu.put(rxCui.toString(), rxPreferredDu);
	//					} else {
	//						if(suop != null) {
	//							df2PreferredDu.put(rxCui.toString(), String.valueOf(suop.getCode()));
	//						}
	//					}
						
					}
				else {
					System.err.println("No Property configured for configuration index " + colIndex);
					System.err.println("Exiting");
					System.exit(-1);
				}						
			}
		}
		//			for( int i=0; i < columns.size(); i++ ) {
		//				columns.elementAt(i).print();
		//			}
	} catch (Exception e) {
		e.printStackTrace();
	} finally {
		// Closing the streams
		try {
			buff.close();
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	}	

}
