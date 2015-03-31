
package arsertest;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import arser.Doctype;
import arser.Document;
import arser.Loader;
import arser.Parser;
import arser.ParseException;
import arser.Validator;

/**
	Load a document into a Document instance an validate it in memory.
*/
public class DocumentTest {
	public static void main( String[] args ) throws IOException, ParseException {
		if( args.length < 1 ) {
			System.err.println( "Usage: java " + DocumentTest.class.getName() + " doctype.doctype input.sgml" );
			System.exit( 0 );
		} else {
			// Load doctype.
			Reader reader = new InputStreamReader( new FileInputStream( args[ 0 ] ), "ISO-8859-1" );
			Doctype doctype = Doctype.parse( reader );
			reader.close();
			if( args.length > 1 ) {
				// Load document. Loader requires the input to be normalized, so a Validator must be used.
				reader = new InputStreamReader( new FileInputStream( args[ 1 ] ), "ISO-8859-1" );
				Loader loader = new Loader();
				Validator validator = new Validator( loader );
				validator.doctype( doctype );
				new Parser().parse( reader, validator );
				reader.close();
				// Validate in-memory (again), to test the in-memory validator.
				loader.getDocument().validate();
				System.out.println( "Document is valid." );
			} else {
				// Output the doctype.
				Writer writer = new OutputStreamWriter( System.out );
				doctype.write( writer );
				writer.close();
			}
		}
	}
}
