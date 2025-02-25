package com.logsay.locale;

import java.util.Map;

public interface LocaleSyncService {

	void reload(String category);
	
	void reload(String category, Map<String, Object> m);

}
