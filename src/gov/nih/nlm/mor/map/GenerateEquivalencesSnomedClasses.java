package gov.nih.nlm.mor.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLEntityRemover;

import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import gov.nih.nlm.mor.RxNorm.RxNormBoss;
import gov.nih.nlm.mor.RxNorm.RxNormDoseForm;
import gov.nih.nlm.mor.RxNorm.RxNormIngredient;
import gov.nih.nlm.mor.RxNorm.RxNormSBD;
import gov.nih.nlm.mor.RxNorm.RxNormSCD;
import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.Snomed.SnomedDefinitionTemplate;
import gov.nih.nlm.mor.Snomed.SnomedManufacturedDoseForm;
import gov.nih.nlm.mor.Snomed.SnomedUnitOfPresentation;
import gov.nih.nlm.mor.util.Constants;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenerateEquivalencesSnomedClasses implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3324801368865331845L;
	Constants Consts = new Constants();
	OWLOntologyManager man = null;
	OWLOntology ontology = null;
	OWLReasoner reasoner = null;
	OWLReasonerFactory reasonerFactory = null;
	OWLDataFactory factory = null;	
	int codeGenerator;
	double snomedCodeGenerator;
	DecimalFormat df = new DecimalFormat("#");	
	final String namespace = new String("http://snomed.info/id/"); //TODO: make this configurable	
	String outputFilename = null;
	Set<OWLClass> classesInOntology = null;
	Set<OWLClass> snomedMPClasses = null;
	public OWLClass medicinalProduct = null;
	public OWLClass numberClass = null;
	public OWLClass rxNumberRoot = null;
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
	public OWLAnnotationProperty mapsToCode = null;
	public OWLAnnotationProperty mapsToName = null;
	public OWLAnnotationProperty inferred = null;
	public OWLAnnotationProperty asserted = null;
	public OWLAnnotationProperty hasNDC = null;
	public OWLAnnotationProperty hasRxCUI = null;
	public OWLAnnotationProperty substanceNoExist = null;
	public OWLAnnotationProperty substanceDifferent = null;
	public OWLAnnotationProperty doseFormDifferent = null;
	public OWLAnnotationProperty unitsDifferent = null;
	public OWLAnnotationProperty valuesDifferent = null;
	public OWLAnnotationProperty bossSubstanceDifferent = null;
	public OWLAnnotationProperty activeIngerdientSubstanceDifferent = null;
	public OWLAnnotationProperty presUnitDifferent = null;	
	public OWLAnnotationProperty countBaseDifferent = null;
	public OWLAnnotationProperty hasExplanation = null;
	public OWLAnnotationProperty vetOnly = null;
	public OWLAnnotationProperty allergenic = null;
	public OWLAnnotationProperty hasAllergenic = null;
	public OWLAnnotationProperty isPrescribable = null;
	public TreeMap<String, Double> unitsOfMeasure = new TreeMap<String, Double>();
	public HashMap<String, OWLClass> snomedNumberQualifier = new HashMap<String, OWLClass>();
//	public Set<Double> numbers = new HashSet<>();
	public Set<String> numbers = new HashSet<>();	
	public Vector<String> vRxUnits = new Vector<String>();
	public Set<OWLClass> vSnomedLiquidDoseForms = new HashSet<OWLClass>();
	public Set<RxNormSCD> noSnomedDefinitionTemplate = new HashSet<RxNormSCD>();
	public HashMap<OWLClass, OWLClass> unitOfMassMap = new HashMap<OWLClass, OWLClass>();
//	public HashMap<Double, OWLClass> numberClassToName = new HashMap<Double, OWLClass>();
	public HashMap<OWLClass, String> unitClassToName = new HashMap<OWLClass, String>();
	public HashMap<String, OWLClass> unitOfPresentationToClass = new HashMap<String, OWLClass>();
	public TreeMap<Integer, SnomedDFPair> rxdf2SnomedDFPair = new TreeMap<Integer, SnomedDFPair>();	
	public TreeMap<OWLClass, ArrayList<OWLClass>> cdEquivalencyMapSnomed = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	public TreeMap<OWLClass, ArrayList<OWLClass>> scdEquivalencyMapRxNorm = new TreeMap<OWLClass, ArrayList<OWLClass>>();
//	public Set<OWLClass> allSBDs = new HashSet<>();
	public TreeMap<OWLClass, ArrayList<OWLClass>> ingEquivalencyMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	public TreeMap<OWLClass, ArrayList<OWLClass>> unitEquivalencyMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	public TreeMap<OWLClass, ArrayList<OWLClass>> numberEquivalencyMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	
	//structures from JNs work on substance mappings
	public TreeMap<String, ArrayList<String>> jnMissingSubstanceMappings = new TreeMap<String, ArrayList<String>>();
	public TreeMap<String, ArrayList<String>> jnWrongSubstanceMappings = new TreeMap<String, ArrayList<String>>();
	
//	public Vector<String> seen = new Vector<String>();
	public Vector<OWLObjectProperty> qaChecks = new Vector<OWLObjectProperty>();
	public Vector<OWLObjectProperty> qaUnitChecks = new Vector<OWLObjectProperty>();
	public Vector<OWLObjectProperty> qaValueChecks = new Vector<OWLObjectProperty>();
	public Set<OWLClass> assertedClasses = new HashSet<>();
	public Set<OWLClass> inferredClasses = new HashSet<>();
	private boolean debug = false;
	private PrintWriter pw = null;
	private PrintWriter ingPw = null;
	Set<EquivalentMapping> mappings = new HashSet<>();
	private Double seed = Math.random();
	Analyzer analyzer = null;
	RestUrl restUrl = new RestUrl();

	public String dir = System.getProperty("user.dir").replace("\\", "/").replace(" ", "%20");	
	public enum OSType {
		Windows, MacOS, Linux, Other
	};
	public OSType detectedOS = null;
	OSType os = getOperatingSystemType();
	private OWLObjectProperty isModificationOf;
	private OWLAnnotationProperty isVaccine;
	private ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	
	
	@SuppressWarnings("deprecation")
	public GenerateEquivalencesSnomedClasses(String filename, FetchRxNormData fetchRxNorm, boolean addGcis, boolean debug) {
		
		this.debug = debug;
		//If the logString ever needed to be built, doesn't
		//make sense to have it here.
		//String logString = null;
		
		if( debug ) {
			Double prod = this.seed * 1000;
			File f = new File("./log_" + String.valueOf(prod) + ".txt");
			try {
				pw = new PrintWriter(f);
			} catch (FileNotFoundException e) {
				// carry on
				e.printStackTrace();
			}
		}
		
		try {
			ingPw = new PrintWriter(new File("substance-map.txt"));
		} catch(Exception e) {
			e.printStackTrace();
		}
	
		rxdf2SnomedDFPair = fetchRxNorm.getDFPairMap();

		try {
			man = OWLManager.createOWLOntologyManager();			
			ontology = man.loadOntologyFromOntologyDocument(new File(filename));
		} catch (OWLOntologyCreationException e1 ) {
			System.out.println("Not sure what could be happening here all of a sudden.");
			e1.printStackTrace();
		}
		
		jnMissingSubstanceMappings = mapFromFile("./config/missing-substance-mappings.txt");
		jnWrongSubstanceMappings = mapFromFile("./config/wrong-substance-mappings.txt");		
		
		log("*** Discovering inferences for ontology " + filename + " ***");

		reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ontology);
		factory = man.getOWLDataFactory();
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);		

		//		if( !reasoner.isConsistent() ) {
		//			System.out.println("Ontology inconsistent. The program will now exit.");
		//			System.exit(0);
		//		}			

//		classesInOntology = ontology.classesInSignature().collect((Collectors.toSet()));
		log("Finished loading ont...");
		log("Creating reasoner...");

		outputFilename = generateOutputFilename(filename);

		//generate dose form map - comment out if not needed or..
		//TODO: Configure if a dose form pair mapping should be ouput

		FetchSnomedDoseForms snomedDoseForms = new FetchSnomedDoseForms(man, ontology, factory, reasoner, fetchRxNorm);
		//snomedDoseForms.examineForPairs(); //this is already done in the constructor.
		
		log("STEP 0");
		if(addGcis) {
			addGcis();
		}

		log("STEP 1");		
		
		setConstants();
		createMapsToAnnotations();		
		
		codeGenerator = 0;
		snomedCodeGenerator = 1000000000;

	
		OWLClass snomedMpParent = factory.getOWLClass(namespace, "763158003");
		OWLClass snomedSubstance = factory.getOWLClass(namespace, "105590001");
		OWLClass snomedUnit = factory.getOWLClass(namespace, "258681007");
		OWLClass snomedNumber = factory.getOWLClass(namespace, "260299005");
		
		OWLClass fakeCdParent = factory.getOWLClass(namespace, df.format(++snomedCodeGenerator) + "-FS");
		OWLClass fakeCdTranslatedParent = factory.getOWLClass(namespace, df.format(++snomedCodeGenerator) + "-FS");
//		OWLClass fakeSBDParent = factory.getOWLClass(namespace, df.format(++snomedCodeGenerator) + "-FS");
		OWLClass fakeIngredientParent = factory.getOWLClass(namespace, df.format(++snomedCodeGenerator) + "-FS");
		OWLClass fakeNumberParent = factory.getOWLClass(namespace, df.format(++snomedCodeGenerator) + "-FS");
		
		addFakeParent(fakeCdTranslatedParent, snomedMpParent, "Translated CDs");
		addFakeParent(fakeCdParent, snomedMpParent, "Untranslated CDs");
		addFakeParent(fakeIngredientParent, snomedSubstance, "Untranslated Substances");
		addFakeParent(fakeNumberParent, snomedNumber, "Untranslated Numbers");
