package semanticClinicalDrugsMappings;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import utilitaries.OntologyClassManagement;

public class GetSCDmappingsCaracteristiques {
	 private static OWLOntology Ontology;

     private static OWLReasoner Elkreasoner;
     private static OWLOntologyManager manager;

   

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/**use RxNorm2SnomedDecember.owl for the original integrated file
		 * use RxNorm2SnomedDecemberSuppressedWrongMappings.owl for the Ontology without wrong ingredients mappings
		 * use RxNorm2SnomedIngredientUpdate.owl for the Ontology with the update corrected mappings for ingredients
		 * use SNOMED2RxNormUpdate.owl for the ontology describing the dose forms of RxNorm
		 * use 
		 */
		File OntologyPath = new File ("./Final/RxNorm2SnomedDecember.owl");
		//File OntologyPath = new File ("./Final/RxNorm2SnomedDecemberSuppressedWrongMappings.owl");
		//File OntologyPath = new File ("./Final/RxNorm2SnomedIngredientUpdate.owl");
		//File OntologyPath = new File ("./Final/SNOMED2RxNormUpdate.owl");
		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		Elkreasoner = classMana.getElkreasoner();
		manager=classMana.getManager();
		Ontology=classMana.getOntology();
		getresults();

	}
	
	public static Set<OWLClass> getresults() {


    	Map<OWLClass, Set<OWLClass>> MappingsSCD= new HashMap<OWLClass, Set<OWLClass>>();
    	Map<OWLClass, Set<OWLClass>> MappingsSCDtoSNOMED= new HashMap<OWLClass, Set<OWLClass>>();
    	Map<OWLClass, Set<OWLClass>> MappingsSCDOneToOne= new HashMap<OWLClass, Set<OWLClass>>();
		
    	Map<OWLClass, Set<OWLClass>> MappingsSCDOneToMany= new HashMap<OWLClass, Set<OWLClass>>();
    	Map<OWLClass, Set<OWLClass>> MappingsSCDOneToZero=new HashMap<OWLClass, Set<OWLClass>>();
		
		
        Set<OWLClass> RxNormMedicinalProductWithVaccines = new HashSet<OWLClass>();
        PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
        OWLDataFactory  factory = manager.getOWLDataFactory();
        OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
        Set<OWLClass> MedicinalProducts=Elkreasoner.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
        MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {RxNormMedicinalProductWithVaccines.add(k);}});
        
        System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProductWithVaccines.size());
        Set<OWLClass> RxNormMedicinalProduct = new HashSet<OWLClass>();
        Set<OWLClass> Vaccines = new HashSet<OWLClass>();
        RxNormMedicinalProductWithVaccines.forEach((d)->{
        	String label="";
        	//Stream<OWLAnnotation> a=EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel());
        	for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				//label
				OWLAnnotationValue j =a.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
						label= vv.getLiteral();
				//label=a.toString();
			}
        	if(label.contains("vaccine ")) {
        		
        		Vaccines.add(d);
        		//System.out.println("label "+label);
        			
        	}
        	else if(label.contains("Vaccine ")) {
        		
        		Vaccines.add(d);
        			
        	}
        	else if(label.contains("VACCINE ")) {
        		
        		Vaccines.add(d);
        			
        	}
        	else if(label.contains("vaccine, ")) {
        		
        		Vaccines.add(d);
        			
        	}
        	else if(label.contains("strain ")) {
        		
        		Vaccines.add(d);
			}
        	else if(label.contains("Strain ")) {
        		
        		Vaccines.add(d);
			}
        	else if(label.contains("STRAIN ")) {
        		
        		Vaccines.add(d);
			}
        	else if(label.contains(" strain,")) {
        		
        		Vaccines.add(d);
			}
        	
        	else if(label.contains("antigen ")) {
    		
        		Vaccines.add(d);
    			
    		}
        	else {
        		RxNormMedicinalProduct.add(d);
        	}
        	
        });
        
