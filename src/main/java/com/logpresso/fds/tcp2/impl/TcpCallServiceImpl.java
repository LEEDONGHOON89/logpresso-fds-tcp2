package com.logpresso.fds.tcp2.impl;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

//--수정부분시작--//
import org.araqne.api.NamedThreadFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.SynchronousQueue;
//--수정부분끝--//

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.api.KeyStoreManager;
import org.araqne.log.api.LogTransformer;
import org.araqne.log.api.LogTransformerRegistry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logpresso.fds.api.FdsConfigService;
import com.logpresso.fds.api.FdsDetectService;
import com.logpresso.fds.api.model.FdsConfig;
import com.logpresso.fds.tcp2.TcpCallService;
import com.logpresso.fds.tcp2.TcpCallStats;
import com.logpresso.query.api.StreamQueryService;

@Component(name = "tcp-call-service")
@Provides
public class TcpCallServiceImpl implements TcpCallService {
	private final Logger slog = LoggerFactory.getLogger(TcpCallServiceImpl.class);

	private Channel listener;

	@Requires
	private FdsDetectService detector;

	@Requires
	private StreamQueryService streamQueryService;

	@Requires
	private FdsConfigService configService;

	@Requires
	private KeyStoreManager keyStoreManager;

	@Requires
	private LogTransformerRegistry transformerRegistry;

	//--수정부분시작--//
	private int maxWorkers;
	private ExecutorService nioPool;
//	private ExecutionHandler executionHandler;
	//--수정부분끝--//

	private TcpCallHandler handler;

	// netty v3 to v4 시작 //
	private ChannelFuture channelFuture;
	private NioEventLoopGroup bossGroup;
	private NioEventLoopGroup workerGroup;
		
	//netty v3 to v4 끝//
		
	@Validate
	public void start() {
		//--수정부분시작--//
		this.maxWorkers = Runtime.getRuntime().availableProcessors();
		if (this.maxWorkers > 32)
			this.maxWorkers = 32;
		
//		executionHandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(maxWorkers, 1048576, 1048676, 60, TimeUnit.SECONDS, new NamedThreadFactory("FDS Worker", true)));
		//--수정부분끝--//
		slog.info("start service tcp");
		FdsConfig config = configService.getFdsConfig();
		if (config != null && config.getTcpPort() != null) {
			bindPort(config.getTcpPort(), config.getTcpKeyAlias(), config.getTcpTrustAlias());

		}

	}

	@Invalidate
	public void stop() {
		slog.info("stop tcp service ");
		// release all connections
		closePort();
	}

	@Override
	public boolean isEchoMode() {
		return handler.isEchoMode();
	}

	@Override
	public void setEchoMode(boolean echo) {
		handler.setEchoMode(echo);
	}

	@Override
	public TcpCallStats getStats() {
		return handler.getStats();
	}

	@Override
	public void resetCounters() {
		handler.resetCounters();
	}

	@Override
	public void open(int port) {
		if (listener != null)
			throw new IllegalStateException("fds tcp already opened: " + listener.localAddress());

		saveTcpPort(port);
		bindPort(port);
	}

	@Override
	public void openSsl(int port, String keyAlias, String trustAlias) {
		if (listener != null)
			throw new IllegalStateException("fds tcp already opened: " + listener.localAddress());

		saveTcpPort(port, keyAlias, trustAlias);
		bindPort(port, keyAlias, trustAlias);
	}

	@Override
	public void close() {
		saveTcpPort(null, null, null);
		closePort();
	}
	
	private void closePort() {
		slog.info("tcpcall service stop ");
//		slog.debug("close channel metadata [{}]",channelFuture);
		
		if(channelFuture !=null){
			try{
				channelFuture.channel().close().sync();
			}catch (InterruptedException e) {
				slog.debug("close port exception e [{}]",e);
			}
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			
			if (detector != null)
				detector.removeMatchListener(handler);
		}
	}
	
	private void saveTcpPort(Integer port) {
		saveTcpPort(port, null, null);
	}

	private void saveTcpPort(Integer port, String keyAlias, String trustAlias) {
		FdsConfig config = configService.getFdsConfig();
		if (config == null)
			config = new FdsConfig();

		config.setTcpPort(port);
		config.setTcpKeyAlias(keyAlias);
		config.setTcpTrustAlias(trustAlias);
		configService.setFdsConfig(config);
	}

