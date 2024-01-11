package gov.nih.nlm.mor.auditmap.definitionalFeaturesAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
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

import gov.nih.nlm.mor.auditmap.utilitaries.OntologyClassManagement;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

public class FindTheMissingIngredientsEquivalence {

	File OntologyPath = null;

	private static OntologyClassManagement classMana = null;
	private static OWLReasoner Elkreasoner = null;
	private static OWLOntologyManager manager= null;
	private static OWLOntology Ontology= null;
	private static ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	private static RestUrl restUrl = new RestUrl();

	//	public static void main(String[] args) throws FileNotFoundException, OWLOntologyStorageException {
	//		// TODO Auto-generated method stub
	//		File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedSuppressedWrongMappings.owl");
	//
	//		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
	//		OWLReasoner elk = classMana.getElkreasoner();
	//		OWLOntologyManager man=classMana.getManager();
	//		OWLOntology Ontology=classMana.getOntology();
	//		System.out.println("ok");
	//		//getRxNormListOfNecessaryIngredientsInSNOMED(man, Ontology, elk);
	//		
	//		/**
	//		 * Update the erroneous non-mapped substance and create the file containing the ingredients of RxNorm that must be used integrated in SNOMED
	//		 * it is necessary to update the file ontology use from RxNorm2SnomedDecember to RxNorm2SnomedDecemberSuppressedWrongMappings
	//		 */
	//				File Path = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedIngredientUpdate.owl");
	//				Set<OWLClass> ListofExtrat=getRxNormListOfExtratIngredientsInRxNorm(man, Ontology, elk);
	//				Set<OWLClass> listofNotMappedSubstance=getRxNormListOfNecessaryIngredientsInSNOMEDWithoutExtrat(man, Ontology, elk);
	//				getmappingsSubstanceNotDefinedByRxNorm(Path,listofNotMappedSubstance, ListofExtrat, man, Ontology, elk);
	//				System.out.println("finish");
	//		
	//	}

	public FindTheMissingIngredientsEquivalence(OntologyClassManagement management) {
		//		this.OntologyPath = new File (filename);
		this.classMana = management;
		this.Elkreasoner = classMana.getElkreasoner();
		this.manager = classMana.getManager();
		this.Ontology = classMana.getOntology();
	}

	public OntologyClassManagement run() {
		try {
			getRxNormListOfNecessaryIngredientsInSNOMED();	
			File Path = new File ("RxNorm2SnomedIngredientUpdate.owl");			
//			Set<OWLClass> ListofExtrat= getRxNormListOfExtratIngredientsInRxNorm(man, Ontology, elk);
//			Set<OWLClass> listofNotMappedSubstance=getRxNormListOfNecessaryIngredientsInSNOMEDWithoutExtrat(man, Ontology, elk);
//			getmappingsSubstanceNotDefinedByRxNorm(Path,listofNotMappedSubstance, ListofExtrat, man, Ontology, elk);
			
			Set<OWLClass> ListofExtrat= getRxNormListOfExtratIngredientsInRxNorm();
			Set<OWLClass> listofNotMappedSubstance=getRxNormListOfNecessaryIngredientsInSNOMEDWithoutExtrat();
			getmappingsSubstanceNotDefinedByRxNorm(Path,listofNotMappedSubstance, ListofExtrat);			
		} catch(Exception e) {
			System.out.println("Problem running FindTheMissingIngredientsEquivalence");
			e.printStackTrace();
		}

		classMana.setManager(manager);
		classMana.setOntology(Ontology);
		classMana.setElkreasoner(Elkreasoner);

		return classMana;

	}

