package gov.nih.nlm.mor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.model.OWLDataFactory;

public class CodesToReport {
	
	final String namespace = new String("http://mor.nlm.nih.gov/RXNORM/"); //TODO: make this configurable	
	public OWLOntology ontology = null;
	public OWLOntologyManager manager = null;
	public String filename = null;
	public PrintWriter pw = null;
	public ArrayList<String> codes = new ArrayList<String>();
	public ArrayList<ReportAtom> reportAtoms = new ArrayList<ReportAtom>();
	public OWLClass medicinalProduct = null;
	public OWLReasoner reasoner = null;
	public OWLReasonerFactory reasonerFactory = null;
	public OWLDataFactory factory = null;
	
	public static void main(String args[]) {
		CodesToReport report = new CodesToReport(args[0], args[1], args[2]);
		report.run();
		report.cleanup();
	}
	
	public CodesToReport(String owlfileName, String codefileName, String reportfileName) {
		try {
			manager = OWLManager.createOWLOntologyManager();			
			ontology = manager.loadOntologyFromOntologyDocument(new File(owlfileName));
		} catch (OWLOntologyCreationException e1 ) {
			System.out.println("Not sure what could be happening here all of a sudden.");
			e1.printStackTrace();
		}
		
		readCodeFile(codefileName);
		
		try {
			pw = new PrintWriter(new File(reportfileName));
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		setConstants();
	}
	
	public void readCodeFile(String filename) {
		FileReader file = null;
		BufferedReader buff = null;
		try {
			file = new FileReader(filename);
			buff = new BufferedReader(file);
			boolean eof = false;
			int colIndex = -1;
			while (!eof) {
				String line = buff.readLine();
				if (line == null)
					eof = true;
				else {	
					String code = line.trim();
					codes.add(code);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Closing the streams
			try {
				buff.close();
				file.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}
	
	public void setConstants() {
		factory = manager.getOWLDataFactory();
		this.medicinalProduct = factory.getOWLClass(namespace, "763158003");
		
		reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ontology);
		factory = manager.getOWLDataFactory();
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		reasoner.precomputeInferences(InferenceType.DATA_PROPERTY_ASSERTIONS);		
	}
	
	public void run() {
		Set<OWLClass> rxClasses = reasoner.getSubClasses(medicinalProduct, false).entities().filter(x -> x.getIRI().getFragment().contains("Rx")).collect(Collectors.toSet());
		Set<String> fragments = new HashSet<>();
		
		for(OWLClass rxClass : rxClasses) {
			fragments.add(rxClass.getIRI().getFragment().toString());
		}
		
		for(String code : codes ) {			
			ReportAtom reportAtom = new ReportAtom();
			reportAtom.setId(code);
			if(fragments.contains(code)) {
				reportAtom.setInOWL(true);
			}
			reportAtoms.add(reportAtom);
		}
		
		for(ReportAtom a : reportAtoms) {
			System.out.println(a.print());
		}
	}
	
	public void cleanup() {		
		pw.close();
	}

}
