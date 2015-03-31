
package arser;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** Represents a document element in memory. */
public class Element implements NodeContainer {
	private Node parentNode;
	private ElementDecl elementDecl;
	private List<Attribute> attributes;
	private List<Node> children;
	private boolean hasInclusions, hasExclusions;

	/**
		Constructor for an Element with the specified element declaration,
		as a child of the specified Node (either a Document or another Element).
	*/
	public Element( ElementDecl decl, Node parent ) {
		elementDecl = decl;
		parentNode = parent;
		if( parentNode instanceof Element ) {
			Element parentElement = ( Element ) parentNode;
			hasInclusions = elementDecl.hasInclusions() || parentElement.hasInclusions;
			hasExclusions = elementDecl.hasExclusions() || parentElement.hasExclusions;
		}
		attributes = new ArrayList<Attribute>();
		children = new ArrayList<Node>();
	}

	/**
		@return the declared name of the element.
	*/
	public String getName() {
		return elementDecl.getName();
	}

	/**
		@return the element declaration.
	*/
	public ElementDecl getElementDecl() {
		return elementDecl;
	}
	
	/** @return the attribute list, which may be modified. */
	public List<Attribute> getAttributes() {
		return attributes;
	}
	
	/** @return the content of the element, which may be modified. */
	public List<Node> getChildren() {
		return children;
	}

	/**
		Validate the attribute list and occurrences of child elements.<p>
		Any optional attributes are automatically created with their
		default values.
	*/
	public void validate() throws ParseException {
		try {
			// Validate, unminimize and infer attributes.
			elementDecl.validate( attributes );
			// Ensure empty elements have no children.
			if( elementDecl.isEmpty() && children.size() > 0 ) {
				throw new ParseException( ParseException.Error.EMPTY_ELEMENT_MAY_NOT_CONTAIN_CHILDREN, getName() );
			}
			// Validate content.
			boolean mark = true;
			ContentModel model = elementDecl.getContentModel();
			for( Node node : children ) {
				if( node instanceof Element ) {
					Element element = ( Element ) node;
					String name = element.getName();
					if( isExcluded( name ) ) {
						throw new ParseException( ParseException.Error.CHILD_ELEMENT_NOT_PERMITTED, name );
					}
					if( !isIncluded( name ) ) {
						model.shift( name, mark );
						mark = false;
						if( !model.marked( true ) ) {
							throw new ParseException( ParseException.Error.CHILD_ELEMENT_NOT_PERMITTED, name );
						}
					}
					element.validate();
				}
			}
			boolean complete = ( mark && model.empty() ) || model.marked( false );
			if( !complete ) {
				throw new ParseException( ParseException.Error.ELEMENT_NOT_COMPLETE, getName() );
			}
		} catch( ParseException parseException ) {
			parseException.setLocation( getContext() );
			throw parseException;
		}
	}
	
	/** Write the markup for this element to the writer. */
	public void write( Writer writer ) throws IOException {
		writer.write( "<" );
		writer.write( elementDecl.getName() );
		for( Attribute attribute : attributes ) {
			writer.write( " " );
			writer.write( attribute.toString() );
		}
		writer.write( "\n>" );
		if( !elementDecl.isEmpty() ) {
			for( Node child : children ) {
				child.write( writer );
			}
			writer.write( "</" );
			writer.write( elementDecl.getName() );
			writer.write( "\n>" );
		}
	}
	
	/** @return a String representation of the location of this element in the document. */
	public String getContext() {
		if( parentNode instanceof Element ) {
			return ( ( Element ) parentNode ).getContext() + "<" + getName() + ">";
		}
		return "";
	}
	
	private boolean isIncluded( String name ) {
		boolean included = false;
		if( hasInclusions ) {
			included = elementDecl.hasInclusion( name );
			if( !included && parentNode instanceof Element ) {
				included = ( ( Element ) parentNode ).isIncluded( name );
			}
		}
		return included;
	}
	
	private boolean isExcluded( String name ) {
		boolean excluded = false;
		if( hasExclusions ) {
			excluded = elementDecl.hasExclusion( name );
			if( !excluded && parentNode instanceof Element ) {
				excluded = ( ( Element ) parentNode ).isExcluded( name );
			}
		}
		return excluded;
	}
}
