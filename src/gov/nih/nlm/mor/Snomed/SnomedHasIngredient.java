package gov.nih.nlm.mor.Snomed;

public class SnomedHasIngredient implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2632395472071105494L;
	private String code = null;
	private String name = null;
	
	public SnomedHasIngredient(String c, String n) {
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