//		addFakeParent(fakeSBDParent, snomedMpParent, "RXNORM SBDs (Brand Drugs)");
		
		
	
		// Create the basic concepts we already know about in RxNorm,
		// categorized under the roots created above.
		for(Integer cui : fetchRxNorm.getINMap().keySet() ) {
			RxNormIngredient ingredient = fetchRxNorm.getINMap().get(cui);
			createIngredientChild(cui, ingredient, snomedSubstance, fakeIngredientParent);
		}

		for( Integer cui : fetchRxNorm.getPINMap().keySet() ) {
			RxNormIngredient ingredient = fetchRxNorm.getPINMap().get(cui);
//			if(ingredient.getRxcui() == 1040027 ) {
//				System.out.println("b");
//			}
			createIngredientChild(cui, ingredient, snomedSubstance, fakeIngredientParent);
		}
		
		//add missing substance mappings
//		addMissingSubstanceMappings();
		
		createIngredientChild(Integer.valueOf(0), new RxNormIngredient(0, "not specified in rxnorm"), snomedSubstance, fakeIngredientParent);		
		
		reasoner.flush();
		
		log("STEP 2");		
		
		//TODO: Modify method to use codes - won't be simple for RxDF if codes
		//are generated on the fly.  Possibly add a code to the config file for what
		//the RxCode should be from RxDF
//		rxdf2SnomedDFPair = fetchRxNorm.getDFPairMap("./RxNorm2SnomedDFMapping.txt");
		
		for( Integer cui : fetchRxNorm.getSCDMap().keySet() ) {
			RxNormSCD rxSCD = fetchRxNorm.getSCDMap().get(cui);
			
			//Set the manufacturedDoseForm and unitOfPresentation to the snomed classes, becuase
			//we want to hardwire these to the definition.  We tried doing this based on pair popularity
			//from the RxNorm DF to Snomed pairs, however the issue is that multiple pairs could map to a
			//single RxDF.  This would necessitate building classes with an anding of the snomed df and up,
			//though, this would break the equivalencies the reasoner is supposed to make
			getMDFAndUP(rxSCD);
			
			createSCDChild(cui, rxSCD, snomedMpParent);
			
			for(RxNormBoss boss : rxSCD.getRxNormBoss() ) {
				try {
				if( !unitsOfMeasure.containsKey(boss.getSnomedDenominatorUnit() ) ) {
					unitsOfMeasure.put(boss.getSnomedDenominatorUnit(), ++snomedCodeGenerator);
				}
				} catch(Exception e) {
					//This is more than likely a product.  And we don't care about products.  So, we aren't going to let it through.
					log("RxCUI " + rxSCD.getCui() + "(" + rxSCD.getName() + ") is more than likely a liquid product. Continuing without a definition.");
					rxSCD.setSnomedDefinitionTemplate(null);
				}
				
				try {
				if( !unitsOfMeasure.containsKey(boss.getSnomedNumeratorUnit() )) {
					unitsOfMeasure.put(boss.getSnomedNumeratorUnit(), ++snomedCodeGenerator);					
				}
				} catch(Exception e) {
					//I haven't seen any cases like this so far, though just to be safe..
					log("RxCUI " + rxSCD.getName() + "(" + rxSCD.getCui() + ") is a dose form where unit values could not be translated. Continuing without a definition.");
					rxSCD.setSnomedDefinitionTemplate(null);				
				}
			
				//some of the best methods are deprecated
				if( !numbers.contains(boss.getSnomedDenominatorValue()) && NumberUtils.isNumber(boss.getSnomedDenominatorValue()) ) {
					numbers.add(boss.getSnomedDenominatorValue());					
				}
			
				if( !numbers.contains(boss.getSnomedNumberatorValue()) && NumberUtils.isNumber(boss.getSnomedNumberatorValue()) ) {
					numbers.add(boss.getSnomedNumberatorValue());
				}
			}

		}	
		
		unitsOfMeasure.remove("1");
		
		//Add Unit Equivalencies
		log("*** Adding predetermined units class equivalencies ****");
		Set<OWLAxiom> unitsOfMeasureAxioms = generateUnitsOfMeasureAxioms(unitsOfMeasure, snomedUnit);
		for(OWLAxiom axiom : unitsOfMeasureAxioms) {
//			System.out.println("Units");
			man.addAxiom(ontology, axiom);
		}
		
		reasoner.flush();
		
		//Add Number Equivalencies
		log("*** Adding number class equivalencies ***");
		Set<OWLClass> snomedNumberClasses = reasoner.getSubClasses(numberClass, false).entities().collect(Collectors.toSet());
		snomedNumberClasses.remove(factory.getOWLNothing());
		
		//this will go away when an EL reasoner works with owlapi 5
		Set<OWLAxiom> sameNumbers = equateSameNumbers(snomedNumberClasses);
		man.addAxioms(ontology, sameNumbers.stream());
		
		reasoner.flush();
		
		Set<OWLAxiom> rxNumberAxioms = generateNumberAxioms(numbers, snomedNumberClasses, snomedNumber);
		man.addAxioms(ontology, rxNumberAxioms.stream());
		
		reasoner.flush();
		
		Set<OWLClass> numberBranchClasses = reasoner.getSubClasses(snomedNumber, false).entities().collect(Collectors.toSet());
		numberBranchClasses.remove(factory.getOWLNothing());
		numberBranchClasses.remove(fakeNumberParent);
		
		log("*** Treeing \"Fake\" numbers not found in SCT ***");
		treeFakes(snomedNumber, fakeNumberParent, numberBranchClasses);	
		
		reasoner.flush();
		
		log("STEP 3");
		
		for( OWLClass cls : reasoner.getSubClasses(snomedSubstance, false).entities().collect((Collectors.toSet())) ) {
//			System.out.println(cls.getIRI());		
			reasoner.getEquivalentClasses(cls).forEach(c -> {
				if( !c.getIRI().getIRIString().replace(namespace, "").equals(cls.getIRI().getIRIString().replace(namespace, "")) ) {
					addToMap(cls, c, ingEquivalencyMap);
				}
			});
		}
		
		for( OWLClass cls : reasoner.getSubClasses(snomedUnit, false).entities().collect((Collectors.toSet())) ) {
			reasoner.getEquivalentClasses(cls).forEach(c -> {
				if(!getCodeForClass(c).equals(getCodeForClass(cls) )) {
					addToMap(c, cls, unitEquivalencyMap);
				}
			});
		}		
		
		log("STEP 4");

		System.out.println("*** Generating RxNorm clinical drug classes with SCT classes where available ****");
		log("*** Generating RxNorm clinical drug classes with SCT classes where available ****");
		Set<OWLAxiom> rxClinicalDrugAxioms = generateClinicalDrugAxioms(fetchRxNorm.getSCDMap());
		man.addAxioms(ontology, rxClinicalDrugAxioms.stream());
		
		reasoner.flush();
		
		//add annotations on SCDs
		reasoner.getSubClasses(this.medicinalProduct, false).entities().filter(x -> !isRx(x) && !x.getIRI().equals(medicinalProduct.getIRI()) && !x.equals(factory.getOWLNothing()))
			.forEach(y -> {
				reasoner.getEquivalentClasses(y).entities().filter(z -> isRx(z) && !z.getIRI().equals(y.getIRI())).forEach(a -> {
					addMapsAnnotation(y, a);
					addAnnotation(y, this.inferred, true);
					//inferredClasses.add(y);
					addToMap(a, y, cdEquivalencyMapSnomed);					
				});
			});
		
		log("STEP 5");
	
		log("*** Reparenting Untranslated CDs ****");
//		treeCdFakes(medicinalProduct, fakeCdParent, fakeCdTranslatedParent, cdEquivalencyMapSnomed);
		treeCdFakes(medicinalProduct, fakeCdParent, fakeCdTranslatedParent);				
		
		log("STEP 6");
		
		OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(this.ontology));
		
		log("*** Annotating SCT substances as asserted where applicable ***");
		Set<OWLClass> substanceBranchClasses = reasoner.getSubClasses(snomedSubstance, false).entities().collect(Collectors.toSet());
		substanceBranchClasses.remove(factory.getOWLNothing());
		substanceBranchClasses.remove(fakeIngredientParent);
		substanceBranchClasses.remove(snomedSubstance);			
		
		for( OWLClass cls : substanceBranchClasses ) {
//			System.out.println(cls.getIRI());
			Set<OWLClass> eqSnomedClasses = null;			
			if( !cls.getIRI().getIRIString().contains("Rx") ) {
				eqSnomedClasses = reasoner.getEquivalentClasses(cls).entities().collect(Collectors.toSet());
				if( eqSnomedClasses != null && eqSnomedClasses.size() > 1) {
					for( OWLClass c : eqSnomedClasses ) {
						if( !getCodeForClass(c).equals(getCodeForClass(cls)) && !getCodeForClass(c).contains("Rx") ) {
							log(getCodeForClass(c) + "\t"+ getRDFSLabel(c) + "\t" + getCodeForClass(cls) + "\t"+ getRDFSLabel(cls) );
//							log("RxNorm ingredient " + getCodeForClass(c) + " : "+ getRDFSLabel(c) + " is asserted to SCT substance " + getCodeForClass(cls) + " : "+ getRDFSLabel(cls) );							
							addAnnotation(cls, this.asserted, true);
							addAnnotation(cls, this.mapsToCode, getCodeForClass(c));
							addAnnotation(cls, this.mapsToName, getRDFSLabel(c));
//							c.accept(remover);
						}
					}
				}
				else {
					log("\tUnasserted SCT substance " + getRDFSLabel(cls) + " : " + this.getCodeForClass(cls));
				}
			}
		}	
		
		man.applyChanges(remover.getChanges());
		
		reasoner.flush();
		
		substanceBranchClasses = reasoner.getSubClasses(snomedSubstance, false).entities().collect(Collectors.toSet());
		substanceBranchClasses.remove(factory.getOWLNothing());
		substanceBranchClasses.remove(fakeIngredientParent);
		substanceBranchClasses.remove(snomedSubstance);			
		
		log("*** Treeing \"Fake\" substances not found in SCT ***");
		treeFakes(snomedSubstance, fakeIngredientParent, substanceBranchClasses);
		
