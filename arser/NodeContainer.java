
package arser;

import java.util.List;

/**
	An interface representing a Node that may contain other Nodes.
*/
public interface NodeContainer extends Node {
	/** @return the children of this Node. */
	public List<Node> getChildren();
}
