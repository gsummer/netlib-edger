package org.networklibrary.edger.parsing.gpml;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.networklibrary.core.config.Dictionary;
import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.core.types.EdgeTypes;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.GroupStyle;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.MPoint;
import org.pathvisio.core.util.Relation;

public class GpmlParser implements Parser<EdgeData> {

	protected static final Logger log = Logger.getLogger(GpmlParser.class.getName());

	private Pathway currPathway = null;
	private Iterator<PathwayElement> peIter = null;

	private Collection<InteractionType> ignoreInteractions = new HashSet<InteractionType>();
	private Map<InteractionType, String> edgeTypes;
	private Dictionary dictionary = null;
	
	public GpmlParser() {
		edgeTypes = new HashMap<InteractionType, String>();
		edgeTypes.put(InteractionType.BINDING, EdgeTypes.INTERACTS_WITH);
		edgeTypes.put(InteractionType.CLEAVAGE, EdgeTypes.MODIFIES_PROTEIN);
		edgeTypes.put(InteractionType.CO_CONTROL, EdgeTypes.CO_CONTROLS_INTERACTION);
		edgeTypes.put(InteractionType.COMPOSED_OF, EdgeTypes.IN_SAME_COMPONENT);
		edgeTypes.put(InteractionType.IN_GROUP, EdgeTypes.IN_GROUP);
		edgeTypes.put(InteractionType.IN_SAME_COMPONENT, EdgeTypes.IN_SAME_COMPONENT);
		edgeTypes.put(InteractionType.INHIBITION, EdgeTypes.INHIBITS);
		edgeTypes.put(InteractionType.INTERACTS_WITH, EdgeTypes.INTERACTS_WITH);
		edgeTypes.put(InteractionType.MEDIATES_INTERACTION, EdgeTypes.MEDIATES_INTERACTION);
		edgeTypes.put(InteractionType.METABOLIC_CATALYSIS, EdgeTypes.CATALYZES);
		edgeTypes.put(InteractionType.NEGATIVE_CORRELATION, EdgeTypes.CORRELATES_NEGATIVE);
		edgeTypes.put(InteractionType.POSITIVE_CORRELATION, EdgeTypes.CORRELATES_POSITIVE);
		edgeTypes.put(InteractionType.REACTS_WITH, EdgeTypes.CO_CONTROLS_INTERACTION);
		edgeTypes.put(InteractionType.RECEPTOR_BINDING, EdgeTypes.BINDS_RECEPTOR);
		edgeTypes.put(InteractionType.STATE_CHANGE, EdgeTypes.MODIFIES_PROTEIN);
		edgeTypes.put(InteractionType.STIMULATION, EdgeTypes.ACTIVATES);
		edgeTypes.put(InteractionType.TRANSPORT, EdgeTypes.TRANSPORTS);
	}

	@Override
	public void setDataSource(String location) throws ParsingErrorException {
		try {
			currPathway = new Pathway();
			currPathway.readFromXml(new File(location), true);
			peIter = currPathway.getDataObjects().iterator();

		} catch (ConverterException e) {
			currPathway = null;
			e.printStackTrace();
			throw new ParsingErrorException("Errors while converting the pathway: "+ location, e);
		}
	}

	@Override
	public boolean ready() throws ParsingErrorException {
		return currPathway != null && peIter.hasNext();
	}

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		Set<EdgeData> addedEdges = new HashSet<EdgeData>();

		PathwayElement pe =  peIter.next();
		//Store symbol
		//			if(pe.getObjectType() == ObjectType.DATANODE) {
		//				
		//				// ?!?! need the symbol / id of the node (no need to map I can deal with most
		//				//awesome error!
		//				symbols.put(pe.getXref().toString(), pe.getTextLabel());
		//			}

