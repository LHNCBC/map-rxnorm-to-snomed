package inferredAndAssertedComparison;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.text.html.parser.Entity;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import utilitaries.OntologyClassManagement;

public class GetSNOMEDCTClinicalDrugs {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
	//	 File OntologyPath = new File ("./Final/SNOMED2RxNormUpdate.owl");
		 File OntologyPath = new File ("/Audit/Livrable/RxNorm2Snomed_2019-12-20_15-37-40/RxNorm2Snomed_2019-12-20_15-37-40.owl");;
			
		 
		 OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		 OWLReasoner Elkreasoner = classMana.getElkreasoner();
		 OWLOntologyManager manager=classMana.getManager();
		 OWLOntology Ontology=classMana.getOntology();
		 Set<OWLClass> SNOMEDclinicalDrugs = new HashSet<OWLClass>();
		 Set<OWLClass> SNOMEDMedicinalProducts = new HashSet<OWLClass>();
	        OWLDataFactory  factory = manager.getOWLDataFactory();
	        
	        OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 
	        
	        OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
	        
	        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("MapsToCodeAsserted",pm);
	       
	        Set<OWLClass> MedicinalProducts=Elkreasoner.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
	        MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {SNOMEDMedicinalProducts.add(k);}});
	        
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
	        SNOMEDclinicalDrugs.forEach(e->{
	       
	        	 String label="";
	            	for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(e, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
	    				//label
	    				OWLAnnotationValue j =a.annotationValue();
	    				OWLLiteral vv= (OWLLiteral) j;
	    						label= vv.getLiteral();
	    				//label=a.toString();
	    			}
	            	if(!label.startsWith("Product containing precisely")) {
	            	System.out.println(e.getIRI().getShortForm()+" ; "+label);
	            	}
	       	
	       	 
	       	 
	        });
	        System.out.println("SNOMEDMedicinalProducts "+SNOMEDMedicinalProducts.size());
	        System.out.println("SNOMEDclinicalDrugs "+SNOMEDclinicalDrugs.size());

	}
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
	

}
