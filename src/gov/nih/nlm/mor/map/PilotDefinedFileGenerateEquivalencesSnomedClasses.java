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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import javax.net.ssl.HttpsURLConnection;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataProperty;
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
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
//import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
//import org.snomed.otf.owltoolkit.ontology.render.SnomedPrefixManager;

import gov.nih.nlm.mor.RxNorm.NDC;
import gov.nih.nlm.mor.RxNorm.Property;
import gov.nih.nlm.mor.RxNorm.RxNormBoss;
import gov.nih.nlm.mor.RxNorm.RxNormDoseForm;
import gov.nih.nlm.mor.RxNorm.RxNormIngredient;
import gov.nih.nlm.mor.RxNorm.RxNormSBD;
import gov.nih.nlm.mor.RxNorm.RxNormSCD;
import gov.nih.nlm.mor.Snomed.SnomedDFPair;
//import gov.nih.nlm.mor.Snomed.SnomedDefinitionTemplate;
//import gov.nih.nlm.mor.Snomed.SnomedManufacturedDoseForm;
//import gov.nih.nlm.mor.Snomed.SnomedUnitOfPresentation;
import gov.nih.nlm.mor.util.Constants;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.FetchRxNormDataAllStatus;
import gov.nih.nlm.mor.util.RestUrl;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;

// RW (July 2021 Release): Update to concrete domains (numbers no longer represented as classes)

public class PilotDefinedFileGenerateEquivalencesSnomedClasses implements java.io.Serializable {

	private static final long serialVersionUID = 3324801368865331845L;
	Constants Consts = new Constants();
	OWLOntologyManager man = null;
	OWLOntology ontology = null;
	OWLOntology ontologyForPilot = null;
	OWLReasoner reasoner = null;
	OWLReasonerFactory reasonerFactory = null;
	OWLDataFactory factory = null;	
	int codeGenerator;
	double snomedCodeGenerator;
	DecimalFormat df = new DecimalFormat("#");	
	final String rxNamespace = new String("http://mor.nlm.nih.gov/RXNORM/");
	final String snomedNamespace = new String("http://snomed.info/id/"); //TODO: make this configurable
	final String oboInOwlNamespace = new String("http://www.geneontology.org/formats/oboInOwl#");
	String outputFilename = null;
	Set<OWLClass> classesInOntology = null;
	Set<OWLClass> snomedMPClasses = null;
	public OWLClass medicinalProduct = null;
	public OWLClass numberClass = null;
	public OWLClass rxNumberRoot = null;
	public OWLObjectProperty hasActiveIngredient = null;
	public OWLObjectProperty roleGroup = null;
	public OWLDataProperty countOfBaseOfActiveIngredient = null;
	public OWLObjectProperty hasPreciseActiveIngredient = null;
	public OWLObjectProperty hasManufacturedDoseForm = null;
	public OWLObjectProperty hasBasisOfStrengthSubstance = null;
	public OWLObjectProperty hasPresentationStrengthNumeratorUnit = null;
	public OWLDataProperty hasPresentationStrengthNumeratorValue = null;
	public OWLObjectProperty hasPresentationStrengthDenominatorUnit = null;
	public OWLDataProperty hasPresentationStrengthDenominatorValue = null;
	public OWLObjectProperty hasConcentrationStrengthNumeratorUnit = null;
	public OWLDataProperty hasConcentrationStrengthNumeratorValue = null;
	public OWLObjectProperty hasConcentrationStrengthDenomniatorUnit = null;
	public OWLDataProperty hasConcentrationStrengthDenominatorValue = null;
	public OWLObjectProperty hasUnitOfPresentation = null;
	public OWLAnnotationProperty mapsToCode = null;
	public OWLAnnotationProperty mapsToName = null;
	public OWLAnnotationProperty inferred = null;
	public OWLAnnotationProperty asserted = null;
	public OWLAnnotationProperty hasNDC = null;
//	public OWLAnnotationProperty hasRxCUI = null;
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
	public OWLAnnotationProperty status = null;
	public OWLAnnotationProperty ndc = null; //AnnotationAssertion(Annotation(<https://mor.nlm.nih.gov/EndDate> "202212"^^xsd:integer) Annotation(<https://mor.nlm.nih.gov/StartDate> "200706"^^xsd:integer) <https://mor.nlm.nih.gov/NDC> :Rx308048 "00228202910"^^xsd:integer)
	public OWLAnnotationProperty startDate = null;
	public OWLAnnotationProperty endDate = null;
	public OWLAnnotationProperty hasDbXref = null;
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
	public TreeMap<String, ArrayList<RxNormSBD>> scd2SBDs = new TreeMap<String, ArrayList<RxNormSBD>>();
	public TreeMap<String, RxNormSBD> cui2SBDs = new TreeMap<String, RxNormSBD>();
	public Set<OWLClass> allSBDs = new HashSet<>();
	public TreeMap<OWLClass, ArrayList<OWLClass>> ingEquivalencyMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	public TreeMap<OWLClass, ArrayList<OWLClass>> unitEquivalencyMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	public TreeMap<OWLClass, ArrayList<OWLClass>> numberEquivalencyMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	public TreeMap<String, OWLAnnotationProperty> annotationAttributeClasses = new TreeMap<String, OWLAnnotationProperty>();
	public TreeMap<String, OWLAnnotationProperty> annotationCodeClasses = new TreeMap<String, OWLAnnotationProperty>();
	public TreeMap<String, OWLAnnotationProperty> annotationNameClasses = new TreeMap<String, OWLAnnotationProperty>();
	public TreeMap<String, OWLAnnotationProperty> annotationSourceClasses = new TreeMap<String, OWLAnnotationProperty>();
	
	//structures from JNs work on substance mappings
	public TreeMap<String, ArrayList<String>> jnMissingSubstanceMappings = new TreeMap<String, ArrayList<String>>();
	public TreeMap<String, ArrayList<String>> jnWrongSubstanceMappings = new TreeMap<String, ArrayList<String>>();
	
//	public Vector<String> seen = new Vector<String>();
	public Vector<OWLPropertyExpression> qaChecks = new Vector<OWLPropertyExpression>();
	public Vector<OWLObjectProperty> qaUnitChecks = new Vector<OWLObjectProperty>();
	public Vector<OWLDataProperty> qaValueChecks = new Vector<OWLDataProperty>();
	public Set<OWLClass> assertedClasses = new HashSet<>();
	public Set<OWLClass> inferredClasses = new HashSet<>();
	private boolean debug = false;
	private PrintWriter pw = null;
	Set<EquivalentMapping> mappings = new HashSet<>();
	private Double seed = Math.random();
	public RestUrl restUrl = new RestUrl();

	public String dir = System.getProperty("user.dir").replace("\\", "/").replace(" ", "%20");	
	public enum OSType {
		Windows, MacOS, Linux, Other
	};
	public OSType detectedOS = null;
	OSType os = getOperatingSystemType();
	private OWLAnnotationProperty isVaccine;	
	private ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	
	@SuppressWarnings("deprecation")
	public PilotDefinedFileGenerateEquivalencesSnomedClasses(String filename, FetchRxNormDataAllStatus fetchRxNorm, boolean addGcis, boolean debug) throws OWLOntologyStorageException {
		
		this.debug = debug;
		
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
	
		rxdf2SnomedDFPair = fetchRxNorm.getRxdf2SnomedDFPair();
		
		String version = fetchRxNorm.getRxNormVersion();

		try {
			man = OWLManager.createOWLOntologyManager();			
			ontology = man.loadOntologyFromOntologyDocument(new File(filename));
			
			IRI versionIRI = IRI.create(rxNamespace + version);
			
			ontologyForPilot = man.createOntology(IRI.create(rxNamespace));
			
			SetOntologyID change = new SetOntologyID(ontologyForPilot, versionIRI);
			
			ontologyForPilot.getOWLOntologyManager().applyChange(change);			
		} catch (OWLOntologyCreationException e1 ) {
			System.out.println("Error configuring ontology from OWLManager.");
			e1.printStackTrace();
		}
		
		jnMissingSubstanceMappings = mapFromFile("./config/missing-substance-mappings.txt");
		jnWrongSubstanceMappings = mapFromFile("./config/wrong-substance-mappings.txt");		
		
		log("*** Discovering inferences for ontology " + filename + " ***");

		reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ontology);
		factory = man.getOWLDataFactory();
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);		

		log("Finished loading ont...");
		log("Creating reasoner...");

		outputFilename = generateOutputFilename(filename);

		//generate dose form map - comment out if not needed or..
		//TODO: Configure if a dose form pair mapping should be ouput
