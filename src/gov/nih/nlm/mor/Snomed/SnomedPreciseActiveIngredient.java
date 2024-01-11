package gov.nih.nlm.mor.Snomed;

public class SnomedPreciseActiveIngredient implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5602876804963283905L;
	private String code = null;
	private String name = null;

	public SnomedPreciseActiveIngredient(String c, String n) {
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
