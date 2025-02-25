package com.logsay.locale;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.logsay.locale.model.Locale;

public interface LocaleManager {

	Locale getLocale(String ip);

	List<Map<String, Locale>> getLocales();

	void reloadLocale(boolean isSync);

	void deleteLocale(String guid);

	void addLocale(Map<String, String> m) throws ParseException;

	String localeMatcher(String ip);
}
