package gov.nih.nlm.mor.map;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import gov.nih.nlm.mor.RxNorm.RxNormBoss;

public class ConcentrationToPresentation implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3667634460690682108L;
	private Double presentationNumeratorValue = null;
	private String presentationNumeratorUnit = null;
	private Double presentationDenominatorValue = null;
	private String presentationDenominatorUnit = null;
	private String[] unitAbbreviations;
	private int unitIndex;
	
	public ConcentrationToPresentation(RxNormBoss boss, String qfString) {
		
		Double qf = null;
		if( !qfString.isEmpty() ) {
			String[] qfVals = qfString.split(" ");
			qf = Double.valueOf(qfVals[0]);			
		}
		else {
			qf = Double.valueOf(1);
		}
		
		Double product = qf * Double.valueOf(boss.getSnomedNumberatorValue());
		presentationNumeratorValue = round(product); 
		presentationNumeratorUnit = boss.getSnomedNumeratorUnit();
		
		//TODO: Are these valid assumptions if we are only
		// concerned with the
		
		presentationDenominatorValue = Double.valueOf(qf);
		presentationDenominatorUnit = "ML";
		
		
		setUnitAbbreviations();
		normalize();
	}
	
	public ConcentrationToPresentation() {
		
	}
	
	private void setUnitAbbreviations() {
		unitAbbreviations = new String[6];
		unitAbbreviations[0] = "PG";
		unitAbbreviations[1] = "NG";
		unitAbbreviations[2] = "MCG";
		unitAbbreviations[3] = "MG";
		unitAbbreviations[4] = "G";
		unitAbbreviations[5] = "KG";
	}
	
	private int getAbbreviationIndex() {
		int index = -1;
		for( int i=0; i < this.unitAbbreviations.length-1; i++ ) {
			if( unitAbbreviations[i].equalsIgnoreCase(presentationNumeratorUnit) ) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	private void normalize() {
//		if( this.baseRxcui.equals(Integer.valueOf("103457")) ) {
//			System.out.println("Debug");
//		}	
		unitIndex = getAbbreviationIndex();			
		if( unitIndex == -1 ) return;			
		boolean done = false;
		while( !done ) { 
			done = checkDone(presentationNumeratorValue);
		}

		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.HALF_UP);
		presentationNumeratorValue  = Double.valueOf(df.format(presentationNumeratorValue));

	}
	
	private void multiply() {
		presentationNumeratorValue = presentationNumeratorValue * 1000;
		unitIndex--;
	}
		
	private void divide() {
		presentationNumeratorValue = presentationNumeratorValue / 1000;
		unitIndex++;
	}
	
	private boolean checkDone(Double value) {
		Double n = value;
		boolean done = false;
		if( (n < 1000 && n >= 1) || (unitIndex == unitAbbreviations.length-1 && value > 1000) || (unitIndex == 0 && value < 1) ) {
			setUnit();
			done = true;
		}
		if( n >= 1000 ) {
			divide();
		}
		else if( n < 1 ) {
			multiply();
		}
		return done;
	}	
	
	private void setUnit() {
		presentationNumeratorUnit = unitAbbreviations[unitIndex];
	}
	
	public Double getPresentationNumeratorValue() {
		return this.presentationNumeratorValue;
	}
	
	public String getPresentationNumeratorUnit() {
		return this.presentationNumeratorUnit;
	}
	
	public Double getPresentationDenominatorValue() {
		return this.presentationDenominatorValue;
	}
	
	public String getPresentationDenominatorUnit() {
		return this.presentationDenominatorUnit;
	}
	
	private Double round(Double p) {
		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.HALF_UP);
		return Double.valueOf(df.format(p));
	}

}
