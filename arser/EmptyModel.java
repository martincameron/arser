
package arser;

/**
	A ContentModel that is satisfied only by no input.
*/
public class EmptyModel implements ContentModel {
	public void reset() {
	}
	
	public boolean empty() {
		return true;
	}
	
	public boolean marked( boolean recursive ) {
		return false;
	}
	
	public boolean shift( String token, boolean mark ) {
		return false;
	}
	
	public ContentModel copy() {
		return this;
	}
	
	public String toString() {
		return "";
	}
	
	public int precedence() {
		return 3;
	}
}
