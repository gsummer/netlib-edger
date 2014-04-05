package org.networklibrary.edger.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.networklibrary.core.storage.StorageEngine;
import org.networklibrary.core.types.EdgeData;

public class DefaultEdgeStorageEngine implements StorageEngine<EdgeData> {

	public static String MATCH = "matchid";
	
	private GraphDatabaseService graph = null;
	
	private Map<String,Node> nodeCache = new HashMap<String,Node>();
	
	public DefaultEdgeStorageEngine(String db) {
		graph = new RestGraphDatabase(db);
	}

	public void store(EdgeData curr) {
		
		Node from = getNode(curr.getFromID(),graph);
		Node to = getNode(curr.getToID(),graph);
		
		if(from == null || to == null){
			// error condition
			return;
		}
		
		try ( Transaction tx = graph.beginTx() ){
			Relationship r = from.createRelationshipTo(to, DynamicRelationshipType.withName(curr.getType()));
			for(Entry<String,Object> prop : curr.getProperties().entrySet()) {
				r.setProperty(prop.getKey(), prop.getValue());
			}

			tx.success();
		}
	}

	public void finishUp() {		
	}

	public void storeAll(Collection<EdgeData> parse) {
		for(EdgeData ed : parse){
			store(ed);
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
