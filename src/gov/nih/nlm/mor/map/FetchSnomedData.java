package gov.nih.nlm.mor.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import gov.nih.nlm.mor.RxNorm.RxNormIngredient;
import gov.nih.nlm.mor.Snomed.SnomedBoss;
import gov.nih.nlm.mor.Snomed.SnomedConcept;

public class FetchSnomedData implements java.io.Serializable {
	
	public static void main(String[] args) {
		FetchSnomedData report = new FetchSnomedData(args[0], args[1]);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 102585790678881017L;
	OWLOntologyManager man = null;
	OWLOntology ontology = null;
	OWLDataFactory factory = null;
	OWLReasoner reasoner = null;
	OWLReasonerFactory reasonerFactory = null;
	final String namespace = new String("http://snomed.info/id/"); //TODO: make this configurable
	final String medicinalProductId = new String("763158003");
	final String substance = new String("http://snomed.info/id/105590001");
	public static final String manufacturedDoseForm = "http://snomed.info/id/736542009";
	public static final String basicDoseForm = new String("http://snomed.info/id/73647002");
	public static final String hasBasisOfStrengthObjectProperty = new String("http://snomed.info/id/732943007");
	public static final String Role_group = new String("http://snomed.info/id/609096000");
	public static final String Has_basic_dose_form = "http://snomed.info/id/736476002";
	public static final String Has_basis_of_strength_substance = "http://snomed.info/id/732943007";
	public static final String Has_concentration_strength_denominator_unit = "http://snomed.info/id/733722007";
	public static final String Has_concentration_strength_denominator_value = "http://snomed.info/id/733723002";
	public static final String Has_concentration_strength_numerator_unit = "http://snomed.info/id/733725009";
	public static final String Has_concentration_strength_numerator_value = "http://snomed.info/id/733724008";
	public static final String Has_ingredient = "http://snomed.info/id/762951001";
	public static final String Has_active_ingredient = "http://snomed.info/id/127489000";
	public static final String Has_presentation_strength_denominator_unit = "http://snomed.info/id/732947008";
	public static final String Has_presentation_strength_denominator_value = "http://snomed.info/id/732946004";
	public static final String Has_presentation_strength_numerator_unit = "http://snomed.info/id/732945000";
	public static final String Has_presentation_strength_numerator_value = "http://snomed.info/id/732944001";
	public static final String Has_unit_of_presentation = "http://snomed.info/id/763032000";
	public static final String Count_of_base_of_active_ingredient = "http://snomed.info/id/766952006";
	Vector<String> parentheticals = new Vector<String>();
	TreeMap<Integer, SnomedConcept> snomedId2SnomedConcept = new TreeMap<Integer, SnomedConcept>();
	
	
	public PrintWriter pw = null;


//	public FetchSnomedData(String filename) {
	public FetchSnomedData(String filename, String codesInputFilename) {
		man = OWLManager.createOWLOntologyManager();
		try {
			ontology = man.loadOntologyFromOntologyDocument(new File(filename));
		} catch (OWLOntologyCreationException e1 ) {
			e1.printStackTrace();
		}
		System.out.println("Discovering inferences for ontology " + filename);

		reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ontology);
		factory = man.getOWLDataFactory();
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);		

		//		if( !reasoner.isConsistent() ) {
		//			System.out.println("Ontology inconsistent. The program will now exit.");
		//			System.exit(0);
		//		}			

		System.out.println("Finished loading ont...");
		System.out.println("Creating reasoner...");
		
