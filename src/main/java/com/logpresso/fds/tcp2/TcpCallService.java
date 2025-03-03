package com.logpresso.fds.tcp2;

import java.util.List;

import org.araqne.log.api.LogTransformer;
import io.netty.channel.Channel;

public interface TcpCallService {
	boolean isEchoMode();

	void setEchoMode(boolean echo);

	void resetCounters();

	TcpCallStats getStats();

	void open(int port);

	void openSsl(int port, String keyAlias, String trustAlias);

	void close();

	Integer getTcpPort();

	List<Channel> getChannels();

	void killChannel(int channelId);

	void setCallTransformer(LogTransformer callTransformer);

	void setPostTransformer(LogTransformer postTransformer);
}
