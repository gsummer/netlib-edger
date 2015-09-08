package org.networklibrary.edger.config;

import org.networklibrary.core.config.ConfigManager;

public class EdgerConfigManager extends ConfigManager implements EdgerSettings {

	public EdgerConfigManager(String type, String dictionary, boolean newNodes) {
		
		setType(type);
		setNewNodes(newNodes);
		
		setDictionaryKey(dictionary);
		
		load(null);
	}

	@Override
	public boolean newNodes() {
		return getConfig().getBoolean("new_nodes");
	}

	@Override
	public String getType() {
		return getConfig().getString("type");
	}
	
	protected void setNewNodes(boolean newNodes){
		getConfig().addProperty("new_nodes", newNodes);
	}
	
	protected void setType(String type){
		getConfig().addProperty("type", type);
	}
	
	private void setDictionaryKey(String dictionary) {
		getConfig().addProperty(DICTIONARY_KEY, dictionary);
	}

}