	public static  Set<String>  getRxNormSubstancesHydrationOvercomingMapping(OWLOntologyManager manager,OWLOntology Ontology, Set<OWLClass>listToVerifie, Set<OWLClass> listSnomedSubstance) {


		Set<OWLAxiom> eq	= new HashSet<OWLAxiom>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = manager.getOWLDataFactory();
		Set<String> listOfENM = new HashSet<String>();
		Map<String,Set<String>> LabelAndCode = new HashMap<String, Set<String>>();
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("http://www.w3.org/2004/02/skos/core#altLabel");
		Set<String> SNOMEDModified= new HashSet<String>();
		Set<String> RxNormModified= new HashSet<String>();

		listSnomedSubstance.forEach(z->{

			//System.out.println("zzzzzzzzzzzzzzzzzzzzzzzzzzzz "+z);

			for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(z, Ontology,factory.getRDFSLabel() ).collect(Collectors.toSet())) {
				String LabelSnomed="";
				OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				LabelSnomed= vv.getLiteral();
				LabelSnomed= LabelSnomed.replace("-", " ");
				LabelSnomed = LabelSnomed.replaceAll("(?i),", ""); 
				if(!LabelSnomed.contains("(substance)")) {
					LabelSnomed=LabelSnomed.trim().replaceAll("\\s{2,}", " ");
					LabelSnomed=LabelSnomed+" (substance)";
				}
				String labeltest = LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");;
				LabelSnomed = LabelSnomed.replaceAll("(?i),", "");         
				LabelSnomed = LabelSnomed.replaceAll("(?i)(obsolete)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic-dibasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(dibasic)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dibasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(tribasic)", "");                       
				LabelSnomed = LabelSnomed.replaceAll("(?i)tribasic", "");                  
				LabelSnomed = LabelSnomed.replaceAll("(?i)(anhydrous)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");             
				LabelSnomed = LabelSnomed.replaceAll("(?i)anhydrous", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)trihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monohydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hemihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)tetrahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)pentahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dodecahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)sesquihydrate", "");                   
				LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hexahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)nonahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)oxyhydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hydrate", "");
				LabelSnomed=LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
				if(!LabelAndCode.containsKey(LabelSnomed)) {
					LabelAndCode.put(LabelSnomed, new HashSet<String>());
				}
				LabelAndCode.get(LabelSnomed).add(z.getIRI().getShortForm());
				if(!labeltest.equals(LabelSnomed)) {
					SNOMEDModified.add(z.getIRI().getShortForm());
					System.out.println("labeltest *"+labeltest+"* LabelSnomed *"+LabelSnomed+"*");

				}
			}

			for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(z, Ontology, prop).collect(Collectors.toSet())) {
				String LabelSnomed="";
				OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				LabelSnomed= vv.getLiteral();
				LabelSnomed= LabelSnomed.replace("-", " ");
				LabelSnomed = LabelSnomed.replaceAll("(?i),", ""); 
				if(!LabelSnomed.contains("(substance)")) {
					LabelSnomed=LabelSnomed.trim().replaceAll("\\s{2,}", " ");
					LabelSnomed=LabelSnomed+" (substance)";
				}
				String labeltest = LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
				LabelSnomed = LabelSnomed.replaceAll("(?i),", "");         
				LabelSnomed = LabelSnomed.replaceAll("(?i)(obsolete)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic-dibasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(dibasic)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dibasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(tribasic)", "");                       
				LabelSnomed = LabelSnomed.replaceAll("(?i)tribasic", "");                  
				LabelSnomed = LabelSnomed.replaceAll("(?i)(anhydrous)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");             
				LabelSnomed = LabelSnomed.replaceAll("(?i)anhydrous", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)trihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monohydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hemihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)tetrahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)pentahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dodecahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)sesquihydrate", "");                   
				LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hexahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)nonahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)oxyhydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hydrate", "");
				LabelSnomed=LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
				if(!LabelAndCode.containsKey(LabelSnomed)) {
					LabelAndCode.put(LabelSnomed, new HashSet<String>());
				}
				LabelAndCode.get(LabelSnomed).add(z.getIRI().getShortForm());
				if(!labeltest.equals(LabelSnomed)) {
					SNOMEDModified.add(z.getIRI().getShortForm());
					System.out.println("labeltest *"+labeltest+"* LabelSnomed *"+LabelSnomed+"*");

				}
			}




		});

		listToVerifie.forEach((d)->{
			String labelRxNom="";
			for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				//label
				OWLAnnotationValue j =a.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				labelRxNom= vv.getLiteral();
				//label=a.toString();
			}
			//	System.out.println("label "+labelRxNom);
			labelRxNom = labelRxNom.replace("()", "");
			labelRxNom=labelRxNom.replace("(Fake)", "(substance)") ;  
			//System.out.println("\t label "+label);
			String labeltest=labelRxNom.toLowerCase().trim().replaceAll("\\s{2,}", " ");
			labelRxNom = labelRxNom.replace("()", "");
			labelRxNom = labelRxNom.replaceAll("(?i),", "");         
			labelRxNom = labelRxNom.replaceAll("(?i)(obsolete)", "");
			labelRxNom = labelRxNom.replaceAll("(?i)monobasic-dibasic", "");
			labelRxNom = labelRxNom.replaceAll("(?i)monobasic", "");
			labelRxNom = labelRxNom.replaceAll("(?i)(dibasic)", "");
			labelRxNom = labelRxNom.replaceAll("(?i)dibasic", "");
			labelRxNom = labelRxNom.replaceAll("(?i)(tribasic)", "");                       
			labelRxNom = labelRxNom.replaceAll("(?i)tribasic", "");                  
			labelRxNom = labelRxNom.replaceAll("(?i)(anhydrous)", "");
			labelRxNom = labelRxNom.replaceAll("(?i)(monohydrate)", "");             
			labelRxNom = labelRxNom.replaceAll("(?i)anhydrous", "");
			labelRxNom = labelRxNom.replaceAll("(?i)trihydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)dihydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)(monohydrate)", "");
			labelRxNom = labelRxNom.replaceAll("(?i)monohydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)hemihydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)tetrahydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)pentahydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)dodecahydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)sesquihydrate", "");                   
			labelRxNom = labelRxNom.replaceAll("(?i)octahydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)hexahydrate", "");               
			labelRxNom = labelRxNom.replaceAll("(?i)octahydrate", "");               
			labelRxNom = labelRxNom.replaceAll("(?i)nonahydrate", "");               
			labelRxNom = labelRxNom.replaceAll("(?i)oxyhydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)hydrate", "");
			labelRxNom=labelRxNom.toLowerCase().trim().replaceAll("\\s{2,}", " ");
			labelRxNom=labelRxNom.trim().replaceAll("\\s{2,}", " ");

			if(!LabelAndCode.containsKey(labelRxNom)) {
				LabelAndCode.put(labelRxNom, new HashSet<String>());
			}
			LabelAndCode.get(labelRxNom).add(d.getIRI().getShortForm());
			if(!labeltest.equals(labelRxNom)) {
				RxNormModified.add(d.getIRI().getShortForm());
				System.out.println("labeltest *"+labeltest+"* labelRxNom *"+labelRxNom+"*");

			}
		});
		LabelAndCode.forEach((a,b)->{
			if(b.size()>1) {

				b.forEach(z->{
					if(z.startsWith("Rx")) {

						Set<OWLClass> mult=new HashSet<OWLClass>();
						for(String at:b) {
							if(!at.equals(z)) {
								OWLClass exaz= factory.getOWLClass(at,pm);
								OWLClass exaz1= factory.getOWLClass(z,pm);

								OWLEquivalentClassesAxiom axaz= factory.getOWLEquivalentClassesAxiom(exaz,exaz1);
								eq.add(axaz);
								if(!at.startsWith("Rx")) {
									if(SNOMEDModified.contains(at)) {
										OWLClass ex= factory.getOWLClass(at,pm);
										OWLClass ex1= factory.getOWLClass(z,pm);

										OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(ex,ex1);
										eq.add(ax);
										String label1=a.replace("(substance)","(Fake)") ; 
										String ligne=z+";"+label1+";"+at+";"+a+";Anhydrous\n";
										// System.out.println("ligne"+ligne);
										listOfENM.add(ligne);
									}
									else if(RxNormModified.contains(z)) {
										OWLClass ex= factory.getOWLClass(at,pm);
										OWLClass ex1= factory.getOWLClass(z,pm);

										OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(ex,ex1);
										eq.add(ax);
										String label1=a.replace("(substance)","(Fake)") ; 
										String ligne=z+";"+label1+";"+at+";"+a+";Anhydrous\n";
										// System.out.println("ligne"+ligne);
										listOfENM.add(ligne);
									}
								}
							}
						}
						if(mult.size()>1) {
							System.out.println("mutlllllllllllllllllllllllllllllll"+z); 
						}
						System.out.println(z+","+b );
					}
					else {
						for(String at:b) {
							OWLClass exaz= factory.getOWLClass(at,pm);
							OWLClass exaz1= factory.getOWLClass(z,pm);

							OWLEquivalentClassesAxiom axaz= factory.getOWLEquivalentClassesAxiom(exaz,exaz1);
							eq.add(axaz);
						}
					}
				});
			}


		});



		return listOfENM;

	}


	public static  Set<OWLAxiom>  getRxNormSubstancesHydrationOvercomingMappingAxiom(OWLOntologyManager manager,OWLOntology Ontology, Set<OWLClass>listToVerifie, Set<OWLClass> listSnomedSubstance) {
		Set<OWLAxiom> eq	= new HashSet<OWLAxiom>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = manager.getOWLDataFactory();
		Set<String> listOfENM = new HashSet<String>();
		Map<String,Set<String>> LabelAndCode = new HashMap<String, Set<String>>();
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("http://www.w3.org/2004/02/skos/core#altLabel");
		Set<String> SNOMEDModified= new HashSet<String>();
		Set<String> RxNormModified= new HashSet<String>();

		listSnomedSubstance.forEach(z->{

			//System.out.println("zzzzzzzzzzzzzzzzzzzzzzzzzzzz "+z);

			for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(z, Ontology,factory.getRDFSLabel() ).collect(Collectors.toSet())) {
				String LabelSnomed="";
				OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				LabelSnomed= vv.getLiteral();
				LabelSnomed= LabelSnomed.replace("-", " ");
				LabelSnomed = LabelSnomed.replaceAll("(?i),", ""); 
				if(!LabelSnomed.contains("(substance)")) {
					LabelSnomed=LabelSnomed.trim().replaceAll("\\s{2,}", " ");
					LabelSnomed=LabelSnomed+" (substance)";
				}
				String labeltest = LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
				LabelSnomed = LabelSnomed.replaceAll("(?i),", "");         
				LabelSnomed = LabelSnomed.replaceAll("(?i)(obsolete)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic-dibasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(dibasic)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dibasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(tribasic)", "");                       
				LabelSnomed = LabelSnomed.replaceAll("(?i)tribasic", "");                  
				LabelSnomed = LabelSnomed.replaceAll("(?i)(anhydrous)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");             
				LabelSnomed = LabelSnomed.replaceAll("(?i)anhydrous", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)trihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monohydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hemihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)tetrahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)pentahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dodecahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)sesquihydrate", "");                   
				LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hexahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)nonahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)oxyhydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hydrate", "");
				LabelSnomed=LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
				if(!LabelAndCode.containsKey(LabelSnomed)) {
					LabelAndCode.put(LabelSnomed, new HashSet<String>());
				}
				LabelAndCode.get(LabelSnomed).add(z.getIRI().getShortForm());
				if(!labeltest.equals(LabelSnomed)) {
					SNOMEDModified.add(z.getIRI().getShortForm());
					System.out.println("labeltest *"+labeltest+"* LabelSnomed *"+LabelSnomed+"*");

				}
			}

			for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(z, Ontology, prop).collect(Collectors.toSet())) {
				String LabelSnomed="";
				OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				LabelSnomed= vv.getLiteral();
				LabelSnomed= LabelSnomed.replace("-", " ");
				LabelSnomed = LabelSnomed.replaceAll("(?i),", ""); 
				if(!LabelSnomed.contains("(substance)")) {
					LabelSnomed=LabelSnomed.trim().replaceAll("\\s{2,}", " ");
					LabelSnomed=LabelSnomed+" (substance)";
				}
				String labeltest = LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
				LabelSnomed = LabelSnomed.replaceAll("(?i),", "");         
				LabelSnomed = LabelSnomed.replaceAll("(?i)(obsolete)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic-dibasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(dibasic)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dibasic", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(tribasic)", "");                       
				LabelSnomed = LabelSnomed.replaceAll("(?i)tribasic", "");                  
				LabelSnomed = LabelSnomed.replaceAll("(?i)(anhydrous)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");             
				LabelSnomed = LabelSnomed.replaceAll("(?i)anhydrous", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)trihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)monohydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hemihydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)tetrahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)pentahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)dodecahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)sesquihydrate", "");                   
				LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hexahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)nonahydrate", "");               
				LabelSnomed = LabelSnomed.replaceAll("(?i)oxyhydrate", "");
				LabelSnomed = LabelSnomed.replaceAll("(?i)hydrate", "");
				LabelSnomed=LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
				if(!LabelAndCode.containsKey(LabelSnomed)) {
					LabelAndCode.put(LabelSnomed, new HashSet<String>());
				}
				LabelAndCode.get(LabelSnomed).add(z.getIRI().getShortForm());
				if(!labeltest.equals(LabelSnomed)) {
					SNOMEDModified.add(z.getIRI().getShortForm());
					System.out.println("labeltest *"+labeltest+"* LabelSnomed *"+LabelSnomed+"*");

				}
			}




		});

		listToVerifie.forEach((d)->{
			String labelRxNom="";
			for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				//label
				OWLAnnotationValue j =a.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				labelRxNom= vv.getLiteral();
				//label=a.toString();
			}
			//	System.out.println("label "+labelRxNom);
			labelRxNom = labelRxNom.replace("()", "");
			labelRxNom=labelRxNom.replace("(Fake)", "(substance)") ; 
			labelRxNom = labelRxNom.replace("()", "");
			//System.out.println("\t label "+label);
			String labeltest=labelRxNom.toLowerCase().trim().replaceAll("\\s{2,}", " ");
			labelRxNom = labelRxNom.replaceAll("(?i),", "");         
			labelRxNom = labelRxNom.replaceAll("(?i)(obsolete)", "");
			labelRxNom = labelRxNom.replaceAll("(?i)monobasic-dibasic", "");
			labelRxNom = labelRxNom.replaceAll("(?i)monobasic", "");
			labelRxNom = labelRxNom.replaceAll("(?i)(dibasic)", "");
			labelRxNom = labelRxNom.replaceAll("(?i)dibasic", "");
			labelRxNom = labelRxNom.replaceAll("(?i)(tribasic)", "");                       
			labelRxNom = labelRxNom.replaceAll("(?i)tribasic", "");                  
			labelRxNom = labelRxNom.replaceAll("(?i)(anhydrous)", "");
			labelRxNom = labelRxNom.replaceAll("(?i)(monohydrate)", "");             
			labelRxNom = labelRxNom.replaceAll("(?i)anhydrous", "");
			labelRxNom = labelRxNom.replaceAll("(?i)trihydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)dihydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)(monohydrate)", "");
			labelRxNom = labelRxNom.replaceAll("(?i)monohydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)hemihydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)tetrahydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)pentahydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)dodecahydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)sesquihydrate", "");                   
			labelRxNom = labelRxNom.replaceAll("(?i)octahydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)hexahydrate", "");               
			labelRxNom = labelRxNom.replaceAll("(?i)octahydrate", "");               
			labelRxNom = labelRxNom.replaceAll("(?i)nonahydrate", "");               
			labelRxNom = labelRxNom.replaceAll("(?i)oxyhydrate", "");
			labelRxNom = labelRxNom.replaceAll("(?i)hydrate", "");
			labelRxNom=labelRxNom.toLowerCase().trim().replaceAll("\\s{2,}", " ");
			labelRxNom=labelRxNom.trim().replaceAll("\\s{2,}", " ");
			if(!LabelAndCode.containsKey(labelRxNom)) {
				LabelAndCode.put(labelRxNom, new HashSet<String>());
			}
			LabelAndCode.get(labelRxNom).add(d.getIRI().getShortForm());

			if(!labeltest.equals(labelRxNom)) {
				RxNormModified.add(d.getIRI().getShortForm());
				System.out.println("labeltest *"+labeltest+"* labelRxNom *"+labelRxNom+"*");

			}
		});
		LabelAndCode.forEach((a,b)->{
			if(b.size()>1) {

				b.forEach(z->{
					if(z.startsWith("Rx")) {

						Set<OWLClass> mult=new HashSet<OWLClass>();
						for(String at:b) {
							if(!at.equals(z)) {
								OWLClass exaz= factory.getOWLClass(at,pm);
								OWLClass exaz1= factory.getOWLClass(z,pm);

								OWLEquivalentClassesAxiom axaz= factory.getOWLEquivalentClassesAxiom(exaz,exaz1);
								eq.add(axaz);
								if(!at.startsWith("Rx")) {
									if(SNOMEDModified.contains(at)) {
										OWLClass ex= factory.getOWLClass(at,pm);
										OWLClass ex1= factory.getOWLClass(z,pm);

										OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(ex,ex1);
										eq.add(ax);
										String label1=a.replace("(substance)","(Fake)") ; 
										String ligne=z+";"+label1+";"+at+";"+a+";Anhydrous\n";
										// System.out.println("ligne"+ligne);
										listOfENM.add(ligne);
									}
									else if(RxNormModified.contains(z)) {
										OWLClass ex= factory.getOWLClass(at,pm);
										OWLClass ex1= factory.getOWLClass(z,pm);

										OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(ex,ex1);
										eq.add(ax);
										String label1=a.replace("(substance)","(Fake)") ; 
										String ligne=z+";"+label1+";"+at+";"+a+";Anhydrous\n";
										// System.out.println("ligne"+ligne);
										listOfENM.add(ligne);
									}
								}
							}
						}
						if(mult.size()>1) {
							System.out.println("mutlllllllllllllllllllllllllllllll"+z); 
						}
						System.out.println(z+","+b );
					}
					else {
						for(String at:b) {
							OWLClass exaz= factory.getOWLClass(at,pm);
							OWLClass exaz1= factory.getOWLClass(z,pm);

							OWLEquivalentClassesAxiom axaz= factory.getOWLEquivalentClassesAxiom(exaz,exaz1);
							eq.add(axaz);
						}
					}
				});
			}


		});
		return eq;

	}


	//	public static  Set<OWLAxiom>  getRxNormSubstancesHydrationOvercomingMappingAxiom(OWLOntologyManager manager,OWLOntology Ontology, Map<String,Set<String>> LabelAndCodeForHydration, Set<OWLClass> listToVerifie) {
	//		Set<OWLAxiom> eq	= new HashSet<OWLAxiom>();
	//        PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
	//		OWLDataFactory  factory = manager.getOWLDataFactory();
	//        Set<String> listOfENM = new HashSet<String>();
	//       Map<String,Set<String>> LabelAndCode = new HashMap<String, Set<String>>();
	//        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("http://www.w3.org/2004/02/skos/core#altLabel");
	//        LabelAndCodeForHydration.forEach((a,b)->{
	//         		b.forEach(z->{
	//         			if(!z.startsWith("Rx")) {
	//         				String LabelSnomed="";
	//         				OWLClass zaa= factory.getOWLClass(z,pm);
	//         	        	for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(zaa, Ontology, prop).collect(Collectors.toSet())) {
	//         	        		OWLAnnotationValue j =d.annotationValue();
	//         					OWLLiteral vv= (OWLLiteral) j;
	//         					LabelSnomed= vv.getLiteral();
	//         					LabelSnomed= LabelSnomed.replace("-", " ");
	//         					if(!LabelSnomed.contains("(substance)")) {
	//         						LabelSnomed=LabelSnomed.trim();
	//         						LabelSnomed=LabelSnomed+" (substance)";
	//         		           	}
	//         					
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i),", "");         
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)(obsolete)", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic-dibasic", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)monobasic", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)(dibasic)", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)dibasic", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)(tribasic)", "");                       
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)tribasic", "");                  
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)(anhydrous)", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");             
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)anhydrous", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)trihydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)dihydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)(monohydrate)", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)monohydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)hemihydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)tetrahydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)pentahydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)dodecahydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)sesquihydrate", "");                   
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)hexahydrate", "");               
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)octahydrate", "");               
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)nonahydrate", "");               
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)oxyhydrate", "");
	//         					LabelSnomed = LabelSnomed.replaceAll("(?i)hydrate", "");
	//         					LabelSnomed=LabelSnomed.toLowerCase();
	//         	               	if(!LabelAndCode.containsKey(LabelSnomed)) {
	//         	               		LabelAndCode.put(LabelSnomed, new HashSet<String>());
	//         	               	}
	//         	               	LabelAndCode.get(LabelSnomed).add(z);
	//         	           	}
	//         				
	//         				
	//         				
	//         			}
	//         			else {
	//         				String labelRxNom=a;
	//         				labelRxNom=labelRxNom.replace("(Fake)", "(substance)") ;  
	//         				if(!labelRxNom.contains("(substance)")) {
	//         					labelRxNom=labelRxNom.trim();
	//         					labelRxNom=labelRxNom+" (substance)";
	//     		           	}
	//         				labelRxNom = labelRxNom.replaceAll("(?i),", "");         
	//     					labelRxNom = labelRxNom.replaceAll("(?i)(obsolete)", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)monobasic-dibasic", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)monobasic", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)(dibasic)", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)dibasic", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)(tribasic)", "");                       
	//     					labelRxNom = labelRxNom.replaceAll("(?i)tribasic", "");                  
	//     					labelRxNom = labelRxNom.replaceAll("(?i)(anhydrous)", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)(monohydrate)", "");             
	//     					labelRxNom = labelRxNom.replaceAll("(?i)anhydrous", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)trihydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)dihydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)(monohydrate)", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)monohydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)hemihydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)tetrahydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)pentahydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)dodecahydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)sesquihydrate", "");                   
	//     					labelRxNom = labelRxNom.replaceAll("(?i)octahydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)hexahydrate", "");               
	//     					labelRxNom = labelRxNom.replaceAll("(?i)octahydrate", "");               
	//     					labelRxNom = labelRxNom.replaceAll("(?i)nonahydrate", "");               
	//     					labelRxNom = labelRxNom.replaceAll("(?i)oxyhydrate", "");
	//     					labelRxNom = labelRxNom.replaceAll("(?i)hydrate", "");
	//     					labelRxNom=labelRxNom.toLowerCase();
	//     	               	if(!LabelAndCode.containsKey(labelRxNom)) {
	//     	               		LabelAndCode.put(labelRxNom, new HashSet<String>());
	//     	               	}
	//     	               	LabelAndCode.get(labelRxNom).add(z);
	//         				
	//         			}
	//         		});
	//        });
	//        
	//        
	//        listToVerifie.forEach((d)->{
	//       	 String labelRxNom="";
	//         	for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
	// 				//label
	// 				OWLAnnotationValue j =a.annotationValue();
	// 				OWLLiteral vv= (OWLLiteral) j;
	// 				labelRxNom= vv.getLiteral();
	// 				//label=a.toString();
	// 			}
	//         //	System.out.println("label "+labelRxNom);
	//         	labelRxNom=labelRxNom.replace("(Fake)", "(substance)") ;  
	//         	//System.out.println("\t label "+label);
	//         	
	//         	labelRxNom = labelRxNom.replaceAll("(?i),", "");         
	//				labelRxNom = labelRxNom.replaceAll("(?i)(obsolete)", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)monobasic-dibasic", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)monobasic", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)(dibasic)", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)dibasic", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)(tribasic)", "");                       
	//				labelRxNom = labelRxNom.replaceAll("(?i)tribasic", "");                  
	//				labelRxNom = labelRxNom.replaceAll("(?i)(anhydrous)", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)(monohydrate)", "");             
	//				labelRxNom = labelRxNom.replaceAll("(?i)anhydrous", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)trihydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)dihydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)(monohydrate)", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)monohydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)hemihydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)tetrahydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)pentahydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)dodecahydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)sesquihydrate", "");                   
	//				labelRxNom = labelRxNom.replaceAll("(?i)octahydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)hexahydrate", "");               
	//				labelRxNom = labelRxNom.replaceAll("(?i)octahydrate", "");               
	//				labelRxNom = labelRxNom.replaceAll("(?i)nonahydrate", "");               
	//				labelRxNom = labelRxNom.replaceAll("(?i)oxyhydrate", "");
	//				labelRxNom = labelRxNom.replaceAll("(?i)hydrate", "");
	//				labelRxNom=labelRxNom.toLowerCase();
	//				labelRxNom=labelRxNom.trim();
	//          	if(!LabelAndCode.containsKey(labelRxNom)) {
	//          		LabelAndCode.put(labelRxNom, new HashSet<String>());
	//          	}
	//          	LabelAndCode.get(labelRxNom).add(d.getIRI().getShortForm());
	//        });
	//        
	//        LabelAndCode.forEach((a,b)->{
	//          	 if(b.size()>1) {
	//          		 
	//          		 b.forEach(z->{
	//          			 if(z.startsWith("Rx")) {
	//          				
	//          				 Set<OWLClass> mult=new HashSet<OWLClass>();
	//          				 for(String at:b) {
	//          					 if(!at.equals(z)) {
	//          						if(!at.startsWith("Rx")) {
	//          						 OWLClass ex= factory.getOWLClass(at,pm);
	//          						OWLClass ex1= factory.getOWLClass(z,pm);
	//          					
	//          						 OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(ex,ex1);
	//          						 eq.add(ax);
	//          						 String label1=a.replace("(substance)","") ; 
	//          						 String ligne=z+";"+label1+";"+at+";"+a+";HYD\n";
	//          						// System.out.println("ligne"+ligne);
	//          						 listOfENM.add(ligne);
	//          						}
	//          					 }
	//          				 }
	//          				 if(mult.size()>1) {
	//          					System.out.println("mutlllllllllllllllllllllllllllllll"+z); 
	//          				 }
	//          				 System.out.println(z+","+b );
	//          			 }
	//          		 });
	//          	 }
	//          	 
	//          	 
	//           });
	//        return eq;
	//        
	//	}

	public static  Set<String>  getRxNormSubstancesAltLabelMapping(OWLOntologyManager manager,OWLOntology Ontology, Map<String,Set<OWLClass>> LabelAndCodeForAlt,Set<OWLClass> listrxNormSubstance,Set<OWLClass> listSnomedSubstance) {

		OWLDataFactory  factory = manager.getOWLDataFactory();

		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		Set<String> listOfENM = new HashSet<String>();
		Set<String> resul = new HashSet<String>();
		Map<String,Set<String>> LabelAndCode = new HashMap<String, Set<String>>();
		Map<String,Set<String>> labelAndCodeForHydrateMapping = new HashMap<String, Set<String>>();
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("http://www.w3.org/2004/02/skos/core#altLabel");
		LabelAndCodeForAlt.forEach((a,b)->{
			b.forEach(z->{
				if(!z.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
					String LabelSnomed="";
					for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(z, Ontology, prop).collect(Collectors.toSet())) {
						OWLAnnotationValue j =d.annotationValue();
						OWLLiteral vv= (OWLLiteral) j;
						LabelSnomed= vv.getLiteral();
						LabelSnomed= LabelSnomed.replace("-", " ");
						LabelSnomed = LabelSnomed.replaceAll("(?i),", "");  
						if(!LabelSnomed.contains("(substance)")) {
							LabelSnomed=LabelSnomed.trim().replaceAll("\\s{2,}", " ");
							LabelSnomed=LabelSnomed+" (substance)";
						}
						LabelSnomed=LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
						if(!LabelAndCode.containsKey(LabelSnomed)) {
							LabelAndCode.put(LabelSnomed, new HashSet<String>());
						}
						LabelAndCode.get(LabelSnomed).add(z.getIRI().getShortForm());
					}



				}
				else {
					String labelRxNom=a;
					labelRxNom = labelRxNom.replace("()", "");
					labelRxNom=labelRxNom.replace("(Fake)", "(substance)") ;  
					if(!labelRxNom.contains("(substance)")) {
						labelRxNom=labelRxNom.trim().replaceAll("\\s{2,}", " ");
						labelRxNom=labelRxNom+" (substance)";
					}
					labelRxNom = labelRxNom.replaceAll("(?i),", ""); 
					labelRxNom=labelRxNom.toLowerCase().trim().replaceAll("\\s{2,}", " ");
					if(!LabelAndCode.containsKey(labelRxNom)) {
						LabelAndCode.put(labelRxNom, new HashSet<String>());
					}
					LabelAndCode.get(labelRxNom).add(z.getIRI().getShortForm());

				}
			});
		});
		// Set<String> listOfMappedRxNorm = new HashSet<String>();
		LabelAndCode.forEach((a,b)->{
			if(b.size()>1) {

				b.forEach(z->{
					if(z.startsWith("Rx")) {

						Set<OWLClass> mult=new HashSet<OWLClass>();
						for(String at:b) {
							if(!at.equals(z)) {
								if(!at.startsWith("Rx")) {
									OWLClass ex= factory.getOWLClass(at,pm);
									OWLClass ex1= factory.getOWLClass(z,pm);

									OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(ex,ex1);

									String label1=a.replace("(substance)","(Fake)") ; 
									String ligne=z+";"+label1+";"+at+";"+a+";ENM\n";
									// System.out.println("ligne"+ligne);
									listOfENM.add(ligne);
								}
							}
						}
						if(mult.size()>1) {
							System.out.println("mutlllllllllllllllllllllllllllllll"+z); 
						}
						System.out.println(z+","+b );
					}
				});
			}
			else {
				if(!labelAndCodeForHydrateMapping.containsKey(a)) {
					labelAndCodeForHydrateMapping.put(a, new HashSet<String>());
				}
				labelAndCodeForHydrateMapping.get(a).addAll(b);
			}

		});
		listOfENM.addAll(getRxNormSubstancesHydrationOvercomingMapping(manager, Ontology, listrxNormSubstance,listSnomedSubstance));

		return listOfENM;

	}

	public static  Set<OWLAxiom>  getRxNormSubstancesAltLabelMappingAxiom(OWLOntologyManager manager,OWLOntology Ontology, Map<String,Set<OWLClass>> LabelAndCodeForAlt,Set<OWLClass> listrxNormSubstance,Set<OWLClass> listSnomedSubstance) {
		Set<OWLAxiom> eq	= new HashSet<OWLAxiom>();
		OWLDataFactory  factory = manager.getOWLDataFactory();

		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		Set<String> listOfENM = new HashSet<String>();
		Set<String> resul = new HashSet<String>();
		Map<String,Set<String>> LabelAndCode = new HashMap<String, Set<String>>();
		Map<String,Set<String>> labelAndCodeForHydrateMapping = new HashMap<String, Set<String>>();
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("http://www.w3.org/2004/02/skos/core#altLabel");
		LabelAndCodeForAlt.forEach((a,b)->{
			b.forEach(z->{
				if(!z.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
					String LabelSnomed="";
					for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(z, Ontology, prop).collect(Collectors.toSet())) {
						OWLAnnotationValue j =d.annotationValue();
						OWLLiteral vv= (OWLLiteral) j;
						LabelSnomed= vv.getLiteral();
						LabelSnomed= LabelSnomed.replace("-", " ");
						LabelSnomed = LabelSnomed.replaceAll("(?i),", "");  
						if(!LabelSnomed.contains("(substance)")) {
							LabelSnomed=LabelSnomed.trim().replaceAll("\\s{2,}", " ");
							LabelSnomed=LabelSnomed+" (substance)";
						}
						LabelSnomed=LabelSnomed.toLowerCase().trim().replaceAll("\\s{2,}", " ");
						if(!LabelAndCode.containsKey(LabelSnomed)) {
							LabelAndCode.put(LabelSnomed, new HashSet<String>());
						}
						LabelAndCode.get(LabelSnomed).add(z.getIRI().getShortForm());
					}



				}
				else {
					String labelRxNom=a;
					labelRxNom = labelRxNom.replace("()", "");
					labelRxNom=labelRxNom.replace("(Fake)", "(substance)") ;
					labelRxNom = labelRxNom.replace("()", "");
					labelRxNom = labelRxNom.replaceAll("(?i),", "");  
					if(!labelRxNom.contains("(substance)")) {
						labelRxNom=labelRxNom.trim().replaceAll("\\s{2,}", " ");
						labelRxNom=labelRxNom+" (substance)";
					}
					labelRxNom=labelRxNom.toLowerCase().trim().replaceAll("\\s{2,}", " ");
					if(!LabelAndCode.containsKey(labelRxNom)) {
						LabelAndCode.put(labelRxNom, new HashSet<String>());
					}
					LabelAndCode.get(labelRxNom).add(z.getIRI().getShortForm());

				}
			});
		});

		LabelAndCode.forEach((a,b)->{
			if(b.size()>1) {

				b.forEach(z->{
					if(z.startsWith("Rx")) {

						Set<OWLClass> mult=new HashSet<OWLClass>();
						for(String at:b) {
							if(!at.equals(z)) {
								if(!at.startsWith("Rx")) {
									OWLClass ex= factory.getOWLClass(at,pm);
									OWLClass ex1= factory.getOWLClass(z,pm);

									OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(ex,ex1);
									eq.add(ax);
									String label1=a.replace("(substance)","(Fake)") ; 
									String ligne=z+";"+label1+";"+at+";"+a+";ENM\n";
									// System.out.println("ligne"+ligne);
									listOfENM.add(ligne);
								}
							}
						}
						if(mult.size()>1) {
							System.out.println("mutlllllllllllllllllllllllllllllll"+z); 
						}
						System.out.println(z+","+b );
					}
				});
			}
			else {
				if(!labelAndCodeForHydrateMapping.containsKey(a)) {
					labelAndCodeForHydrateMapping.put(a, new HashSet<String>());
				}
				labelAndCodeForHydrateMapping.get(a).addAll(b);
			}

		});
		eq.addAll(getRxNormSubstancesHydrationOvercomingMappingAxiom(manager, Ontology, listrxNormSubstance,listSnomedSubstance));

		return eq;

	}







	/**
	 * Created the mappings not defined by RxNorm and also create the list of ingredients not mapped according to their main characteristics.
	 * ENM : Erroneously not-mapped
	 * EXT : susbtances describing Extrats
	 * SSU : Specific substance in RxNorm
	 * SDE : Specific description in RxNorm
	 * GED : general description in RxNorm
	 * DER : Data errors (duplicate substance in RxNorm)
	 * @param listToVerifie
	 * @param listOfExtrat
	 * @param manager
	 * @param Ontology
	 * @param Elkreasoner
	 * @throws OWLOntologyStorageException
	 * @throws FileNotFoundException
	 */