//		FetchSnomedDoseForms snomedDoseForms = new FetchSnomedDoseForms(man, ontology, factory, reasoner, fetchRxNorm);
//		snomedDoseForms.examineForPairs();
		
//		log("STEP 0");
//		if(addGcis) {
//			addGcis();
//		}

		log("STEP 1");		
		
		setConstants();
		setAnnotationConstants(fetchRxNorm.getAttributesList(), fetchRxNorm.getCodesList(), fetchRxNorm.getNamesList(), fetchRxNorm.getSourcesList());
		createAnnotations();		
		
		
		codeGenerator = 0;
		snomedCodeGenerator = 1000000000;  //this is a moving target. snct codes are very large
	
		OWLClass snomedMpParent = factory.getOWLClass(snomedNamespace, "763158003");
		OWLClass snomedSubstance = factory.getOWLClass(snomedNamespace, "105590001");
		OWLClass snomedUnit = factory.getOWLClass(snomedNamespace, "258681007");
//		OWLClass snomedNumber = factory.getOWLClass(namespace, "260299005");
		
		OWLClass fakeCdParent = factory.getOWLClass(snomedNamespace, df.format(++snomedCodeGenerator) + "-FS");
		OWLClass fakeCdTranslatedParent = factory.getOWLClass(snomedNamespace, df.format(++snomedCodeGenerator) + "-FS");
		OWLClass fakeIngredientParent = factory.getOWLClass(snomedNamespace, df.format(++snomedCodeGenerator) + "-FS");
		
		addFakeParent(fakeCdTranslatedParent, snomedMpParent, "Translated CDs");
		addFakeParent(fakeCdParent, snomedMpParent, "Untranslated CDs");
		addFakeParent(fakeIngredientParent, snomedSubstance, "Untranslated Substances");
	
	
		// Create the basic concepts we already know about in RxNorm,
		// categorized under the roots created above.
		for(Integer cui : fetchRxNorm.getCui2IN().keySet()) {			
			RxNormIngredient ingredient = fetchRxNorm.getCui2IN().get(cui);
			createIngredientChild(cui, ingredient, snomedSubstance, fakeIngredientParent);
		}
		
		for( Integer cui : fetchRxNorm.getCui2PIN().keySet() ) {
			RxNormIngredient ingredient = fetchRxNorm.getCui2PIN().get(cui);
// debug checkpoint			
//			if(ingredient.getRxcui() == 1040027 ) {
//				System.out.println("halt");
//			}
			createIngredientChild(cui, ingredient, snomedSubstance, fakeIngredientParent);
		}
		
		//add missing substance mappings
		addMissingSubstanceMappings();
		
		createIngredientChild(Integer.valueOf(0), new RxNormIngredient(0, "not specified in rxnorm"), snomedSubstance, fakeIngredientParent);		
		
		reasoner.flush();
		
		log("STEP 2");		
		
		for( String cui : fetchRxNorm.getCui2SCD().keySet() ) {
			RxNormSCD rxSCD = fetchRxNorm.getCui2SCD().get(cui);
			
			if(rxSCD.getStatus().equalsIgnoreCase("active")) {	
			
				//Set the manufacturedDoseForm and unitOfPresentation to the snomed classes becuase
				//we want to hardwire these to the definition.  We tried doing this based on pair popularity
				//from the RxNorm DF to Snomed pairs, however the issue is that multiple pairs could map to a
				//single RxDF.  This would necessitate building classes with an anding of the snomed df and up,
				//though, this would break the equivalences the reasoner is supposed to make
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
						//I haven't seen any cases like this so far, though just to be safe
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

		}	
	
		//Add Unit Equivalences
		log("*** Adding predetermined units class equivalencies ****");
		Set<OWLAxiom> unitsOfMeasureAxioms = generateUnitsOfMeasureAxioms(unitsOfMeasure, snomedUnit);
		for(OWLAxiom axiom : unitsOfMeasureAxioms) {
			man.addAxiom(ontology, axiom);
			man.addAxiom(ontologyForPilot, axiom);
		}
		
		reasoner.flush();
		

		log("STEP 3");	
		
		for( OWLClass cls : reasoner.getSubClasses(snomedSubstance, false).entities().collect((Collectors.toSet())) ) {
//			System.out.println(cls.getIRI());		
			reasoner.getEquivalentClasses(cls).forEach(c -> {
				if( !c.getIRI().getIRIString().replace(snomedNamespace, "").replace(rxNamespace, "").equals(cls.getIRI().getIRIString().replace(snomedNamespace, "").replace(rxNamespace, "")) ) {
					addToMap(cls, c, ingEquivalencyMap);
				}
			});
		}
		
		for( OWLClass cls : reasoner.getSubClasses(snomedUnit, false).entities().collect((Collectors.toSet())) ) {
			reasoner.getEquivalentClasses(cls).forEach(c -> {
//				if(!getCodeForClass(c).equals(getCodeForClass(cls) )) {
				if(!c.getIRI().equals(cls.getIRI())) {				
					addToMap(c, cls, unitEquivalencyMap);
				}
			});
		}		

		log("STEP 4");

		System.out.println("*** Generating RxNorm clinical drug classes with SCT classes where available ****");
		log("*** Generating RxNorm clinical drug classes with SCT classes where available ****");
		Set<OWLAxiom> rxClinicalDrugAxioms = generateClinicalDrugAxioms(fetchRxNorm.getCui2SCD(), fakeCdParent);
		man.addAxioms(ontology, rxClinicalDrugAxioms.stream());
		man.addAxioms(ontologyForPilot, rxClinicalDrugAxioms.stream());
		
		reasoner.flush();
		
		reasoner.getSubClasses(this.medicinalProduct, false).entities().filter(x -> !isRx(x) && !x.getIRI().equals(medicinalProduct.getIRI()) && !x.equals(factory.getOWLNothing()))
			.forEach(y -> {
				reasoner.getEquivalentClasses(y).entities().filter(z -> isRx(z) && !z.getIRI().equals(y.getIRI())).forEach(a -> {
//					addMapsAnnotation(y, a);
					addAnnotation(y, this.inferred, true);
					//inferredClasses.add(y);
					addToMap(a, y, cdEquivalencyMapSnomed);					
				});
			});
		
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
			if( !cls.getIRI().getNamespace().equals(rxNamespace) ) {			
				eqSnomedClasses = reasoner.getEquivalentClasses(cls).entities().collect(Collectors.toSet());
				if( eqSnomedClasses != null && eqSnomedClasses.size() > 1) {
					for( OWLClass c : eqSnomedClasses ) {
						if( !c.getIRI().equals(cls.getIRI()) && c.getIRI().getNamespace().equals(rxNamespace) ) {						
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

		for( OWLClass c : ingEquivalencyMap.keySet()) {
			ArrayList<OWLClass> list = ingEquivalencyMap.get(c);
			log(c.getIRI().getIRIString().replace(snomedNamespace, "").replace(rxNamespace, "") + "\t" + getRDFSLabel(c));
			for( OWLClass clz : list ) {
				log("\t" + clz.getIRI().getIRIString().replace(rxNamespace, "").replace(snomedNamespace, "" + "\t") + "\t" + getRDFSLabel(clz));
			}
		}
		
		//debugging
//		saveOntology();
		
		System.out.println("********** Saving Defined OWL **********\n");
		log("********** Saving Defined OWL **********\n");		
		
		savePilotOntology();
	
		reasoner.dispose();		
		if( debug ) pw.close();
		System.out.println();
	}
	
	
	private void addMissingSubstanceMappings() {
		for( String key : jnMissingSubstanceMappings.keySet() ) {
			ArrayList<String> substances = jnMissingSubstanceMappings.get(key);
			for(String substance : substances ) {
				OWLClass rxClass = factory.getOWLClass(rxNamespace, key);
				OWLClass snomedClass = factory.getOWLClass(snomedNamespace, substance);
				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(rxClass, snomedClass);
				man.addAxiom(ontology, eq);	
				man.addAxiom(ontologyForPilot, eq);
			}
		}		
	}
	
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
		return c.getIRI().getIRIString();
	}
	
	private void createAnnotation(OWLAnnotationProperty ap, String s) {
		OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(s));
		OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(ap.getIRI(), label);
		man.addAxiom(ontology, labelToClass);	
		man.addAxiom(ontologyForPilot, labelToClass);
	}
	
	private void createAnnotations() {
		log("Creating annotation declarations for informational purposes.");
		
		createAnnotation(this.mapsToCode, "maps to rxnorm code");
		createAnnotation(this.mapsToName, "maps to rxnorm name");
		createAnnotation(this.asserted, "asserted equivalence");
		createAnnotation(this.inferred, "inferred equivalence");
		createAnnotation(this.substanceNoExist, "substance doesn't exist");
		createAnnotation(this.substanceDifferent, "substance is different");
		createAnnotation(this.hasNDC, "has NDC");
//		createAnnotation(this.hasRxCUI, "has RxCUI");
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
		man.addAxiom(ontologyForPilot, annoToClass);
	}	

	public void addFakeParent(OWLClass fakeParent, OWLClass snomedParent, String labelForFake) {
		OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(labelForFake));
		OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(fakeParent.getIRI(), label);
		OWLSubClassOfAxiom subClassAxiom =factory.getOWLSubClassOfAxiom(fakeParent, snomedParent);
		
		log("Adding fake parent");
		man.addAxiom(ontology, labelToClass);
		man.addAxiom(ontology, subClassAxiom);
		man.addAxiom(ontologyForPilot, labelToClass);
		man.addAxiom(ontologyForPilot, subClassAxiom);		
		reasoner.flush();
	}
	
	private void treeCdFakes(OWLClass mp, OWLClass untranslatedParent, OWLClass translatedParent) {
		
		reasoner.getSubClasses(mp, false).entities().filter(w -> !w.equals(mp) && !w.equals(untranslatedParent) && !w.equals(translatedParent) && !w.equals(factory.getOWLNothing()))
//				.filter(x -> (getCodeForClass(x).contains("Rx") && !allSBDs.contains(x)))
				.filter(x -> (x.getIRI().getNamespace().equals(rxNamespace) && !allSBDs.contains(x)))		
				.filter(y -> !EntitySearcher.isDefined(y, ontology) )
				.forEach(z -> {
					retree(z, untranslatedParent);
				});
//				.collect(Collectors.toSet());

		reasoner.getSubClasses(mp, false).entities().filter(w -> !w.equals(mp) && !w.equals(untranslatedParent) && !w.equals(translatedParent) && !w.equals(factory.getOWLNothing()))
//				.filter(x -> getCodeForClass(x).contains("Rx"))
				.filter(x -> x.getIRI().getNamespace().equals(rxNamespace))		
				.filter(y -> EntitySearcher.isDefined(y, ontology) )
				.forEach(z -> {
					retree(z, translatedParent);
				});
//				.collect(Collectors.toSet());		
		
//		for(OWLClass c : rxUntranslatedToRetree) {
//			addAnnotation(c, this.asserted, true);
//			retree(c, untranslatedParent);
//		}
//		
//		for(OWLClass c: rxTranslatedToTree) {
//			addAnnotation(c, this.asserted, true);
//			addAnnotation(c, this.inferred, true);
		
//			retree(c, translatedParent);			
//		}

//		log("STEP A");
//		OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(this.ontology));
//		
//		reasoner.flush();
//		
//		Set<OWLClass> mpSubClasses = reasoner.getSubClasses(mp, false).entities().collect((Collectors.toSet()));
//		Set<OWLClass> equivalentClasses = cdEqMap.keySet();
//		for( OWLClass mSc : mpSubClasses ) {
//			ArrayList<OWLClass> values = cdEqMap.get(mSc);
//			if(values == null || values.isEmpty()) {
//				if(getCodeForClass(mSc).contains("Rx"))	retree(mSc, fakeParent);
//			}
//			
//		}
//		
//
//		for( OWLClass c : cdEqMap.keySet() ) {
//			if( !getCodeForClass(c).contains("Rx") ) {
//				ArrayList<OWLClass> list = cdEqMap.get(c);
//				for( OWLClass clz : list ) {
//					if( getCodeForClass(clz).contains("Rx")) {
//						log("Adding inferred and asserted axioms for the SCT equivalent CD " + getCodeForClass(c));
//						addAnnotation(c, this.asserted, true);
//						addAnnotation(c, this.inferred, true);
//						addAnnotation(c, this.mapsToCode, clz.getIRI().getIRIString().replace(namespace + "Rx", ""));
//						addAnnotation(c, this.mapsToName, getRDFSLabel(clz));
//						assertedClasses.add(c);
//						inferredClasses.add(c);
////						log("Removing the equivalent RxNorm class " + getCodeForClass(clz));
////						clz.accept(remover);
//					}
//				}
//			}
//		}
//		
//		man.applyChanges(remover.getChanges());
//		
//		log("STEP B");
//		reasoner.flush();
//		
//		log("STEP C");
//		
//		Set<OWLClass> cdBranchClasses = reasoner.getSubClasses(mp, false).entities().collect(Collectors.toSet());
//		cdBranchClasses.remove(factory.getOWLNothing());
//		cdBranchClasses.remove(mp);
//		
//		cdBranchClasses.stream().filter(x -> getCodeForClass(x).contains("Rx")).forEach(y -> retree(y, fakeParent));
//		
//		log("STEP D");
//		
//		reasoner.flush();
//		
//		log("STEP E");
	}
	
	private void retree(OWLClass a, OWLClass destination) {
		Set<OWLAxiom> axiomsForRemoval = new HashSet<>();
		Set<OWLAxiom> subclassAxioms = ontology.axioms(a).filter(y -> y.isOfType(AxiomType.SUBCLASS_OF)).collect(Collectors.toSet());
		for( OWLAxiom axiom : subclassAxioms ) {
			axiomsForRemoval.add(axiom);
		}

		man.removeAxioms(ontology, axiomsForRemoval.stream());	
		
		OWLSubClassOfAxiom newSubClass = factory.getOWLSubClassOfAxiom(a, destination);
		man.addAxiom(ontology, newSubClass);	
		man.addAxiom(ontologyForPilot, newSubClass);
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
		if(!scd.getRxNormDoseForm().isEmpty()) {
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
		else {
			scd.setSnomedDefinitionTemplate(null);
		}
		
	}
	
	
	public void setConstants() {
		this.medicinalProduct = factory.getOWLClass(snomedNamespace, "763158003");
		this.numberClass = factory.getOWLClass(snomedNamespace, "260299005");
		this.hasActiveIngredient = factory.getOWLObjectProperty(snomedNamespace, "127489000");
		this.roleGroup = factory.getOWLObjectProperty(snomedNamespace, "609096000");
		this.countOfBaseOfActiveIngredient = factory.getOWLDataProperty(snomedNamespace, "1142139005");
		this.hasPreciseActiveIngredient = factory.getOWLObjectProperty(snomedNamespace, "762949000");
		this.hasManufacturedDoseForm = factory.getOWLObjectProperty(snomedNamespace, "411116001");
		this.hasBasisOfStrengthSubstance = factory.getOWLObjectProperty(snomedNamespace, "732943007");
		this.hasPresentationStrengthNumeratorUnit = factory.getOWLObjectProperty(snomedNamespace, "732945000");
		this.hasPresentationStrengthNumeratorValue = factory.getOWLDataProperty(snomedNamespace, "1142135004");
		this.hasPresentationStrengthDenominatorUnit = factory.getOWLObjectProperty(snomedNamespace, "732947008");
		this.hasPresentationStrengthDenominatorValue = factory.getOWLDataProperty(snomedNamespace, "1142136003");
		this.hasConcentrationStrengthNumeratorUnit = factory.getOWLObjectProperty(snomedNamespace, "733725009");
		this.hasConcentrationStrengthNumeratorValue = factory.getOWLDataProperty(snomedNamespace, "1142138002");
		this.hasConcentrationStrengthDenomniatorUnit = factory.getOWLObjectProperty(snomedNamespace, "733722007");
		this.hasConcentrationStrengthDenominatorValue = factory.getOWLDataProperty(snomedNamespace, "1142137007");
		this.hasUnitOfPresentation = factory.getOWLObjectProperty(snomedNamespace, "763032000");
		
		this.mapsToCode = factory.getOWLAnnotationProperty(rxNamespace, "MapsToCode");
		this.mapsToName = factory.getOWLAnnotationProperty(rxNamespace, "MapsToName");
		this.inferred = factory.getOWLAnnotationProperty(rxNamespace, "Inferred");
		this.asserted = factory.getOWLAnnotationProperty(rxNamespace, "Asserted");
		this.hasNDC = factory.getOWLAnnotationProperty(rxNamespace, "HasNDC");
		this.hasExplanation = factory.getOWLAnnotationProperty(rxNamespace, "HasExplanation");		
//		this.hasRxCUI = factory.getOWLAnnotationProperty(rxNamespace, "HasCUI");		
		this.substanceNoExist = factory.getOWLAnnotationProperty(rxNamespace, "SubstanceNotExist");
		this.substanceDifferent = factory.getOWLAnnotationProperty(rxNamespace, "SubstanceDifferent");
		this.bossSubstanceDifferent = factory.getOWLAnnotationProperty(rxNamespace, "BossSubstanceDifferent");
		this.activeIngerdientSubstanceDifferent = factory.getOWLAnnotationProperty(rxNamespace, "ActiveIngDifferent");		
		this.doseFormDifferent = factory.getOWLAnnotationProperty(rxNamespace, "DoseFormDifferent");
		this.presUnitDifferent = factory.getOWLAnnotationProperty(rxNamespace, "PresUnitDifferent");
		this.unitsDifferent = factory.getOWLAnnotationProperty(rxNamespace, "UnitsDifferent");		
		this.valuesDifferent = factory.getOWLAnnotationProperty(rxNamespace, "ValuesDifferent");
		this.countBaseDifferent = factory.getOWLAnnotationProperty(rxNamespace, "CountOfBaseDifferent");
		this.vetOnly = factory.getOWLAnnotationProperty(rxNamespace, "VetOnly");
		this.allergenic = factory.getOWLAnnotationProperty(rxNamespace, "Allergenic");
		this.hasAllergenic = factory.getOWLAnnotationProperty(rxNamespace, "HasAllergenic");
		this.isPrescribable = factory.getOWLAnnotationProperty(rxNamespace, "IsPrescribable");
		this.isVaccine = factory.getOWLAnnotationProperty(rxNamespace, "IsVaccine");
		//221222
		this.status = factory.getOWLAnnotationProperty(rxNamespace, "Status");

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
	
	public void setAnnotationConstants(TreeMap<String, String> attributes, TreeMap<String, String> codes, TreeMap<String, String> names, TreeMap<String, String> sources ) {
		OWLAnnotationProperty ap = null;
		for(String a : attributes.keySet() ) {
			ap = factory.getOWLAnnotationProperty(rxNamespace + a);
			annotationAttributeClasses.put(attributes.get(a), ap);
			createAnnotation(ap, a);
		}

		for(String n : names.keySet()) {
			ap = factory.getOWLAnnotationProperty(rxNamespace + n);
			annotationNameClasses.put(names.get(n), ap);
			createAnnotation(ap, n);
		}
		for(String s : sources.keySet()) {
			ap = factory.getOWLAnnotationProperty(rxNamespace + s);
			annotationSourceClasses.put(sources.get(s), ap);
			createAnnotation(ap, s);			
		}

		ap = factory.getOWLAnnotationProperty(oboInOwlNamespace + "hasDbXref");
		hasDbXref = ap;		
		createAnnotation(ap, "xRef");
		ap = factory.getOWLAnnotationProperty(rxNamespace + "endDate");
		createAnnotation(ap, "End Date");
		endDate = ap;
		ap = factory.getOWLAnnotationProperty(rxNamespace + "startDate");
		createAnnotation(ap, "Start Date");
		startDate = ap;
		ap = factory.getOWLAnnotationProperty(rxNamespace + "ndc");
		createAnnotation(ap, "NDC");
		ndc = ap;
		
		OWLSubAnnotationPropertyOfAxiom subStartNdc = factory.getOWLSubAnnotationPropertyOfAxiom(startDate, ndc);
		OWLSubAnnotationPropertyOfAxiom subEndNdc = factory.getOWLSubAnnotationPropertyOfAxiom(endDate, ndc);		
		
		man.addAxiom(ontology, subStartNdc);	
		man.addAxiom(ontologyForPilot, subStartNdc);		
		man.addAxiom(ontology, subEndNdc);	
		man.addAxiom(ontologyForPilot, subEndNdc);	
	}
	
	public void addCodeAnnotationAssertions() {
		OWLAnnotationProperty ap = null;
		
		ap = factory.getOWLAnnotationProperty(oboInOwlNamespace + "hasDbXref");
		hasDbXref = ap;
		createAnnotation(ap, "xRef");
		
		ap = factory.getOWLAnnotationProperty(rxNamespace + "ndc");
		createAnnotation(ap, "NDC");
		ndc = ap;
		
		ap = factory.getOWLAnnotationProperty(rxNamespace + "endDate");
		createAnnotation(ap, "End Date");
		makeSubProperty(ap, ndc);
		endDate = ap;		
		
		ap = factory.getOWLAnnotationProperty(rxNamespace + "startDate");
		createAnnotation(ap, "Start Date");
		makeSubProperty(ap, ndc);
		startDate = ap;		
	}
	
	private void makeSubProperty(OWLAnnotationProperty subProp, OWLAnnotationProperty superProp ) {
		OWLSubAnnotationPropertyOfAxiom subAp = factory.getOWLSubAnnotationPropertyOfAxiom(subProp, superProp);
		man.addAxiom(ontology, subAp);		
	}
	
	public Set<OWLAxiom> generateUnitsOfMeasureAxioms(TreeMap<String, Double> unitMap, OWLClass parent) {
		Set<OWLAxiom> axioms = new HashSet<>();
		for(String unit : unitMap.keySet() ) {
			OWLClass unitClass = factory.getOWLClass(snomedNamespace, df.format(++snomedCodeGenerator) + "-FS");
			OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(unit));
			
			unitClassToName.put(unitClass, unit);
			
			OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(unitClass.getIRI(), label);
			OWLSubClassOfAxiom subClassAxiom =factory.getOWLSubClassOfAxiom(unitClass, parent);
			
			OWLClass snomedEq = null;
			if(unit.equalsIgnoreCase("kg")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258683005");				
			}
			else if(unit.equalsIgnoreCase("g")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258682000");
			}
			else if(unit.equalsIgnoreCase("mg")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258684004");
			}
			else if(unit.equalsIgnoreCase("mcg")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258685003");								
			}
			else if(unit.equalsIgnoreCase("ng")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258686002");						
			}
			else if(unit.equalsIgnoreCase("pg")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258687006");				
			}
			else if(unit.equalsIgnoreCase("ml")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258773002");
			}
			else if(unit.equalsIgnoreCase("l")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258770004");
			}
			else if(unit.equalsIgnoreCase("unt")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "767525000");
			}
			else if(unit.equalsIgnoreCase("hr")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "258702006");
			}
			else if(unit.equalsIgnoreCase("actuat")) {
				snomedEq = factory.getOWLClass(snomedNamespace, "732981002");
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
			cls = factory.getOWLClass(snomedNamespace, df.format(++snomedCodeGenerator) + "-FS");
			OWLAnnotation label = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(valString));
			OWLAxiom labelToClass = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(), label);
			OWLSubClassOfAxiom subClassAxiom = factory.getOWLSubClassOfAxiom(cls, numberClass);
//			System.out.println("Adding number class");
			man.addAxiom(ontology, labelToClass);
			man.addAxiom(ontology, subClassAxiom);
			man.addAxiom(ontologyForPilot, labelToClass);
			man.addAxiom(ontologyForPilot, subClassAxiom);			
			log("creating a number class not found in SCT" + valString);
			addAnnotation(cls, this.valuesDifferent, "no SCT number class");
			snomedNumberQualifier.put(valString, cls);
		}
		else {
			cls = snomedNumberQualifier.get(valString);
		}		
		
