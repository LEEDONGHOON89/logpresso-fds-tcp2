package com.logpresso.fds.tcp2;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class TcpPeer {
	private int slot;
	private InetSocketAddress address;
	private boolean ssl;
	private String trustStorePath;
	private String trustStorePassword;

	public TcpPeer(int slot, InetSocketAddress address) {
		this.slot = slot;
		this.address = address;
	}

	public int getSlot() {
		return slot;
	}

	public void setSlot(int slot) {
		this.slot = slot;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	public void setTrustStore(String trustStorePath, String trustStorePassword) {
		if (trustStorePath == null || trustStorePassword == null) {
			this.ssl = false;
			this.trustStorePath = null;
			this.trustStorePassword = null;
		} else {
			this.ssl = true;
			this.trustStorePath = trustStorePath;
			this.trustStorePassword = trustStorePassword;
		}
	}

	public boolean isSsl() {
		return ssl;
	}

	public String getTrustStorePath() {
		return trustStorePath;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	@Override
	public String toString() {
		InetAddress inetAddress = address.getAddress();
		if(inetAddress==null)
			return null;
		return "slot " + slot + ", remote=" + inetAddress.getHostAddress() + ":" + address.getPort() + ", is_ssl=" + ssl;

	}
}
