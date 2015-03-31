
package arser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
	An implementation of Handler that loads markup into a Document instance.<p>
	The doctype must be set before the first open event is received.
*/
public class Loader implements Handler {
	private Document document;
	private Deque<NodeContainer> stack;

	/**
		Instantiate a Loader with an empty Document.
	*/
	public Loader() {
		document = new Document();
		stack = new ArrayDeque<NodeContainer>();
		stack.push( document );
	}

	/**
		@return the Document instance.
	*/
	public Document getDocument() {
		return document;
	}

	/** @see Handler. */
	public void begin() throws ParseException {
	}
	
	/** @see Handler. */
	public void doctype( Doctype doctype ) throws ParseException {
		document.setDoctype( doctype );
	}
	
	/** @see Handler. */
	public void comment( String comment ) throws ParseException {
		stack.peek().getChildren().add( new Comment( comment ) );
	}
	
	/** @see Handler. */
	public void declaration( Declaration declaration ) throws ParseException {
		stack.peek().getChildren().add( declaration );
	}
	
	/** @see Handler. */
	public void pi( String instruction ) throws ParseException {
		stack.peek().getChildren().add( new ProcessingInstruction( instruction ) );
	}
	
	/** @see Handler. */
	public void entity( String name ) throws ParseException {
		stack.peek().getChildren().add( new Entity( name ) );
	}
	
	/** @see Handler. */
	public void open( String name, List<Attribute> attributes ) throws ParseException {
		Doctype doctype = document.getDoctype();
		if( doctype == null ) {
			throw new ParseException( ParseException.Error.DOCTYPE_NOT_SET, name );
		}
		ElementDecl decl = doctype.getElementDecl( name );
		if( decl == null ) {
			throw new ParseException( ParseException.Error.ELEMENT_NOT_DECLARED, name );
		}
		Element element = new Element( decl, stack.peek() );
		element.getAttributes().addAll( attributes );
		stack.peek().getChildren().add( element );
		if( !decl.isEmpty() ) {
			stack.push( element );
		}
	}
	
	/** @see Handler. */
	public void characters( String characters ) throws ParseException {
		stack.peek().getChildren().add( new Characters( characters ) );
	}
	
	/** @see Handler. */
	public void characters( String param, String characters ) throws ParseException {
		stack.peek().getChildren().add( new Characters( param, characters ) );
	}
	
	/** @see Handler. */
	public void close( String name ) throws ParseException {
		if( stack.size() < 2 ) {
			throw new ParseException( ParseException.Error.UNEXPECTED_CLOSE_TAG, name );
		}
		String openName = ( ( Element ) stack.peek() ).getName();
		if( openName.equals( name ) || name.length() == 0 ) {
			/* Name refers to the element at the top of the stack. */
			stack.pop();
		} else {
			throw new ParseException( ParseException.Error.CLOSE_ELEMENT_MISSING, openName );
		}
	}
	
	/** @see Handler. */
	public void end() throws ParseException {
		if( stack.size() > 1 ) {
			throw new ParseException( ParseException.Error.CLOSE_ELEMENT_MISSING, ( ( Element ) stack.peek() ).getName() );
		}
	}
}
