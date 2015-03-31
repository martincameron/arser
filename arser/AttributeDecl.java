
package arser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
	An Attribute declaration for the validation engine.
*/
public class AttributeDecl {
	private static final List<String> NO_VALUES = new ArrayList<String>();

	private String name;
	private List<String> values;
	private String defaultValue;
	private boolean required;

	/**
		@param attName The case-insensitive Attribute name.
		@param attValues A list of permitted values, or null/empty if the attribute is unconstrained.
		@param attDefault A default value if the attribute is to be implied.
		@param isRequired True if the attribute may not be implied.
	*/
	public AttributeDecl( String attName, List<String> attValues, String attDefault, boolean isRequired ) {
		name = attName.toLowerCase();
		values = NO_VALUES;
		if( attValues != null && attValues.size() > 0 ) {
			values = new ArrayList<String>( attValues.size() );
			for( String attValue : attValues ) {
				values.add( attValue.toLowerCase() );
			}
		}
		defaultValue = ( attDefault == null ) ? "" : attDefault;
		required = isRequired;
	}
	
	/**
		@return The attribute name.
	*/
	public String getName() {
		return name;
	}

	/**
		@return A list containing the permitted values.
	*/
	public List<String> getValues() {
		return Collections.unmodifiableList( values );
	}

	/**
		@return Whether specified value is in the list of permitted values.
	*/
	public boolean hasValue( String value ) {
		if( !required && value.length() <= 0 ) {
			// Not required to specify a value.
			return true;
		}
		return values.contains( value.toLowerCase() );
	}
	
	/**
		@return The default value of the attribute.
	*/
	public String getDefaultValue() {
		return defaultValue;
	}
	
	/**
		@return Whether the attribute is required in the markup.
	*/
	public boolean isRequired() {
		return required;
	}

	/**
		@return Whether the attribute is constrained (ie. has a list of permitted values).
	*/
	public boolean isMultiValued() {
		return values.size() > 0;
	}

	/**
		Write the markup that represents this attribute declaration to the Writer.
	*/
	public void write( java.io.Writer writer ) throws java.io.IOException {
		writer.write( "<attribute name=" );
		writer.write( name );
		String vals = toListString( values.iterator() );
		if( vals.length() > 0 ) {
			writer.write( " values=\"" );
			writer.write( vals );
			writer.write( "\"" );
		}
		if( defaultValue.length() > 0 ) {
			writer.write( " default=\"" );
			writer.write( defaultValue );
			writer.write( "\"" );
		}
		if( required ) {
			writer.write( " required" );
		}
		writer.write( ">\n" );	
	}
	
	/* Convert the specified Strings to a comma-separated list. */
	public static String toListString( Iterator<String> iterator ) {
		StringBuilder sb = new StringBuilder();
		if( iterator.hasNext() ) {
			sb.append( iterator.next() );
		}
		while( iterator.hasNext() ) {
			sb.append( ',' );
			sb.append( iterator.next() );
		}
		return sb.toString();
	}
}
