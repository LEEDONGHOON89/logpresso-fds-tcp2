package com.logsay.locale.cmd;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.araqne.api.Script;
import org.araqne.api.ScriptArgument;
import org.araqne.api.ScriptContext;
import org.araqne.api.ScriptUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logsay.locale.LocaleSyncService;
import com.logsay.locale.LocaleManager;
import com.logsay.locale.model.Locale;
import com.logsay.locale.msgbus.LocalePlugin;

public class LocaleScript implements Script {
	private final Logger slog = LoggerFactory.getLogger(LocaleScript.class);

	private LocaleManager localeManager;
	private LocaleSyncService localeSyncService;
	private LocalePlugin localePlugin;

	private ScriptContext context;
	
	public LocaleScript(LocaleManager localeManager, LocaleSyncService localeSyncService) {
		this.localeManager = localeManager;
		this.localeSyncService = localeSyncService;
	}
	
	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}
	
	@ScriptUsage(description = "add locale", arguments = { 
			@ScriptArgument(name = "country", type = "string", description = "An real country of the ip. eg. KR, CN, US and so on."),
			@ScriptArgument(name = "start IP", type = "string", description = "The start ip of range."),
			@ScriptArgument(name = "end IP", type = "string", description = "The end ip of range.", optional = true) })
	public void addLocale(String[] args) throws ParseException {
		String country = args[0];
		String startIp = args[1];
		String endIp = (args.length < 3) ? null : args[2];
		
		Map<String, String> locale = new HashMap<String, String>();
		
		locale.put("guid", UUID.randomUUID().toString());
		locale.put("country", country);
		locale.put("start_ip", startIp);
		locale.put("end_ip", endIp);		
		
		localeManager.addLocale(locale);
		
		context.println("added");
	}
	
	@ScriptUsage(description = "delete locale", arguments = { 
			@ScriptArgument(name = "guid", type = "string", description = "guid") })
	public void deleteLocale(String[] args) {
		String guid = args[0];
		
		localeManager.deleteLocale(guid);
		
		context.println("deleted");
	}
	
	@ScriptUsage(description = "reload locale")
	public void reloadLocale(String[] args) {		
		localeManager.reloadLocale(false);
		
		context.println("reloaded");
	}
	
	@ScriptUsage(description = "view locale")
	public void viewLocale(String[] args) {	
		context.println("GUID" + "\t" + "Country" + "\t" + "From" + "\t" + "To" + "\t" + "REG DATE");
		context.println("--------------------");
		for (Map<String, Locale> m : localeManager.getLocales()) {
			for (String key : m.keySet()) {
				Locale locale = new Locale();
				locale = (Locale) m.get(key);
				context.println(key + "\t" + locale.getCountry() + "\t" + longToIp(locale.getStartIp()) + "\t" + longToIp(locale.getEndIp()) + "\t" + locale.getRegDate());
			}
		}
	}
	
	public String longToIp(long ip) {
		String ipTxt = ((ip >> 24) & 0xFF) + "."
				+ ((ip >> 16) & 0xFF) + "."
				+ ((ip >> 8) & 0xFF) + "."
				+ (ip & 0xFF);
		
		return (ip == 0) ? "" : ipTxt;
	}
}