//		log("*** Beginning the outside analysis report in Excel ***");
//		analyze(fetchRxNorm.getSCDMap());
//		diff(fetchRxNorm.getSCDMap());
//		flagAndCleanup();
				
		
		for( OWLClass c : ingEquivalencyMap.keySet()) {
			ArrayList<OWLClass> list = ingEquivalencyMap.get(c);
			log(c.getIRI().getIRIString().replace(namespace, "") + "\t" + getRDFSLabel(c));
			for( OWLClass clz : list ) {
				log("\t" + clz.getIRI().getIRIString().replace(namespace, "" + "\t") + "\t" + getRDFSLabel(clz));
			}
		}
	
		@SuppressWarnings("unused")
		File file = saveOntology();			
		
		System.out.println("********** Equivalencies Analysis **********\n");
		log("********** Equivalencies Analysis **********\n");
			
		this.analyzer = new Analyzer(fetchRxNorm.getSCDMap(), cdEquivalencyMapSnomed, scdEquivalencyMapRxNorm, ingEquivalencyMap, rxdf2SnomedDFPair, unitEquivalencyMap, numberEquivalencyMap, noSnomedDefinitionTemplate, ontology, factory, reasoner, man, pw);
		analyzer.report();
		analyzer.runAudit();
		
		reasoner.dispose();
		if( debug ) pw.close();
		ingPw.close();
		
		//prepare resources for the next file generation, just in case gc does anything weird
		Consts = null;
		man = null;
		ontology = null;
		reasoner = null;
		reasonerFactory = null;
		factory = null;		
		System.out.println();
	}
	
	public Analyzer getAnalyzer() {
		return this.analyzer;
	}
	
	private Set<OWLAxiom> equateSameNumbers(Set<OWLClass> snomedNumbers) {
		Set<OWLAxiom> axioms = new HashSet<>();
		
//		RWW: 210922
//		
//		As SNCT rightfully evolved into adopting concrete
//		domains the current version of ELK that will work with owlapi version 5.1.10 can no longer detect equivalent
//		classes, or grab the DataHasValue values because DataHasValue isn't fully supported. 
//		(See ELK issues #60 and #61.)
//		
//		Snorocket was studied as an alternative, but it too will only work with owlapi version 4.
//		
//		The latest version of Protege is still using owlapi version 4 and only ELK version 4.3 will work with it.
//		
//		(Note: Equivalenct signs present themselves in the hierarhcy for Protege build 5.5.0-beta7, though I'm not certain if this is
//		still the case in the official 5.5.0.)
//		
//		A downgrade to owlapi version 4 would take quite some time to refactor, and we would no longer be on the
//		bleeding edge. Added to this, our tech stack doesn't permit the lesser version with earlier versions of Java.
//		
//		What we do now is convert datahasvalue back from datatype properties to object properties using the
//		old object property class codes (for strengths, etc.) with newly created number classes as objects (PERL doesn't know how to reason without an acceptable library). 
//		We then create number classes based on the construct value, and *now* we will equate them
//		to existing snct qualifer values (why these qualifier values still exist in the new snct is to probably
//		support other domains outside of the medicinal product branch).
//		
//		I first found the need for this after discovering all SCDs with the number 100 that should map were no longer
		
		//O^2 + 1
		for(OWLClass c : snomedNumbers) {
			String cValue = getRDFSLabel(c).replace(" (qualifier value)", "");
			for(OWLClass d : snomedNumbers) {
				String dValue = getRDFSLabel(d).replace(" (qualifier value)", "");
				if(cValue.equals(dValue)) {
					OWLAxiom a = factory.getOWLEquivalentClassesAxiom(c, d);
					axioms.add(a);
					//there can and should only be one
					break;
				}
			}
		}
		
		return axioms;
	}
	
