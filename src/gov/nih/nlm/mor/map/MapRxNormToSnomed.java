/* Robert Wynne, Black Canyon Consulting
 * First created: December 21, 2018  
 * 
 * Goal #1: Map existing RxNorm SCDs (clinical drugs) to Snomed CDs (clinical drugs)
 *      #2: Report various QA findings while developing these equivalences.  Report will show why an RxCUI
 *          maps. Why an RxCUI maps multiple Snomed concepts. Why an SCD didn't map
 *          in the first place. This report is useful to both ground truths for further harmonization
 *          and improved, facilitated curation.
 * 
 * Given: RxNorm data is our source of truth that is found in both the RxNorm REST API, and the 
 *        RxNorm History REST API.  We are going to build an RxNorm model in order to compare to 
 *        the target source.
 * 
 *        SNOMED CT is presented in asserted OWL2 EL++, and is inferred using the ELK reasoner
 * 	      SNOMED CT has RxNorm definitional features in its equivalent classes
 *        We'll use the data within these equivalent classes to produce a model for the Analyzer/QA report
 *  
 * Tracking:
 *    
 *   181228: LHS (RxNorm content) populated into a model for clinical drugs.
 *   190104: RHS (Snomed concept) ready for population into model for all medicinal products
 *   190108: Begin writing Rx concepts and setting equivalences based on known SNOMED codes found on rxcuis
 *   190109: Integrate a dose form lookup table by pairing RxDoseForm with SNOMED pairs of Dose Form and Unit of Presentation 
 *   		 (RxDF, (SnDF, SnUP))
 *   200301: SNOMED number classes moved to concrete domains (xsd namespace). Preprocessing added to facilitate Analyzer Class.
 *   200905: Integrated expert code from post-doc student to audit various substances, dose forms, and other properties 
 *   210219: Add to Analyzer a column for template differences
 *   221227: Add annotations to Defined files
 *   231101: Include non-Active content in JC's Defined file for EHR Interoperability
 *   230111: Include NDCs with start and end dates
 *   230825: Denominator unit changes in RxNav (e.g., 1 to EACH)
 *   230825: Update the JC flavor of GenerateEquivalenciesSnomedClassesDefinedFile to include (RxNorm:<CUI>) on rdfs:labels
 *   231109: Begin the Pilot class  
 *   231120: Remove redundant and extraneous annotations from Pilot class
 * 
 */

package gov.nih.nlm.mor.map;

import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import gov.nih.nlm.mor.RxNorm.RxNormDoseForm;
import gov.nih.nlm.mor.RxNorm.RxNormIngredient;
import gov.nih.nlm.mor.RxNorm.RxNormSCD;
import gov.nih.nlm.mor.Snomed.SnomedConcept;
import gov.nih.nlm.mor.Snomed.SnomedDFPair;
import gov.nih.nlm.mor.util.FetchRxNormDataAllStatus;
import gov.nih.nlm.mor.util.RestUrl;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.TreeMap;


public class MapRxNormToSnomed implements java.io.Serializable {
	
	private static final long serialVersionUID = 6998258204010038034L;
	OWLOntologyManager man;
	OWLOntology ontology;
	OWLReasonerFactory reasonerFactory = null;
	OWLReasoner reasoner = null;
	String ontologyFilename = null;
	String ontologyFilenamePreprocessed = null;
	TreeMap<Integer, RxNormSCD> rxcui2RxNormSCD = new TreeMap<Integer, RxNormSCD>();
	TreeMap<Integer, RxNormIngredient> rxcui2RxNormIN = new TreeMap<Integer, RxNormIngredient>();
	TreeMap<Integer, SnomedConcept> snomedID2SnomedConcept = new TreeMap<Integer, SnomedConcept>();
	TreeMap<Integer, RxNormDoseForm> rxcui2RxNormDF = new TreeMap<Integer, RxNormDoseForm>();
	TreeMap<Integer, ArrayList<SnomedDFPair>> rxdf2SnomedDFPair = new TreeMap<Integer, ArrayList<SnomedDFPair>>();	
	TreeMap<RxNormSCD, ArrayList<SnomedConcept>> rxSCD2SnomedConcept = new TreeMap<RxNormSCD, ArrayList<SnomedConcept>>();
	URL rxNavAPIUrl = null;
	String dfMapfile = "./RxNorm2SnomedDFMapping.txt";
	boolean includeObsoleteAndNotActive = false;
	boolean serialize;
	boolean deserialize;
	RestUrl restUrl = new RestUrl();
	
	//TODO: Make these URLs configurable so decompiling this class is not necessary
	String snomedOWLInfo = new String("https://confluence.ihtsdotools.org/display/DOCOWL/SNOMED+CT+OWL+Guide");
	String rxNavApiInfo = new String("https://lhncbc.nlm.nih.gov/RxNav/APIs/index.html");
	
