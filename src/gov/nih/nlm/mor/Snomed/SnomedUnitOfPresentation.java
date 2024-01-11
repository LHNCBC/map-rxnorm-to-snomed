package gov.nih.nlm.mor.Snomed;

public class SnomedUnitOfPresentation  implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2108285711598686392L;
	private Long code = null;
	private String name = null;
	
	public SnomedUnitOfPresentation(Long c, String n) {
		this.code = c;
		this.name = n;
	}
	
	public Long getCode() {
		return this.code;
	}
	
	public String getName() {
		return this.name;
	}	

}
