package pw.aru.hungergames.tuples;

import org.intellij.lang.annotations.MagicConstant;
import pw.aru.hungergames.tuples.data.*;
import pw.aru.hungergames.tuples.lexer.Position;
import pw.aru.hungergames.tuples.lexer.Token;
import pw.aru.hungergames.tuples.lexer.TokenType;
import pw.aru.hungergames.tuples.lexer.TupleLexer;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>Parser that converts either a {@link String}, an {@link TupleLexer} or a {@link List}{@literal <}{@link Token}{@literal >} to a {@link Tuple}.</p>
 * <p><b>Summary of the {@link TupleParser} notation:</b></p>
 * <p>
 * <b>{@link Text}</b>: {@code smart text, "quoted example", 'single quotes', `markdown code-quote`, """raw quotes"""}
 * <br>Represents a piece of raw text.
 * <br>({@code \n, \r, \b, \t, \f} and unicode surrogates are available for smart and simple quotes.)
 * <br>(if the text is quoted with multiple quotes, like {@code ""raw quote""} or {@code '''alternate'''}, text will not be escaped.)
 * <p>
 * <b>{@link Tuple}</b>: {@code { ... content ... }} and {@code ( ... content ... )} (smart tuples)
 * <br>Tuples are Lists of {@link Obj}s, which can be {@link Pair}s, Texts or other Tuples.
 * <br>A feature of Smart Tuples is unboxing itself if it contains a single {@link Text} or {@link Tuple} inside.
 * <p>
 * <b>{@link Pair}</b>: {@code key: value} and {@code key = value}
 * <br>Pairs can be used to associate a specific value (an {@link Obj}) to a name ({@link String}).
 * <br>(by default, a pair with a value of pair gets its value transformed into a singleton Tuple with the original value.)
 * <p>
 * <b>Implicit {@link Tuple}</b>: {@code attribute 1, attribute 2, attribute 3; attribute 4, attribute 5}
 * <br>If a semicolon is detected, all commas will automatically create implicit tuples.
 * <br>the above notation will produce the same result as: {@code (attribute 1, attribute 2, attribute 3), (attribute 4, attribute 5)}
 * <p>
 * <p>
 * <p><b>Examples of the {@link TupleParser} notation:</b></p>
 * <p>
 * Example 1: <pre>"key1": "value1", "key2": "value2", "attribute1", "attribute2"</pre>
 * Example 2: <pre>flags: (smartTuples: true, implicitTuples: true, rawPairs: true)</pre>
 * Example 3: <pre>role: (name: user, tier: 0)</pre>
 * Example 4: <pre>roles: user, visitor; privileges: admin, quick support</pre>
 *
 * @see ParserOptions#NO_SMART_TUPLES
 * @see ParserOptions#NO_IMPLICIT_TUPLES
 * @see ParserOptions#RAW_PAIRS
 * @see TupleParser#parse()
 */
public class TupleParser {
    private final boolean semicolons, smartTuples, implicitTuples, rawPairs;
    //yes, a LinkedList. why? because I need Queue capabilities.
    private final LinkedList<Token> tokens;

    /**
     * Parses the tokens, using the provided parsing options.
     *
     * @param tokens the tokens to be parsed.
     * @param flags  the parsing flags.
     * @see ParserOptions
     * @see ParserOptions#NO_SMART_TUPLES
     * @see ParserOptions#NO_IMPLICIT_TUPLES
     * @see ParserOptions#RAW_PAIRS
     * @see TupleParser#parse()
     */
    public TupleParser(List<Token> tokens, @MagicConstant(flagsFromClass = ParserOptions.class) int flags) {
        this.tokens = new LinkedList<>(tokens);

        this.semicolons = tokens.stream().anyMatch(token -> token.getType() == TokenType.SEMICOLON);
        if (tokens.isEmpty()) tokens.add(new Token(new Position(0, 0, 0), TokenType.EOF));

        this.smartTuples = (flags & ParserOptions.NO_SMART_TUPLES) == 0;
        this.implicitTuples = (flags & ParserOptions.NO_IMPLICIT_TUPLES) == 0;
        this.rawPairs = (flags & ParserOptions.RAW_PAIRS) == ParserOptions.RAW_PAIRS;
    }

    /**
     * Parses the tokens generated by the lexer, using the provided parsing options.
     *
     * @param lexer the lexer which tokens will be parsed.
     * @param flags the parsing flags.
     * @see ParserOptions
     * @see ParserOptions#NO_SMART_TUPLES
     * @see ParserOptions#NO_IMPLICIT_TUPLES
     * @see ParserOptions#RAW_PAIRS
     * @see TupleParser#parse()
     */
    public TupleParser(TupleLexer lexer, @MagicConstant(flagsFromClass = ParserOptions.class) int flags) {
        this(lexer.getTokens(), flags);
    }

