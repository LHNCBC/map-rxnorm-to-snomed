package inferredAndAssertedComparison;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
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

import utilitaries.Couple;
import utilitaries.OntologyClassManagement;

public class ComparedAssertedAndInferredBeforeAndAfterAuditingProcess {
	
      public static Set<Couple> AssertedMappings= new HashSet<Couple>();


    
/**
 * find the the difference between the mappings infered and asserted before and after the auditing process
 * @param args
 */
	public static void main(String[] args) {
		
           getAssertedMapping();
           
           Set<Couple> initial= commonMappingStep("initial");
           Set<Couple> finalCommon= commonMappingStep("b");
           System.out.println("list of initial mappings AI not retrieve");
           System.out.println(" finalCommon "+finalCommon.size()+" initial "+initial.size());
          
           comparisonOfDifferentSteps(initial, finalCommon);
           System.out.println("list of final mappings AI originally missed");
           comparisonOfDifferentSteps(finalCommon,initial);
      

	}
	
	public static Set<Couple> commonMappingStep(String a){
		Set<Couple> mapping = new HashSet<Couple>();
		String path="";
		if(a.equals("initial")) {
		 path = "/Audit/Livrable/RxNorm2Snomed_2019-12-20_15-37-40/RxNorm2Snomed_2019-12-20_15-37-40.owl";
		}
		else {
			 path = "/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedToAnalyse.owl";	
		}
		File OntologyPath = new File (path);	
		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		OWLReasoner Elkreasoner = classMana.getElkreasoner();
		OWLOntologyManager manager=classMana.getManager();
		OWLOntology Ontology=classMana.getOntology();
		mapping=getInferredMappings(Elkreasoner, manager, Ontology);
		getCommonMappings(mapping);
				
				return mapping;
	}
	public static void comparisonOfDifferentSteps(Set<Couple> commonInitial, Set<Couple> CommonFinal) {
		Set<Couple> AIlost= new HashSet<Couple>();
		System.out.println("SCTID;RxCUI");
		for(Couple a: commonInitial) {
			if(!CommonFinal.contains(a)) {
				AIlost.add(a);
				System.out.println(a.x+";"+a.y);
			}
		}
		System.out.println(" AIlost "+AIlost.size());
		
	}
	public static void getCommonMappings(Set<Couple> InferredMappings) {
		Set<Couple> CommonMappings= new HashSet<Couple>();
			AssertedMappings.forEach(a->{
				//System.out.println(a.x+" ; "+a.y);
				if(InferredMappings.contains(a)) {
					CommonMappings.add(a);
					
				}
				
			});
			
			System.out.println("CommonMappings "+CommonMappings.size());
	}
	
