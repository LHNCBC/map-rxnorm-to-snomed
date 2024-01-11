package gov.nih.nlm.mor.RxNorm;

import java.util.Objects;

public class NDC {
	String ndc = "";
	String status = "";
	String startDate = "--";
	String endDate = "--";
	
	public NDC(String code, String status, String startDate, String endDate) {
		this.ndc = code;
		this.status = status;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public NDC() {
		
	}

	public String getNdc() {
		return ndc;
	}

	public void setNdc(String ndc) { 
		this.ndc = ndc;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() { 
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	@Override
	public int hashCode() {
		return Objects.hash(endDate, ndc, startDate, status);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NDC other = (NDC) obj;
		return Objects.equals(endDate, other.endDate) && Objects.equals(ndc, other.ndc)
				&& Objects.equals(startDate, other.startDate) && Objects.equals(status, other.status);
	}
	
}
