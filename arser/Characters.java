
package arser;

import java.io.IOException;
import java.io.Writer;

/**
	Represents character data within a document in memory.
*/
public class Characters implements Node {
	private String param, characters;

	/**
		Constructor for plain character data.
	*/
	public Characters( String value ) {
		setValue( value );
	}
	
	/**
		Constructor for parameterized, or marked character data.
	*/
	public Characters( String parameter, String value ) {
		setParameter( parameter );
		setValue( value );
	}
	
	public String getParameter() {
		return param;
	}
	
	public void setParameter( String parameter ) {
		param = parameter;
	}
	
	public String getValue() {
		return characters;
	}
	
	public void setValue( String value ) {
		characters = value;
	}

	public void write( Writer writer ) throws IOException {
		if( param == null ) {
			writer.write( characters );
		} else {
			writer.write( "<![ " + param + " [" + characters + "]]\n>" );
		}
	}
}
