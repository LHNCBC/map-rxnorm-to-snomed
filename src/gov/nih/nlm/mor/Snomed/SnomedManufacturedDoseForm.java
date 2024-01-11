package gov.nih.nlm.mor.Snomed;

public class SnomedManufacturedDoseForm implements java.io.Serializable {

	private static final long serialVersionUID = 5472218692905520377L;
	private Long code = null;
	private String name = null;
	
	public SnomedManufacturedDoseForm(Long c, String n) {
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
