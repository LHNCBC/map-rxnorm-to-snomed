package gov.nih.nlm.mor.auditmap.inferredAndAssertedComparison;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import gov.nih.nlm.mor.auditmap.utilitaries.OntologyClassManagement;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

public class GetAssertedMappingsFromRxNorm {

	private static OntologyClassManagement classMana = null;
	private static OntologyClassManagement initialMana = null;
	private static OWLOntology Ontology = null;

	private static OWLReasoner Elkreasoner = null;
	private static OWLOntologyManager manager = null ;
	private static RestUrl restUrl = new RestUrl();
	private static ExponentialBackoff exponentialBackoff = new ExponentialBackoff();


//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/SNOMED2RxNormUpdate.owl");
//		//File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/RxNorm2Snomed_2019-12-20_15-37-40/RxNorm2Snomed_2019-12-20_15-37-40.owl");
//
//
//		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
//		Elkreasoner = classMana.getElkreasoner();
//		manager=classMana.getManager();
//		Ontology=classMana.getOntology();
//		try {
//			System.out.println("start");
//			getAssertedMappingFromRxNorm();
//		} catch (OWLOntologyStorageException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println("finished");
//
//	}

	public GetAssertedMappingsFromRxNorm(String filename, OntologyClassManagement management) {
		//RW: From my notes, this can be started as the asserted file and saved as the RxNorm2SNOMEDAnalyse file
		//also next to this note is 'diff from beignning to updated dose form' -- so, this may be useful to create
		//two RxNorm2SNOMEDAnalyse files.  Where the diff occurs, however, it not very well known and may need written somehow.
		
		
		classMana = management;  //this is the SNOMED2RxNormUpdate ontology from the UpdateDoseForms class
		initialMana = new OntologyClassManagement(new File (filename));  //this is the original RxNorm2Snomed_date.owl file being audited
				
		classMana = management;
		Ontology = classMana.getOntology();
		Elkreasoner = classMana.getElkreasoner();
		manager = classMana.getManager();

	}
	
	public OntologyClassManagement run() {
		
		try {
			System.out.println("start");
			getAssertedMappingFromRxNorm(classMana, "RxNorm2SnomedToAnalyse-Updated.owl", true);
			getAssertedMappingFromRxNorm(initialMana, "RxNorm2SnomedToAnalyse-Original.owl", false);			
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	
		}
		System.out.println("finished");		
		

		return classMana;
	}



	/**
	 * this method update the RxNorm-SNOMED integrated file to add the asserted mappings between SNOMED CT and RxNorm SCD
	 * @throws OWLOntologyStorageException
	 */
	public static void getAssertedMappingFromRxNorm(OntologyClassManagement management, String filename, boolean isClassMana) throws OWLOntologyStorageException {
		Set<OWLClass> SNOMEDclinicalDrugs = new HashSet<OWLClass>();
		Set<OWLClass> SNOMEDMedicinalProducts = new HashSet<OWLClass>();
		Map<OWLClass, Set<OWLClass>> sertedMappings= new HashMap<OWLClass, Set<OWLClass>>();


		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = management.getManager().getOWLDataFactory();

		OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 

		OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);

		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("MapsToCodeAsserted",pm);

