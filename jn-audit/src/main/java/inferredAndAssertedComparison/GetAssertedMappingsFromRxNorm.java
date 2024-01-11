package inferredAndAssertedComparison;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

import utilitaries.OntologyClassManagement;

public class GetAssertedMappingsFromRxNorm {
	  private static OWLOntology Ontology;

      private static OWLReasoner Elkreasoner;
      private static OWLOntologyManager manager;
   

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/SNOMED2RxNormUpdate.owl");
		File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/RxNorm2Snomed_2019-12-20_15-37-40/RxNorm2Snomed_2019-12-20_15-37-40.owl");
	
        
		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		 Elkreasoner = classMana.getElkreasoner();
		 manager=classMana.getManager();
		 Ontology=classMana.getOntology();
        try {
        	System.out.println("start");
			getAssertedMappingFromRxNorm();
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("finished");

	}
	/**
	 * this method update the RxNorm-SNOMED integrated file to add the asserted mappings between SNOMED CT and RxNorm SCD
	 * @throws OWLOntologyStorageException
	 */
	public static void getAssertedMappingFromRxNorm() throws OWLOntologyStorageException {
		 Set<OWLClass> SNOMEDclinicalDrugs = new HashSet<OWLClass>();
		 Set<OWLClass> SNOMEDMedicinalProducts = new HashSet<OWLClass>();
		 Map<OWLClass, Set<OWLClass>> sertedMappings= new HashMap<OWLClass, Set<OWLClass>>();
		 
        
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
        OWLDataFactory  factory = manager.getOWLDataFactory();
        
        OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 
        
        OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
        
        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("MapsToCodeAsserted",pm);
       
        Set<OWLClass> MedicinalProducts=Elkreasoner.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
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
   	 
		manager.addAxioms(Ontology, resul);
		//File OntologyPath2 = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedToAnalyse.owl");
		File OntologyPath2 = new File ("/git/MapRxNormToSnomed/Audit/Livrable/RxNorm2Snomed_2019-12-20_15-37-40/RxNorm2Snomed_2019-12-20_15-37-40.owl");
		  manager.saveOntology(Ontology, IRI.create(OntologyPath2.toURI()));
       
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

           allSnomedCodes = getresult(restUrl.getRestUrl() + "/REST/rxcui.json?idtype=SNOMEDCT&id=" + snomedCuiString);                   
           //System.out.println(" allSnomedCodes "+allSnomedCodes);
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
/**
 * get the json result from the RestFul API
 * @param URLtoRead
 * @return
 * @throws IOException
 */
public static JSONObject getresult(String URLtoRead) throws IOException {

    URL url;

    HttpsURLConnection connexion;

    BufferedReader reader;

   

    String line;

    String result="";

    url= new URL(URLtoRead);



    connexion= (HttpsURLConnection) url.openConnection();

    connexion.setRequestMethod("GET");

    reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));

    while ((line =reader.readLine())!=null) {

           result += line;

          

    }

   

    JSONObject json = new JSONObject(result);

    return json;

}

}
