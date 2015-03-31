
package arsersgml;

import arser.Attribute;
import arser.AttributeDecl;
import arser.ContentModel;
import arser.ContentModelCompiler;
import arser.Declaration;
import arser.Doctype;
import arser.ElementDecl;
import arser.Handler;
import arser.Parser;
import arser.ParseException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

/**
	Limited tool to convert element declarations in the
	doctype internal subset of an SGML document into an Arser Doctype.
*/
public class SGMLDoctypeConverter implements Handler {
	private List<Declaration> declarations;
	private Map<String,String> parameters;

	/**
		Convert element declarations in the internal subset of an SGML
		DOCTYPE declaration into an Arser Doctype instance. An attempt is
		made to expand parameter entities, and warn of possible inconsistencies.
	*/
	public Doctype convert( Reader input ) throws IOException, ParseException {
		declarations = new LinkedList<Declaration>();
		parameters = new TreeMap<String,String>();
		Parser parser = new Parser();
		parser.parse( input, this );
		// Expand parameters in all collected declarations.
		for( Declaration decl : declarations ) {
			ListIterator<String> listIterator = decl.getParams().listIterator();
			while( listIterator.hasNext() ) {
				String param = listIterator.next();
				listIterator.set( replace( param, parameters ) );
			}
		}
		// Format expanded declarations.
		StringWriter writer = new StringWriter();
		writer.write( "<!doctype[" );
		for( Declaration decl : declarations ) {
			decl.write( writer );
			writer.write( "\n" );
		}
		writer.write( "\n]>\n" );
		// Re-parse.
		declarations.clear();
		input = new StringReader( writer.toString() );
		parser.parse( input, this );
		// Expand and sort group declarations.
		Map<String,Declaration> elementDecls = new TreeMap<String,Declaration>();
		Map<String,Declaration> attlistDecls = new TreeMap<String,Declaration>();
		for( Declaration decl : declarations ) {
			String declName = decl.getName().toLowerCase();
			if( "attlist".equals( declName ) ) {
				List<String> params = decl.getParams();
				String elementNames = params.get( 0 ).toLowerCase();
				if( "#notation".equals( elementNames ) ) {
					System.out.println( "Ignoring notation attlist." );
				} else {
					List<String> names = expandNameGroup( elementNames );
					for( String name : names ) {
						params.set( 0, name );
						if( attlistDecls.get( name ) != null ) {
							System.out.println( "Attlist redeclared: " + name );
						}
						attlistDecls.put( name, new Declaration( declName, params, null ) );
					}
				}
			} else if( "element".equals( declName ) ) {
				List<String> params = decl.getParams();
				List<String> names = expandNameGroup( params.get( 0 ).toLowerCase() );
				for( String name : names ) {
					params.set( 0, name );
					if( elementDecls.get( name ) != null ) {
						System.out.println( "Element redeclared: " + name );
					}
					elementDecls.put( name, new Declaration( declName, params, null ) );
				}
			} else {
				System.out.println( "Ignoring declaration: " + declName );
			}
		}
		// Check all attlists have corresponding elements.
		for( String elementName : attlistDecls.keySet() ) {
			if( elementDecls.get( elementName ) == null ) {
				throw new ParseException( "Attlist has no corresponding element declaration: " + elementName );
			}
		}
		// Generate doctype.
		return generateDoctype( elementDecls, attlistDecls );
	}

	public static Doctype generateDoctype( Map<String,Declaration> elements, Map<String,Declaration> attlists ) throws ParseException {
		List<ElementDecl> elementDecls = new LinkedList<ElementDecl>();
		for( Declaration element : elements.values() ) {
			List<String> params = element.getParams();
			String name = params.get( 0 ).toLowerCase();
			String omitStart = params.get( 1 );
			String omitEnd = params.get( 2 );
			if( !"-".equals( omitStart ) && !"-".equals( omitEnd ) && !"o".equals( omitEnd ) && !"O".equals( omitEnd ) ) {
				throw new ParseException(
					"Invalid omission modifiers in element declaration: " + name );
			}
			// Convert model and check references.
			boolean isEmpty = "empty".equals( params.get( 3 ).toLowerCase() );
			String model = convertContentModel( params.get( 3 ) );
			for( String elem : expandNameGroup( "(" + model + ")" ) ) {
				if( !".".equals( elem ) && elements.get( elem ) == null ) {
					throw new ParseException( "Undeclared element '" + elem + "' in content model for element: " + name );
				}
			}
			List<String> exclusions = null;
			List<String> inclusions = null;
			for( int idx = 4; idx < params.size(); idx++ ) {
				String exc = params.get( idx );
				if( exc.charAt( 0 ) == '-' && exclusions == null ) {
					exclusions = expandNameGroup( exc.substring( 1 ) );
					for( String elem : exclusions ) {
						if( elements.get( elem ) == null ) {
							throw new ParseException( "Undeclared element '" + elem + "' in exclusions for element: " + name );
						}
					}
				} else if( exc.charAt( 0 ) == '+' && inclusions == null ) {
					inclusions = expandNameGroup( exc.substring( 1 ) );
					for( String elem : inclusions ) {
						if( elements.get( elem ) == null ) {
							throw new ParseException( "Undeclared element '" + elem + "' in inclusions for element: " + name );
						}
					}
				} else {
					throw new ParseException( "Unexpected exclusion/inclusion '" + exc + "' for element: " + name );
				}
			}
			ContentModel contentModel = new ContentModelCompiler().compile( model );
			boolean mayOmit = !"-".equals( omitEnd );
			List<AttributeDecl> attributeDecls = generateAttributeDecls( attlists.get( name ) );
			elementDecls.add( new ElementDecl( name, attributeDecls, contentModel, inclusions, exclusions, isEmpty, mayOmit ) );
		}
		return new Doctype( elementDecls );
	}