		parentheticals.add("qualifier value");

//		gather();
//		readFile(codesInputFilename);
		try {
//			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File("./unii-coverage-rx.txt")),StandardCharsets.UTF_8),true);
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File("./rx-mod-nomod-ing-names.txt")),StandardCharsets.UTF_8),true);			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.print("Cannot create the printwriter");
			e.printStackTrace();
		}		
		readFile(codesInputFilename);

		reasoner.dispose();

	}
	
	public void readFile(String filename) { 
		FileReader file = null;
		BufferedReader buff = null;
		try {
			file = new FileReader(filename);
			buff = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null)
					eof = true;
				else {
					String[] codes = line.split(" ");
					if(codes != null && codes.length>0) {
//						for(int i=0; i<codes.length; i++) {
//					JSONObject result = null;
//					try {
//						//String encodedSubstance = URLEncoder.encode(substance, StandardCharsets.UTF_8.toString());
//						//result = getresult(url + encodedSubstance);
//						// result = getresult(url + codes[i] + urlParams);
//					} catch(Exception e) {
//						e.printStackTrace();
//					}
					
//					if(result != null ) {
						for(int i=0; i<codes.length; i++) {						
						ArrayList<String> ings = returnIngsFromOwl(codes[i]);
						ings.forEach(x-> {
							pw.print(x + "|");
							pw.flush();
						});
						}
//					String[] pair = line.split("\\|");
//					spellings.add(pair);
					}
					pw.println();					
				}						
				
		}
			pw.close();						
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Closing the streams
			try {
				buff.close();
				file.close();
			} catch (Exception e) {
				System.err.println("Error reading the file " + filename);
				e.printStackTrace();
			}
		}						
	}	
	
	public ArrayList<String> returnIngsFromOwl(String id) {
		ArrayList<String> vals = new ArrayList<String>();
		vals.forEach(x-> {
			String label = getRDFSLabel(factory.getOWLClass(namespace + x));
			vals.add(label);
		});
		return vals;
	}

	public void gather() {
		OWLClass medicinalProduct = null;
		OWLClass nothing = null;
		medicinalProduct = factory.getOWLClass(namespace, medicinalProductId);
		nothing = factory.getOWLClass(OWLRDFVocabulary.OWL_NOTHING.getIRI());

		System.out.println("********** Fetching SNOMED Data **********");
		Set<OWLClass> allChildrenOfMP = reasoner.getSubClasses(medicinalProduct, false).entities().collect(Collectors.toSet());
		allChildrenOfMP.remove(medicinalProduct);
			// This can't be done.  Snomed offers no guarantee a leaf of the medicinal product branch
			// will be a clinical drug.  There are 8,216 leaf nodes in the branch.  However, only 4,848
			// of these are truly clinical drugs.  Many product forms has no clinical drug(s) attached.
			// Snomed, unfortunately, also does not have any STY-like annotations on the classes to distinguish
			// what is a medicinal product/form/clinical drug.  These are currently represented in parenthetical form
			// on the concept.  Parsing of the label is necessary.  Unless we can say...
			// - All snomed clinical drugs have at least one BoSS
			// - If no boss, but this..
			// - And if not that..
			// 
			// As this is not possible, we need to look at every equivalent class and unravel its components.  If an 
			// equivalent class contains a role group with a BoSS, then we can assume it is a clinicalDrug.  If there is no
			// no BoSS, we'll then look for... 
			//
			//		for( OWLClass c : allChildrenOfMP ) {
			//			Set<OWLClass> children = reasoner.getSubClasses(c, true).entities().collect(Collectors.toSet());
			//			if( children.contains(nothing) ) {
			//				leafs.add(c);
			//			}
			//		}
			//		OWLAnnotationProperty prop = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL);
			//		for(OWLClass mp : leafs ) {
			//			Stream<OWLAnnotation> annotations = EntitySearcher.getAnnotations(mp, ontology, prop);
			//			annotations.forEach(System.out::println);
			//		}

		int count = 0;
		long start = System.currentTimeMillis();		
		for( OWLClass knownClass : allChildrenOfMP ) {
			System.out.println("Working with " + knownClass.getIRI().toString() + ": " + getRDFSLabel(knownClass));
			System.out.println();
			
			Set<OWLClass> eqClasses = reasoner.getEquivalentClasses(knownClass).entities().collect(Collectors.toSet());
			//unravel each eqClass into something we can work with and build the model
			for( OWLClass eqClass : eqClasses ) {
				//				ontology.equivalentClassesAxioms(e).forEach(System.out::println);
				//System.out.println("== EQ CLASS");
				ontology.equivalentClassesAxioms(eqClass).forEach(
						x -> x.classExpressions().forEach( y ->
						buildConcept(y, knownClass)));
				++count;
				if( count % 1000 == 0 ) {
					System.out.println(count + " Snomed Concepts built..");
				}
			}
			System.out.println();

		}
		System.out.println(count + " Snomed Concepts built.");
		System.out.println("Finished building Snomed model in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");			
	}
	

	public void buildConcept(OWLClassExpression operand, OWLClass knownClass) {
		if(knownClass.getIRI().toString().equals("http://snomed.info/id/775507007")) {
			System.out.println("Debugging");
		}		
		Integer conceptId = new Integer(knownClass.getIRI().getIRIString().replace(namespace, ""));
		if(operand instanceof OWLRestriction) {
			// Nothing asserted to see here
			System.out.println("Asserted restriction on snomed concept " + conceptId.toString());
		} else if (operand instanceof OWLNaryBooleanClassExpression ){
			SnomedConcept snomedConcept = new SnomedConcept(conceptId, getRDFSLabel(knownClass));
			SnomedBoss boss = new SnomedBoss();
			processInnerNAryExpression(operand, conceptId, snomedConcept, boss);
			snomedId2SnomedConcept.put(conceptId, snomedConcept);
		}		
	}

	public void processInnerNAryExpression(OWLClassExpression operand, Integer conceptId, SnomedConcept snomedConcept, SnomedBoss boss) {
		if (operand instanceof OWLObjectIntersectionOf ) {
			Set<OWLClassExpression> expressionSet = ((OWLNaryBooleanClassExpression) operand).operands().collect(Collectors.toSet());
			for(OWLClassExpression innerOperand : expressionSet ) {
				if (innerOperand instanceof OWLObjectSomeValuesFrom) {
					OWLObjectPropertyExpression a = ((OWLObjectSomeValuesFrom) innerOperand).getProperty();
					OWLObjectSomeValuesFrom restriction = (OWLObjectSomeValuesFrom) innerOperand;
					OWLClassExpression filler = restriction.getFiller();                	
					if( !filler.isAnonymous() ) {
						System.out.println("NOT ANON FLAT: " + getObjectName(a) + " : " + getRDFSLabel(filler.asOWLClass()) );
					}
					else {
						if( filler instanceof OWLObjectSomeValuesFrom ) {
							OWLObjectPropertyExpression b = ((OWLObjectSomeValuesFrom) filler).getProperty();
							OWLObjectSomeValuesFrom restr = (OWLObjectSomeValuesFrom) filler;
							OWLClassExpression filler2 = restr.getFiller();
							System.out.println("FLAT: " + getObjectName(b) + " : " + getRDFSLabel(filler2.asOWLClass()) );
						}
						processInnerNAryExpression(filler, conceptId, snomedConcept, boss);
					}
				}
			}
		}
	}
	
	public void addToConcept(OWLObjectPropertyExpression pe, OWLClass c, SnomedConcept snomedConcept, SnomedBoss boss) {
		String propertyIRIAsString = getObjectIRIAsString(pe);
		String valueIRIAsString = c.getIRI().getIRIString();
		String label = getRDFSLabel(c);
		if( !propertyIRIAsString.equals(Role_group)) {
		switch(propertyIRIAsString) {
		case manufacturedDoseForm: { System.out.println("SETTING MANUDF"); snomedConcept.setManufacturedDoseForm(valueIRIAsString, label); }
		break;
		case Has_basic_dose_form: snomedConcept.setBasicDoseForm(valueIRIAsString, label);
		break;
		case Has_basis_of_strength_substance: boss.setBasisOfSubstanceStrenght(valueIRIAsString, label); 
		break;
		case Has_concentration_strength_denominator_unit: boss.setConcentrationDu(valueIRIAsString, label);
		break;
		case Has_concentration_strength_denominator_value: boss.setConcentrationDv(valueIRIAsString, label, new Float(normalizeLabel(label)));
		break;
		case Has_concentration_strength_numerator_unit: boss.setConcentrationNu(valueIRIAsString, label);
		break;
		case Has_concentration_strength_numerator_value: boss.setConcentrationNv(valueIRIAsString, label, new Float(normalizeLabel(label)));
		break;
		case Has_ingredient: snomedConcept.setHasIngredient(valueIRIAsString, label);
		break;
		case Has_active_ingredient: snomedConcept.setHasActiveIngredient(valueIRIAsString, label);
		break;
		case Has_presentation_strength_denominator_unit: boss.setPresentationDu(valueIRIAsString, label);
		break;
		case Has_presentation_strength_denominator_value: boss.setPresentationDv(valueIRIAsString, label, new Float(normalizeLabel(label)));
		break;
		case Has_presentation_strength_numerator_unit: boss.setPresentationNu(valueIRIAsString, label);
		break;
		case Has_presentation_strength_numerator_value: boss.setPresentationNv(valueIRIAsString, label, new Float(normalizeLabel(label)));
		break;
		case Has_unit_of_presentation: snomedConcept.setUnitOfPresentation(valueIRIAsString, label);
		break;
		case Count_of_base_of_active_ingredient: snomedConcept.setCountOfBaseOfActiveIngredient(valueIRIAsString, label, new Float(normalizeLabel(label)));
		break;
		default:
			break;
		}
		}
	}
	
	public String normalizeLabel(String label) {
		String normalizedLabel = label;
		
		//this is only being done for qualified number labels for now
		//it is necessary for math
		for(String parenthetical : parentheticals ) {
			normalizedLabel = normalizedLabel.replace(" (" + parenthetical + ")", "");
		}
//		System.out.println("NORMALIZED LABEL: " + normalizedLabel);
		return normalizedLabel;
	}
	
	public String getObjectName(OWLObjectPropertyExpression a) {
		String name = null;
		for( OWLObjectProperty op : a.objectPropertiesInSignature().collect(Collectors.toSet()) ) {
			name = getRDFSLabel(op);
		}
		return name;
	}
	
	public String getObjectIRIAsString(OWLObjectPropertyExpression a) {
		String code = null;
		for( OWLObjectProperty op : a.objectPropertiesInSignature().collect(Collectors.toSet()) ) {
			code = op.getIRI().toString();
		}
		return code;
	}	
	
	public String getRDFSLabel(OWLEntity cls) {
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
	}

	public void printRestriction(OWLClassExpression op, OWLClass c) {
		System.out.println("\tRestriction for: " + c.getIRI().toString() + " => "+ op.toString());			
	}

	public TreeMap<Integer, SnomedConcept> returnResults() {
		return snomedId2SnomedConcept;
	}

}
