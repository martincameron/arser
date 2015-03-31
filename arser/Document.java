
package arser;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
	An instance of this class represents a document in memory.
	The Loader class is able to create Document instances from markup.
*/
public class Document implements NodeContainer {
	private Doctype doctype;
	private List<Node> children = new ArrayList<Node>();

	public Doctype getDoctype() {
		return doctype;
	}
	
	public void setDoctype( Doctype dt ) {
		doctype = dt;
	}

	public List<Node> getChildren() {
		return children;
	}

	/** Validate the document. */
	public void validate() throws ParseException {
		for( Node child : children ) {
			if( child instanceof Element ) {
				Element element = ( Element ) child;
				String name = element.getName();
				if( doctype == null ) {
					throw new ParseException( ParseException.Error.DOCTYPE_NOT_SET, name );
				}
				if( doctype.getElementDecl( name ) == null ) {
					throw new ParseException( ParseException.Error.ELEMENT_NOT_DECLARED, name );
				}
				element.validate();
			}
		}
	}

	public void write( Writer writer ) throws IOException {
		for( Node child : children ) {
			child.write( writer );
		}
	}
}
