
package arser;

import java.io.IOException;
import java.io.Writer;

/**
	An interface representing a part of a document in memory.
*/
public interface Node {
	/** Write the markup for this Node to the Writer. */
	public void write( Writer writer ) throws IOException;
}