//	private void addMissingSubstanceMappings() {
//		for( String key : jnMissingSubstanceMappings.keySet() ) {
//			ArrayList<String> substances = jnMissingSubstanceMappings.get(key);
//			for(String substance : substances ) {
//				OWLClass rxClass = factory.getOWLClass(namespace, key);
//				OWLClass snomedClass = factory.getOWLClass(namespace, substance);
//				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(rxClass, snomedClass);
//				man.addAxiom(ontology, eq);			
//			}
//		}		
//	}
	
	private TreeMap<String, ArrayList<String>> mapFromFile(String filename) {
		TreeMap<String, ArrayList<String>> map = new TreeMap<String, ArrayList<String>>();
		
		FileReader file = null;
		BufferedReader buff = null;
		try {
			file = new FileReader(filename);
			buff = new BufferedReader(file);
			boolean eof = false;
//			int colIndex = -1;
			while (!eof) {
				String line = buff.readLine();
				if (line == null)
					eof = true;
				else {	
					if( line != null && line.contains("\t") ) {
						String[] values = line.split("\t", -1);							
						String code1 = values[0].trim();
						String code2 = values[2].trim();						
						addToMap(code1, code2, map);
					}
				}
			}
						
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
		
		return map;
	}

	private String getCodeForClass(OWLClass c) {
		return c.getIRI().getIRIString().replace(namespace, "");
	}
	
	private void createAnnotation(OWLAnnotationProperty ap, String s) {
		OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(s));
		OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(ap.getIRI(), label);
		man.addAxiom(ontology, labelToClass);	
	}
	
	private void createMapsToAnnotations() {
		log("Creating annotation declarations for informational purposes.");
		
		createAnnotation(this.mapsToCode, "maps to rxnorm code");
		createAnnotation(this.mapsToName, "maps to rxnorm name");
		createAnnotation(this.asserted, "asserted equivalence");
		createAnnotation(this.inferred, "inferred equivalence");
		createAnnotation(this.substanceNoExist, "substance doesn't exist");
		createAnnotation(this.substanceDifferent, "substance is different");
		createAnnotation(this.hasNDC, "has NDC");
		createAnnotation(this.hasRxCUI, "has RxCUI");
		createAnnotation(this.hasExplanation, "misc explanation");
		createAnnotation(this.bossSubstanceDifferent, "BoSS substance difference");
		createAnnotation(this.activeIngerdientSubstanceDifferent, "active ingredient difference");
		createAnnotation(this.doseFormDifferent, "dose form difference");
		createAnnotation(this.presUnitDifferent, "unit of presentation difference");
		createAnnotation(this.unitsDifferent, "unit difference");
		createAnnotation(this.valuesDifferent, "values difference");
		createAnnotation(this.countBaseDifferent, "count of base difference");
		createAnnotation(this.vetOnly, "vet only");
		createAnnotation(this.allergenic, "allergenic");
		createAnnotation(this.hasAllergenic, "has allergenic substance");
		createAnnotation(this.isPrescribable, "prescribable");
		createAnnotation(this.isVaccine, "vaccine");
	}
	
	private void addAnnotation(OWLClass c, OWLAnnotationProperty ap, boolean filler) {
		OWLAnnotation anno = factory.getOWLAnnotation(ap, factory.getOWLLiteral(String.valueOf(filler)));
		OWLAxiom annoToClass = factory.getOWLAnnotationAssertionAxiom(c.getIRI(), anno);
		man.addAxiom(ontology, annoToClass);
	}	
	
	private void addMapsAnnotation(OWLClass snomed, OWLClass rx) {
				
		String code = getCodeForClass(rx);
		String name = getRDFSLabel(rx);
		
		if( code != null && name != null) {
			Set<OWLAxiom> axioms = new HashSet<>();
			OWLAnnotationProperty annop = factory.getOWLAnnotationProperty(namespace, "MapsToCode");
			OWLAnnotation anno = factory.getOWLAnnotation(annop, factory.getOWLLiteral(code));
			OWLAxiom annoToClass = factory.getOWLAnnotationAssertionAxiom(snomed.getIRI(), anno);
			axioms.add(annoToClass);
			
			annop = factory.getOWLAnnotationProperty(namespace, "MapsToName");
			anno = factory.getOWLAnnotation(annop, factory.getOWLLiteral(name));
			annoToClass = factory.getOWLAnnotationAssertionAxiom(snomed.getIRI(), anno);
			axioms.add(annoToClass);
//			System.out.println("Add maps to annos");
			man.addAxioms(ontology, axioms.stream());			
		}
				
	}

	public void addFakeParent(OWLClass fakeParent, OWLClass snomedParent, String labelForFake) {
		OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(labelForFake));
		OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(fakeParent.getIRI(), label);
		OWLSubClassOfAxiom subClassAxiom =factory.getOWLSubClassOfAxiom(fakeParent, snomedParent);
		
		log("Adding fake parent");
		man.addAxiom(ontology, labelToClass);
		man.addAxiom(ontology, subClassAxiom);
		reasoner.flush();
	}
	
	private void treeFakes(OWLClass original, OWLClass destination, Set<OWLClass> possibleFakes) {
		possibleFakes.remove(original);
		for( OWLClass c : possibleFakes ) {
			String label = getRDFSLabel(c);
			if( label != null && label.contains(" (Fake)") ) {
				log("Retreeing \"Fake\" " + label);
				addAnnotation(c, this.asserted, false);				
				retree(c, destination);
				reasoner.flush();
			}
		}
	}
	
	private void treeCdFakes(OWLClass mp, OWLClass untranslatedParent, OWLClass translatedParent) {
		
		reasoner.getSubClasses(mp, false).entities().filter(w -> !w.equals(mp) && !w.equals(untranslatedParent) && !w.equals(translatedParent) && !w.equals(factory.getOWLNothing()))
//				.filter(x -> (getCodeForClass(x).contains("Rx") && !allSBDs.contains(x)))
				.filter(x -> (getCodeForClass(x).contains("Rx")))
				.filter(y -> !EntitySearcher.isDefined(y, ontology) )
				.forEach(z -> {
					retree(z, untranslatedParent);
				});
//				.collect(Collectors.toSet());

		reasoner.getSubClasses(mp, false).entities().filter(w -> !w.equals(mp) && !w.equals(untranslatedParent) && !w.equals(translatedParent) && !w.equals(factory.getOWLNothing()))
				.filter(x -> getCodeForClass(x).contains("Rx"))
				.filter(y -> EntitySearcher.isDefined(y, ontology) )
				.forEach(z -> {
					retree(z, translatedParent);
				});
	}	
	
	private void retree(OWLClass a, OWLClass destination) {
		Set<OWLAxiom> axiomsForRemoval = new HashSet<>();
		Set<OWLAxiom> subclassAxioms = ontology.axioms(a).filter(y -> y.isOfType(AxiomType.SUBCLASS_OF)).collect(Collectors.toSet());
		for( OWLAxiom axiom : subclassAxioms ) {
			axiomsForRemoval.add(axiom);
		}

// This section will toggle the presence of equivalent classes. When commmented all definitions on the classes
// are kept and not removed.
//		Set<OWLAxiom> eqAxioms = ontology.equivalentClassesAxioms(a).collect(Collectors.toSet());
//		for( OWLAxiom axiom : eqAxioms ) {
//			axiomsForRemoval.add(axiom);
//		}
		man.removeAxioms(ontology, axiomsForRemoval.stream());	
		
		OWLSubClassOfAxiom newSubClass = factory.getOWLSubClassOfAxiom(a, destination);
		man.addAxiom(ontology, newSubClass);				
	}
	
	public void addToMap(String c, String d, TreeMap<String, ArrayList<String>> map) {
		if( map.containsKey(c) ) {
			ArrayList<String> list = map.get(c);
			list.add(d);
			map.put(c, list);
		}
		else {
			ArrayList<String> list = new ArrayList<String>();
			list.add(d);
			map.put(c, list);
		}		
	}	
	
	public void addToMap(OWLClass c, OWLClass d, TreeMap<OWLClass, ArrayList<OWLClass>> map) {
		if( map.containsKey(c) ) {
			ArrayList<OWLClass> list = map.get(c);
			list.add(d);
			map.put(c, list);
		}
		else {
			ArrayList<OWLClass> list = new ArrayList<OWLClass>();
			list.add(d);
			map.put(c, list);
		}		
	}		
	
	public void getMDFAndUP(RxNormSCD scd) {
		RxNormDoseForm df = scd.getRxNormDoseForm().get(0);
		Integer rxDfCui = Integer.valueOf(df.getRxcui());
		if( rxdf2SnomedDFPair.containsKey(rxDfCui) ) {
			SnomedDFPair pair = rxdf2SnomedDFPair.get(rxDfCui);
			if( pair.hasDF() ) {
				scd.setManufacturedDoseFormCode(pair.getSnomedManufacturedDoseForm().getCode());
				scd.setManufacturedDoseFormName(pair.getSnomedManufacturedDoseForm().getName());
				scd.setSnomedDefinitionTemplate(pair.getSnomedDefinitionTemplate());
			}
			if( pair.hasUP() ) {
				scd.setUnitOfPresentationCode(pair.getSnomedUnitOfPresentation().getCode());
				scd.setUnitOfPresentationName(pair.getSnomedUnitOfPresentation().getName());
			}	
		}
		else {
			scd.setSnomedDefinitionTemplate(null);
		}
		
	}
	
	
	public void setConstants() {
		this.medicinalProduct = factory.getOWLClass(namespace, "763158003");
		this.numberClass = factory.getOWLClass(namespace, "260299005");
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
		this.allergenic = factory.getOWLAnnotationProperty(namespace, "Allergenic");
		this.hasAllergenic = factory.getOWLAnnotationProperty(namespace, "HasAllergenic");
		this.isPrescribable = factory.getOWLAnnotationProperty(namespace, "IsPrescribable");
		this.isVaccine = factory.getOWLAnnotationProperty(namespace, "IsVaccine");

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
	
	public Set<OWLAxiom> generateUnitsOfMeasureAxioms(TreeMap<String, Double> unitMap, OWLClass parent) {
		Set<OWLAxiom> axioms = new HashSet<>();
		for(String unit : unitMap.keySet() ) {
			OWLClass unitClass = factory.getOWLClass(namespace, df.format(++snomedCodeGenerator) + "-FS");
			OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(unit));
			
			unitClassToName.put(unitClass, unit);
			
			OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(unitClass.getIRI(), label);
			OWLSubClassOfAxiom subClassAxiom =factory.getOWLSubClassOfAxiom(unitClass, parent);
			
			OWLClass snomedEq = null;
			if(unit.equalsIgnoreCase("kg")) {
				snomedEq = factory.getOWLClass(namespace, "258683005");				
			}
			else if(unit.equalsIgnoreCase("g")) {
				snomedEq = factory.getOWLClass(namespace, "258682000");
			}
			else if(unit.equalsIgnoreCase("mg")) {
				snomedEq = factory.getOWLClass(namespace, "258684004");
			}
			else if(unit.equalsIgnoreCase("mcg")) {
				snomedEq = factory.getOWLClass(namespace, "258685003");								
			}
			else if(unit.equalsIgnoreCase("ng")) {
				snomedEq = factory.getOWLClass(namespace, "258686002");						
			}
			else if(unit.equalsIgnoreCase("pg")) {
				snomedEq = factory.getOWLClass(namespace, "258687006");				
			}
			else if(unit.equalsIgnoreCase("ml")) {
				snomedEq = factory.getOWLClass(namespace, "258773002");
			}
			else if(unit.equalsIgnoreCase("l")) {
				snomedEq = factory.getOWLClass(namespace, "258770004");
			}
			else if(unit.equalsIgnoreCase("unt")) {
				snomedEq = factory.getOWLClass(namespace, "767525000");
			}
			else if(unit.equalsIgnoreCase("hr")) {
				snomedEq = factory.getOWLClass(namespace, "258702006");
			}
			else if(unit.equalsIgnoreCase("actuat")) {
				snomedEq = factory.getOWLClass(namespace, "732981002");
			}			
			if( snomedEq != null ) {
				OWLEquivalentClassesAxiom eqUnits = factory.getOWLEquivalentClassesAxiom(unitClass, snomedEq);
				axioms.add(eqUnits);
			}
			
			axioms.add(labelToClass);
			axioms.add(subClassAxiom);
		}
		
		return axioms;
	}
	
	public OWLClass createNumberClass(Double val) {
		OWLClass cls = null;
		String valString = null;
		if( val % 1 == 0) {
			valString = String.valueOf(val.intValue());
		}
		else {
			valString = String.valueOf(val);
		}

		if( valString != null && !snomedNumberQualifier.containsKey(valString) ) {
			cls = factory.getOWLClass(namespace, df.format(++snomedCodeGenerator) + "-FS");
			OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(valString + " (Fake)"));
			OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(), label);
			OWLSubClassOfAxiom subClassAxiom = factory.getOWLSubClassOfAxiom(cls, numberClass);
//			System.out.println("Adding number class");
			man.addAxiom(ontology, labelToClass);
			man.addAxiom(ontology, subClassAxiom);
			log("creating a needed \"Fake\" SCT number class " + valString);
			addAnnotation(cls, this.valuesDifferent, "no SCT number class");
			snomedNumberQualifier.put(valString, cls);
		}
		else {
			cls = snomedNumberQualifier.get(valString);
		}		
		
