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
			Map<String, Object> response = call.getResponse();
			
			if(response==null) {
				if(logger.isErrorEnabled())
					logger.error("fds tcp : response data is null");
				
				return;
			}
			byte[] body = call.getResponseBytes(response);// 응답 전문 body부 생성
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
//			m.put("guid", post.getGuid());
//			String line = JSONConverter.jsonize(m);
			String line = "L"+post.getGuid(); // L+guid값으로 데이터부 ack 세팅
			byte[] body = line.getBytes("euc-kr");
			int len = body.length;
			
			byte[] byteArray = ByteBuffer.allocate(2).putShort((short) len).array();
			out.writeBytes(Unpooled.wrappedBuffer(byteArray,body));
		}
	}
	
	protected byte[] getLengthBytes(int length,int lengthByteSize) {
		byte[] lengthBytes = new byte[2];
		int tempLength = length >> 8;
		lengthBytes[0] = (byte)(tempLength & 0x000000FF);
		tempLength = length;
		lengthBytes[1] = (byte)(tempLength & 0x000000FF);
		
		return lengthBytes;
	}
}
