/*
 * This class will look at both sides after the
 * Rx SCDs are generated as classes and the inference maps
 * are passed in.  It will descend down each SCT branch
 * of interest (particularly medicinal product) to gather
 * all logical components for each defined drug class. These
 * components are then compared to the RxNorm model in memory.
 * Annotations are added where there are detectable differences
 * between the asserted SCT<->RxN drugs.+
 */



package gov.nih.nlm.mor.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
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
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;

import gov.nih.nlm.mor.RxNorm.RxNormBoss;
import gov.nih.nlm.mor.RxNorm.RxNormDoseForm;
import gov.nih.nlm.mor.RxNorm.RxNormIngredient;
import gov.nih.nlm.mor.RxNorm.RxNormSCD;
import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.auditmap.utilitaries.OntologyClassManagement;
import gov.nih.nlm.mor.util.ClassPair;
import gov.nih.nlm.mor.util.ExcelReport;


//This is a class we should hold onto, at least purely for research purposes. (Refer to method call diff -> setMapObjectProperties -> processInnerNAryExpression.)
//This class requires knowledge of both sides.  There are issues when
//detecting differences between values and units inside BoSSs.
//If unit(s) is wrong, chances are good the values are also not correct.
//The assumptions within the logic (e.g., an Rx solid dose for has no denominator whereas in SCT the denominator is
//a 'unit of presentation' (not to be confused with a unit).. or also determining when an injection (not solution/suspension)
//is of the SCT template definition form 'Presentation' or 'Presentation' & 'Concentration'.
//

public class Analyzer implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3186247814820871189L;
	private OWLOntologyManager man = null;
	private OntologyClassManagement ontologyManager = null;
	private EquivalentMappingManager eqManager = null; 	
	private Set<EquivalentMapping> mappings = new HashSet<>();
	private Set<EquivalentMapping> finalizedMappings = new HashSet<>();
	private HashMap<OWLClass, ArrayList<OWLClass>> rxToRx = new HashMap<OWLClass, ArrayList<OWLClass>>();
	final String namespace = new String("http://snomed.info/id/"); //TODO: make this configurable
	public OWLOntology ontology = null;
	public OWLDataFactory factory = null;
	public OWLReasoner reasoner = null;

	public OWLClass medicinalProduct = null;
	public OWLClass numberClass = null;
	public OWLClass substanceClass = null;
	public OWLClass temporaryClass = null;	
	public OWLObjectProperty hasActiveIngredient = null;
	public OWLObjectProperty roleGroup = null;
	public OWLObjectProperty countOfBaseOfActiveIngredient = null;
	public OWLObjectProperty hasPreciseActiveIngredient = null;
	public OWLObjectProperty hasManufacturedDoseForm = null;
	public OWLObjectProperty hasBasisOfStrengthSubstance = null;
	public OWLObjectProperty hasPresentationStrengthNumeratorUnit = null;
	public OWLObjectProperty hasPresentationStrengthNumeratorValue = null;
	public OWLObjectProperty hasPresentationStrengthDenominatorUnit = null;
	public OWLObjectProperty hasPresentationStrengthDenominatorValue = null;
	public OWLObjectProperty hasConcentrationStrengthNumeratorUnit = null;
	public OWLObjectProperty hasConcentrationStrengthNumeratorValue = null;
	public OWLObjectProperty hasConcentrationStrengthDenomniatorUnit = null;
	public OWLObjectProperty hasConcentrationStrengthDenominatorValue = null;
	public OWLObjectProperty hasUnitOfPresentation = null;
	public Vector<OWLObjectProperty> qaChecks = new Vector<OWLObjectProperty>();
	public Vector<OWLObjectProperty> qaUnitChecks = new Vector<OWLObjectProperty>();
	public Vector<OWLObjectProperty> qaValueChecks = new Vector<OWLObjectProperty>();
	public TreeMap<Integer, SnomedDFPair> rxdf2SnomedDFPair = new TreeMap<Integer, SnomedDFPair>();
	public TreeMap<OWLClass, ArrayList<OWLClass>> ingEquivalencyMap = null;
	public TreeMap<OWLClass, ArrayList<OWLClass>> unitEquivalencyMap = null;
	//	public HashMap<String, OWLClass> numberEquivalencyMap = null;
	public TreeMap<OWLClass, ArrayList<OWLClass>> numberEquivalencyMap = null;
	public Set<RxNormSCD> wontMapSCD = new HashSet<>();
	public Set<RxNormSCD> noSnomedDefinitionTemplate = new HashSet<>();	
	public TreeMap<String, Set<String>> falsePositives = new TreeMap<String, Set<String>>();
	private OWLAnnotationProperty mapsToCode; //unused annotations in this class are still declared, just in case
	private OWLAnnotationProperty mapsToName;
	private OWLAnnotationProperty inferred;
	private OWLAnnotationProperty asserted;
	private OWLAnnotationProperty hasNDC;
	private OWLAnnotationProperty hasExplanation;
	private OWLAnnotationProperty hasRxCUI;
	private OWLAnnotationProperty substanceNoExist;
	private OWLAnnotationProperty substanceDifferent;
	private OWLAnnotationProperty bossSubstanceDifferent;
	private OWLAnnotationProperty activeIngerdientSubstanceDifferent;
	private OWLAnnotationProperty doseFormDifferent;
	private OWLAnnotationProperty presUnitDifferent;
	private OWLAnnotationProperty unitsDifferent;
	private OWLAnnotationProperty valuesDifferent;
	private OWLAnnotationProperty countBaseDifferent;
	private OWLAnnotationProperty vetOnly;
	private OWLAnnotationProperty hasAllergenic;
	private OWLAnnotationProperty isPrescribable;	
	private PrintWriter pw = null;
	private Double seed = Math.random();
	private OWLObjectProperty isModificationOf;
	private String globalDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());	
	private String finalFileName = "";
	private boolean standaloneMode = false;   //anything requiring dose form information will not be checked for standalone mode since the config file is not needed
											  //this information and template type will be missing in the report
