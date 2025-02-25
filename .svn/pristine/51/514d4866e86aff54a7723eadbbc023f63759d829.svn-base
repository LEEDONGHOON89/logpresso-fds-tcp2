package com.logsay.locale.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.logdb.client.LogDbClient;
import org.araqne.logdb.client.LogDbSession;
import org.araqne.logdb.client.http.WebSocketTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.logpresso.fds.api.FdsConfigService;
import com.logpresso.fds.api.model.FdsConfig;
import com.logpresso.federation.api.NodeConfig;
import com.logpresso.federation.api.NodeRegistry;
import com.logpresso.federation.api.NodeStatus;
import com.logsay.locale.LocaleSyncService;

@Component(name = "locale-sync-service")
@Provides
public class LocaleSyncServiceImpl implements LocaleSyncService {
	private Logger slog = LoggerFactory.getLogger(LocaleSyncServiceImpl.class);

	@Requires
	private NodeRegistry nodeRegistry;

	@Requires
	private FdsConfigService configService;

	private CopyOnWriteArrayList<String> nodeNames = new CopyOnWriteArrayList<String>();

	@Validate
	public void start() {
		FdsConfig config = configService.getFdsConfig();
		if (config == null)
			return;

		nodeNames = new CopyOnWriteArrayList<String>(config.getSyncNodeNames());
	}

	@Override
	public void reload(String category) {
		reload(category, new HashMap<String, Object>());
	}
	
	@Override
	public void reload(String category, Map<String, Object> m) {
		List<String> nodeNames = new ArrayList<String>(this.nodeNames);
		boolean HTTPS = true; 
		boolean IGNORE_CA = true;

		for (String nodeName : nodeNames) {
			NodeConfig node = nodeRegistry.getNode(nodeName);
			if (node == null)
				throw new IllegalStateException("node-not-found");

			NodeStatus nodeStatus = nodeRegistry.getNodeStatusByName(nodeName);
			if (nodeStatus == null) {
				slog.error("Logsay saids, unknown node [{}]", nodeName);
				continue;
			}

			if (!nodeStatus.isAlive()) {
				slog.error("Logsay saids, node [{}] down, cannot sync data [{}:{}]", new Object[] { nodeName, category, m });
				continue;
			}

			LogDbClient client = null;
			try {
				if(node.isSecure()){
					client = new LogDbClient(new WebSocketTransport(HTTPS, IGNORE_CA));
				}else{
					client = new LogDbClient();
				}
				
				client.connect(node.getAddress(), node.getPort(), node.getLoginName(), node.getPassword());
				LogDbSession session = client.getSession();

				m.put("category", category);

				session.rpc("com.logsay.locale.msgbus.LocaleSyncPlugin.reload", m);

				if (slog.isDebugEnabled())
					slog.debug("Logsay saids, logsay sync completed node [{}], category [{}:{}]", new Object[] { nodeName, category, m });
			} catch (Throwable t) {
				slog.error("Logsay saids, logsay sync failed. category [" + category + "]", t);
			} finally {
				if (client != null) {
					try {
						client.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

}
