
package arser;

/**
	A linked-list representing the element context for the Validator.
*/
public class Context {
	private Context parentContext;
	private ElementDecl elementDecl;
	private ContentModel model;
	private boolean hasInclusions, hasExclusions, mark;
	
	public Context() {
		model = new RepetitionModel( new AnyTokenModel(), true );
		mark = true;
	}
	
	public Context( ElementDecl decl, Context parent ) {
		parentContext = parent;
		elementDecl = decl;
		model = decl.getContentModel();
		hasInclusions = decl.hasInclusions() || parentContext.hasInclusions;
		hasExclusions = decl.hasExclusions() || parentContext.hasExclusions;
		mark = true;
	}

	public Context getParent() {
		return parentContext;
	}

	public ElementDecl getElementDecl() {	
		return elementDecl;
	}

	public String toString() {
		if( elementDecl != null ) {
			return parentContext.toString() + "<" + elementDecl.getName() + ">";
		}
		return "";
	}

	/* Return true is the specified element name is permitted as an inclusion. */
	public boolean isIncluded( String name ) {
		boolean included = false;
		if( hasInclusions ) {
			included = elementDecl.hasInclusion( name ) || parentContext.isIncluded( name );
		}
		return included;
	}
	
	/* Return true is the specified element name has been excluded. */
	public boolean isExcluded( String name ) {
		boolean excluded = false;
		if( hasExclusions ) {
			excluded = elementDecl.hasExclusion( name ) || parentContext.isExcluded( name );
		}
		return excluded;
	}

	/* Advance the content model using the element name. */
	public void shift( String name ) {
		model.shift( name, mark );
		mark = false;
	}
	
	/* Return true if no more input is required. */
	public boolean complete() {
		return ( mark && model.empty() ) || model.marked( false );
	}
	
	/* Returns true if we can possibly become complete.*/
	public boolean marked() {
		return mark || model.marked( true );
	}
}
