package com.logpresso.fds.tcp2.impl;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logpresso.fds.tcp2.FdsAckMessage;
import com.logpresso.fds.tcp2.FdsCallMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class TcpCallEncoder extends MessageToByteEncoder {
	private final Logger logger = LoggerFactory.getLogger(TcpCallEncoder.class.getName());
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		if (msg instanceof FdsCallMessage) {
			FdsCallMessage call = (FdsCallMessage) msg;
//			String line = JSONConverter.jsonize(call.getResponse());
			String response_line = (String) call.getResponse().get("response");
			byte[] body = response_line.getBytes("utf-8");
			int len = body.length;
			
			//6byte 길이사용할경우 
			//String headerStr = String.format("%06d", len);
			//byte[] header = headerStr.getBytes("utf-8");
			//short 2바이트일경우 사용
			byte[] byteArray = ByteBuffer.allocate(2).putShort((short) len).array();
			out.writeBytes(Unpooled.wrappedBuffer(byteArray,body));
		} else if (msg instanceof FdsAckMessage) {
			/*
		 	**DB증권,IM증권 대응용도로 TCP번들 추가작업시에 불필요할 것으로 판단되나, 테스트 완료 후 삭제 예정
		 	*사용하지 않을것으로 보이지만 일단 유지
			*/
			FdsAckMessage post = (FdsAckMessage) msg;
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("guid", post.getGuid());
			String line = JSONConverter.jsonize(m);

			byte[] body = line.getBytes("utf-8");
			byte[] header = new byte[4];

			int len = body.length;
			header[0] = (byte) ((len >> 24) & 0x3f);
			header[1] = (byte) ((len >> 16) & 0xff);
			header[2] = (byte) ((len >> 8) & 0xff);
			header[3] = (byte) (len & 0xff);

			if (post.isTimeout()) {
				header[0] = (byte) (header[0] | 0x40);
			}
			out.writeBytes(Unpooled.wrappedBuffer(header,body));
		}
	}
}
