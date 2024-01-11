package gov.nih.nlm.mor.util;

public class GciDoseForm {
	
	private String RxN = null;
	private String RxNLabel = null;
	private String SNO = null;

	public GciDoseForm(String gciString) {
			String[] lov=gciString.split(";");
			RxN=lov[0].trim();
			RxNLabel=lov[1];
			SNO=lov[2].trim();
	}

	public String getRxN() {
		return RxN;
	}

	public void setRxN(String rxN) {
		RxN = rxN;
	}

	public String getRxNLabel() {
		return RxNLabel;
	}

	public void setRxNLabel(String rxNLabel) {
		RxNLabel = rxNLabel;
	}

	public String getSNO() {
		return SNO;
	}

	public void setSNO(String sNO) {
		SNO = sNO;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((RxN == null) ? 0 : RxN.hashCode());
		result = prime * result + ((RxNLabel == null) ? 0 : RxNLabel.hashCode());
		result = prime * result + ((SNO == null) ? 0 : SNO.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GciDoseForm other = (GciDoseForm) obj;
		if (RxN == null) {
			if (other.RxN != null)
				return false;
		} else if (!RxN.equals(other.RxN))
			return false;
		if (RxNLabel == null) {
			if (other.RxNLabel != null)
				return false;
		} else if (!RxNLabel.equals(other.RxNLabel))
			return false;
		if (SNO == null) {
			if (other.SNO != null)
				return false;
		} else if (!SNO.equals(other.SNO))
			return false;
		return true;
	}
	
	
}