	String savedOutputFilename = null;
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();		
		MapRxNormToSnomed mapper = new MapRxNormToSnomed();
		if( args.length < 2 ) {
			mapper.printHelp();
			mapper.exit();
		}
		
		//http://lhc-lx-imagicdocker.nlm.nih.gov:4000/REST/
//		mapper.configure(args[0], args[1], args[2], restUrl.getRestUrl() + "/REST/");				
		mapper.configure(args[0], args[1]);
		try {
			mapper.run();
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Finished mapping in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");	
	}
	
	//TODO: Get specific. Figure out if Base URL should be explicit as a command parameter or implict within a shell command (i.e., -Dhttps://...)
	public void printHelp() {
		System.out.println("This program is run with X parameters:");
		System.out.println("  1. SNOMED CT OWL filepath");
		System.out.println("  2. Preprocessed SNOMED CT OWL filepath (elk-reasoner Issue #61)");
		System.out.println("  3. ");
		System.out.println("  4. Base URL to the RxNav API endpoint (default is http://lhc-lx-imagicdocker.nlm.nih.gov:4000/REST/)");
		System.out.println("For details on obtaining the SNOMED CT OWL file see: ");
		System.out.println("  " + snomedOWLInfo );
		System.out.println("For more information on the RxNav API see: ");
		System.out.println("  " + rxNavApiInfo );
	}
	
	public void exit() {
		System.out.println("Exiting gracefully.");
		System.exit(0);		
	}
	
	public void configure(String filename, String filenamePreprocessed) {
		
		ontologyFilename = new String(filename);
		ontologyFilenamePreprocessed = new String(filenamePreprocessed);
		
//		boolean serialize = false;
//		boolean deserialize = false;
//		if( baseRestUrl == null ) {
//			serialize = true;
//		}
//		else {
//			try {
//				rxNavAPIUrl = new URL(baseRestUrl);  //reconfigure
//			} catch (MalformedURLException e) {
//				System.out.print("Program is using a malformed base REST URL");
//				printHelp();
//				e.printStackTrace();
//			}
//		}
//		
//		Boolean all = Boolean.valueOf(includeAll);
//		includeObsoleteAndNotActive = all.booleanValue();
//		savedOutputFilename = generateOutputFilename(filename);
	}
	
	public void run() throws OWLOntologyStorageException {
		
		System.out.println("********** Fetching current RxNorm Data **********");
//		System.out.println(System.getProperty("http.proxyHost"));
//		System.out.println(System.getProperty("http.proxyPort"));
//		System.out.println(System.getProperty("https.proxyHost"));
//		System.out.println(System.getProperty("https.proxyPort"));	
		System.out.println("Base REST url: " + restUrl.getRestUrl());
		String[] args = new String[0]; //no detective work or debugs from this class at this time

		FetchRxNormData fetchRxNorm = new FetchRxNormData(false);
		
		
		System.out.println("********** Internal Equivalency checks **********");
		
		//Produce three different OWL files
		//The first will allow us to produce the analysis file (elk reasoner 0.5 has no support for DataHasValue when determining class equivalency (see elk-reasoner issue #61) 
		//It will also contain SNOMED classes for a dash display with equivalence signs in Protege 5.5.0-beta7
		GenerateEquivalencesSnomedClasses equivalences = new GenerateEquivalencesSnomedClasses(ontologyFilenamePreprocessed, fetchRxNorm, true, true);
		equivalences = null; //be sure we have resources available for the next if GC is on the fritz

		System.out.println("********** Fetching All RxNorm Data **********");
		
		//The second file will contain only RxNorm content referencing the SNOMED content (that can be loaded independently as an import)
		//though we need all of RxNav content including non-Active
		FetchRxNormDataAllStatus fetchRxNormAll = new FetchRxNormDataAllStatus(args);
		
		System.out.println("********** Defined Equivalency checks **********");		
	
		PilotDefinedFileGenerateEquivalencesSnomedClasses equivalencesDefined = new PilotDefinedFileGenerateEquivalencesSnomedClasses(ontologyFilename, fetchRxNormAll, true, true);
		equivalencesDefined = null; //GC protection as above
		
		//The third file will contain only RxNorm content that is Active, referencing the SNOMED content (as with the second file)
		DefinedFileGenerateEquivalencesSnomedClasses equivalencesDefinedAllStatus = new DefinedFileGenerateEquivalencesSnomedClasses(ontologyFilename, fetchRxNormAll, true, true);			
	
	}
	
	public void saveOntology() {
		System.out.println("Saving mapped file as " + savedOutputFilename);
		try {
			man.saveOntology(ontology, IRI.create((new File(new URI(savedOutputFilename)))));
		} catch (OWLOntologyStorageException | URISyntaxException e) {
			System.out.println("Unable to save the file.");
			e.printStackTrace();
		} 
	}

}
