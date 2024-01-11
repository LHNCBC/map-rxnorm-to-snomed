package gov.nih.nlm.mor.auditmap.utilitaries;

/**
 * 
 * @author erias
 * classe décrivant un couple composé de deux string
 *
 */
public class Couple {

	  public final String x;
	  public final String y;

	  
	  public Couple(String x, String y) {
	   					this.x = x;
	   						this.y = y;
	   					}

	   @Override
	   public boolean equals(Object o_) {
	      Couple o = (Couple) o_;

	      return (o.x.equals(x) && o.y.equals(y));
	   }

	   	@Override
	   public int hashCode()
	   {
	      return 31 * x.hashCode() + y.hashCode()+178;
	   }
	

}
