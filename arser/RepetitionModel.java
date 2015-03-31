
package arser;

/**
	A ContentModel that represents one-or-more occurrences of a model,
	or zero-or-more if the optional flag is set.
*/
public class RepetitionModel implements ContentModel {
	private boolean empty, marked;
	private ContentModel content;
	
	public RepetitionModel( ContentModel model, boolean optional ) {
		content = model;
		empty = optional;
	}
	
	public void reset() {
		content.reset();
		marked = false;
	}
	
	public boolean empty() {
		return empty || content.empty();
	}
	
	public boolean marked( boolean recursive ) {
		if( recursive )
			return marked || content.marked( true );
		return marked;
	}

	public boolean shift( String token, boolean mark ) {
		return marked = content.shift( token, mark || marked );
	}
	
	public ContentModel copy() {
		return new RepetitionModel( content.copy(), empty );
	}
	
	public String toString() {
		String str = content.toString();
		if( content.precedence() < precedence() ) str = "(" + str + ")";
		return str + ( empty ? "*" : "+" );
	}	
	
	public int precedence() {
		return 3;
	}
	
	public ContentModel getModel() {
		return content;
	}
}
