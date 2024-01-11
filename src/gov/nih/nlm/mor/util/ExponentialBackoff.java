package gov.nih.nlm.mor.util;
/*
 * Credit: https://ericnormand.me/article/exponential-backoff
 */
public class ExponentialBackoff {
	public Long time = Long.valueOf(1000);
	public int rate = 2;
	public Long max = Long.valueOf(64000);
	
	public ExponentialBackoff() {
		
	}

	public void multiply() {
		this.time = time * rate;
	}	
	
	public Long getTime() {
		return time;
	}
	
	public void setTime(Long time) {
		this.time = time;
	}
	
	public int getRate() {
		return rate;
	}
	
	public void setRate(int rate) {
		this.rate = rate;
	}
	
	public Long getMax() {
		return max;
	}
	
	public void setMax(Long max) {
		this.max = max;
	}

}
