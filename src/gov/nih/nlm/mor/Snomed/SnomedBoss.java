package gov.nih.nlm.mor.Snomed;

public class SnomedBoss implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1445020787591993779L;
	private SnomedPresentationNv presentationNv = null;
	private SnomedPresentationNu presentationNu = null;
	private SnomedPresentationDv presentationDv = null;
	private SnomedPresentationDu presentationDu = null;
	private SnomedConcentrationNv concentrationNv = null;
	private SnomedConcentrationNu concentrationNu = null;
	private SnomedConcentrationDv concentrationDv = null;
	private SnomedConcentrationDu concentrationDu = null;
	private SnomedPreciseActiveIngredient preciseActiveIngredient = null;
	private SnomedBasisOfSubstanceStrength basisOfSubstanceStrength = null;
	boolean isConcentration = false;
	
	public SnomedBoss() {
		
	}
	
	public void setIsConcentration() {
		if((this.concentrationDu != null && this.concentrationDv != null && this.concentrationNu != null && this.concentrationNv != null)) {
			this.isConcentration = true;
		}
	}
	
	public boolean isConcentration() {
		return this.isConcentration;
	}
	
	public SnomedBasisOfSubstanceStrength getBasisOfSubstanceStrength() {
		return basisOfSubstanceStrength;
	}
	
	public void setBasisOfSubstanceStrenght(String code, String label) {
		this.basisOfSubstanceStrength = new SnomedBasisOfSubstanceStrength(code, label);
	}

	public SnomedPresentationNv getPresentationNv() {
		return presentationNv;
	}

	public void setPresentationNv(String code, String label, Float presentationNv) {
		this.presentationNv = new SnomedPresentationNv(code, label);
		this.presentationNv.setValue(new Float(presentationNv));
	}

	public SnomedPresentationNu getPresentationNu() {
		return presentationNu;
	}

	public void setPresentationNu(String code, String label) {
		this.presentationNu = new SnomedPresentationNu(code, label);
	}

	public Float getPresentationDv() {
		return presentationDv.getValue();
	}

	public void setPresentationDv(String code, String label, Float presentationDv) {
		this.presentationDv = new SnomedPresentationDv(code, label);
		this.presentationDv.setValue(new Float(presentationDv));
	}

	public SnomedPresentationDu getPresentationDu() {
		return presentationDu;
	}

	public void setPresentationDu(String code, String label) {
		this.presentationDu = new SnomedPresentationDu(code, label);
	}

	public SnomedConcentrationNv getConcentrationNv() {
		return concentrationNv;
	}

	public void setConcentrationNv(String code, String label, Float concentrationNv) {
		this.concentrationNv = new SnomedConcentrationNv(code, label);
		this.concentrationNv.setValue(new Float(concentrationNv));
	}

	public SnomedConcentrationNu getConcentrationNu() {
		return concentrationNu;
	}

	public void setConcentrationNu(String code, String label) {
		this.concentrationNu = new SnomedConcentrationNu(code, label);
	}

	public SnomedConcentrationDv getConcentrationDv() {
		return concentrationDv;
	}

	public void setConcentrationDv(String code, String label, Float concentrationDv) {
		this.concentrationDv = new SnomedConcentrationDv(code, label);
		this.concentrationDv.setValue(new Float(concentrationDv));
	}

	public SnomedConcentrationDu getConcentrationDu() {
		return this.concentrationDu;
	}

	public void setConcentrationDu(String code, String label) {
		this.concentrationDu = new SnomedConcentrationDu(code, label);
	}
	
	public boolean isFull() {
		boolean full = false;
		if( this.basisOfSubstanceStrength != null && ((this.presentationDu != null && this.presentationDv != null && this.presentationNu != null && this.presentationNv != null) || 
			(this.concentrationDu != null && this.concentrationDv != null && this.concentrationNu != null && this.concentrationNv != null)) ) {
			full = true;
		}
		return full;
	}

	public SnomedPreciseActiveIngredient getPreciseActiveIngredient() {
		return preciseActiveIngredient;
	}

	public void setPreciseActiveIngredient(String code, String label) {
		this.preciseActiveIngredient = new SnomedPreciseActiveIngredient(code, label);
	}

	public void print() {
		System.out.println("\t\tBoSS\t=> " + this.basisOfSubstanceStrength.getName() + " : " + this.basisOfSubstanceStrength.getCode() );
		if( !this.isConcentration) {
			System.out.println("\t\t\t(Presentation)");
			System.out.println("\t\t\t nu\t=> " + this.presentationNu.getName() + " : " + this.presentationNu.getCode());
			System.out.println("\t\t\t nv\t=> " + this.presentationNv.getName() + " : " + this.presentationNv.getCode());
			System.out.println("\t\t\t du\t=> " + this.presentationDu.getName() + " : " + this.presentationDu.getCode());
			System.out.println("\t\t\t dv\t=> " + this.presentationDv.getName() + " : " + this.presentationDv.getCode());			
		}
		else {
			System.out.println("\t\t\t(Concentration)");
			System.out.println("\t\t\t nu\t=> " + this.concentrationNu.getName() + " : " + this.concentrationNu.getCode());
			System.out.println("\t\t\t nv\t=> " + this.concentrationNv.getName() + " : " + this.concentrationNv.getCode());
			System.out.println("\t\t\t du\t=> " + this.concentrationDu.getName() + " : " + this.concentrationDu.getCode());
			System.out.println("\t\t\t dv\t=> " + this.concentrationDv.getName() + " : " + this.concentrationDv.getCode());			
		}
		
	}


}
