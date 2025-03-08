package com.logpresso.fds.tcp2.impl;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.security.KeyStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.IIOException;

import org.araqne.log.api.LogTransformer;
import org.araqne.log.api.LogTransformerRegistry;
import org.araqne.log.api.SimpleLog;
import org.araqne.logdb.Row;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
//import io.netty.buffer.ChannelBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logpresso.fds.api.FdsConfigService;
import com.logpresso.fds.api.FdsDetectService;
import com.logpresso.fds.api.MatchListener;
import com.logpresso.fds.api.model.FdsConfig;
import com.logpresso.fds.client.FdsClientPool;
import com.logpresso.fds.client.FdsLogLevel;
import com.logpresso.fds.client.FdsLogListener;
import com.logpresso.fds.tcp2.FdsAckMessage;
import com.logpresso.fds.tcp2.FdsCallMessage;
import com.logpresso.fds.tcp2.FdsPostMessage;
import com.logpresso.fds.tcp2.TcpCallStats;
import com.logpresso.query.api.StreamQueryService;

@Sharable
public class TcpCallHandler extends SimpleChannelInboundHandler<Object> implements MatchListener {
	private final Logger slog = LoggerFactory.getLogger(TcpCallHandler.class);
	private final Logger inlog = LoggerFactory.getLogger("fds-tcp-input");
	private final Logger outlog = LoggerFactory.getLogger("fds-tcp-output");
	private final Logger retryLog = LoggerFactory.getLogger("fds-tcp-retry");
	private final static Set<String> ignoreMsgs;
	private FdsDetectService detector;

	private ConcurrentHashMap<String, FdsCallMessage> pendingCalls = new ConcurrentHashMap<String, FdsCallMessage>();

	// to release call at channel disconnect
	private ConcurrentHashMap<Integer, String> channelGuids = new ConcurrentHashMap<Integer, String>();

	private ConcurrentHashMap<ChannelId, Channel> channels = new ConcurrentHashMap<ChannelId, Channel>();
	private StreamQueryService streamQueryService;
	private volatile boolean echo;

	private AtomicLong redirectCallCounter = new AtomicLong();
	private AtomicLong redirectPostCounter = new AtomicLong();
	private AtomicLong rejectedCallCounter = new AtomicLong();
	private AtomicLong rejectedPostCounter = new AtomicLong();
	private AtomicLong callCounter = new AtomicLong();
	private AtomicLong postCounter = new AtomicLong();
	private AtomicLong okCount = new AtomicLong();
	private AtomicLong blockCount = new AtomicLong();
	private AtomicLong arsCount = new AtomicLong();
	private AtomicLong errorCount = new AtomicLong();
	private volatile long lastCallTime = 0;
	private volatile long lastPostTime = 0;

	private ThreadPoolExecutor peerExecutor;

	private FdsConfigService fdsConfigService;
	private LogTransformerRegistry transformerRegistry;
	private String callTransformerName;
	private String postTransformerName;
	private LogTransformer callTransformer;
	private LogTransformer postTransformer;
	private long callTransformerRefreshTime;
	private long postTransformerRefreshTime;

	static {
		ignoreMsgs = new HashSet<String>();
		ignoreMsgs.add("강제로 끊겼습니다");
		ignoreMsgs.add("호스트 시스템의 소프트웨어의 의해 중단되었습니다");
		ignoreMsgs.add("Connection reset by peer");
		ignoreMsgs.add("연결이 상대편에 의해 끊어짐");
		ignoreMsgs.add("not an SSL/TLS record");
		ignoreMsgs.add("현재 연결은 원격 호스트에 의해 강제로 끊겼습니다");
		ignoreMsgs.add("파이프가 깨어짐");
	}

	public TcpCallHandler(FdsDetectService detector, StreamQueryService streamQueryService, FdsConfigService fdsConfigService,
			LogTransformerRegistry transformerRegistry) {
		this.detector = detector;
		this.streamQueryService = streamQueryService;
		this.fdsConfigService = fdsConfigService;
		this.transformerRegistry = transformerRegistry;

		int cpuCount = Runtime.getRuntime().availableProcessors();
		peerExecutor = new ThreadPoolExecutor(1, Math.max(2, cpuCount), 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
				new NamedThreadFactory());
	}

