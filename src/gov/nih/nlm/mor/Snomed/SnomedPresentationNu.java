package gov.nih.nlm.mor.Snomed;

public class SnomedPresentationNu  implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -256889704670730391L;
	private String code = null;
	private String name = null;
	
	public SnomedPresentationNu(String c, String n) {
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
