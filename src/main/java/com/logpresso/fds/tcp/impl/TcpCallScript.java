package com.logpresso.fds.tcp.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;

import org.araqne.api.PathAutoCompleter;
import org.araqne.api.Script;
import org.araqne.api.ScriptArgument;
import org.araqne.api.ScriptContext;
import org.araqne.api.ScriptUsage;
import io.netty.channel.Channel;

import com.logpresso.fds.client.FdsClientPool;
import com.logpresso.fds.client.FdsClientPoolStatus;
import com.logpresso.fds.client.FdsSession;
import com.logpresso.fds.tcp.TcpCallService;
import com.logpresso.fds.tcp.TcpCallStats;
import com.logpresso.fds.tcp.TcpPeer;

public class TcpCallScript implements Script {
	private TcpCallService tcpCall;
	private ScriptContext context;

	public TcpCallScript(TcpCallService tcpCall) {
		this.tcpCall = tcpCall;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void tcpSessions(String[] args) {
		context.println("FDS Open Sessions");
		context.println("-------------------");

		for (Channel channel : tcpCall.getChannels()) {
			context.println("[" + channel.id() + "] " + channel.toString());
		}
	}

	@ScriptUsage(description = "kill tcp session", arguments = { @ScriptArgument(name = "channel id") })
	public void tcpKillSession(String[] args) {
		tcpCall.killChannel(Integer.parseInt(args[0]));
		context.println("session killed");
	}

	@ScriptUsage(description = "set or get max pool size", arguments = {})
	public void tcpMaxPoolSize(String[] args) {
		context.println("current max pool size=" + FdsClientPool.getInstance().getMaxPoolSize());
	}

	public void tcpPools(String[] args) {
		context.println("FDS Client Pool");
		context.println("-----------------");

		boolean verbose = false;
		if (args.length > 0)
			verbose = args[0].equals("-v");

		int i = 0;
		for (InetSocketAddress addr : FdsClientPool.getInstance().getServerAddresses()) {
			if (i++ != 0)
				context.println("");
			InetAddress inetAddress = addr.getAddress();
			if(inetAddress != null)
				context.println("[" + addr.getAddress().getHostAddress() + ":" + addr.getPort() + "]");

			FdsClientPoolStatus status = FdsClientPool.getInstance().getPoolStatus(addr);
			if (status == null)
				continue;

			context.println("Free Clients: " + status.getFreeClients().size());

			if (verbose) {
				for (FdsSession session : status.getFreeClients())
					context.println(" * " + session);
			}

			context.println("Using Clients: " + status.getUsingClients().size());
			if (verbose) {
				for (FdsSession session : status.getUsingClients())
					context.println(" * " + session);
			}
		}
	}

	public void tcpPeer(String[] args) {
		TcpPeer peer = tcpCall.getPeer();
		if (peer == null)
			context.println("not set");
		else
			context.println(peer);
	}

	@ScriptUsage(description = "", arguments = {
			@ScriptArgument(name = "slot", type = "int", description = "0 or 1"),
			@ScriptArgument(name = "peer host", type = "string", description = "peer host address"),
			@ScriptArgument(name = "peer port", type = "int", description = "peer port"),
			@ScriptArgument(name = "file path", description = "the file path of the key store", autocompletion = PathAutoCompleter.class, optional = true) })
	public void setTcpPeer(String[] args) {
		int slot = Integer.parseInt(args[0]);
		InetSocketAddress address = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
		TcpPeer peer = new TcpPeer(slot, address);

		if (args.length > 3) {
			try {
				context.print("Key-Store Password? ");
				String password = context.readPassword();
				peer.setTrustStore(args[3], password);
			} catch (InterruptedException e) {
				context.println("interrupted");
				return;
			}
		} else
			peer.setTrustStore(null, null);

		tcpCall.setPeer(peer);
		context.println("set");
	}

	public void unsetTcpPeer(String[] args) {
		tcpCall.setPeer(null);
		context.println("unset");
	}

	public void resetTcpCounters(String[] args) {
		tcpCall.resetCounters();
		context.println("reset completed");
	}

	public void tcpCallStats(String[] args) {
		TcpCallStats stats = tcpCall.getStats();
		context.println(stats.toString());
	}

	public void tcpTrends(String[] args) {
		context.println("Press Ctrl-C to stop trace");
		try {
			StatsPrinter printer = new StatsPrinter();
			printer.start();
			while (true) {
				try {
					context.readLine();
				} catch (InterruptedException e) {
					break;
				}
			}

			printer.doStop = true;
			printer.interrupt();
			printer.join(2000);
		} catch (InterruptedException e) {
		} finally {
			context.println("");
			context.println("interrupted");
		}
	}

	private class StatsPrinter extends Thread {
		private volatile boolean doStop = false;

		public StatsPrinter() {
			super("Tcp Call Stats Printer");
		}

		@Override
		public void run() {
			TcpCallStats oldStats = tcpCall.getStats();
			long oldCallCount = oldStats.getCallCount();
			long oldPostCount = oldStats.getPostCount();
			long oldRedirectCallCount = oldStats.getRedirectCallCount();
			long oldRedirectPostCount = oldStats.getRedirectPostCount();
			long oldRejectedCallCount = oldStats.getRejectedCallCount();
			long oldRejectedPostCount = oldStats.getRejectedPostCount();

			try {
				while (!doStop) {
					Thread.sleep(1000);
					TcpCallStats newStats = tcpCall.getStats();

					long callCount = newStats.getCallCount();
					long postCount = newStats.getPostCount();
					long redirectCallCount = newStats.getRedirectCallCount();
					long redirectPostCount = newStats.getRedirectPostCount();
					long rejectedCallCount = newStats.getRejectedCallCount();
					long rejectedPostCount = newStats.getRejectedPostCount();

					long callTime = newStats.getLastCallTime();
					long postTime = newStats.getLastPostTime();

					long callDelta = callCount - oldCallCount;
					long postDelta = postCount - oldPostCount;
					long redirectCallDelta = redirectCallCount - oldRedirectCallCount;
					long redirectPostDelta = redirectPostCount - oldRedirectPostCount;
					long rejectedCallDelta = rejectedCallCount - oldRejectedCallCount;
					long rejectedPostDelta = rejectedPostCount - oldRejectedPostCount;

					long totalDelta = callDelta + postDelta;

					String line = String
							.format("%d TPS, Call [%d, %s] Post [%d, %s] RCall [%d, Reject %d] RPost [%d, Reject %d], Pending [%d] Bypass [%d] ARS [%d] BLK [%d] acc-ERR [%d]",
									totalDelta, callDelta, df(callTime), postDelta, df(postTime), redirectCallDelta,
									rejectedCallDelta, redirectPostDelta, rejectedPostDelta, newStats.getPendingCallCount(),
									newStats.getOkCount(), newStats.getArsCount(), newStats.getBlockCount(),
									newStats.getErrorCount());

					context.println(line);

					oldCallCount = callCount;
					oldPostCount = postCount;
					oldRedirectCallCount = redirectCallCount;
					oldRedirectPostCount = redirectPostCount;
					oldRejectedCallCount = rejectedCallCount;
					oldRejectedPostCount = rejectedPostCount;
				}
			} catch (InterruptedException e) {
			} finally {
			}
		}

		private String df(long time) {
			if (time == 0)
				return "N/A";
			SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
			return df.format(time);
		}
	}

	@ScriptUsage(description = "open tcp call port", arguments = {
			@ScriptArgument(name = "port", type = "int", description = "tcp call port number"),
			@ScriptArgument(name = "key alias", type = "string", description = "key alias of keystore.list", optional = true),
			@ScriptArgument(name = "trust alias", type = "string", description = "trust alias of keystore.list", optional = true) })
	public void openTcp(String[] args) {
		int port = Integer.valueOf(args[0]);
		if (args.length == 1) {
			tcpCall.open(port);
		} else if (args.length >= 3) {
			String keyAlias = args[1];
			String trustAlias = args[2];
			tcpCall.openSsl(port, keyAlias, trustAlias);
		} else {
			context.println("invalid arguments");
			return;
		}
		context.println("opened");
	}

	public void tcpPort(String[] args) {
		Integer port = tcpCall.getTcpPort();
		if (port == null)
			context.println("tcp port not open");
		else
			context.println("tcp port [" + port + "] opened");
	}

	public void closeTcp(String[] args) {
		tcpCall.close();
		context.println("closed");
	}

	public void echoMode(String[] args) {
		if (args.length >= 1)
			tcpCall.setEchoMode(args[0].equals("on"));
		else
			context.println("echo mode = " + (tcpCall.isEchoMode() ? "on" : "off"));
	}
}