		//Process groups and complexes
		if(pe.getObjectType() == ObjectType.GROUP) {
			// in_group
			// in_same_component
			InteractionType interaction = pe.getGroupStyle().equals(GroupStyle.COMPLEX) ? 
					InteractionType.IN_SAME_COMPONENT : InteractionType.IN_GROUP;

			if(ignoreInteractions.contains(interaction)) return addedEdges;

			addedEdges.addAll(addUndirectedCombinations(
					currPathway.getGroupElements(pe.getGroupId()),
					currPathway.getGroupElements(pe.getGroupId()),
					interaction, pe.getGraphId()
					));
		}
		//
		//			//Process relations
		if (isRelation(pe)) {
			Relation r = new Relation(pe);

			TypeFromStyle type = new TypeFromStyle(pe);

			Set<PathwayElement> leftright = new HashSet<PathwayElement>(); // Both input and output
			leftright.addAll(r.getLefts());
			leftright.addAll(r.getRights());

			// Convert to simple binary interactions using the
			// PathwayCommons SIF mappings
			// Try to derive meaning from template style
			boolean iIsMetabolite = isAllType(r.getLefts(),
					DataNodeType.METABOLITE);
			boolean oIsMetabolite = isAllType(r.getRights(),
					DataNodeType.METABOLITE);


			//Process relations

			// metabolic_catalysis
			// Make sure there is at least one mediator
			// Make sure that input/output are metabolites
			if(r.getMediators().size() > 0) {
				InteractionType interaction = InteractionType.METABOLIC_CATALYSIS;

				for (PathwayElement m : r.getMediators()) {
					if(!iIsMetabolite && !oIsMetabolite) {
						interaction = InteractionType.MEDIATES_INTERACTION;
						InteractionType derived = new TypeFromStyle(r.getMediatorLine(m)).getType();
						log.info(pe.getGraphId() + ": " + derived);
						if(derived != null) interaction = derived;
					}
					if(ignoreInteractions.contains(interaction)) return addedEdges;

					for(PathwayElement io : leftright) {
						addedEdges.addAll(addInteraction(m, io, interaction, true, pe.getGraphId()));
					}
				}
			}

			// reacts_with
			if (r.getLefts().size() > 0 && r.getRights().size() > 0 && iIsMetabolite
					&& oIsMetabolite) {
				InteractionType interaction = InteractionType.REACTS_WITH;
				if(ignoreInteractions.contains(interaction)) return addedEdges;

				addedEdges.addAll(addUndirectedCombinations(
						leftright,
						leftright,
						interaction, pe.getGraphId()
						));
			}

			// co_control
			if (r.getMediators().size() > 0 && iIsMetabolite && oIsMetabolite) {
				InteractionType interaction = InteractionType.CO_CONTROL;
				if(ignoreInteractions.contains(interaction)) return addedEdges;
				addedEdges.addAll(addUndirectedCombinations(
						r.getMediators(),
						r.getMediators(),
						interaction, pe.getGraphId()
						));
			}

			// interacts_with
			if (r.getLefts().size() > 0 && r.getRights().size() > 0 && !iIsMetabolite
					&& !oIsMetabolite && !type.isTransport()) {
				InteractionType interaction = InteractionType.INTERACTS_WITH;
				//Try to derive more specific type from style
				InteractionType derived = type.getType();
				if(derived != null) interaction = derived;

				boolean directed = false;
				Set<PathwayElement> lefts = r.getLefts();
				Set<PathwayElement> rights = r.getRights();
				if(type.isDirectedForward()) {
					directed = true;
				} else if(type.isDirectedBackward()) {
					directed = true;
					lefts = r.getRights();
					rights = r.getLefts();
				}
				if(ignoreInteractions.contains(interaction)) return addedEdges;
				for(PathwayElement p1 : lefts) {
					for(PathwayElement p2 : rights) {
						addedEdges.addAll(addInteraction(p1, p2, interaction, directed, pe.getGraphId()));
					}
				}
			}

			// transport
			if(r.getLefts().size() > 0 && r.getRights().size() > 0 && type.isTransport()) {
				InteractionType interaction = InteractionType.TRANSPORT;
				if(ignoreInteractions.contains(interaction)) return addedEdges;

				for(PathwayElement p1 : r.getLefts()) {
					for(PathwayElement p2 : r.getRights()) {
						//Only apply transport if start and end element are actually the same thing
						boolean equal = 
								(p1.getGeneID().equals(p2.getGeneID())) ||
								p1.getTextLabel().equals(p2.getTextLabel());

						if(equal) addedEdges.addAll(addInteraction(p1, p2, interaction, false, pe.getGraphId()));
						else {
							log.warning(
									"Ignored invalid transport (start and end not same): " 
											+ p1.getTextLabel() + " -> " + p2.getTextLabel()
											+ ", " + pe.getGraphId() + "@" 
											+ pe.getParent().getMappInfo().getDynamicProperty("pathwayId")
									);
						}
					}
				}
			}

			// state_change
			if (r.getLefts().size() == 1 && r.getRights().size() == 1 && r.getMediators().size() > 0) {
				InteractionType interaction = InteractionType.STATE_CHANGE;
				if(ignoreInteractions.contains(interaction)) return addedEdges;

				PathwayElement pi = r.getLefts().iterator().next();
				PathwayElement po = r.getRights().iterator().next();
				String idi = pickID(pi);
				String ido = pickID(po);

				if (idi.equals(ido)) {
					for(PathwayElement m : r.getMediators()) {
						addedEdges.addAll(addInteraction(m, pi, interaction, true, pe.getGraphId()));
					}
				}
			}
		}


