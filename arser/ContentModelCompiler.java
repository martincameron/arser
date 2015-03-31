
package arser;

/**
	A compiler for a string representation of a ContentModel. <p>
	The representation is the same as used the SGML/XML doctype, with
	the limitation of whitespace not being permitted, and resembles
	a regular-expression.<p>
	Examples include:
	<ul>
	<li>Empty: .
	<li>Sequence: tag1,tag2,tag3
	<li>Alternative: tag1|tag2|tag3
	<li>Zero-or-more: tag*
	<li>One-or-more: tag+
	<li>Optional: tag?
	<li>Subexpressions: (tag1,tag2)|tag3*
	</ul>
*/
public class ContentModelCompiler {
	private int idx;
	private char[] expr;
		
	/**
		@return the ContentModel for the specified expression.
	*/
	public ContentModel compile( String expression ) throws ParseException  {
		expr = expression.toCharArray();
		idx = 0;
		return compile();
	}

	private ContentModel compile() throws ParseException  {
		ContentModel model = compile2();
		while( idx < expr.length && expr[ idx ] == ',' ) {
			idx++;
			model = sequenceModel( model, compile2() );
		}
		return model;
	}
	
	private ContentModel compile2() throws ParseException  {
		ContentModel model = compile3();
		while( idx < expr.length && expr[ idx ] == '|' ) {
			idx++;
			model = alternativeModel( model, compile3() );
		}
		return model;
	}
	
	private ContentModel compile3() throws ParseException {
		ContentModel model;
		if( idx >= expr.length ) {
			throw new ParseException( ParseException.Error.UNEXPECTED_END_OF_EXPR, new String( expr ) );
		} else if( expr[ idx ] == '(' ) {
			// Subexpression.
			idx++;
			model = compile();
			if( idx >= expr.length ) {
				throw new ParseException( ParseException.Error.UNEXPECTED_END_OF_EXPR, new String( expr ) );
			}
			idx++;
		} else {
			// Single token.
			int len = 0;
			char chr = expr[ idx ];
			while( chr > 32 && "),|*+?".indexOf( chr ) < 0 ) {
				len++;
				if( idx + len >= expr.length ) break;
				chr = expr[ idx + len ];
			}
			model = tokenModel( new String( expr, idx, len ) );
			idx += len;
		}
		while( idx < expr.length ) {
			// Unary operators.
			char chr = expr[ idx ];
			if( chr == '*' || chr == '+' ) {
				idx++;
				model = repetitionModel( model, chr == '*' );
			} else if( chr == '?' ) {
				idx++;
				model = optionalModel( model );
			} else if( chr == ')' || chr == ',' || chr == '|' ) {
				break;
			} else {
				throw new ParseException( ParseException.Error.UNEXPECTED_CHAR_IN_EXPR, new String( expr ) );
			}
		}
		return model;
	}

	private ContentModel tokenModel( String token ) throws ParseException {
		if( token.length() < 1 ) {
			throw new ParseException( ParseException.Error.ZERO_LENGTH_TOKEN_IN_EXPR, token );
		} else if( ".".equals( token ) ) {
			return new EmptyModel();
		} else {
			return new TokenModel( token );
		}
	}

	private ContentModel repetitionModel( ContentModel contentModel, boolean optional ) {
		if( contentModel instanceof RepetitionModel ) {
			/* Optimize (a+*) to (a*). */
			return new RepetitionModel( ((RepetitionModel)contentModel).getModel(), optional || contentModel.empty() );
		} else if( contentModel instanceof OptionalModel ) {
			/* Optimize (a?+) to (a*). */
			return new RepetitionModel( ((OptionalModel)contentModel).getModel(), true );
		} else if( contentModel instanceof AlternativeModel ) {
			/* Recursively remove redundant repetitions, eg (a|b+|c)+ -> (a|b|c)+ */
			ContentModel leftModel = repetitionModel( ((AlternativeModel)contentModel).getLeftModel(), false );
			leftModel = ( leftModel instanceof RepetitionModel ) ? ((RepetitionModel)leftModel).getModel() : leftModel;
			ContentModel rightModel = repetitionModel( ((AlternativeModel)contentModel).getRightModel(), false );
			rightModel = ( rightModel instanceof RepetitionModel ) ? ((RepetitionModel)rightModel).getModel() : rightModel;
			return new RepetitionModel( new AlternativeModel( leftModel, rightModel ), optional || contentModel.empty() );
		} else if( contentModel instanceof EmptyModel ) {
			/* Optimize (.+) to (.). */
			return contentModel;
		} else {
			return new RepetitionModel( contentModel, optional );
		}
	}

	private ContentModel optionalModel( ContentModel contentModel ) {
		if( contentModel instanceof RepetitionModel ) {
			/* Optimize (a+?) to (a*). */
			return new RepetitionModel( ((RepetitionModel)contentModel).getModel(), true );
		} else if( contentModel.empty() ) {
			/* Already optional. */
			return contentModel;
		} else {
			return new OptionalModel( contentModel );
		}
	}

	private ContentModel alternativeModel( ContentModel lhs, ContentModel rhs ) {
		if( lhs instanceof EmptyModel ) {
			/* Optimize (.|a) to (a?). */
			return( optionalModel( rhs ) );
		} else if( lhs instanceof OptionalModel ) {
			/* Move optionality to rhs of expression. */
			lhs = ((OptionalModel)lhs).getModel();
			rhs = optionalModel( rhs );
		}
		if( rhs instanceof EmptyModel ) {
			/* Optimize (a|.) to (a?). */
			return( optionalModel( lhs ) );
		} else if( rhs instanceof OptionalModel ) {
			/* Move optionality to top of expression so (a|b?) becomes (a|b)? */
			return optionalModel( new AlternativeModel( lhs, ((OptionalModel)rhs).getModel() ) );
		} else {
			return new AlternativeModel( lhs, rhs );
		}
	}

	private ContentModel sequenceModel( ContentModel lhs, ContentModel rhs ) {
		if( lhs instanceof EmptyModel ) {
			return rhs;
		} else if( rhs instanceof EmptyModel ) {
			return lhs;
		} else {
			return new SequenceModel( lhs, rhs );
		}
	}
}
