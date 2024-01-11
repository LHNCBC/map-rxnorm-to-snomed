package gov.nih.nlm.mor.Snomed;

public class SnomedConcentrationDu implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5369776587672680717L;
	private String code = null;
	private String name = null;
	
	public SnomedConcentrationDu(String c, String n) {
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
