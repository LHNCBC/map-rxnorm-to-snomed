package bossAndAIsubdivision;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
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
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import utilitaries.OntologyClassManagement;

public class RefineBossAIErrors {
	  private static OWLOntology Ontology;

      private static OWLReasoner Elkreasoner;
      private static OWLOntologyManager manager;
      
	public  RefineBossAIErrors (){
		// TODO Auto-generated method stub
		//getBossrelation();
		String path ="AuditRxNorm2SNOMED.owl";
		File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/"+path);
		
			
		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		  Elkreasoner = classMana.getElkreasoner();
		  manager=classMana.getManager();
		 Ontology=classMana.getOntology();
		 getBossOrSAIrelation();
		//String code ="725658002";
//		String code ="Rx1297766";
//		Set<String> codes = new HashSet<String>();
//		codes.add(code);
//		//System.out.println(getModificationBase(code));
//		System.out.println(getListOfSubstanceAndBaseFromRxNormside(codes));

	}
	public static void SaveOntology(String path, Set<OWLAxiom>ensembleAxiom) throws OWLOntologyStorageException {
		Stream<OWLAxiom> resul=ensembleAxiom.stream();
	   	 
		manager.addAxioms(Ontology, resul);
		File OntologyPath2 = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/"+path);
        manager.saveOntology(Ontology, IRI.create(OntologyPath2.toURI()));
	}
	public static void getBossOrSAIrelation() {
		
		 Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
		 
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
         OWLDataFactory  factory = manager.getOWLDataFactory();
         OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
         OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
         
         Set<OWLClass> MedicinalProducts=Elkreasoner.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
         MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) 
               	 RxNirMedicinalProducts.add(k);
                  
         });
        
         Set<OWLAxiom> Axioms= new HashSet<OWLAxiom>();
         RxNirMedicinalProducts.forEach(a->{
        	 Set<OWLAnnotation> annot= new HashSet<OWLAnnotation>();
             
        	 Boolean OnlySemantic=true;
        	 Boolean Mapped=true;
        	 for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(a, Ontology, prop).collect(Collectors.toSet())) {
        		 annot.add(d);
        			 OWLAnnotationValue j =d.annotationValue();
 				OWLLiteral vv= (OWLLiteral) j;
 						String errorTag= vv.getLiteral();
 						if (errorTag.startsWith("MAI")) {
 							OnlySemantic = false;
 						} 
 						if (errorTag.startsWith("SSU")) {
 							Mapped=false;
 						}
 						
        	 }
        	 if(OnlySemantic) {
        		 if(Mapped) {
        		for(OWLAnnotation d: annot) {
        			OWLAnnotationValue j =d.annotationValue();
     				OWLLiteral vv= (OWLLiteral) j;
     						String errorTag= vv.getLiteral();
     						
        			if (errorTag.startsWith("BOS")) {
        				Set<String> rxNormIngredientBoss = new HashSet<String>();
        				Set<String> SNOMEDsubstBoss = new HashSet<String>();
 							String[] error = errorTag.split("with|on");
 							String setRx=error[2].trim();
 							String[] rx=setRx.split(" ");
 							for(int i=0; i<rx.length; i++) {
 								rxNormIngredientBoss.add(rx[i].trim());
 							}
 							String setSNOMED=error[3].trim();
 							String[] snom=setSNOMED.split(" ");
 							for(int u=0; u<snom.length; u++) {
 								SNOMEDsubstBoss.add(snom[u].trim());
 							}
 							
 							Axioms.addAll(getmodificationRelationorNorbetweenBossorAI(a, rxNormIngredientBoss, SNOMEDsubstBoss, "Boss"));
 						}
 						if (errorTag.startsWith("SAI")) {
 							Set<String> rxNormIngredientAI = new HashSet<String>();
 	        				Set<String> SNOMEDsubstAI = new HashSet<String>();
 	 							String[] error = errorTag.split("with|on");
 	 							String setRx=error[2].trim();
 	 							String[] rx=setRx.split(" ");
 	 							for(int i=0; i<rx.length; i++) {
 	 								rxNormIngredientAI.add(rx[i].trim());
 	 							}
 	 							String setSNOMED=error[3].trim();
 	 							String[] snom=setSNOMED.split(" ");
 	 							for(int i=0; i<snom.length; i++) {
 	 								SNOMEDsubstAI.add(snom[i].trim());
 	 							}
 	 							Axioms.addAll(getmodificationRelationorNorbetweenBossorAI(a, rxNormIngredientAI, SNOMEDsubstAI, "SAI"));	
 						}
        		}
        	 }
        	 }
        	 //System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++Final+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
         	
         });
         String pathSave ="AuditRxNorm2SNOMED.owl";
 		try {
			SaveOntology(pathSave, Axioms);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
         
         
         
         
		
	}
	public static Set<OWLAxiom> getmodificationRelationorNorbetweenBossorAI(OWLClass RxNormToTag, Set<String> RxNormBossOrAI,Set<String> SNOMEDBossOrAI,String BossOrAI) {
		Map<OWLClass, Set<OWLClass>> SNOMEDCTsubstanceAndBase =getListOfSubstanceAndBaseForSNOMEDside(SNOMEDBossOrAI);
		Map<OWLClass, Set<OWLClass>> RxNormAndBase =getListOfSubstanceAndBaseFromRxNormside(RxNormBossOrAI);
		System.out.println(BossOrAI+" "+RxNormToTag+" RxNormAndBase "+RxNormAndBase);
		System.out.println(BossOrAI+" "+RxNormToTag+" SNOMEDCTsubstanceAndBase "+SNOMEDCTsubstanceAndBase);
		Set<OWLAxiom> resultFil= new HashSet<OWLAxiom>();
		Map<String, Set<String>> SNOMEDCTsubstanceAndBaseString =OWLCLassToString(SNOMEDCTsubstanceAndBase);
		Map<String, Set<String>> RxNormAndBaseString =OWLCLassToString(RxNormAndBase);
		Set<String> rexNrelatedWithModification= new HashSet<String>();
		Set<String> SNOMEDrelatedWithModification= new HashSet<String>();
		Set<String> SNOMrestant= new HashSet<String>();
		Set<String> RxNorrestant= new HashSet<String>();
		
//		Set<String> rexNrelatedAsBase= new HashSet<String>();
//		Set<String> SNOMEDrelatedAsModification= new HashSet<String>();
		
		RxNormAndBaseString.forEach((rx,Base)->{
			for(String po:Base) {
				if(SNOMEDCTsubstanceAndBaseString.keySet().contains(po)) {
					rexNrelatedWithModification.add(rx);
					SNOMEDrelatedWithModification.add(po);
				}
			
			}
			
		});
		SNOMEDCTsubstanceAndBaseString.forEach((sn,Base)->{
			for(String po:Base) {
				if(RxNormAndBaseString.keySet().contains(po)) {
					rexNrelatedWithModification.add(po);
					SNOMEDrelatedWithModification.add(sn);
				}
			
			}
		});
		
		for(String aj : SNOMEDCTsubstanceAndBaseString.keySet()) {
			if(!SNOMEDrelatedWithModification.contains(aj)) {
				SNOMrestant.add(aj);
			}
		}
System.out.println(" RxNormAndBaseString.keySet() "+RxNormAndBaseString.keySet());
System.out.println(" RxNorrestant "+RxNorrestant);
		for(String aj : RxNormAndBaseString.keySet()) {
			if(!rexNrelatedWithModification.contains(aj)) {
				RxNorrestant.add(aj);
			}
		}
		Set<OWLAxiom> rrr = getTagAxiomModifications(RxNormToTag, rexNrelatedWithModification, SNOMEDrelatedWithModification, BossOrAI);
		Set<OWLAxiom> rrr2 = getTagAxiomNoModifications(RxNormToTag, RxNorrestant, SNOMrestant, BossOrAI);
//		System.out.println("rrr "+rrr);
//		System.out.println("rrr2 "+rrr2);
		resultFil.addAll(rrr);
		resultFil.addAll(rrr2);
		return resultFil;
	}
	
	public static Set<OWLAxiom> getTagAxiomNoModifications(OWLClass classtoTag, Set<String> Ingredient, Set<String> Substance,String BossOrAI){
		 Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		OWLDataFactory  factory = manager.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		  OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
		  String anno="NOMOD-"+BossOrAI+": ";
		  Boolean a= true;
		  for(String ing:Ingredient ) {
			  if(!ing.equals("")) {
			  anno=anno+ing+" ";
			  a=false;
			  }
		  }
		  anno=anno+" with ";
		  for(String sub:Substance) {
			  if(!sub.equals("")) {
				  anno=anno+sub+" ";
			  }
		  }
		  
		 if(a) {
			 anno="";
		 }
		 if(!anno.equals("")) {
		  
		  OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
 		 OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(classtoTag.getIRI(), annotation);
 		 ensembleAxiom.add(ax1);
		 }
 		 return ensembleAxiom;
	      
	}
	
	public static Set<OWLAxiom> getTagAxiomModifications(OWLClass classtoTag, Set<String> Ingredient, Set<String> Substance,String BossOrAI){
		 Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		OWLDataFactory  factory = manager.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		  OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
		  String anno="MOD-"+BossOrAI+": ";
		  Boolean a= true;
		  for(String ing:Ingredient ) {
			  if(!ing.equals("")) {
			  anno=anno+ing+" ";
			  a=false;
			  }
		  }
		  anno=anno+"modification relation with ";
		  for(String sub:Substance) {
			  if(!sub.equals("")) {
			  anno=anno+sub+" ";
			  }
		  }
		  
		  if(a) {
				 anno="";
			 }
			 if(!anno.equals("")) {
		  
		  OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
  		 OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(classtoTag.getIRI(), annotation);
  		 ensembleAxiom.add(ax1);
			 }
  		 return ensembleAxiom;
	      
	}
	public static Map<String, Set<String>>  OWLCLassToString( Map<OWLClass, Set<OWLClass>>  resu){
		Map<String, Set<String>>  rtty= new HashMap<String, Set<String>>();
		
		resu.forEach((a,b)->{
			if (!rtty.containsKey(a.getIRI().getShortForm())) {
				rtty.put(a.getIRI().getShortForm(), new HashSet<String>());
			}
			for(OWLClass az:b) {
				rtty.get(a.getIRI().getShortForm()).add(az.getIRI().getShortForm());
			}
		});
		
		
		
		
		return rtty;
	}
	public static Map<OWLClass, Set<OWLClass>> getListOfSubstanceAndBaseForSNOMEDside(Set<String> code) {
		Map<OWLClass, Set<OWLClass>> result = new HashMap<OWLClass, Set<OWLClass>>();
		OWLDataFactory  factory = manager.getOWLDataFactory();
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
	        
		for(String ocd :code) {
			  
			  OWLClass substance=factory.getOWLClass(ocd,pm);
			  if(!result.containsKey(substance)) {
				  result.put(substance, new HashSet<OWLClass>());
			  }
			  Set<OWLClass> Base=getModificationBase(ocd);
			  Set<OWLClass> rxNormBase= new HashSet<OWLClass>();
			  
			  for (OWLClass dsa :Base) {
				 Set<OWLClass> rer= Elkreasoner.getEquivalentClasses(dsa).entities().collect(Collectors.toSet());
				 
				 rer.forEach(a->{
					 if(a.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
						 rxNormBase.add(a);
					 }
				 });
			  }
			  result.get(substance).addAll(Base);
			  result.get(substance).addAll(rxNormBase);
		}
	
		return result;
	}
	
	public static Map<OWLClass, Set<OWLClass>> getListOfSubstanceAndBaseFromRxNormside(Set<String> codes) {
		Map<OWLClass, Set<OWLClass>> result = new HashMap<OWLClass, Set<OWLClass>>();
		OWLDataFactory  factory = manager.getOWLDataFactory();
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		 
		 for(String code: codes) {
			  OWLClass Rxsubstance=factory.getOWLClass(code,pm);
			Set<OWLClassExpression> relatedSNom= EntitySearcher.getEquivalentClasses(Rxsubstance, Ontology).collect(Collectors.toSet());
			Set<OWLClass> related= new HashSet<OWLClass>();
			Set<OWLClass> relatedSNOMED= new HashSet<OWLClass>();
			for(OWLClassExpression inter:relatedSNom ) {
					if(!inter.isAnonymous()) {
						OWLClass az= inter.asOWLClass();
						related.add(az);
					}
				
			}
			related.forEach(ac->{
				if(!ac.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
					relatedSNOMED.add(ac);
				 }
			});
			
			if(!result.containsKey(Rxsubstance)) {
				  result.put(Rxsubstance, new HashSet<OWLClass>());
			  }
			Set<OWLClass> Base = new HashSet<OWLClass>();
			for(OWLClass caze : relatedSNOMED) {
				Base.addAll(getModificationBase(caze.getIRI().getShortForm()));
		
				
			}
			
			 result.get(Rxsubstance).addAll(Base);
		 }
		
		return result;
	}
	
	public static Set<OWLClass> getModificationBase(String code) {
		Set<OWLClass>  results = new HashSet<OWLClass>();
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
         OWLDataFactory  factory = manager.getOWLDataFactory();
         OWLObjectProperty getModification = factory.getOWLObjectProperty("http://snomed.info/id/738774007");
         
         OWLClass substance=factory.getOWLClass(code,pm);
         //System.out.println(" substance "+substance);
         for(OWLClassExpression anno:EntitySearcher.getSuperClasses		 
        		 (substance, Ontology).collect(Collectors.toSet())) {
			//System.out.println(anno);
			Set<OWLClass> steps = new HashSet<OWLClass>();
			if(anno.isAnonymous()) {
        	 Set<OWLClass> steeps = resultSpecificRelation(anno, getModification);
        	 steps.addAll(steeps);
			}
        	 
        	 results.addAll(steps);
        	 if(steps.size()>0) {
        		for(OWLClass aer : steps) {
        			results.addAll(getModificationBase(aer.getIRI().getShortForm()));
        		}
        	 }
				
			}
         return results;
        
		
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
