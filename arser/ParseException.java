
package arser;

/**
	An Exception that may be thrown during parsing or validation.
*/
public class ParseException extends Exception {
	public enum Error {
		INVALID_TAG_NAME( "Invalid tag name." ),
		EXPECTED_LT_HERE( "Expected '<' here." ),
		EXPECTED_GT_HERE( "Expected '>' here." ),
		EXPECTED_EX_OR_QM_HERE( "Expected '!' or '?' here." ),
		EXPECTED_LT_OR_CB_HERE( "Expected '<' or ']' here." ),
		EXPECTED_LT_OR_GT_HERE( "Expected '<' or '>' here." ),
		INVALID_DECLARATION( "Invalid declaration." ),
		EXPECTED_OB_HERE( "Expected '[' here." ),
		EXPECTED_OP_HERE( "Expected '(' here." ),
		EXPECTED_HY_HERE( "Expected '-' here." ),
		TOKEN_TOO_LONG( "Token too long." ),
		UNEXPECTED_END_OF_FILE( "Unexpected end of file." ),
		UNEXPECTED_END_OF_EXPR( "Unexpected end of expression." ),
		ZERO_LENGTH_TOKEN_IN_EXPR( "Zero-length token in content model expression." ),
		UNEXPECTED_CHAR_IN_EXPR( "Unexpected character in content model expression." ),
		MALFORMED_LIST( "Malformed comma-separated list." ),
		CHILD_ELEMENT_NOT_PERMITTED( "Child element not permitted." ),
		ELEMENT_NOT_COMPLETE( "Element not complete." ),
		DUPLICATE_ATTRIBUTE( "Duplicate attribute." ),
		REQUIRED_ATTRIBUTE_MISSING( "Required attribute missing." ),
		ATTRIBUTE_VALUE_NOT_PERMITTED( "Attribute value not permitted." ),
		UNDECLARED_ATTRIBUTE( "Undeclared attribute." ),
		DOCTYPE_NOT_SET( "Doctype not set." ),
		ELEMENT_NOT_DECLARED( "Element not declared." ),
		ELEMENT_NOT_PERMITTED( "Element not permitted." ),
		UNEXPECTED_CLOSE_TAG( "Unexpected close tag." ),
		CLOSE_ELEMENT_NOT_PERMITTED( "Close element not permitted." ),
		CLOSE_ELEMENT_MISSING( "Close element missing." ),
		EMPTY_ELEMENT_MAY_NOT_CONTAIN_CHILDREN( "Empty element may not contain children." ),
		OTHER( "Other error." );
		private String name;
		private Error( String name ) { this.name = name; }
		public String toString() { return name; }
	}

	private Error error = Error.OTHER;
	private String item = "", location = "";
	private int line;
		
	public ParseException( Error error ) {
		if( error != null ) {
			this.error = error;
		}
	}

	public ParseException( Error error, String item ) {
		this( error );
		if( item != null ) {
			this.item = item;
		}
	}
	
	public ParseException( String message, String item ) {
		super( message );
		if( item != null ) {
			this.item = item;
		}
	}

	public ParseException( String message ) {
		this( message, "" );
	}
	
	public int getLineNumber() {
		return line;
	}

	public void setLineNumber( int lineNumber ) {
		line = lineNumber;
	}
	
	public String getItem() {
		return item;
	}
	
	public String getLocation() {
		return location;
	}	
	
	public void setLocation( String location ) {
		this.location = location;
	}
	
	public Error getError() {
		return error;
	}
	
	public String getMessage() {
		String msg = ( !Error.OTHER.equals(error) ) ? error.toString() : super.getMessage();
		return msg;
	}
	
	public String toString() {
		return getMessage() + " ('" + item + "' at '" + location + "' on line " + line + ")";
	}
}
