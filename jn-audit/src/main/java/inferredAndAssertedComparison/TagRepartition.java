package inferredAndAssertedComparison;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import utilitaries.OntologyClassManagement;

public class TagRepartition {
	private static OWLOntology Ontology;

    private static OWLReasoner Elkreasoner;
    private static OWLOntologyManager manager;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String path="RxNorm2SNOMEDTag.owl";
		String path="AuditRxNorm2SNOMED.owl";
		File OntologyPath = new File ("./Deliverable/"+path);
		 Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
			
		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		 Elkreasoner = classMana.getElkreasoner();
		 manager=classMana.getManager();
		 Ontology=classMana.getOntology();
		 
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
         OWLDataFactory  factory = manager.getOWLDataFactory();
         
         OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 
         
         OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
         
         OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
         
         Set<OWLClass> MedicinalProducts=Elkreasoner.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
         MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) 
               	 RxNirMedicinalProducts.add(k);
                  
         });
         
         Set<OWLAnnotation> annot= new HashSet<OWLAnnotation>();
        
         Set<OWLClass> SSU = new HashSet<OWLClass>();
         Set<OWLClass> MBO = new HashSet<OWLClass>();
         Set<OWLClass> MAI = new HashSet<OWLClass>();
         Set<OWLClass> ENM = new HashSet<OWLClass>();
         Set<OWLClass> SPE = new HashSet<OWLClass>();
         Set<OWLClass> ERR = new HashSet<OWLClass>();         
         Set<OWLClass> BOS = new HashSet<OWLClass>();
         Set<OWLClass> VAL = new HashSet<OWLClass>();
         Set<OWLClass> DFE = new HashSet<OWLClass>();
         Set<OWLClass> SUM = new HashSet<OWLClass>();
         
         Set<OWLClass> SAI = new HashSet<OWLClass>();
         
         Set<OWLClass> RxNormAnnotated = new HashSet<OWLClass>();
         
         RxNirMedicinalProducts.forEach(a->{
        	 for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(a, Ontology, prop).collect(Collectors.toSet())) {
        		 annot.add(d);
        		 
        		 RxNormAnnotated.add(a);
        		 OWLAnnotationValue j =d.annotationValue();
 				OWLLiteral vv= (OWLLiteral) j;
 						String errorTag= vv.getLiteral();
 						if (errorTag.startsWith("MAI")) {
 							MAI.add(a);
 						}
 						if (errorTag.startsWith("SSU")) {
 							SSU.add(a);
 						}
 						if (errorTag.startsWith("MBO")) {
 							MBO.add(a);
 						}
 						if (errorTag.startsWith("ERR")) {
 							ERR.add(a);
 						}
 						if (errorTag.startsWith("SPE")) {
 							SPE.add(a);
 						}
 						if (errorTag.startsWith("BOS")) {
 							BOS.add(a);
 						}
 						if (errorTag.startsWith("SAI")) {
 							SAI.add(a);
 						}
 						if (errorTag.startsWith("VAL")) {
 							VAL.add(a);
 						}
 						if (errorTag.startsWith("DFE")) {
 							DFE.add(a);
 						}
 						if (errorTag.startsWith("SUM")) {
 							SUM.add(a);
 						}
 						if (errorTag.startsWith("Erroneously not mapped")) {
 							ENM.add(a);
 						}
        		 
        		 
        	 }
        	 
         });
  System.out.println(annot.size());
  System.out.println("RxNormAnnotated "+RxNormAnnotated.size());
System.out.println("MAI "+MAI.size());
System.out.println("SSU "+SSU.size());
System.out.println("MBO "+MBO.size());
System.out.println("ERR "+ERR.size());
System.out.println("SPE "+SPE);
System.out.println("BOS "+BOS.size());
System.out.println("SAI "+SAI.size());
System.out.println("VAL "+VAL.size());
System.out.println("DFE "+DFE.size());
System.out.println("SUM "+SUM.size());
System.out.println("ENM "+ENM.size());



	}

}