//	private OWLClass modifiedClass = null;


	public Analyzer(TreeMap<Integer, RxNormSCD> scdMap, TreeMap<OWLClass, ArrayList<OWLClass>> cdEquivalencyMapSnomed, TreeMap<OWLClass, ArrayList<OWLClass>> scdEquivalencyMapRxNorm, TreeMap<OWLClass, 
			ArrayList<OWLClass>> ingEquivalencyMap, TreeMap<Integer, SnomedDFPair> dfMap, TreeMap<OWLClass, ArrayList<OWLClass>> unitEquivalencyMap, TreeMap<OWLClass, ArrayList<OWLClass>> numberEquivalencyMap, 
			Set<RxNormSCD> noSnomedDefinitionTemplate, OWLOntology ont, OWLDataFactory factory, OWLReasoner reasoner, OWLOntologyManager manager, PrintWriter pw) {

		this.man = manager;
		this.ontology = ont;
		this.factory = factory;
		this.reasoner = reasoner;
////unused		this.rxdf2SnomedDFPair = dfMap;
		this.ingEquivalencyMap = ingEquivalencyMap;
		this.unitEquivalencyMap = unitEquivalencyMap;
		this.numberEquivalencyMap = numberEquivalencyMap;
////unused		this.noSnomedDefinitionTemplate = noSnomedDefinitionTemplate;
		this.pw = pw;
		
		setConstants();

		log("Populating maps of analyzer");
		System.out.println("SCD Map has: " + scdMap.keySet().size() + " SCDs.");
		for( Integer cui : scdMap.keySet() ) {
			RxNormSCD scd = scdMap.get(cui);
			//This is already done in GenerateEquivalents class			scd.setSnomedCodes();	

			String rxCui = "Rx" + cui;
			OWLClass rxClass = factory.getOWLClass(namespace, rxCui);

			if( cdEquivalencyMapSnomed.containsKey(rxClass) ) {
				ArrayList<OWLClass> inferredClasses = cdEquivalencyMapSnomed.get(rxClass);

				for( OWLClass testClass : inferredClasses ) {
					EquivalentMapping eqMap = null;
					String idString = testClass.getIRI().getIRIString();
					idString = idString.replace(namespace, "");
					if( !idString.contains("Rx") ) {
						Long sctCode = Long.valueOf(idString);
						String label = getRDFSLabelForLong(sctCode);
						if( label != null && label.contains("(clinical drug)")) {
							eqMap = new EquivalentMapping(scd, String.valueOf(cui), scd.getName(), sctCode, label);
							eqMap.setRxClass(rxClass);
							eqMap.setSnomedClass(testClass);
							eqMap.setIsInferred(true);
							eqMap.setIsAsserted(isAsserted(scd));	
							if( scd.hasNDC() ) {
								addAnnotation(testClass, this.hasNDC, true);
							}
							else {
								addAnnotation(testClass, this.hasNDC, false);
							}
							mappings.add(eqMap);
							log("Added " + eqMap.getRxName() + "\t" + eqMap.getSnomedName() );							
						}
					}
					else {
						if( falsePositives.containsKey(cui) ) {
							Set<String> ints = falsePositives.get(cui);
							ints.add(idString.replace("Rx", ""));
							falsePositives.put(String.valueOf(cui), ints);
						}
						else {
							Set<String> ints = new HashSet<>();
							ints.add(idString.replace("Rx", ""));
							falsePositives.put(String.valueOf(cui), ints);
						}
					}				
				}
			}
			else {
				boolean found = false;
				for( Long id : scd.getSnomedCodes() ) {
					String snomedLabel = getRDFSLabelForLong(id);
					if( snomedLabel != null && snomedLabel.contains("(clinical drug)") ) {					
						EquivalentMapping eqMap = new EquivalentMapping(scd, String.valueOf(cui), scd.getName(), id, getRDFSLabelForLong(id));
						eqMap.setRxClass(rxClass);
						eqMap.setSnomedClass(factory.getOWLClass(namespace, String.valueOf(id)));
						eqMap.setIsAsserted(true);						
						mappings.add(eqMap);
						log("Added " + eqMap.getRxName() + "\t" + eqMap.getSnomedName() );						
						found = true;
					}
				}	
				if( !found ) {
					//the rest of the 19,000
					wontMapSCD.add(scd);
					EquivalentMapping eqMap = new EquivalentMapping(scd, String.valueOf(cui), scd.getName(), null, null);
					eqMap.setRxClass(rxClass);
					eqMap.setSnomedClass(null);
					eqMap.setIsAsserted(false);
					addAnnotation(rxClass, this.asserted, "false");
					mappings.add(eqMap);
					log("Added " + eqMap.getRxName() + "\t" + eqMap.getSnomedName() );					
				}
			}
			if( scdEquivalencyMapRxNorm.containsKey(rxClass) ) {
				ArrayList<OWLClass> inferredClasses = scdEquivalencyMapRxNorm.get(rxClass);

				for( OWLClass testClass : inferredClasses ) {
					EquivalentMapping eqMap = null;
					String idString = testClass.getIRI().getIRIString();
					idString = idString.replace(namespace, "");
					if( !idString.contains("Rx") ) {
						Long sctCode = Long.valueOf(idString);
						String label = getRDFSLabelForLong(sctCode);
						if( label != null && label.contains("(clinical drug)")) {
							eqMap = new EquivalentMapping(scd, String.valueOf(cui), scd.getName(), sctCode, label);
							eqMap.setRxClass(rxClass);
							eqMap.setSnomedClass(testClass);
							eqMap.setIsInferred(true);
							eqMap.setIsAsserted(isAsserted(scd));
						}
					}
					else {
						if( falsePositives.containsKey(cui) ) {
							Set<String> ints = falsePositives.get(cui);
							ints.add(idString.replace("Rx", ""));
							falsePositives.put(String.valueOf(cui), ints);
						}
						else {
							Set<String> ints = new HashSet<>();
							ints.add(idString.replace("Rx", ""));
							falsePositives.put(String.valueOf(cui), ints);
						}
					}
					if( eqMap != null )	{
						if( mappings.add(eqMap) ) {
							//comment out
							log("Added " + eqMap.getRxName() + "\t" + eqMap.getSnomedName() );
						}
					}					
				}				
			}
			else {
				boolean found = false;
				for( Long id : scd.getSnomedCodes() ) {
					String snomedLabel = getRDFSLabelForLong(id);
					if( snomedLabel != null && snomedLabel.contains("(clinical drug)") ) {					
						EquivalentMapping eqMap = new EquivalentMapping(scd, String.valueOf(cui), scd.getName(), id, getRDFSLabelForLong(id));
						eqMap.setRxClass(factory.getOWLClass(namespace, "Rx" + String.valueOf(cui) ));
						eqMap.setSnomedClass(factory.getOWLClass(namespace, String.valueOf(id)));
						eqMap.setIsAsserted(true);
						if( mappings.add(eqMap) ) {
							log("Added " + eqMap.getRxName() + "\t" + eqMap.getSnomedName() );
						}
						found = true;
					}
				}	
				if( !found ) {
					wontMapSCD.add(scd);
					EquivalentMapping eqMap = new EquivalentMapping(scd, scd.getCui().toString(), scd.getName(), null, null);
					eqMap.setIsAsserted(false);
					eqMap.setRxClass(rxClass);
					eqMap.setSnomedClass(null);
					mappings.add(eqMap);					
				}
			}			
		}

		for( OWLClass cls : ingEquivalencyMap.keySet() ) {
			if( cls.getIRI().getIRIString().contains("Rx")) {
				for( OWLClass ing : ingEquivalencyMap.get(cls)) {
					if( ing.getIRI().getIRIString().contains("Rx")) {
						//An Rx is eq to another Rx
						if( rxToRx.containsKey(cls) ) {
							ArrayList<OWLClass> list = rxToRx.get(cls);
							list.add(ing);
							rxToRx.put(cls, list);
						}
						else {
							ArrayList<OWLClass> list = new ArrayList<OWLClass>();
							list.add(ing);
							rxToRx.put(cls, list);
						}
					}
				}
			}
		}
		System.out.println("Finished populating maps of analyzer");

		diff(mappings);

		System.out.println("*** Dumping Maps ***");
		System.out.println();
		log(" -- SCD Map -- ");
		log("");
		for(Integer i : scdMap.keySet() ) {
			RxNormSCD scd = scdMap.get(i);
			log(scd.getCui() + "\t" + scd.getName());

		}
		System.out.println();

		log(" -- Snomed CD Equivalency Map -- ");
		log("");
		for( OWLClass cls : cdEquivalencyMapSnomed.keySet() ) {
			ArrayList<OWLClass> list = cdEquivalencyMapSnomed.get(cls);
			for(OWLClass c : list ) {
				log(cls.getIRI().getIRIString().replace(namespace, "") + "\t" + c.getIRI().getIRIString().replace(namespace, ""));
			}
		}
		System.out.println();

		log(" -- RxNorm SCD Equivalency Map -- ");
		log("");
		for( OWLClass cls : scdEquivalencyMapRxNorm.keySet() ) {
			ArrayList<OWLClass> list = scdEquivalencyMapRxNorm.get(cls);
			for(OWLClass c : list ) {
				log(cls.getIRI().getIRIString().replace(namespace, "") + "\t" + c.getIRI().getIRIString().replace(namespace, ""));
			}
		}
		System.out.println();

		log(" -- Ingredient Map -- ");
		log("");
		for( OWLClass cls : ingEquivalencyMap.keySet() ) {
			ArrayList<OWLClass> list = ingEquivalencyMap.get(cls);
			for(OWLClass c : list) {
				log(cls.getIRI().getIRIString().replace(namespace, "") + "\t" + c.getIRI().getIRIString().replace(namespace, ""));
			}
		}
	
		saveOntology();
//		report();
//		report();
		//runAudit();		
		
	}
	
	

	//the equivalency class (serializing new RxNorm classes, etc) has maps 
	//passed to the Analyzer.  We would need to run this entire process again
	//in order to detect errors.  Instead, we are going to allow a user to pass
	//any rxnorm-snomed file to the analyzer.
	public static void main(String[] args) {
		//RWW: Adding to audit the audit
		//the class should accept any file
		
		Analyzer analyze = new Analyzer(args[0]);
	}
	
	public Analyzer(String filename) {
		//this is a standalone mode
		this.standaloneMode = true;		
		
		File file = new File(filename);
		this.ontologyManager = new OntologyClassManagement(file); 
		reasoner = ontologyManager.getElkreasoner();
		man = ontologyManager.getManager();
		factory = ontologyManager.getManager().getOWLDataFactory();
		ontology = ontologyManager.getOntology();
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		setConstants();
		
		//we don't have maps available to us.
		//so, we'll create them again within the eq mananger class
		//and then run the diff method as usual
		this.eqManager = new EquivalentMappingManager(reasoner, factory, ontology, substanceClass , numberClass, medicinalProduct);
		ingEquivalencyMap = eqManager.getIngredientMap();
		numberEquivalencyMap = eqManager.getNumberMap();
		mappings = eqManager.getEquivalentMappings();
		diff(mappings);
		report();
	}

	public boolean isAsserted(RxNormSCD scd) {
		boolean asserted = false;
		for(Long id : scd.getSnomedCodes() ) {
			String snomedLabel = getRDFSLabelForLong(id);
			if( snomedLabel != null && snomedLabel.contains("(clinical drug)") ) {
				asserted = true;
			}
		}		
		return asserted;
	}


	public void setConstants() {
		this.medicinalProduct = factory.getOWLClass(namespace, "763158003");
		this.numberClass = factory.getOWLClass(namespace, "260299005");
		this.substanceClass = factory.getOWLClass(namespace, "105590001");
		this.temporaryClass = factory.getOWLClass(namespace, "770654000");
		this.hasActiveIngredient = factory.getOWLObjectProperty(namespace, "127489000");
		this.roleGroup = factory.getOWLObjectProperty(namespace, "609096000");
		this.countOfBaseOfActiveIngredient = factory.getOWLObjectProperty(namespace, "766952006");
		this.hasPreciseActiveIngredient = factory.getOWLObjectProperty(namespace, "762949000");
		this.hasManufacturedDoseForm = factory.getOWLObjectProperty(namespace, "411116001");
		this.hasBasisOfStrengthSubstance = factory.getOWLObjectProperty(namespace, "732943007");
		this.hasPresentationStrengthNumeratorUnit = factory.getOWLObjectProperty(namespace, "732945000");
		this.hasPresentationStrengthNumeratorValue = factory.getOWLObjectProperty(namespace, "732944001");
		this.hasPresentationStrengthDenominatorUnit = factory.getOWLObjectProperty(namespace, "732947008");
		this.hasPresentationStrengthDenominatorValue = factory.getOWLObjectProperty(namespace, "732946004");
		this.hasConcentrationStrengthNumeratorUnit = factory.getOWLObjectProperty(namespace, "733725009");
		this.hasConcentrationStrengthNumeratorValue = factory.getOWLObjectProperty(namespace, "733724008");
		this.hasConcentrationStrengthDenomniatorUnit = factory.getOWLObjectProperty(namespace, "733722007");
		this.hasConcentrationStrengthDenominatorValue = factory.getOWLObjectProperty(namespace, "733723002");		
		this.hasUnitOfPresentation = factory.getOWLObjectProperty(namespace, "763032000");
		this.isModificationOf = factory.getOWLObjectProperty(namespace, "738774007");		
		
		this.mapsToCode = factory.getOWLAnnotationProperty(namespace, "MapsToCode");
		this.mapsToName = factory.getOWLAnnotationProperty(namespace, "MapsToName");
		this.inferred = factory.getOWLAnnotationProperty(namespace, "Inferred");
		this.asserted = factory.getOWLAnnotationProperty(namespace, "Asserted");
		this.hasNDC = factory.getOWLAnnotationProperty(namespace, "HasNDC");
		this.hasExplanation = factory.getOWLAnnotationProperty(namespace, "HasExplanation");		
		this.hasRxCUI = factory.getOWLAnnotationProperty(namespace, "HasCUI");		
		this.substanceNoExist = factory.getOWLAnnotationProperty(namespace, "SubstanceNotExist");
		this.substanceDifferent = factory.getOWLAnnotationProperty(namespace, "SubstanceDifferent");
		this.bossSubstanceDifferent = factory.getOWLAnnotationProperty(namespace, "BossSubstanceDifferent");
		this.activeIngerdientSubstanceDifferent = factory.getOWLAnnotationProperty(namespace, "ActiveIngDifferent");		
		this.doseFormDifferent = factory.getOWLAnnotationProperty(namespace, "DoseFormDifferent");
		this.presUnitDifferent = factory.getOWLAnnotationProperty(namespace, "PresUnitDifferent");
		this.unitsDifferent = factory.getOWLAnnotationProperty(namespace, "UnitsDifferent");		
		this.valuesDifferent = factory.getOWLAnnotationProperty(namespace, "ValuesDifferent");
		this.countBaseDifferent = factory.getOWLAnnotationProperty(namespace, "CountOfBaseDifferent");
		this.vetOnly = factory.getOWLAnnotationProperty(namespace, "VetOnly");
		this.hasAllergenic = factory.getOWLAnnotationProperty(namespace, "HasAllergenic");
		this.isPrescribable = factory.getOWLAnnotationProperty(namespace, "IsPrescribable");		
		
		this.qaChecks.add(this.hasManufacturedDoseForm);
		this.qaChecks.add(this.hasUnitOfPresentation);
		this.qaChecks.add(this.hasBasisOfStrengthSubstance);
		this.qaChecks.add(this.hasPreciseActiveIngredient);
		this.qaChecks.add(this.hasConcentrationStrengthNumeratorValue);		
		this.qaChecks.add(this.hasConcentrationStrengthNumeratorUnit);
		this.qaChecks.add(this.hasConcentrationStrengthDenominatorValue);		
		this.qaChecks.add(this.hasConcentrationStrengthDenomniatorUnit);
		this.qaChecks.add(this.hasPresentationStrengthNumeratorValue);		
		this.qaChecks.add(this.hasPresentationStrengthNumeratorUnit);
		this.qaChecks.add(this.hasPresentationStrengthDenominatorValue);		
		this.qaChecks.add(this.hasPresentationStrengthDenominatorUnit);
		this.qaChecks.add(this.countOfBaseOfActiveIngredient);

		this.qaUnitChecks.add(this.hasConcentrationStrengthNumeratorUnit);
		this.qaUnitChecks.add(this.hasConcentrationStrengthDenomniatorUnit);
		this.qaUnitChecks.add(this.hasPresentationStrengthNumeratorUnit);
		this.qaUnitChecks.add(this.hasPresentationStrengthDenominatorUnit);

		this.qaValueChecks.add(this.hasConcentrationStrengthNumeratorValue);
		this.qaValueChecks.add(this.hasConcentrationStrengthDenominatorValue);
		this.qaValueChecks.add(this.hasPresentationStrengthNumeratorValue);
		this.qaValueChecks.add(this.hasPresentationStrengthDenominatorValue);
		
	}	

	// Test for various matching objects in definitions.  First to fail is the
	// explanation.
	// 1. Dose
	// 2. Unit
	// 3. BoSS
	// 4. Active Ingredient
	// 5. Units
	// 6. Unit Values
	private void diff(Set<EquivalentMapping> mappings) {
		for( EquivalentMapping map : mappings ) {
			
			//i will probably need to add a check back to be sure the scd is not OOS (out of scope)
//			if( map.getIsAsserted() && !map.getIsInferred() ) {
			if( map.getSnomedClass() != null ) {

				setMapObjectProperties(map);
				//				if( map.getSnomedCode().equals(Long.valueOf(330201008))) {
				//					System.out.println("break");
				//				}
				log("Snomed Class " + map.getSnomedCode() + "\t" + map.getSnomedName());
				
//				if(map.getSnomedCode().equals(Long.valueOf("318740009"))) {
//					System.out.println("PAUSE"); //249747	Indoramin 25 MG Oral Tablet
//				}
//				if(map.getSnomedCode().equals(Long.valueOf("377340006"))) {
//					System.out.println("PAUSE"); //833036	Acetaminophen 750 MG / Hydrocodone Bitartrate 7.5 MG Oral Tablet
//				}				
				
				Vector<OWLClass> bossClasses = new Vector<OWLClass>();				
				ArrayList<OWLClass> bossList = map.getExplanationMap().get(this.hasBasisOfStrengthSubstance);
				if( bossList != null && !bossList.isEmpty()) {
					for(OWLClass c : bossList) {
						bossClasses.add(c);
					}
				}
				
				if( map.getExplanationMap().get(this.hasPresentationStrengthNumeratorUnit) != null &&
						map.getExplanationMap().get(this.hasPresentationStrengthNumeratorUnit).size() > 0 ) {
					map.setSnomedPresentationTemplate(true);
				}
				if( map.getExplanationMap().get(this.hasConcentrationStrengthNumeratorUnit) != null &&
						map.getExplanationMap().get(this.hasConcentrationStrengthNumeratorUnit).size() > 0 ) {
					map.setSnomedConcentrationTemplate(true);
				}				

				
//				Vector<String> snomedBossNames = new Vector<String>();
//				Vector<String> snomedBossUnits = new Vector<String>();
//				Vector<String> snomedBossValues = new Vector<String>();
				for( OWLObjectProperty op : qaChecks ) {
					ArrayList<OWLClass> clsList = map.getExplanationMap().get(op);
					
					if( clsList != null ) {
						for( OWLClass cls : clsList ) {
							RxNormSCD scd = map.getRxNormSCD();
							//							boolean isOralSolidDose = isThisASolidDose(scd);						
							if( !standaloneMode && op.equals(this.hasManufacturedDoseForm) ) {
								if( scd.getManufacturedDoseFormCode() != null ) {
									OWLClass scdDf = factory.getOWLClass(namespace, String.valueOf(scd.getManufacturedDoseFormCode()));
									if( !cls.getIRI().equals(scdDf.getIRI()) ) {
										addAnnotation(map.getSnomedClass(), this.doseFormDifferent, "Dose form " + getRDFSLabel(cls) + " does not map to SCD DF " + scd.getRxNormDoseForm().elementAt(0).getName() + ". Configuration expects " + scd.getManufacturedDoseFormName());
										addAnnotation(map.getRxClass(), this.doseFormDifferent, "Dose form " + getRDFSLabel(cls) + " does not map to SCD DF " + scd.getRxNormDoseForm().elementAt(0).getName() + ". Configuration expects " + scd.getManufacturedDoseFormName());
										log("Dose form " + getRDFSLabel(cls) + " does not map to SCD DF " + scd.getRxNormDoseForm().elementAt(0).getName() + ". Configuration expects " + scd.getManufacturedDoseFormName());
										map.setWrongExplanation("boss", cls, factory.getOWLClass(namespace, String.valueOf(scd.getManufacturedDoseFormCode())));
									}
									else {
										map.setSameMdf(true);
									}
								}
							}
							else if( !standaloneMode && op.equals(this.hasUnitOfPresentation)) {
								if( scd.getUnitOfPresentationCode() != null ) {
									OWLClass scdUp = factory.getOWLClass(namespace, String.valueOf(scd.getUnitOfPresentationCode()));
									if( !cls.getIRI().equals(scdUp.getIRI())) {
										
										if( scd.getUnitOfPresentationName() == null || scd.getUnitOfPresentationName().isEmpty()) {
											addAnnotation(map.getSnomedClass(), this.presUnitDifferent, "rx: " + "mapped dose form doesn't expect a unit of presentation");
											addAnnotation(map.getRxClass(), this.presUnitDifferent, "sct: " + getRDFSLabel(cls));
										}
										else {
											addAnnotation(map.getSnomedClass(), this.presUnitDifferent, "rx: " + map.getRxNormSCD().getUnitOfPresentationName());
											addAnnotation(map.getRxClass(), this.presUnitDifferent, "sct: " + getRDFSLabel(cls));
										}
										map.setExplanationMap(op, cls);
										log("Unit of presentation\t" + getRDFSLabel(cls) + "\tdoes not map to SCD DF\t" + scd.getRxNormDoseForm().elementAt(0).getName() + "\twith expected UP\t" + scd.getUnitOfPresentationName());
										map.setWrongExplanation("uop", cls, factory.getOWLClass(namespace, String.valueOf(scd.getUnitOfPresentationCode())));
										map.setSameUnit(false);										
									}
									else {
										map.setSameUnit(true);
									}
								}
							}
							//These object properties are in a BoSS which are random and have no unique identifiers to them
							//we'll loop through all available values to each predicate
							else if(op.equals(this.hasBasisOfStrengthSubstance)) {
								if( this.ingEquivalencyMap.containsKey(cls) ) {
									boolean found = false;								
									ArrayList<OWLClass> eqIngs = ingEquivalencyMap.get(cls);
									for( int i=0; i < scd.getRxNormBoss().size() && !found; i++) {
										OWLClass rxBossClass = factory.getOWLClass(namespace, "Rx" + scd.getRxNormBoss().get(i).getBossRxCui());
										if( eqIngs != null && eqIngs.contains(rxBossClass) ) {
											found = true;
										}
									}
									if( !found ) {
										//											map.setExplanationMap(op, cls);
//										OWLClass modificationOf = getModificationOf(cls, null, map, "BoSS");
										ArrayList<OWLClass> modifiedList = checkIsModificationOf(cls);
										if( modifiedList != null ) {
											for( OWLClass clz : modifiedList ) {
												addAnnotation(map.getRxClass(), this.bossSubstanceDifferent, "SCT BoSS " + getRDFSLabel(cls) + " does not match this RxNorm BoSS. The substance is a modification of " + getRDFSLabel(clz));
												addAnnotation(map.getSnomedClass(), this.bossSubstanceDifferent, "RxNorm BoSS is different from " + getRDFSLabel(cls) + ". The substance is a modiciation of " + getRDFSLabel(clz));
												map.setWrongExplanation("boss", cls, clz);
												map.setWrongExplanation("modificationof", cls, clz);
											}
										}
										else {
											addAnnotation(map.getRxClass(), this.bossSubstanceDifferent, "SCT BoSS: " + getRDFSLabel(cls));											
											addAnnotation(map.getSnomedClass(), this.bossSubstanceDifferent, "RxNorm BoSS is different from " + getRDFSLabel(cls));	
											map.setWrongExplanation("boss", cls, null);
										}
//										map.addExplanation("SCT Basis of strength substance\t" + getRDFSLabel(cls) + "\tisn't equivalent to a RxNorm BoSS.");
										map.setSameBoss(Boolean.FALSE);
									}
									else {
										map.setSameBoss(Boolean.TRUE);
									}
								}
								else {
//									OWLClass modificationOf = getModificationOf(cls, null, map, "NEQ BoSS");
									ArrayList<OWLClass> modifiedList = checkIsModificationOf(cls);
									if( modifiedList != null ) {
										for( OWLClass clz : modifiedList ) {
											addAnnotation(map.getRxClass(), this.bossSubstanceDifferent, "SCT BoSS " + getRDFSLabel(cls) + " does not match this RxNorm BoSS. The substance is a modification of " + getRDFSLabel(clz));
											addAnnotation(map.getSnomedClass(), this.bossSubstanceDifferent, "RxNorm BoSS is different from " + getRDFSLabel(cls) + ". The substance is a modiciation of " + getRDFSLabel(clz));
											map.setWrongExplanation("boss", cls, clz);
											map.setWrongExplanation("modificationof", cls, clz);
										}
									}									
									else {
										addAnnotation(map.getSnomedClass(), this.bossSubstanceDifferent, getRDFSLabel(cls) + " does not have a rxnorm equivalent");
										addAnnotation(map.getRxClass(), this.bossSubstanceDifferent, "sct " + getRDFSLabel(cls) + " does not have a rxnorm equivalent");
										map.setWrongExplanation("boss", cls, null);
									}
//									map.addExplanation("SCT basis of strength substance " + getRDFSLabel(cls) + " is not equivalent to an RxNorm ingredient.");
									map.setSameBoss(Boolean.FALSE);
								}
							}
							else if(op.equals(this.hasPreciseActiveIngredient)) {

								if( this.ingEquivalencyMap.containsKey(cls) ) {								
									boolean found = false;
									ArrayList<OWLClass> eqIngs = ingEquivalencyMap.get(cls);
									for( int i=0; i < scd.getRxNormBoss().size() && !found; i++) {
										OWLClass rxIngClass = factory.getOWLClass(namespace, "Rx" + scd.getRxNormBoss().get(i).getActiveIngredientRxCui());
										if( eqIngs != null && eqIngs.contains(rxIngClass) ) {
											found = true;
										}
									}
									if( !found ) {
										//										map.setExplanationMap(op, cls); 
//										OWLClass modificationOf = getModificationOf(cls, null, map, "Act Ing");
										ArrayList<OWLClass> modifiedList = checkIsModificationOf(cls);
										if( modifiedList != null ) {
											for( OWLClass clz : modifiedList ) {
												log("AI " + getRDFSLabel(cls) + " NOT FOUND as an equivalent. Best guess is the modified substance: " + getRDFSLabel(clz));
												addAnnotation(map.getRxClass(), this.activeIngerdientSubstanceDifferent, "sct " + getRDFSLabel(cls));						
												addAnnotation(map.getSnomedClass(), this.activeIngerdientSubstanceDifferent, "rx lacks: " + getRDFSLabel(cls));												
											}
										}
										else {
											log("AI " + getRDFSLabel(cls) + " NOT FOUND as an equivalent. No best guess.");
											addAnnotation(map.getSnomedClass(), this.activeIngerdientSubstanceDifferent, getRDFSLabel(cls) + " does not have a rxnorm equivalent");
											addAnnotation(map.getRxClass(), this.activeIngerdientSubstanceDifferent, "sct: " + getRDFSLabel(cls));
										}
//										map.addExplanation("SCT Precise Active Ingredient\t" + getRDFSLabel(cls) + "\tisn't equivalent to a RxNorm BoSS Act Ing.");
										map.setActIng(Boolean.FALSE);
									}
									else {
										map.setActIng(Boolean.TRUE);
									}
								}
								else {									
//									OWLClass modificationOf = getModificationOf(cls, null, map, "Act Ing");
									ArrayList<OWLClass> modifiedList = checkIsModificationOf(cls);
									if( modifiedList != null ) {
										for( OWLClass clz : modifiedList ) {
											log("AI " + getRDFSLabel(cls) + " NOT FOUND as an equivalent. Best guess is the modified substance: " + getRDFSLabel(clz));	
										}
									}
									else {
										
									}
									addAnnotation(map.getSnomedClass(), this.activeIngerdientSubstanceDifferent, "sct substance with no rx eq: " + getRDFSLabel(cls));
									addAnnotation(map.getRxClass(), this.activeIngerdientSubstanceDifferent, "sct substance with no rx eq: " + getRDFSLabel(cls));
//									map.addExplanation("SCT Precise Active Ingredient\t" + getRDFSLabel(cls) + "\tis not equivalent to an RxNorm ingredient.");
									map.setActIng(Boolean.FALSE);
								}
							}
							else if( op.equals(this.countOfBaseOfActiveIngredient) ) {
								int count = 0;

								String countString = getRDFSLabel(cls).replace(" (qualifier value)", "");
								count = Integer.valueOf(countString).intValue();

								map.setSnomedCountOfBase(count);
								map.setRxCountOfBase(Integer.valueOf(scd.getBaseCuiCount()));
								if( map.getSnomedCountOfBase() != map.getRxCountOfBase() ) {
									addAnnotation(map.getSnomedClass(), this.countBaseDifferent, "rxnorm: " + map.getRxCountOfBase() );
									addAnnotation(map.getRxClass(), this.countBaseDifferent, "sct: " + map.getSnomedCountOfBase());
								}

							}
							else if( !standaloneMode && this.qaUnitChecks.contains(op) ) {
									if( this.unitEquivalencyMap.containsKey(cls) || isThisASolidDose(getRDFSLabel(cls)) ) {
										ArrayList<OWLClass> eqUnits = new ArrayList<OWLClass>();
										boolean found = false;
										
										if( this.unitEquivalencyMap.get(cls) != null || isThisASolidDose(getRDFSLabel(cls))) {
											eqUnits.add(cls);
										}

										for( int i=0; !eqUnits.isEmpty() && i < scd.getRxNormBoss().size() && !found; i++ ) {
											RxNormBoss boss = scd.getRxNormBoss().get(i);
											if( bossClasses != null ) {
												boolean foundBoss = false;												
												for( OWLClass bossClass : bossClasses ) {
													ArrayList<OWLClass> sctins = ingEquivalencyMap.get(bossClass);
													if( sctins != null && sctins.contains(factory.getOWLClass(namespace, "Rx" + String.valueOf(boss.getBossRxCui()))) )	{												
//													ArrayList<OWLClass> eqBossesIngs = this.ingEquivalencyMap.get(bossClass);
//													OWLClass rxClassIng = factory.getOWLClass(namespace, "Rx" + boss.getBossRxCui());
//													if( eqBossesIngs.contains(rxClassIng) ) {
														foundBoss = true;
														OWLClass unitClass = null;
														ConcentrationToPresentation cp = boss.getConcentrationToPresentation();
														boolean hasCp = false;
														if( cp != null ) hasCp = true;													
		
														if( !hasCp ) {
															if( op.equals(this.hasConcentrationStrengthNumeratorUnit) || op.equals(this.hasPresentationStrengthNumeratorUnit) ) {									
																unitClass = getOWLClassForString(boss.getSnomedNumeratorUnit(), unitEquivalencyMap);
															}
															else if( op.equals(this.hasPresentationStrengthDenominatorUnit) ) {
																if( isThisASolidDose(scd) && isThisASolidDose(getRDFSLabel(cls)) ) unitClass = cls;
															}
															else {
																if( !isThisASolidDose(scd) ) {
																	unitClass = getOWLClassForString(boss.getSnomedDenominatorUnit(), unitEquivalencyMap);		
																}
																else {
																	unitClass = factory.getOWLClass(namespace, String.valueOf(scd.getUnitOfPresentationCode()));
																}
															}
														}
														else {
															if( op.equals(this.hasPresentationStrengthNumeratorUnit ) ) {
																unitClass = getOWLClassForString(cp.getPresentationNumeratorUnit(), unitEquivalencyMap);
															}
															else if( op.equals(this.hasPresentationStrengthDenominatorUnit) ) {
																unitClass = getOWLClassForString(cp.getPresentationDenominatorUnit(), unitEquivalencyMap);																
															}
															else if( op.equals(this.hasConcentrationStrengthNumeratorUnit) ) {
																unitClass = getOWLClassForString(boss.getSnomedNumeratorUnit(), unitEquivalencyMap);
															}
															else if( op.equals(this.hasConcentrationStrengthDenomniatorUnit) ) {
																unitClass = getOWLClassForString(boss.getSnomedDenominatorUnit(), unitEquivalencyMap);
															}
														}
		
														if( unitClass != null ) {
															found = true;
															break;
														}									
													}
													else {
														if( !foundBoss ) {
															addAnnotation(map.getSnomedClass(), this.unitsDifferent, "units unintelligible different BoSS: " + getRDFSLabel(bossClass));
															addAnnotation(map.getRxClass(), this.unitsDifferent, "units unintelligible different BoSS: " + getRDFSLabel(bossClass));
															map.setWrongUnits(getRDFSLabel(cls));
															break;
														}														
													}
												}
											}
										}
										if( !found ) {
											addAnnotation(map.getSnomedClass(), this.unitsDifferent , "rx lacks: " + getRDFSLabel(cls) );
											addAnnotation(map.getRxClass(), this.unitsDifferent, "sct: " + getRDFSLabel(cls));
											map.setWrongUnits(getRDFSLabel(cls));
										}
								}
								else {
									addAnnotation(map.getSnomedClass(), this.unitsDifferent, "rx lacks: " + getRDFSLabel(cls));
									addAnnotation(map.getRxClass(), this.unitsDifferent, "sct: " + getRDFSLabel(cls));									
									map.setWrongUnits(getRDFSLabel(cls));
								}
							}

							else if( !standaloneMode && this.qaValueChecks.contains(op) ) {
										String snomedClassLabel = getRDFSLabelForClass(cls);
										String snomedClassValue = null;
										try {
											snomedClassValue = String.valueOf(snomedClassLabel.replace(" (qualifier value)", ""));
										}
										catch(Exception e) {
											log("Could not create a number for class " + cls.getIRI().getIRIString() + " (" + snomedClassLabel + ")");
										}
										boolean found = false;							
										String rxVal = null;										
										for( int i=0; i < scd.getRxNormBoss().size() && !found; i++ ) {
											RxNormBoss boss = scd.getRxNormBoss().get(i);
//											ingEquivalencyMap.get(bossClass).contains(factory.getOWLClass(namespace, String.valueOf(boss.getBossRxCui())))											
											if( bossClasses != null ) {
												//if the BoSS is wrong, then how do I check the values?
												//premise is the boss should equal on both sides and then the
												//values can be checked
												boolean foundBoss = false;
												for( OWLClass bossClass : bossClasses ) {
													ArrayList<OWLClass> sctins = ingEquivalencyMap.get(bossClass);
													if( sctins != null && sctins.contains(factory.getOWLClass(namespace, "Rx" + String.valueOf(boss.getBossRxCui()))) ) {
														foundBoss = true;
														if( op.equals(this.hasConcentrationStrengthNumeratorValue) ) {
															rxVal = boss.getSnomedNumberatorValue();
														}
														else if( op.equals(this.hasConcentrationStrengthDenominatorValue) ) {
															rxVal = boss.getSnomedDenominatorValue();
														}
														else if( op.equals(this.hasPresentationStrengthNumeratorValue) ) {						
															rxVal = boss.getSnomedNumberatorValue();
														}
														else if( op.equals(this.hasPresentationStrengthDenominatorValue) ) {						
															rxVal = boss.getSnomedDenominatorValue();
														}											
														if( rxVal != null && snomedClassValue != null && Double.valueOf(rxVal).equals(Double.valueOf(snomedClassValue)) ) {
															found = true;
															break;
														}														
													}
												}
												if( !foundBoss ) {
													addAnnotation(map.getSnomedClass(), this.valuesDifferent, "unable to compare, unknown BoSS");
													addAnnotation(map.getRxClass(), this.valuesDifferent, "unable to compare, unknown BoSS");
													break;
												}
											}
										}
										if( !found ) {
											//addAnnotation(map.getRxClass(), this.valuesDifferent, String.valueOf(snomedClassLabel));
											if( snomedClassValue != null ) {
												map.setWrongValues(null);
												addAnnotation(map.getSnomedClass(), this.valuesDifferent , "rx lacks: " + String.valueOf(snomedClassValue));
												addAnnotation(map.getSnomedClass(), this.valuesDifferent , "sct: " + String.valueOf(snomedClassValue));												
											}											
										}
								}
						}
					}										
				}
			}
			else {
				map.setIsOOS(true);
			}
			finalizedMappings.add(map);
		}				
	}

	private boolean isThisASolidDose(RxNormSCD scd) {
		boolean solid = false;
		RxNormDoseForm df = scd.getRxNormDoseForm().get(0);
		String doseForm = df.getName().toLowerCase();
		if( doseForm.contains("tablet") || doseForm.contains("capsule") 
				|| doseForm.contains("suppository")) {
			solid = true;
		}
		return solid;
	}

	private boolean isThisASolidDose(String scd) {
		boolean solid = false;
		String doseForm = scd.toLowerCase();
		if( doseForm.contains("tablet") || doseForm.contains("capsule") 
				|| doseForm.contains("suppository")) {
			solid = true;
		}
		return solid;
	}

	private void setMapObjectProperties(EquivalentMapping map) {

		OWLClass snomedClass = factory.getOWLClass(namespace, String.valueOf(map.getSnomedCode()));
//211109: SNOMED No longer has a TEMPORARY CD class, so this was throwing a null
//		if( reasoner.getSuperClasses(snomedClass, false).containsEntity(this.temporaryClass) ) {
//			map.setTemporary(true);
//			//			System.out.println("Found a temporary snomed class " + snomedClass.getIRI().getIRIString() + " (" + getRDFSLabel(snomedClass) + ") for mapping to " + map.getRxCui() + " (" + map.getRxName() + ")");
//		}
//		else {
		try {
			for(OWLClass eqClass : reasoner.getEquivalentClasses(snomedClass).entities().collect(Collectors.toSet())) {
				ontology.equivalentClassesAxioms(eqClass).forEach(
						x -> x.classExpressions().forEach( y -> {
							if( y instanceof OWLNaryBooleanClassExpression) {
								for( OWLObjectProperty op : qaChecks) {
									processInnerNAryExpression(y, op, map);
								}
							}
						}
								));			
			}
		} catch(Exception e) {
			System.out.print("Problem getting equivalent classes for: " + map.getSnomedCode() + "\t" +  map.getSnomedClass());
			e.printStackTrace();
		}
	}

	public void processInnerNAryExpression(OWLClassExpression oce, OWLObjectProperty op, EquivalentMapping map) {
		if (oce instanceof OWLObjectIntersectionOf ) {
			Set<OWLClassExpression> expressionSet = ((OWLNaryBooleanClassExpression) oce).operands().collect(Collectors.toSet());
			for(OWLClassExpression expression : expressionSet ) {
				processInnerNAryExpression(expression, op, map);
			}
		}
		if (oce instanceof OWLObjectSomeValuesFrom ) {
			//			System.out.println("ClassExpression " + oce.toString());
			OWLObjectPropertyExpression a = ((OWLObjectSomeValuesFrom) oce).getProperty();
			String propertyName = getObjectName(a);
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom) oce).getFiller();
			if( filler.isOWLClass() ) {
				if( propertyName.equals(getObjectName(op)) ) {
					//					System.out.println(getObjectName(op));
					map.setExplanationMap(op, filler.asOWLClass());
				}
			}
			else { //the filler is an intersection
				processInnerNAryExpression(filler, op, map);
			}
			//			OWLObjectSomeValuesFrom restriction = (OWLObjectSomeValuesFrom) oce;
			//			OWLClassExpression filler = restriction.getFiller();
			//			System.out.println("Property Expression: " + a.toString());
			//			System.out.println("Filler: " + filler.toString());
		}
	}
	
	public ArrayList<OWLClass> checkIsModificationOf(OWLClass cls) {
		EquivalentMapping modificationMap = new EquivalentMapping();		
		ontology.axioms(cls).filter(y -> y instanceof OWLSubClassOfAxiom).forEach(x -> {
			Set<OWLClassExpression> expressionSet = x.nestedClassExpressions().collect(Collectors.toSet());
			for(OWLClassExpression expression : expressionSet ) {			
				processInnerNAryExpression(expression, this.isModificationOf, modificationMap);
			}
		}
		);
		
//		ArrayList<OWLClass> modificationList = modificationMap.getExplanationMap().get(this.isModificationOf);
//		if( modificationList != null ) {
//			for( OWLClass m : modificationList ) {
//				System.out.println(getRDFSLabel(cls) + " is modification of " + getRDFSLabel(m));
//			}
//		}
		
		ArrayList<OWLClass> modificationList = modificationMap.getExplanationMap().get(this.isModificationOf);
		return modificationList;	
	}

	public String getObjectName(OWLObjectPropertyExpression a) {
		String name = null;
		for( OWLObjectProperty op : a.objectPropertiesInSignature().collect(Collectors.toSet()) ) {
			name = getRDFSLabel(op);
		}
		return name;
	}

	public String getRDFSLabel(OWLEntity cls) {
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
	}		

	public void addToMap(Integer cui, Long snomedId, TreeMap<Integer, Set<Long>> map) {
		if( map.containsKey(cui) ) {
			Set<Long> list = map.get(cui);
			list.add(snomedId);
			map.put(cui, list);
		}
		else {
			Set<Long> list = new HashSet<Long>();
			list.add(snomedId);
			map.put(cui, list);
		}		
	}

	public static JSONObject getresult(String URLtoRead) throws IOException {
		URL url;
//		HttpsURLConnection connexion;
		HttpURLConnection connexion;	
		BufferedReader reader;

		String line;
		String result="";
		url= new URL(URLtoRead);

//		connexion= (HttpsURLConnection) url.openConnection();
		connexion= (HttpURLConnection) url.openConnection();		
		connexion.setRequestMethod("GET");
		reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));	
		while ((line =reader.readLine())!=null) {
			result += line;

		}

		JSONObject json = new JSONObject(result);
		return json;
	}

	public String getRDFSLabelForLong(Long code) {
		OWLClass cls = factory.getOWLClass(namespace, String.valueOf(code));
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
	}		

	public String getRDFSLabelForClass(OWLClass cls) {
		try {
			Set<OWLAnnotation> annos = EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet());
			for (OWLAnnotation a : annos) {
				OWLAnnotationValue val = a.getValue();
				if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
				else return val.toString();
			}
		}
		catch(Exception e) {
			System.out.println("Entity search failure on: " + cls.getIRI().getIRIString());
			return null;			
		}

		return null;
	}

	public void report() {

		//		FileWriter fileWriter = null;
		//		PrintWriter printWriter = null;
		ExcelReport qaOverview = new ExcelReport("./RxNorm2SnomedCT_QAOverview-" + globalDate + ".xls");


		//TODO: Break into separate sheets, use POI
		ArrayList<EquivalentMapping> differentMdf = new ArrayList<EquivalentMapping>();
		ArrayList<EquivalentMapping> differentUp = new ArrayList<EquivalentMapping>();
		ArrayList<EquivalentMapping> differentBoss = new ArrayList<EquivalentMapping>();
		ArrayList<EquivalentMapping> differentAI = new ArrayList<EquivalentMapping>();
		ArrayList<EquivalentMapping> differentUnits = new ArrayList<EquivalentMapping>();
		ArrayList<EquivalentMapping> differentValues = new ArrayList<EquivalentMapping>();
		ArrayList<EquivalentMapping> differentCountOfBase = new ArrayList<EquivalentMapping>();
		ArrayList<EquivalentMapping> noNDC = new ArrayList<EquivalentMapping>();
		ExcelReport qaMDF = new ExcelReport("./RxNorm2SnomedCT_QA_ManufacturedDoseForm.xls");
		ExcelReport qaUP = new ExcelReport("./RxNorm2SnomedCT_QA_UnitOfPresentation.xls");
		ExcelReport qaBoss = new ExcelReport("./RxNorm2SnomedCT_QA_BoSS_Names.xls");
		ExcelReport kqaAi = new ExcelReport("./RxNorm2SnomedCT_QA_Known_Precise_Active_Ingredient.xls");
		ExcelReport uqaAi = new ExcelReport("./RxNorm2SnomedCT_QA_Unkonwn_Precise_Active_Ingredient.xls");		
		ExcelReport unmappedScds = new ExcelReport("./RxNorm2SnomedCT_QA_Unmapped_Scds.xls");
		ExcelReport falsePositivesReport = new ExcelReport("./RxNorm2SnomedCT_QA_False_Positives.xls");
		ExcelReport duplicateIngredients = new ExcelReport("./RxNorm2SnomedCT_QA_RxNorm_Same_Ingredients.xls");
		ExcelReport differentBossUnits = new ExcelReport("./RxNorm2SnomedCT_QA_RxNorm_BoSS_Units.xls");
		ExcelReport differentBossValues = new ExcelReport("./RxNorm2SnomedCT_QA_RxNorm_BoSS_Values.xls");
		ExcelReport equivalentIngredients = new ExcelReport("./RxNorm2SnomedCT_QA_Equivalent_Ingredients.xls");	
		ExcelReport noDefinitionTemplate = new ExcelReport("./RxNorm2SnomedCT_QA_No_Definition_Template.xls");
		ExcelReport differentCountOfBaseReport = new ExcelReport("./RxNorm2SnomedCT_QA_No_Definition_Template.xls");
		ExcelReport noNDCReport = new ExcelReport("./RxNorm2SnomedCT_QA_No_NDC.xls");

		ExcelReport multiSheet = new ExcelReport("./RxNorm2SnomedCT_Monster_QA.xls-" + globalDate + ".xls");

		Vector<OWLClass> seen = new Vector<OWLClass>();
		qaOverview.printHeader("RxCUI SCD\tRxName SCD\tAsserted and/or Inferred\tRx Dose Form\tTemplate\tDiff DF\tDiff UP\tDiff BoSS\tDiff Active Ingredient\tDiff Unit\tDiff Unit Value\tDiff Count Base\tNo NDC\tNot Prescribable\tIs Vaccine\tHas Qualitative Distinction\tDiff Template\tSCT Code CD\tSCT Name CD" );
		multiSheet.printHeader("RxCUI SCD\tRxName SCD\tAsserted and/or Inferred\tRx Dose Form\tTemplate\tDiff DF\tDiff UP\tDiff BoSS\tDiff Active Ingredient\tDiff Unit\tDiff Unit Value\tDiff Count Base\tNo NDC\tNot Prescribable\tIs Vaccine\tHas Qualitative Distinction\tDiff Template\tSCT Code CD\tSCT Name CD" );
		for(EquivalentMapping map : finalizedMappings ) {
			if( !seen.contains(map.getRxClass()) ) {  //although, many to one?
				seen.add(map.getRxClass());
				String reportRow = new String("");
				reportRow = reportRow.concat(map.getRxCui() + "\t" + map.getRxName() + "\t" );
				if( map.getIsAsserted() && map.getIsInferred() ) {
					reportRow = reportRow.concat("AI");
				}
				else if( map.getIsAsserted() && !map.getIsInferred() ) {
					reportRow = reportRow.concat("A");
				}
				else if( !map.getIsAsserted() && map.getIsInferred() ) {
					reportRow = reportRow.concat("I");
				}
				else {
					reportRow = reportRow.concat("");
				}			

				reportRow = reportRow.concat("\t");
				reportRow = printQaReasonString(reportRow, map.getRxNormSCD().getRxNormDoseForm().get(0).getName());
				
				String rxnormTemplate = "";
				
				if( map.getRxNormSCD().getSnomedDefinitionTemplate() != null ) {
					if( map.getRxNormSCD().getSnomedDefinitionTemplate().getTemplateType() != null) {
						rxnormTemplate = map.getRxNormSCD().getSnomedDefinitionTemplate().getTemplateType().toUpperCase();
						reportRow = printQaReasonString(reportRow, rxnormTemplate);
					}
				}
				else {
					reportRow = printQaReasonString(reportRow, "");
				}			
				if( !map.isOOS ) {
					if( map.getIsAsserted() && !map.getIsInferred() ) {				
						reportRow = printQaReason(reportRow, map.getSameMdf());
						reportRow = printQaReason(reportRow, map.getSameUnit());
						if( map.getSameBoss().contains(Boolean.FALSE) ) reportRow = printQaReason(reportRow, false); else reportRow = printQaReason(reportRow, true);
						if( map.getActIng().contains(Boolean.FALSE) ) reportRow = printQaReason(reportRow, false); else reportRow = printQaReason(reportRow, true);
						
						//an x means the issue exists
						//a y means the issue exists due to the former issue
						//anything else is a more descriptive reason there is an issue (e.g., unit of presentation difference)
						if( !map.getWrongUnits().isEmpty() ) {
							String reason = "x";					
							if( map.getSameBoss().contains(Boolean.FALSE) ) {
								reason = "y";
							}
							for( String unit : map.getWrongUnits() ) {
								if( unit.contains("(unit of presentation)")) {
									reason = unit;
								}
							}
		//running out of letters :)
		//this might be superfluous
		//					if( map.getActIng().contains(Boolean.FALSE)) {
		//						reason = "z";
		//					}
							reportRow = printQaReasonString(reportRow, reason);
						}
						else {
							reportRow = printQaReason(reportRow, true);
						}
						
						if( !map.getWrongValues().isEmpty() ) {
							String reason = "x";
							if( map.getSameBoss().contains(Boolean.FALSE) ) {
								reason = "y";
							}
							//					if( map.getActIng().contains(Boolean.FALSE)) {
							//						reason = "z";
							//					}
							reportRow = printQaReasonString(reportRow, reason);
						}
						else {
							reportRow = printQaReason(reportRow, true);
						}

						if( map.getSnomedCountOfBase() != map.getRxCountOfBase() ) reportRow = printQaReason(reportRow, false); else reportRow = printQaReason(reportRow, true);

						if( !map.getSameMdf() ) differentMdf.add(map); 
						if( !map.getSameUnit() ) {
							// 					String up = map.getRxNormSCD().getUnitOfPresentationName();
							//					if( up == null || up.isEmpty() ) addAnnotation(map.getSnomedClass(), this.presUnitDifferent, "translated dose form doesn't include a unit of presentation");
							//					else addAnnotation(map.getSnomedClass(), this.presUnitDifferent, "expected " + up);							
							differentUp.add(map); 
						}
						if( map.getSameBoss().contains(Boolean.FALSE) ) differentBoss.add(map);
						if( map.getActIng().contains(Boolean.FALSE)) differentAI.add(map);
						if( !map.getWrongUnits().isEmpty()) differentUnits.add(map);
						if( !map.getWrongValues().isEmpty()) differentValues.add(map);
						if( map.getSnomedCountOfBase() != map.getRxCountOfBase() ) {
							//					addAnnotation(map.getSnomedClass(), this.countBaseDifferent, "expected count of " + map.getRxCountOfBase() );
							differentCountOfBase.add(map);
						}
						if( map.getRxNormSCD().hasNDC() ) noNDC.add(map);
		
					}
					else {
						reportRow = reportRow.concat("\t\t\t\t\t\t\t");
					}
					if( !map.getRxNormSCD().hasNDC() ) reportRow = printQaReason(reportRow, false); else reportRow = printQaReason(reportRow, true);
					if( !map.getRxNormSCD().getIsPrescribable() ) reportRow = printQaReason(reportRow, false); else reportRow = printQaReason(reportRow, true);
					if( map.getRxNormSCD().getIsVaccine() ) reportRow = printQaReason(reportRow, false); else reportRow = printQaReason(reportRow, true);
					if( map.getRxNormSCD().hasRxNormQualitativeDistinction() ) reportRow = printQaReason(reportRow, false); else reportRow = printQaReason(reportRow, true);					
					
					String snomedTemplate = "";
					if(map.getSnomedPresentationTemplate()) {
						snomedTemplate = "P";
					}
					if(map.getSnomedConcentrationTemplate()) {
						snomedTemplate = snomedTemplate.concat("C");
					}
					
					
					if(!snomedTemplate.equalsIgnoreCase(rxnormTemplate)) reportRow = printQaReason(reportRow, false); else reportRow = printQaReason(reportRow, true);
					
					reportRow = reportRow.concat(map.getSnomedCode() + "\t" + map.getSnomedName() );
				}	
				qaOverview.print(reportRow);

				if(!standaloneMode) {
					multiSheet.print(reportRow);
				}							

			}
			else {
				//do nothing, we already know the SCD exists in the report
			}
		}

		if(!standaloneMode) {
			//		printWriter.println();
			//		printWriter.println();
			//		printWriter.println("*** Different Manufactured Dose Forms ***");
			//		for( EquivalentMapping map : differentMdf ) {
			//			RxNormSCD scd = map.getRxNormSCD();
			//			printWriter.println(scd.getCui() + "\t" + scd.getName() + "\t" + scd.getManufacturedDoseFormName() + "\t" + map.get);
			//		}

			multiSheet.createNewSheet("Manufactured Dose Forms");
			printDifferentDose(differentMdf, "**** Differed Manufactured Dose Forms ****", qaMDF);
			printDifferentDose(differentMdf, "**** Differed Manufactured Dose Forms ****", multiSheet);
			qaMDF.close();


			multiSheet.createNewSheet("Units of Presentation");
			printDifferentUnitsofPresentation(differentUp, "**** Different Unit of Presentation ****", qaUP);
			printDifferentUnitsofPresentation(differentUp, "**** Different Unit of Presentation ****", multiSheet);
			qaUP.close();

			multiSheet.createNewSheet("Different BoSS Names");
			printDifferentBoss(differentBoss, "**** Different BoSS Names ****", qaBoss);
			printDifferentBoss(differentBoss, "**** Different BoSS Names ****", multiSheet);
			qaBoss.close();

			multiSheet.createNewSheet("Known - Active Ingredient");
			printKnownDifferentActiveIngredient(differentAI, "**** Different Active Ingredient in BoSS ****", kqaAi);
			printKnownDifferentActiveIngredient(differentAI, "**** Different Active Ingredient in BoSS ****", multiSheet);
			kqaAi.close();
			
			multiSheet.createNewSheet("Unknown - Active Ingredient");
			printUnknownDifferentActiveIngredient(differentAI, "**** Different Active Ingredient in BoSS ****", uqaAi);
			printUnknownDifferentActiveIngredient(differentAI, "**** Different Active Ingredient in BoSS ****", multiSheet);
			uqaAi.close();			


			multiSheet.createNewSheet("Different Units");
			printDifferentUnits(differentUnits, "**** Different Units in BoSS ****", differentBossUnits);
			printDifferentUnits(differentUnits, "**** Different Units in BoSS ****", multiSheet);
			differentBossUnits.close();

			multiSheet.createNewSheet("Different Values");
			printDifferentValues(differentValues, "**** Different Units in BoSS ****", differentBossValues);
			printDifferentValues(differentValues, "**** Different Units in BoSS ****", multiSheet);
			differentBossValues.close();

			multiSheet.createNewSheet("Different Count of Base");
			printDifferentCountOfBase(differentCountOfBase, "**** Different Count of Base ****", differentCountOfBaseReport);
			printDifferentCountOfBase(differentCountOfBase, "**** Different Count of Base ****", multiSheet);

			multiSheet.createNewSheet("No NDC");
			printDifferentCountOfBase(differentCountOfBase, "**** No NDC ****", noNDCReport);
			printDifferentCountOfBase(differentCountOfBase, "**** No NDC ****", multiSheet);		

			multiSheet.createNewSheet("No Associated Template");		
			printNoTemplate(noSnomedDefinitionTemplate, "**** No Template Available ****", noDefinitionTemplate);
			printNoTemplate(noSnomedDefinitionTemplate, "**** No Template Available ****", multiSheet);
			noDefinitionTemplate.close();

			multiSheet.createNewSheet("Duplicate RxINs");
			duplicateIngredients.printHeader("Rx Ingredient\tRxCUI\tRxIngredient\tRxCUI");
			multiSheet.printHeader("Rx Ingredient\tRxCUI\tRxIngredient\tRxCUI");
			for( OWLClass c : rxToRx.keySet() ) {
				for( OWLClass d : rxToRx.get(c)) {
					duplicateIngredients.print(getRDFSLabelForClass(c) + "\t" + c.getIRI().getFragment() + "\t" + getRDFSLabelForClass(d) + "\t" + d.getIRI().getFragment());
					multiSheet.print(getRDFSLabelForClass(c) + "\t" + c.getIRI().getFragment() + "\t" + getRDFSLabelForClass(d) + "\t" + d.getIRI().getFragment());
				}
			}
			duplicateIngredients.close();		

			multiSheet.createNewSheet("Unmapped SCDs");
			multiSheet.printHeader("Rx SCD CUI\tRx SCD Name");
			unmappedScds.printHeader("Rx SCD CUI\tRx SCD Name");
			for( RxNormSCD scd : wontMapSCD ) {
				unmappedScds.print(scd.getCui() + "\t" + scd.getName());
				multiSheet.print(scd.getCui() + "\t" + scd.getName());
			}
			unmappedScds.close();

			multiSheet.createNewSheet("False Positives");
			falsePositivesReport.printHeader("CUI 1\tCUI 2");
			multiSheet.printHeader("CUI 1\tCUI 2");		
			for( String i : falsePositives.keySet() ) {
				Set<String> ids = falsePositives.get(i);
				for( String j : ids ) {
					falsePositivesReport.print("Rx" + i + "\t" + "Rx" + j);
					multiSheet.print("Rx" + i + "\t" + "Rx" + j);				
				}
			}
			falsePositivesReport.close();

			multiSheet.createNewSheet("Equivalent Ingredients");
			multiSheet.printHeader("IN Code\tIN name\tIN Code\tIN name");
			equivalentIngredients.printHeader("RxIN CUI\tRxIN name\tRxIN CUI\tRxIN name");
			for( OWLClass cls : this.ingEquivalencyMap.keySet() ) {
				ArrayList<OWLClass> list = this.ingEquivalencyMap.get(cls);
				for( OWLClass c : list ) {
					if( !(cls.getIRI().getIRIString().contains("Rx") && c.getIRI().getIRIString().contains("Rx")) && !( !cls.getIRI().getIRIString().contains("Rx") && !c.getIRI().getIRIString().contains("Rx")) )  {
						equivalentIngredients.print(cls.getIRI().getIRIString().replace(namespace, "") + "\t" + getRDFSLabel(cls) + "\t" + c.getIRI().getIRIString().replace(namespace, "") + "\t" + getRDFSLabel(c) );
						multiSheet.print(cls.getIRI().getIRIString().replace(namespace, "") + "\t" + getRDFSLabel(cls) + "\t" + c.getIRI().getIRIString().replace(namespace, "") + "\t" + getRDFSLabel(c) );
					}
				}
			}
			equivalentIngredients.close();
		}

		qaOverview.close();
		multiSheet.close();

	}

	public void addAnnotation(OWLClass c, OWLAnnotationProperty a, String b) {
		if(c == null ) return;
		OWLAnnotation anno = factory.getOWLAnnotation(a, factory.getOWLLiteral(b));
		OWLAxiom annoToClass = factory.getOWLAnnotationAssertionAxiom(c.getIRI(), anno);
		man.addAxiom(ontology, annoToClass);		
	}	

	public void addAnnotation(OWLClass c, OWLAnnotationProperty a, boolean b) {
		if(c == null ) return;
		OWLAnnotation anno = factory.getOWLAnnotation(a, factory.getOWLLiteral(String.valueOf(b)));
		OWLAxiom annoToClass = factory.getOWLAnnotationAssertionAxiom(c.getIRI(), anno);
		man.addAxiom(ontology, annoToClass);		
	}		

	private void printDifferentDose(ArrayList<EquivalentMapping> mappings, String header, ExcelReport excel) {

		excel.printHeader("Rx SCD CUI\tSCD Name\tRxNorm Dose Form\tExpected Manufactured Dose Form\tSCT Dose Form\tSCT CD Name\tSCT CD Code");
		for( EquivalentMapping map : mappings ) {
			HashMap<OWLObjectProperty, ArrayList<OWLClass>> obMap = map.getExplanationMap();
			for( OWLObjectProperty op : obMap.keySet() ) {
				if( op.equals(this.hasManufacturedDoseForm)) {
					ArrayList<OWLClass> classes = obMap.get(op);
					for( OWLClass c : classes ) {
						excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" + map.getRxNormSCD().getRxNormDoseForm().get(0).getName() + "\t" + map.getRxNormSCD().getManufacturedDoseFormName() + "\t" + getRDFSLabelForClass(c) + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode());
					}
				}
			}
		}
	}

	private void printDifferentUnitsofPresentation(ArrayList<EquivalentMapping> mappings, String header, ExcelReport excel) {
		excel.printHeader("Rx SCD CUI\tSCD Name\tRxNorm Dose Form\tExpected Unit of Presentation\tSCT Unit of Presentation\tSCT CD Code");		
		for( EquivalentMapping map : mappings ) {
			HashMap<OWLObjectProperty, ArrayList<OWLClass>> obMap = map.getExplanationMap();
			for( OWLObjectProperty op : obMap.keySet() ) {
				if( op.equals(this.hasUnitOfPresentation)) {
					ArrayList<OWLClass> classes = obMap.get(op);
					for( OWLClass c : classes ) {
						excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" + map.getRxNormSCD().getRxNormDoseForm().get(0).getName() + "\t" + map.getRxNormSCD().getUnitOfPresentationName() + "\t" + getRDFSLabelForClass(c) + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode() );
					}
				}
			}
		}
	}	

	private void printDifferentBoss(ArrayList<EquivalentMapping> mappings, String header, ExcelReport excel) {
		excel.printHeader("Rx SCD CUI\tSCD Name\tPossible Issue\tRxNorm BoSS Name\tSCT BoSS Name\tSCT CD Name\tSCT CD Code");			
		for( EquivalentMapping map : mappings ) {
			HashMap<OWLObjectProperty, ArrayList<OWLClass>> obMap = map.getExplanationMap();
			for( OWLObjectProperty op : obMap.keySet() ) {
				if( op.equals(this.hasBasisOfStrengthSubstance) && !map.getRxNormSCD().hasUnknownBoss()) {
					ArrayList<OWLClass> snomedClasses = obMap.get(op);
					ArrayList<ClassPair> pairs = reportCrossProductBoss(map.getRxNormSCD().getRxNormBoss(), snomedClasses);
					for(ClassPair pair : pairs) {
						excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" + "different" + "\t" + getRDFSLabelForClass(pair.getRxClass()) + "\t" + getRDFSLabelForClass(pair.getSnomedClass()) + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode());
					}
//					for( OWLClass c : classes ) {
//						for(RxNormBoss boss :  map.getRxNormSCD().getRxNormBoss() ) {
//							String rxBossName = boss.getBossName();
//							if(boss.getBossName() == null) rxBossName = "not specified in rxnorm";
//							if(!c.getIRI().getIRIString().contains("Rx")) excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" + rxBossName +  "\t" + getRDFSLabelForClass(c) +  "\t" + map.getSnomedName() + "\t" + map.getSnomedCode());
//						}
//					}
				}
			}
		}
	}

	private void printKnownDifferentActiveIngredient(ArrayList<EquivalentMapping> mappings, String header, ExcelReport excel) {
		excel.printHeader("Rx SCD CUI\tSCD Name\tPossible Issue\tRxNorm AI\tSCT AI\tSCT Modification of\tSCT CD Name\tSCT CD Code");
		for( EquivalentMapping map : mappings ) {
			HashMap<OWLObjectProperty, ArrayList<OWLClass>> obMap = map.getExplanationMap();
			for( OWLObjectProperty op : obMap.keySet() ) {
				if( op.equals(this.hasPreciseActiveIngredient) && !map.getRxNormSCD().hasUnknownActiveIngredient()) {
					ArrayList<OWLClass> snomedClasses = obMap.get(op);
					ArrayList<ClassPair> pairs = reportCrossProductAI(map.getRxNormSCD().getRxNormBoss(), snomedClasses);
					for(ClassPair pair : pairs) {
						excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" + "different" + "\t" + getRDFSLabelForClass(pair.getRxClass()) + "\t" + getRDFSLabelForClass(pair.getSnomedClass()) + "\t" + getModificationOf(pair.getSnomedClass()) + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode());
					}
//					for( OWLClass c : snomedClasses ) {
//						for(RxNormBoss boss :  map.getRxNormSCD().getRxNormBoss() ) {
//							String ing = boss.getActiveIngredientName();
//							if(ing == null || ing.equals("null") ) ing = "";
//							if(!c.getIRI().getIRIString().contains("Rx")) excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" + getPossibleIssueRxBossData(boss.getActiveIngredientRxCui(), c) + "\t" + ing + "\t" + getRDFSLabelForClass(c) + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode() );
//						}
//					}

				}
			}			
		}
	}
	
	private void printUnknownDifferentActiveIngredient(ArrayList<EquivalentMapping> mappings, String header, ExcelReport excel) {
		excel.printHeader("Rx SCD CUI\tSCD Name\tPossible Issue\tRxNorm AI\tSCT AI\tSCT CD Name\tSCT CD Code");
		for( EquivalentMapping map : mappings ) {
			HashMap<OWLObjectProperty, ArrayList<OWLClass>> obMap = map.getExplanationMap();
			for( OWLObjectProperty op : obMap.keySet() ) {
				if( op.equals(this.hasPreciseActiveIngredient) && map.getRxNormSCD().hasUnknownActiveIngredient()) {
					ArrayList<OWLClass> snomedClasses = obMap.get(op);															
						for( OWLClass c : snomedClasses ) {
							for(RxNormBoss boss :  map.getRxNormSCD().getRxNormBoss() ) {
								String ing = boss.getActiveIngredientName();
								if(ing == null || ing.equals("null") ) ing = "";
								if(!c.getIRI().getIRIString().contains("Rx")) excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" + getPossibleIssueRxBossData(boss.getActiveIngredientRxCui(), c) + "\t" + ing + "\t" + getRDFSLabelForClass(c) + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode() );
							}
						}

				}
			}			
		}
	}
	
	//suggested by @ykazakov, https://github.com/owlcs/owlapi/issues/1036
	@SuppressWarnings("deprecation")
	private String getModificationOf(OWLClass snomedSubstance) {
		String result = ""; // the list of all fillers for the property
		for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(snomedSubstance)) {
			OWLClassExpression sup = ax.getSuperClass();
			if (sup instanceof OWLObjectIntersectionOf) {
				OWLObjectIntersectionOf inters = (OWLObjectIntersectionOf) sup; 
				for (OWLClassExpression conj : inters.getOperands()) {
					if (conj instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom exist = (OWLObjectSomeValuesFrom) conj;
						if (exist.getProperty().equals(isModificationOf)) {
							System.out.println("**\t" + getRDFSLabel(snomedSubstance) + " is modification of -> " + getRDFSLabel(exist.getFiller().asOWLClass()));
							//result.add(getRDFSLabel(exist.getFiller().asOWLClass()));
							if(result.equals("")) {
								result = getRDFSLabel(exist.getFiller().asOWLClass());
							} else {
								result = result + "|" + getRDFSLabel(exist.getFiller().asOWLClass());
							}
						}
					}
				}
			}
		}				
		
		return result;
	}
	
	private ArrayList<ClassPair> reportCrossProductAI(Vector<RxNormBoss> bosses, ArrayList<OWLClass> snomedClasses) {
		ArrayList<ClassPair> pairs = new ArrayList<ClassPair>();
		
		for(int i=0; i < bosses.size(); i++) {
			RxNormBoss boss = bosses.get(i);
			OWLClass rxClass = factory.getOWLClass(namespace, "Rx" + boss.getActiveIngredientRxCui().toString());
			for(int j=0; j < snomedClasses.size(); j++) {
				OWLClass snctClass = snomedClasses.get(j);
				ClassPair pair = new ClassPair(rxClass, snctClass);
				pairs.add(pair);
			}
		}
		
		return reducePairs(pairs);
		
	}
	
	private ArrayList<ClassPair> reportCrossProductBoss(Vector<RxNormBoss> bosses, ArrayList<OWLClass> snomedClasses) {
		ArrayList<ClassPair> pairs = new ArrayList<ClassPair>();
		
		for(int i=0; i < bosses.size(); i++) {
			RxNormBoss boss = bosses.get(i);
			OWLClass rxClass = factory.getOWLClass(namespace, "Rx" + boss.getBossRxCui().toString());
			for(int j=0; j < snomedClasses.size(); j++) {
				OWLClass snctClass = snomedClasses.get(j);
				ClassPair pair = new ClassPair(rxClass, snctClass);
				pairs.add(pair);
			}
		}
		
		return reducePairs(pairs);
		
	}	
	
	private ArrayList<ClassPair> reducePairs(ArrayList<ClassPair> pairs) {
		//We want to remove any pairs from the QA report containing a substance that was found equivalent.
		//For example, if an SCD has multiple bosses and ibuprofen in rxnorm is equivalent to the ibuprofen in snomed,
		//we should not report ibuprofen in rxnorm and pseudophedrine in snomed as an issue because we already know
		//both ibuprofens are accounted for
		
		ArrayList<ClassPair> keptPairs = new ArrayList<ClassPair>();
		
		ArrayList<OWLClass> squelch = new ArrayList<OWLClass>();

		for(int i=0; i < pairs.size(); i++) {
			ClassPair pair = pairs.get(i);
			if(classesAreEquivalent(pair.getRxClass(), pair.getSnomedClass())) {
				pair.setEquivalent(true); //the classes aren't equivalent according to the reasoner
				if(!squelch.contains(pair.getRxClass())) squelch.add(pair.getRxClass());
				if(!squelch.contains(pair.getSnomedClass())) squelch.add(pair.getSnomedClass());
			}
		}
		
		//reduce
		for(ClassPair pair : pairs) {
			boolean keep = true;
			if(squelch.contains(pair.getRxClass())) keep = false;
			if(squelch.contains(pair.getSnomedClass())) keep = false;
			if(keep) keptPairs.add(pair);
		}
		
		return keptPairs;
	}
	
	private String getPossibleIssueRxBossData(Integer rxcui, OWLClass snomedSubstanceClass) {
		String possibleIssue = "";
		Integer nonExistentCui = Integer.valueOf(-1);

		//1: Does the rxcui exist? (i.e., not set to my "null" cui, -1)
		if(rxcui.equals(nonExistentCui)) {
			possibleIssue = "missing substance";
		}
		else {
		//2: Is the substance mapped?
			boolean mapped = false;
			OWLClass rxSubstanceClass = factory.getOWLClass(namespace, "Rx" + rxcui.toString() );
			if(classesAreEquivalent(rxSubstanceClass, snomedSubstanceClass)) mapped = true;
			if(!mapped) possibleIssue = "inspect mapping";
			else {
		//3: Is the substance different?
				String snomedName = this.getRDFSLabel(snomedSubstanceClass).replace(" (substance)", "").toLowerCase();
				String rxName = this.getRDFSLabel(rxSubstanceClass).replace(" (substance)", "").toLowerCase();
				if(!snomedName.equals(rxName)) {
					possibleIssue = "different";
				}
			}
		}
		
		return possibleIssue;
	}
	
	private boolean classesAreEquivalent(OWLClass rxClass, OWLClass snomedClass) {
		for(OWLClass eqClass : reasoner.getEquivalentClasses(snomedClass).entities().collect(Collectors.toSet())) {
			if(eqClass.equals(rxClass)) return true;
		}
		return false;
	}

	private void printDifferentUnits(ArrayList<EquivalentMapping> mappings, String header, ExcelReport excel) {
		excel.printHeader("Rx SCD CUI\tSCD Name\tRx Unit Type\tRxNorm Unit in BoSS\tSCT Unit in BoSS\tSCT CD Name\tSCT CD Code");
		for( EquivalentMapping map : mappings ) {
			HashMap<OWLObjectProperty, ArrayList<OWLClass>> obMap = map.getExplanationMap();
			for( OWLObjectProperty op : obMap.keySet() ) {
				String snomedObjectProperty = getObjectName(op);				
				for( OWLObjectProperty unitCheck : qaUnitChecks ) {
					if( op.equals(unitCheck) ) {
						for( OWLClass c : obMap.get(op) ) {
							for(RxNormBoss boss :  map.getRxNormSCD().getRxNormBoss() ) {
								boolean reportPresentation = false;
								boolean reportNumerator = false;
								String rxUnit = null;
								if( snomedObjectProperty.contains("presentation") ) reportPresentation = true;
								if( snomedObjectProperty.contains("numerator") ) reportNumerator = true;
								if( reportNumerator ) rxUnit = boss.getSnomedNumeratorUnit();
								else if( !reportNumerator ) rxUnit = boss.getDenominatorUnit();

								String type = "";
								if( reportPresentation && reportNumerator ) type = "Presentation Numerator Unit";
								if( reportPresentation && !reportNumerator ) type = "Presentation Denominator Unit";
								if( !reportPresentation && reportNumerator ) type = "Concentration Numerator Unit";								
								if( !reportPresentation && !reportNumerator ) type = "Concentration Denominator Unit";

								if(!c.getIRI().getIRIString().contains("Rx")) excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" +  type + "\t" + rxUnit + "\t" + getRDFSLabelForClass(c) + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode() );
							}
						}
					}
				}
			}			
		}				
	}

	private void printNoTemplate(Set<RxNormSCD> scdsNoTemplate, String header, ExcelReport excel) {
		excel.printHeader("Rx SCD CUI\tSCD Name\tSCD Dose Form");
		for( RxNormSCD scd : scdsNoTemplate ) {
			excel.print(scd.getCui() + "\t" + scd.getName() + "\t" + scd.getRxNormDoseForm().get(0).getName());
		}
	}

	private void printDifferentValues(ArrayList<EquivalentMapping> mappings, String header, ExcelReport excel) {
		excel.printHeader("Rx SCD CUI\tSCD Name\tRx Value Type\tRxNorm Value in BoSS\tSCT Value in BoSS\tSCT CD Name\tSCT CD Code");
		for( EquivalentMapping map : mappings ) {
			HashMap<OWLObjectProperty, ArrayList<OWLClass>> obMap = map.getExplanationMap();
			for( OWLObjectProperty op : obMap.keySet()) {
				String snomedObjectProperty = getObjectName(op);
				for( OWLObjectProperty valueCheck : qaValueChecks ) {
					if( op.equals(valueCheck) ) {
						for( OWLClass c : obMap.get(op) ) {
							for( RxNormBoss boss : map.getRxNormSCD().getRxNormBoss() ) {
								boolean reportPresentation = false;
								boolean reportNumerator = false;
								String rxValue = null;
								if( snomedObjectProperty.contains("presentation") ) reportPresentation = true;
								if( snomedObjectProperty.contains("numerator") ) reportNumerator = true;
								if( reportNumerator ) rxValue = boss.getSnomedNumberatorValue();
								else if( !reportNumerator) rxValue = boss.getSnomedDenominatorValue();

								String type = "";
								if( reportPresentation && reportNumerator ) type = "Presentation Numerator Value";
								if( reportPresentation && !reportNumerator ) type = "Presentation Denominator Value";
								if( !reportPresentation && reportNumerator ) type = "Concentration Numerator Value";								
								if( !reportPresentation && !reportNumerator ) type = "Concentration Denominator Value";								

								if(!c.getIRI().getIRIString().contains("Rx")) excel.print(map.getRxCui() + "\t" +map.getRxName() + "\t" + type + "\t" + rxValue + "\t" + getRDFSLabelForClass(c) + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode() );
							}
						}
					}
				}
			}
		}
	}

	private void printDifferentCountOfBase(ArrayList<EquivalentMapping> mappings, String header, ExcelReport excel) {
		excel.printHeader("Rx SCD CUI\tSCD Name\tRx Count of Base\tSCT Count of Base\tSCT Name\tSCT Code");
		for( EquivalentMapping map : mappings ) {
			if( map.getSnomedCountOfBase() != map.getRxCountOfBase() ) {
				excel.print(map.getRxCui() + "\t" + map.getRxName() + "\t" + map.getRxCountOfBase() + "\t" + map.getSnomedCountOfBase() + "\t" + map.getSnomedName() + "\t" + map.getSnomedCode() );
			}
		}
	}

	private String printQaReason(String row, boolean b) {
		String reason = "";
		if( !b ) {
			reason = "x";
		}
		row = row.concat(reason + "\t");
		return row;
	}

	private String printQaReasonString(String row, String s) {
		String reason = s;
		row = row.concat(reason + "\t");
		return row;
	}

	public OWLClass getOWLClassForString(String value, TreeMap<OWLClass, ArrayList<OWLClass>> map) {
		OWLClass clz = null;

		for( OWLClass c : map.keySet() ) {
			for (OWLAnnotation a : EntitySearcher.getAnnotations(c, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				OWLAnnotationValue val = a.getValue();
				if (val instanceof OWLLiteral) {
					if(((OWLLiteral) val).getLiteral().toString().equals(value)) {
						clz = c;
						return clz;
					}
				}
			}			
		}

		return clz;		

	}

	public File saveOntology() {
		File file = null;
		String fileString = null;
		try {
			fileString = "./RxNorm2Snomed_" + globalDate + ".owl";
			file = new File (fileString);
			man.saveOntology(ontology, IRI.create(file));
			this.finalFileName = fileString;
			//			System.out.println("Saving file as " + outputFilename);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}

		return file;
	}

	private void log(String s) {
		if(!standaloneMode) {
			pw.println(s);
			pw.flush();
		}
	}

	public void runAudit() {
		String[] args = new String[1];
		args[0] = finalFileName;
		Audit.main(args);
	}

}
