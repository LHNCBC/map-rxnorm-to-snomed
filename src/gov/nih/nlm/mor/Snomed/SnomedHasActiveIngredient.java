package gov.nih.nlm.mor.Snomed;

public class SnomedHasActiveIngredient implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7239269079701306503L;
	private String code = null;
	private String name = null;
	
	public SnomedHasActiveIngredient(String c, String n) {
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
