package gov.nih.nlm.mor.Snomed;

public class SnomedDFPair implements java.io.Serializable {

	private static final long serialVersionUID = 4111727229032863096L;
	public SnomedManufacturedDoseForm df = null;
	public SnomedUnitOfPresentation up = null;
	public SnomedDefinitionTemplate template = null;
	
	public SnomedDFPair(SnomedManufacturedDoseForm dform, SnomedUnitOfPresentation upres, SnomedDefinitionTemplate t) {
		this.df = dform;
		this.up = upres;
		this.template = t;
	}
	
	public SnomedDFPair() {
		
	}
	
	public boolean isNull() {
		boolean isNull = false;
		if( this.df == null && this.up == null ) {
			isNull = true;
		}
		return isNull;
	}
	
	public void setDF(SnomedManufacturedDoseForm df) {
		this.df = df;
	}
	
	public void setUP(SnomedUnitOfPresentation up) {
		this.up = up;
	}
	
	public boolean hasDF() {
		boolean hasDF = false;
		if( this.df != null && (df.getName() != null && !df.getName().isEmpty()) &&
				(df.getCode() != null) ) {
			hasDF = true;
		}
		return hasDF;
	}
	
	public boolean hasUP() {
		boolean hasUP = false;
		if( this.up != null && (up.getName() != null && !up.getName().isEmpty()) && 
				(up.getCode() != null) ) {
			hasUP = true;
		}
		return hasUP;
	}	
	
	public void print() {
		String dfString = this.df == null ? "\t" : this.df.getCode() + "\t" + this.df.getName();
		String upString = this.up == null ? "\t" : this.up.getCode() + "\t" + this.up.getName();
		System.out.print("\t" + dfString);
		System.out.print("\t" + upString);		
	}
	
	public SnomedDefinitionTemplate getSnomedDefinitionTemplate() { 
		if( template != null ) {
			return this.template;
		}
		else {
			return null;  //going to need to check this just for injectables
		}
	}
	
	public SnomedManufacturedDoseForm getSnomedManufacturedDoseForm() {
		return this.df;
	}
	
	public SnomedUnitOfPresentation getSnomedUnitOfPresentation() {
		return this.up;
	}
	
    public synchronized int hashCode() {
        int _hashCode = 1;
        if (this.df != null) {
            _hashCode += this.df.getCode().hashCode();
        }
        if (this.up != null) {
            _hashCode += this.up.getCode().hashCode();
        }
        return _hashCode;
    }
		  
	public boolean equals(Object o) {
		boolean equal = true;
		if (o instanceof SnomedDFPair) {
			String a = "";
			String b = "";
			if( this.hasDF() ) {
				a = this.df.getName();
			}
			if( ((SnomedDFPair) o).hasDF() ) {
				b = ((SnomedDFPair) o).getSnomedManufacturedDoseForm().getName();
			}
			if( !a.equals(b) ) {
				equal = false;
			}
			a = "";
			b = "";
			if( this.hasUP() ) {
				a = this.up.getName();
			}
			if( ((SnomedDFPair) o).hasUP() ) {
				b = ((SnomedDFPair) o).getSnomedUnitOfPresentation().getName();
			}
			if( !a.equals(b)) {
				equal = false;
			}
		}
		else {
			equal = false;
		}		
		return equal;
	}	

}