//        System.out.println("vaccines "+Vaccines.size());
//        System.out.println("RxNormMedicinalProductWithVaccines "+RxNormMedicinalProductWithVaccines.size());
//        System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProduct.size());
       
        RxNormMedicinalProduct.forEach((d)->{
        	
        	 Set<OWLClass> enseEq= new HashSet<OWLClass>();
        	Set<OWLClass> eq=	Elkreasoner.getEquivalentClasses(d).entities().collect(Collectors.toSet());
        	eq.forEach((yu)->{
        		if(!yu.getIRI().toString().equals(d.getIRI().toString())) {enseEq.add(yu);}
        	});
        	//System.out.println(d+" eq "+enseEq);
        	if(!MappingsSCD.containsKey(d)) {
        		MappingsSCD.put(d, enseEq);
        	}
        	
        });
     //   MappingsSCDOneToOne;
     //   Set<OWLClass> RxNormMappingProblematik = new HashSet<OWLClass>();
        Set<OWLClass> RxNormMappingOnetoOneProblem = new HashSet<OWLClass>();
        Set<OWLClass> RxNormMappingOnetoManyProblem = new HashSet<OWLClass>();
        MappingsSCD.forEach((f,op)->{
        	if(op.size()==1) {
        		MappingsSCDOneToOne.put(f, op);
        		op.forEach(az->{
        			if(az.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
        				RxNormMappingOnetoOneProblem.add(f);
        				//System.out.println("f "+f+", "+az);
        			}
        		});
        	}
        	else if(op.size()>1) {
        		MappingsSCDOneToMany.put(f, op);
        		op.forEach(az->{
        			if(az.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
        				RxNormMappingOnetoManyProblem.add(f);
        			}
        		});
        		System.out.println("f "+f+", "+op);
        	}
        	else if(op.size()==0) {
        		MappingsSCDOneToZero.put(f, op);
        		
        	}
        }
        
        		);
        
        Set<OWLClass> Mappings1_0 = new HashSet<OWLClass>();
        Set<OWLClass> Mappings1_1 = new HashSet<OWLClass>();
        Set<OWLClass> Mappings1_N = new HashSet<OWLClass>();
        
        
        Set<OWLClass> Snomedmapped = new HashSet<OWLClass>();
        
        MappingsSCD.forEach((f,op)->{
        	Set<OWLClass>snomedRelated= new HashSet<OWLClass>();
        	op.forEach(az->{
    			if(!az.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
    				snomedRelated.add(az);
    				
    			}
    		});
        	Snomedmapped.addAll(snomedRelated);
        	if(snomedRelated.size()==1) {
        		Mappings1_1.add(f);
        	}
        	else if(snomedRelated.size()>1) {
        		Mappings1_N.add(f);
        	}
        	else {
        		Mappings1_0.add(f);
        	}
        });
        
        Set<OWLClass> SNOMEDMappings1_0 = new HashSet<OWLClass>();
        Set<OWLClass> SNOMEDMappings1_1 = new HashSet<OWLClass>();
        Set<OWLClass> SNOMEDMappings1_N = new HashSet<OWLClass>();
        Snomedmapped.forEach(atre->{
        	Set<OWLClass> relatedRx= new HashSet<OWLClass>();
        	MappingsSCD.forEach((f,op)->{
//            	if(op.contains(a)) {
//            		relatedRx.add(f);
//            	}
        		op.forEach(az->{
        			if(az.getIRI().toString().equals(atre.getIRI().toString())) {
        				relatedRx.add(f);
        				//System.out.println(" az "+az);
        			}
        		});
            	
            });
        	if(relatedRx.size()>1) {
        		SNOMEDMappings1_N.add(atre);
        	}
        	else if(relatedRx.size()==1) {
        		SNOMEDMappings1_1.add(atre);
        	}
        	else {
        		SNOMEDMappings1_0.add(atre);
        	}
        });
        
        
      //  MappingsSCDtoSNOMED
        System.out.println("Snomedmapped "+Snomedmapped.size());
       System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProduct.size());
        System.out.println("MappingsSCDOneToOne "+MappingsSCDOneToOne.size());
        System.out.println("RxNormMappingOnetoOneProblem "+RxNormMappingOnetoOneProblem.size());
        System.out.println("MappingsSCDOneToMany "+MappingsSCDOneToMany.size());
        System.out.println("RxNormMappingOnetoManyProblem "+RxNormMappingOnetoManyProblem.size());
        System.out.println("MappingsSCDOneToZero "+MappingsSCDOneToZero.size());
        
        System.out.println("Update RxNorm to SNOMED Mappings1_0 "+Mappings1_0.size());
        System.out.println("Update RxNorm to SNOMED Mappings1_1 "+Mappings1_1.size());
        System.out.println("Update RxNorm to SNOMED Mappings1_N "+Mappings1_N);
        
        System.out.println("Update SNOMED to RxNorm SNOMEDMappings1_0 "+SNOMEDMappings1_0.size());
        System.out.println("Update SNOMED to RxNorm SNOMEDMappings1_1 "+SNOMEDMappings1_1.size());
        System.out.println("Update SNOMED to RxNorm SNOMEDMappings1_N "+SNOMEDMappings1_N.size());
        return RxNormMedicinalProduct;
       

    
	}

}