	public static List<AttributeDecl> generateAttributeDecls( Declaration attlist ) throws ParseException {
		/*<!ATTLIST #NOTATION ...> */
		/*<!ATTLIST elem(s) attname (value list)|CDATA|NUMBER|etc #FIXED? default|#REQUIRED|#IMPLIED ...> */
		Map<String,AttributeDecl> attributes = new TreeMap<String,AttributeDecl>();
		if( attlist != null ) {
			ListIterator<String> iterator = attlist.getParams().listIterator();
			String elementName = iterator.next();
			while( true ) {
				String attName = iterator.next().toLowerCase();
				if( !iterator.hasNext() ) {
					throw new ParseException( "Type or value list missing from attlist declaration." );
				}
				String attType = iterator.next().toLowerCase();
				List<String> attValues = new LinkedList<String>();
				if( attType.charAt( 0 ) == '(' ) {
					attValues = expandNameGroup( attType );
					Collections.sort( attValues );
				} else if( "cdata".equals( attType ) ) {
				} else if( "entity".equals( attType ) ) {
				} else if( "entities".equals( attType ) ) {
				} else if( "id".equals( attType ) ) {
				} else if( "idref".equals( attType ) ) {
				} else if( "idrefs".equals( attType ) ) {
				} else if( "name".equals( attType ) ) {
				} else if( "names".equals( attType ) ) {
				} else if( "nmtoken".equals( attType ) ) {
				} else if( "nmtokens".equals( attType ) ) {
				} else if( "notation".equals( attType ) ) {
				} else if( "number".equals( attType ) ) {
				} else if( "numbers".equals( attType ) ) {
				} else if( "nutoken".equals( attType ) ) {
				} else if( "nutokens".equals( attType ) ) {
				} else {
					throw new ParseException( "Unexpected attribute type: " + attType );
				}
				if( !iterator.hasNext() ) {
					throw new ParseException( "Default value, missing from attlist declaration." );
				}
				String attDefault = iterator.next();
				if( "#FIXED".equals( attDefault ) ) {
					if( !iterator.hasNext() ) {
						throw new ParseException( "Default value missing from attlist declaration." );
					}
					attDefault = iterator.next();
					attDefault = Declaration.unQuote( attDefault.toLowerCase() );
					attValues.clear();
					attValues.add( attDefault );
				}
				boolean attRequired = false;
				if( "#REQUIRED".equals( attDefault ) ) {
					attDefault = "";
					attRequired = true;
				} else if( "#IMPLIED".equals( attDefault ) ) {
					attDefault = "";
				} else {
					attDefault = Declaration.unQuote( attDefault.toLowerCase() );
				}
				AttributeDecl attributeDecl = new AttributeDecl( attName, attValues, attDefault, attRequired );
				attributes.put( attName, attributeDecl );
				if( !iterator.hasNext() ) {
					break;
				}
			}
		}
		return new LinkedList<AttributeDecl>( attributes.values() );
	}

	public void begin() throws ParseException {}
	public void declaration( Declaration declaration ) throws ParseException {
		for( Declaration decl : declaration.getSubset() ) {
			String name = decl.getName().toLowerCase();
			List<String> params = decl.getParams();
			if( "entity".equals( name ) ) {
				if( "%".equals( params.get( 0 ) ) ) {
					String paramName = params.get( 1 ).toLowerCase();
					String paramValue = Declaration.unQuote( params.get( 2 ) );
					if( parameters.containsKey( paramName ) ) {
						System.out.println( "Parameter redeclared: " + paramName
							+ "\nFrom: " + parameters.get( paramName ) + "\nTo:   " + paramValue );
					}
					parameters.put( paramName, paramValue );
					//System.out.println( name + " % " + paramName + " = " + paramValue );
				} else {
					System.out.println( "Ignoring entity declaration: " + name );
				}
			} else {
				declarations.add( decl );
			}
		}
	}

