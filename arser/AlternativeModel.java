
package arser;

/**
	A ContentModel representing a choice between two models.
*/
public class AlternativeModel implements ContentModel {
	private boolean marked;
	private ContentModel lhs, rhs;
	
	public AlternativeModel( ContentModel left, ContentModel right ) {
		lhs = left;
		rhs = right;
	}
	
	public void reset() {
		lhs.reset();
		rhs.reset();
		marked = false;
	}
	
	public boolean empty() {
		return lhs.empty() || rhs.empty();
	}
	
	public boolean marked( boolean recursive ) {
		if( recursive )
			return marked || lhs.marked( true ) || rhs.marked( true );
		return marked;
	}
	
	public boolean shift( String token, boolean mark ) {
		return marked = lhs.shift( token, mark ) | rhs.shift( token, mark );
	}
	
	public ContentModel copy() {
		return new AlternativeModel( lhs.copy(), rhs.copy() );
	}
	
	public String toString() {
		String left = lhs.toString();
		if( lhs.precedence() < precedence() )
			left = "(" + left + ")";
		String right = rhs.toString();
		if( rhs.precedence() < precedence() )
			right = "(" + right + ")";
		return left + "|" + right;
	}

	public int precedence() {
		return 2;
	}

	public ContentModel getLeftModel() {
		return lhs;
	}

	public ContentModel getRightModel() {
		return rhs;
	}
}
