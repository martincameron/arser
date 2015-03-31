
package arser;

import java.io.Reader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
	This class parses markup from a Reader and passes the results onto a Handler.
*/
public class Parser {
	private static final int
		INPUT_BUF_LEN = 32768,
		TOKEN_BUF_LEN = 2048;
	
	private Reader input;
	private Handler handler;
	private char[] inputBuf = new char[ INPUT_BUF_LEN ];
	private char[] tokenBuf = new char[ TOKEN_BUF_LEN ];
	private int inputIdx, inputLen, tokenIdx, currentChar, currentLine;

	/**
		Parse the specified input and feed the resulting markup events to the Handler.
	*/
	public void parse( Reader input, Handler handler ) throws IOException, ParseException {
		this.input = input;
		this.handler = handler;
		inputIdx = inputLen = tokenIdx = 0;
		currentLine = 1;
		try {
			handler.begin();
			while( true ) {
				nextChar();
				if( currentChar < 0 ) {
					/* End of file. */
					if( tokenIdx > 0 ) {
						flushCharacters();
					}
					handler.end();
					return;
				} else if( currentChar == '<' ) {
					/* Tag. */
					if( tokenIdx > 0 ) {
						flushCharacters();
					}
					while( currentChar == '<' )	{
						tag();
					}
				} else if( currentChar == '&' ) {
					/* Entity. */
					if( tokenIdx > 0 ) {
						flushCharacters();
					}
					nextCharNoEof();
					handler.entity( token( ';' ) );
				} else {
					/* Characters. */
					tokenBuf[ tokenIdx++ ] = ( char ) currentChar;
					if( tokenIdx >= TOKEN_BUF_LEN ) {
						flushCharacters();
					}
				}
			}
		} catch( ParseException parseException ) {
			parseException.setLineNumber( currentLine );
			throw parseException;
		}
	}

	private void tag() throws IOException, ParseException {
		if( currentChar != '<' ) {
			throw new ParseException( ParseException.Error.EXPECTED_LT_HERE );
		}
		nextCharNoEof();
		if( currentChar == '?' ) {
			/* Processing instruction.*/
			nextCharNoEof();
			handler.pi( token( '>' ) );
		} else if( currentChar == '!' ) {
			nextCharNoEof();
			if( currentChar == '>' ) {
				/* Empty comment.*/
				handler.comment( "" );
			} else if( currentChar == '-' ) {
				/* Comment.*/
				while( true ) {
					if( currentChar == '-' ) {
						comment();
						whitespace();
					} else if( currentChar == '>' ) {
						break;
					} else {
						throw new ParseException( ParseException.Error.EXPECTED_GT_HERE );
					}
				}
			} else if( currentChar == '[' ) {
				/* Marked section.*/
				markedSection();
			} else {
				/* Some other declaration.*/
				handler.declaration( declaration() );
			}
		} else if( currentChar == '/' ) {
			/* Close tag.*/
			nextCharNoEof();
			String name = nameToken();
			whitespace();
			if( currentChar != '<' && currentChar != '>' ) {
				throw new ParseException( ParseException.Error.EXPECTED_LT_OR_GT_HERE );
			}
			handler.close( name );
		} else {
			/* Open tag.*/
			String name = nameToken();
			if( name.length() <= 0 ) {
				throw new ParseException( ParseException.Error.INVALID_TAG_NAME );
			}
			List<Attribute> attributes = new ArrayList<Attribute>();
			while( true ) {
				/* Parse attributes.*/
				whitespace();
				if( currentChar == '<' || currentChar == '>' || currentChar == '/' ) {
					break;
				} else {
					String attName = nameToken();
					whitespace();
					if( currentChar == '=' ) {
						nextCharNoEof();
						whitespace();
						attributes.add( new Attribute( attName, valueToken() ) );
					} else {
						/* Minimized attribute.*/
						attributes.add( new Attribute( attName ) );
					}
				}
			}
			handler.open( name, attributes );
			if( currentChar == '/' ) {
				// Self-closing tag.
				nextCharNoEof();
				if( currentChar != '>' ) {
					throw new ParseException( ParseException.Error.EXPECTED_GT_HERE );
				}
				handler.close( name );
			}
		}
	}

