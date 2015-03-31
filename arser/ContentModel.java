
package arser;

/**
	Represents a node in the NFA of a content model expression.
	Based on the regular expression engine from:<p>
	http://morepypy.blogspot.com/2010/05/efficient-and-elegant-regular.html
*/
public interface ContentModel {
	void reset();
	/** Returns true if this model can be satisfied with no input. */
	boolean empty();
	/** Returns true if this model is marked. */
	public boolean marked( boolean recursive );
	/** Advance the state of the model using the token and the mark. */
	boolean shift( String token, boolean mark );
	/** Return a copy of this model. */
	ContentModel copy();
	/** Return an integer representing the operator precedence of this model. */
	int precedence();
}