	public void doctype( Doctype doctype ) throws ParseException {}
	public void comment( String comment ) throws ParseException {}
	public void pi( String instruction ) throws ParseException {}
	public void entity( String name ) throws ParseException {}
	public void open( String name, java.util.List<Attribute> attributes ) throws ParseException {}
	public void characters( String characters ) throws ParseException {}
	public void characters( String param, String characters ) throws ParseException {}
	public void close( String name ) throws ParseException {}
	public void end() throws ParseException {}
	
	/* Read the entire content into a String. */
	public static String readString( Reader reader ) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		char[] buf = new char[ 65536 ];
		int read = 0;
		while( read > -1 ) {
			stringBuilder.append( buf, 0, read );
			read = reader.read( buf );
		}
		return stringBuilder.toString();
	}

	/* Return the names represented by the specified group, either a single name, or "(name1|name2|etc)" */
	public static List<String> expandNameGroup( String nameGroup ) {
		List<String> names = new LinkedList<String>();
		int start = 0;
		int end = nameGroup.length();
		for( int idx = 0; idx < end; idx++ ) {
			char chr = nameGroup.charAt( idx );
			if( chr <= 32 ) {
				chr = ',';
			}
			if( "(&|,*+?)".indexOf( chr ) >= 0 ) {
				String name = nameGroup.substring( start, idx );
				if( name.length() > 0 ) {
					names.add( name );
					//System.out.println("Name group item: " + name);
				}
				start = idx + 1;
			}
		}
		if( start < nameGroup.length() ) {
			names.add( nameGroup.substring( start ) );
		}
		return names;
	}

	/* Replace parameter entities in the specified String. */
	public static String replace( String str, Map<String,String> parameters ) throws ParseException {
		int idx = 0, len = str.length();
		while( idx < len ) {
			char chr = str.charAt( idx++ );
			if( chr == '%' ) {
				int start = idx;
				while( chr != ';' ) {
					chr = str.charAt( idx++ );
				}
				String name = str.substring( start, idx - 1 );
				String repl = parameters.get( name.toLowerCase() );
				if( repl == null ) {
					throw new ParseException( "Undefined entity: " + name );
				}
				//System.out.print( str );
				str = str.substring( 0, start - 1 ) + repl + str.substring( idx );
				idx = 0;
				len = str.length();
				//System.out.println( " -> " + str );
			}
		}
		return str;
	}

	/* Convert an SGML content model to an "arser compatible" one. */
	public static String convertContentModel( String model ) throws ParseException {
		// model could be "CDATA", "RCDATA", "EMPTY", "ANY", or "(content model)".
		// (content model) could contain "#PCDATA" as an occurrence.
		model = model.toLowerCase();
		if( "cdata".equals( model ) || "rcdata".equals( model ) || "empty".equals( model ) ) {
			return ".";
		} else if( "any".equals( model ) ) {
			throw new ParseException( "ANY content not supported." );
		} else if( model.charAt( 0 ) != '(' ) {
			throw new ParseException( "Expected '(' as first character of content model: " + model );
		}
		char[] chars = model.toCharArray();
		int len = 0;
		for( int idx = 0; idx < chars.length; idx++ ) {
			// Remove spaces.
			char chr = chars[ idx ];
			if( chr == '&' ) {
				throw new ParseException( "AND connector not supported. Please re-state in terms of OR." );
			}
			if( chr > 32 ) {
				chars[ len++ ] = chr;
			}
		}
		model = new String( chars, 0, len );
		int idx = model.indexOf( "#pcdata", 0 );
		while( idx >= 0 ) {
			model = model.substring( 0, idx ) + "." + model.substring( idx + 7 );
			idx = model.indexOf( "#pcdata", idx );
		}
		return model;
	}

	public static void main( String[] args ) throws Exception {
		if( args.length != 2 ) {
			System.err.println( "Usage java " + SGMLDoctypeConverter.class.getName() + " input.sgm output.sgm" );
			System.exit( 1 );
		}
		Reader input = new InputStreamReader( new FileInputStream( args[ 0 ] ), "ISO-8859-1" );
		Doctype doctype = new SGMLDoctypeConverter().convert( input );

		// Test the doctype parser.
		Writer writer = new StringWriter();
		doctype.write( writer );
		writer.close();
		doctype = Doctype.parse( new StringReader( writer.toString() ) );

		writer = new OutputStreamWriter( new FileOutputStream( args[ 1 ] ) );
		doctype.write( writer );
		writer.close();
	}
}