	private List<Declaration> declarationSubset() throws IOException, ParseException {
		List<Declaration> decls = new ArrayList<Declaration>();
		if( currentChar != '[' ) {
			throw new ParseException( ParseException.Error.EXPECTED_OB_HERE );
		}
		nextCharNoEof();
		while( true ) {
			whitespace();
			if( currentChar == '<' ) {
				nextCharNoEof();
				if( currentChar == '!' ) {
					nextCharNoEof();
					if( currentChar == '-' || currentChar == '>' ) {
						/* Comment.*/
						while( currentChar == '-' ) {
							nextCharNoEof();
							commentSeparator();
							whitespace();
						}
						if( currentChar != '>' ) {
							throw new ParseException( ParseException.Error.EXPECTED_GT_HERE );
						}
						nextCharNoEof();
					} else if( currentChar == '[' ) {
						/* Marked section open.*/
						nextCharNoEof();
						token( '[' );
						nextCharNoEof();
					} else {
						/* Declaration.*/
						decls.add( declaration() );
						nextCharNoEof();
					}
				} else if( currentChar == '?' ) {
					/* Processing instruction.*/
					token( '>' );
					nextCharNoEof();
				} else {
					throw new ParseException( ParseException.Error.EXPECTED_EX_OR_QM_HERE );
				}
			} else if( currentChar == '%' ) {
				/* Parameter entity reference.*/
				token( ';' );
				nextCharNoEof();
			} else if( currentChar == ']' ) {
				nextCharNoEof();
				if( currentChar == ']' ) {
					/* Marked section close.*/
					nextCharNoEof();
					if( currentChar != '>' ) {
						throw new ParseException( ParseException.Error.EXPECTED_GT_HERE );
					}
					nextCharNoEof();
				} else {
					/* End of subset.*/
					break;
				}
			} else {
				throw new ParseException( ParseException.Error.EXPECTED_LT_OR_CB_HERE );
			}
		}
		return decls;
	}
	
	private Declaration declaration() throws IOException, ParseException {
		String name = paramToken();
		if( name.length() == 0 ) {
			throw new ParseException( ParseException.Error.INVALID_DECLARATION );
		}
		List<String> params = new ArrayList<String>();
		List<Declaration> subset = null;
		while( true ) {
			whitespace();
			if( currentChar == '>' ) {
				/* End of declaration. */
				break;
			} else if( currentChar == '-' ) {
				nextCharNoEof();
				if( currentChar == '-' ) {
					/* Comment. */
					commentSeparator();
				} else if( currentChar == '(' ) {
					/* Exclusion specifier. */
					params.add( "-" + nameGroup() );
				} else {
					/* Minimization specifier. */
					params.add( "-" );
				}
			} else if( currentChar == '+' ) {
				/* Inclusion specifier. */
				nextCharNoEof();
				params.add( "+" + nameGroup() );
			} else if( currentChar == '"' || currentChar == '\'' ) {
				/* Quote-delimited param. */
				char delim = ( char ) currentChar;
				params.add( delim + valueToken() + delim );
			} else if( currentChar == '(' ) {
				/* Model or name group.*/
				params.add( nameGroup() );
			} else if( currentChar == '[' ) {
				/* Declaration subset. */
				subset = declarationSubset();
				while( true ) {
					/* Skip over parameter separators.*/
					whitespace();
					if( currentChar == '-' ) {
						nextCharNoEof();
						commentSeparator();
					} else if( currentChar == '>' ) {
						break;
					} else {
						throw new ParseException( ParseException.Error.EXPECTED_GT_HERE );
					}
				}
			} else {
				/* Unquoted param. */
				params.add( paramToken() );
			}
		}
		return new Declaration( name, params, subset );
	}

