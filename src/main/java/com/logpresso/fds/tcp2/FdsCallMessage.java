package com.logpresso.fds.tcp2;

import java.util.Date;
import java.util.Map;

import io.netty.channel.Channel;

public class FdsCallMessage {
	private Channel channel;
	private long requestTime;
	private long responseTime;
	private Map<String, Object> request;
	private Map<String, Object> response;
	private boolean error;
	private Date created = new Date();
	private boolean isTransformed = false;
	private byte[] sms_response_bytes;
	//응답 전문 정의
	final static int[] bodyAligns = {
			FixedLengthMerge.ALIGN_LEFT, // 응답 전문 코드[RE]  고정값 'C'[1]
			FixedLengthMerge.ALIGN_LEFT, // BYPASS여부 default 'N'
			FixedLengthMerge.ALIGN_LEFT, // 고정값 'RE'
			FixedLengthMerge.ALIGN_LEFT, // 01 고정[2] 
			FixedLengthMerge.ALIGN_LEFT, // 로그일자 [8]
			FixedLengthMerge.ALIGN_LEFT, // 로그시간 [9]
			FixedLengthMerge.ALIGN_LEFT, // HTS_ID [20] 
			FixedLengthMerge.ALIGN_LEFT, // 고객번호 [20]
			FixedLengthMerge.ALIGN_LEFT, // 로그 랜덤 KEY [10]
			FixedLengthMerge.ALIGN_LEFT, // 탐지결과 리턴 [1]
	};
	final static int[] bodyLengths = {
			1,
			1,
			2,
			2,
			8,
			9,
			20,
			20,
			10,
			1
	};
	public FdsCallMessage(Channel channel, Map<String, Object> request) {
		this.channel = channel;
		this.request = request;
		this.requestTime = System.currentTimeMillis();
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public long getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(long requestTime) {
		this.requestTime = requestTime;
	}

	public long getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}

	public long getElapsedTime() {
		if (responseTime == 0)
			return -1;
		return responseTime - requestTime;
	}

	public String getGuid() {
		return (String) request.get("guid");
	}

	public void setGuid(String guid) {
		request.put("guid", guid);
	}

	public Map<String, Object> getRequest() {
		return request;
	}

	public void setRequest(Map<String, Object> request) {
		this.request = request;
	}

	public Map<String, Object> getResponse() {
		return response;
	}

	public void setResponse(Map<String, Object> response) {
		this.response = response;
	}

	public void setError() {
		this.error = true;
	}

	public boolean isError() {
		return this.error;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public boolean isTransformed() {
		return isTransformed;
	}

	public void setTransformed(boolean isTransformed) {
		this.isTransformed = isTransformed;
	}
	
	public byte[] getResponseBytes(Map<String,Object> response) {
		String fdsProtocolVer = "02";
		if(response.get("fds_protocol_ver")!=null)
			fdsProtocolVer = (String)response.get("fds_protocol_ver");
		
		String trDt = (String)response.get("tr_dt");
		String trDlTmd = (String)response.get("tr_dl_tmd");
		String custId = (String)response.get("cust_id");
		String login = (String)response.get("login");
		String otr_guid = (String)response.get("otr_guid");
		String detectResult = (String)response.get("measure");

		//sms_response_bytes 세팅
		//bytes 데이터 생성
		String[] bodyStrings = new String[bodyLengths.length];
		bodyStrings[0] = "C";
		bodyStrings[1] = "N"; //고정 값 전달
		bodyStrings[2] = "RE";
		bodyStrings[3] = fdsProtocolVer;
		bodyStrings[4] = trDt;
		bodyStrings[5] = trDlTmd;
		bodyStrings[6] = custId;
		bodyStrings[7] = login;
		bodyStrings[8] = otr_guid;
		bodyStrings[9] = detectResult;
		
		FixedLengthMerge bodyMerge = new FixedLengthMerge();
		bodyMerge.setAligns(bodyAligns);
		bodyMerge.setLengths(bodyLengths);
		bodyMerge.setStrings(bodyStrings);
		
		return bodyMerge.merge();
	}
}
