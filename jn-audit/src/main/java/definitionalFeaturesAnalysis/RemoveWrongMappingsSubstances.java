package definitionalFeaturesAnalysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import utilitaries.OntologyClassManagement;

public class RemoveWrongMappingsSubstances {
	private static OWLOntology Ontology;

    private static OWLReasoner Elkreasoner;
    private static OWLOntologyManager manager;
    /**
     * Analyzing process of the ingredients mappings 
     * Create a CSV file containing all the ingredients related with multiples SNOMED CT substances as mappings. 
     * - SU: Suppressed wrong mappings
     * - VA: validated mappings
     * - NI: mappings not disambiguated
     * @param args
     * @throws FileNotFoundException
     * @throws OWLOntologyStorageException
     */
	public static void main(String[] args) throws FileNotFoundException, OWLOntologyStorageException {
		// TODO Auto-generated method stub
		
		//File OntologyPath = new File ("./Final/RxNorm2SnomedDecember.owl");
		File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/RxNorm2Snomed_2019-12-20_15-37-40/RxNorm2Snomed_2019-12-20_15-37-40.owl");;
		//File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/RxNorm2SnomedJanuary.owl");
	
        
		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		 Elkreasoner = classMana.getElkreasoner();
		 manager=classMana.getManager();
		 Ontology=classMana.getOntology();
		 getRxNormSubstancesMultipleMappingAnalysis();

	}
	/**
	 * Retrieve the altlabel of SNOMED CT substances
	 * @param RxNorm
	 * @param RelatedSNOMED
	 * @param label
	 * @return
	 */
	public static  Set<String>  getRxNormSubstancesAltLabelMapping(OWLClass RxNorm, Set<OWLClass>RelatedSNOMED, String label) {
		
        OWLDataFactory  factory = manager.getOWLDataFactory();
        Set<String> resul = new HashSet<String>();
        
        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("http://www.w3.org/2004/02/skos/core#altLabel");
		Map<String,Set<String>> LabelAndCode = new HashMap<String, Set<String>>();
		if(!LabelAndCode.containsKey(label)) {
       		LabelAndCode.put(label, new HashSet<String>());
       	}
       	LabelAndCode.get(label).add(RxNorm.getIRI().getShortForm());
		RelatedSNOMED.forEach(snID->{
			 
        	String LabelSnomed="";
        	for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(snID, Ontology, prop).collect(Collectors.toSet())) {
        		OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				LabelSnomed= vv.getLiteral();
				LabelSnomed= LabelSnomed.replace("-", " ");
				if(!LabelSnomed.contains("(substance)")) {
					LabelSnomed=LabelSnomed.trim();
					LabelSnomed=LabelSnomed+" (substance)";
	           	}
				LabelSnomed=LabelSnomed.toLowerCase();
               	if(!LabelAndCode.containsKey(LabelSnomed)) {
               		LabelAndCode.put(LabelSnomed, new HashSet<String>());
               	}
               	LabelAndCode.get(LabelSnomed).add(snID.getIRI().getShortForm());
           	}
			
		});
		
