package com.logsay.locale.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.logpresso.jdbc.JdbcProfile;
import org.logpresso.jdbc.JdbcProfileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logsay.locale.LocaleQueryTemplateManager;

@Component(name = "locale-query-template-manager")
@Provides
public class LocaleQueryTemplateManagerImpl implements LocaleQueryTemplateManager {
	private final Logger slog = LoggerFactory.getLogger(LocaleQueryTemplateManagerImpl.class);

	private ConcurrentHashMap<String, String> mariaMap = new ConcurrentHashMap<String, String>();
	private ConcurrentHashMap<String, String> oraMap = new ConcurrentHashMap<String, String>();

	@Requires
	private JdbcProfileRegistry profileRegistry;

	@Validate
	public void start() {
		addMariaSqls();
		addOracleSqls();
	}

	private void addOracleSqls() {
		add(oraMap, "FISS_DIC_001", "SELECT TABLE_NAME FROM USER_TABLES UNION ALL SELECT TABLE_NAME FROM USER_SYNONYMS");

		for (String table : Arrays.asList("FDS_LOCALE")) {
			add(oraMap, "SEQ_" + table, newSeq(table));
		}

		add(oraMap, "SEQ", "CREATE SEQUENCE NEXT_SEQ INCREMENT BY 1 START WITH 1 MAXVALUE 100000000 NOCACHE NOCYCLE");
		
		add(oraMap, "FDS_LOCALE_S001", "SELECT * FROM FDS_LOCALE");
		add(oraMap, "FDS_LOCALE_I001", "INSERT INTO FDS_LOCALE (GUID, COUNTRY, START_IP, END_IP, REG_DATE) VALUES (?,?,?,?)");
		add(oraMap, "FDS_LOCALE_D001", "DELETE FROM FDS_LOCALE WHERE GUID=?");
		
	}

	private void addMariaSqls() {
		add(mariaMap, "FDS_LOCALE_S001", "SELECT * FROM FDS_LOCALE");
		add(mariaMap, "FDS_LOCALE_I001", "INSERT INTO FDS_LOCALE (GUID, COUNTRY, START_IP, END_IP, REG_DATE) VALUES (?,?,?,?,?)");
		add(mariaMap, "FDS_LOCALE_D001", "DELETE FROM FDS_LOCALE WHERE GUID=?");
	}

	private String newSeq(String table) {
		String name = table.substring(4);
		return "CREATE OR REPLACE TRIGGER \"id_" + name + "\" BEFORE INSERT ON \"" + table
				+ "\" FOR EACH ROW WHEN (new.\"id\" is null) DECLARE v_id \"" + table
				+ "\".\"id\"%TYPE; BEGIN SELECT next_seq.nextval INTO v_id FROM DUAL; :new.\"id\" := v_id; END \"" + name + "\";";
	}

	@Invalidate
	public void stop() {
		mariaMap.clear();
		oraMap.clear();
	}

	private void add(Map<String, String> map, String code, String sql) {
		map.put(code, sql);
	}

	@Override
	public String get(String code) {
		JdbcProfile profile = profileRegistry.getProfile("fds");
		if (profile == null) {
			slog.error("logpresso fds: jdbc profile not found");
			return null;
		}

		String connString = profile.getConnectionString();
		if (connString.contains("oracle:"))
			return oraMap.get(code);
		else
			return mariaMap.get(code);
	}

}
