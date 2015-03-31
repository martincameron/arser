
package arser;

/**
	A ContentModel that represents a sequence of two models.
*/
public class SequenceModel implements ContentModel {
	private boolean marked;
	private ContentModel lhs, rhs;
	
	public SequenceModel( ContentModel left, ContentModel right ) {
		lhs = left;
		rhs = right;
	}
	
	public void reset() {
		lhs.reset();
		rhs.reset();
		marked = false;
	}
	
	public boolean empty() {
		return lhs.empty() && rhs.empty();
	}
	
	public boolean marked( boolean recursive ) {
		if( recursive )
			return marked || lhs.marked( true ) || rhs.marked( true );
		return marked;
	}
	
	public boolean shift( String token, boolean mark ) {
		boolean old_marked_left = lhs.marked( false );
		boolean marked_left = lhs.shift( token, mark );
		boolean marked_right = rhs.shift( token, old_marked_left || ( mark && lhs.empty() ) );
		return marked = ( marked_left && rhs.empty() ) || marked_right;
	}
	
	public ContentModel copy() {
		return new SequenceModel( lhs.copy(), rhs.copy() );
	}
	
	public String toString() {
		String left = lhs.toString();
		if( lhs.precedence() < precedence() )
			left = "(" + left + ")";
		String right = rhs.toString();
		if( rhs.precedence() < precedence() )
			right = "(" + right + ")";
		return left + "," + right;
	}
	
	public int precedence() {
		return 1;
	}

	public ContentModel getLeftModel() {
		return lhs;
	}

	public ContentModel getRightModel() {
		return rhs;
	}
}
