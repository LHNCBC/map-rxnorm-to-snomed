package gov.nih.nlm.mor.Snomed;

public class SnomedBasicDoseForm implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8486195911636987943L;
	private String code = null;
	private String name = null;
	
	public SnomedBasicDoseForm(String c, String n) {
		this.code = c;
		this.name = n;
	}
	
	public String getCode() {
		return this.code;
	}
	
	public String getName() {
		return this.name;
	}
	
}
