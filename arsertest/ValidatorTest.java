
package arsertest;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import arser.Attribute;
import arser.Declaration;
import arser.Doctype;
import arser.Handler;
import arser.Parser;
import arser.ParseException;
import arser.Validator;

/**
	Stream a document through the validator.
*/
public class ValidatorTest implements Handler {
	public static void main( String[] args ) throws IOException, ParseException {
		if( args.length < 2 ) {
			System.out.println( "Usage: java " + ValidatorTest.class.getName() + " doctype.doctype input.sgml" );
			System.exit( 0 );
		}
		// Load doctype.
		Reader reader = new InputStreamReader( new FileInputStream( args[ 0 ] ), "ISO-8859-1" );
		Doctype doctype = Doctype.parse( reader );
		reader.close();
		// Parse document.
		reader = new InputStreamReader( new FileInputStream( args[ 1 ] ), "ISO-8859-1" );
		Validator validator = new Validator( new ValidatorTest() );
		validator.doctype( doctype );
		new Parser().parse( reader, validator );
		reader.close();
	}
	
	/** Called at the beginning of a document. */
	public void begin() throws ParseException {
	}
	/** Called by the Validator when a doctype is configured.*/
	public void doctype( Doctype doctype ) throws ParseException {
	}
	/** Called when a comment is encountered. */
	public void comment( String comment ) throws ParseException {
	}
	/** Called when a declaration is encountered. */
	public void declaration( Declaration declaration ) throws ParseException {
	}
	/** Called when a processing-instruction is encountered. */
	public void pi( String instruction ) throws ParseException {
	}
	/** Called when an entity reference is encountered. */
	public void entity( String name ) throws ParseException {
	}
	/** Called when an element is encountered. */
	public void open( String name, java.util.List<Attribute> attributes ) throws ParseException {
	}
	/** Called when character data between elements is encountered. */
	public void characters( String characters ) throws ParseException {
	}
	/** Called when characters within a marked section are encountered. */
	public void characters( String param, String characters ) throws ParseException {
	}
	/** Called when an element is closed. */
	public void close( String name ) throws ParseException {
	}
	/** Called when the end of the document is encountered. */
	public void end() throws ParseException {
		System.out.println( "Document is valid." );
	}
}
