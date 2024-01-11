package gov.nih.nlm.mor.Snomed;

public class SnomedCountOfBaseOfActiveIngredient implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6126112734695842546L;
	String code = null;
	String name = null;
	Float value = null;

	public SnomedCountOfBaseOfActiveIngredient(String c, String n) {
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
