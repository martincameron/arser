
package arsertest;

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
	Parser/validator unit tests.
*/
public class ParserTest {
	public static void main( String[] args ) throws IOException, ParseException {
		/* Load the doctype into the document object model and validate. */
		Loader loader = new Loader();
		StringWriter doctypeWriter = new StringWriter();
		Doctype.doctype().write( doctypeWriter );
		test( doctypeWriter.toString(), Doctype.doctype(), loader, null );
		loader.getDocument().validate();
		doctypeWriter = new StringWriter();
		loader.getDocument().write( doctypeWriter );
		Doctype.parse( new StringReader( doctypeWriter.toString() ) );
		/* Handler for unit tests.*/
		Handler handler = new Handler() {
			private Doctype doctype;
			private Stack<String> stack = new Stack<String>();
			public void begin() throws ParseException {}
			public void doctype( Doctype dt ) throws ParseException {
				doctype = dt;
			}
			public void comment( String comment ) throws ParseException {
				if( comment.length() > 0 && !"comm-ent".equals( comment ) ) {
					throw new ParseException( "Unexpected Comment Text.", comment );
				}
			}
			public void declaration( Declaration declaration ) throws ParseException {
				if( !"decl".equals( declaration.getName() ) ) {
					throw new ParseException( "Unexpected declaration name.", declaration.getName() );
				}
				for( String param : declaration.getParams() ) {
					if( !( "param".equals( param )
						|| "\"param\"".equals( param )
						|| "'param'".equals( param )
						|| "'par am'".equals( param )
						|| "(par( )am)".equals( param )
						|| "(par( )am)+".equals( param )
						|| "(par( )am)*".equals( param )
						|| "(par( )am)?".equals( param )
						|| "+(par( )am)".equals( param )
						|| "-(par( )am)".equals( param )
						|| "-".equals( param ) ) ) {
						throw new ParseException( "Unexpected declaration parameter.", param );
					}
				}
				for( Declaration decl : declaration.getSubset() ) {
					declaration( decl );
				}
			}
			public void pi( String instruction ) throws ParseException {
				if( !"pi".equals( instruction ) ) {
					throw new ParseException( "Unexpected Processing Instruction.", instruction );
				}
			}
			public void entity( String name ) throws ParseException {
				if( !"ent".equals( name ) ) {
					throw new ParseException( "Unexpected Entity.", name );
				}
			}
			public void open( String name, java.util.List<Attribute> attributes ) throws ParseException {
				if( !doctype.getElementDecl( name ).isEmpty() ) {
					stack.push( name );
				}
				for( Attribute attr : attributes ) {
					String attName = attr.getName();
					if( !"attr".equalsIgnoreCase( attName ) ) {
						throw new ParseException( "Unexpected attribute name.", attName );
					}
					String value = attr.getValue();
					if( value.length() > 0 && !"a".equals( value )
						&& !"b".equals( value ) && !"c".equals( value ) ) {
						throw new ParseException( "Unexpected attribute value.", value );
					}
				}
			}
			public void characters( String characters ) throws ParseException {
				if( characters.length() == 1 && characters.charAt( 0 ) > 32 ) {
					throw new ParseException( "Expected whitespace here.", characters );
				}
				if( characters.length() > 1 ) {
					String elem = stack.peek();
					if( !elem.equals( characters ) ) {
						throw new ParseException( "Expected '" + elem + "' here.", characters );
					}
				}
			}
			public void characters( String param, String characters ) throws ParseException {
				if( !"CDATA".equals( param ) ) {
					throw new ParseException( "Expected 'CDATA' here.", param );
				}
				characters( characters );
			}
			public void close( String name ) throws ParseException {
				String current = stack.pop();
				if( !current.equalsIgnoreCase( name ) ) {
					throw new ParseException( "Unexpected close tag.", name );
				}
			}
			public void end() throws ParseException {}
		};
		/* Doctype for unit tests. */
		Doctype doctype = Doctype.parse(
			new StringReader( "<doctype>" +
				"<element name=doc content=a? omit>" +
					"<attribute name=attr values=a,b,c default=a>" +
				"<element name=book><attribute name=title required>" +
				"<element name=content content=a,b,c+,d*,e?,(f|x,g|y),h?+>" +
				"<element name=parent content=a>" +
				"<element name=a empty>" +
				"<element name=b empty>" +
				"<element name=c empty>" +
				"<element name=d empty>" +
				"<element name=e empty>" +
				"<element name=f empty>" +
				"<element name=g empty>" +
				"<element name=h include=i>" +
				"<element name=i content=j>" +
				"<element name=j exclude=i>" +
				"<element name=x empty>" +
				"<element name=y empty>" +
				"<element name=z>"
			)
		);
		/* Basic syntax.*/
		test( "", doctype, handler, null );
		test( " ", doctype, handler, null );
		test( "\n", doctype, handler, null );
		test( "<", doctype, handler, ParseException.Error.UNEXPECTED_END_OF_FILE );
		/* Processing instructions.*/
		test( "<?pi>", doctype, handler, null );
		test( " <?pi> ", doctype, handler, null );
		/* Entities.*/
		test( "&ent;", doctype, handler, null );
		test( "&ent;<?pi>", doctype, handler, null );
		test( " &ent; <?pi> ", doctype, handler, null );
		/* Comments. */
		test( "<!>", doctype, handler, null );
		test( "<!---->", doctype, handler, null );
		test( "<!--comm-ent-->", doctype, handler, null );
		test( "<!--comm-ent--   > ", doctype, handler, null );
		test( "<!--comm-ent-- --comm-ent--> ", doctype, handler, null );
		test( "<!--comm-ent-- --comm-ent-- > ", doctype, handler, null );
		test( "<!--comm-ent-- --comm-ent-- ----> ", doctype, handler, null );
		test( "<!--comm-ent----comm-ent------> ", doctype, handler, null );
		test( "<doc><!--comm-ent-->doc", doctype, handler, null );
		test( "<!->", doctype, handler, ParseException.Error.EXPECTED_HY_HERE );
		test( "<!---- X>", doctype, handler, ParseException.Error.EXPECTED_GT_HERE );
		/* Marked sections.*/
		test( "<doc><![CDATA[]]></DOC>", doctype, handler, null );
		test( "<doc><![CDATA[doc]]>", doctype, handler, null );
		test( "<doc><![ CDATA [doc]] >doc", doctype, handler, null );
		test( "<![CDATA CDATA[]]>", doctype, handler, ParseException.Error.EXPECTED_OB_HERE );
		test( "<![CDATA[]] X>", doctype, handler, ParseException.Error.EXPECTED_GT_HERE );
		test( "<![]>", doctype, handler, ParseException.Error.EXPECTED_OB_HERE );
		/* Declarations. */
		test( "<!decl>", doctype, handler, null );
		test( "<!decl >", doctype, handler, null );
		test( "<!decl param>", doctype, handler, null );
		test( "<!decl param >", doctype, handler, null );
		test( "<!decl --comment-- param ---- >", doctype, handler, null );
		test( "<!decl--comment--param---->", doctype, handler, null );
		test( "<!decl--comment--parm---c-->", doctype, handler, ParseException.Error.OTHER );
		test( "<!decl param \"param\">", doctype, handler, null );
		test( "<!decl param 'param' >", doctype, handler, null );
		test( "<!decl 'par am'>", doctype, handler, null );
		test( "<!decl (par( )am)>", doctype, handler, null );
		test( "<!decl (par( )am)+'par am' >", doctype, handler, null );
		test( "<!decl (par( )am)*param>", doctype, handler, null );
		test( "<!decl (par( )am)?--comment--'par am'>", doctype, handler, null );
		test( "<!decl +(par( )am) -(par( )am)>", doctype, handler, null );
		test( "<!decl -(par( )am)--comment--->", doctype, handler, null );
		test( "<!decl[]>", doctype, handler, null );
		test( "<!decl[<!decl>]>", doctype, handler, null );
		test( "<!decl 'param'[ <!decl > ] >", doctype, handler, null );
		test( "<!decl 'param' [ <!decl[<!decl>]> ] >", doctype, handler, null );
		test( "<!decl param[<!decl[<!decl param>]>]>", doctype, handler, null );
		test( "<!decl[%ent;]>", doctype, handler, null );
		test( "<!decl[%ent;<?pi>]>", doctype, handler, null );
		test( "<!decl[%ent;<!--comment-->]>", doctype, handler, null );
		test( "<!decl[%ent;<![ INCLUDE[<!---c--> <?pi> <!decl>]]>]>", doctype, handler, null );
		test( "<!decl[%ent;<![ INCLUDE[<!><?pi>%pr;<!declr>]]>]>", doctype, handler, ParseException.Error.OTHER );
		test( "<! >", doctype, handler, ParseException.Error.INVALID_DECLARATION );
		test( "<!decl[<a>]>", doctype, handler, ParseException.Error.EXPECTED_EX_OR_QM_HERE );
		test( "<!decl[a]>", doctype, handler, ParseException.Error.EXPECTED_LT_OR_CB_HERE );
		test( "<!decl--comment--param2---->", doctype, handler, ParseException.Error.OTHER );
		/* Tags and models. */
		test( "<a>", doctype, handler, null );
		test( "<a> </a>", doctype, handler, ParseException.Error.UNEXPECTED_CLOSE_TAG );
		test( "<z>", doctype, handler, ParseException.Error.CLOSE_ELEMENT_MISSING );
		test( "<z/>", doctype, handler, null );
		test( "<z />", doctype, handler, null );
		test( "<z></z>", doctype, handler, null );
		test( "<parent>", doctype, handler, ParseException.Error.CLOSE_ELEMENT_MISSING );
		test( "<parent><a>parent</>", doctype, handler, null );
		test( "<parent><a>parent</parent>", doctype, handler, null );
		test( "<doc>doc<a></><b>", doctype, handler, null );
		test( "<doc>doc</doc>", doctype, handler, null );
		test( "<doc>doc<a>doc", doctype, handler, null );
		test( "<doc<a</doc>", doctype, handler, null );
		test( "<i<j</j</i>", doctype, handler, null );
		test( "<>", doctype, handler, ParseException.Error.INVALID_TAG_NAME );
		test( "<doc></ doc>", doctype, handler, ParseException.Error.EXPECTED_LT_OR_GT_HERE );
		test( "<wibble>", doctype, handler, ParseException.Error.ELEMENT_NOT_DECLARED );
		test( "<content><A><B><C><D><E><F><G></content>", doctype, handler, null );
		test( "<content><B></content>", doctype, handler, ParseException.Error.ELEMENT_NOT_PERMITTED );
		test( "<content><A><B><C><c><D><E><F><G></content>", doctype, handler, null );
		test( "<content><A><B><D><E><F><G></content>", doctype, handler, ParseException.Error.ELEMENT_NOT_PERMITTED );
		test( "<content><A><B><C><E><F><G></content>", doctype, handler, null );
		test( "<content><A><B><C><d><d><E><F><G></content>", doctype, handler, null );
		test( "<content><A><B><C><d><F><G></content>", doctype, handler, null );
		test( "<content><A><B><C><F><G></content>", doctype, handler, null );
		test( "<content><A><B><C><e><F><G></content>", doctype, handler, null );
		test( "<content><A><B><C><e><e><F><G></content>", doctype, handler, ParseException.Error.ELEMENT_NOT_PERMITTED );
		test( "<content><A><B><C><X><Y></content>", doctype, handler, null );
		test( "<content><A><B><C><F><Y></content>", doctype, handler, null );
		test( "<content><A><B><C><X><G></content>", doctype, handler, null );
		test( "<content><A><B><C><Y></content>", doctype, handler, ParseException.Error.ELEMENT_NOT_PERMITTED );
		test( "<content><A><B><C></content>", doctype, handler, ParseException.Error.CLOSE_ELEMENT_NOT_PERMITTED );
		test( "<content>", doctype, handler, ParseException.Error.CLOSE_ELEMENT_MISSING );
		test( "<content></content>", doctype, handler, ParseException.Error.CLOSE_ELEMENT_NOT_PERMITTED );
		/* Inclusions and exclusions. */
		test( "<h><i><j></j></i></h>", doctype, handler, null );
		test( "<h><i><j></j></i><i><j></j></i></h>", doctype, handler, null );
		test( "<h><i><j></j><i><j></j></i></i></h>", doctype, handler, null );
		test( "<h><i><j><i><j></j></i></j></i></h>", doctype, handler, ParseException.Error.ELEMENT_NOT_PERMITTED );
		/* Attributes. */
		test( "<doc attr=''>", doctype, handler, null );
		test( "<doc attr=c>", doctype, handler, null );
		test( "<doc attr='b'>", doctype, handler, null );
		test( "<doc attr=\"c\">", doctype, handler, null );
		test( "<doc attr=a >", doctype, handler, null );
		test( "<doc attr = a>", doctype, handler, null );
		test( "<doc attr =a >", doctype, handler, null );
		test( "<doc attr= a>", doctype, handler, null );
		test( "<doc b>", doctype, handler, null );
		test( "<doc c >", doctype, handler, null );
		test( "<doc d>", doctype, handler, ParseException.Error.UNDECLARED_ATTRIBUTE );
		test( "<doc a b>", doctype, handler, ParseException.Error.DUPLICATE_ATTRIBUTE );
		test( "<doc ATTR=D>", doctype, handler, ParseException.Error.ATTRIBUTE_VALUE_NOT_PERMITTED );
		test( "<book>", doctype, handler, ParseException.Error.REQUIRED_ATTRIBUTE_MISSING );
		System.out.println( "All tests passed." );
	}

	/**
		Test the parser and validator.
		@param document the document to parse.
		@param doctype the doctype to use.
		@param handler the handler to receive the parse events for testing.
		@param expected the expected ParseException.Error, or null if no exception is expected.
		@throws ParseException if an unexpected error occurs.
	*/
	public static void test( String document, Doctype doctype, Handler handler, ParseException.Error expected ) throws IOException, ParseException {
		System.out.println( "Testing: " + document );
		try {
			Validator validator = new Validator( handler );
			validator.doctype( doctype );
			new Parser().parse( new StringReader( document ), validator );
			if( expected != null ) {
				throw new ParseException( "Exception expected but none thrown.", expected.toString() );
			}
		} catch( ParseException e ) {
			if( expected == null || e.getError() != expected ) {
				throw e;
			}
		}
	}
}
