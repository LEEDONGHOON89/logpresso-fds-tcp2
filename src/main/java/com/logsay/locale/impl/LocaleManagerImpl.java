package com.logsay.locale.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logpresso.fds.api.SqlConnectorService;
import com.logpresso.fds.api.model.Query;
import com.logsay.locale.LocaleManager;
import com.logsay.locale.LocaleQueryTemplateManager;
import com.logsay.locale.LocaleSyncService;
import com.logsay.locale.model.Locale;

@Component(name = "locale-manager")
@Provides
public class LocaleManagerImpl implements LocaleManager {
	private final static Logger slog = LoggerFactory.getLogger(LocaleManagerImpl.class);
	
	@Requires
	private SqlConnectorService db;
	
	@Requires
	private LocaleQueryTemplateManager qtm;
	
	@Requires
	private LocaleSyncService localeSyncService;
	
	private ConcurrentHashMap<String, Locale> localeMap = new ConcurrentHashMap<String, Locale>();

	@Validate
	public void start() {
		if (!db.isConfigured())
			return;

		Set<String> tableNames = db.getTableNames();
		
		if (tableNames.contains("FDS_LOCALE") || tableNames.contains("fds_locale"))
			loadLookup("LOCALE");
	}
	
	public void loadLookup(String lookupName) {
		String q = qtm.get("FDS_" + lookupName + "_S001");
		if (q == null)
			return;

		for (Map<String, Object> row : db.query(q)) {
			if (row.isEmpty()) {
				slog.info("Logsay saids, doesn't set domestic IP range.");
				return;
			}
			
			try {
				String guid = (String) row.get("guid");
				
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Locale locale = new Locale();
				
				locale.setGuid(guid);
				locale.setCountry((String) row.get("country"));
				locale.setStartIp((long) row.get("start_ip"));
				locale.setEndIp((long) row.get("end_ip"));
				locale.setRegDate(df.parse((String) row.get("reg_date")));

				localeMap.put(guid, locale);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public String localeMatcher(String ip) {
		long inputIp = ipToLong(ip);
		
		for(String guid : localeMap.keySet()) {
			Locale locale = new Locale();
			locale = localeMap.get(guid);
			
			long startIp = locale.getStartIp();
			long endIp = locale.getEndIp();
			
			if (endIp > 0) {
				if (startIp <= inputIp && inputIp <= endIp)
					return locale.getCountry();
			} else {
				if (startIp == inputIp)
					return locale.getCountry();
			}
		}
		
		return "NA";
	}
	
	@Override
	public void addLocale(Map<String, String> m) throws ParseException {
		String p1 = m.get("guid");
		String p2 = m.get("country");
		long p3 = ipToLong(m.get("start_ip"));
		long p4 = 0;
		
		String endIp = m.get("end_ip"); 
		if (endIp == null || endIp.length()<1)
			endIp = null;
		else 
			p4 = ipToLong((String) endIp);
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String p5 = df.format(new Date());
		
		Query query = new Query(qtm.get("FDS_LOCALE_I001"), p1, p2, p3, p4, p5);
		db.insert(query);
		
		Locale locale = new Locale();
		locale.setGuid(p1);
		locale.setCountry(p2);
		locale.setStartIp(p3);
		locale.setEndIp(p4);
		locale.setRegDate(df.parse(p5));
		
		localeMap.put(p1, locale);
		
		localeSyncService.reload("LOCALE");
	}
	
	public long ipToLong(String ipAddress) {
		long result = 0;
		String[] ipAddressInArray = ipAddress.split("\\.");

		for (int i = 3; i >= 0; i--) {
			long ip = Long.parseLong(ipAddressInArray[3 - i]);
			result |= ip << (i * 8);
		}

		return result;
	}
	
	public String longToIp(long ip) {
		return ((ip >> 24) & 0xFF) + "."
				+ ((ip >> 16) & 0xFF) + "."
				+ ((ip >> 8) & 0xFF) + "."
				+ (ip & 0xFF);
	}

	@Override
	public Locale getLocale(String guid) {
		return localeMap.get(guid);
	}

	@Override
	public List<Map<String, Locale>> getLocales() {
		List<Map<String, Locale>> results = new ArrayList<Map<String, Locale>>();
		
		for(String guid : localeMap.keySet()) {
			Map<String, Locale> result = new HashMap<String, Locale>();
			result.put(guid, localeMap.get(guid));
			results.add(result);
		}
		
		return results;
	}

	@Override
	public void reloadLocale(boolean isSync) {
		localeMap.clear();
		loadLookup("LOCALE");	
		
		if (!isSync)
			localeSyncService.reload("LOCALE");
	}

	@Override
	public void deleteLocale(String guid) {
		if (getLocale(guid) == null) {
			slog.info("Logsay saids, can't find it");
			return;
		}
		
		Query query = new Query(qtm.get("FDS_LOCALE_D001"), guid);
		db.delete(query);
		
		localeMap.remove(guid);		
		
		localeSyncService.reload("LOCALE");
	}

}
