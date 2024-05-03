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
        boolean mutable = match("VAR");
        if (!mutable) {
            match("VAL");
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
        String type = "Any"; // Default type
        if (match(":")) {
            type = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        }
        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        if (!match(";")) {
            throw new ParseException("Missing :", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        return new Ast.Global(name, type, mutable, value);
    }




    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        match("LIST"); // Match the LIST keyword.
        String name = tokens.get(0).getLiteral(); // Get the list name.
        match(Token.Type.IDENTIFIER); // Ensure it's an identifier.
        match("="); // Match the equals sign.
        match("["); // Start of the list.
        List<Ast.Expression> values = new ArrayList<>();
        if (!peek("]")) { // Check if the list is not empty.
            do {
                values.add(parseExpression()); // Parse each expression in the list.
            } while (match(",")); // Continue if there's a comma.
        }
        match("]"); // End of the list.
        return new Ast.Global(name, true, Optional.of(new Ast.Expression.PlcList(values))); // Since it's a list, mutable is assumed true.
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
        List<String> parameters = new ArrayList<>(); // Assuming parameters are parsed elsewhere
        List<String> parameterTypes = new ArrayList<>(); // Assuming parameter types are parsed elsewhere
        match(")");
        Optional<String> returnType = Optional.of("Any"); // Default return type
        if (match(":")) {
            returnType = Optional.of(tokens.get(0).getLiteral());
            match(Token.Type.IDENTIFIER);
        }
        if (!match("DO")) {
            int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Missing Operand", size);
        }
        List<Ast.Statement> statements = parseBlock();
        match("END");
        return new Ast.Function(name, parameters, parameterTypes, returnType, statements);
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


    public Ast.Statement parseStatement() throws ParseException {
        if (match("IF")) {
            return parseIfStatement();
        } else if (match("WHILE")) {
            return parseWhileStatement();
        } else if (match("RETURN")) {
            return parseReturnStatement();
        } else if (match("LET")) {
            return parseDeclarationStatement();
        } else if (peek("SWITCH"))
            return parseSwitchStatement();

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier at the beginning of the statement", tokens.get(0).getIndex());
        }

        String identifier = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        if (match("=")) {
            // It's an assignment statement
            Ast.Expression value = parseExpression();
            if (!match(";")) {
                throw new ParseException("Missing ;", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            return new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), identifier), value);
        } else if (match("(")) {
            // It's a function call
            List<Ast.Expression> arguments = new ArrayList<>();
            if (!peek(")")) {
                do {
                    arguments.add(parseExpression());
                } while (match(","));
            }
            match(")");
            match(";"); // Function calls must end with a semicolon
            return new Ast.Statement.Expression(new Ast.Expression.Function(identifier, arguments));
        }
        else if (match("[")) {
            //match("[");
            //list, get next expression
            Ast.Expression nextExp = parseExpression();
            if (peek("]")) {
                match("]");
            }
            else {
                int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                throw new ParseException("Missing ]", size);
            }
            if (match("=")) {
                Ast.Expression value = parseExpression();
                if (!match(";")) {
                    int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                    throw new ParseException("Missing ;", size);
                }
                return new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.of(nextExp), identifier), value);
            }
            //return new Ast.Statement.Declaration(identifier, Optional.of(Ast.Expression.Access(Optional.of(nextExp)));
            /*Ast.Expression offset = parseExpression();
            if (match("=")) {
                Ast.Expression value = parseExpression();
                return new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.of(offset), identifier), value);
            }*/
            /*match("[");
            //list, get next expression
            Ast.Expression nextExp = parseExpression();
            if (peek("]")) {
                match("]");
            }
            else {
                int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                throw new ParseException("Missing ]", size);
                //throw new ParseException("Missing ]", tokens.get(0).getIndex() + tokens.get(0).getLiteral().length());
            }
            if (match("=")) {
                Ast.Expression value = parseExpression();
                return new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.of(nextExp), identifier), value);
            }
            else {
                //declaration
                return new Ast.Statement.Declaration(identifier, Optional.of(nextExp));
            }*/
        }
        // It's a simple expression statement or an error
        if (!match(";")) {
            throw new ParseException("Missing ;", 1);
        }
        match(";"); // Assuming it was just a variable access
        return new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), identifier));
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
        Optional<Ast.Expression> value = Optional.empty();
        Optional<String> typeName = Optional.empty();  // Initialize type name as empty
        if (match(":")) {  // Check if there is a type specifier
            if (!match(Token.Type.IDENTIFIER)){
                //throw a parse exception if type identifier is missing after ':'
                throw new ParseException("Expected type identifier but was not provided", -1);
            }
            typeName = Optional.of(tokens.get(-1).getLiteral());  // Capture the type name
        }
        if (match("=")){
            value = Optional.of(parseExpression());
        }
        return new Ast.Statement.Declaration(name, typeName,value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Expression condition = parseExpression();
        if (!match("DO")) {
            int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Missing DO", size);
        }
        List<Ast.Statement> thenStatements = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            thenStatements.add(parseStatement());
        }
        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            while (!peek("END")) {
                elseStatements.add(parseStatement());
            }
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END'", tokens.get(-1).getIndex());
        }
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        if (!match("SWITCH")) {
            throw new ParseException("Expected 'SWITCH'", tokens.get(0).getIndex());
        }

        List<Ast.Statement.Case> cases = new ArrayList<>();
        Ast.Expression condition = parseExpression();

        while (match("CASE")) { Ast.Expression caseExpression = parseExpression();
            if (!match(":")) {
                throw new ParseException("Expected ':'", tokens.get(0).getIndex());
            }
            List<Ast.Statement> caseStatements = parseBlock();
            cases.add(new Ast.Statement.Case(Optional.of(caseExpression), caseStatements));
        }
        if (!match("DEFAULT")) {
            throw new ParseException("Missing 'DEFAULT", tokens.get(0).getIndex());
        }
        if (!match(":")) {
            throw new ParseException("Expected ':'", tokens.get(0).getIndex());
        }
        List<Ast.Statement> defaultStatements = parseBlock();
        cases.add(new Ast.Statement.Case(Optional.empty(), defaultStatements));
        if (!match("END")) {
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex());
        }
        return new Ast.Statement.Switch(condition, cases);
    }


    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        if (!match("CASE")) {throw new ParseException("Expected CASE", tokens.get(0).getIndex());}
        if (!match(":")) {throw new ParseException("Expected semicolon", tokens.get(0).getIndex());}
        Ast.Expression cExpression = parseExpression();
        List<Ast.Statement> cStatements = parseBlock();
        return new Ast.Statement.Case(Optional.of(cExpression), cStatements);
    }

    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expression condition = parseExpression();

        if (!match("DO")) {
            int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Missing DO", size);
        }
        List<Ast.Statement> statements = parseBlock();

        if (!match("END")) {
            throw new ParseException("No END", tokens.get(0).getIndex());
        }
        return new Ast.Statement.While(condition, statements);
    }

    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN"); // Match the RETURN keyword.
        Ast.Expression value = parseExpression(); // Parse the return expression.
        match(";"); // Match the semicolon to end the return statement.
        return new Ast.Statement.Return(value);
    }

// You'll need to integrate these methods into your parsing logic where appropriate,
// likely within the parseStatement() method where you determine the type of statement
// based on the current token and delegate to the specific parse method.


    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */



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
                        match("]");
                    }
                    else {
                        int size = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                        throw new ParseException("Missing ]", size);
                    }
                    return new Ast.Expression.Access(Optional.of(nextExp), temp);
                    /*match("[");
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
                    return new Ast.Expression.Access(Optional.of(nextExp), nextName);*/
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