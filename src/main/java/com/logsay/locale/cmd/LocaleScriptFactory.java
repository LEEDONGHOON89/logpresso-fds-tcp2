package com.logsay.locale.cmd;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.api.Script;
import org.araqne.api.ScriptFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logsay.locale.LocaleManager;
import com.logsay.locale.LocaleSyncService;

@Component(name = "locale-script-factory")
@Provides
public class LocaleScriptFactory implements ScriptFactory {
	private final Logger slog = LoggerFactory.getLogger(LocaleScriptFactory.class);
	
	@Requires
	private LocaleManager nationalityManager;
	
	@Requires
	private LocaleSyncService localeSyncService;
	
	@Validate
	public void start() {
	}
	
	@ServiceProperty(name = "alias", value = "locale")
	private String alias;
	
	@Override
	public Script createScript() {
		return new LocaleScript(nationalityManager, localeSyncService);
	}
}
