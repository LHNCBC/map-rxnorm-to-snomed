package gov.nih.nlm.mor.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;

import gov.nih.nlm.mor.RxNorm.RxNormDoseForm;
import gov.nih.nlm.mor.RxNorm.RxNormSCD;
import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.Snomed.SnomedManufacturedDoseForm;
import gov.nih.nlm.mor.Snomed.SnomedUnitOfPresentation;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.FetchRxNormDataAllStatus;
import gov.nih.nlm.mor.util.RestUrl;

public class FetchSnomedDoseForms implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7180407402478580977L;
	final String namespace = new String("http://snomed.info/id/");
	@SuppressWarnings("unused")
	private OWLOntologyManager man = null;
	private OWLOntology ontology = null;
	private OWLDataFactory factory = null;
	private OWLReasoner reasoner = null;
	public TreeMap<Integer, RxNormDoseForm> rxcui2DF = new TreeMap<Integer, RxNormDoseForm>();
	public HashMap<Integer, Integer> rxdfCUI2SCDCount = new HashMap<Integer, Integer>();
	public TreeMap<Integer, RxNormSCD> rxcui2SCD = new TreeMap<Integer, RxNormSCD>();
	public TreeMap<Integer, SnomedDFPair> snomed2DFPair = new TreeMap<Integer, SnomedDFPair>();
	public TreeMap<Integer, ArrayList<SnomedDFPair>> rxdf2SnomedDFPair = new TreeMap<Integer, ArrayList<SnomedDFPair>>();	
	@SuppressWarnings("unused")
	private Map<SnomedDFPair, Integer> cdSnomedDFPair2Count = Collections.checkedMap(new HashMap<SnomedDFPair, Integer>(), SnomedDFPair.class, Integer.class);  //leave it
	private Map<SnomedDFPair, Integer> mpSnomedDFPair2Count = Collections.checkedMap(new HashMap<SnomedDFPair, Integer>(), SnomedDFPair.class, Integer.class);
	private Map<SnomedDFPair, Integer> mpfSnomedDFPair2Count = Collections.checkedMap(new HashMap<SnomedDFPair, Integer>(), SnomedDFPair.class, Integer.class);	
	final String medicinalProductId = new String("763158003");
	final String hasManufacturedDoseForm = "736542009";
	final String hasUnitOfPresentation = "763032000";
	@SuppressWarnings("unused")
	private OWLClass classHasManufacturedDoseForm = null;  //leave these
	@SuppressWarnings("unused")
	private OWLClass classHasUnitOfPresentation = null;
	private Double seed = Math.random();
	private ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	private RestUrl restUrl = new RestUrl();
	
	
	public FetchSnomedDoseForms(OWLOntologyManager man, OWLOntology ontology, OWLDataFactory factory, OWLReasoner reasoner, FetchRxNormDataAllStatus fetchRxNorm) {
//		rxdf2SnomedDFPair = fetchRxNorm.getDFPairMap("./RxNorm2SnomedDFMapping.txt");
		rxcui2DF = fetchRxNorm.getCui2DF();
//		rxcui2SCD = fetchRxNorm.getCui2SCD();
		this.man = man;
		this.ontology = ontology;
		this.factory = factory;
		this.reasoner = reasoner;
		this.classHasManufacturedDoseForm = factory.getOWLClass(namespace, hasManufacturedDoseForm);
		this.classHasUnitOfPresentation = factory.getOWLClass(namespace, hasUnitOfPresentation);
		examineForPairs();
	}
	
	public FetchSnomedDoseForms(OWLOntologyManager man, OWLOntology ontology, OWLDataFactory factory, OWLReasoner reasoner, FetchRxNormData fetchRxNorm) {
//		rxdf2SnomedDFPair = fetchRxNorm.getDFPairMap("./RxNorm2SnomedDFMapping.txt");
		rxcui2DF = fetchRxNorm.getDFMap();
		rxcui2SCD = fetchRxNorm.getSCDMap();
		this.man = man;
		this.ontology = ontology;
		this.factory = factory;
		this.reasoner = reasoner;
		this.classHasManufacturedDoseForm = factory.getOWLClass(namespace, hasManufacturedDoseForm);
		this.classHasUnitOfPresentation = factory.getOWLClass(namespace, hasUnitOfPresentation);
		examineForPairs();
	}	
	
	public void examineForPairs() {
		PrintWriter doseFormPrint = null;
		File doseOutFil = null;
		try {
			 doseOutFil = new File("dose-form-pair-raw-" + String.valueOf(seed) + ".txt");
			 doseFormPrint = new PrintWriter(doseOutFil);
		} catch(Exception e) {
			e.printStackTrace();
		}
			
		OWLClass medicinalProduct = factory.getOWLClass(namespace, medicinalProductId);
		OWLClass temporaryClass = factory.getOWLClass(namespace, "770654000");
		Set<OWLClass> allChildrenOfMP = reasoner.getSubClasses(medicinalProduct, false).entities().collect(Collectors.toSet());
		Set<OWLClass> allChildrenOfTemporary = reasoner.getSubClasses(temporaryClass, false).entities().collect(Collectors.toSet());
		allChildrenOfMP.remove(medicinalProduct);
		allChildrenOfTemporary.remove(temporaryClass);
		allChildrenOfMP.addAll(allChildrenOfTemporary);
		allChildrenOfMP.remove(factory.getOWLNothing());
		System.out.println("Looking at " + allChildrenOfMP.size());
		doseFormPrint.println("Looking at " + allChildrenOfMP.size());
		doseFormPrint.flush();
		
		for(OWLClass mpChild : allChildrenOfMP) {
			SnomedDFPair pair = new SnomedDFPair();
			String label = getRDFSLabel(mpChild);
//			if( mpChild.getIRI().toString().contains("400480005") ) {
//				System.out.println("BREAK");
//			}
			findPairs( mpChild, pair);
			//			if( label == null ) {
			//				System.out.println("No label for: " + mpChild.toString());
			//			}
			//			else 
			if( label != null ) {
				String type = null;
				if( label.contains("(clinical drug)") ) {
					type = "clinical drug";
				}
				else if( label.contains("(product)") ) {
					type = "product";
				}
				else if( label.contains("(medicinal product)") ) {
					type = "medicinal product";
				}
				else if( label.contains("(medicinal product form)") ) {
					type = "medicinal product form";
				}					
//								if( cdSnomedDFPair2Count.containsKey(pair) ) {
//									Integer i = cdSnomedDFPair2Count.get(pair);
//									cdSnomedDFPair2Count.put(pair, ++i);
//								}
//								else {
//									cdSnomedDFPair2Count.put(pair, new Integer(1));
//									Long snomedCode = new Long(mpChild.getIRI().getIRIString().replace(namespace, ""));
//									Integer rxcui = getRxCuiForSnomedCode(snomedCode);
//									if( rxcui != null ) {
//										RxNormSCD rxscd = rxcui2SCD.get(rxcui);
//										if( rxscd != null ) {
//											RxNormDoseForm rxdf = rxscd.getRxNormDoseForm().get(0);
//											String rxdfString = rxdf.getName();
//											System.out.println(rxcui + "\t" + rxscd.getName() + "\t" + rxdfString + "\t" + snomedCode + "\t" + label + "\t" );
//											if( pair.hasDF() ) {
//												System.out.print(pair.getSnomedManufacturedDoseForm().getName() + "\t");
//												
//											}
//											if( pair.hasUP() ) {
//												System.out.print(pair.getSnomedUnitOfPresentation().getName());
//											}
//											System.out.print("\n");
//										}
//									}
//									else {
//										System.out.println("" + "\t" + "" + "\t" + "" + "\t" + snomedCode + "\t" + label + "\t" );
//										if( pair.hasDF() ) {
//											System.out.print(pair.getSnomedManufacturedDoseForm().getName() + "\t");
//											
//										}
//										if( pair.hasUP() ) {
//											System.out.print(pair.getSnomedUnitOfPresentation().getName());
//										}
//										System.out.print("\n");						
//									}
//									
//								}
				Long snomedCode = new Long(mpChild.getIRI().getIRIString().replace(namespace, ""));
				Integer rxcui = getRxCuiForSnomedCode(snomedCode);
				if( rxcui != null ) {
					RxNormSCD rxscd = rxcui2SCD.get(rxcui);
					if( rxscd != null ) {
						RxNormDoseForm rxdf = null;						
						try {
							rxdf = rxscd.getRxNormDoseForm().get(0);
						} catch(Exception e) {
							System.err.println(rxcui + " (" + rxscd.getStatus() + ") has no dose form");
						}
						if(rxdf != null) {
							String rxdfString = rxdf.getName();
							//System.out.print(rxcui + "\t" + rxscd.getName() + "\t" + rxdfString + "\t" + type + "\t" + snomedCode + "\t" + label + "\t" );
							doseFormPrint.print(rxcui + "\t" + rxscd.getName() + "\t" + rxdfString + "\t" + type + "\t" + snomedCode + "\t" + label + "\t" );
							if( pair.hasDF() ) {
								//System.out.print(pair.getSnomedManufacturedDoseForm().getName() + "\t");
								doseFormPrint.print(pair.getSnomedManufacturedDoseForm().getName() + "\t");
								
							}
							if( pair.hasUP() ) {
								//System.out.print(pair.getSnomedUnitOfPresentation().getName());
								doseFormPrint.print(pair.getSnomedUnitOfPresentation().getName());
							}
							//System.out.print("\n");
							doseFormPrint.println();
							doseFormPrint.flush();
						}
					}
					else {
//						System.out.println("NOT A SCD" + "\t" + snomedCode + "\t" + rxcui);
					}
				}
				else {
					//System.out.print("" + "\t" + "" + "\t" + "" + "\t" + type + "\t" + snomedCode + "\t" + label + "\t" );
					doseFormPrint.print("" + "\t" + "" + "\t" + "" + "\t" + type + "\t" + snomedCode + "\t" + label + "\t" );
					if( pair.hasDF() ) {
						//System.out.print(pair.getSnomedManufacturedDoseForm().getName() + "\t");
						doseFormPrint.print(pair.getSnomedManufacturedDoseForm().getName() + "\t");
					}
					if( pair.hasUP() ) {
						//System.out.print(pair.getSnomedUnitOfPresentation().getName());
						doseFormPrint.print(pair.getSnomedUnitOfPresentation().getName());
					}
					//System.out.print("\n");
					doseFormPrint.println();
					doseFormPrint.flush();
				}				
				if( type.equals("medicinal product") ) {
					if( mpSnomedDFPair2Count.containsKey(pair) ) {
						Integer i = mpSnomedDFPair2Count.get(pair);
						mpSnomedDFPair2Count.put(pair, ++i);
					}
					else {
						mpSnomedDFPair2Count.put(pair, new Integer(1));					
					}				
				}
				else if( label.contains("(medicinal product form)") ) {
					if( mpfSnomedDFPair2Count.containsKey(pair) ) {
						Integer i = mpfSnomedDFPair2Count.get(pair);
						mpfSnomedDFPair2Count.put(pair, ++i);
					}
					else {
						mpfSnomedDFPair2Count.put(pair, new Integer(1));					
					}						
				}



//				System.out.println("*** Medincal Product df pair count ***");		
//				for( SnomedDFPair pair : mpSnomedDFPair2Count.keySet() ) {
//					pair.print();			
//					System.out.println("\t" + mpSnomedDFPair2Count.get(pair));
//				}
//				System.out.println();
//				System.out.println("*** Medicinal Product Form df pair count ***");
//				for( SnomedDFPair pair : mpfSnomedDFPair2Count.keySet() ) {
//					pair.print();			
//					System.out.println("\t" + mpfSnomedDFPair2Count.get(pair));
//				}
//				System.out.println();	

//				System.out.println("*** Clinical Drug df pair count ***");
//				Vector<Integer> cuisFound = new Vector<Integer>();
//				for( Integer cui : this.rxcui2SCD.keySet() ) {
//					RxNormSCD scd = rxcui2SCD.get(cui);
//					for( RxNormDoseForm df : scd.getRxNormDoseForm() ) {
//						Integer dfCui = new Integer(df.getRxcui());
//						if( rxdfCUI2SCDCount.containsKey(dfCui) ) {
//							Integer i = rxdfCUI2SCDCount.get(dfCui) + 1;
//							rxdfCUI2SCDCount.put(dfCui, i);
//						}
//						else {
//							rxdfCUI2SCDCount.put(dfCui,  new Integer(1));
//						}
//					}	
//				}

// 221220 - Unused maps				
//				HashMap<Integer, Integer> rxCUI2SCDDFCount = new HashMap<Integer, Integer>();
//				for( String i : rxcui2SCD.keySet() ) {
//					if(rxcui2SCD.get(i).getStatus().equalsIgnoreCase("active")) {
//						String dfCui = rxcui2SCD.get(i).getRxNormDoseForm().elementAt(0).getRxcui();
//						if( rxCUI2SCDDFCount.containsKey(new Integer(dfCui))) {
//							Integer count = rxCUI2SCDDFCount.get(new Integer(dfCui));
//							count = count + 1;
//							rxCUI2SCDDFCount.put(new Integer(dfCui), count);
//						}
//						else {
//							rxCUI2SCDDFCount.put(new Integer(dfCui), new Integer(1));
//						}
//					}
//				}

//				System.out.println("RxCUI\tCount");
//				for( Integer i : rxCUI2SCDDFCount.keySet() ) {
//					System.out.println(i + "\t" + rxCUI2SCDDFCount.get(i) );
//				}

//						System.out.println("RxCUI Dose Form\tRxCUI Name Dose Form\tCount of SCDs with Dose Form\tSnomed Dose Form Code\tSnomed Dose Form Name\tSnomed UP Code\tSnomed UP Name\tSnomed CD Count");
//						for( Integer i : rxdfCUI2SCDCount.keySet() ) {
//							System.out.print(this.rxcui2DF.get(i).getRxcui() + "\t" + this.rxcui2DF.get(i).getName() + "\t" + rxCUI2SCDDFCount.get(new Integer(rxcui2DF.get(i).getRxcui())));
//							String rxcui1 = this.rxcui2DF.get(i).getRxcui();
//							if( rxdf2SnomedDFPair.containsKey(new Integer(rxcui1)) ) {
//								for( SnomedDFPair pair1 : rxdf2SnomedDFPair.get(new Integer(rxcui1)) ) {
//									String df = pair1.hasDF() ? pair1.getSnomedManufacturedDoseForm().getCode().toString() + "\t" + pair1.getSnomedManufacturedDoseForm().getName() : "\t";
//									String up = pair1.hasUP() ? pair1.getSnomedUnitOfPresentation().getCode().toString() + "\t" + pair1.getSnomedUnitOfPresentation().getName() : "\t";
//									System.out.print("\t" + df + "\t" + up );
//									if( cdSnomedDFPair2Count.containsKey(pair1) ) {
//										System.out.print("\t" + cdSnomedDFPair2Count.get(pair1));
//										cdSnomedDFPair2Count.remove(pair1);
//									}
//									else {
//										System.out.print("\t");
//									}
//								}
//							}
//							else {
//								System.out.print("\t\t\t\t\t");
//							}
//							System.out.print("\n");
//						}
//						for( SnomedDFPair pair1 : cdSnomedDFPair2Count.keySet() ) {
//							System.out.print("\t\t");
//							pair1.print();
//							System.out.println("\t" + cdSnomedDFPair2Count.get(pair1));			
//						}


				//		for( SnomedDFPair pair : cdSnomedDFPair2Count.keySet() ) {
				//			boolean rxcuiFound = false;
				//			for( Integer cui : this.rxdf2SnomedDFPair.keySet() ) {
				//				if( rxdf2SnomedDFPair.get(cui).contains(pair) ) {
				//					System.out.print(cui + "\t" + this.rxcui2DF.get(cui).getName() );
				//					cuisFound.add(cui);
				//					rxcuiFound = true;
				//				}
				//			}
				//			if( !rxcuiFound ) {
				//				System.out.print("\t");
				//			}
				//			pair.print();			
				//			System.out.println("\t" + cdSnomedDFPair2Count.get(pair));
				//		}
				//		
				//		for( Integer i : rxdfCUI2SCDCount.keySet() ) {
				//			System.out.println(i + rxcui2DF.get(i).getName() + rxdfCUI2SCDCount.get(i));
				//		}

			}
		}
		doseFormPrint.close();
		System.out.println("Dose configuration raw file: " + doseOutFil.getAbsolutePath());
	}
	
	public void findPairs(OWLClass mpChild, SnomedDFPair pair) {
		Set<OWLClass> eqClasses = reasoner.getEquivalentClasses(mpChild).entities().collect(Collectors.toSet());
		for( OWLClass eqClass : eqClasses ) {
			ontology.equivalentClassesAxioms(eqClass).forEach(
					x -> x.classExpressions().forEach( y -> {
						if( y instanceof OWLNaryBooleanClassExpression)
							processInnerNAryExpression(y, pair);
					}
							));
		}
		if( pair.isNull() ) {
			for( OWLAxiom axiom : ontology.axioms( mpChild ).collect( Collectors.toSet() ) ) {
				if( axiom instanceof OWLSubClassOfAxiom ) {
					OWLClassExpression oce = ((OWLSubClassOfAxiom) axiom).getSuperClass();
					processInnerNAryExpression(oce, pair);
				}
//				System.out.println( "\tAxiom: " + axiom.toString() );
//				// create an object visitor to get to the subClass restrictions
//				axiom.accept( new OWLObjectVisitor() {
//					public void visit( OWLSubClassOfAxiom subClassAxiom ) {
//
//						// create an object visitor to read the underlying (subClassOf) restrictions
//						processInnerNAryExpression(subClassAxiom.getSuperClass(), pair);
//					};
//				});
			}
			//		Long code = new Long(mpChild.getIRI().getIRIString().replace(namespace, ""));
			//		Integer rxCui = getRxCuiForSnomedCode(code);
		}
	}
	
	public void printQuantifiedRestriction( OWLClass mpChild, OWLObjectIntersectionOf a ) {
		System.out.println("Class: " + mpChild.toString() + "\t" + a.toString() );
		
	}
	
	public Integer getRxCuiForSnomedCode(Long sc) {
		Integer cui = null;
		
		String method = restUrl.getRestUrl() + "/REST/rxcui.json?idtype=SNOMEDCT&id=";
		method = method.concat(sc.toString());		
		JSONObject result = null;
		
		try {
			result = getresult(method, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONObject idGroup = null;
		JSONArray minConceptArray = null;
		idGroup = (JSONObject) result.get("idGroup");
		if( idGroup.has("rxnormId")) {
			minConceptArray = (JSONArray) idGroup.get("rxnormId");
			for(int i = 0; i < minConceptArray.length(); i++ ) {
				
				String cuiString = (String) minConceptArray.get(i);
				cui = Integer.valueOf(cuiString);		
			}			
		}
		else {
//			System.out.println("None for " + sc.toString());
		}
		
		return cui;
	}
	
	public void processInnerNAryExpression(OWLClassExpression oce, SnomedDFPair pair) {
		if (oce instanceof OWLObjectIntersectionOf ) {
			Set<OWLClassExpression> expressionSet = ((OWLNaryBooleanClassExpression) oce).operands().collect(Collectors.toSet());
			for(OWLClassExpression expression : expressionSet ) {
				processInnerNAryExpression(expression, pair);
			}
		}
		if (oce instanceof OWLObjectSomeValuesFrom ) {
//			System.out.println("ClassExpression " + oce.toString());
			OWLObjectPropertyExpression a = ((OWLObjectSomeValuesFrom) oce).getProperty();
			String propertyName = getObjectName(a);
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom) oce).getFiller();
			if( filler.isOWLClass() ) {
				if( propertyName.equals("Has manufactured dose form (attribute)") ) {
					String code = filler.asOWLClass().getIRI().getIRIString().replace(namespace, "");
					String label = getRDFSLabel((OWLClass) filler);
					SnomedManufacturedDoseForm df = new SnomedManufacturedDoseForm(new Long(code), label);
					pair.setDF(df);
				}
				if( propertyName.equals("Has unit of presentation (attribute)" )) {				
					String code = filler.asOWLClass().getIRI().getIRIString().replace(namespace, "");
					String label = getRDFSLabel((OWLClass) filler);
					SnomedUnitOfPresentation up = new SnomedUnitOfPresentation(new Long(code), label);
					pair.setUP(up);
				}
			}
//			OWLObjectSomeValuesFrom restriction = (OWLObjectSomeValuesFrom) oce;
//			OWLClassExpression filler = restriction.getFiller();
//			System.out.println("Property Expression: " + a.toString());
//			System.out.println("Filler: " + filler.toString());
		}
	}
	
	public String getObjectName(OWLObjectPropertyExpression a) {
		String name = null;
		for( OWLObjectProperty op : a.objectPropertiesInSignature().collect(Collectors.toSet()) ) {
			name = getRDFSLabel(op);
		}
		return name;
	}
	
	public String getObjectIRIAsString(OWLObjectPropertyExpression a) {
		String code = null;
		for( OWLObjectProperty op : a.objectPropertiesInSignature().collect(Collectors.toSet()) ) {
			code = op.getIRI().toString();
		}
		return code;
	}	
	
	public String getRDFSLabel(OWLEntity cls) {
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
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
	

}
