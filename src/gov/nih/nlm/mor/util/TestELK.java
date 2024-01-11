package gov.nih.nlm.mor.util;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;

public class TestELK {
	
	OWLOntologyManager man = null;
	OWLOntology ontology = null;	
	OWLReasoner reasoner = null;
	OWLReasonerFactory reasonerFactory = null;	
	OWLDataFactory factory = null;
	int cdCount = 0;
	
	
	public static void main(String args[]) {
		TestELK test = new TestELK();
		test.config(args[0]);
		test.run();
	}

	private void run() {
		
		OWLClass medicinalProduct = factory.getOWLClass("http://snomed.info/id/763158003");
		OWLClass substances = factory.getOWLClass("http://snomed.info/id/105590001");
		if(true) {
			System.out.println("HALT");
		}
		
		reasoner.getSubClasses(medicinalProduct, false).entities().filter(x -> !x.getIRI().equals(medicinalProduct.getIRI()) && !x.equals(factory.getOWLNothing()))
		.forEach(y -> {
			if(getRDFSLabel(y).contains("(clinical drug)")) {
				cdCount++;
			}
//			reasoner.getEquivalentClasses(y).entities().filter(z -> !z.getIRI().equals(y.getIRI())).forEach(a -> {
//				//System.out.println(a.getIRI().getIRIString() + " === " + y.getIRI().getIRIString());	
//				if(true) {System.out.println("HALT");}
//				if(gettemplate(a).equalsIgnoreCase("p")) {
//					System.out.println(y.getIRI().getIRIString() + " === P");
//				}
//			});
		});		
		
		System.out.println(cdCount + " total clinical drugs in the SNOMED MP Hierarchy");
		
		reasoner.dispose();
		
	}
	
	public String getRDFSLabel(OWLEntity cls) {
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
	}	
	
	public String gettemplate(OWLClass clz) {

		String a="P";
		String b="PC";
		String C="C";
		String fi="";
		//OWLDataFactory  factory = manager.getOWLDataFactory();
		OWLObjectProperty getPresentationValue = factory.getOWLObjectProperty("http://snomed.info/id/732944001");
		OWLObjectProperty getConcentrationValue = factory.getOWLObjectProperty("http://snomed.info/id/733724008");
		String PresentationValue = "732944001";
		String ConcentrationValue ="733724008";


		Set<OWLObjectProperty>obj=clz.objectPropertiesInSignature().collect(Collectors.toSet());
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

	private void config(String filename) {
		File file = new File(filename);
		
		try {
			man = OWLManager.createOWLOntologyManager();			
			ontology = man.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e1 ) {
			System.out.println("Not sure what could be happening here all of a sudden.");
			e1.printStackTrace();
		}		
		
		reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ontology);
		factory = man.getOWLDataFactory();
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		reasoner.precomputeInferences(InferenceType.DATA_PROPERTY_ASSERTIONS);
		
	}

}
