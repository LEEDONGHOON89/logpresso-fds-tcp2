package com.logpresso.fds.tcp2;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

import org.bouncycastle.util.Arrays;

public class FixedLengthMerge {
	public final static int ALIGN_LEFT =1;
	public final static int ALIGN_RIGHT =2;
	
	String[] strings;
	int[] lengths;
	int[] aligns;
	
	
	public String[] getStrings() {
		return strings;
	}
	public void setStrings(String[] strings) {
		this.strings = strings;
	}
	public int[] getLengths() {
		return lengths;
	}
	public void setLengths(int[] lengths) {
		this.lengths = lengths;
	}
	public int[] getAligns() {
		return aligns;
	}
	public void setAligns(int[] aligns) {
		this.aligns = aligns;
	}
	public static int getAlignLeft() {
		return ALIGN_LEFT;
	}
	public static int getAlignRight() {
		return ALIGN_RIGHT;
	}
	
	public byte[] merge() {
		Vector<byte[]> list = new Vector<byte[]>();
		int len = lengths.length;
	
		for(int i=0; i<len; i++) {
			try {
				if(strings[i]==null)
					list.add("".getBytes("euc-kr"));
				else 
					list.add(strings[i].getBytes("euc-kr"));
			} catch (UnsupportedEncodingException e) {
			}
		}
		
		int index =0;
		byte[] retBytes = new byte[sum(lengths)];
		Arrays.fill(retBytes, (byte)' ');
		for(int i =0; i<len; i++) {
			byte[] bytes = list.get(i);
			int fixLen =0;
			if(bytes.length> lengths[i]) {
				fixLen = lengths[i];
			}else {
				fixLen = bytes.length;
			}
			
			if(aligns[i] == ALIGN_LEFT) {
				System.arraycopy(bytes, 0, retBytes, index, fixLen);
			}else {
				int rightIndex = index + (lengths[i] - fixLen);
				System.arraycopy(bytes, 0, retBytes, rightIndex, fixLen);
			}
			
			index += lengths[i];
			
		}
		return retBytes;
	}
	
	private int sum(int[] array) {
		int sumValue=0;
		for(int val : array) {
			sumValue +=val;
		}
		return sumValue;
			
	}
	
}
