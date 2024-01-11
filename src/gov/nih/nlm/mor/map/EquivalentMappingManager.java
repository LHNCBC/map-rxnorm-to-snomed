//this class is to support the standalone mode only

package gov.nih.nlm.mor.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;

import gov.nih.nlm.mor.RxNorm.RxNormSCD;

public class EquivalentMappingManager {
	
	final String namespace = new String("http://snomed.info/id/");
	OWLReasoner reasoner = null;
	OWLDataFactory factory = null;
	OWLOntology ontology = null;
	Set<EquivalentMapping> eqMappings = null;
	TreeMap<OWLClass, ArrayList<OWLClass>> ingMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	TreeMap<OWLClass, ArrayList<OWLClass>> numMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	TreeMap<OWLClass, ArrayList<OWLClass>> scdMap = new TreeMap<OWLClass, ArrayList<OWLClass>>();
	private OWLClass ingRoot = null;
	private OWLClass mpRoot = null;
	private OWLClass numberRoot = null;
//	private OWLClass untranslatedRoot = null;
	
	
	public EquivalentMappingManager(OWLReasoner reasoner, OWLDataFactory factory, OWLOntology ontology, OWLClass ingRoot, OWLClass numRoot, OWLClass mpRoot) {
		this.reasoner = reasoner;
		this.factory = factory;
		this.ontology = ontology;
		this.reasoner.flush();
		this.eqMappings = new HashSet<EquivalentMapping>();
		setConstants();
		setEquivalenceMaps();
	}
	
	public EquivalentMappingManager() {
		
	}
	
	public void setConstants() {
		mpRoot = factory.getOWLClass(namespace + "763158003");
		ingRoot = factory.getOWLClass(namespace + "105590001");
		numberRoot = factory.getOWLClass(namespace + "260299005");
	}
	
	public TreeMap<OWLClass, ArrayList<OWLClass>> getIngredientMap() {
		return this.ingMap;
	}
	
	public TreeMap<OWLClass, ArrayList<OWLClass>> getNumberMap() {
		return this.numMap;
	}
	
	public TreeMap<OWLClass, ArrayList<OWLClass>> getScdMap() {
		return this.scdMap;
	}
	
	public Set<EquivalentMapping> getEquivalentMappings() {
		return this.eqMappings;
	}
	
