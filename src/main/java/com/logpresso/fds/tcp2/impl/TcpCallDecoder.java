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
		
		// 잉카 소스 참조 start
//		int headerbit1 = buffer.readByte();
//		if(headerbit1 == -1) {
//			channel.close().sync();
//			buffer.resetReaderIndex();
//			return null;
//		}
//			
//		int headerbit2 = buffer.readByte();
//		if(headerbit2 == -1) {
//			channel.close().sync();
//			buffer.resetReaderIndex();
//			return null;
//		}	
//		headerbit1 = headerbit1 <<8;
//		headerbit1 = headerbit1 & 0x0000FF00;
//		headerbit2 = headerbit2 & 0x000000FF;
//		
//		int bodyLen = (headerbit1 | headerbit2);
		// 잉카 소스 참조 end
		
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
		String line = new String(payload, "euc-kr");
		if (logger.isDebugEnabled())
			logger.debug("tcp call: line [{}]", line);
		String fdsDataCd = line.substring(0, 1);
		logger.debug("tcp call: fdsDataCd [{}]",fdsDataCd);
		//opt 체크
		if (!fdsDataCd.equals("C") && !fdsDataCd.equals("P")) {
			logger.warn("tcp call: invalid header opt [{}] from [{}]", fdsDataCd, channel.remoteAddress());
			channel.close().sync();
			buffer.resetReaderIndex();
			return null;
		}
		
		String fdsBypassYn = line.substring(1, 2);
		if (!fdsBypassYn.equals("N") && !fdsBypassYn.equals("Y")) {
			logger.warn("tcp call: invalid header opt [{}] from [{}]", fdsDataCd, channel.remoteAddress());
			channel.close().sync();
			buffer.resetReaderIndex();
			return null;
		}
		
		String fdsProtocolId = line.substring(2, 4);
		Map<String, Object> params = new HashMap<String, Object>();
		if(fdsDataCd.equals("C")) {
			if (logger.isDebugEnabled())
				logger.debug("tcp call: client [{}] request params [{}]", channel.remoteAddress(), params);
			String guid = UUID.randomUUID().toString();
			params.put("line", line);
			params.put("guid", guid);
			params.put("fdsDataCd", fdsDataCd);
			params.put("fdsBypassYn", fdsBypassYn);
			params.put("fdsProtocolId", fdsProtocolId);
			FdsCallMessage msg = new FdsCallMessage(ctx.channel(), params);
			msg.setTransformed(isTransformed);
			out.add(msg);
			return msg;
		}else if(fdsDataCd.equals("P")) {
			if (logger.isDebugEnabled())
				logger.debug("tcp call: client [{}] post line [{}]", channel.remoteAddress(), line);
			String guid = UUID.randomUUID().toString();
			params.put("line", line);
			params.put("guid", guid);
			params.put("fdsDataCd", fdsDataCd);
			params.put("fdsBypassYn", fdsBypassYn);
			params.put("fdsProtocolId", fdsProtocolId);
			FdsPostMessage msg = new FdsPostMessage(null,null,params);
			msg.setTransformed(isTransformed);
			out.add(msg);
			return msg;
		}else {
			return null;
		}
		
//		Map<String, Object> params = JSONConverter.parse(new JSONObject(line));

//		if ((opt & 0xF0) == 0xF0) {
//			if (logger.isDebugEnabled())
//				logger.debug("tcp call: client [{}] post params [{}]", channel.remoteAddress(), params);

//			String topic = (String) params.get("topic");
//			String login = (String) params.get("login");
//			FdsPostMessage msg = new FdsPostMessage(topic, login, params);
//			msg.setTransformed(isTransformed);
//			out.add(msg);
//			return msg;
//		} else if ((opt & 0x80) == 0x80) {
//			if (logger.isDebugEnabled())
//				logger.debug("tcp call: client [{}] post params [{}]", channel.remoteAddress(), params);
//
//			String guid = (String) params.get("guid");
//			String topic = (String) params.get("topic");
//			String login = (String) params.get("login");
//			FdsPostMessage msg = new FdsPostMessage(topic, login, params, ctx.channel(), guid);
//			msg.setTransformed(isTransformed);
//			out.add(msg);
//			return msg;
//		} else {
//			if (logger.isDebugEnabled())
//				logger.debug("tcp call: client [{}] request params [{}]", channel.remoteAddress(), params);
//
//			FdsCallMessage msg = new FdsCallMessage(ctx.channel(), params);
//			msg.setTransformed(isTransformed);
//			out.add(msg);
//			return msg;
//		}
	}

	private void addCounter() {
		int cc = c.incrementAndGet();
		if (cc % 10 == 0) {
			if (logger.isDebugEnabled())
				logger.debug("TcpCallDecoder called {} times (error found: {})", cc, ec.get());
		}
	}
}
