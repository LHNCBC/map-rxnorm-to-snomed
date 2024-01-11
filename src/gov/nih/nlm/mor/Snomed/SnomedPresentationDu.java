package gov.nih.nlm.mor.Snomed;

public class SnomedPresentationDu implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9009831677061535708L;
	private String code = null;
	private String name = null;
	
	public SnomedPresentationDu(String c, String n) {
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