		return addedEdges;
	}

	@Override
	public boolean hasExtraParameters() {
		return false;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
	}


	private boolean isAllType(Collection<PathwayElement> elms,
			DataNodeType type) {
		boolean isAllType = true;
		for (PathwayElement pe : elms) {
			if (!type.getName().equals(pe.getDataNodeType())) {
				isAllType = false;
				break;
			}
		}
		return isAllType;
	}

	private String pickID(PathwayElement pe){
		return (pe.getGeneID() != null && !pe.getGeneID().isEmpty()) ? pe.getGeneID() : pe.getTextLabel();
	}
	private Set<EdgeData> addUndirectedCombinations(Set<PathwayElement> s1, Set<PathwayElement> s2, InteractionType interaction, String graphId) {
		Set<String> addedIds = new HashSet<String>();
		Set<EdgeData> addedEdges = new HashSet<EdgeData>();

		for(PathwayElement e1 : s1) {
			for(PathwayElement e2 : s2) {
				if(e1 == e2) continue;
				//Check if interaction was already added in other direction
				if(addedIds.contains(e2.getGraphId() + "-" + e1.getGraphId())) continue;

				addedIds.add(e1.getGraphId() + "-" + e2.getGraphId());
				addedEdges.addAll(addInteraction(e1, e2, interaction, false, graphId));
			}
		}
		return addedEdges;
	}

	private String getEdgeType(InteractionType interactionType) {
		String type = edgeTypes.get(interactionType);
		if(type == null) {
			type = interactionType.toString();
		}
		return type;
	}
	
	// ah directionality... the old problem how to treat this properly?
	private Set<EdgeData> addInteraction(PathwayElement p1, PathwayElement p2, InteractionType interaction, boolean directed, String graphId) {
		Set<EdgeData> edges = new HashSet<EdgeData>();

		String from = p1.getElementID();
		String to = p2.getElementID();

		Map<String, Object> props = new HashMap<String,Object>();
		if(graphId != null && !graphId.isEmpty())
			props.put("graphid",graphId);
		
		String pathwayId = currPathway.getMappInfo().getDynamicProperty("pathwayId");
		
		props.put("data_source", "gpml");
		
		if(pathwayId != null && !pathwayId.isEmpty())
			props.put("data_source_pathway", currPathway.getMappInfo().getDynamicProperty("pathwayId"));

		props.put("original_type", interaction.name());
		edges.add(new EdgeData(from, to, getEdgeType(interaction), props));
		//		edges.add(new EdgeData(to, from, interaction.name(), props));

		return edges;
	}

	private boolean isRelation(PathwayElement pe) {
		if (pe.getObjectType() == ObjectType.LINE) {
			MPoint s = pe.getMStart();
			MPoint e = pe.getMEnd();
			if (s.isLinked() && e.isLinked()) {
				// Objects behind graphrefs should be PathwayElement
				// so not MAnchor
				if (pe.getParent().getElementById(s.getGraphRef()) != null
						&& pe.getParent().getElementById(e.getGraphRef()) != null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void setDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;
	}
}
