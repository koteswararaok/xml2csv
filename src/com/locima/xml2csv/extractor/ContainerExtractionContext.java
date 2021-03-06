package com.locima.xml2csv.extractor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.locima.xml2csv.BugException;
import com.locima.xml2csv.configuration.IMapping;
import com.locima.xml2csv.configuration.IMappingContainer;
import com.locima.xml2csv.configuration.MappingList;
import com.locima.xml2csv.configuration.XPathValue;
import com.locima.xml2csv.output.IExtractionResults;
import com.locima.xml2csv.output.IExtractionResultsContainer;
import com.locima.xml2csv.output.inline.CsiInputStream;

/**
 * Used to manage the evaluation and storage of results of an {@link MappingList} instance.
 */
public class ContainerExtractionContext extends AbstractExtractionContext implements IExtractionResultsContainer {

	private static final Logger LOG = LoggerFactory.getLogger(ContainerExtractionContext.class);

	private static final long serialVersionUID = 1L;

	/**
	 * A list of all the child contexts (also a list) found as a result of evaluating the {@link ContainerExtractionContext#mapping}'s
	 * {@link IMappingContainer#getMappingRoot()} query.
	 */
	private List<List<IExtractionResults>> children;

	/**
	 * The mapping that this extraction context is representing the evaluation of.
	 */
	private transient IMappingContainer mapping;

	/**
	 * Default no-arg constructor required for serialization.
	 */
	public ContainerExtractionContext() {
	}

	/**
	 * Constructs a new instance to manage the evaluation of the <code>mapping</code> passed.
	 *
	 * @param mapping the mapping configuration that this context is responsible for evaluating.
	 * @param parent the parent for this context (should be null if this is a top-level mapping on the configuration).
	 * @param positionRelativeToOtherRootNodes the index of the new context, with respect to its siblings (first child of the parent has index 0,
	 *            second has index 1, etc.).
	 * @param positionRelativeToIMappingSiblings The position of this extraction context with respect to its sibling {@link IMapping} instances
	 *            beneath the parent.
	 */
	public ContainerExtractionContext(IExtractionResultsContainer parent, IMappingContainer mapping, int positionRelativeToOtherRootNodes,
					int positionRelativeToIMappingSiblings) {
		super(parent, positionRelativeToOtherRootNodes, positionRelativeToIMappingSiblings);
		this.mapping = mapping;
		this.children = new ArrayList<List<IExtractionResults>>();
	}

	/**
	 * Used to construct a root instance with no parent.
	 *
	 * @param mapping the mapping configuration that this context is responsible for evaluating.
	 * @param positionRelativeToOtherRootNodes the index of the new context, with respect to its siblings (first child of the parent has index 0,
	 *            second has index 1, etc.).
	 * @param positionRelativeToIMappingSiblings The position of this extraction context with respect to its sibling {@link IMapping} instances
	 *            beneath the parent.
	 */
	public ContainerExtractionContext(MappingList mapping, int positionRelativeToOtherRootNodes, int positionRelativeToIMappingSiblings) {
		this(null, mapping, positionRelativeToOtherRootNodes, positionRelativeToIMappingSiblings);
	}

	/**
	 * Execute this mapping for the passed XML document by:
	 * <ol>
	 * <li>Getting the mapping root(s) of the mapping, relative to the rootNode passed.</li>
	 * <li>If there isn't a mapping root, use the root node passed.</li>
	 * <li>Execute this mapping for each of the root(s).</li>
	 * <li>Each execution results in a single call to om (one or more CSV records though).</li>
	 * </ol>
	 *
	 * @param rootNode the XML node against which to evaluate the mapping.
	 * @param eCtx passed down evaluation context.  May be null.  Currently unused in {@link ContainerExtractionContext}s.
	 * @throws DataExtractorException if an error occurred whilst extracting data (typically this would be caused by bad XPath, or XPath invalid from
	 *             the <code>rootNode</code> specified).
	 */
	@Override
	public void evaluate(XdmNode rootNode, EvaluationContext eCtx) throws DataExtractorException {
		XPathValue mappingRoot = this.mapping.getMappingRoot();
		// If there's no mapping root expression, use the passed node as a single root
		int rootCount = 0;
		if (mappingRoot != null) {
			LOG.debug("Executing mappingRoot {} for {}", mappingRoot, this.mapping);
			XPathSelector rootIterator = mappingRoot.evaluate(rootNode);
			for (XdmItem item : rootIterator) {
				if (item instanceof XdmNode) {
					// All evaluations have to be done in terms of nodes, so if the XPath returns something like a value then warn and move on.
					evaluateChildren((XdmNode) item, rootCount);
				} else {
					LOG.warn("Expected to find only elements after executing XPath on mapping list, got {}", item.getClass().getName());
				}
				rootCount++;
			}
		} else {
			// If there is no root specified by the contextual context, then use "." , or current node passed as rootNode parameter.
			if (LOG.isDebugEnabled()) {
				LOG.debug("No mapping root specified for {}, so executing against passed context node", mappingRoot, this.mapping);
			}
			evaluateChildren(rootNode, rootCount);
			rootCount = 1;
		}

		// Keep track of the most number of results we've found for a single invocation of the mapping root.
		getMapping().setHighestFoundValueCount(rootCount);
	}

