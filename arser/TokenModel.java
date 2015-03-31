
package arser;

/**
	A ContentModel that matches a single occurrence of a token.
*/
public class TokenModel implements ContentModel {
	private boolean marked;
	private String value;
	
	public TokenModel( String token ) {
		value = token.toLowerCase();
	}
	
	public void reset() {
		marked = false;
	}
	
	public boolean empty() {
		return false;
	}
	
	public boolean marked( boolean recursive ) {
		return marked;
	}
	
	public boolean shift( String token, boolean mark ) {
		return marked = mark && value.equals( token.toLowerCase() );
	}
	
	public ContentModel copy() {
		return new TokenModel( value );
	}
	
	public String toString() {
		return value;
	}
	
	public int precedence() {
		return 3;
	}
}
