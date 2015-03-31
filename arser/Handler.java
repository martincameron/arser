
package arser;

/**
	The main interface for both Parser and Validator.<p>
	Applications should implement this interface to receive markup events from either.
*/
public interface Handler {
	/** Called at the beginning of a document. */
	public void begin() throws ParseException;
	/** Called by the Validator when a doctype is configured.*/
	public void doctype( Doctype doctype ) throws ParseException;
	/** Called when a comment is encountered. */
	public void comment( String comment ) throws ParseException;
	/** Called when a declaration is encountered. */
	public void declaration( Declaration declaration ) throws ParseException;
	/** Called when a processing-instruction is encountered. */
	public void pi( String instruction ) throws ParseException;
	/** Called when an entity reference is encountered. */
	public void entity( String name ) throws ParseException;
	/** Called when an element is encountered. */
	public void open( String name, java.util.List<Attribute> attributes ) throws ParseException;
	/** Called when character data between elements is encountered. */
	public void characters( String characters ) throws ParseException;
	/** Called when characters within a marked section are encountered. */
	public void characters( String param, String characters ) throws ParseException;
	/** Called when an element is closed. */
	public void close( String name ) throws ParseException;
	/** Called when the end of the document is encountered. */
	public void end() throws ParseException;
}
