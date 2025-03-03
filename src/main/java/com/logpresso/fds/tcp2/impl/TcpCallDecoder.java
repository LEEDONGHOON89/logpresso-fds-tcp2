package com.logpresso.fds.tcp2.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.json.JSONConverter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logpresso.fds.tcp2.FdsCallMessage;
import com.logpresso.fds.tcp2.FdsPostMessage;

public class TcpCallDecoder extends ByteToMessageDecoder {
	private static final int DEFAULT_MAX_FRAMESIZE = 32769;//short 2바이트일경우 최대 바이트크기
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
//		int len = buffer.readableBytes();
		logger.debug("tcp call: channel [{}]", channel.remoteAddress());
		
		int len = buffer.readableBytes();
		if (logger.isDebugEnabled()) {
			logger.debug("tcp call: channel [{}] readable [{}]", channel.remoteAddress(), len);
		}
		if (len < 2) {
			ec.incrementAndGet();
			return null;
		}

//		if (logger.isDebugEnabled())
//			logger.debug("tcp call: opt [{}]", optStr);
//
		//전문 길이 header 6바이트일경우 사용 ---start
//		byte[] bodybytes = new byte[6];  
//		buffer.readBytes(bodybytes);
//		String bodyByteStr = new String(bodybytes, "utf-8");
//		if (logger.isDebugEnabled())
//			logger.debug("tcp call: bodybytes [{}], bodyByteStr= [{}]", bodybytes,bodyByteStr);
//		int bodyLen = Integer.parseInt(bodyByteStr);

		//전문 길이 header 6바이트일경우 사용 ---end
		
		int bodyLen = buffer.readShort(); 
		if (logger.isDebugEnabled())
			logger.debug("tcp call: bodyLen [{}]", bodyLen);
		
		if (bodyLen < 0 || bodyLen >= DEFAULT_MAX_FRAMESIZE) {
			logger.warn("tcp call: invalid body length [{}] from [{}]", bodyLen, channel.remoteAddress());
			buffer.resetReaderIndex();
			channel.close().sync();
			return null;
		}

		if (len < 2 + bodyLen) {
			buffer.resetReaderIndex();
			ec.incrementAndGet();
			return null;
		}
	
		
		byte[] payload = new byte[bodyLen];
		buffer.readBytes(payload, 0, bodyLen);
		String line = new String(payload, "utf-8");
		if (logger.isDebugEnabled())
			logger.debug("tcp call: line [{}]", line);
		String optStr = line.substring(0, 1);
		logger.debug("tcp call: optStr [{}]",optStr);
		//opt 체크
		if (!optStr.equals("C") && !optStr.equals("P")) {
			logger.warn("tcp call: invalid header opt [{}] from [{}]", optStr, channel.remoteAddress());
			channel.close().sync();
			buffer.resetReaderIndex();
			return null;
		}
		
		Map<String, Object> params = new HashMap<String, Object>();
		if(optStr.equals("C")) {
			if (logger.isDebugEnabled())
				logger.debug("tcp call: client [{}] request params [{}]", channel.remoteAddress(), params);
			String guid = UUID.randomUUID().toString();
			params.put("line", line);
			params.put("guid", guid);
			FdsCallMessage msg = new FdsCallMessage(ctx.channel(), params);
			msg.setTransformed(isTransformed);
			out.add(msg);
			return msg;
		}else if(optStr.equals("P")) {
			if (logger.isDebugEnabled())
				logger.debug("tcp call: client [{}] post line [{}]", channel.remoteAddress(), line);
			String guid = UUID.randomUUID().toString();
			params.put("line", line);
			params.put("guid", guid);
			FdsPostMessage msg = new FdsPostMessage(null,null,params);
			msg.setTransformed(isTransformed);
			out.add(msg);
			return msg;
		}else {
			return null;
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
