package gov.nih.nlm.mor.auditmap.definitionalFeaturesAnalysis;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import gov.nih.nlm.mor.auditmap.utilitaries.OntologyClassManagement;

public class UpdateDoseForms {

	private static OntologyClassManagement classMana = null;
	private static OWLReasoner Elkreasoner = null;
	private static OWLOntologyManager manager= null;
	private static OWLOntology Ontology= null;
	private static Set<String> list= new HashSet<String>();

	//	public static void main(String[] args) throws OWLOntologyStorageException {
	//		// TODO Auto-generated method stub
	//
	//		File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/RxNorm2SnomedIngredientUpdate.owl");
	//		File OntologyPath2 = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/SNOMED2RxNormUpdate.owl");
	//		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
	//		//OWLReasoner elk = classMana.getElkreasoner();
	//		OWLOntologyManager manager=classMana.getManager();
	//		OWLOntology Ontology=classMana.getOntology();
	//		if(Ontology == null ) {
	//			System.out.println("Ontology is null");
	//			System.exit(-1);
	//		}
	//		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
	//		 OWLDataFactory  factory = manager.getOWLDataFactory();
	//		 OWLObjectProperty propertyHasManufacturedDoseForm=factory.getOWLObjectProperty("411116001",pm);
	//		 OWLClass phar=factory.getOWLClass("http://snomed.info/id/736542009");
	//	    
	//		 getList();
	//		 Set<OWLAxiom> axios= new HashSet<OWLAxiom>();
	//		 list.forEach(az->{
	//				String[] lov=az.split(";");
	//				String RxN=lov[0].trim();
	//				String RxNLabel=lov[1];
	//				String SNO=lov[2].trim();
	//				System.out.println("RxNorm: "+RxN+" Label: "+RxNLabel+" SNOMED: "+SNO);
	//				OWLAnnotation commentAnno = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(RxNLabel));
	//				
	//		         OWLClass RxNormClassDf=factory.getOWLClass(RxN,pm);
	//		         OWLAxiom ax = factory.getOWLAnnotationAssertionAxiom(RxNormClassDf.getIRI(), commentAnno);
	//		         /**
	//			         * add RxNorm Dose forms with labels
	//			         */
	//		        axios.add(ax);
	//		        OWLSubClassOfAxiom sub=factory.getOWLSubClassOfAxiom(RxNormClassDf, phar);
	//		        /**
	//		         * add subclass relation for RxNorm Dose Forms
	//		         */
	//		        axios.add(sub);
	//		        OWLClass SNCTClassDf=factory.getOWLClass(SNO,pm);
	//		        
	//		        
	//		         if(RxNormClassDf.getIRI().getShortForm().equals("Rx1649574")) {
	//		        	 OWLClassExpression exalpha=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNCTClassDf);
	//		        	 OWLClassExpression ex =factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, RxNormClassDf);
	//		        	 //System.out.println("exalpha "+exalpha);
	//		        	 OWLObjectProperty hasUnitOfPresentation = factory.getOWLObjectProperty("763032000",pm);
	//		        	 OWLClass vial = factory.getOWLClass("732996003",pm);
	//		        	 
	//		        	 OWLClassExpression exbetha=factory.getOWLObjectSomeValuesFrom(hasUnitOfPresentation, vial);
	//		        	 //System.out.println("exbetha "+exbetha);
	//		        	 OWLObjectIntersectionOf ex2= factory.getOWLObjectIntersectionOf(exbetha,exalpha);
	//		        	 OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
	//		        	 System.out.println("eq "+eq);
	//						/**
	//						 * alternate definition for RxNorm dose forms
	//						 */
	//						axios.add(eq);
	//		         }
	//		         else {
	//		        	 OWLClassExpression ex=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNCTClassDf);
	//		        	 OWLClassExpression ex2=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, RxNormClassDf);
	//		        	 OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
	//						/**
	//						 * alternate definition for RxNorm dose forms
	//						 */
	//						axios.add(eq);
	//		         }
	//				
	//				
	//			});
	//		 
	//		 
	//		 Stream<OWLAxiom> resul=axios.stream();
	//		 
	//		 
	//         manager.addAxioms(Ontology, resul);
	//         manager.saveOntology(Ontology, IRI.create(OntologyPath2.toURI()));
	////       System.out.println("finish ");
	//
	//	}	

