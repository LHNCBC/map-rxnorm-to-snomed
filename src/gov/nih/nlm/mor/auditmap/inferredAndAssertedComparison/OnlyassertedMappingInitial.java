package gov.nih.nlm.mor.auditmap.inferredAndAssertedComparison;

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
import org.semanticweb.owlapi.model.ClassExpressionType;
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

import gov.nih.nlm.mor.auditmap.definitionalFeaturesAnalysis.bossAndAIsubdivision.ExportFile;
import gov.nih.nlm.mor.auditmap.utilitaries.Couple;
import gov.nih.nlm.mor.auditmap.utilitaries.OntologyClassManagement;
import gov.nih.nlm.mor.util.ExponentialBackoff;
import gov.nih.nlm.mor.util.RestUrl;

public class OnlyassertedMappingInitial {
	public static Set<Couple> AssertedMappings= new HashSet<Couple>();
	private static OWLOntology Ontology;
	public static  Map<String, Set<Couple>> MappingAssetedandInferredSNOMED= new HashMap<String, Set<Couple>>();
	public static OntologyClassManagement classMana;

	private static OWLReasoner Elkreasoner;
	private static OWLOntologyManager manager;
	public static Set<String> RxNormAI = new HashSet<String>();
	public static Set<String> SNOMEDAI = new HashSet<String>();
	public static Set<String> RxNormA = new HashSet<String>();
	public static Set<Couple> CommonMappings= new HashSet<Couple>();
	public static Set<Couple> OnlyAssertedMappings= new HashSet<Couple>();
	public static Set<Couple> AssertedforTags= new HashSet<Couple>();
	public static Set<Couple> OnlyInferredMappings= new HashSet<Couple>();
	public static Set<Couple> InferredMappings= new HashSet<Couple>();
	public static  Map<String, Set<String>> MappingAnalysis= new HashMap<String, Set<String>>();
	public static  Map<String, Set<Couple>> MappingOnlyAsserted= new HashMap<String, Set<Couple>>();
	public static  Map<String, Set<Couple>> MappingOnlyAssertedFortag= new HashMap<String, Set<Couple>>();
	private static ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	public static RestUrl restUrl = new RestUrl();


	//	public static void main(String[] args) throws FileNotFoundException {
	//		// TODO Auto-generated method stub
	//
	//		//String	 path = "/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedToAnalyse.owl";	
	//		//		String path="Q/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2Snomed_2019-12-20_15-37-40.owl";
	//		String path = "Q:/git/MapRxNormToSnomed/RxNorm2Snomed_2020-01-10_12-28-50.owl";
	//
	//
	//		File OntologyPath = new File (path);	
	//		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
	//		Elkreasoner = classMana.getElkreasoner();
	//		manager=classMana.getManager();
	//		Ontology=classMana.getOntology();
	//		getAssertedMapping();
	//		getInferredMappings();
	//		getCommonMappings();
	//		OnlyassertedMappingsCharacteristics();
	//		Set<OWLAxiom> setAxiom = new HashSet<OWLAxiom>();
	//		setAxiom.addAll(OnlyassertedMappingsTags());
	//		setAxiom.addAll(OnlyassertedMappingsTagsSemanticErrors());
	//		System.out.println(" AssertedforTags "+AssertedforTags.size());
	//
	//		Set<String> test = new HashSet<String>();
	//		AssertedforTags.forEach(a->{
	//			test.add(a.y);
	//		});
	//		System.out.println(" test "+test);
	//		System.out.println(" OnlyAssertedMappings "+OnlyAssertedMappings.size());
	//		System.out.println(" test "+test.size());
	//		System.out.println(" AssertedforTags "+AssertedforTags.size());
	//		setAxiom.addAll(gettagENM());
	//
	//		try {
	//			SaveOntology("RxNorm2SNOMEDInitialTag.owl", setAxiom);
	//			System.out.println("finish");
	//		} catch (OWLOntologyStorageException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		analysis();
	//
	//	}

	public OnlyassertedMappingInitial(OntologyClassManagement management) {
		classMana = management;
		Ontology = classMana.getOntology();
		Elkreasoner = classMana.getElkreasoner();
		manager = classMana.getManager();

	}

	public OntologyClassManagement run() {
		try {
			getAssertedMapping();
			getInferredMappings();
			getCommonMappings();
			OnlyassertedMappingsCharacteristics();
			Set<OWLAxiom> setAxiom = new HashSet<OWLAxiom>();
			setAxiom.addAll(OnlyassertedMappingsTags());
			setAxiom.addAll(OnlyassertedMappingsTagsSemanticErrors());
			System.out.println(" AssertedforTags "+AssertedforTags.size());

			Set<String> test = new HashSet<String>();
			AssertedforTags.forEach(a->{
				test.add(a.y);
			});
			System.out.println(" test "+test);
			System.out.println(" OnlyAssertedMappings "+OnlyAssertedMappings.size());
			System.out.println(" test "+test.size());
			System.out.println(" AssertedforTags "+AssertedforTags.size());
			setAxiom.addAll(gettagENM());

			try {
				SaveOntology("RxNorm2SNOMEDInitialTag.owl", setAxiom);
				System.out.println("finish");
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			analysis();	

		} catch(Exception e) {
			System.err.println("Exception in the OnlyassertedMappingInitial run method.");
			e.printStackTrace();
		}

		classMana.setOntology(Ontology);
		classMana.setElkreasoner(Elkreasoner);
		classMana.setManager(manager);
		return classMana;

	}



	public static Set<OWLAxiom> getTag(OWLOntologyManager man,OWLReasoner Elk,OWLClass RxNormClass, Set<OWLClass> Ingredients, String typeOferror){
		OWLDataFactory  factory = man.getOWLDataFactory();
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);

		Ingredients.forEach(ing->{
			if(ing.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
				Set<OWLClass> rerp=Elk.getEquivalentClasses(ing).entities().collect(Collectors.toSet());
				rerp.forEach(op->{
					if(!op.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
						String anno="ENM of "+typeOferror+" between "+ing.getIRI().getShortForm()+" with "+op.getIRI().getShortForm();

						OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
						OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(RxNormClass.getIRI(), annotation);
						ensembleAxiom.add(ax1);
					}
				});

			}
		});

		return ensembleAxiom; 
	}

	public static Set<OWLAxiom> getTag(OWLClass RxNormClass, Set<OWLClass> Ingredients, String typeOferror){

		File OntologyPath = new File ("RxNorm2SnomedSuppressedWrongMappings.owl");

		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		OWLReasoner Elk = classMana.getElkreasoner();
		OWLOntologyManager mana=classMana.getManager();
		OWLOntology Ontol=classMana.getOntology();


		OWLDataFactory  factory = mana.getOWLDataFactory();
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);

		Ingredients.forEach(ing->{
			if(ing.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
				Set<OWLClass> rerp=Elk.getEquivalentClasses(ing).entities().collect(Collectors.toSet());
				rerp.forEach(op->{
					if(!op.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
						String anno="ENM of "+typeOferror+" between "+ing.getIRI().getShortForm()+" with "+op.getIRI().getShortForm();

						OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
						OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(RxNormClass.getIRI(), annotation);
						ensembleAxiom.add(ax1);
					}
				});

			}
			else {
				Set<OWLClassExpression> rerp= EntitySearcher.getEquivalentClasses(ing, Ontol).collect(Collectors.toSet());
				if(rerp.isEmpty()) {
					String anno="ENM of "+typeOferror+" because "+ing.getIRI().getShortForm()+" is erroneously picked";

					OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
					OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(RxNormClass.getIRI(), annotation);
					ensembleAxiom.add(ax1);
				}
			}
		});

