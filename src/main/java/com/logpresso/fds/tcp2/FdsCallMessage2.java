package com.logpresso.fds.tcp2;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FdsCallMessage2 {
	private Map<String,Object> response;
	private byte[] sms_response_bytes;
	
	
	// 고정 바이트  : 74
	private String fdsDataCd="C";		// 응답 전문 코드[RE]  고정값 'C'[1]
	private String fsdBypassYn="N"; 	// BYPASS여부 default 'N'
	private String fdsProtocoleId="RE"; // 고정값 'RE'
	private String fdsProtocolVer="01"; // 01 고정[2]
	private String trDt; 				// 로그일자 [8]
	private String trDlTmd; 			// 로그시간 [9]
	private String custId;	 			// HTS_ID [20] 
	private String login;	 			// 고객번호 [20]
	private String otr_guid;  			// 로그 랜덤 KEY [10]
	private String detectResult; 		// 탐지결과 리턴 [1]
	
	//sms 메세지정의
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

	
	public FdsCallMessage2(Map<String, Object> response) {
		this.response = response;
		parse(response);
	}
	public Map<String, Object> getSms_response() {
		return response;
	}

	public void setSms_response(Map<String, Object> response) {
		this.response = response;
	}
	
	
	private void parse(Map<String,Object> response) {
		
//		fdsDataCd = (String)response.get("fdsDataCd");
//		fsdBypassYn = (String)sms_response.get("fsdBypassYn");
//		fdsProtocoleId = (String)sms_response.get("fdsProtocoleId");
		if(response.get("fdsProtocolVer")!=null)
			fdsProtocolVer = (String)response.get("fdsProtocolVer");
		
		trDt = (String)response.get("trDt");
		trDlTmd = (String)response.get("trDlTmd");
		custId = (String)response.get("custId");
		login = (String)response.get("login");
		otr_guid = (String)response.get("otr_guid");
		detectResult = (String)response.get("detectResult");

		//sms_response_bytes 세팅
		//bytes 데이터 생성
		sms_response_bytes = getResponseBytes();
	}
	
	public byte[] getSms_response_bytes() {
		return sms_response_bytes;
	}
	public void setSms_response_bytes(byte[] sms_response_bytes) {
		this.sms_response_bytes = sms_response_bytes;
	}
	private byte[] getResponseBytes() {
		String[] bodyStrings = new String[bodyLengths.length];
		bodyStrings[0] = this.fdsDataCd;
		bodyStrings[1] = this.fsdBypassYn;
		bodyStrings[2] = this.fdsProtocoleId;
		bodyStrings[3] = this.fdsProtocolVer;
		bodyStrings[4] = this.trDt;
		bodyStrings[5] = this.trDlTmd;
		bodyStrings[6] = this.custId;
		bodyStrings[7] = this.login;
		bodyStrings[8] = this.otr_guid;
		bodyStrings[9] = this.detectResult;
		
		FixedLengthMerge bodyMerge = new FixedLengthMerge();
		bodyMerge.setAligns(bodyAligns);
		bodyMerge.setLengths(bodyLengths);
		bodyMerge.setStrings(bodyStrings);
		
		return bodyMerge.merge();
	}
	
	
}
