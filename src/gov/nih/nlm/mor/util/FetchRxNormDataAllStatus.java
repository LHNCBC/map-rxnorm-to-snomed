package gov.nih.nlm.mor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import gov.nih.nlm.mor.RxNorm.NDC;
import gov.nih.nlm.mor.RxNorm.Property;
import gov.nih.nlm.mor.RxNorm.RxNormBoss;
import gov.nih.nlm.mor.RxNorm.RxNormDoseForm;
import gov.nih.nlm.mor.RxNorm.RxNormIngredient;
import gov.nih.nlm.mor.RxNorm.RxNormSBD;
import gov.nih.nlm.mor.RxNorm.RxNormSCD;
import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.Snomed.SnomedDefinitionTemplate;
import gov.nih.nlm.mor.Snomed.SnomedManufacturedDoseForm;
import gov.nih.nlm.mor.Snomed.SnomedUnitOfPresentation;

public class FetchRxNormDataAllStatus {
	
	RestUrl restUrl = new RestUrl();
	transient String rxnormVersionCall = restUrl.getRestUrl() + "/REST/version.json";
	transient String allConcepts = restUrl.getRestUrl() + "/REST/allstatus.json";
	transient String historyCall = restUrl.getRestUrl() + "/REST/historystatus.json";
//	transient String propertyNames = restUrl.getRestUrl() + "/REST/propnames.json";
	transient String idNames = restUrl.getRestUrl() + "/REST/idtypes.json"; //for xRefs via hasDbXref oboInOWL
	TreeMap<String, RxNormSCD> cui2SCD = new TreeMap<String, RxNormSCD>();
	TreeMap<String, RxNormSBD> cui2SBD = new TreeMap<String, RxNormSBD>();
	TreeMap<String, ArrayList<RxNormSBD>> scd2SBDs = new TreeMap<String, ArrayList<RxNormSBD>>();
	
	//uncharted - informational for now
//	TreeMap<String, RxNormGPCK> cui2GPCK = new TreeMap<String, RxNormGPCK>();
//	TreeMap<String, RxNormBPCK> cui2BPCK = new TreeMap<String, RxNormBPCK>();
	
	
	TreeMap<Integer, RxNormIngredient> cui2IN = new TreeMap<Integer, RxNormIngredient>();
	TreeMap<Integer, RxNormIngredient> cui2PIN = new TreeMap<Integer, RxNormIngredient>();
	TreeMap<Integer, RxNormIngredient> cui2MIN = new TreeMap<Integer, RxNormIngredient>();	
	TreeMap<Integer, RxNormDoseForm> cui2DF = new TreeMap<Integer, RxNormDoseForm>();
	TreeMap<Integer, SnomedDFPair> rxdf2SnomedDFPair = new TreeMap<Integer, SnomedDFPair>();
	TreeMap<String, String> attributesIds2Name = new TreeMap<String, String>();
	TreeMap<String, String> codesIds2Name = new TreeMap<String, String>();
	TreeMap<String, String> namesIds2Name = new TreeMap<String, String>();
	TreeMap<String, String> sourcesIds2Name = new TreeMap<String, String>();
	String rxNormVersion = "";
	transient String closeTerm = restUrl.getRestUrl() + "/REST/rxcui.json?name=";
	transient String closeTermSearchParam = "&search=2";
	boolean scanUnits = false;
	String scanUnitType = "";
	boolean processSbds = false;
	TreeMap<String, Integer> tallyDenominatorUnits = new TreeMap<String, Integer>();
	TreeMap<String, ArrayList<RxNormSCD>> du2Scd = new TreeMap<String, ArrayList<RxNormSCD>>();
	TreeMap<String, ArrayList<RxNormSBD>> du2Sbd = new TreeMap<String, ArrayList<RxNormSBD>>();	
	
	//Introduced with Mar-06-2023
	HashMap<String, String[]> df2PreferredDu = new HashMap<String, String[]>();
	
	HashMap<RxNormDoseForm, SnomedDFPair> rxNormDoseForm2SnomedDFPair = new HashMap<RxNormDoseForm, SnomedDFPair>();

	Vector<String> unitNamesToInspect = new Vector<String>();
	PrintWriter pwUnitNames = null;
	private ExponentialBackoff exponentialBackoff = new ExponentialBackoff();	
	
	public FetchRxNormDataAllStatus() {
		
	}	
	
	
	public FetchRxNormDataAllStatus(String args[]) {
		this.config(args);
		this.run(); //sanity check		
	}
	