		return ensembleAxiom; 
	}
	public static Set<OWLAxiom>  gettagENM(){
		String path ="RxNorm2SNOMEDTag.owl";
		File OntologyPath = new File (path);

		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		OWLReasoner Elk = classMana.getElkreasoner();
		OWLOntologyManager mana=classMana.getManager();
		OWLOntology Ontol=classMana.getOntology();



		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = mana.getOWLDataFactory();
		OWLObjectProperty getPreciseIngredient = factory.getOWLObjectProperty("http://snomed.info/id/762949000");

		OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 

		OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);

		Set<OWLClass> MedicinalProducts=Elk.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
		MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) 
			RxNirMedicinalProducts.add(k);

		});
		RxNirMedicinalProducts.forEach(az->{
			Stream<OWLClassExpression> eq= EntitySearcher.getEquivalentClasses(az, Ontol);
			Set<OWLClass>IngredientsBoss = new HashSet<OWLClass>();
			Set<OWLClass>IngredientsAI = new HashSet<OWLClass>();
			eq.forEach(q->{
				IngredientsBoss.addAll(resultSpecificRelation(q, getBOss));
				IngredientsAI.addAll(resultSpecificRelation(q, getBOss));
			});
			Set<OWLClass> IngredBB= new HashSet<OWLClass>();
			Set<OWLClass> IngredAA= new HashSet<OWLClass>();
			for(OWLClass aaaa: IngredientsBoss) {
				if(!aaaa.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
					IngredBB.add(aaaa);
				}
			}
			for(OWLClass aaaa: IngredAA) {
				if(!aaaa.getIRI().toString().startsWith("http://snomed.info/id/Rx")) {
					IngredAA.add(aaaa);
				}
			}
			ensembleAxiom.addAll(getTag(mana, Elk, az, IngredientsAI, "AI"));
			ensembleAxiom.addAll(getTag(mana, Elk,az, IngredientsBoss, "Boss"));
			ensembleAxiom.addAll(getTag(mana, Elk, az, IngredAA, "AI"));
			ensembleAxiom.addAll(getTag(mana, Elk,az, IngredBB, "Boss"));

		});
		return ensembleAxiom;

	}



	public static void SaveOntology(String path, Set<OWLAxiom>ensembleAxiom) throws OWLOntologyStorageException {
		Stream<OWLAxiom> resul=ensembleAxiom.stream();

		manager.addAxioms(Ontology, resul);
		classMana.flushReasoner();
		File OntologyPath2 = new File (path);
		manager.saveOntology(Ontology, IRI.create(OntologyPath2.toURI()));
	}
	
	public static void getAssertedMapping() {
		Set<OWLClass> SNOMEDclinicalDrugs = new HashSet<OWLClass>();
		Set<OWLClass> SNOMEDMedicinalProducts = new HashSet<OWLClass>();
		Set<String> RxNirMedicinalProducts = new HashSet<String>();
		Map<OWLClass, Set<OWLClass>> sertedMappings= new HashMap<OWLClass, Set<OWLClass>>();
//		File OntologyPath = new File ("Q/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedToAnalyse.owl");
//		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
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
		System.out.println("asserted mapping"+AssertedMappings.size());
		System.out.println("SNOMEDclinicalDrugs  in the asserted "+SNOMEDclinicalDrugs.size());
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

	public static Set<OWLClass> getInferredMappings() {

		Map<OWLClass, Set<OWLClass>> MappingsSCD= new HashMap<OWLClass, Set<OWLClass>>();
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

		// System.out.println("vaccines "+Vaccines.size());
		// System.out.println("RxNormMedicinalProductWithVaccines "+RxNormMedicinalProductWithVaccines.size());
		// System.out.println("RxNormMedicinalProduct "+RxNormMedicinalProduct.size());

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
		return RxNormMedicinalProduct;


	}
	public static void getCommonMappings() {
		Set<String> SnomedCommonMapping = new HashSet<String>();
		AssertedMappings.forEach(a->{
			if(InferredMappings.contains(a)) {
				//	CommonMappings.add(a);
				SnomedCommonMapping.add(a.x);
				RxNormAI.add(a.y);
				SNOMEDAI.add(a.x);
				if(!MappingAssetedandInferredSNOMED.containsKey(a.x)) {
					MappingAssetedandInferredSNOMED.put(a.x, new HashSet<Couple>());
				}
				MappingAssetedandInferredSNOMED.get(a.x).add(a);
			}
			else {
				OnlyAssertedMappings.add(a);
				RxNormA.add(a.y);
				if(!MappingOnlyAsserted.containsKey(a.y)) {
					MappingOnlyAsserted.put(a.y, new HashSet<Couple>());
				}
				MappingOnlyAsserted.get(a.y).add(a);
				//System.out.println("SNO "+a.x+" Rx "+a.y);
			}
		});

		System.out.println("MappingAssetedandInferredSNOMED "+MappingAssetedandInferredSNOMED.size());
		System.out.println("MappingOnlyAsserted "+MappingOnlyAsserted.size());
	}

	public static void OnlyassertedMappingsCharacteristics() throws FileNotFoundException {
		Set<String> AssertedProblems= new HashSet<String>();
		Set<String> OnlyAssertedRxNorm= new HashSet<String>();
		Set<String> AssertedMultiples= new HashSet<String>();
		Set<Couple> AssertedProblemsMappings= new HashSet<Couple>();
		Set<Couple> AssertedProblemsMMMappings= new HashSet<Couple>();
		Set<String> resultatErreoneousMappingsAsserted= new HashSet<String>();
		OWLDataFactory factory=manager.getOWLDataFactory();
		RxNormA.forEach(z->{
			if(RxNormAI.contains(z)) {
				AssertedProblems.add(z);
				MappingOnlyAsserted.forEach((e,f)->{
					if(e.equals(z)) {
						OnlyAssertedRxNorm.add(e);
						AssertedProblemsMappings.addAll(f);
						f.forEach(afgt->{
							try {
								String aVVV = ExportFile.onlyAsserted(factory, Ontology, afgt.y, afgt.x);
								aVVV=aVVV+";R\n";
								resultatErreoneousMappingsAsserted.add(aVVV);
							} catch (FileNotFoundException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							//		System.out.println("Snomed : " + afgt.x + " RxNorm : " + afgt.y);
						}

								);

					}

				});
			}
			else {
				MappingOnlyAsserted.forEach((e,f)->{
					OnlyAssertedRxNorm.add(e);
					if(e.equals(z)) {
						if(f.size()>1) {
							AssertedMultiples.add(z);
							AssertedProblemsMMMappings.addAll(f);
							f.forEach(afgt->{
								try {
									String aVVV = ExportFile.onlyAsserted(factory, Ontology, afgt.y, afgt.x);
									aVVV=aVVV+";M\n";
									resultatErreoneousMappingsAsserted.add(aVVV);
								} catch (FileNotFoundException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								//		System.out.println("Snomed : " + afgt.x + " RxNorm : " + afgt.y);
							}

									);

						}
						else {
							if(!MappingOnlyAssertedFortag.containsKey(e)) {
								MappingOnlyAssertedFortag.put(e, new HashSet<Couple>());
								AssertedforTags.addAll(f);
							}
							MappingOnlyAssertedFortag.get(e).addAll(f);
						}
					}

				});

			}
		});

		String finalres="Rxcui;Label_SCD;SCTID;Label_CD;Tag\n";
		for(String aaaaa:resultatErreoneousMappingsAsserted) {
			finalres=finalres+aaaaa;
		}

		try (PrintWriter out = new PrintWriter("/git/MapRxNormToSnomed/Audit/Livrable/File/FileCSV/ListErroneousAssertionInitial.csv")) {
			out.println(finalres);

			classMana.getReport().createNewSheet("Only asserted mapping initial");
			classMana.getReport().printHeader("Rxcui;Label_SCD;SCTID;Label_CD;Tag", ";");			
			finalres.replace("Rxcui;Label_SCD;SCTID;Label_CD;Tag\n", "");
			
			String[] lines = finalres.split("\n");
			for(String line : lines ) {
				classMana.getReport().print(line, ";");
			}


		} catch(Exception e) {
			System.err.println("Unable to create output for class OnlyassertedMappingInitial.");
			e.printStackTrace();
		}

		SNOMEDAI.forEach(b->{
			MappingOnlyAsserted.forEach((e,f)->{
				f.forEach(afgt->{
					if(afgt.x.equals(b)) {
						AssertedProblemsMappings.addAll(f);
						OnlyAssertedRxNorm.add(e);
					}

				});

			});
		});

		System.out.println("AssertedProblems "+AssertedProblems.size());
		System.out.println("AssertedMultiples "+AssertedMultiples.size());
		System.out.println("AssertedProblemsMappings "+AssertedProblemsMappings.size());
		System.out.println("AssertedProblemsMMMappings "+AssertedProblemsMMMappings.size());
		System.out.println("MappingOnlyAsserted "+MappingOnlyAsserted.keySet().size());
		System.out.println(" AssertedforTags "+AssertedforTags.size());
	}

	public static void analysis() {

		// TODO Auto-generated method stub
		//String path="RxNorm2SNOMEDTag.owl";
		String path ="RxNorm2SNOMEDInitialTag.owl";
		File OntologyPath = new File (path);
		Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();

		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
		OWLReasoner Elk = classMana.getElkreasoner();
		OWLOntologyManager mana=classMana.getManager();
		OWLOntology Ontol=classMana.getOntology();

		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = mana.getOWLDataFactory();

		OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 

		OWLClass MedicinalProductHierarchy=factory.getOWLClass("763158003",pm);

		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);

		Set<OWLClass> MedicinalProducts=Elk.getSubClasses(MedicinalProductHierarchy, false).entities().collect(Collectors.toSet());
		MedicinalProducts.forEach((k)->{if(k.getIRI().toString().startsWith("http://snomed.info/id/Rx")) 
			RxNirMedicinalProducts.add(k);

		});

		Set<OWLAnnotation> annot= new HashSet<OWLAnnotation>();

		Set<OWLClass> SSU = new HashSet<OWLClass>();
		Set<OWLClass> MBO = new HashSet<OWLClass>();
		Set<OWLClass> MAI = new HashSet<OWLClass>();
		Set<OWLClass> SPE = new HashSet<OWLClass>();
		Set<OWLClass> ERR = new HashSet<OWLClass>();         
		Set<OWLClass> BOS = new HashSet<OWLClass>();
		Set<OWLClass> VAL = new HashSet<OWLClass>();
		Set<OWLClass> DFE = new HashSet<OWLClass>();
		Set<OWLClass> SUM = new HashSet<OWLClass>();
		Set<OWLClass> ENM = new HashSet<OWLClass>();
		Set<OWLClass> SAI = new HashSet<OWLClass>();

		Set<String> SSUString = new HashSet<String>();
		Set<String> MBOString = new HashSet<String>();
		Set<String> MAIString = new HashSet<String>();
		Set<String> SPEString = new HashSet<String>();
		Set<String> ERRString = new HashSet<String>();         
		Set<String> BOSString = new HashSet<String>();
		Set<String> VALString = new HashSet<String>();
		Set<String> DFEString = new HashSet<String>();
		Set<String> SUMString = new HashSet<String>();
		Set<String> ENMString = new HashSet<String>();
		Set<String> SAIString = new HashSet<String>();

		Set<OWLClass> RxNormAnnotated = new HashSet<OWLClass>();
		Set<String> RxNormAnnotatedString = new HashSet<String>();
		RxNirMedicinalProducts.forEach(a->{
			for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(a, Ontol, prop).collect(Collectors.toSet())) {
				annot.add(d);

				RxNormAnnotated.add(a);
				RxNormAnnotatedString.add(a.getIRI().getShortForm());
				OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
				String errorTag= vv.getLiteral();
				if (errorTag.startsWith("MAI")) {
					MAI.add(a);
					MAIString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("SSU")) {
					SSU.add(a);
					SSUString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("MBO")) {
					MBO.add(a);
					MBOString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("ERR")) {
					ERR.add(a);
					ERRString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("SPE")) {
					SPE.add(a);
					SPEString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("BOS")) {
					BOS.add(a);
					BOSString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("SAI")) {
					SAI.add(a);
					SAIString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("VAL")) {
					VAL.add(a);
					VALString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("DFE")) {
					DFE.add(a);
					DFEString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("SUM")) {
					SUM.add(a);
					SUMString.add(a.getIRI().getShortForm());
				}
				if (errorTag.startsWith("ENM")) {
					ENM.add(a);
					ENMString.add(a.getIRI().getShortForm());
				}


			}

		});

		Set<String> SSUAO = new HashSet<String>();
		Set<String> MBOAO = new HashSet<String>();
		Set<String> MAIAO = new HashSet<String>();
		Set<String> SPEAO = new HashSet<String>();
		Set<String> ERRAO = new HashSet<String>();         
		Set<String> BOSAO = new HashSet<String>();
		Set<String> VALAO = new HashSet<String>();
		Set<String> DFEAO = new HashSet<String>();
		Set<String> SUMAO = new HashSet<String>();
		Set<String> ENMAO = new HashSet<String>();
		Set<String> SAIAO = new HashSet<String>();


		Set<String> SemanticIssue = new HashSet<String>();
		Set<String> test2 = new HashSet<String>();
		Set<String> quoi = new HashSet<String>();

		MappingOnlyAssertedFortag.forEach((rxNorm,couP)->{
			quoi.add(rxNorm.trim());
			if(SPEString.contains(rxNorm)) {
				SPEAO.add(rxNorm);
			}
			else if(MAIString.contains(rxNorm)) {
				MAIAO.add(rxNorm);
			}
			else if(ENMString.contains(rxNorm)) {
				ENMAO.add(rxNorm);
			}
			else if(SSUString.contains(rxNorm)) {
				SSUAO.add(rxNorm);
			}
			else if(ERRString.contains(rxNorm)) {
				ERRAO.add(rxNorm);
			}
			else if(BOSString.contains(rxNorm)) {
				SemanticIssue.add(rxNorm);
				// BOSAO.add(rxNorm);
			}
			else if(SAIString.contains(rxNorm)) {
				SemanticIssue.add(rxNorm);
				// SAIAO.add(rxNorm);
			}
			else if(SUMString.contains(rxNorm)) {
				SemanticIssue.add(rxNorm);
				// SUMAO.add(rxNorm);
			}

			else if(VALString.contains(rxNorm)) {
				SemanticIssue.add(rxNorm);
				// VALAO.add(rxNorm);
			}
			else if(DFEString.contains(rxNorm)) {
				SemanticIssue.add(rxNorm);
				// DFEAO.add(rxNorm);
			}
			else {
				// SemanticIssue.add(rxNorm);
				OWLClass d = factory.getOWLClass(rxNorm,pm);
				String label="";
				for(OWLAnnotation anno:EntitySearcher.getAnnotationObjects(d, Ontol, factory.getRDFSLabel()).collect(Collectors.toSet())) {

					OWLAnnotationValue j =anno.annotationValue();
					OWLLiteral vv= (OWLLiteral) j;
					label= vv.getLiteral();
				}

				test2.add(rxNorm+" "+label);
			}




		});
		Map<String, Set<String>> getRepartition = new HashMap<String, Set<String>>();
		SemanticIssue.forEach(aRxNorm->{
			String link = "";
			if(BOSString.contains(aRxNorm)) {
				//SemanticIssue.add(rxNorm);
				link=link+"Boss-";
				BOSAO.add(aRxNorm);
			}
			if(SAIString.contains(aRxNorm)) {
				//SemanticIssue.add(rxNorm);
				SAIAO.add(aRxNorm);
				link=link+"SAI-";
			}
			if(SUMString.contains(aRxNorm)) {
				//SemanticIssue.add(rxNorm);
				SUMAO.add(aRxNorm);
				link=link+"SUM-";
			}

			else if(VALString.contains(aRxNorm)) {
				// SemanticIssue.add(rxNorm);
				VALAO.add(aRxNorm);
				link=link+"VAL-";
			}
			else if(DFEString.contains(aRxNorm)) {
				// SemanticIssue.add(rxNorm);
				DFEAO.add(aRxNorm);
				link=link+"DFE-";
			}
			if(!getRepartition.containsKey(link)) {
				getRepartition.put(link, new HashSet<String>());
			}
			getRepartition.get(link).add(aRxNorm);

		});
		getRepartition.forEach((clef,rxnormC)->{
			try {
				ExportFile.RepartitionSemanticError(factory, Ontology, rxnormC, clef.trim());
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(clef+" "+rxnormC.size());
		});
		System.out.println(annot.size());
		System.out.println("RxNormAnnotated "+RxNormAnnotated.size());
		System.out.println("MappingOnlyAssertedFortag "+MappingOnlyAssertedFortag.keySet().size());
		Set<String> test = new HashSet<String>();
		AssertedforTags.forEach(a->{
			if(!RxNormAnnotatedString.contains(a.y)) {

				OWLClass d = factory.getOWLClass(a.y,pm);
				String label="";
				for(OWLAnnotation anno:EntitySearcher.getAnnotationObjects(d, Ontol, factory.getRDFSLabel()).collect(Collectors.toSet())) {

					OWLAnnotationValue j =anno.annotationValue();
					OWLLiteral vv= (OWLLiteral) j;
					label= vv.getLiteral();
				}
				test.add(a.y+" "+label);
			};

		});
		test2.forEach(a->{
			System.out.println(a);
		});
		Set<String> retour =MappingOnlyAssertedFortag.keySet();

		System.out.println(" quoi "+quoi.size());
		System.out.println("test2 "+test2.size());
		System.out.println("MAI "+MAI.size());
		System.out.println("SSU "+SSU.size());
		System.out.println("MBO "+MBO.size());
		System.out.println("ERR "+ERR.size());
		System.out.println("SPE "+SPE.size());
		System.out.println("BOS "+BOS.size());
		System.out.println("SAI "+SAI.size());
		System.out.println("VAL "+VAL.size());
		System.out.println("DFE "+DFE.size());
		System.out.println("SUM "+SUM.size());
		System.out.println("ENM "+ENM.size());


		System.out.println("SPEAO "+SPEAO.size());
		System.out.println("MBOAO "+MBOAO.size());
		System.out.println("MAIAO "+MAIAO.size());
		System.out.println("ENMAO "+ENMAO.size());
		System.out.println("SSUAO "+SSUAO.size());
		System.out.println("ERRAO "+ERRAO.size());
		System.out.println("SemanticIssue "+SemanticIssue.size());

		System.out.println("BOSAO "+BOSAO.size());
		System.out.println("SAIAO "+SAIAO.size());
		System.out.println("VALAO "+VALAO.size());
		System.out.println("DFEAO "+DFEAO.size());
		System.out.println("SUMAO "+SUMAO.size());
		try {
			ExportFile.createdFileSPE(factory,Ontol,SPEAO, "SPE");
			ExportFile.createdFileMAI_ENM_SSU(factory,Ontol,MAIAO, "MAI");
			ExportFile.createdFileMAI_ENM_SSU(factory,Ontol,ENMAO, "ENM");
			ExportFile.createdFileMAI_ENM_SSU(factory,Ontol,SSUAO, "SSU");
			ExportFile.createdFileERR(factory,Ontol,ERRAO, "ERR");
			ExportFile.createdFileBOS_SAI(factory,Ontol,BOSAO, "Boss");
			ExportFile.createdFileBOS_SAI(factory,Ontol,SAIAO, "SAI");
			ExportFile.createdFileDFE(factory,Ontol,DFEAO, "DFE");
			ExportFile.createdFileSUM_VAL(factory,Ontol,SUMAO, "SUM");
			ExportFile.createdFileSUM_VAL(factory,Ontol,VALAO, "VAL");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(" to annotated without "+test.size());




	}


	public static Set<OWLAxiom> OnlyassertedMappingsTagsSemanticErrors() {
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		OWLDataFactory  factory = manager.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLObjectProperty getPreciseIngredient = factory.getOWLObjectProperty("http://snomed.info/id/762949000");
		OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 

		OWLObjectProperty getPresentationNumerotorValue = factory.getOWLObjectProperty("http://snomed.info/id/732944001");
		OWLObjectProperty getPresentationNumerotorUnit = factory.getOWLObjectProperty("http://snomed.info/id/732945000");
		OWLObjectProperty getPresentationDenominatorValue = factory.getOWLObjectProperty("http://snomed.info/id/732946004");
		OWLObjectProperty getPresentationDenominatorUnit = factory.getOWLObjectProperty("http://snomed.info/id/732947008");

		OWLObjectProperty getConcentrationDenominatortorValue = factory.getOWLObjectProperty("http://snomed.info/id/733723002");
		OWLObjectProperty getConcentrationDenominatortorUnit = factory.getOWLObjectProperty("http://snomed.info/id/733722007");
		OWLObjectProperty getConcentrationNumeratorValue = factory.getOWLObjectProperty("http://snomed.info/id/733724008");
		OWLObjectProperty getConcentrationNumeratorUnit = factory.getOWLObjectProperty("http://snomed.info/id/733725009");

		OWLObjectProperty propertyHasManufacturedDoseForm=factory.getOWLObjectProperty("411116001",pm);
		OWLObjectProperty hasUnitOfPresentation = factory.getOWLObjectProperty("763032000",pm);

		System.out.println("MappingOnlyAssertedFortag "+MappingOnlyAssertedFortag.size());
		MappingOnlyAssertedFortag.forEach((a,b)->{
			OWLClass SCDtoTag=factory.getOWLClass(a,pm);
			Set<OWLClass> ingredientsBoss = new HashSet<OWLClass>();
			//Set<OWLClass> ingredients = new HashSet<OWLClass>();
			Set<OWLClass> ingredientsAI = new HashSet<OWLClass>();

			Set<OWLClass> RxNormPresentationNumeratorValue = new HashSet<OWLClass>();
			Set<OWLClass> RxNormPresentationNumeratorUnit = new HashSet<OWLClass>();
			Set<OWLClass> RxNormPresentationDenominatorValue = new HashSet<OWLClass>();
			Set<OWLClass> RxNormPresentationDenominatorUnit = new HashSet<OWLClass>();

			Set<OWLClass> RxNormConcentrationNumeratorValue = new HashSet<OWLClass>();
			Set<OWLClass> RxNormConcentrationNumeratorUnit = new HashSet<OWLClass>();
			Set<OWLClass> RxNormConcentrationDenominatorValue = new HashSet<OWLClass>();
			Set<OWLClass> RxNormConcentrationDenominatorUnit = new HashSet<OWLClass>();

			Set<OWLClass> RxNormDoseForm = new HashSet<OWLClass>();
			Set<OWLClass> RxNormUOP = new HashSet<OWLClass>();


			String templateRxNorm ="";

			Set<OWLClassExpression> equivalentmapping= EntitySearcher.getEquivalentClasses(SCDtoTag, Ontology).collect(Collectors.toSet());
			/**
			 * the non translate SCD corresponded to DF not mapped
			 */
			if(equivalentmapping.size()==0) {
				ensembleAxiom.addAll(getTagAxioms(SCDtoTag, "DF"));
			}

			/**
			 * extraction of BOSS and AI of RxNorm concepts
			 */
			equivalentmapping.forEach(eq->{
				Set<OWLClass> relatedAI=resultSpecificRelation(eq, getPreciseIngredient);
				for(OWLClass ingt:relatedAI) {
					if(ingt.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
						ingredientsAI.add(ingt);
					}
					else {
						Set<OWLClass> substanceRelated= Elkreasoner.getEquivalentClasses(ingt).entities().collect(Collectors.toSet());
						substanceRelated.forEach(azazz->{
							if(azazz.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
								ingredientsAI.add(azazz);
							}
						});
					}
				}
				// ingredientsAI.addAll();
				//Ingredients.addAll(resultSpecificRelation(q, getIngredient));
				//ingredientsBoss.addAll(resultSpecificRelation(eq, getBOss));
				Set<OWLClass> relatedBoss=resultSpecificRelation(eq, getBOss);
				for(OWLClass ingt:relatedBoss) {
					if(ingt.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
						ingredientsBoss.add(ingt);
					}
					else {
						Set<OWLClass> substanceRelated= Elkreasoner.getEquivalentClasses(ingt).entities().collect(Collectors.toSet());
						substanceRelated.forEach(azazz->{
							if(azazz.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
								ingredientsBoss.add(azazz);
							}
						});
					}
				}
				RxNormPresentationNumeratorValue.addAll(resultSpecificRelation(eq, getPresentationNumerotorValue));
				RxNormPresentationNumeratorUnit.addAll(resultSpecificRelation(eq, getPresentationNumerotorUnit));
				RxNormPresentationDenominatorValue.addAll(resultSpecificRelation(eq, getPresentationDenominatorValue));
				RxNormPresentationDenominatorUnit.addAll(resultSpecificRelation(eq, getPresentationDenominatorUnit));

				RxNormConcentrationNumeratorValue.addAll(resultSpecificRelation(eq, getConcentrationNumeratorValue));
				RxNormConcentrationNumeratorUnit.addAll(resultSpecificRelation(eq, getConcentrationNumeratorUnit));
				RxNormConcentrationDenominatorValue.addAll(resultSpecificRelation(eq, getConcentrationDenominatortorValue));
				RxNormConcentrationDenominatorUnit.addAll(resultSpecificRelation(eq, getConcentrationDenominatortorUnit));

				RxNormDoseForm.addAll(resultSpecificRelation(eq, propertyHasManufacturedDoseForm));
				RxNormUOP.addAll(resultSpecificRelation(eq, hasUnitOfPresentation));

			});

			/**
			 * Extract Boss and AI of asserted Mapped concepts
			 */

			/**
			 * extraction of dose forms and Unit of presentation RxNorm concepts
			 */
			// System.out.println(" rxn "+SCDtoTag+" "+RxNormDoseForm);
			//OWLClass 
			//	         OWLClass RXNormPDF1= null;
			//	         if(RxNormDoseForm.size()>0) {
			//	        	 RXNormPDF1= RxNormDoseForm.iterator().next();
			//	         }

			for(Couple az:b){
				OWLClass snomed = factory.getOWLClass(az.x,pm);

				Set<OWLClass> SNOMEDsubstancesAI = new HashSet<OWLClass>();
				Set<OWLClass> SNOMEDsubstancesBoss = new HashSet<OWLClass>();

				Set<OWLClass> SNOMEDDF = new HashSet<OWLClass>();
				Set<OWLClass> SNOMEDUOP = new HashSet<OWLClass>();

				Set<OWLClass> SNOMEDstrenghtPresentationNumeratorValue = new HashSet<OWLClass>();
				Set<OWLClass> SNOMEDstrenghtPresentationNumeratorUnit = new HashSet<OWLClass>();
				Set<OWLClass> SNOMEDstrenghtPresentationDenominatorValue = new HashSet<OWLClass>();
				Set<OWLClass> SNOMEDstrenghtPresentationDenominatorUnit = new HashSet<OWLClass>();

				Set<OWLClass> SNOMEDstrenghtConcentrationNumeratorValue = new HashSet<OWLClass>();
				Set<OWLClass> SNOMEDstrenghtConcentrationNumeratorUnit = new HashSet<OWLClass>();
				Set<OWLClass> SNOMEDstrenghtConcentrationDenominatorValue = new HashSet<OWLClass>();
				Set<OWLClass> SNOMEDstrenghtConcentrationDenominatorUnit = new HashSet<OWLClass>();

				Set<OWLClassExpression> equivalentmappingSnomed= EntitySearcher.getEquivalentClasses(snomed, Ontology).collect(Collectors.toSet());

				equivalentmappingSnomed.forEach(eq->{
					SNOMEDsubstancesAI.addAll(resultSpecificRelation(eq, getPreciseIngredient));
					SNOMEDsubstancesBoss.addAll(resultSpecificRelation(eq, getBOss));

					SNOMEDDF.addAll(resultSpecificRelation(eq, propertyHasManufacturedDoseForm));
					SNOMEDUOP.addAll(resultSpecificRelation(eq, hasUnitOfPresentation));

					SNOMEDstrenghtPresentationNumeratorValue.addAll(resultSpecificRelation(eq, getPresentationNumerotorValue));
					SNOMEDstrenghtPresentationNumeratorUnit.addAll(resultSpecificRelation(eq, getPresentationNumerotorUnit));
					SNOMEDstrenghtPresentationDenominatorValue.addAll(resultSpecificRelation(eq, getPresentationDenominatorValue));
					SNOMEDstrenghtPresentationDenominatorUnit.addAll(resultSpecificRelation(eq, getPresentationDenominatorUnit));

					SNOMEDstrenghtConcentrationNumeratorValue.addAll(resultSpecificRelation(eq, getConcentrationNumeratorValue));
					SNOMEDstrenghtConcentrationNumeratorUnit.addAll(resultSpecificRelation(eq, getConcentrationNumeratorUnit));
					SNOMEDstrenghtConcentrationDenominatorValue.addAll(resultSpecificRelation(eq, getConcentrationDenominatortorValue));
					SNOMEDstrenghtConcentrationDenominatorUnit.addAll(resultSpecificRelation(eq, getConcentrationDenominatortorUnit));

					//		     		System.out.println("SCDtoTag "+SCDtoTag+" snomed "+snomed+" ingredientsBoss "+ingredientsBoss+" SNOMEDsubstancesBoss "+SNOMEDsubstancesBoss);
					//		     		System.out.println("SCDtoTag "+SCDtoTag+" snomed "+snomed+" ingredientsAI "+ingredientsAI+" SNOMEDsubstancesAI "+SNOMEDsubstancesAI);
					//		        	 
					ensembleAxiom.addAll(getTagsemanticSubstance(SCDtoTag, snomed, ingredientsBoss, SNOMEDsubstancesBoss, "BOS"));
					ensembleAxiom.addAll(getTagsemanticSubstance(SCDtoTag, snomed, ingredientsAI, SNOMEDsubstancesAI, "SAI"));
					OWLClass SNOMEDPDF= SNOMEDDF.iterator().next();
					//OWLClass SNOMEDUOPP= SNOMEDUOP.iterator().next();

					//OWLClass RxNormUOPP= RxNormUOP.iterator().next();
					for(OWLClass RXNormPDF:RxNormDoseForm) {

						if(!RXNormPDF.getIRI().getIRIString().equals(SNOMEDPDF.getIRI().getIRIString())) {
							ensembleAxiom.addAll(getTagsemanticDoseForm(SCDtoTag, snomed, SNOMEDPDF, SNOMEDUOP));
						}
					}
					ensembleAxiom.addAll(getTagsemanticStrenght(SCDtoTag, snomed, RxNormPresentationNumeratorValue, SNOMEDstrenghtPresentationNumeratorValue, "presentation numerator value", "VAL"));
					ensembleAxiom.addAll(getTagsemanticStrenght(SCDtoTag, snomed, RxNormPresentationNumeratorUnit, SNOMEDstrenghtPresentationNumeratorUnit, "presentation numerator unit", "SUM"));
					ensembleAxiom.addAll(getTagsemanticStrenght(SCDtoTag, snomed, RxNormPresentationDenominatorValue, SNOMEDstrenghtPresentationDenominatorValue, "presentation denominator value", "VAL"));
					ensembleAxiom.addAll(getTagsemanticStrenght(SCDtoTag, snomed, RxNormPresentationDenominatorUnit, SNOMEDstrenghtPresentationDenominatorUnit, "presentation denominator unit", "SUM"));


					ensembleAxiom.addAll(getTagsemanticStrenght(SCDtoTag, snomed, RxNormConcentrationNumeratorValue, SNOMEDstrenghtConcentrationNumeratorValue, "Concentration numerator value", "VAL"));
					ensembleAxiom.addAll(getTagsemanticStrenght(SCDtoTag, snomed, RxNormConcentrationNumeratorUnit, SNOMEDstrenghtConcentrationNumeratorUnit, "Concentration numerator unit", "SUM"));
					ensembleAxiom.addAll(getTagsemanticStrenght(SCDtoTag, snomed, RxNormConcentrationDenominatorValue, SNOMEDstrenghtConcentrationDenominatorValue, "Concentration denominator value", "VAL"));
					ensembleAxiom.addAll(getTagsemanticStrenght(SCDtoTag, snomed, RxNormConcentrationDenominatorUnit, SNOMEDstrenghtConcentrationDenominatorUnit, "Concentration denominator unit", "SUM"));

				});
			}				
		});


		return ensembleAxiom;
	}
	public static Set<OWLAxiom> getTagAxioms(OWLClass classtoTag, String errorType, String Ingredient){
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		OWLDataFactory  factory = manager.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
		String anno="";
		if(errorType.equals("NotMapped")) {
			anno="SSU-"+Ingredient;
		}
		else if(errorType.equals("TEMPL")) {
			anno="ERR-with-"+Ingredient;
		}


		OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
		OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(classtoTag.getIRI(), annotation);
		ensembleAxiom.add(ax1);
		return ensembleAxiom;

	}

	public static Set<OWLAxiom> getTagAxioms(OWLClass classtoTag, String errorType){
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		OWLDataFactory  factory = manager.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
		String anno="";
		if(errorType.equals("bos")) {
			anno="MBO";
		}
		else if(errorType.equals("sai")) {
			anno="MAI";
		}
		//	  else if(errorType.equals("NM")) {
		//		  anno="SSU";
		//	  }
		//	  else if(errorType.equals("err")) {
		//		  anno="ERR";
		//	  }
		else if(errorType.equals("SEM")) {
			anno="BOS";
		}
		else if(errorType.equals("Val")) {
			anno="VAL";
		}
		else if(errorType.equals("SUM")) {
			anno="SUM";
		}
		else if(errorType.equals("SAI")) {
			anno="SAI";
		}
		else if(errorType.equals("DF")) {
			anno="SPE";
		}

		OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
		OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(classtoTag.getIRI(), annotation);
		ensembleAxiom.add(ax1);
		return ensembleAxiom;

	}
	public static String gettemplate(OWLClassExpression expr) {

		String a="P";
		String b="PC";
		String C="C";
		String fi="";
		OWLDataFactory  factory = manager.getOWLDataFactory();
		OWLObjectProperty getPresentationValue = factory.getOWLObjectProperty("http://snomed.info/id/732944001");
		OWLObjectProperty getConcentrationValue = factory.getOWLObjectProperty("http://snomed.info/id/733724008");
		String PresentationValue = "732944001";
		String ConcentrationValue ="733724008";


		Set<OWLObjectProperty>obj=expr.objectPropertiesInSignature().collect(Collectors.toSet());
		Set<String>objlist= new HashSet<String>();
		obj.forEach(az->{
			String mot= az.getIRI().getShortForm();
			objlist.add(mot);
		});

		if (objlist.contains(PresentationValue)) {
			if(objlist.contains(ConcentrationValue)) {
				fi=b;
			}
			else {
				fi=a;
			}
		}
		else if(objlist.contains(ConcentrationValue)) {
			fi=C;
		}
		return fi;
	}
	/**
	 * first step of the tagging process. find ingredient, DF and template error
	 * @return
	 */
	public static Set<OWLAxiom>  OnlyassertedMappingsTags() {
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = manager.getOWLDataFactory();
		OWLObjectProperty getPreciseIngredient = factory.getOWLObjectProperty("http://snomed.info/id/762949000");
		OWLObjectProperty getBOss = factory.getOWLObjectProperty("http://snomed.info/id/732943007"); 
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();

		/**
		 * list of asserted mappings to tag
		 */
		MappingOnlyAssertedFortag.forEach((a,b)->{
			OWLClass SCDtoTag=factory.getOWLClass(a,pm);
			Set<OWLClass> ingredientsBoss = new HashSet<OWLClass>();
			Set<OWLClass> ingredients = new HashSet<OWLClass>();
			Set<OWLClass> ingredientsAI = new HashSet<OWLClass>();

			String templateRxNorm ="";

			Set<OWLClassExpression> equivalentmapping= EntitySearcher.getEquivalentClasses(SCDtoTag, Ontology).collect(Collectors.toSet());
			/**
			 * the non translate SCD corresponded to DF not mapped
			 */
			if(equivalentmapping.size()==0) {
				ensembleAxiom.addAll(getTagAxioms(SCDtoTag, "DF"));
			}

			/**
			 * extraction of BOSS and AI
			 */
			equivalentmapping.forEach(eq->{
				Set<OWLClass> relatedAI=resultSpecificRelation(eq, getPreciseIngredient);
				for(OWLClass ingt:relatedAI) {
					if(ingt.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
						ingredientsAI.add(ingt);
					}
					else {
						Set<OWLClass> substanceRelated= Elkreasoner.getEquivalentClasses(ingt).entities().collect(Collectors.toSet());
						substanceRelated.forEach(azazz->{
							if(azazz.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
								ingredientsAI.add(azazz);
							}
						});
					}
				}
				// ingredientsAI.addAll();
				//Ingredients.addAll(resultSpecificRelation(q, getIngredient));
				//ingredientsBoss.addAll(resultSpecificRelation(eq, getBOss));
				Set<OWLClass> relatedBoss=resultSpecificRelation(eq, getBOss);
				for(OWLClass ingt:relatedBoss) {
					if(ingt.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
						ingredientsBoss.add(ingt);
					}
					else {
						Set<OWLClass> substanceRelated= Elkreasoner.getEquivalentClasses(ingt).entities().collect(Collectors.toSet());
						substanceRelated.forEach(azazz->{
							if(azazz.getIRI().getIRIString().startsWith("http://snomed.info/id/Rx")) {
								ingredientsBoss.add(azazz);
							}
						});
					}
				}
				//ingredientsAI.addAll(resultSpecificRelation(eq, getPreciseIngredient));
				//Ingredients.addAll(resultSpecificRelation(q, getIngredient));
				//ingredientsBoss.addAll(resultSpecificRelation(eq, getBOss));
			});

			/**
			 * comparison of template of asserted mappings and creation of annotaton TAG
			 */
			for(OWLClassExpression ap:equivalentmapping) {
				templateRxNorm=gettemplate(ap);
			}

			for(Couple az:b){
				String templateSnomed ="";
				System.out.println("az.x "+az.x+" "+az.y);
				OWLClass snomed = factory.getOWLClass(az.x,pm);

				Set<OWLClassExpression> equivalentmappingSnomed= EntitySearcher.getEquivalentClasses(snomed, Ontology).collect(Collectors.toSet());
				for(OWLClassExpression ap:equivalentmappingSnomed) {
					templateSnomed=gettemplate(ap);
				}
				if(!templateSnomed.equals(templateRxNorm)) {
					ensembleAxiom.addAll(getTagAxioms(SCDtoTag, "TEMPL", snomed.getIRI().getShortForm()));
				}
			}
			// Set<OWLClass> equivalentmapping = new HashSet<OWLClass>();
			// Set<OWLClass> equivalentmapping = new HashSet<OWLClass>();




			/**
			 * Missing Boss and AI tag creation
			 */
			ingredientsAI.forEach(ing->{
				if (ing.getIRI().getShortForm().equals("Rx0")) {
					ensembleAxiom.addAll(getTagAxioms(SCDtoTag, "sai"));
				}
				else {
					ingredients.add(ing);
				}
			});

			ingredientsBoss.forEach(ing->{
				if (ing.getIRI().getShortForm().equals("Rx0")) {
					ensembleAxiom.addAll(getTagAxioms(SCDtoTag, "bos"));
				}
				else {
					ingredients.add(ing);
				}
			});
			/**
			 * specific substance tag creation
			 */
			ingredients.forEach(l->{
				Set<OWLClass> sper1=Elkreasoner.getEquivalentClasses(l).entities().collect(Collectors.toSet());
				Set<OWLClass>  sper= new HashSet<OWLClass>();
				sper1.forEach(rt->{
					if(!rt.getIRI().toString().equals(l.getIRI().toString())) {
						sper.add(rt);
					}
				});
				if(sper.size()==0) {
					ensembleAxiom.addAll(getTagAxioms(SCDtoTag, "NotMapped",l.getIRI().getShortForm()));
				}
			});

		});

		return ensembleAxiom;
	}


	public static Set<OWLAxiom> getTagsemanticStrenght(OWLClass RxNormMP, OWLClass SNOMEDMP, Set<OWLClass>RxNormStrengh, Set<OWLClass>SNOMEDStrengthPrinc, String denominatorOrNumeratorPresentationOrConcentration, String typeOfError) {
		OWLDataFactory  factory = manager.getOWLDataFactory();
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
		Set<String> snomedStrenghElement= new HashSet<String>();
		//Set<OWLClass>RxNormStenght2 =RxNormStrengh;
		//Set<OWLClass>SNOMEDStrength =SNOMEDStrengthPrinc;
		Set<OWLClass>RxNormStenghttoremove =new HashSet<OWLClass>();

		Set<OWLClass>SNOMEDStrengthtoremove =new HashSet<OWLClass>();
		SNOMEDStrengthPrinc.forEach(aer->{
			snomedStrenghElement.add(aer.getIRI().getShortForm());
		});

		RxNormStrengh.forEach(a->{
			Set<OWLClass> eq=	Elkreasoner.getEquivalentClasses(a).entities().collect(Collectors.toSet());
			eq.forEach(er->{
				if(snomedStrenghElement.contains(er.getIRI().getShortForm())) {
					//SNOMEDStrength.remove(er);
					//RxNormStenght2.remove(a);
					SNOMEDStrengthtoremove.add(er);
					RxNormStenghttoremove.add(a);
				}
			});

		});
		Set<OWLClass>RxNormStenght2 = new HashSet<OWLClass>();
		for(OWLClass azr:RxNormStrengh) {
			if(!RxNormStenghttoremove.contains(azr)) {
				RxNormStenght2.add(azr);

			}
		}
		Set<OWLClass>SNOMEDStrength = new HashSet<OWLClass>();
		for(OWLClass azr:SNOMEDStrengthPrinc) {
			if(!SNOMEDStrengthtoremove.contains(azr)) {
				SNOMEDStrength.add(azr);

			}
		}

		String anno="";
		if(RxNormStenght2.size()>0) {
			anno =typeOfError+" with "+SNOMEDMP.getIRI().getShortForm()+" on "+denominatorOrNumeratorPresentationOrConcentration+" ";

			for(OWLClass art:RxNormStenght2) {
				String alh= art.getIRI().getIRIString().replace("http://snomed.info/id/", " ").trim();
				anno=anno+alh+" ";
			}
			anno=anno+"and ";
			String alpha="";
			for(OWLClass art:SNOMEDStrength) {
				anno=anno+art.getIRI().getShortForm()+" ";
				alpha=alpha+art.getIRI().getShortForm();
			}
			if(alpha.equals("")) {
				anno="";
			}
		}
		if(!anno.equals("")) {
			OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
			OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(RxNormMP.getIRI(), annotation);
			ensembleAxiom.add(ax1);
		}
		return ensembleAxiom;

	}

	public static Set<OWLAxiom> getTagsemanticDoseForm(OWLClass RxNormMP, OWLClass SNOMEDMP, OWLClass SNOMEDPDF, Set<OWLClass> SNOMEDUOPP) {
		OWLDataFactory  factory = manager.getOWLDataFactory();
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
		Set<String> doseFormSNs=getRxCodes(RxNormMP);
		//  System.out.println(RxNormMP+" doseFormSNs "+doseFormSNs);
		String doseFormSNString =doseFormSNs.iterator().next();
		OWLClass doseFormSN=factory.getOWLClass("Rx"+doseFormSNString,pm);
		OWLObjectProperty propertyHasManufacturedDoseForm=factory.getOWLObjectProperty("411116001",pm);
		OWLObjectProperty hasUnitOfPresentation = factory.getOWLObjectProperty("763032000",pm);


		OWLClassExpression ex=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, doseFormSN);

		OWLClassExpression ex2=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNOMEDPDF);


		OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
		String anno="";

		if(SNOMEDUOPP.size()>0) {
			OWLClass SNOMEDUOP= SNOMEDUOPP.iterator().next();
			OWLClassExpression exUOP=factory.getOWLObjectSomeValuesFrom(hasUnitOfPresentation, SNOMEDUOP);

			OWLObjectIntersectionOf Inter= factory.getOWLObjectIntersectionOf(ex2,exUOP);
			OWLEquivalentClassesAxiom eq2 = factory.getOWLEquivalentClassesAxiom(ex,Inter);

			if (!Ontology.containsAxiom(eq2)) {
				if (!Ontology.containsAxiom(eq)) {
					anno="DFE with "+SNOMEDMP.getIRI().getShortForm()+" on "+doseFormSNString+" and "+SNOMEDPDF.getIRI().getShortForm()+"+"+SNOMEDUOP.getIRI().getShortForm();
				}
			}
		}
		else {
			if (!Ontology.containsAxiom(eq)) {
				anno="DFE with "+SNOMEDMP.getIRI().getShortForm()+" on "+doseFormSNString+" and "+SNOMEDPDF.getIRI().getShortForm()+" without UOP";
			}
		}
		if(!anno.equals("")) {
			OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
			OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(RxNormMP.getIRI(), annotation);
			ensembleAxiom.add(ax1);
		}
		return ensembleAxiom;

	}
	public static Set<String> getRxCodes(OWLClass code) {

		Set<String> codes = new HashSet<>();

		JSONObject DoseFormForSCD = null;
		//String RxNormCuiString = code.trim();

		String RxNormCuiString = code.getIRI().getShortForm().replace("Rx", " ").trim();

		try {
			String urltest =restUrl.getRestUrl() + "/REST/rxcui/"+RxNormCuiString+"/related.json?rela=has_dose_form";
			DoseFormForSCD = getresult(urltest, null);

		}

		catch(Exception e) {

			System.out.println("Unable to fetch dose form codes for Rxcui: " + RxNormCuiString);

		}



		if( !DoseFormForSCD.isNull("relatedGroup") ) {

			JSONObject propConceptGroup = (JSONObject) DoseFormForSCD.get("relatedGroup");

			if( !propConceptGroup.isNull("conceptGroup") ) {

				JSONArray rxnormConcepGroup = (JSONArray) propConceptGroup.get("conceptGroup");
				for(int i=0; i < rxnormConcepGroup.length(); i++ ) {
					JSONObject listelent= (JSONObject) rxnormConcepGroup.get(i);
					if(!listelent.isNull("conceptProperties")) {
						JSONArray conceptProperties = (JSONArray) listelent.get("conceptProperties");
						for(int u=0; u < conceptProperties.length(); u++ ) {
							JSONObject listDFdescription= (JSONObject) conceptProperties.get(u);
							if(!listDFdescription.isNull("rxcui")) {
								String rxString = listDFdescription.get("rxcui").toString();
								codes.add(rxString);
							}
						}

					}
				}


			}

		}



		return codes;

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


	public static Set<OWLAxiom> getTagsemanticSubstance(OWLClass RxNormMP, OWLClass SNOMEDMP, Set<OWLClass>RxNormIngredient, Set<OWLClass>SNOMEDIngredientPrinc, String typeOfError) {
		OWLDataFactory  factory = manager.getOWLDataFactory();
		Set<OWLAxiom>  ensembleAxiom = new HashSet<OWLAxiom>();
		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
		Set<String> snomedSubstance= new HashSet<String>();
		//Set<OWLClass>RxNormIngredient2 =RxNormIngredient;
		//Set<OWLClass> SNOMEDIngredient=SNOMEDIngredientPrinc;
		Set<OWLClass> SNOMEDtoremove = new HashSet<OWLClass>();
		Set<OWLClass> RxNormtoremove = new HashSet<OWLClass>();
		//Set<OWLClass>RxNormIngredientPrinc
		SNOMEDIngredientPrinc.forEach(aer->{
			snomedSubstance.add(aer.getIRI().getShortForm());
		});
		String anno="";
		System.out.println(typeOfError+" "+RxNormMP+" RxNormIngredient "+RxNormIngredient);
		RxNormIngredient.forEach(a->{
			Set<OWLClass> eq=	Elkreasoner.getEquivalentClasses(a).entities().collect(Collectors.toSet());
			eq.forEach(er->{
				if(snomedSubstance.contains(er.getIRI().getShortForm())) {
					//SNOMEDIngredient.remove(er);
					SNOMEDtoremove.add(er);
					//RxNormIngredient2.remove(a);
					RxNormtoremove.add(a);
				}
			});

		});
		Set<OWLClass>RxNormIngredient2 = new HashSet<OWLClass>();
		for (OWLClass aze:RxNormIngredient) {
			if(!RxNormtoremove.contains(aze)) {
				RxNormIngredient2.add(aze);
			}
		}

		Set<OWLClass>SNOMEDIngredient = new HashSet<OWLClass>();
		for (OWLClass aze:SNOMEDIngredientPrinc) {
			if(!SNOMEDtoremove.contains(aze)) {
				SNOMEDIngredient.add(aze);
			}
		}

		if(RxNormIngredient2.size()>0) {
			anno =typeOfError+" with "+SNOMEDMP.getIRI().getShortForm()+" on ";
			Set<OWLClass> restP= new HashSet<OWLClass>();
			for(OWLClass art:RxNormIngredient2) {
				anno=anno+art.getIRI().getShortForm()+" ";
				restP.add(art);
			}
			anno=anno+"with ";
			for(OWLClass art:SNOMEDIngredient) {
				anno=anno+art.getIRI().getShortForm()+" ";
			}
			if(restP.size()==1) {
				for(OWLClass azrt:restP) {
					if(azrt.getIRI().getShortForm().equals("Rx0")) {
						anno="";
					}
				}
			}
		}
		if(!anno.equals("")) {
			OWLAnnotation annotation = factory.getOWLAnnotation(prop, factory.getOWLLiteral(anno));
			OWLAxiom ax1 = factory.getOWLAnnotationAssertionAxiom(RxNormMP.getIRI(), annotation);
			ensembleAxiom.add(ax1);
		}
		return ensembleAxiom;

	}


}
