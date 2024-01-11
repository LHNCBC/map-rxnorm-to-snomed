package gov.nih.nlm.mor.Snomed;

public class SnomedConcentrationNv implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8516848622133245240L;
	private String code = null;
	private String name = null;
	private Float value = null;
	
	public SnomedConcentrationNv(String c, String n) {
		this.code = c;
		this.name = n;
	}
	
	public String getCode() {
		return this.code;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Float getValue() {
		return this.value;
	}
	
	public void setValue(Float f) {
		this.value = f;
	}		

}
