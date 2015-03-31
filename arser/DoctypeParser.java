
package arser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
	An implementation of Handler that builds a Doctype instance from markup.<p>
	Used by Doctype.parse().
*/
public class DoctypeParser implements Handler {
	private Doctype doctype;

	private List<Attribute> elementParams;
	private List<AttributeDecl> attributeDecls;
	private List<ElementDecl> elementDecls;

	/** @return The Doctype that was the result of the markup passed to this instance.*/
	public Doctype getDoctype() {
		return doctype;
	}

	public void begin() {}
	public void doctype( Doctype doctype ) {}
	public void comment( String comment ) {}
	public void declaration( Declaration declaration ) {}
	public void pi( String instruction ) {}
	public void entity( String name ) {}
	
	public void open( String name, java.util.List<Attribute> atts ) throws ParseException {
		name = name.toLowerCase();
		if( name.equals( "doctype" ) ) {
			doctype = null;
			attributeDecls = null;
			elementDecls = new ArrayList<ElementDecl>();
		}
		if( name.equals( "element" ) ) {
			elementParams = new ArrayList<Attribute>( atts );
			attributeDecls = new ArrayList<AttributeDecl>();
		}
		if( name.equals( "attribute" ) ) {
			attributeDecls.add( createAttributeDecl( atts ) );
		}
	}
	
	public void characters( String characters ) {}
	public void characters( String param, String characters ) {}
	
	public void close( String name ) throws ParseException {
		name = name.toLowerCase();
		if( name.equals( "doctype" ) ) {
			doctype = new Doctype( elementDecls );
		}
		if( name.equals( "element" ) ) {
			elementDecls.add( createElementDecl( elementParams, attributeDecls ) );
		}
	}

	public void end() {}

	private ElementDecl createElementDecl( List<Attribute> params, List<AttributeDecl> decls ) throws ParseException {
		String name = Attribute.getValue( params, "name" );
		String content = Attribute.getValue( params, "content" );
		ContentModel model = null;
		if( content != null && content.length() > 0 ) {
			model = new ContentModelCompiler().compile( content );
		}
		List<String> include = split( Attribute.getValue( params, "include" ) );
		List<String> exclude = split( Attribute.getValue( params, "exclude" ) );
		String type = Attribute.getValue( params, "type" ).toLowerCase();
		boolean empty = "empty".equals( type );
		boolean omit = "omit".equals( type ) || empty;
		return new ElementDecl( name, decls, model, include, exclude, empty, omit );
	}
	
	private AttributeDecl createAttributeDecl( List<Attribute> params ) throws ParseException {
		String name = Attribute.getValue( params, "name" );
		List<String> values = split( Attribute.getValue( params, "values" ) );
		String defValue = Attribute.getValue( params, "default" );
		boolean required = "required".equals( Attribute.getValue( params, "required" ).toLowerCase() );
		return new AttributeDecl( name, values, defValue, required );
	}
	
	private static List<String> split( String string ) throws ParseException {
		List<String> list = new ArrayList<String>();
		int start = 0;
		int length = string.length();
		for( int idx = 0; idx < length; idx++ ) {
			if( string.charAt( idx ) == ',' ) {
				if( start == idx ) {
					throw new ParseException( ParseException.Error.MALFORMED_LIST, string );
				}
				list.add( string.substring( start, idx ) );
				start = idx + 1;
			}
		}
		if( start < length ) {
			list.add( string.substring( start, length ) );
		}
		return list;
	}
}
