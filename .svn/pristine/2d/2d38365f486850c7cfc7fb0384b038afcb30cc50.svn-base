package com.logpresso.fds.tcp;

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
}