	public UpdateDoseForms(OntologyClassManagement management) {
		classMana = management;
		Elkreasoner = classMana.getElkreasoner();
		manager= classMana.getManager();
		Ontology= classMana.getOntology();	
	}

	public OntologyClassManagement run() {

		File OntologyPath2 = new File ("SNOMED2RxNormUpdate.owl");

		PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		OWLDataFactory  factory = manager.getOWLDataFactory();
		OWLObjectProperty propertyHasManufacturedDoseForm=factory.getOWLObjectProperty("411116001",pm);
		OWLClass phar=factory.getOWLClass("http://snomed.info/id/736542009");

		getList();
		Set<OWLAxiom> axios= new HashSet<OWLAxiom>();
		list.forEach(az->{
			String[] lov=az.split(";");
			String RxN=lov[0].trim();
			String RxNLabel=lov[1];
			String SNO=lov[2].trim();
			System.out.println("RxNorm: "+RxN+" Label: "+RxNLabel+" SNOMED: "+SNO);
			OWLAnnotation commentAnno = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(RxNLabel));

			OWLClass RxNormClassDf=factory.getOWLClass(RxN,pm);
			OWLAxiom ax = factory.getOWLAnnotationAssertionAxiom(RxNormClassDf.getIRI(), commentAnno);
			/**
			 * add RxNorm Dose forms with labels
			 */
			axios.add(ax);
			OWLSubClassOfAxiom sub=factory.getOWLSubClassOfAxiom(RxNormClassDf, phar);
			/**
			 * add subclass relation for RxNorm Dose Forms
			 */
			axios.add(sub);
			OWLClass SNCTClassDf=factory.getOWLClass(SNO,pm);

			if(RxNormClassDf.getIRI().getShortForm().equals("Rx1649574")) {
				OWLClassExpression exalpha=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNCTClassDf);
				OWLClassExpression ex =factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, RxNormClassDf);
				//System.out.println("exalpha "+exalpha);
				OWLObjectProperty hasUnitOfPresentation = factory.getOWLObjectProperty("763032000",pm);
				OWLClass vial = factory.getOWLClass("732996003",pm);

				OWLClassExpression exbetha=factory.getOWLObjectSomeValuesFrom(hasUnitOfPresentation, vial);
				//System.out.println("exbetha "+exbetha);
				OWLObjectIntersectionOf ex2= factory.getOWLObjectIntersectionOf(exbetha,exalpha);
				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
				System.out.println("eq "+eq);
				/**
				 * alternate definition for RxNorm dose forms
				 */
				axios.add(eq);
			}
			else {
				OWLClassExpression ex=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, SNCTClassDf);
				OWLClassExpression ex2=factory.getOWLObjectSomeValuesFrom(propertyHasManufacturedDoseForm, RxNormClassDf);
				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ex,ex2);
				/**
				 * alternate definition for RxNorm dose forms
				 */
				axios.add(eq);
			}


		});


		Stream<OWLAxiom> resul=axios.stream();


		manager.addAxioms(Ontology, resul);
		classMana.flushReasoner();
		saveOntology(OntologyPath2);		

		System.out.println("finish ");

		classMana.setManager(manager);
		classMana.setOntology(Ontology);
		classMana.setElkreasoner(Elkreasoner);

		return classMana;
	}

	public void saveOntology(File path) {
		try {
			manager.saveOntology(Ontology, IRI.create(path.toURI()));
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			System.err.println("Unable to save ontology in UpdateDoseForms.");
			e.printStackTrace();
		}		
	}


	public static void getList() {

		list.add("Rx657710;Drug Implant;764296002;Prolonged-release subcutaneous implant (dose form);732996003;Implant (unit of presentation);MGE;Any implant of drug can be see as an Implant Drug. This RxNorm DF seems to be a general description of all the implant form in SNOMED CT.\n");
		list.add("Rx657710;Drug Implant;785910004;Prolonged-release intralesional implant (dose form);732996003;Implant (unit of presentation);MGE;Any implant of drug can be see as an Implant Drug. This RxNorm DF seems to be a general description of all the implant form in SNOMED CT.\n");
		list.add("Rx657710;Drug Implant;764842006;Prolonged-release ocular implant (dose form);732996003;Implant (unit of presentation);MGE;Any implant of drug can be see as an Implant Drug. This RxNorm DF seems to be a general description of all the implant form in SNOMED CT.\n");
		list.add("Rx317678;Enema;385187001;Conventional release rectal solution (dose form);NA;NA;ALT; Enema is a patient-friendly term to designate rectal solution, rectal suspension and rectal suspension. These dose form in SNOMED CT can be see as alternated definition.\n");
		list.add("Rx317678;Enema;385188006;Conventional release rectal suspension (dose form);NA;NA;ALT; Enema is a patient-friendly term to designate rectal solution, rectal suspension and rectal suspension. These dose form in SNOMED CT can be see as alternated definition.\n");
		list.add("Rx317678;Enema;385189003;Conventional release rectal emulsion (dose form);NA;NA;ALT; Enema is a patient-friendly term to invariably designate rectal solution, rectal suspension and rectal suspension. These dose form in SNOMED CT can be see as alternated definition.\n");
		list.add("Rx316949;Injectable Solution;385219001;Conventional release solution for injection (dose form);NA;NA;ALT; injectable solution is defined in RXNorm as <a multiple use solution or reconstituted powder intended to be injected>. Then this dose form can correspondent to multiple dose forms in SNOMED CT without a specific unit of presentation.\n");
		list.add("Rx316949;Injectable Solution;385229008;Conventional release solution for infusion (dose form);NA;NA;ALT; injectable solution is defined in RXNorm as <a multiple use solution or reconstituted powder intended to be injected>. Then this dose form can correspondent to multiple dose forms in SNOMED CT without a specific unit of presentation. Infusion beeing a slow injection. We added this form. \n");
		list.add("Rx316949;Injectable Solution;385223009;Powder for conventional release solution for injection (dose form);NA;NA;ALT; injectable solution is defined in RXNorm as <a multiple use solution or reconstituted powder intended to be injected>. Then this dose form can correspondent to multiple dose forms in SNOMED CT without a specific unit of presentation.\n");
		list.add("Rx316950;Injectable Suspension;385220007;Conventional release suspension for injection (dose form);NA;NA;ALT; injectable suspension is defined in RXNorm as <a multiple use suspension or reconstituted powder intended to be injected>. Then this dose form can correspondent to multiple dose forms in SNOMED CT without a specific unit of presentation.\n");
		list.add("Rx316950;Injectable Suspension;764799005;Conventional release suspension for infusion (dose form);NA;NA;ALT; injectable suspension is defined in RXNorm as <a multiple use suspension or reconstituted powder intended to be injected>. Then this dose form can correspondent to multiple dose forms in SNOMED CT without a specific unit of presentation. Infusion beeing a slow injection. We added this form.\n");
		list.add("Rx316950;Injectable Suspension;385224003;Powder for conventional release suspension for injection (dose form);NA;NA;ALT; injectable suspension is defined in RXNorm as <a multiple use suspension or reconstituted powder intended to be injected>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct without a specific unit of presentation.\n");
		list.add("Rx1649574;Injection;385219001;Conventional release solution for injection (dose form);733026001;Vial (unit of presentation);ALT; injection is defined in RXNorm as <a single use sterile solution, suspension, or reconstituted powder intended for parenteral use>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct with a mandatory unit of presentation.\n");
		list.add("Rx1649574;Injection;385229008;Conventional release solution for infusion (dose form);733026001;Vial (unit of presentation);ALT; injection is defined in RXNorm as <a single use sterile solution, suspension, or reconstituted powder intended for parenteral use>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct with a mandatory unit of presentation.\n");
		list.add("Rx1649574;Injection;385223009;Powder for conventional release solution for injection (dose form);733026001;Vial (unit of presentation);ALT; injection is defined in RXNorm as <a single use sterile solution, suspension, or reconstituted powder intended for parenteral use>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct with a mandatory unit of presentation.\n");
		list.add("Rx1649574;Injection;385220007;Conventional release suspension for injection (dose form);733026001;Vial (unit of presentation);ALT; injection is defined in RXNorm as <a single use sterile solution, suspension, or reconstituted powder intended for parenteral use>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct with a mandatory unit of presentation.\n");
		list.add("Rx1649574;Injection;764799005;Conventional release suspension for infusion (dose form);733026001;Vial (unit of presentation);ALT; injection is defined in RXNorm as <a single use sterile solution, suspension, or reconstituted powder intended for parenteral use>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct with a mandatory unit of presentation.\n");
		list.add("Rx1649574;Injection;385224003;Powder for conventional release suspension for injection (dose form);733026001;Vial (unit of presentation);ALT; injection is defined in RXNorm as <a single use sterile solution, suspension, or reconstituted powder intended for parenteral use>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct with a mandatory unit of presentation.\n");
		list.add("Rx1649574;Injection;421637006;Lyophilized powder for conventional release solution for injection (dose form);733026001;Vial (unit of presentation);ALT; injection is defined in RXNorm as <a single use sterile solution, suspension, or reconstituted powder intended for parenteral use>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct with a mandatory unit of presentation.\n");
		list.add("Rx1649574;Injection;421943006;Lyophilized powder for conventional release suspension for injection (dose form);733026001;Vial (unit of presentation);ALT; injection is defined in RXNorm as <a single use sterile solution, suspension, or reconstituted powder intended for parenteral use>. Then this dose form can correspondent to multiple dose forms in SNOMED Ct with a mandatory unit of presentation.\n");
		list.add("Rx346163;Mucosal Spray;385073003;Conventional release oromucosal spray (dose form);NA;NA;ALT; Mucosal Spray is defined as <a spray intended for use on the mucous membranes>. Unit of presentation is not required \" see metered form\".\n");
		list.add("Rx346163;Mucosal Spray;385074009;Conventional release sublingual spray (dose form);NA;NA;ALT; Mucosal Spray is defined as <a spray intended for use on the mucous membranes>. Unit of presentation is not required \" see metered form\".\n");
		list.add("Rx316956;Mucous Membrane Topical Solution;385070000;Conventional release oromucosal solution (dose form);NA;NA;ALT;Mucous Membrane Topical Solution is defined as <an oral solution intended for use on the mucous membranes>.\n");
		list.add("Rx316956;Mucous Membrane Topical Solution;764788008;Conventional release oropharyngeal solution (dose form);NA;NA;ALT;Mucous Membrane Topical Solution is defined as <an oral solution intended for use on the mucous membranes>.\n");
		list.add("Rx316962;Nasal Solution;422336005;Conventional release nasal solution (dose form);NA;NA;ALT;Nasal Solution also designate Nasal drop in RxNorm.\n");
		list.add("Rx316962;Nasal Solution;385152001;Conventional release nasal drops (dose form);NA;NA;ALT;Nasal Solution also designate Nasal drop in RxNorm.\n");
		list.add("Rx316962;Nasal Solution;385153006;Conventional release solution for nasal drops (dose form);NA;NA;ALT;Nasal Solution also designate Nasal drop in RxNorm.\n");
		list.add("Rx126542;Nasal Spray;385157007;Conventional release nasal spray (dose form);NA;NA;ALT;Unit of presentation is not required \" see metered form\".\n");
		list.add("Rx7670;Ophthalmic Solution;385125006;Conventional release solution for eye drops (dose form);NA;NA;ALT;Ophthalmic Solution also designate eye drop and eye solution in RxNorm. However, 385124005-Conventional release eye drops (dose form) seems to be too general\n");
		list.add("Rx7670;Ophthalmic Solution;422060001;Conventional release eye solution (dose form);NA;NA;ALT;Ophthalmic Solution also designate eye drop and eye solution in RxNorm. However, 385124005-Conventional release eye drops (dose form) seems to be too genera\n");
		list.add("Rx7670;Ophthalmic Solution;385124005;Conventional release eye drops (dose form);NA;NA;ALT;Ophthalmic Solution also designate eye drop and eye solution in RxNorm. However, 385124005-Conventional release eye drops (dose form) seems to be too general\n");
		list.add("Rx316964;Ophthalmic Suspension;422068008;Conventional release suspension for eye drops (dose form);NA;NA;ALT;Ophthalmic suspension also designate eye drop and eye solution in RxNorm. However, 385124005-Conventional release eye drops (dose form) is too general\n");
		list.add("Rx316964;Ophthalmic Suspension;420736004;Conventional release eye suspension (dose form);NA;NA;ALT;Ophthalmic suspension also designate eye drop and eye solution in RxNorm.\n");
		list.add("Rx346169;Oral Gel;385088008;Conventional release dental gel (dose form);NA;NA;ALT; all oral gel are valuable\n");
		list.add("Rx346169;Oral Gel;385038000;Conventional release oral gel (dose form);NA;NA;ALT; all oral gel are valuable\n");
		list.add("Rx346169;Oral Gel;385078007;Conventional release oromucosal gel (dose form);NA;NA;ALT; all oral gel are valuable\n");
		list.add("Rx346171;Oral Paste;385039008;Conventional release oral paste (dose form);NA;NA;ALT; all oral paste are valuable\n");
		list.add("Rx346171;Oral Paste;385079004;Conventional release oromucosal paste (dose form);NA;NA;ALT; all oral paste are valuable\n");
		list.add("Rx316968;Oral Solution;385023001;Conventional release oral solution (dose form);NA;NA;ALT;NA \n");
		list.add("Rx316968;Oral Solution;414951009;Conventional release oral elixir (dose form);NA;NA;ALT;NA\n");
		list.add("Rx316968;Oral Solution;385070000;Conventional release oromucosal solution (dose form);NA;NA;ALT;NA\n");
		list.add("Rx316968;Oral Solution;385019009;Conventional release solution for oral drops (dose form);NA;NA;ALT; 385018001-Conventional release oral drops (dose form) is too general.\n");
		list.add("Rx346164;Oral Spray;784575004;Conventional release oral spray (dose form);732981002;Actuation (unit of presentation);ALT; UOP optional.\n");
		list.add("Rx346164;Oral Spray;385073003;Conventional release oromucosal spray (dose form);732981002;Actuation (unit of presentation);ALT; UOP optional.\n");
		list.add("Rx346164;Oral Spray;385074009;Conventional release sublingual spray (dose form);732981002;Actuation (unit of presentation);ALT; UOP optional \n");
		list.add("Rx316969;Oral Suspension;385024007;Conventional release oral suspension (dose form);733013000;Sachet (unit of presentation);ALT; UOP optional.\n");
		list.add("Rx316969;Oral Suspension;385020003;Conventional release suspension for oral drops (dose form);733013000;Sachet (unit of presentation);ALT; UOP optional. 385018001-Conventional release oral drops (dose form) is too general\n");
		list.add("Rx1649571;Pen Injector;385223009;Powder for conventional release solution for injection (dose form);733006000;Pen (unit of presentation);UOP; As  Auto-Injector, Cartridge, Jet Injector, Pen Injector, Prefilled Syringe it mainly represente a unit of presentation of emulsion, suspension and solution.\n");
		list.add("Rx2107950;Vaginal Insert;385175008;Conventional release vaginal capsule (dose form);732937005;Capsule (unit of presentation);ALT;NA \n");
		list.add("Rx2107950;Vaginal Insert;767059009;Conventional release vaginal pessary (dose form);733007009;Pessary (unit of presentation);ALT;NA \n");
		list.add("Rx2107950;Vaginal Insert;385178005;Conventional release vaginal tablet (dose form);732936001;Tablet (unit of presentation);ALT;NA \n");
	}

}
