package gov.nih.nlm.mor.util;

import org.semanticweb.owlapi.model.OWLClass;

public class ClassPair {
	OWLClass rxClass = null;
	OWLClass snomedClass = null;
	boolean equivalent = false;
	
	public ClassPair(OWLClass rx, OWLClass snct) {
		this.rxClass = rx;
		this.snomedClass = snct;
	}
	
	public OWLClass getRxClass() {
		return rxClass;
	}

	public void setRxClass(OWLClass rxClass) {
		this.rxClass = rxClass;
	}

	public OWLClass getSnomedClass() {
		return snomedClass;
	}

	public void setSnomedClass(OWLClass snomedClass) {
		this.snomedClass = snomedClass;
	}

	public boolean isEquivalent() {
		return equivalent;
	}

	public void setEquivalent(boolean equivalent) {
		this.equivalent = equivalent;
	}
	
	public boolean hasClass(OWLClass c) {
		if(this.rxClass.equals(c) || this.snomedClass.equals(c)) return true;
		return false;
	}
	
}