	private void bindPort(int port) {
		bindPort(port, null, null);
	}

	private void bindPort(int port, String keyAlias, String trustAlias) {
		this.handler = new TcpCallHandler(detector, streamQueryService, configService, transformerRegistry);
		detector.addMatchListener(handler);
		TcpCallChannelFactory channelInitializer = new TcpCallChannelFactory(keyAlias, trustAlias);
		//--수정부분시작--//
//		this.nioPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("FDS I/O"));
		//- this.serverChannelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
//		this.serverChannelFactory = new NioServerSocketChannelFactory(nioPool, 1, nioPool, this.maxWorkers);
		
		//--수정부분끝--//
		
		ServerBootstrap bootstrap = new ServerBootstrap();
		bossGroup = new NioEventLoopGroup(1);
		workerGroup = new NioEventLoopGroup(1,new ThreadPoolExecutor(0, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("FDS I/O")));
		try {
			slog.info("bind port, [{}]",port);
			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(channelInitializer)
			.childOption(ChannelOption.SO_REUSEADDR, true)
			.childOption(ChannelOption.TCP_NODELAY, true)
			.childOption(ChannelOption.SO_KEEPALIVE, true)
			.localAddress(new InetSocketAddress(port));
		//		InetSocketAddress address = new InetSocketAddress(port);
			channelFuture = bootstrap.bind().sync();
		} catch (InterruptedException e) {
			slog.info("tcp call: bootstrap bind sync error",keyAlias ,trustAlias);
		}catch (Exception e) {
			slog.info("tcp call: bootstrap bind sync error",keyAlias ,trustAlias);
		}
		if (keyAlias != null && trustAlias != null)
			slog.info("tcp call: ssl listener opened, keystore [{}] truststore [{}]",keyAlias ,trustAlias);
																		
		else
			slog.info("tcp call: listener [{}] opened");
	}

	@Override
	public Integer getTcpPort() {
		FdsConfig config = configService.getFdsConfig();
		if (config == null)
			return null;

		return config.getTcpPort();
	}

	@Override
	public List<Channel> getChannels() {
		return handler.getChannels();
	}

	@Override
	public void killChannel(Channel channel) {
		handler.killChannel(channel);
	}

	private class TcpCallChannelFactory extends ChannelInitializer {

		private String keyAlias;
		private String trustAlias;

		public TcpCallChannelFactory(String keyAlias, String trustAlias) {
			this.keyAlias = keyAlias;
			this.trustAlias = trustAlias;
		}
		@Override
		public void initChannel(Channel ch) throws Exception {
			ChannelPipeline p = ch.pipeline();
			if (keyAlias != null && trustAlias != null)
				addSslHandler(p);

			// decoder, encoder and handler
			p.addLast("decoder", new TcpCallDecoder());
			p.addLast("encoder", new TcpCallEncoder());
			//--수정부분시작--//
//			p.addLast("executor", executionHandler);
			//--수정부분끝--//
			p.addLast("handler", handler);
		}

		private void addSslHandler(ChannelPipeline p) throws Exception {
			String alg = KeyManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = keyStoreManager.getTrustManagerFactory(trustAlias, alg);
			KeyManagerFactory kmf = keyStoreManager.getKeyManagerFactory(keyAlias, alg);

			TrustManager[] trustManagers = null;
			KeyManager[] keyManagers = null;
			if (tmf != null)
				trustManagers = tmf.getTrustManagers();
			if (kmf != null)
				keyManagers = kmf.getKeyManagers();

			SSLContext serverContext = SSLContext.getInstance("TLS");
			serverContext.init(keyManagers, trustManagers, new SecureRandom());

			SSLEngine engine = serverContext.createSSLEngine();
			engine.setUseClientMode(false);
			engine.setNeedClientAuth(false);

			p.addLast("ssl", new SslHandler(engine));
		}
	}

	@Override
	public void setCallTransformer(LogTransformer callTransformer) {
		if (handler != null)
			handler.setCallTransformer(callTransformer);
	}

	@Override
	public void setPostTransformer(LogTransformer postTransformer) {
		if (handler != null)
			handler.setPostTransformer(postTransformer);
	}
}
