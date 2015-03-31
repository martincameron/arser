
package arser;

/**
	A ContentModel that matches a single occurrence of any token.
*/
public class AnyTokenModel implements ContentModel {
	private boolean marked;

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
		return marked = mark;
	}
	
	public ContentModel copy() {
		return new AnyTokenModel();
	}
	
	public String toString() {
		/* This model is not handled by ContentModelCompiler. */
		return "<Any>";
	}
	
	public int precedence() {
		return 3;
	}
}