	private final class NamedThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "FDS Peer Call");
		}
	}

	public List<Channel> getChannels() {
		return new ArrayList<Channel>(channels.values());
	}

	public void killChannel(int channelId) {
		Channel channel = channels.remove(channelId);
		if (channel == null)
			throw new IllegalStateException("already closed channel: " + channelId);

		channel.close();

		String remote = getRemoteAddress((InetSocketAddress) channel.remoteAddress());
		slog.info("fds tcp: killed channel [id {}, remote {}]", channelId, remote);
	}

	private String getRemoteAddress(InetSocketAddress addr) {
		return addr.getAddress().getHostAddress() + ":" + addr.getPort();
	}

	public void setCallTransformer(LogTransformer callTransformer) {
		this.callTransformer = callTransformer;
	}

	public void setPostTransformer(LogTransformer postTransformer) {
		this.postTransformer = postTransformer;
	}


	public void resetCounters() {
		redirectCallCounter.set(0);
		redirectPostCounter.set(0);
		rejectedCallCounter.set(0);
		rejectedPostCounter.set(0);
		callCounter.set(0);
		postCounter.set(0);
		okCount.set(0);
		blockCount.set(0);
		arsCount.set(0);
		errorCount.set(0);
		lastCallTime = 0;
		lastPostTime = 0;
	}

	public TcpCallStats getStats() {
		TcpCallStats stats = new TcpCallStats();
		stats.setRedirectCallCount(redirectCallCounter.get());
		stats.setRedirectPostCount(redirectPostCounter.get());
		stats.setRejectedCallCount(rejectedCallCounter.get());
		stats.setRejectedPostCount(rejectedPostCounter.get());
		stats.setCallCount(callCounter.get());
		stats.setPostCount(postCounter.get());
		stats.setLastCallTime(lastCallTime);
		stats.setLastPostTime(lastPostTime);
		stats.setPendingCallCount(pendingCalls.size());
		stats.setOkCount(okCount.get());
		stats.setBlockCount(blockCount.get());
		stats.setArsCount(arsCount.get());
		stats.setErrorCount(errorCount.get());
		return stats;
	}

	public boolean isEchoMode() {
		return echo;
	}

	public void setEchoMode(boolean enabled) {
		this.echo = enabled;
	}

	@Override
	public void onMatch(Map<String, Object> m) {
		Object o = m.get("guid");
		if (o == null) {
			if (slog.isDebugEnabled())
				slog.debug("fds tcp: guid not found for match result [{}]", m);

			return;
		}

		String guid = o.toString();
		FdsCallMessage call = pendingCalls.remove(guid);
		if (call == null) {
			if (slog.isDebugEnabled())
				slog.debug("fds tcp: pending call not found for request [{}]", guid);

			return;
		}

		if (!call.getChannel().isOpen()) {
			if (slog.isDebugEnabled())
				slog.debug("fds tcp: channel is already closed for request [{}]", guid);
			return;
		}

		call.setResponseTime(System.currentTimeMillis());

		if (slog.isDebugEnabled()) {
			long elapsed = call.getElapsedTime();
			if (elapsed >= 500)
				slog.debug("fds tcp: slow response time [{}]ms", elapsed);
		}

		call.setResponse(m);
		try {
			call.getChannel().writeAndFlush(call);
		}catch(Exception e) {
			if (slog.isDebugEnabled()) {
			slog.error("tcp response elapsed = [{}] exception=[{}]",call.getResponseTime(),e);
			}
		}
		Object measureO = m.get("measure");
		String measure = null;
		if (measureO != null)
			measure = m.get("measure").toString();

		if (measure == null) {
			okCount.incrementAndGet();
		} else if (measure.equals("ARS")) {
			arsCount.incrementAndGet();
		} else if (measure.equals("BLOCK")) {
			blockCount.incrementAndGet();
		}

		if (outlog.isDebugEnabled())
			outlog.debug("fds tcp: measure [{}] for request [{}]", measure, call.getRequest());
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		try{
			super.channelActive(ctx);
			Channel channel = ctx.channel();
			channels.put(channel.id(), channel);
		}catch (Exception e){
			slog.debug("tcp call: channelActive channel id[{}]",ctx.channel().id());
		}							 
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		try{
			super.channelInactive(ctx);
			
			Channel channel = ctx.channel();
			channels.remove(channel.id());
			String guid = channelGuids.remove(channel.id());
			if (guid != null) {
				FdsCallMessage call = pendingCalls.remove(guid);
				if (call != null)
					slog.debug("fds tcp: discard request [{}] of closed channel [{}]", call.getRequest(), ctx.channel()
							.remoteAddress());
			}
		}catch(Exception e){
			slog.debug("tcp call: channelInactive channel id[{}]",ctx.channel().id());
		}
	}
	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		Object m = msg;
		slog.debug("tcp call: channelRead0, [{}]", m);
		try {
			if (m instanceof FdsCallMessage) {
				FdsCallMessage call = (FdsCallMessage) m;
				handleMatchCall(ctx, call);
			} else if (m instanceof FdsPostMessage) {
				FdsPostMessage post = (FdsPostMessage) m;
				handlePost(ctx, post);
			} else {
				slog.debug("tcp call: unknown msg discarded, [{}]", m);
			}
		} catch (Exception ex) {
			long ec = errorCount.incrementAndGet();
			slog.error(String.format("fds tcp: unexpected exception (%d)", ec), ex);
			throw ex;
		} catch (Throwable t) {
			long ec = errorCount.incrementAndGet();
			slog.error(String.format("fds tcp: unexpected exception (%d)", ec), t);
		}
	}
	
	@Override
 	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);	
 	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
