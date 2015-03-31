
package arser;

import java.io.IOException;
import java.io.Writer;

/**
	Represents a processing instruction within a document in memory.
*/
public class ProcessingInstruction implements Node {
	private String value;

	public ProcessingInstruction( String instruction ) {
		setValue( instruction );
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue( String instruction ) {
		value = instruction;
	}

	public void write( Writer writer ) throws IOException {
		writer.write( "<?" + value + ">" );
	}
}
