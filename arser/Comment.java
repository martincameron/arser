
package arser;

import java.io.IOException;
import java.io.Writer;

/**
	Represents a comment within a document in memory.
*/
public class Comment implements Node {
	private String value;

	public Comment( String comment ) {
		setValue( comment );
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue( String comment ) {
		value = comment;
	}

	public void write( Writer writer ) throws IOException {
		writer.write( "<!--" + value + "--\n>" );
	}
}