//		numberClassToName.put(val, cls);
		
		
		return cls;
	}
	
	@SuppressWarnings("deprecation")
	public Set<OWLAxiom> generateNumberAxioms(Set<String> numberSet, Set<OWLClass> snomedNumberClasses, OWLClass parent) {
		Set<OWLAxiom> axioms = new HashSet<>();
		snomedNumberClasses.remove(numberClass);
		

		//We need integers for any number of BoSSes.
		for(int i = 0; i < 8; i++ ) {
			if( !numberSet.contains(String.valueOf(i)) ) {
				numberSet.add(String.valueOf(i));
			}
		}
	
		//Before we can loop through RxNorm numbers (and create axioms)
		//we need a map of all the SNOMED numbers to their identifiers.  This is going
		//to be an extra step I hope remove someday since SNOMED isn't using concrete xsd
	    //numerics such as xsd:decimal. This was a decision to support SOLOR.
		//We'll use the reasoner to get all the descendants, obtain the value
		//of the rdfs:label and put the <String label, OWLClass> into a map
		for( OWLClass sn : snomedNumberClasses ) {
			String label = getRDFSLabel(sn);
			if( label != null ) {
				label = label.replace(" (qualifier value)", "");
				if(NumberUtils.isNumber(label)) {
					snomedNumberQualifier.put(label, sn);
				}
				else {
					log("Cannot create a number for SCT Number child " + label);
				}
			}
		}
		
		for( String numberString : numberSet ) {
			
			String labelString = numberString;
			
			if( !snomedNumberQualifier.containsKey(labelString) && NumberUtils.isNumber(labelString) ) {
				OWLClass snClass = factory.getOWLClass(namespace, df.format(++snomedCodeGenerator) + "-FS");
				OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(labelString + " (Fake)"));
				OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(snClass.getIRI(), label);				
				OWLSubClassOfAxiom subClassAxiom = factory.getOWLSubClassOfAxiom(snClass, numberClass);				
				snomedNumberQualifier.put(labelString, snClass);
				axioms.add(labelToClass);
				axioms.add(subClassAxiom);						
				addToMap(snClass, numberClass, this.numberEquivalencyMap);						
			}

		}
		
		return axioms;
	}
	
	public String getRDFSLabel(OWLEntity cls) {
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
	}	
	
	public Set<OWLAxiom> generateClinicalDrugAxioms(TreeMap<Integer, RxNormSCD> scdMap) {	
		Set<OWLAxiom> axioms = new HashSet<>();
		int scdCount = 0;
		
		log("*** Adding Rx clinical drug classes with SCT classes where applicable ***");
		
		log("Working with " + scdMap.keySet().size() + " SCDs");
		
		for( Integer i : scdMap.keySet() ) {
			
			boolean applyDefinition = true;
			
			RxNormSCD scd = scdMap.get(i); 
			
			scd.setSnomedCodes();
			
			log("\tRx SCD class " + scd.getName() + " : " + i);
			OWLClass scdClass = factory.getOWLClass(namespace, "Rx" + i);
			addAnnotation(scdClass, this.hasRxCUI, String.valueOf(scd.getCui()));
			
			if(scd.getSnomedCodes().size() > 0) {
				addAnnotation(scdClass, this.asserted, true);
			}			
			
			if( scd.isVetOnly() ) {
				addAnnotation(scdClass, this.vetOnly, true);
			}
			
			for( RxNormIngredient in : scd.getRxNormIngredient() ) {
				if( in.getIsAllergenic() ) {
					addAnnotation(scdClass, this.hasAllergenic, in.getName() + " : " + String.valueOf(in.getRxcui()) );
				}
			}
			
			Collection<OWLClassExpression> oces = new HashSet<OWLClassExpression>();
			oces.add(this.medicinalProduct);

			if( scd.hasNDC() ) {
				log("\t\tNDC exists");
				this.addAnnotation(scdClass, this.hasNDC, true);
			}
			else {
				log("\t\tNDC DOES NOT exist");
				this.addAnnotation(scdClass,  this.hasNDC, false);
			}
			
			if( scd.getIsPrescribable() ) {
				log("\t\tSCD Prescribable");
				this.addAnnotation(scdClass, this.isPrescribable, true);
			}
			else {
				log("\t\tSCD NOT Prescribable");
				this.addAnnotation(scdClass,  this.isPrescribable, false);
			}
			if( scd.getIsVaccine() ) {
				log("\t\tSCD IS Vaccine");
				this.addAnnotation(scdClass, this.isVaccine , true);
			}
			 
			String doseFormCui = scd.getRxNormDoseForm().get(0).getRxcui();
			
			if( scd.getSnomedDefinitionTemplate() == null ) {
				log("\t\tNo SCT clinical drug exists with the following RxNorm DF: " + scd.getRxNormDoseForm().get(0).getName());
				log(false);
				
				applyDefinition = false;
			}
			
			//Manufactured Dose Form assertion on the concept.  There can only be one per SCD.
			//We need to assume this class already exists
			if( applyDefinition && scd.getManufacturedDoseFormCode() != null ) {
				OWLClass manufactureDoseFormClass = factory.getOWLClass(namespace, String.valueOf(scd.getManufacturedDoseFormCode()));
				OWLObjectSomeValuesFrom objectSomeValuesFromHasManufacturedDoseForm = factory.getOWLObjectSomeValuesFrom(hasManufacturedDoseForm, manufactureDoseFormClass);
				oces.add(objectSomeValuesFromHasManufacturedDoseForm);
			}
			else if( scd.getManufacturedDoseFormCode() == null ){
				log("\t\tUnable to resolve the SCT manufactured dose form code.");
				addAnnotation(scdClass, this.doseFormDifferent, "cannot map SCT manufactured dose form");
				log(false);
				applyDefinition = false;
			}
			
			//There can be more than one BoSS for any SCD.
			//Apply each to the new concept as a Role Group.
			//We'll keep track of how many there are per SCD so we can combine
			//them later (if necessary) for the class expression for the equivalent class
			//intoto.
			if( applyDefinition ) {
				log("\t\tA template and SCT mapped DF exist--building BoSS(s) for definition");
				for( RxNormBoss boss : scd.getRxNormBoss() ) {
					
					if( boss.getBossRxCui() != null ) {
						scd.addBaseCui(boss.getBaseRxcui());  //RxCUI 313551 solo rarity - yes, this is not being done correctly because there are more missing AI than BoSS
															  //Could we instead count vector size?
					}

					OWLObjectSomeValuesFrom objectSomeValuesFromHasPresentationStrengthNumeratorValue = null;
					OWLObjectSomeValuesFrom objectSomeValuesFromHasPresentationStrengthNumeratorUnit = null;
					OWLObjectSomeValuesFrom objectSomeValuesFromHasPresentationStrengthDenominatorValue = null;
					OWLObjectSomeValuesFrom objectSomeValuesFromHasPresentationStrengthDenominatorUnit = null;

					OWLObjectSomeValuesFrom objectSomeValuesFromHasConcentrationStrengthNumeratorValue = null;
					OWLObjectSomeValuesFrom objectSomeValuesFromHasConcentrationStrengthNumeratorUnit = null;
					OWLObjectSomeValuesFrom objectSomeValuesFromHasConcentrationStrengthDenominatorValue = null;
					OWLObjectSomeValuesFrom objectSomeValuesFromHasConcentrationStrengthDenominatorUnit = null;				

					OWLClass bossNameClass = null;
					if( boss.getBossRxCui() != null && boss.getBossRxCui() != -1) {
						bossNameClass = factory.getOWLClass(namespace, "Rx" + boss.getBossRxCui().toString());
						if( ingEquivalencyMap.containsKey(bossNameClass)) {
							ArrayList<OWLClass> list = ingEquivalencyMap.get(bossNameClass);
							//TODO: Deciding how to handle the case for many SCT equivalents (there should be none)
							for(OWLClass c : list ) {
								if( !isRx(c) ) {
									bossNameClass = c;
								}
							}
							if( isRx(bossNameClass) ) {
								addAnnotation(scdClass, this.bossSubstanceDifferent, bossNameClass + " not found in SCT");
								log("\t\t\tThe ingredient in this boss cannot be found in SCT " + boss.getBossRxCui() + " : " + boss.getBossName());
								log("\t\t\tA \"Fake\" should already exist.");
							}
						}
					}
					else {
						addAnnotation(scdClass, this.bossSubstanceDifferent , "ing not found in RxNorm");
						log("\t\t\tMissing BossRxCui in RxNorm");
						bossNameClass = factory.getOWLClass(namespace, "Rx0");
					}

					if( getRDFSLabel(bossNameClass) != null && getRDFSLabel(bossNameClass).contains("(Fake)")) {
						addAnnotation(scdClass, this.substanceNoExist, "No equivalent SCT substance for Rx BoSS " + boss.getBossRxCui() + " : " + boss.getBossName());						
					}
					OWLObjectSomeValuesFrom objectSomeValuesFromBasisOfStrength = factory.getOWLObjectSomeValuesFrom(hasBasisOfStrengthSubstance, bossNameClass);
					
					OWLClass preciseActiveIngredientClass = null;			
					if( boss.getActiveIngredientRxCui() != -1 ) {
						preciseActiveIngredientClass = factory.getOWLClass(namespace, "Rx" + String.valueOf(boss.getActiveIngredientRxCui()));								
						if( ingEquivalencyMap.containsKey(preciseActiveIngredientClass) ) {
							ArrayList<OWLClass> list = ingEquivalencyMap.get(preciseActiveIngredientClass);
							for( OWLClass c : list ) {
								if( !isRx(c) ) {
									preciseActiveIngredientClass = c;
								}
								else {
									addAnnotation(scdClass, this.activeIngerdientSubstanceDifferent , "fake SCT AI in BoSS " + boss.getBossRxCui() );
									log("\t\tA fake IN for this Rx Active Ingredient exists\t" + boss.getActiveIngredientRxCui() + "\t" + boss.getActiveIngredientRxCui());
								}
							}
						}
					}
					else {
						//add annotation to the class the AI doesn't exist
						log("\t\tAn Active Ingredient doesn't exist in BoSS " + boss.getBossName() + " : " + boss.getBossRxCui());
						preciseActiveIngredientClass = factory.getOWLClass(namespace, "Rx0");								
						addAnnotation(scdClass, this.activeIngerdientSubstanceDifferent, "missing Rx AI in BoSS " + boss.getBossName() + " : " + boss.getBossRxCui());
						for( Long snomedCode : scd.getSnomedCodes() ) {
							OWLClass snomedClass = factory.getOWLClass(namespace, String.valueOf(snomedCode));
							if( getRDFSLabel(snomedClass) != null && getRDFSLabel(snomedClass).contains("(clinical drug)") ) {
								addAnnotation(snomedClass, this.asserted, true);
								addAnnotation(snomedClass, this.inferred, false);  //yes, it is not inferred. we don't need a reasoner to tell us this.
							}
						}									
					}
					
					
					OWLObjectSomeValuesFrom objectSomeValuesFromPreciseIngredient = factory.getOWLObjectSomeValuesFrom(hasPreciseActiveIngredient, preciseActiveIngredientClass);

					boolean isPresentation = scd.getSnomedDefinitionTemplate().isIsPresentation();
					boolean isConcentration = scd.getSnomedDefinitionTemplate().isIsConcentration();
					boolean applyPresentationUnitOfPresentation = true;
					boolean applyConcentrationUnitOfPresentation = true;

					if( applyDefinition && isPresentation && isConcentration ) {
						log("\t\tapplying a Presentation & Concentration definition");

						//					System.out.println(scd.getCui() + "\t" + scd.getName() + "\t" + scd.getRxNormQuantityFactor());

						// Numerator Value
						OWLClass numeratorValueClass = getOWLClassForNumberString(String.valueOf(boss.getSnomedNumberatorValue()), snomedNumberQualifier);

						OWLClass hasConcentrationStrengthNumeraterValue = factory.getOWLClass(numeratorValueClass); 
						objectSomeValuesFromHasConcentrationStrengthNumeratorValue = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthNumeratorValue, hasConcentrationStrengthNumeraterValue);

						// Numerator Unit
						OWLClass numeratorUnitClass = getOWLClassForString(boss.getSnomedNumeratorUnit(), this.unitClassToName);

						OWLClass hasConcentrationStrengthNumeratorUnit = factory.getOWLClass(numeratorUnitClass);
						objectSomeValuesFromHasConcentrationStrengthNumeratorUnit = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthNumeratorUnit, hasConcentrationStrengthNumeratorUnit);

						// Denomiator Value
						OWLClass denominatorValueClass = getOWLClassForNumberString(String.valueOf(boss.getSnomedDenominatorValue()), snomedNumberQualifier);

						OWLClass hasConcentrationStrengthDenominatorValue = factory.getOWLClass(denominatorValueClass);
						objectSomeValuesFromHasConcentrationStrengthDenominatorValue = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthDenominatorValue, hasConcentrationStrengthDenominatorValue);

						// Denominator Unit
						OWLClass denominatorUnitClass = getOWLClassForString(boss.getSnomedDenominatorUnit(), this.unitClassToName);
						if( denominatorUnitClass == null ) {
							OWLClass du = factory.getOWLClass(namespace, String.valueOf(scd.getUnitOfPresentationCode()));
							if( du != null && !du.getIRI().getFragment().equals("null")) {
								denominatorUnitClass = du;
							}
							else {
								applyConcentrationUnitOfPresentation = false;
							}
						}

						OWLClass hasConcentrationStrengthDenominatorUnit = factory.getOWLClass(denominatorUnitClass);
						objectSomeValuesFromHasConcentrationStrengthDenominatorUnit = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthDenomniatorUnit, hasConcentrationStrengthDenominatorUnit);


						//Also add presentation strengths
						log("\t\tcalculating the presentation units based on the existing quantity factor");
						ConcentrationToPresentation presentationUnits = new ConcentrationToPresentation(boss, scd.getRxNormQuantityFactor());
						boss.setConcentrationToPresentation(presentationUnits);

						//If the presentation numerator doesnt exist as a number add it
						OWLClass presentationNumeratorValueClass = getOWLClassForNumberString(String.valueOf(presentationUnits.getPresentationNumeratorValue()), snomedNumberQualifier);
						if( presentationNumeratorValueClass == null ) {
							log("\t\tthe calculated numerator doesn't exist so will create one for: " + presentationUnits.getPresentationNumeratorValue());
							addAnnotation(scdClass, this.valuesDifferent, "presentation numerator has no sct number "  + boss.getBossRxCui() + " : " + boss.getBossName());							
							presentationNumeratorValueClass = createNumberClass(presentationUnits.getPresentationNumeratorValue());
						}

						objectSomeValuesFromHasPresentationStrengthNumeratorValue = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthNumeratorValue, presentationNumeratorValueClass);

						numeratorUnitClass = getOWLClassForString(presentationUnits.getPresentationNumeratorUnit(), this.unitClassToName);
						objectSomeValuesFromHasPresentationStrengthNumeratorUnit = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthNumeratorUnit, numeratorUnitClass);

						OWLClass presentationDenominatorValueClass = getOWLClassForNumberString(String.valueOf(presentationUnits.getPresentationDenominatorValue()), snomedNumberQualifier);
						if( presentationDenominatorValueClass == null ) {
							log("\t\tthe calculated denominator doesn't exist so will create one for: " + presentationUnits.getPresentationDenominatorValue());
							addAnnotation(scdClass, this.valuesDifferent, "presentation denominator has no sct number "  + boss.getBossRxCui() + " : " + boss.getBossName());							
							presentationDenominatorValueClass = createNumberClass(presentationUnits.getPresentationDenominatorValue());

						}

						objectSomeValuesFromHasPresentationStrengthDenominatorValue = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthDenominatorValue, presentationDenominatorValueClass);

						// Denominator Unit
						denominatorUnitClass = getOWLClassForString(presentationUnits.getPresentationDenominatorUnit(), this.unitClassToName);
						if( denominatorUnitClass == null ) {
							OWLClass du = factory.getOWLClass(namespace, String.valueOf(scd.getUnitOfPresentationCode()));
							if( du != null && !du.getIRI().getFragment().equals("null")) {
								denominatorUnitClass = du;
							}
							else {
								log("\t\tthe unit of presentation code on BoSS denominator doesn't exist " + boss.getBossRxCui() + " : " + boss.getBossName());
								log("\t\t\tUnable to apply the Presentation predicate's unit class");
								addAnnotation(scdClass, this.unitsDifferent, "no eq unit denomintator in BoSS " + boss.getBossRxCui() + " : " + boss.getBossName());
								applyPresentationUnitOfPresentation = false;
							}
						}

						OWLClass hasPresentationStrengthDenominatorUnit = factory.getOWLClass(denominatorUnitClass);
						objectSomeValuesFromHasPresentationStrengthDenominatorUnit = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthDenominatorUnit, hasPresentationStrengthDenominatorUnit);					



					}				
					else if( applyDefinition && isConcentration ) {
						// Numerator Value
						OWLClass numeratorValueClass = getOWLClassForNumberString(String.valueOf(boss.getSnomedNumberatorValue()), snomedNumberQualifier);

						OWLClass hasConcentrationStrengthNumeraterValue = factory.getOWLClass(numeratorValueClass); 
						objectSomeValuesFromHasConcentrationStrengthNumeratorValue = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthNumeratorValue, hasConcentrationStrengthNumeraterValue);

						// Numerator Unit
						OWLClass numeratorUnitClass = getOWLClassForString(boss.getSnomedNumeratorUnit(), this.unitClassToName);

						OWLClass hasConcentrationStrengthNumeratorUnit = factory.getOWLClass(numeratorUnitClass);
						objectSomeValuesFromHasConcentrationStrengthNumeratorUnit = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthNumeratorUnit, hasConcentrationStrengthNumeratorUnit);

						// Denomiator Value
						OWLClass denominatorValueClass = getOWLClassForNumberString(String.valueOf(boss.getSnomedDenominatorValue()), snomedNumberQualifier);

						OWLClass hasConcentrationStrengthDenominatorValue = factory.getOWLClass(denominatorValueClass);
						objectSomeValuesFromHasConcentrationStrengthDenominatorValue = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthDenominatorValue, hasConcentrationStrengthDenominatorValue);

						// Denominator Unit
						OWLClass denominatorUnitClass = getOWLClassForString(boss.getSnomedDenominatorUnit(), this.unitClassToName);
						if( denominatorUnitClass == null ) {
							OWLClass du = factory.getOWLClass(namespace, String.valueOf(scd.getUnitOfPresentationCode()));
							if( du != null ) {
								denominatorUnitClass = du;
							}
							else {
								log("\t\tthe unit on BoSS denominator doesn't exist " + boss.getBossName() + " : " + boss.getBossRxCui());
								log("\t\t\tUnable to apply the Concentration predicate's unit class");
								addAnnotation(scdClass, this.unitsDifferent, "concentration denominator has no sct number  " + boss.getBossRxCui() + " : " + boss.getBossName());								
								applyConcentrationUnitOfPresentation = false;
							}
						}

						OWLClass hasConcentrationStrengthDenominatorUnit = factory.getOWLClass(denominatorUnitClass);
						objectSomeValuesFromHasConcentrationStrengthDenominatorUnit = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthDenomniatorUnit, hasConcentrationStrengthDenominatorUnit);
					}					
					else if( applyDefinition && isPresentation ) {						
						// Numerator Value
						OWLClass numeratorValueClass = getOWLClassForNumberString(String.valueOf(boss.getSnomedNumberatorValue()), snomedNumberQualifier);

						OWLClass hasPresentationStrengthNumeraterValue = factory.getOWLClass(numeratorValueClass); 
						objectSomeValuesFromHasPresentationStrengthNumeratorValue = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthNumeratorValue, hasPresentationStrengthNumeraterValue);

						// Numerator Unit
						OWLClass numeratorUnitClass = getOWLClassForString(boss.getSnomedNumeratorUnit(), this.unitClassToName);

						OWLClass hasPresentationStrengthNumeratorUnit = factory.getOWLClass(numeratorUnitClass);
						objectSomeValuesFromHasPresentationStrengthNumeratorUnit = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthNumeratorUnit, hasPresentationStrengthNumeratorUnit);

						// Denomiator Value
						OWLClass denominatorValueClass = getOWLClassForNumberString(String.valueOf(boss.getSnomedDenominatorValue()), snomedNumberQualifier);

						OWLClass hasPresentationStrengthDenominatorValue = factory.getOWLClass(denominatorValueClass);
						objectSomeValuesFromHasPresentationStrengthDenominatorValue = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthDenominatorValue, hasPresentationStrengthDenominatorValue);

						// Denominator Unit
						OWLClass denominatorUnitClass = null;
