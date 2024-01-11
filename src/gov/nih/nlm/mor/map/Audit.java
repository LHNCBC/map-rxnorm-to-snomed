package gov.nih.nlm.mor.map;

import java.io.File;
import java.io.FileNotFoundException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import gov.nih.nlm.mor.auditmap.definitionalFeaturesAnalysis.FindTheMissingIngredientsEquivalence;
import gov.nih.nlm.mor.auditmap.definitionalFeaturesAnalysis.RemoveWrongMappingsSubstances;
import gov.nih.nlm.mor.auditmap.definitionalFeaturesAnalysis.UpdateDoseForms;
import gov.nih.nlm.mor.auditmap.inferredAndAssertedComparison.GetAssertedMappingsFromRxNorm;
import gov.nih.nlm.mor.auditmap.inferredAndAssertedComparison.OnlyAssertedMappingsAnalysis;
import gov.nih.nlm.mor.auditmap.inferredAndAssertedComparison.OnlyassertedMappingInitial;
import gov.nih.nlm.mor.auditmap.utilitaries.OntologyClassManagement;

public class Audit {
	
	OntologyClassManagement management = null;
	OWLOntologyManager manager = null;
	OWLOntology ontology = null;
	OWLReasoner reasoner = null;
	
	String filename = "Q:/git/MapRxNormToSnomed/RxNorm2Snomed_2020-01-10_12-28-50.owl";
	
	public static void main(String[] args) {
		System.out.println("****** Audit in progress ******");		
		Audit audit = new Audit(args[0]);
		audit.run();
		System.out.println("****** Audit complete ******");
	}
	
	public Audit(String filename) {
		if(!filename.isEmpty()) {
			this.filename = filename;
		}
		this.management = new OntologyClassManagement(new File (filename));
	}
	
	@SuppressWarnings("static-access")
	public void run() {
		//kick everything off
		
		status("finding wrong substance mappings");
		RemoveWrongMappingsSubstances wrongSubstances = new RemoveWrongMappingsSubstances(management);
	
		try {
			management = wrongSubstances.run();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			error("Problem with RemoveWrongMappingsSubstances.");
			e.printStackTrace();
		}

		status("finding missing substance mappings");
		FindTheMissingIngredientsEquivalence findMissingIngs = new FindTheMissingIngredientsEquivalence(management);
		try {
			management = findMissingIngs.run();
		} catch(Exception e) {
			error("Problem with FindTheMissingIngredientsEquivalence class.");
			e.printStackTrace();
		}

		status("updating ambiguous dose forms");		
		UpdateDoseForms updateDoseForms = new UpdateDoseForms(management);
		try {
			management = updateDoseForms.run();
		} catch(Exception e) {
			error("Problem with UpdateDoseForms class.");
			e.printStackTrace();
		}
		
		status("getting all asserted mappings from rxnorm");
		GetAssertedMappingsFromRxNorm assertedMappings = new GetAssertedMappingsFromRxNorm(filename, management);
		try {
			management = assertedMappings.run();
		} catch(Exception e) {
			error("Problem with GetAssertedMappingsFromRxNorm class.");
			e.printStackTrace();
		}

// This might be the same exact class as the one following it.  Initial = draft perhaps?		
//		OnlyassertedMappingInitial assertedInitial = new OnlyassertedMappingInitial(management);
//		try {
//			management = assertedInitial.run();
//		} catch(Exception e) {
//			System.err.println("");
//			e.printStackTrace();
//		}

		status("analyzing the asserted mappings");
		OnlyAssertedMappingsAnalysis assertedAnalysis = new OnlyAssertedMappingsAnalysis(management);
		try {
			management = assertedAnalysis.run();
		} catch(Exception e) {
			error("Problem with OnlyAssertedMappingsAnalysis class.");
			e.printStackTrace();
		}
		
		management.getReport().close();
	}
	
	public void status(String s) {
		System.out.println(s);
	}
	
	public void error(String e) {
		System.err.println(e);
	}
	
}
