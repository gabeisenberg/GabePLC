package plc.project;

import java.util.List;
import java.util.ArrayList;

//import regex
//import java.util.regex.Pattern;
//import java.util.regex.Matcher;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> res = new ArrayList<>();
<<<<<<< HEAD
        while (chars.index < chars.input.length()) {
            //skip white space
            if (peek("[\b\n\r\t ]"))
                lexEscape();
            else
                res.add(lexToken());
=======
        while (chars.index != chars.input.length()) {
            //skip white space
            if (peek("[\b\n\r\t ]"))
                lexEscape(); //DO NOT COUNT WHITESPACE
            else
                res.add(lexToken()); //we should always keep building to the same token
>>>>>>> 899ff17 (initial)
        }
        return res;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("\""))
<<<<<<< HEAD
            return lexString();
        else if (peek("\'"))
            return lexCharacter();
        else if (peek("[1-9-]"))
            return lexNumber();
        else if (peek("[A-Za-z@]"))
            return lexIdentifier();
        return lexOperator();
    }

    public Token lexIdentifier() {
        match("[A-Za-z@]");
=======
            return lexString(); //a " indicates a string
        else if (peek("\'"))
            return lexCharacter(); //a ' indicates a character
        else if (peek("[0-9-]"))
            return lexNumber(); //must be a number
        else if (peek("[A-Za-z@]"))
            return lexIdentifier(); //identifier has letters or @ at first
        else {
            return lexOperator(); //if not anything else must be an operator
        }
    }

    public Token lexIdentifier() {
        match("[A-Za-z@]"); //
>>>>>>> 899ff17 (initial)
        while (peek("[A-Za-z0-9_-]")) {
            match("[A-Za-z0-9_-]");
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
<<<<<<< HEAD
        //check if number is after .
=======
        // Check if number is after .
>>>>>>> 899ff17 (initial)
        String test = chars.input.substring(chars.index);
        if (test.contains(".")) {
            int num = test.indexOf(".") + 1;
            int num2 = test.length();
            if (num2 > num) {
                char temp = test.charAt((test.indexOf(".") + 1));
                if (test.length() > test.indexOf(".") + 1 && Character.isDigit(temp))
                    return lexDecimal();
            }
        }
<<<<<<< HEAD
        match("[1-9-]");
        while (peek("[0-9]")) {
            match("[0-9]");
        }
        return chars.emit(Token.Type.INTEGER);
=======

        // Check for negative sign
        boolean negative = false;
        if (peek("-")) {
            match("-");
            negative = true;
        }

        // Check for leading '0'
        if (peek("0")) {
            match("0");
            return chars.emit(Token.Type.INTEGER);
        }

        match("[1-9]");
        while (peek("[0-9]")) {
            match("[0-9]");
        }

        if (negative) {
            return chars.emit(Token.Type.INTEGER);
        } else {
            return chars.emit(Token.Type.INTEGER);
        }
>>>>>>> 899ff17 (initial)
    }


    public Token lexDecimal() {
<<<<<<< HEAD
        match("[1-9-]");
=======
        match("[0-9-]");
>>>>>>> 899ff17 (initial)
        while (peek("[0-9]")) {
            match("[0-9]");
        }
        if (peek("\\.")) {
            match("\\.");
        }
        while (peek("[0-9]")) {
            match("[0-9]");
        }
        return chars.emit(Token.Type.DECIMAL);
    }

    public Token lexCharacter() {
        match("\'");
        if (peek("\'")) {
            //empty character
            throw new ParseException("Empty character!", chars.index);
        }
        if (peek("[^\\\\]")) {
            //not escape character
            match("[^\\\\]");
        }
        else {
            //escape character
            match("[\\\\]");
            if (peek("[bnrt\"\'\\\\]")) {
                match("[bnrt\"\'\\\\]");
            }
            else {
                throw new ParseException("Invalid escape!", chars.index);
            }
        }
        //check final quotation
        if (peek("\'")) {
            match("\'");
            return chars.emit(Token.Type.CHARACTER);
        }
        else {
            //unterminated string
            throw new ParseException("Unterminated character!", chars.index);
        }
    }

    public Token lexString() {
        match("\"");
        while(peek("[^\"]")) {
            if (peek("[^\\\\]")) {
                //if does not start with escape sequence
                match("[^\\\\]");
            }
            else {
                //starts with escape sequence
                match("[\\\\]");
                //check if next character is valid escape
                if (peek("[bnrt\"\'\\\\]")) {
                    match("[bnrt\"\'\\\\]");
                }
                else {
                    //invalid escape
                    throw new ParseException("Invalid escape!", chars.index);
                }
            }
        }
        //check final quotation
        if (peek("\"")) {
            match("\"");
            return chars.emit(Token.Type.STRING);
        }
        else {
            //unterminated string
            throw new ParseException("Unterminated string!", chars.index);
        }
    }

    public void lexEscape() {
        //match("[\b\n\r\t ]");
        //bug where i have to remove whitespace
        chars.index++;
    }

    public Token lexOperator() {
<<<<<<< HEAD
        if (peek("=", "="))
=======
        if (peek("=", "=")) //these operators were given to us
>>>>>>> 899ff17 (initial)
            match("=", "=");
        else if (peek("!", "="))
            match("!", "=");
        else if (peek("&", "&"))
            match("&", "&");
        else if (peek("|", "|"))
            match("|", "|");
        else
            match(".");
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
<<<<<<< HEAD
//     * return true if the next characters are {@code 'a', 'b', 'c'}.
=======
     //     * return true if the next characters are {@code 'a', 'b', 'c'}.
>>>>>>> 899ff17 (initial)
     */
    public boolean peek(String... patterns) {
        for (int i=0; i < patterns.length; i++){
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek){
            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}