
package arser;

/**
	A ContentModel that represents zero or one occurences of a model.
*/
public class OptionalModel implements ContentModel {
	private ContentModel content;
	
	public OptionalModel( ContentModel model ) {
		content = model;
	}
	
	public void reset() {
		content.reset();
	}
	
	public boolean empty() {
		return true;
	}
	
	public boolean marked( boolean recursive ) {
		return content.marked( recursive );
	}
	
	public boolean shift( String token, boolean mark ) {
		return content.shift( token, mark );
	}
	
	public ContentModel copy() {
		return new OptionalModel( content.copy() );
	}
	
	public String toString() {
		String str = content.toString();
		if( content.precedence() < precedence() ) str = "(" + str + ")";
		return str + "?";
	}

	public int precedence() {
		return 3;
	}
	
	public ContentModel getModel() {
		return content;
	}
}