//		ctx.channel().close().sync();
		ctx.close();
		String msg = e.getCause().getMessage();
		slog.error("exceptionCaught : [{}]",msg);
		SocketAddress remote = ctx.channel().remoteAddress();
		if (msg != null && ignoreMsgs.contains(msg)) {
			slog.debug("fds tcp: session " + remote + " closed", e.getCause());
		} else {
			if (e.getCause() instanceof ClosedChannelException)
				slog.error("fds tcp: session " + remote + " error: "
						+ e.getCause().getClass().getCanonicalName());
			else
				slog.error("fds tcp: session " + remote + " error", e.getCause());
		}
	}
	
	private void handlePost(ChannelHandlerContext ctx, FdsPostMessage post) {
		if (System.currentTimeMillis() - postTransformerRefreshTime > 1000)
			refreshPostTransformer();
		
		/* peer 세팅 제거 2025.02.26
		** 사유 : no clinet 버전의 경우 login을 가져올수 없어서 peer처리 불가로 인해 제거
		**/
		if (inlog.isDebugEnabled())
			inlog.debug("tcp call: received post, [{}]", post);
		
		handleLocalPost(post);
	}

	private void handleLocalPost(FdsPostMessage post) {
		if (post.getGuid() != null){
			post.getChannel().writeAndFlush(new FdsAckMessage(post.getGuid()));
		}
		postCounter.incrementAndGet();
		lastPostTime = System.currentTimeMillis();
		
		slog.debug("handleLocalPost post = {}",post);
		Row row = new Row(post.getParams());
		row.put("_time", post.getCreated());
		streamQueryService.pushStream("FDS수집로그입력", row);
	}

	private void handleMatchCall(ChannelHandlerContext ctx, FdsCallMessage call) {
		if (System.currentTimeMillis() - callTransformerRefreshTime > 60000)
			refreshCallTransformer();
		if (inlog.isDebugEnabled())
			inlog.debug("tcp call: received match call, [{}]", call.getRequest());

		handleLocalMatchCall(ctx, call);
	}

	private void handleLocalMatchCall(ChannelHandlerContext ctx, FdsCallMessage call) {
		callCounter.incrementAndGet();
		lastCallTime = System.currentTimeMillis();

		String guid = call.getGuid();
		Channel channel = ctx.channel();
		Integer channelId =channel.id().compareTo(channel.id());
		
		pendingCalls.put(guid, call);
		channelGuids.put(channelId, guid);

		Map<String, Object> row = call.getRequest();
		row.put("_time", call.getCreated());
		detector.write(row);
	}

	private void refreshCallTransformer() {
		FdsConfig fdsConfig = fdsConfigService.getFdsConfig();
		if (fdsConfig == null) {
			this.callTransformer = null;
			this.callTransformerName = null;
			return;
		}

		callTransformerName = fdsConfig.getCallTransformerName();

		if (callTransformerName != null) {
			try {
				callTransformer = transformerRegistry.newTransformer(callTransformerName);
			} catch (IllegalStateException e) {
				slog.error("logpresso fds tcp: cannot set call transformer", e);
			}
		}
		callTransformerRefreshTime = System.currentTimeMillis();
	}

	private void refreshPostTransformer() {
		FdsConfig fdsConfig = fdsConfigService.getFdsConfig();
		if (fdsConfig == null) {
			this.postTransformer = null;
			this.postTransformerName = null;
			return;
		}

		postTransformerName = fdsConfig.getPostTransformerName();

		if (postTransformerName != null) {
			try {
				postTransformer = transformerRegistry.newTransformer(postTransformerName);
			} catch (IllegalStateException e) {
				slog.error("logpresso fds tcp: cannot set post transformer", e);
				postTransformer = null;
			}
		}
		postTransformerRefreshTime = System.currentTimeMillis();
	}
}