		Set<OWLClass> MedicinalProducts=management.getElkreasoner().getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
		MedicinalProducts.forEach((k)->{if(!k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {SNOMEDMedicinalProducts.add(k);}});

		SNOMEDMedicinalProducts.forEach(al->{
			Stream<OWLClassExpression> eq= EntitySearcher.getEquivalentClasses(al, Ontology);
			Set<OWLClass>Ingredients = new HashSet<OWLClass>();
			eq.forEach((q)->{
				// System.out.println("medicilab produc "+al);
				Ingredients.addAll(resultSpecificRelation(q, getBOss));
				//System.out.println(" ing "+Ingredients);


			});
			if(Ingredients.size()>=1) {
				SNOMEDclinicalDrugs.add(al);
			}

		});
		Set<OWLAxiom> ensembleAxiom= new HashSet<OWLAxiom>();
		SNOMEDclinicalDrugs.forEach(a->{
			String code=a.getIRI().getShortForm();
			Set<String> rxnorRelated=getRxCodes(code);

			rxnorRelated.forEach(anno->{
				OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
				OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(a.getIRI(), annotation);
				ensembleAxiom.add(ax1);
			});


		});

		Stream<OWLAxiom> resul=ensembleAxiom.stream();

		management.getManager().addAxioms(management.getOntology(), resul);
		File OntologyPath2 = new File (filename);
		//	File OntologyPath2 = new File ("./git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2Snomed_2019-12-20_15-37-40.owl");
		management.getManager().saveOntology(management.getOntology(), IRI.create(OntologyPath2.toURI()));
		
		if( isClassMana ) {
			classMana.setOntology(management.getOntology());
			classMana.setManager(management.getManager());
			classMana.setElkreasoner(management.getElkreasoner());
		}

	}
	/**
	 * select the related object for a specific OWLObjectProperty in a OWLClassExpression
	 * @param Expression
	 * @param propert
	 * @return
	 */
	public static Set<OWLClass> resultSpecificRelation(OWLClassExpression Expression, OWLObjectProperty propert){

		Set<OWLClass> resultats= new HashSet<OWLClass>();
		OWLObjectIntersectionOf express= (OWLObjectIntersectionOf) Expression;
		ClassExpressionType ar= express.getClassExpressionType();

		for(OWLClassExpression inter:express.operands().collect(Collectors.toSet())){
			if(!inter.isAnonymous()) {
				OWLClass az= inter.asOWLClass();
			}
			else {
				OWLObjectSomeValuesFrom restic= (OWLObjectSomeValuesFrom) inter;

				OWLObjectProperty ert=(OWLObjectProperty) restic.getProperty();
				if(ert.getIRI().toString().equals(propert.getIRI().toString())) {
					resultats.add((OWLClass) restic.getFiller());
				}
				else if (restic.getFiller().isAnonymous()){

					OWLClassExpression expression=restic.getFiller();
					if(expression.getClassExpressionType().equals(ar)){
						resultats.addAll(resultSpecificRelation(restic.getFiller(), propert));
					};



				}		
			}
		}
		return resultats;

	}
	/**
	 * Retrieve for a specific SCTID, its related RxCui
	 * @param code
	 * @return
	 */
	public static Set<String> getRxCodes(String code) {

		Set<String> codes = new HashSet<>();

		JSONObject allSnomedCodes = null;

		String snomedCuiString = code;

		try {

			allSnomedCodes = getresult(restUrl.getRestUrl() + "/REST/rxcui.json?idtype=SNOMEDCT&id=" + snomedCuiString, null);                   
			//System.out.println(" allSnomedCodes "+allSnomedCodes);
		}

		catch(Exception e) {

			System.out.println("Unable to fetch rx codes for snomed cui: " + snomedCuiString);
			e.printStackTrace();

		}



		if( allSnomedCodes != null && !allSnomedCodes.isNull("idGroup") ) {

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
	/**
	 * get the json result from the RestFul API
	 * @param URLtoRead
	 * @return
	 * @throws IOException
	 */
//	public static JSONObject getresult(String URLtoRead) throws IOException {
//
//		URL url;
//
//		HttpsURLConnection connexion;
//
//		BufferedReader reader;
//
//
//
//		String line;
//
//		String result="";
//
//		url= new URL(URLtoRead);
//
//
//
//		connexion= (HttpsURLConnection) url.openConnection();
//
//		connexion.setRequestMethod("GET");
//
//		reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));
//
//		while ((line =reader.readLine())!=null) {
//
//			result += line;
//
//
//
//		}
//
//
//
//		JSONObject json = new JSONObject(result);
//
//		return json;
//
//	}
	
	public static JSONObject getresult(String URLtoRead, ExponentialBackoff b) throws IOException, InterruptedException {
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
