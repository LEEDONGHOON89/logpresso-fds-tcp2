package com.logpresso.fds.tcp.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.json.JSONConverter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logpresso.fds.tcp.FdsCallMessage;
import com.logpresso.fds.tcp.FdsPostMessage;

public class TcpCallDecoder extends ByteToMessageDecoder {
	private static final int DEFAULT_MAX_FRAMESIZE = 2097152;
	private final Logger logger = LoggerFactory.getLogger(TcpCallDecoder.class.getName());

	private static AtomicInteger c = new AtomicInteger();
	private static AtomicInteger ec = new AtomicInteger();

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
		logger.debug("tcp call exceptionCaught: unexpected exception caught", e.getCause());
		//super.exceptionCaught(ctx, e);
		ctx.channel().close().sync();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			decodeI(ctx, ctx.channel(), in,out);
		} catch (Exception e) {
			logger.error("tcp call decode: unexpected exception caught", e);
			throw e;
		} catch (Throwable t) {
			logger.error("tcp call decode: unexpected exception caught", t);
			   
		}
	}

	protected Object decodeI(ChannelHandlerContext ctx, Channel channel, ByteBuf buffer,List<Object> out) throws Exception {
		boolean isTransformed = false;
		addCounter();
		buffer.markReaderIndex();
		int len = buffer.readableBytes();
		if (logger.isDebugEnabled())
			logger.debug("tcp call: channel [{}] readable [{}]", channel.remoteAddress(), len);

		if (len < 4) {
			ec.incrementAndGet();
			return null;
		}

		int headerBits = buffer.readInt();
		int opt = (headerBits >> 24) & 0xFF;

		int bodyLen = headerBits & 0xFFFFFF;

		if ((opt & 0x01) != 0)
			isTransformed = true;

		if ((opt & 0x0E) != 0) {
			logger.warn("tcp call: invalid header opt [{}] from [{}]", Integer.toHexString(opt), channel.remoteAddress());
			channel.close().sync();
			buffer.resetReaderIndex();
			return null;
		}

		if (bodyLen < 0 || bodyLen >= DEFAULT_MAX_FRAMESIZE) {
			logger.warn("tcp call: invalid body length [{}] from [{}]", bodyLen, channel.remoteAddress());
			buffer.resetReaderIndex();
			channel.close().sync();
			return null;
		}

		if (len < 4 + bodyLen) {
			buffer.resetReaderIndex();
			ec.incrementAndGet();
			return null;
		}

		byte[] payload = new byte[bodyLen];
		buffer.readBytes(payload, 0, bodyLen);
		String line = new String(payload, "utf-8");
		Map<String, Object> params = JSONConverter.parse(new JSONObject(line));

		if ((opt & 0xF0) == 0xF0) {
			if (logger.isDebugEnabled())
				logger.debug("tcp call: client [{}] post params [{}]", channel.remoteAddress(), params);

			String topic = (String) params.get("topic");
			String login = (String) params.get("login");
			FdsPostMessage msg = new FdsPostMessage(topic, login, params);
			msg.setTransformed(isTransformed);
			out.add(msg);
			return msg;
		} else if ((opt & 0x80) == 0x80) {
			if (logger.isDebugEnabled())
				logger.debug("tcp call: client [{}] post params [{}]", channel.remoteAddress(), params);

			String guid = (String) params.get("guid");
			String topic = (String) params.get("topic");
			String login = (String) params.get("login");
			FdsPostMessage msg = new FdsPostMessage(topic, login, params, ctx.channel(), guid);
			msg.setTransformed(isTransformed);
			out.add(msg);
			return msg;
		} else {
			if (logger.isDebugEnabled())
				logger.debug("tcp call: client [{}] request params [{}]", channel.remoteAddress(), params);

			FdsCallMessage msg = new FdsCallMessage(ctx.channel(), params);
			msg.setTransformed(isTransformed);
			out.add(msg);
			return msg;
		}
	}

	private void addCounter() {
		int cc = c.incrementAndGet();
		if (cc % 10 == 0) {
			if (logger.isDebugEnabled())
				logger.debug("TcpCallDecoder called {} times (error found: {})", cc, ec.get());
		}
	}
}
