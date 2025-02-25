package com.logsay.locale.msgbus;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.httpd.DefaultFileDownloadHandler;
import org.araqne.httpd.FileDownloadFuture;
import org.araqne.httpd.FileDownloadService;
import org.araqne.msgbus.MsgbusException;
import org.araqne.msgbus.Request;
import org.araqne.msgbus.Response;
import org.araqne.msgbus.handler.MsgbusMethod;
import org.araqne.msgbus.handler.MsgbusPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import com.logsay.locale.LocaleManager;
import com.logsay.locale.LocaleSyncService;
import com.logsay.locale.model.Locale;

@Component (name = "locale-plugin")
@MsgbusPlugin
public class LocalePlugin {
	private final Logger slog = LoggerFactory.getLogger(LocalePlugin.class);

	@Requires
	private LocaleManager localeManager;
	
	@Requires
	private FileDownloadService downloadService;
	
	@Requires
	private LocaleSyncService localeSyncService;
	
	@Validate
	public void start() {
	}
	
	@MsgbusMethod
	public void getLocales(Request req, Response resp) {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		
		for (Map<String, Locale> m : localeManager.getLocales()) {
			for (String key : m.keySet()) {
				Locale locale = new Locale();
				locale = (Locale) m.get(key);
				
				Map<String, Object> result = new HashMap<String, Object>();
				result.put("guid", locale.getGuid());
				result.put("country", locale.getCountry());
				result.put("start_ip", longToIp(locale.getStartIp()));
				result.put("end_ip", longToIp(locale.getEndIp()));
				result.put("reg_date", locale.getRegDate());
				
				results.add(result);
			}
		}
		
		resp.put("results", results);
	}
	
	@MsgbusMethod
	public void addLocale(Request req, Response resp) {
		try {
			String country = req.getString("country");
			String startIp = req.getString("start_ip");
			String endIp = req.getString("end_ip");
			
			Map<String, String> locale = new HashMap<String, String>();
			
			locale.put("guid", UUID.randomUUID().toString());
			locale.put("country", country);
			locale.put("start_ip", startIp);
			locale.put("end_ip", endIp);		
			
			try {
				localeManager.addLocale(locale);
				resp.put("result", "completed.");
			} catch (ParseException e) {
				resp.put("result", "fail.");
				e.printStackTrace();
			}
		} catch (Throwable t) {
			throw new MsgbusException("locale", "add-locale-failure");
		}
	}
	
	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void addLocales(Request req, Response resp) {
		try {
			List<Map<String, String>> locales = (List<Map<String, String>>) req.get("locales");
			for (Map<String, String> locale : locales) {
				locale.put("guid", UUID.randomUUID().toString());
				localeManager.addLocale(locale);
			}
			
			resp.put("result", "completed.");
		} catch (Throwable t) {
			resp.put("result", "fail.");
			throw new MsgbusException("fds", "add-blacklist-failure");
		}
	}
	
	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeLocales(Request req, Response res) {
		try {
			List<String> guids = (List<String>) req.get("guids");
			
			for (String guid : guids)
				localeManager.deleteLocale(guid);
			
			localeSync("locale");
		} catch (Throwable t) {
			throw new MsgbusException("locale", "remove-locale-failure");
		}
	}
	
	public String longToIp(long ip) {
		String ipTxt = ((ip >> 24) & 0xFF) + "."
				+ ((ip >> 16) & 0xFF) + "."
				+ ((ip >> 8) & 0xFF) + "."
				+ (ip & 0xFF);
		
		return (ip == 0) ? "" : ipTxt;
	}
	
	@MsgbusMethod
	public void prepareDownload(Request req, Response resp) {
		try {
			Downloader downloader = new Downloader();
			downloadService.addHandler(downloader);

			resp.put("token", downloader.getToken());
		} catch (Throwable t) {
			throw new MsgbusException("logsay", "locale-download-failiure");
		}
	}
	
	private class Downloader extends DefaultFileDownloadHandler {
		private String fileName;
		private boolean downloading;

		public Downloader() {
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmm");
			this.fileName = "locale" + "_" + df.format(new Date()) + ".csv";
		}

		@Override
		public String getFileName() {
			return fileName;
		}

		@Override
		public boolean isDownloading() {
			return downloading;
		}

		@Override
		public void startDownload(OutputStream os, FileDownloadFuture future) {
			downloading = true;

			CSVWriter writer = null;

			try {
				writer = new CSVWriter(new OutputStreamWriter(os, "utf-8"));
				for (Map<String, Locale> m : localeManager.getLocales()) {
					for (String key : m.keySet()) {
						Locale locale = new Locale();
						locale = (Locale) m.get(key);
						
						Map<String, Object> result = new HashMap<String, Object>();
						result.put("guid", locale.getGuid());
						result.put("country", locale.getCountry());
						result.put("start_ip", longToIp(locale.getStartIp()));
						result.put("end_ip", longToIp(locale.getEndIp()));
						result.put("reg_date", locale.getRegDate());
						
						String[] line = new String[] {locale.getGuid(), locale.getCountry(), longToIp(locale.getStartIp()), longToIp(locale.getEndIp())};
						writer.writeNext(line);
					}
				}
			} catch (Throwable t) {
				slog.error("logsay saids, cannot download locales", t);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
					}
				}
				future.onCompleted();
			}
		}
	}
	
	@MsgbusMethod
	public void importLocales(Request req, Response resp) {
		try {
			List<Map<String, Object>> errors = new ArrayList<Map<String, Object>>();

			String content = req.getString("content", true);
			StringTokenizer tok = new StringTokenizer(content, "\n");

			while (tok.hasMoreTokens()) {
				String line = tok.nextToken().trim();
				String elements[] = line.split(",");
				
				Map<String, String> entry = new HashMap<String, String>();
				entry.put("country", elements[0].toString());
				entry.put("start_ip", elements[1].toString());
				
				String endIp = (elements[2] == null) ? null : elements[2].toString(); 
				entry.put("end_ip", endIp);
				
				try {
					localeManager.addLocale(entry);
				} catch (Throwable t) {
					Map<String, Object> error = new HashMap<String, Object>();
					error.put("line", line);
					error.put("cause", t.getMessage());
					errors.add(error);
				}
			}

			resp.put("errors", errors);
		} catch (Throwable t) {
			throw new MsgbusException("locale", "import-locale-failure");
		}
	}
	
	private void localeSync(String category) {
		try {
			localeSyncService.reload(category);
		} catch (Throwable t) {
			slog.error("logpresso fds: cannot sync fds blacklist", t);
		}
	}

}
