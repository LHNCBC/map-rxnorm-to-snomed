package gov.nih.nlm.mor.Snomed;

public class SnomedDefinitionTemplate implements java.io.Serializable {

	private static final long serialVersionUID = -3041134301623898449L;
	
	private boolean isPresentation = false;
	private boolean isConcentration = false;
	
	public SnomedDefinitionTemplate(String template) {
		if( template.equalsIgnoreCase("p")) {
			isPresentation = true;
			isConcentration = false;
		}
		if( template.equalsIgnoreCase("c")) {
			isPresentation = false;			
			isConcentration = true;
		}
		if( template.equalsIgnoreCase("pc")) {
			isPresentation = true;
			isConcentration = true;
		}
	}
	
	public SnomedDefinitionTemplate() {
		
	}
	
	public void setIsPresentation(boolean b) {
		this.isPresentation = b;
	}
	
	public void setIsConcentration(boolean b) {
		this.isConcentration = b;
	}		
	
	public boolean isIsPresentation() {
		if( isPresentation ) return true; else return false;
	}
	
	public boolean isIsConcentration() {
		if( isConcentration ) return true; else return false;
	}	
	
	public String getTemplateType() {
		String type = null;
		if( isConcentration ) {
			type = "C";
		}		
		if( isPresentation ) {
			type = "P";
		}
		if( isConcentration && isPresentation ) {
			type = "PC";
		}		
		return type;
	}

}
