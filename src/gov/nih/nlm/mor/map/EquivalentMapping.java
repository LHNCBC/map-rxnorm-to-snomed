package gov.nih.nlm.mor.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import gov.nih.nlm.mor.RxNorm.RxNormSCD;

public class EquivalentMapping implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7520362837282247206L;
	String rxCui = null;
	String rxName = null;
	Long snomedCode = null;
	String snomedName = null;
//	Vector<String> explanation = new Vector<String>();
	HashMap<String, HashMap<OWLClass, ArrayList<OWLClass>>> wrongSomethingExplanation = new HashMap<String, HashMap<OWLClass, ArrayList<OWLClass>>>();	
	RxNormSCD scd = null;
	HashMap<OWLObjectProperty, ArrayList<OWLClass>> explanationMap = new HashMap<OWLObjectProperty, ArrayList<OWLClass>>();
	boolean isAsserted = false;
	boolean isInferred = false;
	boolean isOOS = false;
	
	boolean temporary = false;	
	boolean sameMdf = false;
	boolean sameUnit = true;  //we need to use true because this is an optional
	Vector<Double> vWrongUnitValues = new Vector<Double>();
	Vector<String> vWrongUnits = new Vector<String>();
	Vector<Boolean> vSameBoss = new Vector<Boolean>();
	Vector<Boolean> vSameActIng = new Vector<Boolean>();
	int snomedCountBase = 0;
	int rxCountBase = 0;
	public OWLClass rxClass = null;
	public OWLClass snomedClass = null;
	boolean snomedConcentrationTemplate = false;
	boolean snomedPresentationTemplate = false;	
	
	public EquivalentMapping(RxNormSCD scd, String rx, String rxN, Long sct, String sctName) {
		this.scd = scd;
		this.rxCui = rx;
		this.rxName = rxN;
		this.snomedCode = sct;
		this.snomedName = sctName;
	}
	
	public EquivalentMapping() {
		
	}
	
	public void setExplanationMap(OWLObjectProperty op, OWLClass cls) {
		if( !explanationMap.containsKey(op)) {
			ArrayList<OWLClass> list = new ArrayList<OWLClass>();
			list.add(cls);
			explanationMap.put(op, list);
		}
		else {
			ArrayList<OWLClass> list = explanationMap.get(op);
			if( !list.contains(cls)) {
				list.add(cls);
				explanationMap.put(op, list);
			}
		}
	}
	
	public void setWrongExplanation(String type, OWLClass sctClass, OWLClass bestRxGuess) {
		if( wrongSomethingExplanation.containsKey(type) ) {
			HashMap<OWLClass, ArrayList<OWLClass>> map = wrongSomethingExplanation.get(type);
			if( map.get(sctClass) != null ) {
				ArrayList<OWLClass> list = map.get(sctClass);
				list.add(bestRxGuess);
				map.put(sctClass, list);
				wrongSomethingExplanation.put(type, map);
			}
			else {
				ArrayList<OWLClass> list = new ArrayList<OWLClass>();
				list.add(bestRxGuess);
				map.put(sctClass, list);
				wrongSomethingExplanation.put(type, map);
			}
		}
		else {
			HashMap<OWLClass, ArrayList<OWLClass>> map = new HashMap<OWLClass, ArrayList<OWLClass>>();
			ArrayList<OWLClass> list = new ArrayList<OWLClass>();
			list.add(bestRxGuess);
			map.put(sctClass, list);
			wrongSomethingExplanation.put(type, map);
		}
	}
	
	public HashMap<OWLClass, ArrayList<OWLClass>> getWrongExplanationByType(String type) {
		HashMap<OWLClass, ArrayList<OWLClass>> m = null;
		if( wrongSomethingExplanation.containsKey(type) ) {
			m = wrongSomethingExplanation.get(type);
		}
		return m;
	}
	
	public HashMap<String, HashMap<OWLClass, ArrayList<OWLClass>>> getWrongSomethingExplanation() {
		return this.wrongSomethingExplanation;
	}
	
	public OWLClass getRxClass() {
		return this.rxClass;
	}
	
	public OWLClass getSnomedClass() {
		return this.snomedClass;
	}
	
	public void setRxClass(OWLClass c) {
		this.rxClass = c;
	}
	
	public void setSnomedClass(OWLClass c) {
		this.snomedClass = c;
	}	
	
	public void setRxCountOfBase(Integer i) {
		this.rxCountBase = i;
	}
	
	public int getRxCountOfBase() {
		return this.rxCountBase;
	}
	
	public void setSnomedCountOfBase(int i) {
		this.snomedCountBase = i;
	}
	
	public int getSnomedCountOfBase() {
		return this.snomedCountBase;
	}
	
	public void setExplanationMap(OWLObjectProperty op, ArrayList<OWLClass> list) {
		explanationMap.put(op, list);
	}
	
	public void setIsAsserted(boolean b) {
		this.isAsserted = b;
	}
	
	public void setIsInferred(boolean b) {
		this.isInferred = b;
	}
	
	public HashMap<OWLObjectProperty, ArrayList<OWLClass>> getExplanationMap() {
		return this.explanationMap;
	}
	
	public boolean getIsAsserted() {
		return this.isAsserted;
	}
	
	public boolean getIsInferred() {
		return this.isInferred;
	}
	
	public String getRxCui() {
		return this.rxCui;
	}
	
	public Long getSnomedCode() {
		return this.snomedCode;
	}
	
	public String getRxName() {
		return this.rxName;
	}
	
	public String getSnomedName() {
		return this.snomedName;
	}
	
	public RxNormSCD getRxNormSCD() {
		return this.scd;
	}

	public void setSameMdf(boolean b) {
		this.sameMdf = b;
	}
	
	public boolean getSameMdf() {
		return this.sameMdf;
	}
	
	public void setSameUnit(boolean b) {
		this.sameUnit = b;
	}
	
	public boolean getSameUnit() {
		return this.sameUnit;
	}
	
	public void setSameBoss(Boolean b) {
		this.vSameBoss.add(b);
	}
	
	public Vector<Boolean> getSameBoss() {
		return this.vSameBoss;
	}	
	
	public void setActIng(Boolean b) {
		this.vSameActIng.add(b);
	}
	
	public Vector<Boolean> getActIng() {
		return this.vSameActIng;
	}	
	
	public boolean getTemporary() {
		return this.temporary;
	}
	
	public void setTemporary(boolean b) {
		this.temporary = b;
	}
	
	public void setWrongValues(Double d) {
		this.vWrongUnitValues.add(d);
	}
	
	public Vector<Double> getWrongValues() {
		return this.vWrongUnitValues;
	}
	
	public void setWrongUnits(String s) {
		this.vWrongUnits.add(s);
	}
	
	public Vector<String> getWrongUnits() {
		return this.vWrongUnits;
	}
	
	public void setIsOOS(boolean b) {
		this.isOOS = b;
	}
	
	public boolean getIsOOS() {
		return this.isOOS;
	}
	
	public boolean getSnomedConcentrationTemplate() {
		return this.snomedConcentrationTemplate;
	}
	
	public void setSnomedConcentrationTemplate(boolean b) {
		this.snomedConcentrationTemplate = b;
	}
	
	public boolean getSnomedPresentationTemplate() {
		return this.snomedPresentationTemplate;
	}
	
	public void setSnomedPresentationTemplate(boolean b) {
		this.snomedPresentationTemplate = b;
	}	
	
	
    public synchronized int hashCode() {
        int _hashCode = 1;
        if (this.rxCui != null) {
            _hashCode += this.rxCui.hashCode();
        }
        if (this.snomedCode != null) {
            _hashCode += this.snomedCode.hashCode();
        }
        if (this.rxName != null) {
            _hashCode += this.rxName.hashCode();
        }
        if (this.snomedName != null) {
            _hashCode += this.snomedName.hashCode();
        }
        return _hashCode;
    }
		  
	public boolean equals(Object o) {
		boolean equal = true;
		if (o instanceof EquivalentMapping) {
			String a = this.rxCui;
			String b = ((EquivalentMapping) o).getRxCui();
			if( !a.equals(b) ) {
				equal = false;
			}
			String r = this.rxName;
			String s = ((EquivalentMapping) o).getRxName();
			if( !r.equals(s)) {
				equal = false;
			}
			Long c = this.snomedCode;
			Long d = ((EquivalentMapping) o).getSnomedCode();
			if( c != null && !c.equals(d) ) {
				equal = false;
			}
			String t = this.snomedName;
			String u = ((EquivalentMapping) o).getSnomedName();
			if( t != null && !t.equals(u)) {
				equal = false;
			}
			boolean v = this.isAsserted;
			boolean w = ((EquivalentMapping) o).getIsAsserted();
			if( v != w ) {
				equal = false;
			}
			boolean x = this.isInferred;
			boolean y = ((EquivalentMapping) o).getIsInferred();
			if( x != y ) {
				equal = false;
			}
		}
		else {
			equal = false;
		}		
		return equal;
	}	

}
