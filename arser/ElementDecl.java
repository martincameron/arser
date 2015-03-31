
package arser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
	Represents an element declaration used by the validation engine.<p>
	An element declaration may specify a ContentModel to determine the permitted
	child elements and their order.<p>
	A list of included elements may also be specified. If an element is included,
	it is permitted as a child of this element and also its children, even if it
	is not permitted by the content model. Inclusions are not submitted to the
	content model and therefore should not be used as required elements in models.<p>
	Exclusions are essentially the opposite of inclusions, and an excluded element
	is not permitted as a child of this element or any of its children.<p>
	If an element declaration is specified as empty, no close tag is expected for it.<p>
	An element declaration may also specify if it is to permit the insertion of a
	close tag for it if a child element is not permitted. This allows for the omission
	of close tags in the markup in certain circumstances.
*/
public class ElementDecl {
	private static final List<AttributeDecl> NO_DECLS = new ArrayList<AttributeDecl>();
	private static final List<String> NO_VALUES = new ArrayList<String>();
	private static final ContentModel EMPTY_MODEL = new EmptyModel();

	private String name;
	private List<AttributeDecl> attributeDecls;
	private ContentModel contentModel;
	private List<String> inclusions, exclusions;
	private boolean empty, omit;

	/**
		@param elementName The name of the element.
		@param decls The list of declared attributes.
		@param model The content model for child elements, or null.
		@param include The list of elements that may be included as children.
		@param exclude The list of elements that are excluded as children.
		@param isEmpty Whether the tag does not have any content at all.
		@param mayOmit If true, the element permits the validator to close it automatically.
	*/
	public ElementDecl( String elementName, List<AttributeDecl> decls,
			ContentModel model, List<String> include, List<String> exclude,
			boolean isEmpty, boolean mayOmit ) {
		name = elementName.toLowerCase();
		attributeDecls = NO_DECLS;
		if( decls != null && decls.size() > 0 ) {
			attributeDecls = new ArrayList<AttributeDecl>( decls );
		}
		contentModel = EMPTY_MODEL;
		if( model != null && !isEmpty ) {
			contentModel = model.copy();
		}
		inclusions = NO_VALUES;
		if( include != null && include.size() > 0 ) {
			inclusions = new ArrayList<String>( include.size() );
			for( String inclusion : include ) {
				inclusions.add( inclusion.toLowerCase() );
			}
		}
		exclusions = NO_VALUES;
		if( exclude != null && exclude.size() > 0 ) {
			exclusions = new ArrayList<String>( exclude.size() );
			for( String exclusion : exclude ) {
				exclusions.add( exclusion.toLowerCase() );
			}
		}
		empty = isEmpty;
		omit = mayOmit;
	}

	/**
		@return the element name.
	*/
	public String getName() {
		return name;
	}

	/**
		@return the list of declared attributes.
	*/
	public List<AttributeDecl> getAttributeDecls() {
		return Collections.unmodifiableList( attributeDecls );
	}
	
	/**
		@return the content model for the element.
	*/
	public ContentModel getContentModel() {
		return contentModel.copy();
	}
	
	/**
		@return the list of included elements.
	*/
	public List<String> getInclusions() {
		return Collections.unmodifiableList( inclusions );
	}
	
	/**
		@return true if the specified name is in the list of inclusions.
	*/
	public boolean hasInclusion( String name ) {
		return inclusions.contains( name.toLowerCase() );
	}
	
	/** @return true if the element declaration has inclusions. */
	public boolean hasInclusions() {
		return inclusions.size() > 0;
	}
	
	/**
		@return the list of excluded elements.
	*/
	public List<String> getExclusions() {
		return Collections.unmodifiableList( exclusions );
	}
	
	/**
		@return true if the specified name is in the list of exclusions.
	*/
	public boolean hasExclusion( String name ) {
		return exclusions.contains( name.toLowerCase() );
	}

	/** @return true if the element declaration has exclusions. */
	public boolean hasExclusions() {
		return exclusions.size() > 0;
	}

	/**
		@return true if the element is declared as empty.
	*/
	public boolean isEmpty() {
		return empty;
	}
	
	/**
		@return true if the element is declared as may omit end tags.
	*/
	public boolean mayOmit() {
		return omit;
	}

	/** Validate an unminimize the specified Attribute list against this declaration. */
	public void validate( List<Attribute> attributes ) throws ParseException {
		for( AttributeDecl attributeDecl : attributeDecls ) {
			Attribute attribute = null;
			// Find the Attribute for the AttributeDecl.
			String declName = attributeDecl.getName();
			ListIterator<Attribute> attIterator = attributes.listIterator();
			while( attIterator.hasNext() ) {
				Attribute att = attIterator.next();
				String attName = att.getName();
				if( attName == null && attributeDecl.hasValue( att.getValue() ) ) {
					// Unminimize.
					attName = attributeDecl.getName();
				}
				if( declName.equals( attName ) ) {
					// Attribute found.
					if( attribute != null ) {
						throw new ParseException( ParseException.Error.DUPLICATE_ATTRIBUTE, declName );
					}
					attribute = new Attribute( attributeDecl, att.getValue() );
					attIterator.set( attribute );
				}
			}
			if( attribute == null ) {
				// Create "implied" Attribute.
				if( attributeDecl.isRequired() ) {
					throw new ParseException( ParseException.Error.REQUIRED_ATTRIBUTE_MISSING, declName );
				}
				attribute = new Attribute( attributeDecl, attributeDecl.getDefaultValue() );
				attributes.add( attribute );
			}
			if( !attribute.isValid() ) {
				// Attribute value is not in the list of allowed values.
				throw new ParseException( ParseException.Error.ATTRIBUTE_VALUE_NOT_PERMITTED,
					attribute.toString() );
			}
		}
		for( Attribute attribute : attributes ) {
			if( attribute.getAttributeDecl() == null ) {
				throw new ParseException( ParseException.Error.UNDECLARED_ATTRIBUTE, attribute.toString() );
			}
		}
	}
	
	/** Write the markup that represents this element declaration to the specified Writer. */
	public void write( java.io.Writer writer ) throws java.io.IOException {
		writer.write( "<element name=" );
		writer.write( name );
		String model = contentModel.toString();
		if( model.length() > 0 ) writer.write( " content=" + model );
		String incl = AttributeDecl.toListString( inclusions.iterator() );
		if( incl.length() > 0 ) {
			writer.write( " include=" );
			writer.write( incl );
		}
		String excl = AttributeDecl.toListString( exclusions.iterator() );
		if( excl.length() > 0 ) {
			writer.write( " exclude=" );
			writer.write( excl );
		}
		if( empty )
			writer.write( " empty" );
		else if( omit )
			writer.write( " omit" );
		writer.write( ">\n" );
		if( attributeDecls != null ) {
			for( AttributeDecl attributeDecl : attributeDecls ) {
				writer.write( '\t' );
				attributeDecl.write( writer );
			}
		}
	}
}
