
package arser;

/**
	Represents a single attribute of an element.<p>
	The parser may generate an Attribute with an empty
	name if it is minimized in the markup.
*/
public class Attribute {
	private AttributeDecl decl;
	private String name, value;
	
	/** Constructor for an untyped, minimized attribute. */
	public Attribute( String attributeValue ) {
		value = attributeValue.toString();
	}
	
	/** Constructor for an untyped attribute. */
	public Attribute( String attributeName, String attributeValue ) {
		name = attributeName.toLowerCase();
		value = attributeValue.toString();
	}
	
	/** Constructor for a typed attribute. */
	public Attribute( AttributeDecl attributeDecl, String attributeValue ) {
		decl = attributeDecl;
		name = attributeDecl.getName();
		value = attributeValue.toString();
	}

	/** @return the name of the attribute. */
	public String getName() {
		return name;
	}
	
	/** @return the attribute value. */
	public String getValue() {
		return value;
	}
	
	/** @return the declaration for the attribute, or null if the attribute is untyped. */
	public AttributeDecl getAttributeDecl() {
		return decl;
	}

	/** Check the attribute value against those permitted by the declaration. */
	public boolean isValid() {
		if( decl != null && decl.isMultiValued() ) {
			return decl.hasValue( value );
		}
		return true;
	}

	/** Set the attribute value. */
	public void setValue( String attributeValue ) {
		value = attributeValue.toString();
	}

	public String toString() {
		String str = value;
		if( name != null ) {
			char delim = '"';
			for( int idx = 0, end = value.length(); idx < end; idx++ ) {
				if( value.charAt( idx ) == '"' ) {
					delim = '\'';
					break;
				}
			}
			str = name + '=' + delim + value + delim;
		}
		return str;
	}
	
	/** @return the value of the Attribute with the specified name within the specified Collection. */
	public static String getValue( java.util.Collection<Attribute> attributes, String name ) {
		name = name.toLowerCase();
		for( Attribute attribute : attributes ) {
			if( name.equals( attribute.name ) ) {
				return attribute.value;
			}
		}
		return null;
	}
}