	/**
	 * get the inferred mappings induced by our process between RxCUI and SCTID clinical drugs 
	 * @return
	 */
	public static Set<Couple> getInferredMappings(OWLReasoner Elkreaso, OWLOntologyManager manag, OWLOntology Ontol) {
		Set<Couple> InferredMappings= new HashSet<Couple>();

     	Map<OWLClass, Set<OWLClass>> MappingsSCD= new HashMap<OWLClass, Set<OWLClass>>();
     	Map<OWLClass, Set<OWLClass>> MappingsSCDOneToOne= new HashMap<OWLClass, Set<OWLClass>>();
 		
     	Map<OWLClass, Set<OWLClass>> MappingsSCDOneToMany= new HashMap<OWLClass, Set<OWLClass>>();
     	Map<OWLClass, Set<OWLClass>> MappingsSCDOneToZero=new HashMap<OWLClass, Set<OWLClass>>();
 		
 		
         Set<OWLClass> RxNormMedicinalProductWithVaccines = new HashSet<OWLClass>();
         PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
         OWLDataFactory  factory = manag.getOWLDataFactory();
         OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
         Set<OWLClass> MedicinalProducts=Elkreaso.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
         MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {RxNormMedicinalProductWithVaccines.add(k);}});
         
         System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProductWithVaccines.size());
         Set<OWLClass> RxNormMedicinalProduct = new HashSet<OWLClass>();
         Set<OWLClass> Vaccines = new HashSet<OWLClass>();
         RxNormMedicinalProductWithVaccines.forEach((d)->{
         	String label="";
         	//Stream<OWLAnnotation> a=EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel());
         	for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(d, Ontol, factory.getRDFSLabel()).collect(Collectors.toSet())) {
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
         
         
         RxNormMedicinalProduct.forEach((d)->{
         	
         	 Set<OWLClass> enseEq= new HashSet<OWLClass>();
         	Set<OWLClass> eq=	Elkreaso.getEquivalentClasses(d).entities().collect(Collectors.toSet());
         	eq.forEach((yu)->{
         		if(!yu.getIRI().toString().equals(d.getIRI().toString())) {enseEq.add(yu);}
         	});
          	if(!MappingsSCD.containsKey(d)) {
         		MappingsSCD.put(d, enseEq);
         	}
         	
         });
         Set<OWLClass> SNOMEDMapped = new HashSet<OWLClass>();
         Set<OWLClass> RxNormMappingOnetoOneProblem = new HashSet<OWLClass>();
         Set<OWLClass> RxNormMappingOnetoManyProblem = new HashSet<OWLClass>();
         MappingsSCD.forEach((f,op)->{
        	 String b= f.getIRI().getShortForm();
         	if(op.size()==1) {
         		MappingsSCDOneToOne.put(f, op);
         		op.forEach(az->{
         			if(az.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
         				RxNormMappingOnetoOneProblem.add(f);
         				//System.out.println("f "+f+", "+az);
         			}
         			else {
         				SNOMEDMapped.add(az);
         				String a = az.getIRI().getShortForm();
         				//System.out.println(" a "+a+" b "+b);
         				Couple cp= new Couple(a, b);
         				InferredMappings.add(cp);
         			}
         		});
         	}
         	else if(op.size()>1) {
         		MappingsSCDOneToMany.put(f, op);
         		op.forEach(az->{
         			if(az.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
         				RxNormMappingOnetoManyProblem.add(f);
         			}
         			else {
         				SNOMEDMapped.add(az);
         				String a = az.getIRI().getShortForm();
         				//System.out.println(" a "+a+" b "+b);
         				Couple cp= new Couple(a, b);
         				InferredMappings.add(cp);
         			}
         		});
         		//System.out.println("f "+f+", "+op);
         	}
         	else if(op.size()==0) {
         		MappingsSCDOneToZero.put(f, op);
         	}
         }
         
         		);
         
        System.out.println("SNOMEDMapped.add(az); "+SNOMEDMapped.size());
         System.out.println("InferredMappings "+InferredMappings.size());
        System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProduct.size());
         System.out.println("MappingsSCDOneToOne "+MappingsSCDOneToOne.size());
         System.out.println("RxNormMappingOnetoOneProblem "+RxNormMappingOnetoOneProblem.size());
         System.out.println("MappingsSCDOneToMany "+MappingsSCDOneToMany.size());
         System.out.println("RxNormMappingOnetoManyProblem "+RxNormMappingOnetoManyProblem.size());
         System.out.println("MappingsSCDOneToZero "+MappingsSCDOneToZero.size());
         return InferredMappings;
        

     }
	public static void getAssertedMapping() {
		 Set<OWLClass> SNOMEDclinicalDrugs = new HashSet<OWLClass>();
		 Set<OWLClass> SNOMEDMedicinalProducts = new HashSet<OWLClass>();
		 Set<String> RxNirMedicinalProducts = new HashSet<String>();
		 Map<OWLClass, Set<OWLClass>> sertedMappings= new HashMap<OWLClass, Set<OWLClass>>();
		 File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedToAnalyse.owl");
			OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
			OWLReasoner Elk = classMana.getElkreasoner();
			OWLOntologyManager man=classMana.getManager();
			OWLOntology Onto=classMana.getOntology();
		 
        
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
        OWLDataFactory  factory = man.getOWLDataFactory();
        
        OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 
        
        OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
        
        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("MapsToCodeAsserted",pm);
       
        Set<OWLClass> MedicinalProducts=Elk.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
        MedicinalProducts.forEach((k)->{if(!k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) 
        {SNOMEDMedicinalProducts.add(k);
        }else {
       	 RxNirMedicinalProducts.add(k.getIRI().getShortForm());
        }
        
        });
        
        SNOMEDMedicinalProducts.forEach(al->{
       	 Stream<OWLClassExpression> eq= EntitySearcher.getEquivalentClasses(al, Onto);
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
        
        SNOMEDclinicalDrugs.forEach(a->{

       		 
       		 
       		 Set<OWLClass> related= new HashSet<OWLClass>();
       		 
       	 for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(a, Onto, prop).collect(Collectors.toSet())) {
       		 //System.out.println("d "+a+" "+d);
       		 OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
						String RxNorm= vv.getLiteral();
						if(RxNorm.startsWith("Rx")) {
							 OWLClass rx1=factory.getOWLClass(RxNorm,pm);
							 if(RxNirMedicinalProducts.contains(RxNorm)) {
								 related.add(rx1);
							 }
							// System.out.println("d rx1 "+a+" "+rx1);
					        
						}
						else {
							OWLClass rx2=factory.getOWLClass("Rx"+RxNorm,pm);
							if(RxNirMedicinalProducts.contains("Rx"+RxNorm)) {
								 related.add(rx2);
							 }
							// System.out.println("d rx2 "+a+" "+rx2);
						}
       	 }
       	// if(!related.isEmpty()) {
           	 if(!sertedMappings.containsKey(a)) {
           		 sertedMappings.put(a, related);
       	// }
       		 
       	 }
        });
       	 
       	 Map<OWLClass, Set<OWLClass>> MappingsSCDOneToOne = new HashMap<OWLClass, Set<OWLClass>>();
       	 Map<OWLClass, Set<OWLClass>> MappingsSCDOneToMany = new HashMap<OWLClass, Set<OWLClass>>();
       	 Map<OWLClass, Set<OWLClass>> MappingsSCDOneToZero = new HashMap<OWLClass, Set<OWLClass>>();
            sertedMappings.forEach((f,op)->{
           	 //System.out.println(f+"f "+f.getIRI().getShortForm());
           	 String a=f.getIRI().getShortForm();
           	 op.forEach(t->{
           		String y=t.getIRI().getShortForm();
           		//System.out.println(a+" y "+y);
           		 Couple cp= new Couple(a, y);
           		 AssertedMappings.add(cp);
           	 });
            	if(op.size()==1) {
            		MappingsSCDOneToOne.put(f, op);
            		
            	}
            	else if(op.size()>1) {
            		MappingsSCDOneToMany.put(f, op);
            	
            	}
            	else if(op.size()==0) {
            		MappingsSCDOneToZero.put(f, op);
            	}
            
            }
            );
            Set<String> sno = new HashSet<String>();
            Set<String> RxNo= new HashSet<String>();
            AssertedMappings.forEach(aHH->{
            	sno.add(aHH.x);
            	RxNo.add(aHH.y);
            });
            System.out.println(" RxNo with asserted mapping "+RxNo.size());
            System.out.println(" sno with asserted mapping "+sno.size());
            System.out.println("asserted mapping"+AssertedMappings.size());
            System.out.println("SNOMEDclinicalDrugs for the asserted "+SNOMEDclinicalDrugs.size());
            System.out.println("MappingsSCDOneToOne "+MappingsSCDOneToOne.size());
            System.out.println("MappingsSCDOneToMany "+MappingsSCDOneToMany.size());
            System.out.println("MappingsSCDOneToZero "+MappingsSCDOneToZero.size());
          
     
		
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
