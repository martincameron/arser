
package arsersgml;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Stack;

import arser.Attribute;
import arser.Declaration;
import arser.Doctype;
import arser.Handler;
import arser.Loader;
import arser.ParseException;
import arser.Parser;
import arser.Validator;

/**
	Validate an SGML file using a Catalog to determine the Doctype.
*/
public class ParseSgml implements Handler {
	private Catalog catalog;
	private Handler validator;

	public ParseSgml( Catalog catalog ) {
		this.catalog = catalog;
	}

	public void setValidator( Handler validator ) {
		this.validator = validator;
	}

	public void begin() throws ParseException {}
	public void doctype( Doctype dt ) throws ParseException {
	}
	public void comment( String comment ) throws ParseException {
	}
	public void declaration( Declaration declaration ) throws ParseException {
		if( "doctype".equals( declaration.getName().toLowerCase() ) ) {
			java.util.List<String> params = declaration.getParams();
			if( params.size() > 2 && "public".equals( params.get( 1 ).toLowerCase() ) ) {
				String publicId = params.get( 2 );
				if( validator != null ) {
					try {
						validator.doctype( catalog.getDoctype( Declaration.unQuote( publicId ) ) );
					} catch( IOException e ) {
						throw new ParseException( e.getMessage() );
					}
				}
			}
		}
	}
	public void pi( String instruction ) throws ParseException {
	}
	public void entity( String name ) throws ParseException {
	}
	public void open( String name, java.util.List<Attribute> attributes ) throws ParseException {
	}
	public void characters( String characters ) throws ParseException {
	}
	public void characters( String param, String characters ) throws ParseException {
	}
	public void close( String name ) throws ParseException {
	}
	public void end() throws ParseException {}
	
	public static void main( String[] args ) throws Exception {
		if( args.length != 2 ) {
			System.err.println( "Usage: java " + ParseSgml.class.getName() + " catalog.xml input.sgm" );
		} else {
			ParseSgml parseSgml = new ParseSgml( new Catalog( new java.io.File( args[ 0 ] ) ) );
			Validator validator = new Validator( parseSgml );
			parseSgml.setValidator( validator );
			new Parser().parse( new java.io.InputStreamReader( new java.io.FileInputStream( args[ 1 ] ), "ISO-8859-1" ), validator );
			System.out.println( "Document is valid." );
		}
	}
}
