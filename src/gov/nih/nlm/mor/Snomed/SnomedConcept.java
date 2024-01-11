package gov.nih.nlm.mor.Snomed;

import java.util.Vector;


//The "half-baked" method approach that should work just fine for
//a LHS & RHS model comparison. Was decided to serialize OWL all with SCT
//classes (without knowing what is in SCT).  And so, a model comparison would
//more than likely follow many of the business rules already in place in both
//the Analysis and the (now unused) GenerateEquivalencies.java.  GenerateEquivalencies.java
//was later forked to GenerateEquivalenciesSnomedClasses.java.  Stuff was added, stuff was subtracted
//for that class.  It works, though does not provide enough granularity this approach would.
//When untangling SCT into the model, the difficulty is when to end the streamed recursive method when/if
//a BoSS has been completely finished and it is time to move on to the next BoSS.  The method in charge of
//this "probing" (which would had been setting) is fine.  Passing a boolean along with it is fine.
//The difficult part is just determining how to compare a RxNorm SCD with 0...n valid BoSSs to a SCT
//class with 1..n BoSSs.  There has to be an analyst who knows how to handle this cross comparison in
//a logical, meaninngful way without any lexical involvement, without any ing manipulation, etc.
//It is good to have this class stick around just in case we'd ever want to try this approach again.
//It's not my call? :-)
public class SnomedConcept implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6753767780038231349L;
	private Integer conceptCode = null;
	private String conceptName = null;
	private SnomedManufacturedDoseForm manufacturedDoseForm = null;
	private SnomedBasicDoseForm basicDoseForm = null;	
	private SnomedUnitOfPresentation unitOfPresentation = null;
	private SnomedCountOfBaseOfActiveIngredient countOfBaseOfActiveIngredient = null;
	private SnomedHasActiveIngredient hasActiveIngredient = null;
	private SnomedHasIngredient hasIngredient = null;  //is this ever used? for what?
	private Vector<SnomedBoss> vBoss = new Vector<SnomedBoss>();	
	boolean isClinicalDrug = false;
	boolean isMedicinalProductFormOnly = false;	
	boolean isMedicinalProductForm = false;	
	boolean isMedicinalProduct = false;
	boolean isProduct = false;		
	
	public SnomedConcept(Integer code, String name) {
		this.conceptCode = new Integer(code);
		this.conceptName = new String(name);
	}
	
	public Integer getConceptCode() {
		return this.conceptCode;
	}
	
	public String getConceptName() {
		return this.conceptName;
	}
	
	public void setHasActiveIngredient(String code, String label) {
		this.hasActiveIngredient = new SnomedHasActiveIngredient(code, label);		
	}
	
	public void setHasIngredient(String code, String label) {
		this.hasIngredient = new SnomedHasIngredient(code, label);
	}
	
	public void setManufacturedDoseForm(String code, String label) {
		this.manufacturedDoseForm = new SnomedManufacturedDoseForm(Long.valueOf(code), label);
	}
	
	public void setUnitOfPresentation(String code, String label) {
		unitOfPresentation = new SnomedUnitOfPresentation(Long.valueOf(code), label);
	}
	
	public void setCountOfBaseOfActiveIngredient(String code, String label, Float value) {
		this.countOfBaseOfActiveIngredient = new SnomedCountOfBaseOfActiveIngredient(code, label);
		this.countOfBaseOfActiveIngredient.setValue(new Float(value));
	}
	
	public void setVBoss(SnomedBoss boss) {
		if( !this.vBoss.contains(boss)) {
			boss.setIsConcentration();
			vBoss.add(boss);
		}
	}
	
	public void setBasicDoseForm(String code, String label) {
		basicDoseForm = new SnomedBasicDoseForm(code, label);
	}
	
	public SnomedHasActiveIngredient getHasActiveIngredient() {
		return this.hasActiveIngredient;
	}
	
	public SnomedHasIngredient getHasIngredient() {
		return this.hasIngredient;
	}
	
	public SnomedManufacturedDoseForm getManufacturedDoseForm() {
		return this.manufacturedDoseForm;
	}
	
	public SnomedUnitOfPresentation getUnitOfPresentation() {
		return this.unitOfPresentation;
	}
	
	public SnomedCountOfBaseOfActiveIngredient getCountOfBaseOfActiveIngredient() {
		return this.countOfBaseOfActiveIngredient;
	}
	
	public Vector<SnomedBoss> getVBoss() {
		return vBoss;
	}
	
	public SnomedBasicDoseForm getBasicDoseForm() {
		return basicDoseForm;
	}
	
	public void print() {
		System.out.println(conceptCode.toString() + ": " + conceptName);
		if( this.manufacturedDoseForm != null ) {
			System.out.println("\tManufactured Dose From\t=>" + this.manufacturedDoseForm.getCode() + " : " + this.manufacturedDoseForm.getName());
		}
		if( this.basicDoseForm != null ) {
			System.out.println("\tBasic Dose Form\t=> " + this.basicDoseForm.getCode() + " : " + this.basicDoseForm.getName());
		}
		if( this.unitOfPresentation != null ) {
			System.out.println("\tUnit of Presentation\t=> " + this.unitOfPresentation.getCode() + " : " + this.unitOfPresentation.getName() );
		}
		if( this.countOfBaseOfActiveIngredient != null) {
			System.out.println("\tCount of Base of Active Ingredient\t=> " + this.countOfBaseOfActiveIngredient.getCode() + " : " + this.countOfBaseOfActiveIngredient.getName());			
		}
		if( this.hasActiveIngredient != null ) {
			System.out.println("\tHas Active Ingredient\t=> " + this.hasActiveIngredient.getCode() + " : " + this.hasActiveIngredient.getName());
		}
		if( this.hasIngredient != null ) {
			System.out.println("\tHas Ingredient\t=> " + this.hasIngredient.getCode() + " : " + this.hasIngredient.getName());			
		}
		if( !vBoss.isEmpty() ) {
			System.out.println("\tBoss(es):");
			for( SnomedBoss boss : vBoss ) {
				boss.print();
			}
		}
		
	}


}