//						if( denominatorUnitClass == null ) {
						if(scd.getUnitOfPresentationCode() != null) {
							OWLClass du = factory.getOWLClass(namespace, String.valueOf(scd.getUnitOfPresentationCode()));
							if( du != null && !du.getIRI().getFragment().equals("null") ) {
								denominatorUnitClass = du;
							}
							else {
								log("\t\tthe unit on BoSS denominator doesn't exist " + boss.getBossName() + " : " + boss.getBossRxCui());
								log("\t\t\tUnable to apply the Presentation predicate's unit class");
								addAnnotation(scdClass, this.unitsDifferent, "no eq unit denomintator BoSSRxCUI " + boss.getBossRxCui());								
								applyPresentationUnitOfPresentation = false;
							}
						}
						else {
							denominatorUnitClass = getOWLClassForString(boss.getSnomedDenominatorUnit(), this.unitClassToName);							
						}

						OWLClass hasPresentationStrengthDenominatorUnit = factory.getOWLClass(denominatorUnitClass);
						objectSomeValuesFromHasPresentationStrengthDenominatorUnit = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthDenominatorUnit, hasPresentationStrengthDenominatorUnit);
					}			



					// Produce the intersection of all the above
					OWLObjectIntersectionOf intersection = null;

					if( applyDefinition && isPresentation && isConcentration ) {
						log("\t\tapplying a Presentation & Concentration definition");						
						
						Vector<OWLClassExpression> desiredAxioms = new Vector<OWLClassExpression>();
						
						desiredAxioms.add(objectSomeValuesFromBasisOfStrength);
						desiredAxioms.add(objectSomeValuesFromHasPresentationStrengthNumeratorValue);
						desiredAxioms.add(objectSomeValuesFromHasPresentationStrengthNumeratorUnit);
						desiredAxioms.add(objectSomeValuesFromHasPresentationStrengthDenominatorValue);			
						if( applyPresentationUnitOfPresentation ) {
							desiredAxioms.add(objectSomeValuesFromHasPresentationStrengthDenominatorUnit);
						}
						desiredAxioms.add(objectSomeValuesFromHasConcentrationStrengthNumeratorValue);
						desiredAxioms.add(objectSomeValuesFromHasConcentrationStrengthNumeratorUnit);
						desiredAxioms.add(objectSomeValuesFromHasConcentrationStrengthDenominatorValue);							
						if( applyConcentrationUnitOfPresentation ) {
							desiredAxioms.add(objectSomeValuesFromHasConcentrationStrengthDenominatorUnit);
						}
						desiredAxioms.add(objectSomeValuesFromPreciseIngredient);

						
						intersection = factory.getOWLObjectIntersectionOf(desiredAxioms);				

					}
					else if( applyDefinition && isConcentration ) {
						log("\t\tapplying a Concentration definition");						
						Vector<OWLClassExpression> desiredAxioms = new Vector<OWLClassExpression>();
						
						desiredAxioms.add(objectSomeValuesFromBasisOfStrength);
						desiredAxioms.add(objectSomeValuesFromHasConcentrationStrengthNumeratorValue);
						desiredAxioms.add(objectSomeValuesFromHasConcentrationStrengthNumeratorUnit);
						desiredAxioms.add(objectSomeValuesFromHasConcentrationStrengthDenominatorValue);							
						if( applyConcentrationUnitOfPresentation ) {
							desiredAxioms.add(objectSomeValuesFromHasConcentrationStrengthDenominatorUnit);
						}
						desiredAxioms.add(objectSomeValuesFromPreciseIngredient);						

						
						intersection = factory.getOWLObjectIntersectionOf(desiredAxioms);				

					}
					else if( applyDefinition && isPresentation ) {
						log("\t\tapplying a Presentation definition");						
						Vector<OWLClassExpression> desiredAxioms = new Vector<OWLClassExpression>();
						
						desiredAxioms.add(objectSomeValuesFromBasisOfStrength);
						desiredAxioms.add(objectSomeValuesFromHasPresentationStrengthNumeratorValue);
						desiredAxioms.add(objectSomeValuesFromHasPresentationStrengthNumeratorUnit);
						desiredAxioms.add(objectSomeValuesFromHasPresentationStrengthDenominatorValue);			
						if( applyPresentationUnitOfPresentation ) {
							desiredAxioms.add(objectSomeValuesFromHasPresentationStrengthDenominatorUnit);
						}
						desiredAxioms.add(objectSomeValuesFromPreciseIngredient);
						
						intersection = factory.getOWLObjectIntersectionOf(desiredAxioms);							
					}
					else {
						intersection = factory.getOWLObjectIntersectionOf(objectSomeValuesFromBasisOfStrength,
								objectSomeValuesFromPreciseIngredient);
					}


					OWLObjectSomeValuesFrom objectSomeValuesFromRoleGroupContent = factory.getOWLObjectSomeValuesFrom(this.roleGroup, intersection);
					//				roleGroups.add(objectSomeValuesFromRoleGroupContent);
					oces.add(objectSomeValuesFromRoleGroupContent);

				}
				
				if( applyDefinition && scd.getUnitOfPresentationCode() != null ) {
					OWLClass unitOfPresentationClass = factory.getOWLClass(namespace, String.valueOf(scd.getUnitOfPresentationCode()));
					OWLObjectSomeValuesFrom objectSomeValuesFromUnitOfPresentation = factory.getOWLObjectSomeValuesFrom(hasUnitOfPresentation, unitOfPresentationClass);
					oces.add(objectSomeValuesFromUnitOfPresentation);
				}

				//Count of Base of Active Ingredient - again, we need to assume this integer class already exists
				//There do not seem to be any extreme cases at the moment.
				try {
					Double size = new Double(0);
					if( scd.getBaseCuiCount() == 0 ) {
						//do nothing
					}
					else { 
						size = Double.valueOf(scd.getBaseCuiCount());
						OWLClass integerClass = getOWLClassForNumberString(String.valueOf(size), snomedNumberQualifier);
						OWLObjectSomeValuesFrom objectSomeValuesFromCountOfBaseFromActiveIngredient = factory.getOWLObjectSomeValuesFrom(this.countOfBaseOfActiveIngredient, integerClass);
						oces.add(objectSomeValuesFromCountOfBaseFromActiveIngredient);
					}
				}
				catch(Exception e) {
					log("\t\tthere is no whole number for the boss size of cui " + scd.getCui() + "\t" + scd.getName());
					addAnnotation(scdClass, this.countBaseDifferent, "no number class for");
				}

				if( applyDefinition ) {
					if( ++scdCount % 100 == 0 ) {
						log(" * " + scdCount + " equivalent classes asserted..");
					}
					log("\t\tdefinition SUCCESS fully applied");
					OWLObjectIntersectionOf intersection = factory.getOWLObjectIntersectionOf(oces);
					OWLEquivalentClassesAxiom axiom = factory.getOWLEquivalentClassesAxiom(scdClass, intersection);	
					axioms.add(axiom);
			
				}
				else {
					log("\t\tdefinition FAIL s to apply");
					noSnomedDefinitionTemplate.add(scd);				
				}
			}			
		}
		
		return axioms;
		
	}

	public Set<OWLClass> createSBDClasses(RxNormSCD scd) {
		Set<OWLClass> sbdClasses = new HashSet<>();
		
		for(RxNormSBD sbd : scd.getBrandDrugs()) {
			OWLClass sbdClass = factory.getOWLClass(namespace, "Rx" + sbd.getRxcui().toString());
			OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(sbd.getName()));
			OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(sbdClass.getIRI(), labelForClass);
			
			man.addAxiom(ontology, classLabel);
			
			addAnnotation(sbdClass, this.hasRxCUI, sbd.getRxcui().toString());
			sbdClasses.add(sbdClass);
		}
		
		return sbdClasses;		
	}
	
	public OWLClass getOWLClassForString(String value, HashMap<OWLClass, String> map) {
		OWLClass clz = null;
		
		for( OWLClass c : map.keySet() ) {
			String stringToMatch = getRDFSLabel(c);
			if( stringToMatch.equals(value) ) {
				clz = c;
				break;
			}
		}
		
		return clz;		
		
	}
	
	public OWLClass getOWLClassForDouble(Double value, HashMap<Double, OWLClass> map) {
		OWLClass clz = null;
		
		if( value % 1 == 0 ) {
			Long i = Math.round(value);
			value = Double.valueOf(i);
		}
				
		if( map.containsKey(value) ) {
			clz = map.get(value);
		}
		else {
			log("Couldn't find the class for " + value);
		}
		
		return clz;
	}
	
	
	public OWLClass getOWLClassForNumberString(String value, HashMap<String, OWLClass> map) {
		OWLClass clz = null;
		if( value == null ) return clz;
		
		Double num = Double.valueOf(value);
		
		if( num % 1 == 0 ) {
			Long i = Math.round(num);
			num = Double.valueOf(i);
		}
		
		value = String.valueOf(num);
				
		
		for(String s : map.keySet() ) {
			if( Double.valueOf(s) == num ) {
				clz = map.get(s);
				break;
			}
		}
		if( clz == null ) {
			clz = createNumberClass(num);
		}
		
//		if(clz == null ) {
//			System.out.println("debug");
//		}
		
		return clz;

	}	
	
	public OWLClass createRxRoot(int code, String label) {
		OWLClass rxRoot = factory.getOWLClass(namespace, "RxRoot" + code);
		OWLClass thing = factory.getOWLClass(OWLRDFVocabulary.OWL_THING.toString());
		OWLSubClassOfAxiom subClassAxiom =factory.getOWLSubClassOfAxiom(rxRoot, thing);
		
		OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label));
		OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(rxRoot.getIRI(), labelForClass);
		
		man.addAxiom(ontology, classLabel);
		man.addAxiom(ontology, subClassAxiom);	
		
		return rxRoot;
	}
	
	
	public void createDoseFormChild(Integer code, RxNormDoseForm df, OWLClass parent) {
		OWLClass doseFormClass = factory.getOWLClass(namespace, "Rx" + code.toString());
		OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(df.getName()));
		OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(doseFormClass.getIRI(), labelForClass);
		OWLSubClassOfAxiom subClassOfAxiom = factory.getOWLSubClassOfAxiom(doseFormClass, parent);
		man.addAxiom(ontology, classLabel);
		man.addAxiom(ontology, subClassOfAxiom);		
		
	}
	
	//everything is a string, less xsd:??????
	public void addAnnotation(OWLClass c, OWLAnnotationProperty a, String b) {
		OWLAnnotation anno = factory.getOWLAnnotation(a, factory.getOWLLiteral(b));
		OWLAxiom annoToClass = factory.getOWLAnnotationAssertionAxiom(c.getIRI(), anno);
		man.addAxiom(ontology, annoToClass);		
	}
	
	public void createIngredientChild(Integer code, RxNormIngredient ingredient, OWLClass substanceParent, OWLClass fakeIngredientParent) {
		String printableRxCode = code.toString();
		String rxCode = "Rx" + code.toString();
		Set<String> codesToEquate = new HashSet<String>();
		
//		if(scd.getCui().toString().equals("866103")) {
//			System.out.println("halt - check paliperidone gets mapped");
//		}
//		
//		if(scd.getCui().toString().equals("2393795")) {
//			System.out.println("halt - check wrong substance mapping");
//		}
//		if(scd.getCui().toString().equals("9789")) {
//			System.out.println("halt - check silver nitrate as wrong substance mapping to AgN (in water)");
//		}		
		
//		if(rxCode.equals("Rx866103") || rxCode.equals("Rx2393795") || rxCode.equals("Rx9789") ) {
//			System.out.println("halt - checking missing/wrong: " + ingredient.getName());
//		}
		
		Vector<Long> snomedIngredients = ingredient.getSnomedCodes();
		
		for(Long sctid : snomedIngredients) {
			codesToEquate.add(String.valueOf(sctid));
		}
				
//		if(this.jnMissingSubstanceMappings.containsKey(rxCode)) {
//			ArrayList<String> codesToAdd = jnMissingSubstanceMappings.get(rxCode);
//			codesToEquate.addAll(codesToAdd);
//		}
		
		if(this.jnWrongSubstanceMappings.containsKey(rxCode)) {
			ArrayList<String> codesToRemove = jnWrongSubstanceMappings.get(rxCode);
			codesToEquate.removeAll(codesToRemove);
		}
		
		OWLClass ingredientClass = factory.getOWLClass(namespace, rxCode);
		String ingName = ingredient.getName();
		
		if(ingName != null) {
			
			if(ingredient.getIsAllergenic()) {
				addAnnotation(ingredientClass, this.allergenic, true);
				log("Adding allergenic annotation: " + ingredient.getName() + " : " + ingredient.getRxcui());
			}
			
			for(String snomedCode : codesToEquate) {
				OWLClass snomedClass = factory.getOWLClass(namespace, snomedCode);				
				String snomedLabel = getRDFSLabel(snomedClass);				
				if( snomedLabel != null && snomedLabel.contains(" (substance)")) {
					OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ingredientClass, snomedClass);
					man.addAxiom(ontology, eq);
					ingPw.println(snomedCode + "|" + snomedLabel + "|" + printableRxCode + "|" + ingName );
					if( snomedClass != null && ingredientClass != null) addMapsAnnotation(ingredientClass, snomedClass);
					if( ingredient.getIsAllergenic() ) addAnnotation(snomedClass, this.allergenic, true);
				}				
			}
			
			ingPw.flush();
			
			OWLClass parentClass = null;
			if(codesToEquate.size() == 0) {
				ingName = ingName.concat(" (Fake)");
				parentClass = fakeIngredientParent;
				addAnnotation(ingredientClass, this.substanceNoExist, "substance does not map to SCT");
				addAnnotation(ingredientClass, this.asserted, false);				
			} else {
				parentClass = substanceParent;
			}
			
			OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(ingName));
			OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(ingredientClass.getIRI(), labelForClass);			
			
			OWLSubClassOfAxiom ingredientSubClassAxiom = factory.getOWLSubClassOfAxiom(ingredientClass, parentClass);				
			man.addAxiom(ontology, classLabel);
			man.addAxiom(ontology, ingredientSubClassAxiom);			
		}
		
	}
	
	public boolean ingredientInSnomed(Vector<Long> snomedIngCodes) {
		for(Long id : snomedIngCodes) {
			String label = getRDFSLabelForLong(id);
			if(label != null && label.contains(" (substance)")) return true;
		}
		return false;
	}
	
	public void createSCDChild(Integer code, RxNormSCD scd, OWLClass scdParent) {
		OWLClass scdClass = factory.getOWLClass(namespace, "Rx" + code.toString());
		OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(scd.getName()));
		OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(scdClass.getIRI(), labelForClass);
		addAnnotation(scdClass, this.hasRxCUI, code.toString());
		
		OWLSubClassOfAxiom scdSubClassAxiom = factory.getOWLSubClassOfAxiom(scdClass, scdParent);				
		man.addAxiom(ontology, classLabel);
		man.addAxiom(ontology, scdSubClassAxiom);
	}
	
	public String getObjectName(OWLObjectPropertyExpression a) {
		String name = null;
		for( OWLObjectProperty op : a.objectPropertiesInSignature().collect(Collectors.toSet()) ) {
			name = getRDFSLabel(op);
		}
		return name;
	}
	
	public OSType getOperatingSystemType() {
		if (detectedOS == null) {
		  String tOS = System.getProperty("os.name", "generic").toLowerCase();
		  if ((tOS.indexOf("mac") >= 0) || (tOS.indexOf("darwin") >= 0)) {
		    detectedOS = OSType.MacOS;
		  } else if (tOS.indexOf("win") >= 0) {
		    detectedOS = OSType.Windows;
		  } else if (tOS.indexOf("nux") >= 0) {
		    detectedOS = OSType.Linux;
		  } else {
		    detectedOS = OSType.Other;
		    System.err.println("Unsupported OS detected: " + tOS);
		  }
		}
		return detectedOS;
	}		
	
	public File saveOntology() {
		File file = null;
		String fileString = null;
		try {
			Double prod = this.seed * 1000;
			fileString = "./RxNorm2Snomed_With_Fake_Snomed" + String.valueOf(prod) + ".owl";
			file = new File (fileString);
			man.saveOntology(ontology, IRI.create(file));
			System.out.println("Saving file as " + fileString);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return file;
	}
	
	public String generateOutputFilename(String inputFile){
		return inputFile.replace(".owl", "_DRAFT.owl");
	}
	
	private boolean isRx(OWLClass c) {
		return c.getIRI().getIRIString().contains("Rx");
	}
	
	public Set<String> getRxCodes(String code) {
		Set<String> codes = new HashSet<>();
		JSONObject allSnomedCodes = null;
		String snomedCuiString = code;
		try {
			allSnomedCodes = getresult(restUrl.getRestUrl() + "/REST/rxcui.json?idtype=SNOMEDCT&id=" + snomedCuiString, null);			
		}
		catch(Exception e) {
			System.out.println("Unable to fetch rx codes for snomed cui: " + snomedCuiString);
		}
		
		if( !allSnomedCodes.isNull("idGroup") ) {
			JSONObject propConceptGroup = (JSONObject) allSnomedCodes.get("idGroup");
			if( !propConceptGroup.isNull("rxnormId") ) {
				JSONArray rxnormIds = (JSONArray) propConceptGroup.get("rxnormId");
					for( int i=0; i < rxnormIds.length(); i++ ) {
						String rxString = rxnormIds.get(i).toString();
						codes.add(rxString);
					}
			}
		}
		
		return codes;
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
	
	public String getRDFSLabelForLong(Long code) {
		OWLClass cls = factory.getOWLClass(namespace, String.valueOf(code));
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
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
	
	
	private void addGcis() {
		OWLObjectProperty propertyHasManufacturedDoseForm=factory.getOWLObjectProperty("411116001",namespace);
		OWLClass phar=factory.getOWLClass("http://snomed.info/id/736542009");

		Set<OWLAxiom> axios= new HashSet<OWLAxiom>();
		
		Consts.doseFormGcis.forEach(x -> {

			System.out.println("RxNorm: "+ x.getRxN() +" Label: "+ x.getRxNLabel() +" SNOMED: "+ x.getSNO());
			OWLAnnotation commentAnno = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(x.getRxNLabel()));

			OWLClass RxNormClassDf=factory.getOWLClass(namespace, x.getRxN());
			OWLAxiom ax = factory.getOWLAnnotationAssertionAxiom(RxNormClassDf.getIRI(), commentAnno);
			/**
			 * add RxNorm Dose forms with labels
			 */
			axios.add(ax);
			OWLSubClassOfAxiom sub=factory.getOWLSubClassOfAxiom(RxNormClassDf, phar);
			/**
			 * add subclass relation for RxNorm Dose Forms
			 */
			axios.add(sub);
			OWLClass SNCTClassDf=factory.getOWLClass(namespace, x.getSNO());

			if(RxNormClassDf.getIRI().getShortForm().equals("Rx1649574")) {
				OWLClassExpression exalpha=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNCTClassDf);
				OWLClassExpression ex =factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, RxNormClassDf);
				//System.out.println("exalpha "+exalpha);
				OWLObjectProperty hasUnitOfPresentation = factory.getOWLObjectProperty(namespace, "763032000");
				OWLClass vial = factory.getOWLClass(namespace,"732996003");

				OWLClassExpression exbetha=factory.getOWLObjectSomeValuesFrom(hasUnitOfPresentation, vial);
				//System.out.println("exbetha "+exbetha);
				OWLObjectIntersectionOf ex2= factory.getOWLObjectIntersectionOf(exbetha,exalpha);
				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
				System.out.println("eq "+eq);
				/**
				 * alternate definition for RxNorm dose forms
				 */
				axios.add(eq);
			}
			else {
				OWLClassExpression ex=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNCTClassDf);
				OWLClassExpression ex2=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, RxNormClassDf);
				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
				/**
				 * alternate definition for RxNorm dose forms
				 */
				axios.add(eq);
			}
		});
		
		Stream<OWLAxiom> resul=axios.stream();

		man.addAxioms(ontology, resul);
		reasoner.flush();		
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
	
	private void log(String s) {
		if( debug ) {
			pw.println(s);
			pw.flush();
		}
	}
	
	private void log(boolean b) {
		if( !b ) {
			pw.println("\t\t\tUnable to write full definition because of this.");
			pw.flush();
		}
	}	
}