	public static void main(String[] args) {
		FetchRxNormDataAllStatus test = new FetchRxNormDataAllStatus();
		test.config(args);
		test.run();
	}
	
	public void config(String[] args) {
		if(args.length > 0) {
			if(args[0].equalsIgnoreCase("scan-denominators") || args[0].equalsIgnoreCase("scan-numerators")) {
				scanUnits = true;
				scanUnitType = args[0];
			}
			if(Boolean.getBoolean(args[1])) {
				processSbds = true;
				System.out.println("processing booleans");
			}
			System.out.println(scanUnitType + "-ing!");
//			this.unitNamesToInspect.add("1");
//			this.unitNamesToInspect.add("ACTUAT");
//			this.unitNamesToInspect.add("AU");
//			this.unitNamesToInspect.add("EACH");
//			this.unitNamesToInspect.add("KG");			
//			this.unitNamesToInspect.add("G");			
//			this.unitNamesToInspect.add("MG");
//			this.unitNamesToInspect.add("MCG");
//			this.unitNamesToInspect.add("ML");
//			this.unitNamesToInspect.add("L");			
//			this.unitNamesToInspect.add("MEQ");
//			this.unitNamesToInspect.add("SQ-HDM");
//			this.unitNamesToInspect.add("SQCM");
//			this.unitNamesToInspect.add("VIAL");
			
			try {
				this.pwUnitNames = new PrintWriter(new File("scan-units.txt"));
			} catch(Exception e) {
				System.err.println(scanUnitType + " - Ooops.");
				e.printStackTrace();				
			}
		}
		long start = System.currentTimeMillis();
		JSONObject rxNormVersionOb = null;
		JSONObject allConceptsResponse = null;
		try {
			rxNormVersionOb = getresult(rxnormVersionCall, null);
		} catch(Exception e) {
			System.err.println("Error returning RxNorm version.");
			e.printStackTrace();
		}
		try {
			allConceptsResponse = getresult(allConcepts, null);
		} catch(Exception e) {
			System.out.println("Error returning allstatus");
		}
		if(rxNormVersionOb != null) {
			rxNormVersion = rxNormVersionOb.getString("version");
		}
		else {
			rxNormVersion = "default-unknown";
		}
		if(allConceptsResponse != null) {
			setDFPairMap("./config/doseFormMap.txt");	

			JSONObject group = null;
			JSONArray minConceptArray = null;		
			
			group = (JSONObject) allConceptsResponse.get("minConceptGroup");
			minConceptArray = (JSONArray) group.get("minConcept");
			
			System.out.println("Processing cuis in order returned");
			for(int i=0; i < minConceptArray.length(); i++) {
				JSONObject obj = minConceptArray.getJSONObject(i);
				String cui = obj.getString("rxcui");
				if(cui.equals("1000022")) {
					System.out.println("HALT");
				}
				if(cui.equals("198039")) {
					System.out.println("Remapped - HALT");
				}		
				if(cui.equals("1117551")) {
					System.out.println("Remapped - HALT");
				}				
				if( (i != 0) && (i % 10000) == 0) {
					System.out.println(i + " RxNorm concepts read to memory.." + " in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
				}
				String name = obj.getString("name");
				String tty = obj.getString("tty");
				if(tty.equals("SCD")) {
				//	System.out.println("SCD " + cui);					
					String scdHistoryCall = historyCall.replace("/historystatus", "/rxcui/" + cui + "/historystatus");
//					RxNormSCD scd = new RxNormSCD(cui, name, scdHistoryCall);

					//Introduced RxNav DU names Mar-06-2023
					RxNormSCD scd = new RxNormSCD(cui, name, scdHistoryCall, rxNormDoseForm2SnomedDFPair);
					
					cui2SCD.put(cui, scd);
					setProperties(scd);
					
					if(scanUnits) {
						for(RxNormBoss b : scd.getRxNormBoss()) {
							String unit = scanUnitType.equalsIgnoreCase("scan-denominators") ? b.getDenominatorUnit() : b.getNumeratorUnit(); 

							Vector<RxNormDoseForm> vdf = scd.getRxNormDoseForm();
							String doseForm = "unknown";
							if(vdf.size() > 0) {
								doseForm = vdf.get(0).getName();
							}
							if(unit != null) {
								if(tallyDenominatorUnits.containsKey(unit)) {
									this.pwUnitNames.println(doseForm + "\t" + unit + "\t" + scd.getCui() + "\t" + scd.getName());
									Integer count = tallyDenominatorUnits.get(unit);
									count++;
									tallyDenominatorUnits.put(unit, count);
								}
								else {
									this.pwUnitNames.println(doseForm + "\t" + unit + "\t" + scd.getCui() + "\t" + scd.getName());							
									tallyDenominatorUnits.put(unit, new Integer(1));
								}
							}
						}
					}
					
				}
//extra argument (false) was for "Fast" production of Pilot				
//				else if(processSbds && tty.equals("SBD")) {
				else if(tty.equals("SBD")) {				
				//	System.out.println("SBD " + cui);					
					String sbdHistoryCall = historyCall.replace("/historystatus", "/rxcui/" + cui + "/historystatus");
					//RxNormSBD sbd = new RxNormSBD(cui, name, sbdHistoryCall);
					
					//Introduced RxNav DU names Mar-06-2023
					RxNormSBD sbd = new RxNormSBD(cui, name, sbdHistoryCall, rxNormDoseForm2SnomedDFPair);
					
					cui2SBD.put(cui, sbd);
					setProperties(sbd);	
					
//					if(scanDenominatorUnits) {
//						for(RxNormBoss b : sbd.getvBoss()) {
//							String dUnit = b.getDenominatorUnit();							
//							if(dUnit != null) {
//								if(tallyDenominatorUnits.containsKey(dUnit)) {
//									if(this.unitNamesToInspect.contains(dUnit)) {
//										this.pwUnitNames.println(dUnit + "\t" + sbd.getRxcui() + "\t" + sbd.getName());
//									}
//									Integer count = tallyDenominatorUnits.get(dUnit);
//									count++;
//									tallyDenominatorUnits.put(dUnit, count);
//								}
//								else {
//									if(this.unitNamesToInspect.contains(dUnit)) {
//										this.pwUnitNames.println(dUnit + "\t" + sbd.getRxcui() + "\t" + sbd.getName());
//									}									
//									tallyDenominatorUnits.put(dUnit, new Integer(1));
//								}
//							}
//						}				
//					}					
					
					if(sbd.getScdRxcuis().size() > 0) {
						linkSbd(sbd);
					}
					if(sbd.getRemappedSbdCuis().size() > 0 ||
						sbd.getQuantifiedSbdCuis().size() > 0) {
						Set<RxNormSBD> sbdSiblings = new HashSet<>();
						sbdSiblings.addAll(sbd.getRemappedSbds());
						sbdSiblings.addAll(sbd.getQuantifiedSbds());
						for(RxNormSBD sibling : sbdSiblings) {
							cui2SBD.put(sibling.getRxcui().toString(), sibling);
							setProperties(sibling);
							linkSbd(sibling);
						}
					}
				}
//TODO: WIP				
//informational only
//				else if(tty.equals("GPCK")) {					
//					String sbdHistoryCall = historyCall.replace("/historystatus", "/rxcui/" + cui + "/historystatus");
//					RxNormGPCK gpck = new RxNormGPCK(cui, name, sbdHistoryCall);
//					cui2GPCK.put(cui, gpck);
//					setProperties(gpck);	
//					
//					if(scanDenominatorUnits) {
//						for(RxNormBoss b : gpck.getvBoss()) {
//							String dUnit = b.getDenominatorUnit();
//							if(dUnit != null) {
//								if(tallyDenominatorUnits.containsKey(dUnit)) {
//									if(this.unitNamesToInspect.contains(dUnit)) {
//										this.pwUnitNames.println(dUnit + "\t" + gpck.getRxcui() + "\t" + gpck.getName());
//									}
//									Integer count = tallyDenominatorUnits.get(dUnit);
//									count++;
//									tallyDenominatorUnits.put(dUnit, count);
//								}
//								else {
//									if(this.unitNamesToInspect.contains(dUnit)) {
//										this.pwUnitNames.println(dUnit + "\t" + gpck.getRxcui() + "\t" + gpck.getName());
//									}									
//									tallyDenominatorUnits.put(dUnit, new Integer(1));
//								}
//							}
//						}				
//					}					
//				}
//				//informational only
//				else if(tty.equals("BPCK")) {
//				//	System.out.println("SBD " + cui);					
//					String sbdHistoryCall = historyCall.replace("/historystatus", "/rxcui/" + cui + "/historystatus");
//					RxNormBPCK bpck = new RxNormBPCK(cui, name, sbdHistoryCall);
//					cui2BPCK.put(cui, bpck);
//					setProperties(bpck);	
//					
//					if(scanDenominatorUnits) {
//						for(RxNormBoss b : bpck.getvBoss()) {
//							String dUnit = b.getDenominatorUnit();
//							if(dUnit != null) {
//								if(tallyDenominatorUnits.containsKey(dUnit)) {
//									if(this.unitNamesToInspect.contains(dUnit)) {
//										this.pwUnitNames.println(dUnit + "\t" + bpck.getRxcui() + "\t" + bpck.getName());
//									}
//									Integer count = tallyDenominatorUnits.get(dUnit);
//									count++;
//									tallyDenominatorUnits.put(dUnit, count);
//								}
//								else {
//									if(this.unitNamesToInspect.contains(dUnit)) {
//										this.pwUnitNames.println(dUnit + "\t" + bpck.getRxcui() + "\t" + bpck.getName());
//									}									
//									tallyDenominatorUnits.put(dUnit, new Integer(1));
//								}
//							}
//						}				
//					}					
//					
//					if(bpck.getGpckRxcuis().size() > 0) {
//						linkBpck(bpck);
//					}
//					if(bpck.getRemappedBpckCuis().size() > 0 ||
//						bpck.getQuantifiedBpckCuis().size() > 0) {
//						Set<RxNormSBD> sbdSiblings = new HashSet<>();
//						sbdSiblings.addAll(bpck.getRemappedBpcks());
//						sbdSiblings.addAll(bpck.getQuantifiedBpcks());
//						for(RxNormSBD sibling : sbdSiblings) {
//							cui2SBD.put(sibling.getRxcui().toString(), sibling);
//							setProperties(sibling);
//							linkSbd(sibling);
//						}
//					}
//				}				
				else if(tty.equals("IN")) {
					RxNormIngredient rxnormIngredient = new RxNormIngredient(Integer.valueOf(cui), name);
					rxnormIngredient.setSnomedCodes();
					cui2IN.put(Integer.valueOf(cui), rxnormIngredient);					
				}
				else if(tty.equals("MIN")) {
					try {
					RxNormIngredient rxnormIngredient = new RxNormIngredient(Integer.valueOf(cui), name);
					rxnormIngredient.setSnomedCodes();
					cui2MIN.put(Integer.valueOf(cui), rxnormIngredient);
					} catch(Exception e) {
						System.out.println("What's up with MIN: " + cui + "\t" + name);
						e.printStackTrace();
					}
				}
				else if(tty.equals("PIN")) {
					RxNormIngredient rxnormIngredient = new RxNormIngredient(Integer.valueOf(cui), name);
					rxnormIngredient.setSnomedCodes();
					cui2PIN.put(Integer.valueOf(cui), rxnormIngredient);						
				}
				else if(tty.equals("DF")) {
					RxNormDoseForm rxnormDF = new RxNormDoseForm(Integer.valueOf(cui), name);
					cui2DF.put(Integer.valueOf(cui), rxnormDF);					
				}
			}
			
			try {
				setAllergens("./config/allergenics.txt");
			}
			catch(Exception e) {
				e.printStackTrace();
			}			
		}
		
		System.out.println("Finished building RxNorm model in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
		System.out.println();		
	}
	
	private void linkSbd(RxNormSBD sbd) {
		for(String c : sbd.getScdRxcuis()) {
			if(scd2SBDs.containsKey(c)) {
				ArrayList<RxNormSBD> tmp = scd2SBDs.get(c);
				tmp.add(sbd);
				scd2SBDs.put(c, tmp);
			}
			else {
				ArrayList<RxNormSBD> tmp = new ArrayList<RxNormSBD>();
				tmp.add(sbd);
				scd2SBDs.put(c, tmp);
			}
		}		
	}
	
	//We aren't using Generics.
	private void setProperties(RxNormSCD scd) {
		Set<Property> scdProperties = scd.getProperties();
		for(Property p : scdProperties) {
			switch(p.getCategory()) {
			case "ATTRIBUTES":
				attributesIds2Name.put(p.getId(), p.getName());
				break;
			case "CODES":
				codesIds2Name.put(p.getId(), p.getName());
				break;
			case "NAMES":
				namesIds2Name.put(p.getId(), p.getName());
				break;
			case "SOURCES":
				sourcesIds2Name.put(p.getId(), p.getName());
				break;
			default:
				//do nothing
				break;			
			}		
		}	
	}

	private void setProperties(RxNormSBD sbd) {
		Set<Property> sbdProperties = sbd.getProperties();
		for(Property p : sbdProperties) {
			switch(p.getCategory()) {
			case "ATTRIBUTES":
				attributesIds2Name.put(p.getId(), p.getName());
				break;
			case "CODES":
				codesIds2Name.put(p.getId(), p.getName());
				break;
			case "NAMES":
				namesIds2Name.put(p.getId(), p.getName());
				break;
			case "SOURCES":
				sourcesIds2Name.put(p.getId(), p.getName());
				break;
			default:
				//do nothing
				break;			
			}		
		}	
	}

//uncharted (out of scope)
//	private void setProperties(RxNormBPCK bpck) {
//		Set<Property> scdProperties = bpck.getProperties();
//		for(Property p : scdProperties) {
//			switch(p.getCategory()) {
//			case "ATTRIBUTES":
//				attributesIds2Name.put(p.getId(), p.getName());
//				break;
//			case "CODES":
//				codesIds2Name.put(p.getId(), p.getName());
//				break;
//			case "NAMES":
//				namesIds2Name.put(p.getId(), p.getName());
//				break;
//			case "SOURCES":
//				sourcesIds2Name.put(p.getId(), p.getName());
//				break;
//			default:
//				//do nothing
//				break;			
//			}		
//		}	
//	}

//out of scope
//	private void setProperties(RxNormGPCK gpck) {
//		Set<Property> scdProperties = gpck.getProperties();
//		for(Property p : scdProperties) {
//			switch(p.getCategory()) {
//			case "ATTRIBUTES":
//				attributesIds2Name.put(p.getId(), p.getName());
//				break;
//			case "CODES":
//				codesIds2Name.put(p.getId(), p.getName());
//				break;
//			case "NAMES":
//				namesIds2Name.put(p.getId(), p.getName());
//				break;
//			case "SOURCES":
//				sourcesIds2Name.put(p.getId(), p.getName());
//				break;
//			default:
//				//do nothing
//				break;			
//			}		
//		}	
//	}	

	public void run() {
		// what has what?
		// 1. active scd-to-active sbd
		// 2. active scd-to-inactive sbd
		// 3. inactive scd-to-inactive sbd
		// 4. obsolete?...

		PrintWriter pwINS = null;
		PrintWriter pwCui = null;
		PrintWriter pwSCD = null;
		PrintWriter pwSBD = null;
		PrintWriter pwBOSS = null;
		PrintWriter pwProperties = null;
		
		try {
			pwINS = new PrintWriter(new File("in-min-pin-report.txt"));
			pwCui = new PrintWriter(new File("cui-report.txt"));
			pwSCD = new PrintWriter(new File("scd-report.txt"));
			pwSBD = new PrintWriter(new File("sbd-report.txt"));
			pwBOSS = new PrintWriter(new File("scd-to-sbd-view-by-boss.txt"));
			pwProperties = new PrintWriter(new File("prpoerty-report.txt"));
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		if(scanUnits) {
			for(String unit : tallyDenominatorUnits.keySet()) {
				System.out.println(unit + "\t" + tallyDenominatorUnits.get(unit));
			}
			System.out.flush();				
		}
		System.out.println("****** spilling cui2IN ******");
		System.out.flush();		
		for(Integer cui : cui2IN.keySet()) {
			pwINS.println(cui + "\t" + cui2IN.get(cui).getName() + "\t" + cui2IN.get(cui).getSnomedCodes());
		}
		
		System.out.println("****** spilling cui2MIN ******");
		System.out.flush();		
		for(Integer cui : cui2MIN.keySet()) {
			pwINS.println(cui + "\t" + cui2MIN.get(cui).getName() + "\t" + cui2MIN.get(cui).getSnomedCodes());			
		}
		
		System.out.println("****** spilling cui2PIN ******");
		System.out.flush();		
		for(Integer cui : cui2PIN.keySet()) {
			pwINS.println(cui + "\t" + cui2PIN.get(cui).getName() + "\t" + cui2PIN.get(cui).getSnomedCodes());			
		}		
		
		System.out.println("****** spilling cui2SCD ******");
		System.out.flush();		
		
		for(String cui : cui2SCD.keySet()) {
			pwCui.println(cui + " ==> " + cui2SCD.get(cui).getName());
		}
		
		System.out.println("****** spilling cui2SBD ******");
		System.out.flush();				
		
		for(String sbdCui : cui2SBD.keySet()) {
			pwSBD.println(sbdCui + " ==> " + cui2SBD.get(sbdCui).getName());
		}		
		
		System.out.println("****** spilling derived SCDs from SBDs with status ******");
		System.out.flush();				
		
		for(String scdCui : scd2SBDs.keySet()) {
			String scdName = cui2SCD.get(scdCui).getName();
			String scdStatus = cui2SCD.get(scdCui).getStatus();
			String scdDf = "DF not present";
			if( cui2SCD.get(scdCui).getRxNormDoseForm().size() > 0 ) {
				scdDf = cui2SCD.get(scdCui).getRxNormDoseForm().get(0).getName();
			}
//			if(!scdStatus.equalsIgnoreCase("active")) {
				pwSCD.println(scdCui + " ==> " + scdName + "\t" + scdStatus + "\t" + scdDf); //this *should* work
				int scdBoss = 0;
				boolean scdHasBoss = false;
				boolean scdHasAct = false;
				for(RxNormBoss b : cui2SCD.get(scdCui).getRxNormBoss()) {
					if(b.getBossRxCui() != -1) scdHasBoss = true;
					if(b.getActiveIngredientRxCui() != -1) scdHasAct = true;
					pwSCD.println("\t" + ++scdBoss + ".\t" + b.getBossRxCui() + "\t" + b.getActiveIngredientRxCui());
				}
				for(RxNormSBD sbd : scd2SBDs.get(scdCui)) {
					String sbdName = sbd.getName();
					Integer sbdCui = sbd.getRxcui();
					String sbdStatus = sbd.getStatus();
					pwSCD.println("\t\t" + sbdCui + " ==> " + sbdName + "\t" + sbdStatus);
					int numBoss = 0;
					for(RxNormBoss b : sbd.getvBoss()) {
						String bossIngCui = b.getBossRxCui().toString();
						String activIngCui = b.getActiveIngredientRxCui().toString();
						if( b.getBossRxCui() == -1 && scdHasBoss ) { bossIngCui = "*"; }
						if(	b.getActiveIngredientRxCui() == -1 && scdHasAct ) { activIngCui = "*"; }						
						pwSCD.println("\t\t\t" + ++numBoss + ".\t" + bossIngCui + "\t" + activIngCui);
					}
				}
//			}
		}
		
		for(String scdCui : cui2SCD.keySet()) {
			RxNormSCD scd = cui2SCD.get(scdCui);
			String scdName = scd.getName();
			String scdStatus = scd.getStatus();
			pwProperties.println(scdCui + "\t" + scdName + "\t" + scdStatus);
			Set<Property> properties = scd.getProperties();
			for(Property p : properties) {
				pwProperties.println("\t" + p.getName() + " (" + p.getCategory() + "): " + p.getValue() );
			}
			Set<NDC> ndcs = scd.getNdcSet();
			for(NDC n : ndcs) {
				pwProperties.println("\t" + n.getNdc() + "\t" + n.getStatus() + "\t" + n.getStartDate() + "\t" + n.getEndDate());
			}
			pwProperties.println();
			pwProperties.flush();
		}
		
		System.out.println("******");
		System.out.flush();	
		
		pwCui.close();
		pwSCD.close();
		pwSBD.close();
		pwProperties.close();
		
		if(scanUnits) {
			this.pwUnitNames.close();
		}		
		
	}
	
	public String getRxNormVersion() {
		return rxNormVersion;
	}
	
	public TreeMap<String, RxNormSCD> getCui2SCD() {
		return cui2SCD;
	}

	public void setCui2SCD(TreeMap<String, RxNormSCD> cui2scd) {
		cui2SCD = cui2scd;
	}

	public TreeMap<String, RxNormSBD> getCui2SBD() {
		return cui2SBD;
	}

	public void setCui2SBD(TreeMap<String, RxNormSBD> cui2sbd) {
		cui2SBD = cui2sbd;
	}

	public TreeMap<String, ArrayList<RxNormSBD>> getScd2SBDs() {
		return scd2SBDs;
	}

	public void setScd2SBDs(TreeMap<String, ArrayList<RxNormSBD>> scd2sbDs) {
		scd2SBDs = scd2sbDs;
	}

	public TreeMap<Integer, RxNormIngredient> getCui2IN() {
		return cui2IN;
	}

	public void setCui2IN(TreeMap<Integer, RxNormIngredient> cui2in) {
		cui2IN = cui2in;
	}

	public TreeMap<Integer, RxNormIngredient> getCui2PIN() {
		return cui2PIN;
	}

	public void setCui2PIN(TreeMap<Integer, RxNormIngredient> cui2pin) {
		cui2PIN = cui2pin;
	}

	public TreeMap<Integer, RxNormIngredient> getCui2MIN() {
		return cui2MIN;
	}

	public void setCui2MIN(TreeMap<Integer, RxNormIngredient> cui2min) {
		cui2MIN = cui2min;
	}

	public TreeMap<Integer, RxNormDoseForm> getCui2DF() {
		return cui2DF;
	}

	public void setCui2DF(TreeMap<Integer, RxNormDoseForm> cui2df) {
		cui2DF = cui2df;
	}

	public TreeMap<Integer, SnomedDFPair> getRxdf2SnomedDFPair() {
		return rxdf2SnomedDFPair;
	}

	public void setRxdf2SnomedDFPair(TreeMap<Integer, SnomedDFPair> rxdf2SnomedDFPair) {
		this.rxdf2SnomedDFPair = rxdf2SnomedDFPair;
	}

	public void setDFPairMap(String filename) {
		readTable(filename);		
	}
	
	public TreeMap<String, String> getAttributesList() {
		return attributesIds2Name;
	}

	public TreeMap<String, String> getCodesList() {
		return codesIds2Name;
	}

	public TreeMap<String, String> getNamesList() {
		return namesIds2Name;
	}

	public TreeMap<String, String> getSourcesList() {
		return sourcesIds2Name;
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
//					if( line != null && line.contains("\t") ) {
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
//								if( i == 2 ) {
//									df2PreferredDu.put(rxDf, rxPreferredDu);
//								}
								if( i == 3 || i == 4 ) {
									snDFValue = false;
								}
								if( i == 5 || i == 6 ) {
									snUPValue = false;
								}
								if( i == 7 ) {
									template = false;
								}
//								if(!snUPValue) {
//									forceDuNecessary = true;
//								}
							}
						}

//						if( rxDf.equals("316949") ) {
//							System.out.print("BREAK");
//						}
						
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
							
//							//(1)
//							if( smdf.getCode().equals(Long.valueOf("385223009")) && suop.getCode().equals(Long.valueOf("733026001")) ) {
//								//we don't know which template an SCD will use
//								dfPair = new SnomedDFPair(smdf, suop, null);
//								rxdf2SnomedDFPair.put(rxCui,  dfPair);
//							}
//							//(2)
//							else if( smdf.getCode().equals(Long.valueOf("385220007")) && suop.getCode().equals(Long.valueOf("733026001")) ) {
//								dfPair = new SnomedDFPair(smdf, suop, null);								
//								rxdf2SnomedDFPair.put(rxCui,  dfPair);								
//							}
//							//(3)
//							else if( smdf.getCode().equals(Long.valueOf("385223009")) && suop.getCode().equals(Long.valueOf("733026001")) ) {
								dfPair = new SnomedDFPair(smdf, suop, null);								
								rxdf2SnomedDFPair.put(rxCui,  dfPair);								
						}

//implicitly applied later
//						if(forceDuNecessary) {
//							df2PreferredDu.put(rxCui.toString(), rxPreferredDu);
//						} else {
//							if(suop != null) {
//								df2PreferredDu.put(rxCui.toString(), String.valueOf(suop.getCode()));
//							}
//						}
						
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

	public void setAllergens(String filename) {
		//TODO: reuse the readTable method, and move the logic
		//and setting to another function
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
							if( cui2IN.containsKey(Integer.valueOf(cuiString)) ) {
								RxNormIngredient in = cui2IN.get(Integer.valueOf(cuiString));
								in.setAllergenic(true);
								cui2IN.put(Integer.valueOf(cuiString), in);
							}
							if( cui2MIN.containsKey(Integer.valueOf(cuiString))) {
								RxNormIngredient in = cui2MIN.get(Integer.valueOf(cuiString));
								in.setAllergenic(true);
								cui2MIN.put(Integer.valueOf(cuiString), in);
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
	
}