    /**
     * Creates a new parser, lexing the String first, and using the provided parsing options.
     *
     * @param s     the string to be parsed.
     * @param flags the parsing flags.
     * @throws SyntaxException if the string is not in a valid notation.
     * @see ParserOptions
     * @see ParserOptions#NO_SMART_TUPLES
     * @see ParserOptions#NO_IMPLICIT_TUPLES
     * @see ParserOptions#RAW_PAIRS
     * @see TupleParser#parse()
     */
    public TupleParser(String s, @MagicConstant(flagsFromClass = ParserOptions.class) int flags) {
        this(new TupleLexer(s), flags);
    }

    /**
     * Parses the tokens, using the default parsing options.
     *
     * @param tokens the tokens to be parsed.
     * @see TupleParser#parse()
     */
    public TupleParser(List<Token> tokens) {
        this(tokens, ParserOptions.DEFAULT);
    }

    /**
     * Parses the tokens generated by the lexer, using the default parsing options.
     *
     * @param lexer the lexer which tokens will be parsed.
     * @see TupleParser#TupleParser(TupleLexer, int)
     * @see TupleParser#parse()
     */
    public TupleParser(TupleLexer lexer) {
        this(lexer, ParserOptions.DEFAULT);
    }

    /**
     * Creates a new parser, lexing the String first, and using the default parsing options.
     *
     * @param s the string to be parsed.
     * @throws SyntaxException if the string is not in a valid notation.
     * @see TupleParser#TupleParser(String, int)
     * @see TupleParser#parse()
     */
    public TupleParser(String s) {
        this(s, ParserOptions.DEFAULT);
    }

    /**
     * Parses the content to a {@link Tuple}.
     *
     * @return the result of the parsing
     * @throws SyntaxException if the string is not in a valid notation.
     * @see Obj
     * @see Tuple
     */
    public Tuple parse() {
        if (tokens.peek().is(TokenType.EOF)) return new LinkedTuple();

        Tuple tuple = parseTuple(null, true, false);
        if (tuple.isSingleton()) {
            Obj obj = tuple.firstArg();
            if (obj.isTuple()) return obj.asTuple();
        }

        return tuple;
    }

    private Obj parseOnce(boolean root) {
        Token token = tokens.poll();

        switch (token.getType()) {
            case LINE: {
                return parseOnce(root);
            }
            case LEFT_PAREN: {
                return parseSmartTuple();
            }
            case LEFT_BRACE: {
                return parseTuple(null, false, false);
            }
            case TEXT: {
                String text = token.getString();

                if (tokens.peek().is(TokenType.ASSIGN)) {
                    tokens.poll();

                    Obj value = parseOnce(false);

                    if (root && semicolons && implicitTuples && tokens.peek().is(TokenType.COMMA)) {
                        tokens.poll();
                        value = parseTuple(value, false, false);
                    }

                    if (value.isPair() && !rawPairs) {
                        value = new LinkedTuple(value);
                    }

                    return new Pair(text, value);
                } else {
                    Obj obj = new Text(text);

                    if (root && semicolons && tokens.peek().is(TokenType.COMMA)) {
                        tokens.poll();
                        return parseTuple(obj, false, false);
                    }

                    return obj;
                }
            }
        }

        throw new SyntaxException("Unexpected " + token, token.getPosition());
    }

    private Obj parseSmartTuple() {
        Tuple tuple = parseTuple(null, false, true);
        if (smartTuples && tuple.isSingleton()) {
            Obj obj = tuple.firstArg();
            if (!obj.isPair() && !rawPairs) return obj;
        }
        return tuple;
    }

    private Tuple parseTuple(Obj first, boolean root, boolean smart) {
        Tuple tuple = new LinkedTuple();

        boolean implicit = first != null;

        assert !(implicit && root && smart);

        if (implicit) {
            tuple.add(first);
        }

        if (!implicit && !root && tokens.peek().is(smart ? TokenType.RIGHT_PAREN : TokenType.RIGHT_BRACE)) {
            tokens.poll();
            return tuple;
        }

        while (true) {
            tuple.add(parseOnce(!implicit));

            Token token = tokens.poll();
            switch (token.getType()) {
                case RIGHT_PAREN: {
                    if (implicit) {
                        tokens.push(token);
                        return tuple;
                    }
                    if (!root && smart) return tuple;
                    break;
                }

                case RIGHT_BRACE: {
                    if (implicit) {
                        tokens.push(token);
                        return tuple;
                    }
                    if (!root && !smart) return tuple;
                    break;
                }

                case COMMA: {
                    if (root && semicolons && implicitTuples) break;
                    continue;
                }

                case LINE:
                case SEMICOLON: {
                    if (implicit) {
                        tokens.push(token);
                        return tuple;
                    }

                    if (tokens.peek().is(TokenType.EOF)) {
                        return tuple;
                    }
                    continue;
                }
                case EOF: {
                    tokens.push(token);
                    if (!implicit && !root) break;
                    return tuple;
                }
            }

            throw new SyntaxException("Unexpected " + token, token.getPosition());
        }
    }
}