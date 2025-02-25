package com.logpresso.fds.tcp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TcpCallStats {
	private long redirectCallCount;
	private long redirectPostCount;
	private long rejectedCallCount;
	private long rejectedPostCount;
	private long callCount;
	private long postCount;
	private long lastCallTime;
	private long lastPostTime;
	private long pendingCallCount;
	private long okCount;
	private long arsCount;
	private long blockCount;
	private long errorCount;

	public long getErrorCount() {
		return errorCount;
	}

	public long getRedirectCallCount() {
		return redirectCallCount;
	}

	public void setRedirectCallCount(long redirectCallCount) {
		this.redirectCallCount = redirectCallCount;
	}

	public long getRedirectPostCount() {
		return redirectPostCount;
	}

	public void setRedirectPostCount(long redirectPostCount) {
		this.redirectPostCount = redirectPostCount;
	}

	public long getRejectedCallCount() {
		return rejectedCallCount;
	}

	public void setRejectedCallCount(long rejectedCallCount) {
		this.rejectedCallCount = rejectedCallCount;
	}

	public long getRejectedPostCount() {
		return rejectedPostCount;
	}

	public void setRejectedPostCount(long rejectedPostCount) {
		this.rejectedPostCount = rejectedPostCount;
	}

	public long getCallCount() {
		return callCount;
	}

	public void setCallCount(long callCount) {
		this.callCount = callCount;
	}

	public long getPostCount() {
		return postCount;
	}

	public void setPostCount(long postCount) {
		this.postCount = postCount;
	}

	public long getLastCallTime() {
		return lastCallTime;
	}

	public void setLastCallTime(long lastCallTime) {
		this.lastCallTime = lastCallTime;
	}

	public long getLastPostTime() {
		return lastPostTime;
	}

	public void setLastPostTime(long lastPostTime) {
		this.lastPostTime = lastPostTime;
	}

	public long getPendingCallCount() {
		return pendingCallCount;
	}

	public void setPendingCallCount(long pendingCallCount) {
		this.pendingCallCount = pendingCallCount;
	}

	public long getOkCount() {
		return okCount;
	}

	public void setOkCount(long okCount) {
		this.okCount = okCount;
	}

	public long getArsCount() {
		return arsCount;
	}

	public void setArsCount(long arsCount) {
		this.arsCount = arsCount;
	}

	public long getBlockCount() {
		return blockCount;
	}

	public void setBlockCount(long blockCount) {
		this.blockCount = blockCount;
	}

	@Override
	public String toString() {
		return "call=" + callCount + " (redirect " + redirectCallCount + ", last " + df(lastCallTime) + "), post=" + postCount
				+ " (redirect " + redirectPostCount + ", last " + df(lastPostTime) + "), pending=" + pendingCallCount;
	}

	private String df(long t) {
		if (t == 0)
			return "N/A";

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return df.format(new Date(t));
	}

	public void setErrorCount(long l) {
		this.errorCount = l;
	}

}
