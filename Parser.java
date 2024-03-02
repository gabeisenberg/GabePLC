package plc.project;

import java.util.*;

//do i need these imports?
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();

        while (tokens.has(0)) {
            if (peek("VAL") || peek("VAR")) {
                globals.add(parseGlobal());
            } else if (peek("FUN")) {
                functions.add(parseFunction());
            } else {
                throw new ParseException("Unexpected token: " + tokens.get(0).getLiteral(), tokens.get(0).getIndex());
            }
        }

        return new Ast.Source(globals, functions);
    }


    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if (peek("VAL")) {
            return parseImmutable();
        } else if (peek("VAR")) {
            return parseMutable();
        } else {
            throw new ParseException("Expected 'VAL' or 'VAR'", tokens.get(0).getIndex());
        }
    }


    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        match("VAR");
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
        match("=");
        Ast.Expression value = parseExpression();
        match(";");
        return new Ast.Global(name, true, Optional.of(value));
    }

    public Ast.Global parseImmutable() throws ParseException {
        match("VAL");
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
        match("=");
        Ast.Expression value = parseExpression();
        match(";");
        return new Ast.Global(name, false, Optional.of(value));
    }


    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        match("FUN");
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
        match("(");
        // Assuming no parameters for simplicity; adjust as needed
        match(")");
        match("DO");
        List<Ast.Statement> statements = parseBlock();
        match("END");
        return new Ast.Function(name, Collections.emptyList(), statements); // Adjust for parameters
    }


    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        return statements;
    }


    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        Ast.Statement res = null;
        //check for assignment
        //check for first expression
        String lhs = tokens.get(0).getLiteral();
        Ast.Expression exp1 = parseExpression();
        //check for =
        if (peek("=")) {
            match("=");
            //get next name of rhs var
            String rhs = tokens.get(0).getLiteral();
            Ast.Expression exp2 = parseExpression();
            res = new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), lhs),
                    new Ast.Expression.Access(Optional.empty(), rhs)
            );
        }
        else {
            //not assignment
            res = new Ast.Statement.Expression(exp1);
        }
        //MUST match ;
        if (peek(";")) {
            match(";");
        }
        else {
            int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Missing ;", size);
            //throw new ParseException("Missing ;", tokens.index);
        }
        return res;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */

    //"Let identifier ('=' expression)? ';'
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        if (!match(Token.Type.IDENTIFIER)){
            //throw a parse exception
            throw new ParseException("Expected identifier but was not provided", -1);
        }
        String name = tokens.get(-1).getLiteral();
        //do not need to test for reserved keywords yet
        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")){
            value = Optional.of(parseExpression());
        }
        return new Ast.Statement.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        //get first comp exp
        Ast.Expression lhs = parseComparisonExpression();
        //match logical operator
        if ((!(peek("&&") || peek("||")))) {
            //check next operation
            return lhs;
            //throw new ParseException("Missing logical operator", tokens.index);
        }
        String op = tokens.get(0).getLiteral();
        match(Token.Type.OPERATOR);
        //get second comp exp
        Ast.Expression rhs = parseLogicalExpression();
        return new Ast.Expression.Binary(op, lhs, rhs);
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        //get first add exp
        Ast.Expression lhs = parseAdditiveExpression();
        //match logical operator
        if (!(peek("<") || peek(">") || peek("==") || peek("!="))) {
            //check next operation
            return lhs;
            //throw new ParseException("Missing comparison operator", tokens.index);
        }
        String op = tokens.get(0).getLiteral();
        match(Token.Type.OPERATOR);
        //get second add exp
        Ast.Expression rhs = parseComparisonExpression();
        return new Ast.Expression.Binary(op, lhs, rhs);
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        //get first multtplicative exp
        Ast.Expression lhs = parseMultiplicativeExpression();
        //match logical operator
        if (!(peek("+") || peek("-"))) {
            //check next operation
            return lhs;
            //throw new ParseException("Missing multiplicative operator", tokens.index);
        }
        String op = tokens.get(0).getLiteral();
        match(Token.Type.OPERATOR);
        //get second multiplicative exp
        Ast.Expression rhs = parseAdditiveExpression();
        return new Ast.Expression.Binary(op, lhs, rhs);
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        //get first multtplicative exp
        Ast.Expression lhs = parsePrimaryExpression();
        //match logical operator
        if (!(peek("*") || peek("/") || peek("^"))) {
            //check next operation
            return lhs;
            //throw new ParseException("Missing mu operator", tokens.index);
        }
        String op = tokens.get(0).getLiteral();
        match(Token.Type.OPERATOR);
        //get second multiplicative exp
        Ast.Expression rhs = parseMultiplicativeExpression();
        return new Ast.Expression.Binary(op, lhs, rhs);
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (!tokens.has(0)) {
            //no operand to get
            int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Missing Operand", size);
        }
        String temp = tokens.get(0).getLiteral();
        if (peek(Token.Type.IDENTIFIER)) {
            if (peek("NIL")) {
                match("NIL");
                return new Ast.Expression.Literal(null);
            }
            else if (peek("TRUE")) {
                match("TRUE");
                return new Ast.Expression.Literal(Boolean.TRUE);
            }
            else if (peek("FALSE")) {
                match("FALSE");
                return new Ast.Expression.Literal(Boolean.FALSE);
            }
            else {
                //get identifier
                match(Token.Type.IDENTIFIER);
                //Ast.Expression id = parsePrimaryExpression();
                //check for list or function
                if (peek("(")) {
                    match("(");
                    //function, create list of parameters
                    ArrayList<Ast.Expression> parameters = new ArrayList<>();
                    while (!peek(")")) {
                        Ast.Expression nextExp = parseExpression();
                        parameters.add(nextExp);
                        //check for separating comma
                        if (peek(",")) {
                            match(",");
                            if (peek(")")) {
                                int size = tokens.get(-1).getIndex();
                                throw new ParseException("Trailing ,", size);
                            }
                        }
                        /*else if (!peek(")")) {
                            int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                            throw new ParseException("Missing ,", size);
                            //throw new ParseException("Missing ,", tokens.get(0).getIndex() + tokens.get(0).getLiteral().length());
                        }*/
                    }
                    if (!peek(")")) {
                        int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                        throw new ParseException("Missing )", size);
                        //throw new ParseException("Missing )", tokens.index);
                    }
                    match(")");
                    return new Ast.Expression.Function(temp, parameters);
                }
                else if (peek("[")) {
                    match("[");
                    //list, get next expression
                    String nextName = tokens.get(0).getLiteral();
                    Ast.Expression nextExp = parseExpression();
                    if (peek("]")) {
                        match("[");
                    }
                    else {
                        int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                        throw new ParseException("Missing ]", size);
                        //throw new ParseException("Missing ]", tokens.get(0).getIndex() + tokens.get(0).getLiteral().length());
                    }
                    return new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), nextName)), temp);
                }
                else {
                    //normal identifier
                    return new Ast.Expression.Access(Optional.empty(), temp);
                }
            }
        }
        else if (peek(Token.Type.INTEGER)) {
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(new BigInteger(temp));
        }
        else if (peek(Token.Type.DECIMAL)) {
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(new BigDecimal(temp));
        }
        else if (peek(Token.Type.CHARACTER)) {
            match(Token.Type.CHARACTER);
            //edit char
            return new Ast.Expression.Literal(editChar(temp));
        }
        else if (peek(Token.Type.STRING)) {
            match(Token.Type.STRING);
            //edit the string
            return new Ast.Expression.Literal(editString(temp));
        }
        else if (peek(Token.Type.OPERATOR) && peek("(")) {
            //create group expression
            match("(");
            Ast.Expression inside = parseExpression();
            if (peek(")")) {
                match(")");
                return new Ast.Expression.Group(inside);
            }
            else {
                // (func
                int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                throw new ParseException("Missing )", size);
            }
        }
        int size = tokens.get(0).getIndex();
        throw new ParseException("Invalid Expression", size);

    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        //throw new UnsupportedOperationException(); TODO (in lecture)
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        //throw new UnsupportedOperationException(); TODO (in lecture)
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    //helper functions i made

    public char editChar(String temp) {
        char test = temp.charAt(2);
        int test2 = temp.length();
        if (temp.length() == 3) {
            // Handles a single character or an escaped character like '\n', '\t', etc.
            return temp.charAt(1);
        } else if (test == 'u') {
            if (test2 == 8) {
                return '\u000B'; // Vertical tab
            }

            // Add more conditions if other specific Unicode characters need to be supported
        }else if (temp.length() == 4 && temp.charAt(1) == '\\') {
            // Handles escape sequences
            switch (temp.charAt(2)) {
                case 'n':
                    return '\n'; // Newline
                case 't':
                    return '\t'; // Tab
                case 'r':
                    return '\r'; // Carriage return
                case 'b':
                    return '\b'; // Backspace
                case 'f':
                    return '\f'; // Form feed
                // Add cases for other escape sequences as needed
                default:
                    return 'x'; // Represents an unrecognized escape sequence
            }
        }
        // Fallback for any other scenarios not covered
        return 'x';
    }





    public String editString(String temp) {
        //remove quotes
        String noQuotes = temp.substring(1, temp.length() - 1);
        //convert literal escape sequences into real escapes
        StringBuilder res = new StringBuilder("");
        String escapes = "[bnrt'\"\\\\]";
        for (int i = 0; i < noQuotes.length(); i++) {
            char c = noQuotes.charAt(i);
            //check for first forward slash
            if (c == '\\') {
                if (i == noQuotes.length() - 1) {
                    //just forward slash, append and move on
                    res.append(c);
                }
                else {
                    //next char may match escape sequence
                    char next = noQuotes.charAt(i + 1);
                    if ((next + "").matches(escapes)) {
                        //ONLY append escape
                        if (next == 'b') {
                            res.append('\b');
                        }
                        else if (next == 'n') {
                            res.append('\n');
                        }
                        else if (next == 'r') {
                            res.append('\r');
                        }
                        else if (next == 't') {
                            res.append('\t');
                        }
                        else if (next == '\'') {
                            res.append('\'');
                        }
                        else if (next == '\"') {
                            res.append('\"');
                        }
                        else if (next == '\\') {
                            res.append('\\');
                        }
                        i++;
                    }
                    else {
                        //just forward slash
                        res.append(c);
                    }
                }
            }
            else {
                res.append(c);
            }
        }
        return res.toString();
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}