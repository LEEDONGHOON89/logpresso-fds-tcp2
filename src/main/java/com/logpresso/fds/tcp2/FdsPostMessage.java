package com.logpresso.fds.tcp2;

import java.util.Date;
import java.util.Map;

import io.netty.channel.Channel;

public class FdsPostMessage {

	private Channel channel;
	// for acked post
	private String guid;
	private String topic;
	private String login;
	private Map<String, Object> params;
	private Date created = new Date();
	private boolean isTransformed = false;

	public FdsPostMessage() {
	}
	
	public FdsPostMessage(String topic, String login, Map<String, Object> params) {
		this(topic, login, params, null, null);
	}

	public FdsPostMessage(String topic, String login, Map<String, Object> params, Channel channel, String guid) {
		this.topic = topic;
		this.login = login;
		this.params = params;
		this.channel = channel;
		this.guid = guid;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
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