//	public static void getmappingsSubstanceNotDefinedByRxNorm(File Path,Set<OWLClass> listToVerifie,Set<OWLClass>listOfExtrat,OWLOntologyManager manager,OWLOntology Ontology,OWLReasoner Elkreasoner) throws OWLOntologyStorageException, FileNotFoundException {
	public static void getmappingsSubstanceNotDefinedByRxNorm(File Path, Set<OWLClass> listToVerifie, Set<OWLClass> listOfExtrat) throws OWLOntologyStorageException, FileNotFoundException {	

		Set<OWLClass> SnomedSubstancesMapped = new HashSet<OWLClass>();
		// Set<OWLClass> RxNormSubstanceToRestMapping = new HashSet<OWLClass>();
		Set<OWLAxiom> eq	= new HashSet<OWLAxiom>();
		Set<OWLClass> RxNormSubstancesErroniouslynotmapped = new HashSet<OWLClass>();
		Set<OWLClass> FinalList = new HashSet<OWLClass>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = manager.getOWLDataFactory();
		OWLClass substanceHierarchy=factory.getOWLClass("105590001",pm);
		Set<OWLClass> Substances=Elkreasoner.getSubClasses(substanceHierarchy, false).entities().collect(Collectors.toSet());
		Substances.forEach((k)->{if(!k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {SnomedSubstancesMapped.add(k);}});
		// Substances.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {RxNormSubstanceToMap.add(k);}});
		Map<String,Set<OWLClass>> LabelAndCode = new HashMap<String, Set<OWLClass>>();
		Map<String,Set<OWLClass>> LabelAndCodeForAltSearch = new HashMap<String, Set<OWLClass>>();
		SnomedSubstancesMapped.forEach(e->{
			String label="";
			for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(e, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				//label
				OWLAnnotationValue j =a.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				label= vv.getLiteral();
				//label=a.toString();
			}
			label = label.replaceAll("(?i),", "");  
			label=label.toLowerCase().trim().replaceAll("\\s{2,}", " ");
			label=label.trim();
			if(!LabelAndCode.containsKey(label)) {
				LabelAndCode.put(label, new HashSet<OWLClass>());
			}
			LabelAndCode.get(label).add(e);
		});

		listToVerifie.forEach((d)->{
			String label="";
			for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				//label
				OWLAnnotationValue j =a.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				label= vv.getLiteral();
				//label=a.toString();
			}
			System.out.println("label "+label);
			label = label.replace("()", "");
			label=label.replace("(Fake)", "(substance)") ;  
			label = label.replace("()", "");
			System.out.println("\t label "+label);

			label=label.toLowerCase().trim().replaceAll("\\s{2,}", " ");
			label=label.trim().replaceAll("\\s{2,}", " ");
			if(!LabelAndCode.containsKey(label)) {
				LabelAndCode.put(label, new HashSet<OWLClass>());
			}
			LabelAndCode.get(label).add(d);
		});
//		String resu="Rxcui;label;SCTID;SClabel;TypeOfError\n";
		String resu="";		
		Set<String> listOfENM = new HashSet<String>();

		LabelAndCode.forEach((a,b)->{
			if(b.size()>1) {

				b.forEach(z->{
					if(z.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
						RxNormSubstancesErroniouslynotmapped.add(z);
						Set<OWLClass> mult=new HashSet<OWLClass>();
						for(OWLClass at:b) {
							if(!at.getIRI().toString().equals(z.getIRI().toString())) {
								mult.add(at);
								OWLEquivalentClassesAxiom ax= factory.getOWLEquivalentClassesAxiom(at,z);
								eq.add(ax);
								String label1=a.replace("(substance)","(Fake)") ; 
								String ligne=z.getIRI().getShortForm()+";"+label1+";"+at.getIRI().getShortForm()+";"+a+";ENM\n";
								// System.out.println("ligne"+ligne);
								listOfENM.add(ligne);
								//	 RxNormSubstanceToRestMapping.add(z);
							}
						}
						if(mult.size()>1) {
							System.out.println("mutlllllllllllllllllllllllllllllll"+z); 
						}
						System.out.println(z+","+b );
					}
				});
			}
			else {
				if(!LabelAndCodeForAltSearch.containsKey(a)) {
					LabelAndCodeForAltSearch.put(a, new HashSet<OWLClass>());
				}
				LabelAndCodeForAltSearch.get(a).addAll(b);
			}

		});
		//         Set<OWLClass> RxNormSubstanceToMap = new HashSet<OWLClass>();
		//         listToVerifie.forEach(azaz->{
		//        	 if(!RxNormSubstanceToRestMapping.contains(azaz)) {
		//        		 RxNormSubstanceToMap.add(azaz);
		//        	 }
		//         });


		listOfENM.addAll(getRxNormSubstancesAltLabelMapping(manager, Ontology, LabelAndCodeForAltSearch,listToVerifie, SnomedSubstancesMapped));
		eq.addAll(getRxNormSubstancesAltLabelMappingAxiom(manager, Ontology, LabelAndCodeForAltSearch,listToVerifie, SnomedSubstancesMapped));

		for(OWLClass av:listOfExtrat) {
			String label="";
			for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(av, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				//label
				OWLAnnotationValue j =a.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				label= vv.getLiteral();
				//label=a.toString();
			}
			String ligne=av.getIRI().getShortForm()+";"+label+";NA;NA;EXT\n";
			resu=resu+ligne;
		}

		for(String ar:listOfENM) {
			resu=resu+ar;
		}

		listToVerifie.forEach(av->{
			if(!RxNormSubstancesErroniouslynotmapped.contains(av)) {
				FinalList.add(av);
			}
		});
		updateOntologyIngredientMappingsFinal(Path, eq, manager, Ontology);;
		// System.out.println("RxNormSubstancesErroniouslynotmapped "+RxNormSubstancesErroniouslynotmapped.size());
		// System.out.println("rest "+FinalList.size());

		Set<OWLClass> specific = new HashSet<OWLClass>();
		for(OWLClass q:FinalList){
			String label="";
			for(OWLAnnotation gh:EntitySearcher.getAnnotationObjects(q, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				//label
				OWLAnnotationValue j =gh.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				label= vv.getLiteral();
				//label=a.toString();
				if(label.contains("human ")) {
					System.out.println(q+" ; "+label);
					specific.add(q);
					resu=resu+q.getIRI().getShortForm()+";"+label+";NA;NA;Specific description in RxNorm\n";
				}
				else {
					resu=resu+q.getIRI().getShortForm()+";"+label+";NA;NA; \n";
				}
			}


			//System.out.println("code "+q+" label: "+label);

		};
		System.out.println("specific "+specific.size());


		try { 
			PrintWriter out = new PrintWriter("UnmappedMappingsIngredients.csv");
			out.println(resu);
			String[] lines = resu.split("\n");
			classMana.getReport().createNewSheet("unmapped ingredients");	
			classMana.getReport().printHeader("Rxcui;label;SCTID;SClabel;TypeOfError", ";");			
			for(String line : lines) {
				classMana.getReport().print(line, ";");
			}
			out.close();
		} catch(Exception e) {
			System.err.println("Unable to create output to FindTheMissingIngredientsEquivalence.  Ensure the Excel file is not already open!");
		}


	}
	/**
	 * get the lit of ingredients used in RxNorm to describe semantic clinical drugs without extrat
	 * @param manager
	 * @param Ontology
	 * @param Elkreasoner
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Set<OWLClass> getRxNormListOfNecessaryIngredientsInSNOMEDWithoutExtrat() throws FileNotFoundException {

		Set<OWLClass> RxNormMedicinalProductWithVaccines = new HashSet<OWLClass>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = manager.getOWLDataFactory();
		OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
		Set<OWLClass> MedicinalProducts=Elkreasoner.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
		MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {RxNormMedicinalProductWithVaccines.add(k);}});

		//System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProductWithVaccines.size());
		Set<OWLClass> RxNormMedicinalProduct = new HashSet<OWLClass>();
		Set<OWLClass> Vaccines = new HashSet<OWLClass>();
		RxNormMedicinalProductWithVaccines.forEach((d)->{
			String label="";
			for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {

				OWLAnnotationValue j =a.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				label= vv.getLiteral();
			}
			if(label.contains("vaccine ")) {

				Vaccines.add(d);

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
			else if(label.contains("extract ")) {

				Vaccines.add(d);

			}
			else {
				RxNormMedicinalProduct.add(d);
			}

		});

		System.out.println("vaccines "+Vaccines.size());
		System.out.println("RxNormMedicinalProductWithVaccines "+RxNormMedicinalProductWithVaccines.size());
		System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProduct.size());
		Set<OWLClass> Ingredients= new HashSet<OWLClass>();
		Set<OWLClass> IngredientsRxNorm= new HashSet<OWLClass>();

		OWLObjectProperty getPreciseIngredient = factory.getOWLObjectProperty("http://snomed.info/id/762949000");
		OWLObjectProperty getIngredient = factory.getOWLObjectProperty("http://snomed.info/id/127489000"); 
		OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 

		RxNormMedicinalProduct.forEach((d)->{
			Stream<OWLClassExpression> eq= EntitySearcher.getEquivalentClasses(d, Ontology);

			eq.forEach((q)->{
				Ingredients.addAll(resultSpecificRelation(q, getPreciseIngredient));
				Ingredients.addAll(resultSpecificRelation(q, getBOss));
			});
		});


		Ingredients.forEach(d->{
			if(d.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
				if(!d.getIRI().toString().startsWith("http://snomed.info/id/Rx0")) {
					IngredientsRxNorm.add(d);
				}
			}
			else {
				Set<OWLClass> substanceRelated= Elkreasoner.getEquivalentClasses(d).entities().collect(Collectors.toSet());

				substanceRelated.forEach(a->{
					if(a.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
						if(!a.getIRI().toString().startsWith("http://snomed.info/id/Rx0")) {
							IngredientsRxNorm.add(a);
						}
					}

				});
			}
		});

		Set<OWLClass> IngredientsRxNormValidated= new HashSet<OWLClass>();


		IngredientsRxNorm.forEach(d->{
			Set<OWLClass> substanceRelated= Elkreasoner.getEquivalentClasses(d).entities().collect(Collectors.toSet());
			substanceRelated.forEach(a->{
				if(!a.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
					IngredientsRxNormValidated.add(d);
				}
			});
		});

		Set<OWLClass> listforSNOMED = new HashSet<OWLClass>();
		IngredientsRxNorm.forEach(d->{
			if(!IngredientsRxNormValidated.contains(d)) {
				listforSNOMED.add(d);
			}
		});

		return listforSNOMED;


	}
	public static Set<OWLClass> resultSpecificRelation(OWLClassExpression Expression, OWLObjectProperty propert){

		Set<OWLClass> resultats= new HashSet<OWLClass>();
		OWLObjectIntersectionOf express= (OWLObjectIntersectionOf) Expression;

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
					//System.out.println("\t \t restic.getFiller()) "+restic.getFiller());
					resultats.addAll(resultSpecificRelation(restic.getFiller(), propert));


				}		
			}
		}
		//System.out.println(resultats);
		return resultats;

	}
	public static void updateOntologyIngredientMappingsFinal(File OntologyPath2,Set<OWLAxiom> eq,OWLOntologyManager manager,OWLOntology Ontology) throws OWLOntologyStorageException {

		Stream<OWLAxiom> resul=eq.stream();

		manager.addAxioms(Ontology, resul);
		classMana.flushReasoner();
		manager.saveOntology(Ontology, IRI.create(OntologyPath2.toURI()));


	}
	public static Set<OWLClass> getRxNormListOfExtratIngredientsInRxNorm() throws FileNotFoundException {
		Set<OWLClass> ExtratList = new HashSet<OWLClass>();
//		Set<OWLClass>Withoutextrat=getRxNormListOfNecessaryIngredientsInSNOMEDWithoutExtrat(manager, Ontology, Elkreasoner);
		Set<OWLClass>Withoutextrat=getRxNormListOfNecessaryIngredientsInSNOMEDWithoutExtrat();		
		Set<OWLClass> totalList=getRxNormListOfNecessaryIngredientsInSNOMED();
		totalList.forEach(z->{
			if(!Withoutextrat.contains(z)) {
				ExtratList.add(z);
			}
		});

		return ExtratList;

	}
	/**
	 * get the list of RxNorm ingredients used to describe semantic clinical drugs
	 * @param manager
	 * @param Ontology
	 * @param Elkreasoner
	 * @return
	 * @throws FileNotFoundException
	 */
//	public static Set<OWLClass> getRxNormListOfNecessaryIngredientsInSNOMED(OWLOntologyManager manager,OWLOntology Ontology,OWLReasoner Elkreasoner) throws FileNotFoundException {
	public static Set<OWLClass> getRxNormListOfNecessaryIngredientsInSNOMED() throws FileNotFoundException {	

		Set<OWLClass> RxNormMedicinalProductWithVaccines = new HashSet<OWLClass>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = manager.getOWLDataFactory();
		OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);
		Set<OWLClass> MedicinalProducts=Elkreasoner.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
		MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {RxNormMedicinalProductWithVaccines.add(k);}});

		System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProductWithVaccines.size());
		Set<OWLClass> RxNormMedicinalProduct = new HashSet<OWLClass>();
		Set<OWLClass> RxNormMedicinalProductCVXPotential = new HashSet<OWLClass>();
		Set<OWLClass> Vaccines = new HashSet<OWLClass>();
		RxNormMedicinalProductWithVaccines.forEach((d)->{
			String label="";
			for(OWLAnnotation a:EntitySearcher.getAnnotationObjects(d, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {

				OWLAnnotationValue j =a.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				label= vv.getLiteral();
			}
			if(label.contains("vaccine ")) {

				Vaccines.add(d);

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
				//RxNormMedicinalProductCVXPotential.add(d);
				RxNormMedicinalProduct.add(d);
			}

		});
		//add this to check potential CVX related concepts
		//       RxNormMedicinalProductCVXPotential.forEach(az->{
		//    	   if(!getRxCodesCVXVaccine(az)) {
		//    		   RxNormMedicinalProduct.add(az);
		//    	   }
		//       });

		System.out.println("vaccines "+Vaccines.size());
		System.out.println("RxNormMedicinalProductWithVaccines "+RxNormMedicinalProductWithVaccines.size());
		System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProduct.size());
		System.out.println("RxNormMedicinalProductCVXPotential "+RxNormMedicinalProductCVXPotential.size());
		Set<OWLClass> Ingredients= new HashSet<OWLClass>();
		Set<OWLClass> IngredientsRxNorm= new HashSet<OWLClass>();

		OWLObjectProperty getPreciseIngredient = factory.getOWLObjectProperty("http://snomed.info/id/762949000");
		OWLObjectProperty getIngredient = factory.getOWLObjectProperty("http://snomed.info/id/127489000"); 
		OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 

		RxNormMedicinalProduct.forEach((d)->{
			Stream<OWLClassExpression> eq= EntitySearcher.getEquivalentClasses(d, Ontology);

			eq.forEach((q)->{
				Ingredients.addAll(resultSpecificRelation(q, getPreciseIngredient));
				Ingredients.addAll(resultSpecificRelation(q, getBOss));



			});
		});


		Ingredients.forEach(d->{
			if(d.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
				if(!d.getIRI().toString().startsWith("http://snomed.info/id/Rx0")) {
					IngredientsRxNorm.add(d);
				}
			}
			else {
				Set<OWLClass> substanceRelated= Elkreasoner.getEquivalentClasses(d).entities().collect(Collectors.toSet());

				substanceRelated.forEach(a->{
					if(a.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
						if(!a.getIRI().toString().startsWith("http://snomed.info/id/Rx0")) {
							IngredientsRxNorm.add(a);
						}
					}

				});
			}
		});

		Set<OWLClass> IngredientsRxNormValidated= new HashSet<OWLClass>();


		IngredientsRxNorm.forEach(d->{
			Set<OWLClass> substanceRelated= Elkreasoner.getEquivalentClasses(d).entities().collect(Collectors.toSet());
			substanceRelated.forEach(a->{
				if(!a.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
					IngredientsRxNormValidated.add(d);
				}
			});
		});

		Set<OWLClass> listforSNOMED = new HashSet<OWLClass>();
		IngredientsRxNorm.forEach(d->{
			if(!IngredientsRxNormValidated.contains(d)) {
				listforSNOMED.add(d);
			}
		});

		return listforSNOMED;


	}
	
	
	public static Boolean getRxCodesCVXVaccine(OWLClass code) {

		Set<String> codes = new HashSet<>();
		Boolean a= false;

		JSONObject SourceForSCD = null;

		String RxNormCuiString = code.getIRI().getShortForm().replace("Rx", " ").trim();

		try {
			//String urltest=restUrl.getRestUrl() + "/REST/rxcui/1099941/allProperties.json?prop=all";
			String urltest=restUrl.getRestUrl() + "/REST/rxcui/"+RxNormCuiString+"/allProperties.json?prop=all";
			SourceForSCD = getresult(urltest, null);
			// System.out.println(SourceForSCD);
		}

		catch(Exception e) {

			System.out.println("Unable to fetch dose form codes for Rxcui: " + RxNormCuiString);

		}



		if( !SourceForSCD.isNull("propConceptGroup") ) {

			JSONObject propConceptGroup = (JSONObject) SourceForSCD.get("propConceptGroup");

			if( !propConceptGroup.isNull("propConcept") ) {

				JSONArray propConcept = (JSONArray) propConceptGroup.get("propConcept");
				for(int i=0; i < propConcept.length(); i++ ) {
					JSONObject listelent= (JSONObject) propConcept.get(i);
					if(!listelent.isNull("propCategory")) {

						String rxString = listelent.get("propCategory").toString();
						//System.out.println("rxString "+rxString);
						if(rxString.equals("SOURCES")) {
							String Source = listelent.get("propValue").toString();
							//System.out.println(Source);
							if(Source.equals("HL7 Clinical Vaccine Formulation")) {

								a=true;
								break;
							}

						}


						codes.add(rxString);
					}



				}


			}

		}



		return a;

	}

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
//		connexion= (HttpsURLConnection) url.openConnection();
//		connexion.setRequestMethod("GET");
//		reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));
//		while ((line =reader.readLine())!=null) {
//
//			result += line;
//
//
//
//		}
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
