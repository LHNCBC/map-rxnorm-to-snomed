package gov.nih.nlm.mor.Snomed;

public class SnomedConcentrationNu implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2154942849078257704L;
	private String code = null;
	private String name = null;
	
	public SnomedConcentrationNu(String c, String n) {
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
