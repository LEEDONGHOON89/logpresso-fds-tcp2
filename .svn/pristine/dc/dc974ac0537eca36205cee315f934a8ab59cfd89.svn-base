package com.logsay.locale.query;

import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.Row;
import org.araqne.logdb.RowBatch;
import org.araqne.logdb.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logsay.locale.LocaleManager;

public class FdsLocaleQuery extends QueryCommand implements ThreadSafe {
	private final Logger slog = LoggerFactory.getLogger(FdsLocaleQuery.class);

	private LocaleManager localeManager;
	private String fn;

	public FdsLocaleQuery(LocaleManager localeManager, String fn) {
		this.localeManager = localeManager;
		this.fn = fn;
	}

	@Override
	public String getName() {
		return "fdslocale";
	}
	
	@Override
	public void onStart() {
	}
	
	@Override
	public void onClose() {
	}
	
	@Override
	public void onPush(RowBatch rowBatch) {
		for (int i=0; i<rowBatch.size; i++) {
			Row row = rowBatch.rows[i];
			onPush(row);
		}
	}
	
	@Override
	public void onPush(Row row) {
		Object ip = row.get(fn);
		
		if (ip == null) {
			pushPipe(row);
			return;
		}
		
		String locale = localeManager.localeMatcher(ip.toString());
		
		if (locale != null && !locale.equalsIgnoreCase("NA"))
			row.put("country", locale);
		
		pushPipe(row);
		
		return;
	}
	
	@Override
	public String toString() {
		return "fdslocale";
	}
	@Override
	public boolean isStreamable() {
        return true;
    }
}
