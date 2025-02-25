package com.logpresso.fds.tcp.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.araqne.api.Script;
import org.araqne.api.ScriptFactory;

import com.logpresso.fds.tcp.TcpCallService;

@Component(name = "tcp-call-script-factory")
@Provides
public class TcpCallScriptFactory implements ScriptFactory {

	@Requires
	private TcpCallService tcpCall;

	@ServiceProperty(name = "alias", value = "fds")
	private String alias;

	@Override
	public Script createScript() {
		return new TcpCallScript(tcpCall);
	}

}
