package gov.nih.nlm.mor.util;

public class ReportAtom {
	String id = null;
	boolean inOWL = false;
	boolean equivalent = false;
	
	ReportAtom(String id, boolean nOWL, boolean isEquivalent) {
		this.id = id;
		this.inOWL = nOWL;
		this.equivalent = isEquivalent;
	}
	
	ReportAtom() {
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isInOWL() {
		return inOWL;
	}
	
	public String sInOWL() {
		String inOWL = "";
		if(this.inOWL) {
			inOWL = "x";
		}
		return inOWL;
	}

	public void setInOWL(boolean inOWL) {
		this.inOWL = inOWL;
	}

	public boolean isEquivalent() {
		return equivalent;
	}

	public void setEquivalent(boolean equivalent) {
		this.equivalent = equivalent;
	}
	
	public String print() {
		String p = sInOWL() + "\t\t" + getId();
		return p;
	}

}
