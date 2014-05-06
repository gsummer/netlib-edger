package org.networklibrary.edger.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.networklibrary.core.config.ConfigManager;
import org.networklibrary.core.storage.MultiTxStrategy;
import org.networklibrary.core.types.EdgeData;

public class EdgeStorageEngine extends MultiTxStrategy<EdgeData> {
	protected static final Logger log = Logger.getLogger(EdgeStorageEngine.class.getName());

	private final static String MATCH = "matchid";
	private Map<String,Set<Node>> nodeCache = new HashMap<String,Set<Node>>();
	
	private int numEdges = 0;

	public EdgeStorageEngine(GraphDatabaseService graph, ConfigManager confMgr) {
		super(graph, confMgr);
	}

	@Override
	protected void doStore(EdgeData curr) {
		Set<Node> froms = getNode(curr.getFromID(),getGraph());
		Set<Node> tos = getNode(curr.getToID(),getGraph());

		if(froms == null || tos == null){
			log.warning("failed to store edge: " + curr.toString());		
			log.warning("from = " + froms + " to = " + tos);
			return;
		}

		for(Node from : froms){
			for(Node to : tos){
				Relationship r = from.createRelationshipTo(to, DynamicRelationshipType.withName(curr.getType()));
				for(Entry<String,Object> prop : curr.getProperties().entrySet()) {
					addProperty(prop.getKey(),prop.getValue(),r);
				}
				++numEdges;
			}
		}

	}

	private void addProperty(String key, Object prop, Relationship r) {
		
//		if(prop instanceof Collection<?>){
//			Collection<?> collection = (Collection<?>)prop;
//
//			Object[] arr = ((Collection) prop).toArray();
//
//			if(arr[0] instanceof String){
//				String[] values = (String[])arr;
//				r.setProperty(key, values);
//			}
//		} else {
			r.setProperty(key, prop);
//		}
	}
	
	@Override
	public void finishUp() {
		super.finishUp();
		log.info("edges stored: " + numEdges);
	}

	protected Set<Node> getNode(String name, GraphDatabaseService g){
		Set<Node> result = nodeCache.get(name);

		if(result == null){
			result = new HashSet<Node>();
			
			IndexHits<Node> hits = g.index().forNodes("matchable").get(MATCH, name);

			if(hits.size() > 1){
				log.warning("query for name = " + name + " returned more than one hit.");
			}
			
			if(hits.size() == 0){
				log.warning("could not find a hit for name = " + name);
			}

			for(Node n : hits){
				result.add(n);
			}
			hits.close();
			nodeCache.put(name,result);
		}

		return result;
	}

}