	public void setEquivalenceMaps() {
		
		//anything that is asserted will not be found in this branch. it's retreed to unstranslated.
		//we will need to include descendants of the untranslated branch and also check for equivalences
		//established by the substance fixes, etc.
		
		Set<OWLClass> mpDescendants = reasoner.getSubClasses(mpRoot, false).entities().filter(a -> a.getIRI().getIRIString().contains("Rx")).collect((Collectors.toSet()));
//		Set<OWLClass> untranslatedDescendants = reasoner.getSubClasses(factory.getOWLClass("http://snomed.info/id/1.000000001E9-FS"), false).entities().filter(a -> a.getIRI().getIRIString().contains("Rx")).collect((Collectors.toSet()));
		mpDescendants.remove(mpRoot);
		Set<OWLClass> allRxClasses = new HashSet<OWLClass>();
		allRxClasses.addAll(mpDescendants);
//		allRxClasses.addAll(untranslatedDescendants);
		
		//eq scds
		for( OWLClass cls : allRxClasses ) {
//			System.out.println(cls.getIRI());
			Set<OWLClass> eqClasses = reasoner.getEquivalentClasses(cls).entities().collect(Collectors.toSet());
			eqClasses.remove(cls);
//			System.out.println(cls + " => " + eqClasses);
			if(!eqClasses.isEmpty()) {
				reasoner.getEquivalentClasses(cls).forEach(c -> {
					if( !c.getIRI().getIRIString().replace(namespace, "").equals(cls.getIRI().getIRIString().replace(namespace, "")) ) {
						addToMap(cls, c, scdMap);
						EquivalentMapping eqm = null;	
						if(cls.getIRI().getIRIString().contains("Rx") ) {
							Integer rxCode = Integer.valueOf(cls.getIRI().getIRIString().replace(namespace + "Rx", ""));						
							RxNormSCD scd = new RxNormSCD(null, rxCode, getRDFSLabelForClass(cls));
							scd.setSnomedCodes();						
							if( !c.getIRI().getIRIString().contains("Rx") ) {
								String codeString = c.getIRI().getIRIString().replace(namespace, "");
								Long snomedCode = Long.valueOf(codeString);						
								eqm = new EquivalentMapping(scd, scd.getCui().toString(), scd.getName(), snomedCode, getRDFSLabelForClass(c));
								eqm.setIsInferred(true);
								eqm.setRxClass(cls);
								eqm.setSnomedClass(c);
								if(!eqm.getRxNormSCD().getSnomedCodes().isEmpty()) {
									eqm.setIsAsserted(true);
								}
								eqMappings.add(eqm);
							}	
							else {					
								eqm = new EquivalentMapping(scd, scd.getCui().toString(), scd.getName(), null, null);
								eqm.setRxClass(cls);
								eqm.setSnomedClass(null);							
								eqm.setIsInferred(false);
								if(eqm.getRxNormSCD().getSnomedCodes().isEmpty()) {
									eqm.setIsAsserted(false);
								}
								else {
									eqm.setIsAsserted(true);
								}
								eqMappings.add(eqm);
							}
						}
						else {
							//do nothing
						}
	//					else {
	//						//do nothing, there is repetition in the map bc equivalance is both classes
	//						System.err.println("The map key is backwards if the reasoner is returning codes out of order.");
	//					}
					}
				});
			}
			else {
				EquivalentMapping eqm = null;
				if(cls.getIRI().getIRIString().contains("Rx") ) {
					Integer rxCode = Integer.valueOf(cls.getIRI().getIRIString().replace(namespace + "Rx", ""));						
					RxNormSCD scd = new RxNormSCD(null, rxCode, getRDFSLabelForClass(cls));
					scd.setSnomedCodes();
					eqm = new EquivalentMapping(scd, scd.getCui().toString(), scd.getName(), null, null);
					eqm.setRxClass(cls);
					if(!eqm.getRxNormSCD().getSnomedCodes().isEmpty()) {
						eqm.setIsAsserted(true);
					}
					else {
						eqm.setIsAsserted(false);  //this shouldn't happen, but we'll see.
					}
					eqMappings.add(eqm);
				}
			}
		}	
		
		//eq ings
		for( OWLClass cls : reasoner.getSubClasses(ingRoot, false).entities().collect((Collectors.toSet())) ) {
//			System.out.println(cls.getIRI());		
			reasoner.getEquivalentClasses(cls).forEach(c -> {
				if( !c.getIRI().getIRIString().replace(namespace, "").equals(cls.getIRI().getIRIString().replace(namespace, "")) ) {
					addToMap(cls, c, ingMap);
				}
			});
		}		
		
		//eq numbers - phasing out eventually from xsd:decimal with DataHasValue
		for( OWLClass cls : reasoner.getSubClasses(numberRoot, false).entities().collect((Collectors.toSet())) ) {
//			System.out.println(cls.getIRI());		
			reasoner.getEquivalentClasses(cls).forEach(c -> {
				if( !c.getIRI().getIRIString().replace(namespace, "").equals(cls.getIRI().getIRIString().replace(namespace, "")) ) {
					addToMap(cls, c, numMap);
				}
			});
		}
	}
	
	public void addToMap(OWLClass c, OWLClass d, TreeMap<OWLClass, ArrayList<OWLClass>> map) {
		if( map.containsKey(c) ) {
			ArrayList<OWLClass> list = map.get(c);
			list.add(d);
			map.put(c, list);
		}
		else {
			ArrayList<OWLClass> list = new ArrayList<OWLClass>();
			list.add(d);
			map.put(c, list);
		}		
	}	
	
	public String getRDFSLabelForLong(Long code) {
		OWLClass cls = factory.getOWLClass(namespace, String.valueOf(code));
		for (OWLAnnotation a : EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
			OWLAnnotationValue val = a.getValue();
			if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
			else return val.toString();
		}

		return null;
	}		

	public String getRDFSLabelForClass(OWLClass cls) {
		try {
			Set<OWLAnnotation> annos = EntitySearcher.getAnnotations(cls, ontology, factory.getRDFSLabel()).collect(Collectors.toSet());
			for (OWLAnnotation a : annos) {
				OWLAnnotationValue val = a.getValue();
				if (val instanceof OWLLiteral) return ((OWLLiteral) val).getLiteral().toString();
				else return val.toString();
			}
		}
		catch(Exception e) {
			System.out.println("Entity search failure on: " + cls.getIRI().getIRIString());
			return null;			
		}

		return null;
	}

}