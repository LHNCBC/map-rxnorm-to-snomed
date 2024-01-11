package gov.nih.nlm.mor.RxNorm;

import java.util.HashMap;
import java.util.Objects;

import org.json.JSONObject;

public class RxNormDoseForm implements java.io.Serializable {

	private static final long serialVersionUID = 3938462378653440871L;
	
	private String rxcui = null;
	private String name = null;
	private String synonym = null;
	private String tty = null;
	private String language = null;
	private String suppress = null;
	private String umlscui = null;
	
	//With the March edition, RxNav made the decision to supplant
	//denominator unit names where none is given. It was then necessary
	//to analyze all past and present drug products for their most common, agreeable
	//denominator unit name (in contrast with those derived from RXNORM_STRENGTH_STR).
	//Rules were put in place to use EACH (especially solid dose forms such as tablets)
	private String rxnavPreferredDenominatorUnitName = null;
	
	public RxNormDoseForm(Integer code, String label) {
		rxcui = code.toString();
		name = label;
	}
	
	public RxNormDoseForm(JSONObject b) {
		rxcui = b.get("rxcui").toString();
		name = b.get("name").toString();
		synonym = b.get("synonym").toString();
		tty = b.get("tty").toString();
		language = b.get("language").toString();
		suppress = b.get("suppress").toString();
		umlscui = b.get("umlscui").toString(); 
	}
	
	public RxNormDoseForm() {
		
	}

	public String getRxcui() {
		return rxcui;
	}
	
	public String getName() {
		return name;
	}
	
	public String getSynonym() {
		return synonym;
	}
	
	public String getTty() {
		return tty;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public String getSuppress() {
		return suppress;
	}
	
	public String getUmlscui() {
		return umlscui;
	}

	public String getRxnavPreferredDenominatorUnitName() {
		return rxnavPreferredDenominatorUnitName;
	}

	public void setRxnavPreferredDenominatorUnitName(String rxnavPreferredDenominatorUnitName) {
		this.rxnavPreferredDenominatorUnitName = rxnavPreferredDenominatorUnitName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(language, name, rxcui, rxnavPreferredDenominatorUnitName, suppress, synonym, tty, umlscui);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RxNormDoseForm other = (RxNormDoseForm) obj;
		return Objects.equals(language, other.language) && Objects.equals(name, other.name)
				&& Objects.equals(rxcui, other.rxcui)
				&& Objects.equals(rxnavPreferredDenominatorUnitName, other.rxnavPreferredDenominatorUnitName)
				&& Objects.equals(suppress, other.suppress) && Objects.equals(synonym, other.synonym)
				&& Objects.equals(tty, other.tty) && Objects.equals(umlscui, other.umlscui);
	}

}
