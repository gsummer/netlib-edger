package org.networklibrary.edger.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.networklibrary.core.config.ConfigManager;
import org.networklibrary.core.storage.MultiTxStrategy;
import org.networklibrary.core.types.EdgeData;

public class EdgeStorageEngine extends MultiTxStrategy<EdgeData> {
	private final static String MATCH = "matchid";
	private Map<String,Node> nodeCache = new HashMap<String,Node>();
	
	public EdgeStorageEngine(GraphDatabaseService graph, ConfigManager confMgr) {
		super(graph, confMgr);
	}

	@Override
	protected void doStore(EdgeData curr) {
		Node from = getNode(curr.getFromID(),getGraph());
		Node to = getNode(curr.getToID(),getGraph());

		if(from == null || to == null){
			// error condition
			return;
		}

		Relationship r = from.createRelationshipTo(to, DynamicRelationshipType.withName(curr.getType()));
		for(Entry<String,Object> prop : curr.getProperties().entrySet()) {
			r.setProperty(prop.getKey(), prop.getValue());
		}

	}

	protected Node getNode(String name, GraphDatabaseService g){
		Node result = nodeCache.get(name);

		if(result == null){
			IndexHits<Node> hits = g.index().forNodes("matchable").get(MATCH, name);

			if(hits.size() > 1){
				// error condition
			}

			result = hits.getSingle();
			nodeCache.put(name,result);
		}

		return result;
	}

}
