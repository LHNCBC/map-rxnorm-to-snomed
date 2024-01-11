package utilitaries;
import java.io.File;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class OntologyClassManagement {
	private OWLOntology Ontology;

    private OWLReasoner Elkreasoner;
    private OWLOntologyManager manager;
    public OntologyClassManagement( File OntologyPath) {
		// TODO Auto-generated constructor stub
    	try {
			LaodOntology(OntologyPath);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
    
    public  void LaodOntology( File OntologyPath) throws OWLOntologyCreationException {

        
    	manager = OWLManager.createOWLOntologyManager();
        Ontology=manager.loadOntologyFromOntologyDocument(OntologyPath);

        System.out.println("ontology ok");

        ElkReasonerFactory reasonerForOntology= new ElkReasonerFactory();

        System.out.println("reasoner ok");

        Elkreasoner=reasonerForOntology.createReasoner(Ontology);

        System.out.println("inferred ontology ok");

       

    }

	public OWLOntology getOntology() {
		return Ontology;
	}

	public void setOntology(OWLOntology ontology) {
		Ontology = ontology;
	}

	public OWLReasoner getElkreasoner() {
		return Elkreasoner;
	}

	public void setElkreasoner(OWLReasoner elkreasoner) {
		Elkreasoner = elkreasoner;
	}

	public OWLOntologyManager getManager() {
		return manager;
	}

	public void setManager(OWLOntologyManager manager) {
		this.manager = manager;
	}

}