//		numberClassToName.put(val, cls);
		
		
		return cls;
	}
	
	public String getRDFSLabel(OWLEntity cls) {
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
	}	
	
	public Set<OWLAxiom> generateClinicalDrugAxioms(TreeMap<String, RxNormSCD> scdMap, OWLClass fakeCdParent) {

		Set<OWLAxiom> axioms = new HashSet<>();
		int scdCount = 0;
		
		log("*** Adding Rx clinical drug classes with SCT classes where applicable ***");
		
		log("Working with " + scdMap.keySet().size() + " SCDs");
		
		for( String i : scdMap.keySet() ) {
			
			boolean applyDefinition = true;
			
			RxNormSCD scd = scdMap.get(i);
			
			if(scd.getStatus().equalsIgnoreCase("active")) {
			
				if(scd.getCui().toString().equals("198039")) {
					System.out.println("Remapped - HALT");
				}
				if(scd.getCui().toString().equals("1000019")) {
					System.out.println("NotCurrent - HALT");
				}
				
				
				//this will slow things down some
				scd.setSnomedCodes();
				
				log("\tRx SCD class " + scd.getName() + " : " + i);
				OWLClass scdClass = factory.getOWLClass(rxNamespace, i);
//				addAnnotation(scdClass, this.hasRxCUI, String.valueOf(scd.getCui()));
				
				String conceptStatus = scd.getStatus();
				addAnnotation(scdClass, this.status, conceptStatus);
				
				
				if( scd.isVetOnly() ) {
					addAnnotation(scdClass, this.vetOnly, true);
				}
				
				for( RxNormIngredient in : scd.getRxNormIngredient() ) {
					if( in.getIsAllergenic() ) {
						addAnnotation(scdClass, this.hasAllergenic, in.getName() + " : " + String.valueOf(in.getRxcui()) );
					}
				}
				
				Set<Property> properties = scd.getProperties();
				applyProperties(scdClass, properties);
			
				Set<NDC> theNdcs = scd.getNdcSet();
				applyNdcs(scdClass, theNdcs);
				
				Collection<OWLClassExpression> oces = new HashSet<OWLClassExpression>();
				oces.add(this.medicinalProduct);
	
				if( scd.hasNDC() ) {
					log("\t\tNDC exists");
	//				this.addAnnotation(scdClass, this.hasNDC, true);
				}
				else {
					log("\t\tNDC DOES NOT exist");
	//				this.addAnnotation(scdClass,  this.hasNDC, false);
				}
	
				if( scd.getIsVaccine() ) {
					log("\t\tSCD IS Vaccine");
	//				this.addAnnotation(scdClass, this.isVaccine , true);
				}
				
				String denominatorUoPID = "";
				String doseFormCui = "-1";
				if(!scd.getRxNormDoseForm().isEmpty()) {
					doseFormCui = scd.getRxNormDoseForm().get(0).getRxcui();
				}
				else {
					applyDefinition = false;
				}
					
				if( scd.getSnomedDefinitionTemplate() == null ) {
	//				log("\t\tNo SCT clinical drug exists with the following RxNorm DF: " + scd.getRxNormDoseForm().get(0).getName());
	//				log(false);
					
					applyDefinition = false;
				}
				
				
				//Manufactured Dose Form assertion on the concept.  There can only be one per SCD.
				//We need to assume this class already exists
				if( applyDefinition && scd.getManufacturedDoseFormCode() != null ) {
					OWLClass manufactureDoseFormClass = factory.getOWLClass(snomedNamespace, String.valueOf(scd.getManufacturedDoseFormCode()));
					OWLObjectSomeValuesFrom objectSomeValuesFromHasManufacturedDoseForm = factory.getOWLObjectSomeValuesFrom(hasManufacturedDoseForm, manufactureDoseFormClass);
					oces.add(objectSomeValuesFromHasManufacturedDoseForm);
				}
				else if( scd.getManufacturedDoseFormCode() == null ){
					log("\t\tUnable to resolve the SCT manufactured dose form code.");
	//				addAnnotation(scdClass, this.doseFormDifferent, "cannot map SCT manufactured dose form");
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
	
						OWLDataHasValue objectSomeValuesFromHasPresentationStrengthNumeratorValue = null;
						OWLObjectSomeValuesFrom objectSomeValuesFromHasPresentationStrengthNumeratorUnit = null;
						OWLDataHasValue objectSomeValuesFromHasPresentationStrengthDenominatorValue = null;
						OWLObjectSomeValuesFrom objectSomeValuesFromHasPresentationStrengthDenominatorUnit = null;
	
						OWLDataHasValue objectSomeValuesFromHasConcentrationStrengthNumeratorValue = null;
						OWLObjectSomeValuesFrom objectSomeValuesFromHasConcentrationStrengthNumeratorUnit = null;
						OWLDataHasValue objectSomeValuesFromHasConcentrationStrengthDenominatorValue = null;
						OWLObjectSomeValuesFrom objectSomeValuesFromHasConcentrationStrengthDenominatorUnit = null;				
	
						OWLClass bossNameClass = null;
						if( boss.getBossRxCui() != null && boss.getBossRxCui() != -1) {
							bossNameClass = factory.getOWLClass(rxNamespace, boss.getBossRxCui().toString());
							if( ingEquivalencyMap.containsKey(bossNameClass)) {
								ArrayList<OWLClass> list = ingEquivalencyMap.get(bossNameClass);
								//TODO: Deciding how to handle the case for many SCT equivalents (there should be none)
								//Except with GCIs created by a postdoc and/or QF miscalculations (at one time a QF value didn't exist as a prefix on a name)
								for(OWLClass c : list ) {
									if( !isRx(c) ) {
										bossNameClass = c;
									}
								}
								if( isRx(bossNameClass) ) {
	//								addAnnotation(scdClass, this.bossSubstanceDifferent, bossNameClass + " not found in SCT");
									log("\t\t\tThe ingredient in this boss cannot be found in SCT " + boss.getBossRxCui() + " : " + boss.getBossName());
									log("\t\t\tA \"Fake\" should already exist.");
								}
							}
						}
						else {
	//						addAnnotation(scdClass, this.bossSubstanceDifferent , "ing not found in RxNorm");
							log("\t\t\tMissing BossRxCui in RxNorm");
							bossNameClass = factory.getOWLClass(rxNamespace, "0");
	//						log(false);
	//						applyDefinition = false;
	//						break;
						}
	
						OWLObjectSomeValuesFrom objectSomeValuesFromBasisOfStrength = factory.getOWLObjectSomeValuesFrom(hasBasisOfStrengthSubstance, bossNameClass);
						
						OWLClass preciseActiveIngredientClass = null;			
						if( boss.getActiveIngredientRxCui() != -1 ) {
							preciseActiveIngredientClass = factory.getOWLClass(rxNamespace, String.valueOf(boss.getActiveIngredientRxCui()));								
							if( ingEquivalencyMap.containsKey(preciseActiveIngredientClass) ) {
								ArrayList<OWLClass> list = ingEquivalencyMap.get(preciseActiveIngredientClass);
								for( OWLClass c : list ) {
									if( !isRx(c) ) {
										preciseActiveIngredientClass = c;
									}
									else {
										log("\t\tA fake IN for this Rx Active Ingredient exists\t" + boss.getActiveIngredientRxCui() + "\t" + boss.getActiveIngredientRxCui());
									}
								}
							}
						}
						else {					
							applyDefinition = false;
							//add annotation to the class the AI doesn't exist
							log("\t\tAn Active Ingredient doesn't exist in BoSS " + boss.getBossName() + " : " + boss.getBossRxCui());
							preciseActiveIngredientClass = factory.getOWLClass(rxNamespace, "0");								
	//						addAnnotation(scdClass, this.activeIngerdientSubstanceDifferent, "missing Rx AI in BoSS " + boss.getBossName() + " : " + boss.getBossRxCui());
	//						addAIAnnotation(scdClass);
	//						System.out.println("Missing Active Inredient cui (not applying definition) in\t" + scd.getCui() + "\t" + scd.getName());
							for( Long snomedCode : scd.getSnomedCodes() ) {
								OWLClass snomedClass = factory.getOWLClass(snomedNamespace, String.valueOf(snomedCode));
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
	
							objectSomeValuesFromHasConcentrationStrengthNumeratorValue = getOwlDataHasValue(this.hasConcentrationStrengthNumeratorValue, boss.getSnomedNumberatorValue());
	
							// Numerator Unit
							OWLClass numeratorUnitClass = getOWLClassForString(boss.getSnomedNumeratorUnit(), this.unitClassToName);
	
							OWLClass hasConcentrationStrengthNumeratorUnit = factory.getOWLClass(numeratorUnitClass);
							objectSomeValuesFromHasConcentrationStrengthNumeratorUnit = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthNumeratorUnit, hasConcentrationStrengthNumeratorUnit);
	
							objectSomeValuesFromHasConcentrationStrengthDenominatorValue = getOwlDataHasValue(this.hasConcentrationStrengthDenominatorValue, boss.getSnomedDenominatorValue());
	
							// Denominator Unit
							OWLClass denominatorUnitClass = getOWLClassForString(boss.getSnomedDenominatorUnit(), this.unitClassToName);
							if( denominatorUnitClass == null ) {
								OWLClass du = factory.getOWLClass(snomedNamespace, String.valueOf(scd.getUnitOfPresentationCode()));
								if( du != null && !du.getIRI().getFragment().equals("null") ) {
									denominatorUnitClass = du;
								}
								else {
									applyDefinition = false;
									denominatorUnitClass = du;
									applyConcentrationUnitOfPresentation = false;
								}
							}
	
							OWLClass hasConcentrationStrengthDenominatorUnit = factory.getOWLClass(denominatorUnitClass);
							objectSomeValuesFromHasConcentrationStrengthDenominatorUnit = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthDenomniatorUnit, hasConcentrationStrengthDenominatorUnit);
	
	
							//Also add presentation strengths
							log("\t\tcalculating the presentation units based on the existing quantity factor");
							ConcentrationToPresentation presentationUnits = new ConcentrationToPresentation(boss, scd.getRxNormQuantityFactor());
							boss.setConcentrationToPresentation(presentationUnits);
	
							objectSomeValuesFromHasPresentationStrengthNumeratorValue = getOwlDataHasValue(this.hasPresentationStrengthNumeratorValue, String.valueOf(presentationUnits.getPresentationNumeratorValue()));
	
							numeratorUnitClass = getOWLClassForString(presentationUnits.getPresentationNumeratorUnit(), this.unitClassToName);
							objectSomeValuesFromHasPresentationStrengthNumeratorUnit = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthNumeratorUnit, numeratorUnitClass);
	
							objectSomeValuesFromHasPresentationStrengthDenominatorValue = getOwlDataHasValue(this.hasPresentationStrengthDenominatorValue, String.valueOf(presentationUnits.getPresentationDenominatorValue()));
							
							// Denominator Unit
							denominatorUnitClass = getOWLClassForString(presentationUnits.getPresentationDenominatorUnit(), this.unitClassToName);
							if( denominatorUnitClass == null ) {
								OWLClass du = factory.getOWLClass(snomedNamespace, String.valueOf(scd.getUnitOfPresentationCode()));
								if( du != null && !du.getIRI().getFragment().equals("null")) {
									denominatorUnitClass = du;
								}
								else {
									applyDefinition = false;
									denominatorUnitClass = du;
									log("\t\tthe unit of presentation code on BoSS denominator doesn't exist " + boss.getBossRxCui() + " : " + boss.getBossName());
									log("\t\t\tUnable to apply the Presentation predicate's unit class");
									applyPresentationUnitOfPresentation = false;
								}
							}
	
							OWLClass hasPresentationStrengthDenominatorUnit = factory.getOWLClass(denominatorUnitClass);
							objectSomeValuesFromHasPresentationStrengthDenominatorUnit = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthDenominatorUnit, hasPresentationStrengthDenominatorUnit);					
	
	
	
						}				
						else if( applyDefinition && isConcentration ) {
	
							objectSomeValuesFromHasConcentrationStrengthNumeratorValue = getOwlDataHasValue(this.hasConcentrationStrengthNumeratorValue, boss.getSnomedNumberatorValue());
							
							// Numerator Unit
							OWLClass numeratorUnitClass = getOWLClassForString(boss.getSnomedNumeratorUnit(), this.unitClassToName);
	
							OWLClass hasConcentrationStrengthNumeratorUnit = factory.getOWLClass(numeratorUnitClass);
							objectSomeValuesFromHasConcentrationStrengthNumeratorUnit = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthNumeratorUnit, hasConcentrationStrengthNumeratorUnit);
	
							objectSomeValuesFromHasConcentrationStrengthDenominatorValue = getOwlDataHasValue(this.hasConcentrationStrengthDenominatorValue, boss.getSnomedDenominatorValue());
							
							OWLClass denominatorUnitClass = null;						
							if(scd.getUnitOfPresentationCode() != null) {
	//						if( denominatorUnitClass == null ) {
								OWLClass du = factory.getOWLClass(snomedNamespace, String.valueOf(scd.getUnitOfPresentationCode()));
								if( du != null && !du.getIRI().getFragment().equals("null") ) {
									denominatorUnitClass = du;
								}
								else {
									applyDefinition = false; //JC, make classify remove nulls
									denominatorUnitClass = du;
									log("\t\tthe unit on BoSS denominator doesn't exist " + boss.getBossName() + " : " + boss.getBossRxCui());
									log("\t\t\tUnable to apply the Concentration predicate's unit class");						
									applyConcentrationUnitOfPresentation = false;
								}
							}
							else {
								denominatorUnitClass = getOWLClassForString(boss.getSnomedDenominatorUnit(), this.unitClassToName);
							}
	
							OWLClass hasConcentrationStrengthDenominatorUnit = factory.getOWLClass(denominatorUnitClass);
							objectSomeValuesFromHasConcentrationStrengthDenominatorUnit = factory.getOWLObjectSomeValuesFrom(this.hasConcentrationStrengthDenomniatorUnit, hasConcentrationStrengthDenominatorUnit);
						}					
						else if( applyDefinition && isPresentation ) {						
	
							objectSomeValuesFromHasPresentationStrengthNumeratorValue = getOwlDataHasValue(this.hasPresentationStrengthNumeratorValue, boss.getSnomedNumberatorValue());
							
							// Numerator Unit
							try {
							OWLClass numeratorUnitClass = getOWLClassForString(boss.getSnomedNumeratorUnit(), this.unitClassToName);
	
							OWLClass hasPresentationStrengthNumeratorUnit = factory.getOWLClass(numeratorUnitClass);
							objectSomeValuesFromHasPresentationStrengthNumeratorUnit = factory.getOWLObjectSomeValuesFrom(this.hasPresentationStrengthNumeratorUnit, hasPresentationStrengthNumeratorUnit);
							} catch(Exception e) {
								System.err.println("???numerator unit change rxcui: " + scd.getCui().toString() + "\tboss of getSnomedNumeratorUnit:" + boss.getSnomedNumeratorUnit());
							}
	
							objectSomeValuesFromHasPresentationStrengthDenominatorValue = getOwlDataHasValue(this.hasPresentationStrengthDenominatorValue, boss.getSnomedDenominatorValue());
	
							// Denominator Unit
							OWLClass denominatorUnitClass = null;						
	//						if( denominatorUnitClass == null ) {
							if(scd.getUnitOfPresentationCode() != null) {
								OWLClass du = factory.getOWLClass(snomedNamespace, String.valueOf(scd.getUnitOfPresentationCode()));
								if( du != null && !du.getIRI().getFragment().equals("null") ) {
									denominatorUnitClass = du;
								}
								else {
									applyDefinition = false; //JC, make classify remove nulls
									denominatorUnitClass = du;								
									log("\t\tthe unit on BoSS denominator doesn't exist " + boss.getBossName() + " : " + boss.getBossRxCui());
									log("\t\t\tUnable to apply the Presentation predicate's unit class");	
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
						OWLClass unitOfPresentationClass = factory.getOWLClass(snomedNamespace, String.valueOf(scd.getUnitOfPresentationCode()));
						OWLObjectSomeValuesFrom objectSomeValuesFromUnitOfPresentation = factory.getOWLObjectSomeValuesFrom(hasUnitOfPresentation, unitOfPresentationClass);
						oces.add(objectSomeValuesFromUnitOfPresentation);
					}
	
					//Count of Base of Active Ingredient - again, we need to assume this integer class already exists
					//There do not seem to be any extreme cases at the moment.
					try {
						@SuppressWarnings("removal")
						Double size = new Double(0);
						if( scd.getBaseCuiCount() == 0 ) {
							//do nothing
						}
						else { 
							size = Double.valueOf(scd.getBaseCuiCount());
							OWLDataHasValue objectSomeValuesFromCountOfBaseFromActiveIngredient = getOwlDataHasValue(this.countOfBaseOfActiveIngredient, String.valueOf(size));
							oces.add(objectSomeValuesFromCountOfBaseFromActiveIngredient);
						}
					}
					catch(Exception e) {
						log("\t\tthis should never be seen with concrete domains-- there is no whole number for the boss size of cui " + scd.getCui() + "\t" + scd.getName());
					}
			
					if( ++scdCount % 1000 == 0 ) {
						log(" * " + scdCount + " SCDs added..");
					}	
					
					if( applyDefinition ) {
						
						//applying the logical definition axioms places the class where it falls in the MP hierarchy - few cases where SNCT has no bucket
						//e.g., (as of ... TODO: )
						log("\t\tdefinition SUCCESS fully applied");
						OWLObjectIntersectionOf intersection = factory.getOWLObjectIntersectionOf(oces);
						OWLEquivalentClassesAxiom axiom = factory.getOWLEquivalentClassesAxiom(scdClass, intersection);				
						axioms.add(axiom);		
	
					}
					else {
									
						OWLSubClassOfAxiom subClassAx = factory.getOWLSubClassOfAxiom(scdClass, fakeCdParent);
						axioms.add(subClassAx);
					}
				}
				else {
				
					OWLSubClassOfAxiom subClassAx = factory.getOWLSubClassOfAxiom(scdClass, fakeCdParent);
					axioms.add(subClassAx);				
				}
			}
		}
		
		return axioms;
		
	}
	
	private void applyNdcs(OWLClass scdClass, Set<NDC> theNdcs) {
		Set<OWLAxiom> ndcsForClass = new HashSet<>();
		for(NDC ndc : theNdcs) {
			OWLLiteral startDateValue = factory.getOWLLiteral(ndc.getStartDate());
			OWLLiteral endDateValue = factory.getOWLLiteral(ndc.getEndDate());
			OWLLiteral ndcValue = factory.getOWLLiteral(ndc.getNdc());
//			OWLAnnotation ndcAnnotation = factory.getOWLAnnotation(this.ndc, ndcValue);
			OWLAnnotation startDateAnnotation = factory.getOWLAnnotation(this.startDate, startDateValue);
			OWLAnnotation endDateAnnotation = factory.getOWLAnnotation(this.endDate, endDateValue);
			Set<OWLAnnotation> subAxioms = new HashSet<>();
			subAxioms.add(startDateAnnotation);
			subAxioms.add(endDateAnnotation);
			
			OWLAnnotation anno = factory.getOWLAnnotation(this.ndc, ndcValue, subAxioms);
			OWLAnnotationAssertionAxiom ndcAxiom = factory.getOWLAnnotationAssertionAxiom(scdClass.getIRI(), anno);
			
			ndcsForClass.add(ndcAxiom);
		
//			OWLAnnotation anno = factory.getOWLAnnotation(a, factory.getOWLLiteral(b));
//			OWLAxiom annoToClass = factory.getOWLAnnotationAssertionAxiom(c.getIRI(), anno);			

		}
		
		if(!ndcsForClass.isEmpty()) {
			man.addAxioms(ontology, ndcsForClass.stream());
			man.addAxioms(ontologyForPilot, ndcsForClass.stream());
		}
		
	}


	public Set<OWLClass> createSBDClasses(RxNormSCD scd) {
		Set<OWLClass> sbdClasses = new HashSet<>();
		
		for(RxNormSBD sbd : scd.getBrandDrugs()) {
			OWLClass sbdClass = factory.getOWLClass(rxNamespace, sbd.getRxcui().toString());
			OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(sbd.getName()));			
			OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(sbdClass.getIRI(), labelForClass);
			
			man.addAxiom(ontology, classLabel);
			man.addAxiom(ontologyForPilot, classLabel);
			
			sbdClasses.add(sbdClass);
		}
		
		return sbdClasses;		
	}
	
	public OWLClass createSBDClass(RxNormSBD sbd) {
		OWLClass sbdClass = null;
		
		if(sbd.getRxcui() != null ) {
			sbdClass = factory.getOWLClass(rxNamespace, sbd.getRxcui().toString());
			OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(sbd.getName()));			
			OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(sbdClass.getIRI(), labelForClass);
			
			man.addAxiom(ontology, classLabel);
			man.addAxiom(ontologyForPilot, classLabel);
			
			addAnnotation(sbdClass, this.status, sbd.getStatus());
		}
		
		return sbdClass;		
	}	
	
	public OWLClass getOWLClassForString(String value, HashMap<OWLClass, String> map) {
		OWLClass clz = null;

		if(	value != null && !value.equals("null") ) {
			for( OWLClass c : map.keySet() ) {
				String stringToMatch = getRDFSLabel(c);
				if( stringToMatch.equals(value) ) {
					clz = c;
					break;
				}
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
	
	public void createDoseFormChild(Integer code, RxNormDoseForm df, OWLClass parent) {
		//This is going to require a table since Snomed codes are not represented on rxcui
		//dose forms.  For now, place all dose forms into the tree.  It's possible the snomed
		//propery has a range we could work with.
		
		//There are no snomed mappings for any of these
		//Generate pairs based on (DFrx, (DFs, UPs))
		OWLClass doseFormClass = factory.getOWLClass(rxNamespace, code.toString());
		OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(df.getName()));		
		OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(doseFormClass.getIRI(), labelForClass);
		OWLSubClassOfAxiom subClassOfAxiom = factory.getOWLSubClassOfAxiom(doseFormClass, parent);
		man.addAxiom(ontology, classLabel);
		man.addAxiom(ontology, subClassOfAxiom);
		man.addAxiom(ontologyForPilot, classLabel);
		man.addAxiom(ontologyForPilot, subClassOfAxiom);		
	}
	
	public void addAnnotation(OWLClass c, OWLAnnotationProperty a, String b) {
		OWLAnnotation anno = factory.getOWLAnnotation(a, factory.getOWLLiteral(b));
		OWLAxiom annoToClass = factory.getOWLAnnotationAssertionAxiom(c.getIRI(), anno);
		man.addAxiom(ontology, annoToClass);	
		man.addAxiom(ontologyForPilot, annoToClass);		
	}
	

	public void createIngredientChild(Integer code, RxNormIngredient ingredient, OWLClass substanceParent, OWLClass fakeIngredientParent) {
		String rxCode = code.toString();		
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
		
		if(rxCode.equals("Rx866103") || rxCode.equals("Rx2393795") || rxCode.equals("Rx9789") ) {
			System.out.println("halt - checking missing/wrong: " + ingredient.getName());
		}
		
		Vector<Long> snomedIngredients = ingredient.getSnomedCodes();
		
		for(Long sctid : snomedIngredients) {
			codesToEquate.add(String.valueOf(sctid));
		}
				
		if(this.jnWrongSubstanceMappings.containsKey(rxCode)) {
			ArrayList<String> codesToRemove = jnWrongSubstanceMappings.get(rxCode);
			codesToEquate.removeAll(codesToRemove);
		}
		
		OWLClass ingredientClass = factory.getOWLClass(rxNamespace, rxCode);
		String ingName = ingredient.getName();
		
		if(ingName != null) {
			
			for(String snomedCode : codesToEquate) {
				OWLClass snomedClass = factory.getOWLClass(snomedNamespace, snomedCode);				
				String snomedLabel = getRDFSLabel(snomedClass);				
				if( snomedLabel != null && snomedLabel.contains(" (substance)")) {
					OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ingredientClass, snomedClass);
					man.addAxiom(ontology, eq);
					man.addAxiom(ontologyForPilot, eq);					
				}				
			}
			
			OWLClass parentClass = substanceParent;
			if(codesToEquate.size() == 0) {
//				ingName = ingName.concat(" (Fake)");
//				parentClass = fakeIngredientParent;
//				addAnnotation(ingredientClass, this.substanceNoExist, "no mapping from RxNorm to SCT");
//				addAnnotation(ingredientClass, this.asserted, false);				
			} 
//			else {
//				parentClass = substanceParent;
//			}
			
			OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(ingName));
			OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(ingredientClass.getIRI(), labelForClass);			
			
			OWLSubClassOfAxiom ingredientSubClassAxiom = factory.getOWLSubClassOfAxiom(ingredientClass, parentClass);				
			man.addAxiom(ontology, classLabel);
			man.addAxiom(ontology, ingredientSubClassAxiom);
			man.addAxiom(ontologyForPilot, classLabel);
			man.addAxiom(ontologyForPilot, ingredientSubClassAxiom);			
		}
		
	}
	
	public void createSCDChild(String code, RxNormSCD scd, OWLClass scdParent) {
		OWLClass scdClass = factory.getOWLClass(rxNamespace, code.toString());
		OWLAnnotation labelForClass = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(scd.getName()));		
		OWLAxiom classLabel = factory.getOWLAnnotationAssertionAxiom(scdClass.getIRI(), labelForClass);
		
		OWLSubClassOfAxiom scdSubClassAxiom = factory.getOWLSubClassOfAxiom(scdClass, scdParent);				
		man.addAxiom(ontology, classLabel);
		man.addAxiom(ontology, scdSubClassAxiom);
		man.addAxiom(ontologyForPilot, classLabel);
		man.addAxiom(ontologyForPilot, scdSubClassAxiom);		
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
	
	public File savePilotOntology() {
		File file = null;
		String fileString = null;
		try {
			String globalDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
			globalDate = globalDate.substring(0, 10);
			fileString = "./Pilot-Defined-RxNorm-with-SNCT-classes-" +  globalDate + ".owl";
			file = new File(fileString);
			
			FunctionalSyntaxDocumentFormat owlDocumentFormat = getFunctionalSyntaxDocumentFormat();
			ontologyForPilot.getOWLOntologyManager().setOntologyFormat(ontologyForPilot, owlDocumentFormat);
			
			man.saveOntology(ontologyForPilot, IRI.create(file));
			System.out.println("Saving file as " + fileString);
		}	catch(OWLOntologyStorageException e) {
			e.printStackTrace();
		}
		return file;
	}
	
	public FunctionalSyntaxDocumentFormat getFunctionalSyntaxDocumentFormat() {
		FunctionalSyntaxDocumentFormat owlDocumentFormat = new FunctionalSyntaxDocumentFormat();
		DefaultPrefixManager prefixManager = new DefaultPrefixManager();
		owlDocumentFormat.setPrefixManager(prefixManager);
		owlDocumentFormat.setPrefix("sct", snomedNamespace);
		owlDocumentFormat.setPrefix("oboInOwl", oboInOwlNamespace);
		owlDocumentFormat.setPrefix("skos", "http://www.w3.org/2004/02/skos/core#");		
		owlDocumentFormat.setDefaultPrefix(rxNamespace);
		return owlDocumentFormat;
	}	

	public String generateOutputFilename(String inputFile){
		return inputFile.replace(".owl", "_DRAFT.owl");
	}
	
	private boolean isRx(OWLClass c) {
		return c.getIRI().getNamespace().equals(rxNamespace);
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
	
	public String getRDFSLabelForLong(Long code) {
//		OWLClass cls = factory.getOWLClass(namespace, String.valueOf(code));
		OWLClass cls = factory.getOWLClass(snomedNamespace, String.valueOf(code)); //there are no Long/Double codes in RxNorm as of 230206		
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
	
//	private void addGcis() {
//		OWLObjectProperty propertyHasManufacturedDoseForm=factory.getOWLObjectProperty("411116001",namespace);
//		OWLClass phar=factory.getOWLClass("http://snomed.info/id/736542009");
//
//		Set<OWLAxiom> axios= new HashSet<OWLAxiom>();
//		
//		Consts.doseFormGcis.forEach(x -> {
//
//			System.out.println("RxNorm: "+ x.getRxN() +" Label: "+ x.getRxNLabel() +" SNOMED: "+ x.getSNO());
//			OWLAnnotation commentAnno = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(x.getRxNLabel()));
//
//			OWLClass RxNormClassDf=factory.getOWLClass(namespace, x.getRxN());
//			OWLAxiom ax = factory.getOWLAnnotationAssertionAxiom(RxNormClassDf.getIRI(), commentAnno);
//			/**
//			 * add RxNorm Dose forms with labels
//			 */
//			axios.add(ax);
//			OWLSubClassOfAxiom sub=factory.getOWLSubClassOfAxiom(RxNormClassDf, phar);
//			/**
//			 * add subclass relation for RxNorm Dose Forms
//			 */
//			axios.add(sub);
//			OWLClass SNCTClassDf=factory.getOWLClass(namespace, x.getSNO());
//
//			if(RxNormClassDf.getIRI().getShortForm().equals("Rx1649574")) {
//				OWLClassExpression exalpha=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNCTClassDf);
//				OWLClassExpression ex =factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, RxNormClassDf);
//				//System.out.println("exalpha "+exalpha);
//				OWLObjectProperty hasUnitOfPresentation = factory.getOWLObjectProperty(namespace, "763032000");
//				OWLClass vial = factory.getOWLClass(namespace,"732996003");
//
//				OWLClassExpression exbetha=factory.getOWLObjectSomeValuesFrom(hasUnitOfPresentation, vial);
//				//System.out.println("exbetha "+exbetha);
//				OWLObjectIntersectionOf ex2= factory.getOWLObjectIntersectionOf(exbetha,exalpha);
//				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
//				System.out.println("eq "+eq);
//				/**
//				 * alternate definition for RxNorm dose forms
//				 */
//				axios.add(eq);
//			}
//			else {
//				OWLClassExpression ex=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNCTClassDf);
//				OWLClassExpression ex2=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, RxNormClassDf);
//				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
//				/**
//				 * alternate definition for RxNorm dose forms
//				 */
//				axios.add(eq);
//			}
//		});
//		
//		Stream<OWLAxiom> resul=axios.stream();
//
//		man.addAxioms(ontology, resul);
//		reasoner.flush();		
//	}
	
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
	

// credit to: https://github.com/IHTSDO/snomed-owl-toolkit/blob/988cdc54fddb750424ffe21b4a2a1029412eb0bc/src/main/java/org/snomed/otf/owltoolkit/ontology/OntologyService.java
	private OWLDataHasValue getOwlDataHasValue(OWLDataProperty property, String val) {
		OWLLiteral owlLiteral;
		if (property.equals(this.countOfBaseOfActiveIngredient)) {
			int decimalIndex = val.indexOf(".");
			if(decimalIndex != -1) {
				val = val.substring(0, decimalIndex);
			}
			owlLiteral = factory.getOWLLiteral(val, OWL2Datatype.XSD_INTEGER);
		}
		else {
			if(Double.valueOf(val) % 1 == 0) {
				int decimalIndex = val.indexOf(".");
				if(decimalIndex != -1) {
					val = val.substring(0, decimalIndex);
				}				
			}
			owlLiteral = factory.getOWLLiteral(val, OWL2Datatype.XSD_DECIMAL);
		} 
		return factory.getOWLDataHasValue(property, owlLiteral);
	}
	
	public String formatConcreteValue(Double concreteValue) {
		String val = "1";		
		if(concreteValue != null ) {        
			DecimalFormat df = new DecimalFormat("#");
	        df.setMaximumFractionDigits(3);  //see MP IG
			val = df.format(concreteValue);
			if( val != null ) {
				return val;
			}
		}
		return val;
	}
	
	private void applyProperties(OWLClass c, Set<Property> properties) {
		for(Property p : properties) {
			OWLAnnotationProperty ap;
			ap = null;
			switch(p.getCategory()) {
			case("ATTRIBUTES"):
				ap = annotationAttributeClasses.get(p.getName()); //there is no such thing a code for properties...
				addAnnotation(c, ap, p.getValue());					
				break;
			case("CODES"):
				//use hasDbXref for meaningful use 
				//and also to remove ambiguity amongst attribute names overlapping with idTypes
				//e.g., CUI 308048: ANDA:ANDA074046
				//serialized as
				//AnnotationAssertion(oboInOwl:hasDbXref :Rx308048 "ANDA:ANDA074046")
				ap = annotationCodeClasses.get(p.getName());
				addAnnotation(c, this.hasDbXref, p.getName() + ":" + p.getValue());
				break;
			case("NAMES"):
				ap = annotationNameClasses.get(p.getName());
				addAnnotation(c, ap, p.getValue());
				break;
			case("SOURCES"):
				ap = annotationSourceClasses.get(p.getName());
				addAnnotation(c, ap, p.getValue());
				break;
			default:
				//do nothing
				break;
			}	
		}
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
