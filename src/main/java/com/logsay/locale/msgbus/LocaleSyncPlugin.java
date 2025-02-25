package com.logsay.locale.msgbus;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.msgbus.Request;
import org.araqne.msgbus.Response;
import org.araqne.msgbus.handler.MsgbusMethod;
import org.araqne.msgbus.handler.MsgbusPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logsay.locale.LocaleManager;

@Component (name = "locale-sync-plugin")
@MsgbusPlugin
public class LocaleSyncPlugin {
	private final Logger slog = LoggerFactory.getLogger(LocaleSyncPlugin.class);

	@Requires
	private LocaleManager localeManager;
	
	private static ThreadLocal<Boolean> msgbusCall = new ThreadLocal<Boolean>();
	
	@Validate
	public void start() {
	}
	
	@MsgbusMethod
	public void reload(Request req, Response resp) {
		String category = req.getString("category", true);

		try {
			msgbusCall.set(true);
			if (category.equalsIgnoreCase("locale")) {
				localeManager.reloadLocale(true);
			} 
			
			slog.debug("Logsay debug, locale sync completed. category [{}]", category);
		} catch (Exception e) {
			slog.error("Logsay saids, locale sync service failed.");
		} finally {
			msgbusCall.set(false);
		}
	}
}