	private void markedSection() throws IOException, ParseException {
		if( currentChar != '[' ) {
			throw new ParseException( ParseException.Error.EXPECTED_OB_HERE );
		}
		nextCharNoEof();
		whitespace();
		String param = nameToken();
		whitespace();
		if( currentChar != '[' ) {
			throw new ParseException( ParseException.Error.EXPECTED_OB_HERE );
		}
		while( true ) {
			nextCharNoEof();
			if( currentChar == ']' ) {
				nextCharNoEof();
				if( currentChar == ']' ) {
					nextCharNoEof();
					whitespace();
					if( currentChar != '>' ) {
						throw new ParseException( ParseException.Error.EXPECTED_GT_HERE );
					}
					if( tokenIdx > 0 ) {
						handler.characters( param, new String( tokenBuf, 0, tokenIdx ) );
						tokenIdx = 0;
					}
					break;
				} else {
					/* False alarm.*/
					tokenBuf[ tokenIdx++ ] = ']';
					if( tokenIdx >= TOKEN_BUF_LEN ) {
						handler.characters( param, new String( tokenBuf, 0, TOKEN_BUF_LEN ) );
						tokenIdx = 0;
					}
				}
			}
			tokenBuf[ tokenIdx++ ] = ( char ) currentChar;
			if( tokenIdx >= TOKEN_BUF_LEN ) {
				handler.characters( param, new String( tokenBuf, 0, TOKEN_BUF_LEN ) );
				tokenIdx = 0;
			}
		}
	}

	/* Parse a comment from the first hyphen and generate an event. */
	private void comment() throws IOException, ParseException {
		int hyphen = currentChar;
		nextCharNoEof();
		if( hyphen != '-' || currentChar != hyphen ) {
			throw new ParseException( ParseException.Error.EXPECTED_HY_HERE );
		}
		while( true ) {
			nextCharNoEof();
			if( currentChar == '-' ) {
				nextCharNoEof();
				if( currentChar == '-' ) {
					nextCharNoEof();
					if( tokenIdx > 0 ) {
						handler.comment( new String( tokenBuf, 0, tokenIdx ) );
						tokenIdx = 0;
					}
					break;
				} else {
					/* False alarm.*/
					tokenBuf[ tokenIdx++ ] = '-';
					if( tokenIdx >= TOKEN_BUF_LEN ) {
						handler.comment( new String( tokenBuf, 0, TOKEN_BUF_LEN ) );
						tokenIdx = 0;
					}
				}
			}
			tokenBuf[ tokenIdx++ ] = ( char ) currentChar;
			if( tokenIdx >= TOKEN_BUF_LEN ) {
				handler.comment( new String( tokenBuf, 0, TOKEN_BUF_LEN ) );
				tokenIdx = 0;
			}
		}
	}

	/* Read a String, which may contain whitespace or "special" characters if quoted. */
	private String valueToken() throws IOException, ParseException {
		if( currentChar == '\'' || currentChar == '"' ) {
			int delim = currentChar;
			nextCharNoEof();
			String str = token( delim );
			nextCharNoEof();
			return str;
		} else {
			return nameToken();
		}
	}

	/* Read a String, delimited by a "special" character or whitespace. */
	private String nameToken() throws IOException, ParseException {
		DELIMIT:
		while( currentChar > 32 ) {
			switch( currentChar ) {
				case '<': case '>': case '[': case ']': case ';': case '=': case '/':
					break DELIMIT;
			}
			tokenBuf[ tokenIdx++ ] = ( char ) currentChar;
			if( tokenIdx >= TOKEN_BUF_LEN ) {
				throw new ParseException( ParseException.Error.TOKEN_TOO_LONG );
			}
			nextCharNoEof();
		}
		String str = new String( tokenBuf, 0, tokenIdx );
		tokenIdx = 0;
		return str;
	}

