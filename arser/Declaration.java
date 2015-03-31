
package arser;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
	Represents a generalized declaration in the markup,
	such an SGML <!DOCTYPE>, containing a nested subset of declarations.
*/
public class Declaration implements Node {
	private String name;
	private List<String> params = new ArrayList<String>();
	private List<Declaration> subset = new ArrayList<Declaration>();
	
	/**
		@param declName The name of the declaration.
	*/
	public Declaration( String declName ) {
		name = declName;
	}

	/**
		@param declName The name of the declaration.
		@param declParams The list of parameters following the declaration name.
		@param declSubset A list of Declarations to be nested within this one.
	*/
	public Declaration( String declName, List<String> declParams, List<Declaration> declSubset ) {
		this( declName );
		if( declParams != null && declParams.size() > 0 ) {
			params.addAll( declParams );
		}
		if( declSubset != null && declSubset.size() > 0 ) {
			subset.addAll( declSubset );
		}
	}
	
	/**
		@return The declaration name.
	*/
	public String getName() {
		return name;
	}
	
	/**
		Quotes around parameters are left intact for consistency,
		and may be removed using the unQuote() method.
		@return The list of parameters.
	*/
	public List<String> getParams() {
		return params;
	}
	
	/**
		@return The list of sub-declarations.
	*/
	public List<Declaration> getSubset() {
		return subset;
	}
	
	/**
		Write the markup that represents this Declaration to the Writer.
	*/
	public void write( Writer writer ) throws IOException {
		writer.write( "<!" );
		writer.write( name );
		for( String param : params ) {
			writer.write( ' ' );
			writer.write( param );
		}
		if( subset.size() > 0 ) {
			writer.write( "\n[" );
			for( Declaration decl : subset ) {
				decl.write( writer );
			}
			writer.write( "]\n" );
		}
		writer.write( ">" );
	}

	/* Remove quotes from around a String, if necessary. */
	public static String unQuote( String string ) {
		if( string.length() > 0 ) {
			char chr = string.charAt( 0 );
			if( chr == '\'' || chr == '"' ) {
				string = string.substring( 1, string.length() - 1 );
			}
		}
		return string;
	}
}
