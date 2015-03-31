
package arser;

import java.io.IOException;
import java.io.Writer;

/**
	Represents an entity-reference within a document in memory.
*/
public class Entity implements Node {
	private String value;

	public Entity( String value ) {
		setValue( value );
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue( String entity ) {
		value = entity;
	}

	public void write( Writer writer ) throws IOException {
		writer.write( "&" + value + ";" );
	}
}
