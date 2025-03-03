package com.logpresso.fds.tcp2;

public class FdsAckMessage {
	private String guid;
	private boolean timeout;

	public FdsAckMessage(String guid) {
		this(guid, false);
	}

	public FdsAckMessage(String guid, boolean timeout) {
		this.guid = guid;
		this.timeout = timeout;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public boolean isTimeout() {
		return timeout;
	}

	public void setTimeout(boolean timeout) {
		this.timeout = timeout;
	}
}