		Boolean oups=false;
       	String labelValidated="";
       	Set<String> relatedIntroduced = new HashSet<String>();
       	for(String lab: LabelAndCode.keySet()) {
       		Set<String> mapp= LabelAndCode.get(lab);
       		
       		if(mapp.size()>1) {
       			//System.out.println(az.getIRI().getShortForm()+" 2:2 "+labelRxNorm+" ==> "+lab+" "+LabelAndCode.get(lab));
       			if(mapp.contains(RxNorm.getIRI().getShortForm())) {
       				//System.out.println(az.getIRI().getShortForm()+" : "+labelRxNorm+" ==> "+lab+" "+mapp);
       				for(String mp:mapp) {
       					if(!mp.equals(RxNorm.getIRI().getShortForm())) {
       						System.out.println(RxNorm.getIRI().getShortForm()+" IN : "+label+" ==> "+lab+" "+mp);
       						if(!relatedIntroduced.contains(mp)) {
       						resul.add(RxNorm.getIRI().getShortForm()+";"+label.replace("(substance)", "").trim()+";"+mp+";"+lab+";"+"Validated mapping\n");
       						relatedIntroduced.add(mp);
       						}
       						//"RxCUI;RxNormLabel;SCTID;SCTLabel;Tag";
       						oups = true;
       						labelValidated=lab;
       						
       					}
       				}
       			}
       		}
       	}
       	if(oups) {
       		for(String lab: LabelAndCode.keySet()) {
       			if(!lab.equals(labelValidated)) {
       				for(String apo:LabelAndCode.get(lab)) {
       				System.out.println(RxNorm.getIRI().getShortForm()+" : "+label+" <==> "+lab+" "+apo);
       				if(!relatedIntroduced.contains(apo)) {
       				resul.add(RxNorm.getIRI().getShortForm()+";"+label.replace("(substance)", "").trim()+";"+apo+";"+lab+";"+"Suppressed wrong mapping\n");
       				relatedIntroduced.add(apo);
						}	
       				}
       			}
       		}
       	}
       	else {
       		
       		           		
       		
       		for(String lab: LabelAndCode.keySet()) {
       			if(!lab.equals(labelValidated)) {
       				for(String apo:LabelAndCode.get(lab)) {
       					System.out.println(RxNorm.getIRI().getShortForm()+" : "+label+" <=><=> "+lab+" "+apo);
       					if(!apo.equals(RxNorm.getIRI().getShortForm())) {
       					if(!relatedIntroduced.contains(apo)) {	
       					resul.add(RxNorm.getIRI().getShortForm()+";"+label.replace("(substance)", "").trim()+";"+apo+";"+lab+";"+"Mappings not disambiguted\n");
       					relatedIntroduced.add(apo);
   						}
       					}
       				}
       			}
       		}
       		
       	}
	return resul;
		
	}
	/**
	 * Retrieve the list of RxNorm ingredients mapped with multiple substances in SNOMED CT 
	 * Remove the wrong mappings in the OWL File
	 * @throws FileNotFoundException
	 * @throws OWLOntologyStorageException
	 */
	public static void getRxNormSubstancesMultipleMappingAnalysis() throws FileNotFoundException, OWLOntologyStorageException {

    	Set<OWLClass> RxNormsubstances = new HashSet<OWLClass>();
    	Map<OWLClass, Set<OWLClass>> RxNormMultipleSNOMED = new HashMap<OWLClass, Set<OWLClass>>();
    	
    	Set<OWLClass> RepeatedSNOMED = new HashSet<OWLClass>();
    	   PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
    	
           OWLDataFactory  factory = manager.getOWLDataFactory();
       
    	 OWLClass substanceHierarchy=factory.getOWLClass("105590001",pm);
         Set<OWLClass> Substances=Elkreasoner.getSubClasses(substanceHierarchy, false).entities().collect(Collectors.toSet());
         Substances.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {RxNormsubstances.add(k);}});
         
    	
        RxNormsubstances.forEach((k)->{Set<OWLClass> eq=Elkreasoner.getEquivalentClasses(k).entities().collect(Collectors.toSet());
        Set<OWLClass> eqs = new HashSet<OWLClass>();
        eq.forEach((l)->{if(!l.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {eqs.add(l);}});
        //SnomedMapped.addAll(eqs);
        if(eqs.size()>1) {
        	if(!RxNormMultipleSNOMED.containsKey(k)) {
        		RxNormMultipleSNOMED.put(k, new HashSet<OWLClass>());
        	}
        	RxNormMultipleSNOMED.get(k).addAll(eqs);
        	
        };
       
        });
        System.out.println(" list of erroneous mappings "+RxNormMultipleSNOMED.keySet().size());
        String resultFinal = "RxCUI;RxNormLabel;SCTID;SCTLabel;Tag\n";
        Set<String> resul = new HashSet<String>();
        RxNormMultipleSNOMED.forEach((az,po)->{
        	 Map<String,Set<String>> LabelAndCode = new HashMap<String, Set<String>>();
        	 Set<OWLClass> SnomedMapped = new HashSet<OWLClass>();
         	
        	 SnomedMapped.addAll(po);
        	String labelRxNorm="";
           	for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(az, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
    				//label
    				OWLAnnotationValue j =a.annotationValue();
    				OWLLiteral vv= (OWLLiteral) j;
    				labelRxNorm= vv.getLiteral();
    				//label=a.toString();
    			}
           	labelRxNorm=labelRxNorm.replace("(Fake)", "(substance)") ;
           	if(!labelRxNorm.contains("(substance)")) {
           		labelRxNorm=labelRxNorm.trim();
           		labelRxNorm=labelRxNorm+" (substance)";
           	}
           	labelRxNorm= labelRxNorm.replace("-", " ");
           	labelRxNorm=labelRxNorm.toLowerCase();
          	if(!LabelAndCode.containsKey(labelRxNorm)) {
           		LabelAndCode.put(labelRxNorm, new HashSet<String>());
           	}
           	LabelAndCode.get(labelRxNorm).add(az.getIRI().getShortForm());
           	Set<String> relatedSNOMED = new HashSet<String>();
           	po.forEach(tre->{
           		relatedSNOMED.add(tre.getIRI().getShortForm());
           		String labelSNOMED="";
               	for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(tre, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
        				//label
        				OWLAnnotationValue j =a.annotationValue();
        				OWLLiteral vv= (OWLLiteral) j;
        				labelSNOMED= vv.getLiteral();
        				//label=a.toString();
        			}
               	labelSNOMED= labelSNOMED.replace("-", " ");
               	labelSNOMED=labelSNOMED.toLowerCase();
               	if(!LabelAndCode.containsKey(labelSNOMED)) {
               		LabelAndCode.put(labelSNOMED, new HashSet<String>());
               	}
               	LabelAndCode.get(labelSNOMED).add(tre.getIRI().getShortForm());
           	});
           
           	Boolean oups=false;
           	String labelValidated="";
           	for(String lab: LabelAndCode.keySet()) {
           		Set<String> mapp= LabelAndCode.get(lab);
           		
           		if(mapp.size()>1) {
           			//System.out.println(az.getIRI().getShortForm()+" 2:2 "+labelRxNorm+" ==> "+lab+" "+LabelAndCode.get(lab));
           			if(mapp.contains(az.getIRI().getShortForm())) {
           				//System.out.println(az.getIRI().getShortForm()+" : "+labelRxNorm+" ==> "+lab+" "+mapp);
           				for(String mp:mapp) {
           					if(!mp.equals(az.getIRI().getShortForm())) {
           						System.out.println(az.getIRI().getShortForm()+" : "+labelRxNorm+" ==> "+lab+" "+mp);
           						resul.add(az.getIRI().getShortForm()+";"+labelRxNorm.replace("(substance)", "").trim()+";"+mp+";"+lab+";"+"Validated ampping\n");
           						//"RxCUI;RxNormLabel;SCTID;SCTLabel;Tag";
           						oups = true;
           						labelValidated=lab;
           						
           					}
           				}
           			}
           		}
           		//System.out.println(az.getIRI().getShortForm()+" : "+labelRxNorm+" ==> "+lab+" "+LabelAndCode.get(lab));
           	}
           	if(oups) {
           		for(String lab: LabelAndCode.keySet()) {
           			if(!lab.equals(labelValidated)) {
           				for(String apo:LabelAndCode.get(lab)) {
           				System.out.println(az.getIRI().getShortForm()+" : "+labelRxNorm+" <==> "+lab+" "+apo);
           				resul.add(az.getIRI().getShortForm()+";"+labelRxNorm.replace("(substance)", "").trim()+";"+apo+";"+lab+";"+"Suppressed wrong mapping\n");
   						
           				}
           			}
           		}
           	}
           	else {
           		
           			resul.addAll(getRxNormSubstancesAltLabelMapping(az, SnomedMapped, labelRxNorm));
				
           		
           		
//           		for(String lab: LabelAndCode.keySet()) {
//           			if(!lab.equals(labelValidated)) {
//           				for(String apo:LabelAndCode.get(lab)) {
//           					System.out.println(az.getIRI().getShortForm()+" : "+labelRxNorm+" <=><=> "+lab+" "+apo);
//           					if(!apo.equals(az.getIRI().getShortForm())) {
//           					resul.add(az.getIRI().getShortForm()+";"+labelRxNorm.replace("(substance)", "").trim()+";"+apo+";"+lab+";"+"NI\n");
//           					}
//           				}
//           			}
//           		}
           		
           	}
           	
        });
        
        for(String result:resul ) {
        	resultFinal=resultFinal+result;
        }
        Set<OWLAxiom> axiomsToRemove = new HashSet<OWLAxiom>();
        for(String result:resul ) {
        	if(result.endsWith("Suppressed wrong mapping\n")) {
        		System.out.println("result "+result);
        		//Ontology.removeAxiom(ax);
        		String[] a=result.split(";");
  				 String rx=a[0].trim();
  				 String Msonmed=a[2].trim();
  				 OWLClass substanceRxNorm = factory.getOWLClass(rx,pm);
  				OWLClass l = factory.getOWLClass(Msonmed,pm);
  				OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(substanceRxNorm,l);
  				axiomsToRemove.add(ax);
  				 System.out.println(" remove mapping Msonmed "+rx+" ;;;; Msonmed "+Msonmed);
        	}
        }
       Stream<OWLAxiom>  aer=axiomsToRemove.stream();
       manager.removeAxioms(Ontology, aer);
       File OntologyPath2 = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedSuppressedWrongMappings.owl");
		
       manager.saveOntology(Ontology, IRI.create(OntologyPath2.toURI()));
        try (PrintWriter out = new PrintWriter("/git/MapRxNormToSnomed/Audit/Livrable/File/FileCSV/MultipleIngredientsMappings.csv")) {
     	    out.println(resultFinal);
     	}
        
        
       // System.out.println("MultipleMappings : "+MultipleMappings.size());
        
       // System.out.println("RepeatedSNOMED "+RepeatedSNOMED.size()+" "+RepeatedSNOMED);
        //return MultipleMappings;
       

    }

}
