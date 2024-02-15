package plc.project;

import java.util.List;

//do i need these imports?
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
        //return new Ast.Statement.Expression();
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
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
        //check how many operations there are
        if (tokens.has(1)) {
            //check operator of second token
            String check = tokens.get(1).getLiteral();
            if (check.matches("[*/^]"))
                return parseMultiplicativeExpression();
            else if (check.matches("[+-]"))
                return parseAdditiveExpression();
            else if (check.matches("[<>]|(==)|(!=)"))
                return parseComparisonExpression();
            else if (check.matches("(&&)|(||)"))
                return parseLogicalExpression();
        }
        else {
            return parsePrimaryExpression();
        }
        return null;
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        //throw new UnsupportedOperationException(); TODO
        StringBuilder lhs = new StringBuilder("");
        Ast.Expression prim1 = null;
        Ast.Expression prim2 = null;
        if (peek(Token.Type.IDENTIFIER)) {
            lhs.append(tokens.get(0).getLiteral());
            prim1 = parsePrimaryExpression();
        }
        //find operator
        StringBuilder op = new StringBuilder("");
        if (peek(Token.Type.OPERATOR)) {
            if (peek("&&") || peek("||")) {
                op.append(tokens.get(0).getLiteral().toString());
                match(Token.Type.OPERATOR);
            }
        }
        //find last expression
        StringBuilder rhs = new StringBuilder("");
        if (peek(Token.Type.IDENTIFIER)) {
            rhs.append(tokens.get(0).getLiteral());
            prim2 = parsePrimaryExpression();
        }

        //return new statement
        return new Ast.Expression.Binary(op.toString(), prim1, prim2);
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        //throw new UnsupportedOperationException(); TODO
        StringBuilder lhs = new StringBuilder("");
        Ast.Expression prim1 = null;
        Ast.Expression prim2 = null;
        if (peek(Token.Type.IDENTIFIER)) {
            lhs.append(tokens.get(0).getLiteral());
            prim1 = parsePrimaryExpression();
        }
        //find operator
        StringBuilder op = new StringBuilder("");
        if (peek(Token.Type.OPERATOR)) {
            if (peek("<") || peek(">") || peek("==") || peek("!=")) {
                op.append(tokens.get(0).getLiteral().toString());
                match(Token.Type.OPERATOR);
            }
        }
        //find last expression
        StringBuilder rhs = new StringBuilder("");
        if (peek(Token.Type.IDENTIFIER)) {
            rhs.append(tokens.get(0).getLiteral());
            prim2 = parsePrimaryExpression();
        }

        //return new statement
        return new Ast.Expression.Binary(op.toString(), prim1, prim2);
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        StringBuilder lhs = new StringBuilder("");
        Ast.Expression prim1 = null;
        Ast.Expression prim2 = null;
        if (peek(Token.Type.IDENTIFIER)) {
            lhs.append(tokens.get(0).getLiteral());
            prim1 = parsePrimaryExpression();
        }
        //find operator
        StringBuilder op = new StringBuilder("");
        if (peek(Token.Type.OPERATOR)) {
            if (peek("+") || peek("-")) {
                op.append(tokens.get(0).getLiteral().toString());
                match(Token.Type.OPERATOR);
            }
        }
        //find last expression
        StringBuilder rhs = new StringBuilder("");
        if (peek(Token.Type.IDENTIFIER)) {
            rhs.append(tokens.get(0).getLiteral());
            prim2 = parsePrimaryExpression();
        }

        //return new statement
        return new Ast.Expression.Binary(op.toString(), prim1, prim2);
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        //find first expression
        StringBuilder lhs = new StringBuilder("");
        Ast.Expression prim1 = null;
        Ast.Expression prim2 = null;
        if (peek(Token.Type.IDENTIFIER)) {
            lhs.append(tokens.get(0).getLiteral());
            prim1 = parsePrimaryExpression();
        }
        //find operator
        StringBuilder op = new StringBuilder("");
        if (peek(Token.Type.OPERATOR)) {
            if (peek("*") || peek("/") || peek("^")) {
                op.append(tokens.get(0).getLiteral().toString());
                match(Token.Type.OPERATOR);
            }
        }
        //find last expression
        StringBuilder rhs = new StringBuilder("");
        if (peek(Token.Type.IDENTIFIER)) {
            rhs.append(tokens.get(0).getLiteral());
            prim2 = parsePrimaryExpression();
        }

        //return new statement
        return new Ast.Expression.Binary(op.toString(), prim1, prim2);
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
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
                //normal identifier, TODO: check for recursive logical expressions
                match(Token.Type.IDENTIFIER);
                return new Ast.Expression.Access(Optional.empty(), temp);
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
        return new Ast.Expression.Literal(null);

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
        //weird bug where i remove quotes are char disappears for escape chars
        if (temp.length() == 3) {
            var check3 = temp.charAt(1);
            return temp.charAt(1);
        }
        else {
            //char is an escape
            char check1 = temp.charAt(1);
            char next = temp.charAt(2);
            if (check1 != '\\') {
                return 'x';
            }
            else {
                //check if check2 is escape
                if (next == 'b') {
                    return '\b';
                }
                else if (next == 'n') {
                    return '\n';
                }
                else if (next == 'r') {
                    return '\r';
                }
                else if (next == 't') {
                    return '\t';
                }
                else if (next == '\'') {
                    return '\'';
                }
                else if (next == '\"') {
                    return '\"';
                }
                else if (next == '\\') {
                    return '\\';
                }
            }
        }
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
