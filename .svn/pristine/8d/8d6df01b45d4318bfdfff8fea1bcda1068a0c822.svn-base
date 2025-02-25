package com.logsay.locale.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Locale {

	private String guid;
	private String country;
	private long startIp;
	private long endIp;
	private Date regDate;
	
	public String getGuid()	{
		return guid;
	}
	public void setGuid(String guid) {
		this.guid = guid;
	}
	
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	
	public long getStartIp() {
		return startIp;
	}
	public void setStartIp(long startIp) {
		this.startIp = startIp;
	}
	
	public long getEndIp() {
		return endIp;
	}
	public void setEndIp(long endIp) {
		this.endIp = endIp;
	}
	
	public Date getRegDate() {
		return regDate;
	}
	public void setRegDate(Date regDate) {
		this.regDate = regDate;
	}
	
	public Map<String, Object> toMap(Locale locale) {
		Map<String, Object> result = new HashMap<String, Object>();
		
		result.put("guid", locale.getGuid());
		result.put("country", locale.getCountry());
		result.put("start_ip", locale.getStartIp());
		result.put("end_id", locale.getEndIp());
		result.put("reg_date", locale.getRegDate());
		
		return result;
	}
}