	/**
	 * Evaluates a nested mapping, appending the results to the output line passed.
	 *
	 * @param node the node from which all mappings will be based on.
	 * @param positionRelativeToOtherRootNodes the position of this set of children relative to all the other roots found by this container's
	 *            evaluation of the mapping root.
	 * @throws DataExtractorException if an error occurred whilst extracting data (typically this would be caused by bad XPath, or XPath invalid from
	 *             the <code>mappingRoot</code> specified).
	 */
	private void evaluateChildren(XdmNode node, int positionRelativeToOtherRootNodes) throws DataExtractorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing {} child mappings of {}", this.mapping.size(), this.mapping);
		}
		int positionRelativeToIMappingSiblings = 0;
		List<IExtractionResults> iterationECs = new ArrayList<IExtractionResults>(size());
		
		EvaluationContext childECtx = new EvaluationContext();
		
		for (IMapping childMapping : this.mapping) {
			IExtractionContext childCtx =
							AbstractExtractionContext.create(this, childMapping, positionRelativeToOtherRootNodes, positionRelativeToIMappingSiblings);
			childCtx.evaluate(node, childECtx);

			// Only add a CEC or MEC to the collection if it's not empty.
			if (childCtx.size() > 0) {
				iterationECs.add(childCtx);
			}
			positionRelativeToIMappingSiblings++;
		}
		this.children.add(iterationECs);
	}

	@Override
	public List<List<IExtractionResults>> getChildren() {
		return this.children;
	}

	@Override
	public IMapping getMapping() {
		return this.mapping;
	}

	@Override
	public IMappingContainer getMappingContainer() {
		return this.mapping;
	}

	@Override
	public String getName() {
		return this.mapping.getName();
	}

	@Override
	public List<IExtractionResults> getResultsSetAt(int valueIndex) {
		return (this.children.size() > valueIndex) ? this.children.get(valueIndex) : null;
	}

	/**
	 * Overridden to manage not writing {@link #mapping} to the output stream.
	 *
	 * @param rawInputStream target stream for serialized state.
	 * @throws IOException if any issues occur during writing.
	 * @throws ClassNotFoundException if an unexpected class instance appears in the CSI file, should never, ever happen.
	 */
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream rawInputStream) throws IOException, ClassNotFoundException {
		if (!(rawInputStream instanceof CsiInputStream)) {
			throw new BugException("Bug found when deserializing PEC.  I've been given a %s instead of a CsiInputStream", rawInputStream.getClass());
		}
		CsiInputStream stream = (CsiInputStream) rawInputStream;
		Object readObject = stream.readObject();
		try {
			this.children = (ArrayList<List<IExtractionResults>>) readObject;
		} catch (ClassCastException cce) {
			throw new IOException("Unexpected object type found in stream.  Expected List<List<IExtractionResults>> but got "
							+ readObject.getClass().getName());
		}
		readObject = stream.readObject();
		String mappingName;
		try {
			mappingName = (String) readObject;
		} catch (ClassCastException cce) {
			throw new IOException("Unexpected object type found in stream.  Expected String but got " + readObject.getClass().getName());
		}
		this.mapping = stream.getMappingContainer(mappingName);
		if (this.mapping == null) {
			throw new IOException("Unable to restore link to IMappingContainer " + mappingName + " as it was not found");
		}
	}

	/**
	 * Returns the number of mapping roots found for this object to evaluate against.
	 *
	 * @return the number of mapping roots found for this object to evaluate against.
	 */
	@Override
	public int size() {
		return this.children.size();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("CEC(");
		sb.append(this.mapping);
		sb.append(", ");
		sb.append(getPositionRelativeToIMappingSiblings());
		sb.append(", ");
		sb.append(getPositionRelativeToOtherRootNodes());
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Overridden to manage not writing {@link #mapping} to the output stream.
	 *
	 * @param stream target stream for serialized state.
	 * @throws IOException if any issues occur during writing.
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		String mappingName = getMappingContainer().getName();
		int size = this.children.size();
		if (LOG.isInfoEnabled()) {
			LOG.info("Writing {} results to the CSI file for {}", size, mappingName);
		}
		stream.writeObject(this.children);
		stream.writeObject(mappingName);
	}

}
