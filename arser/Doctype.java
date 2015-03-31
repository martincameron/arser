
package arser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
	A doctype is essentially list of element declarations for the validation engine.<p>
	When passed to a Validator instance, elements within the markup are checked against the
	declarations within the Doctype.<p>
	The doctype is specified in markup and therefore has a doctype that can validate
	itself. This doctype used by the doctype parser and is generated by the static
	method Doctype.doctype().
*/
public class Doctype {
	private Map<String,ElementDecl> elementDecls;

	/**
		Constructor for a doctype containing the specified element declarations.
	*/
	public Doctype( Collection<ElementDecl> decls ) {
		elementDecls = new TreeMap<String,ElementDecl>();
		for( ElementDecl decl : decls ) {
			elementDecls.put( decl.getName(), decl );
		}
	}
	
	/**
		@return The ElementDecl with the specified name, or null if none exists.
	*/
	public ElementDecl getElementDecl( String name ) {
		return elementDecls.get( name.toLowerCase() );
	}

	/** Write this Doctype in the format expected by parse(). */
	public void write( Writer writer ) throws java.io.IOException {
		writer.write( "<doctype>\n" );
		for( ElementDecl elementDecl : elementDecls.values() )
			elementDecl.write( writer );
	}
	
	/** Parse the specified Doctype. */
	public static Doctype parse( Reader reader ) throws IOException, ParseException {
		DoctypeParser doctypeParser = new DoctypeParser();
		Validator validator = new Validator( doctypeParser );
		validator.doctype( doctype() );
		new Parser().parse( reader, validator );
		return doctypeParser.getDoctype();
	}

	/** Return the Doctype for the Doctype. */
	public static Doctype doctype() throws ParseException {
		List<ElementDecl> elementDecls = new ArrayList<ElementDecl>();
		elementDecls.add(
			new ElementDecl(
				"doctype",
				null,
				new ContentModelCompiler().compile( "element*" ),
				null,
				null,
				false,
				true
			)
		);
		elementDecls.add(
			new ElementDecl(
				"element",
				Arrays.asList( new AttributeDecl[] {
					new AttributeDecl( "name", null, null, true ),
					new AttributeDecl( "content", null, null, false ),
					new AttributeDecl( "include", null, null, false ),
					new AttributeDecl( "exclude", null, null, false ),
					new AttributeDecl( "type",
						Arrays.asList( new String[] { "mixed", "empty", "omit" } ),
						"mixed", false )
				} ),
				new ContentModelCompiler().compile( "attribute*" ),
				null,
				null,
				false,
				true
			)
		);
		elementDecls.add(
			new ElementDecl(
				"attribute",
				Arrays.asList( new AttributeDecl[] {
					new AttributeDecl( "name", null, null, true ),
					new AttributeDecl( "values", null, null, false ),
					new AttributeDecl( "default", null, null, false ),
					new AttributeDecl( "required",
						Arrays.asList( new String[] { "required", "optional" } ),
						"optional", false )
				} ),
				new EmptyModel(),
				null,
				null,
				true,
				true
			)
		);
		return new Doctype( elementDecls );
	}
}