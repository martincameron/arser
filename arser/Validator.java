
package arser;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Stack;

/**
	An implementation of Handler that validates and un-minimizes markup.<p>
	The doctype() method must be called before the first element
	is encountered, Parser will not do this automatically.
*/
public class Validator implements Handler {
	private Handler handler;
	private Context context;
	private Doctype doctype;

	/**
		Constructor.
		@param h The handler that will receive the validated markup.
	*/
	public Validator( Handler h ) {
		handler = h;
		context = new Context();
	}

	/** @see Handler */
	public void begin() throws ParseException {
		try {
			handler.begin();
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}
	
	/** @see Handler */
	public void doctype( Doctype dt ) throws ParseException {
		doctype = dt;
		try {
			handler.doctype( dt );
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}
	
	/** @see Handler */
	public void comment( String comment ) throws ParseException {
		try {
			handler.comment( comment );
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}
	
	/** @see Handler */
	public void declaration( Declaration declaration ) throws ParseException {
		try {
			handler.declaration( declaration );
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}
	
	/** @see Handler */
	public void pi( String instruction ) throws ParseException  {
		try {
			handler.pi( instruction );
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}
	
	/** @see Handler */
	public void entity( String name ) throws ParseException {
		try {
			handler.entity( name );
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}

	/** @see Handler */
	public void open( String name, List<Attribute> attributes ) throws ParseException {
		try {
			if( doctype == null ) {
				throw new ParseException( ParseException.Error.DOCTYPE_NOT_SET, name );
			}
			ElementDecl elementDecl = doctype.getElementDecl( name );
			if( elementDecl == null ) {
				throw new ParseException( ParseException.Error.ELEMENT_NOT_DECLARED, name );
			}
			elementDecl.validate( attributes );
			boolean permitted = false;
			boolean complete = context.complete();
			if( !context.isExcluded( name ) ) {
				if( context.isIncluded( name ) ) {
					permitted = true;
				} else {
					/* Ensure element is permitted by content model. */
					context.shift( name );
					if( context.marked() ) {
						// Element accepted.
						permitted = true;
					}
				}
			}
			if( permitted ) {
				if( !elementDecl.isEmpty() ) {
					/* Create a new context if element not empty. */
					context = new Context( elementDecl, context );
				}
				handler.open( elementDecl.getName(), attributes );
			} else {
				ElementDecl decl = context.getElementDecl();
				if( complete && decl != null && decl.mayOmit() ) {
					/* Attempt markup un-minimization. */
					handler.close( decl.getName() );
					context = context.getParent();
					open( name, attributes );
				} else {
					throw new ParseException( ParseException.Error.ELEMENT_NOT_PERMITTED, name );
				}
			}
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}

	/** @see Handler */
	public void characters( String characters ) throws ParseException {
		try {
			handler.characters( characters );
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}
	
	/** @see Handler */
	public void characters( String param, String characters ) throws ParseException {
		try {
			handler.characters( param, characters );
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}

	/** @see Handler */
	public void close( String name ) throws ParseException {
		try {
			if( doctype == null ) {
				throw new ParseException( ParseException.Error.DOCTYPE_NOT_SET, name );
			}
			name = name.toLowerCase();
			/* Check the element to be closed is at the top of the stack. */
			ElementDecl elementDecl = context.getElementDecl();
			if( elementDecl == null ) {
				throw new ParseException( ParseException.Error.UNEXPECTED_CLOSE_TAG, name );
			}
			boolean current = name.length() == 0 || name.equals( elementDecl.getName() );
			boolean permitted = context.complete() && ( current || elementDecl.mayOmit() );
			if( permitted ) {
				/* Pop the element. */
				handler.close( elementDecl.getName() );
				context = context.getParent();
				if( !current ) {
					/* Close the parent recursively until the named element is found. */
					close( name );
				}
			} else {
				throw new ParseException( ParseException.Error.CLOSE_ELEMENT_NOT_PERMITTED, name );
			}
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}

	/** @see Handler */
	public void end() throws ParseException {
		try {
			/* Attempt to close any remaining elements. */
			ElementDecl decl = context.getElementDecl();
			while( decl != null ) {
				if( !decl.mayOmit() ) {
					throw new ParseException( ParseException.Error.CLOSE_ELEMENT_MISSING, decl.getName() );
				}
				close( "" );
				decl = context.getElementDecl();
			}
			handler.end();
		} catch( ParseException parseException ) {
			parseException.setLocation( context.toString() );
			throw parseException;
		}
	}
}
