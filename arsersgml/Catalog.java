
package arsersgml;

import java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import arser.Attribute;
import arser.AttributeDecl;
import arser.Declaration;
import arser.Doctype;
import arser.ElementDecl;
import arser.Handler;
import arser.Loader;
import arser.ParseException;
import arser.Parser;
import arser.RepetitionModel;
import arser.TokenModel;
import arser.Validator;

/**
	An instance of Catalog associates SGML system-IDs to arser doctypes.
	The mappings are loaded from a UTF-8 encoded catalog file, parsed using the schema given by Catalog.DOCTYPE.
*/
public class Catalog implements Handler {
	public static final Doctype DOCTYPE = new Doctype( Arrays.asList( new ElementDecl[] {
		new ElementDecl( "catalog", null, new RepetitionModel( new TokenModel( "doctype" ), true ), null, null, false, true ),
		new ElementDecl( "doctype", Arrays.asList( new AttributeDecl[] {
			new AttributeDecl( "public", null, null, true ),
			new AttributeDecl( "system", null, null, true ) } ),
			null, null, null, false, true )
	} ) );
	
	private File catalogDir;
	private Map<String,String> publicIdToSystemId = new HashMap<String, String>();
	private Map<String,Doctype> doctypeCache = new HashMap<String, Doctype>();;

	public Catalog( File catalogXml ) throws IOException, ParseException {
		catalogDir = catalogXml.getParentFile();
		Reader reader = new InputStreamReader( new FileInputStream( catalogXml ), "UTF-8" );
		try {
			Validator validator = new Validator( this );
			validator.doctype( DOCTYPE );
			new Parser().parse( reader, validator );
		} finally {
			reader.close();
		}
	}

	public Doctype getDoctype( String publicId ) throws IOException, ParseException {
		String systemId = publicIdToSystemId.get( publicId );
		if( systemId == null ) {
			throw new IllegalArgumentException( "No Doctype in catalog for public ID: " + publicId );
		}
		Doctype doctype = doctypeCache.get( publicId );
		if( doctype == null ) {
			Reader reader = new InputStreamReader( new FileInputStream( new File( catalogDir, systemId ) ), "UTF-8" );
			try {
				doctype = Doctype.parse( reader );
				doctypeCache.put( publicId, doctype );
			} finally {
				reader.close();
			}
		}
		return doctype;
	}

	public void begin() throws ParseException {}
	public void doctype( Doctype dt ) throws ParseException {}
	public void comment( String comment ) throws ParseException {}
	public void declaration( Declaration declaration ) throws ParseException {}
	public void pi( String instruction ) throws ParseException {}
	public void entity( String name ) throws ParseException {}
	public void open( String name, java.util.List<Attribute> attributes ) throws ParseException {
		if( "doctype".equals( name ) ) {
			String publicId = "", systemId = "";
			for( Attribute attribute : attributes ) {
				if( "public".equals( attribute.getName() ) ) {
					publicId = attribute.getValue();
				} else if( "system".equals( attribute.getName()  ) ) {
					systemId = attribute.getValue();
				}
			}
			publicIdToSystemId.put( publicId, systemId );
		}
	}
	public void characters( String characters ) throws ParseException {}
	public void characters( String param, String characters ) throws ParseException {}
	public void close( String name ) throws ParseException {}
	public void end() throws ParseException {}
}