	/* Return a token delimited by whitespace, '>', '[' or '--comment--' */
	private String paramToken() throws IOException, ParseException {
		while( currentChar > 32 && currentChar != '>' && currentChar != '[' ) {
			if( currentChar == '-' ) {
				nextCharNoEof();
				if( currentChar == '-' ) {
					commentSeparator();
					break;
				} else {
					tokenBuf[ tokenIdx++ ] = '-';
					if( tokenIdx >= TOKEN_BUF_LEN ) {
						throw new ParseException( ParseException.Error.TOKEN_TOO_LONG );
					}
				}
			}
			tokenBuf[ tokenIdx++ ] = ( char ) currentChar;
			if( tokenIdx >= TOKEN_BUF_LEN ) {
				throw new ParseException( ParseException.Error.TOKEN_TOO_LONG );
			}
			nextCharNoEof();
		}
		String str = new String( tokenBuf, 0, tokenIdx );
		tokenIdx = 0;
		return str;
	}

	/* A name group delimited by (nested) parentheses, followed by an optional repetition. */
	private String nameGroup() throws IOException, ParseException {
		if( currentChar != '(' ) {
			throw new ParseException( ParseException.Error.EXPECTED_OP_HERE );
		}
		int level = 0;
		while( true ) {
			tokenBuf[ tokenIdx++ ] = ( char ) currentChar;
			if( tokenIdx >= TOKEN_BUF_LEN ) {
				throw new ParseException( ParseException.Error.TOKEN_TOO_LONG );
			}
			if( currentChar == '(' ) {
				level++;
			} else if( currentChar == ')' ) {
				level--;
				if( level <= 0 ) {
					break;
				}
			}
			nextCharNoEof();
		}
		nextCharNoEof();
		if( currentChar == '?' || currentChar == '+' || currentChar == '*' ) {
			/* Handle repetition operator. */
			tokenBuf[ tokenIdx++ ] = ( char ) currentChar;
			if( tokenIdx >= TOKEN_BUF_LEN ) {
				throw new ParseException( ParseException.Error.TOKEN_TOO_LONG );
			}
			nextCharNoEof();
		}
		String token = new String( tokenBuf, 0, tokenIdx );
		tokenIdx = 0;
		return token;
	}

	/* Skip a comment from the second hyphen, ie '-comment--'. */
	private void commentSeparator() throws IOException, ParseException {
		if( currentChar != '-' ) {
			throw new ParseException( ParseException.Error.EXPECTED_HY_HERE );
		}
		while( true ) {
			nextCharNoEof();
			if( currentChar == '-' ) {
				nextCharNoEof();
				if( currentChar == '-' ) {
					nextCharNoEof();
					break;
				}
			}
		}
	}

	/* Read a token delimited only by the specified character. */
	private String token( int delim ) throws IOException, ParseException {
		while( currentChar != delim ) {
			tokenBuf[ tokenIdx++ ] = ( char ) currentChar;
			if( tokenIdx >= TOKEN_BUF_LEN ) {
				throw new ParseException( ParseException.Error.TOKEN_TOO_LONG );
			}
			nextCharNoEof();
		}
		String str = new String( tokenBuf, 0, tokenIdx );
		tokenIdx = 0;
		return str;
	}

	private void whitespace() throws IOException, ParseException {
		while( currentChar <= 32 ) {
			nextCharNoEof();
		}
	}

	private void flushCharacters() throws ParseException {
		handler.characters( new String( tokenBuf, 0, tokenIdx ) );
		tokenIdx = 0;
	}

	private void nextChar() throws IOException {
		if( ( inputIdx >= inputLen ) && !fillInputBuf() ) {
			currentChar = -1;
		} else {
			currentChar = inputBuf[ inputIdx++ ];
			if( currentChar == 10 ) {
				currentLine++;
			}
		}
	}
	
	private void nextCharNoEof() throws IOException, ParseException {
		nextChar();
		if( currentChar < 0 ) {
			throw new ParseException( ParseException.Error.UNEXPECTED_END_OF_FILE );
		}
	}
	
	private boolean fillInputBuf() throws IOException {
		inputIdx = 0;
		inputLen = input.read( inputBuf, 0, INPUT_BUF_LEN );
		return inputLen > 0;
	}
}
