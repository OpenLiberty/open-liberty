/*
 * Copyright (c) 2006, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation
//
package org.eclipse.persistence.jpa.jpql.parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.persistence.jpa.jpql.ExpressionTools;
import org.eclipse.persistence.jpa.jpql.JPAVersion;
import org.eclipse.persistence.jpa.jpql.WordParser;
import org.eclipse.persistence.jpa.jpql.WordParser.WordType;
import org.eclipse.persistence.jpa.jpql.utility.iterable.ListIterable;
import org.eclipse.persistence.jpa.jpql.utility.iterable.SnapshotCloneListIterable;

/**
 * This is the abstract definition of all the parts used to create the tree hierarchy representing
 * the parsed JPQL query.
 *
 * @see ExpressionFactory
 * @see JPQLGrammar
 *
 * @version 2.6
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public abstract class AbstractExpression implements Expression {

    /**
     * The string representation of this {@link AbstractExpression} (which includes its children).
     * The string includes characters that are considered virtual, i.e. that was parsed when the
     * query is incomplete and is needed for functionality like content assist.
     *
     * @see #toActualText()
     */
    private String actualText;

    /**
     * The children of this {@link AbstractExpression}.
     *
     * @see #children()
     */
    private List<Expression> children;

    /**
     * The position of this {@link AbstractExpression} in relation to its parent hierarchy by
     * calculating the length of the string representation of what comes before.
     */
    private int offset;

    /**
     * The string representation of this {@link AbstractExpression}.
     *
     * @see #orderedChildren()
     */
    private List<Expression> orderedChildren;

    /**
     * The parent of this {@link AbstractExpression} or <code>null</code> if this object is {@link
     * JPQLExpression} - the root of the parsed tree hierarchy.
     */
    private AbstractExpression parent;

    /**
     * The string representation of this {@link AbstractExpression} (which includes its children).
     * The string does not include characters that are considered virtual, i.e. that was parsed when
     * the query is incomplete.
     *
     * @see #toParsedText()
     */
    private String parsedText;

    /**
     * This attribute can be used to store the {@link AbstractExpression}'s JPQL identifier or a literal.
     */
    private String text;

    /**
     * The constant for ','.
     */
    public static final char COMMA = ',';

    /**
     * The constant for '.'.
     */
    public static final char DOT = '.';

    /**
     * The constant for '"'.
     */
    public static final char DOUBLE_QUOTE = '\"';

    /**
     * The constant for '{'.
     */
    public static final char LEFT_CURLY_BRACKET = '{';

    /**
     * The constant for '('.
     */
    public static final char LEFT_PARENTHESIS = '(';

    /**
     * The constant for a character that is not defined.
     */
    public static final char NOT_DEFINED = '\0';

    /**
     * The constant for '}'.
     */
    public static final char RIGHT_CURLY_BRACKET = '}';

    /**
     * The constant for ')'.
     */
    public static final char RIGHT_PARENTHESIS = ')';

    /**
     * The constant for '''.
     */
    public static final char SINGLE_QUOTE = '\'';

    /**
     * The constant for ' '.
     */
    public static final char SPACE = ' ';

    /**
     * The constant for '_'.
     */
    public static final char UNDERSCORE = '_';

    /**
     * Creates a new <code>AbstractExpression</code>.
     *
     * @param parent The parent of this expression
     */
    protected AbstractExpression(AbstractExpression parent) {
        this(parent, ExpressionTools.EMPTY_STRING);
    }

    /**
     * Creates a new <code>AbstractExpression</code>.
     *
     * @param parent The parent of this expression
     * @param text The text to be stored in this expression, <code>null</code> cannot be passed
     */
    protected AbstractExpression(AbstractExpression parent, String text) {
        super();
        this.offset = -1;
        this.text   = text;
        this.parent = parent;
    }

    /**
     * The given {@link ExpressionVisitor} needs to visit this class but it is defined by a third-
     * party provider. This method will programmatically invoke the <b>visit</b> method defined on
     * the visitor. The method signature should be:
     *
     * <div><code>{public|protected|private} void visit(ThirdPartyExpression expression)</code></div>
     * <p>
     * or
     *
     * <div><code>{public|protected|private} void visit(Expression expression)</code></div>
     * <p>
     * <b>Note:</b> The package protected visibility (default) should be used with care, if the code
     * is running inside OSGi, then the method will not be accessible, even through reflection.
     *
     * @param visitor The {@link ExpressionVisitor} to visit this {@link Expression} programmatically
     * @return <code>true</code> if the call was successfully executed; <code>false</code> otherwise
     * @since 2.4
     */
    protected boolean acceptUnknownVisitor(ExpressionVisitor visitor) {
        try {
            try {
                acceptUnknownVisitor(visitor, visitor.getClass(), getClass());
            }
            catch (NoSuchMethodException e) {
                // Try with Expression as the parameter type
                acceptUnknownVisitor(visitor, visitor.getClass(), Expression.class);
            }
            return true;
        }
        catch (NoSuchMethodException e) {
            // Ignore, just do nothing
            return false;
        }
        catch (IllegalAccessException e) {
            // Ignore, just do nothing
            return false;
        }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            RuntimeException actual;
            if (cause instanceof RuntimeException) {
                actual = (RuntimeException) cause;
            }
            else {
                actual = new RuntimeException(cause);
            }
            throw actual;
        }
    }

    /**
     * The given {@link ExpressionVisitor} needs to visit this class but it is defined by a third-
     * party provider. This method will programmatically invoke the <b>visit</b> method defined on
     * the visitor. The method signature should be:
     *
     * <div><code>{public|protected|private} void visit(ThirdPartyExpression expression)</code></div>
     * <br>
     * or
     *
     * <div><code>{public|protected|private} void visit(Expression expression)</code></div>
     *
     * @param visitor The {@link ExpressionVisitor} to visit this {@link Expression} programmatically
     * @param type The type found in the hierarchy of the given {@link ExpressionVisitor} that will
     * be used to retrieve the visit method
     * @param parameterType The parameter type of the visit method
     * @see #acceptUnknownVisitor(ExpressionVisitor)
     * @since 2.4
     */
    protected void acceptUnknownVisitor(ExpressionVisitor visitor,
                                        Class<?> type,
                                        Class<?> parameterType) throws NoSuchMethodException,
                                                                       IllegalAccessException,
                                                                       InvocationTargetException{

        try {
            Method visitMethod = type.getDeclaredMethod("visit", parameterType);
            if (!visitMethod.canAccess(visitor)) {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {visitMethod.setAccessible(true); return null;});
            }
            visitMethod.invoke(visitor, this);
        }
        catch (NoSuchMethodException e) {
            type = type.getSuperclass();
            if (type == Object.class) {
                throw e;
            }
            else {
                acceptUnknownVisitor(visitor, type, parameterType);
            }
        }
    }

    /**
     * Adds the children of this {@link AbstractExpression} to the given collection.
     *
     * @param children The collection used to store the children
     */
    protected void addChildrenTo(Collection<Expression> children) {
    }

    /**
     * Adds the children of this {@link AbstractExpression} to the given list.
     *
     * @param children The list used to store the string representation of this {@link AbstractExpression}
     */
    protected void addOrderedChildrenTo(List<Expression> children) {
    }

    /**
     * No factories were found to create an {@link Expression} with the content of {@link WordParser},
     * this method will retrieve the fallback {@link ExpressionFactory} defined in the given {@link
     * JPQLQueryBNF BNF}.
     *
     * @param wordParser The text to parse based on the current position of the cursor
     * @param word The word that was retrieved from the given text, which is the first word in the text
     * @param queryBNF The {@link JPQLQueryBNF} used to determine how to parse from the current
     * position of the cursor within the JPQL query
     * @param expression The {@link Expression} that has just been parsed or <code>null</code>
     * @param tolerant Determines whether the parsing system should be tolerant, meaning if it should
     * try to parse invalid or incomplete queries
     * @return The {@link Expression} representing the given sub-query
     */
    protected final AbstractExpression buildExpressionFromFallingBack(WordParser wordParser,
                                                                      String word,
                                                                      JPQLQueryBNF queryBNF,
                                                                      AbstractExpression expression,
                                                                      boolean tolerant) {

        ExpressionFactory factory = findFallBackExpressionFactory(queryBNF);

        if (factory == null) {
            return null;
        }

        // When parsing an invalid or incomplete query, it is possible two literals would be parsed
        // but in some cases, a CollectionExpression should not be created and the parsing should
        // actually stop here. Example: BETWEEN 10 20, when parsing 20, it should not be parsed as
        // part of the lower bound expression
        if (tolerant &&
            (factory.getId() == LiteralExpressionFactory.ID) &&
            shouldSkipLiteral(expression)) {

            return null;
        }

        return factory.buildExpression(this, wordParser, word, queryBNF, expression, tolerant);
    }

    /**
     * Creates a new <code>null</code>-{@link Expression} parented with this one.
     *
     * @return A new <code>null</code> version of an {@link Expression}
     */
    protected final AbstractExpression buildNullExpression() {
        return new NullExpression(this);
    }

    /**
     * Creates a new {@link Expression} wrapping the given character value.
     *
     * @param value The character to wrap as a {@link Expression}
     * @return The {@link Expression} representation of the given identifier where the owning
     * {@link Expression} is this one
     */
    protected final Expression buildStringExpression(char value) {
        return buildStringExpression(String.valueOf(value));
    }

    /**
     * Creates a new {@link Expression} wrapping the given string value.
     *
     * @param value The string to wrap as a <code>Expression</code>
     * @return The {@link Expression} representation of the given identifier where the owning
     * {@link Expression} is this one
     */
    protected final Expression buildStringExpression(String value) {
        return new DefaultStringExpression(this, value);
    }

    /**
     * Creates an {@link Expression} that contains a malformed expression.
     *
     * @param text The text causing the expression to be malformed
     * @return A new {@link Expression} where {@link #toActualText()} returns the given text
     */
    protected final AbstractExpression buildUnknownExpression(String text) {
        return new UnknownExpression(this, text);
    }

    /**
     * Calculates the position of the given {@link Expression} by calculating the length of what is before.
     *
     * @param expression The {@link Expression} for which its position within the parsed tree needs
     * to be determined
     * @param length The current cursor position within the JPQL query while digging into the tree
     * until the search reaches the expression
     * @return The length of the string representation for what is coming before the given {@link Expression}
     * @since 2.4
     */
    protected final int calculatePosition(Expression expression, int length) {

        Expression parent = expression.getParent();

        // Reach the root
        if (parent == null) {
            return length;
        }

        // Traverse the child expression until the expression
        for (Expression childExpression : parent.orderedChildren()) {

            // Continue to calculate the position by going up the hierarchy
            if (childExpression == expression) {
                return calculatePosition(parent, length);
            }

            length += childExpression.getLength();
        }

        // It should never reach this
        throw new RuntimeException("The position of the Expression could not be calculated: " + expression);
    }

    @Override
    public final ListIterable<Expression> children() {
        if (children == null) {
            children = new LinkedList<>();
            addChildrenTo(children);
        }
        return new SnapshotCloneListIterable<>(children);
    }

    /**
     * Retrieve the {@link ExpressionFactory} from the given {@link JPQLQueryBNF} by following the
     * path of fallback {@link JPQLQueryBNF JPQLQueryBNFs} and then returns the {@link ExpressionFactory}
     * from the leaf {@link JPQLQueryBNF}.
     *
     * @param queryBNF The {@link JPQLQueryBNF} for which its associated fallback {@link ExpressionFactory}
     * will be searched
     * @return Either the fallback {@link ExpressionFactory} linked to the given {@link JPQLQueryBNF}
     * or <code>null</code> if none was declared
     */
    protected final ExpressionFactory findFallBackExpressionFactory(JPQLQueryBNF queryBNF) {

        String fallBackBNFId = queryBNF.getFallbackBNFId();

        // No fall back BNF is defined, then nothing can be done
        if (fallBackBNFId == null) {
            return null;
        }

        JPQLQueryBNF fallBackQueryBNF = getQueryBNF(fallBackBNFId);

        // Traverse the fall back BNF because it has its own fall back BNF
        if (fallBackQueryBNF != queryBNF &&
            fallBackQueryBNF.getFallbackBNFId() != null) {

            return findFallBackExpressionFactory(fallBackQueryBNF);
        }

        // Retrieve the factory associated with the fall back BNF
        return getExpressionRegistry().getExpressionFactory(fallBackQueryBNF.getFallbackExpressionFactoryId());
    }

    @Override
    public JPQLQueryBNF findQueryBNF(Expression expression) {
        return getQueryBNF();
    }

    /**
     * Retrieves the registered {@link ExpressionFactory} that was registered for the given unique
     * identifier.
     *
     * @param expressionFactoryId The unique identifier of the {@link ExpressionFactory} to retrieve
     * @return The {@link ExpressionFactory} mapped with the given unique identifier
     * @see ExpressionRegistry#getExpressionFactory(String)
     */
    protected final ExpressionFactory getExpressionFactory(String expressionFactoryId) {
        return getExpressionRegistry().getExpressionFactory(expressionFactoryId);
    }

    /**
     * Returns the registry containing the {@link JPQLQueryBNF JPQLQueryBNFs} and the {@link
     * org.eclipse.persistence.jpa.jpql.parser.ExpressionFactory ExpressionFactories} that are used
     * to properly parse a JPQL query.
     *
     * @return The registry containing the information related to the JPQL grammar
     */
    protected final ExpressionRegistry getExpressionRegistry() {
        return getGrammar().getExpressionRegistry();
    }

    @Override
    public JPQLGrammar getGrammar() {
        return getRoot().getGrammar();
    }

    /**
     * Retrieves the JPA version in which the identifier was first introduced.
     *
     * @return The version in which the identifier was introduced
     */
    public JPAVersion getIdentifierVersion(String identifier) {
        return getRoot().getIdentifierVersion(identifier);
    }

    /**
     * Returns the version of the Java Persistence to support.
     *
     * @return The JPA version supported by the grammar
     * @see JPQLGrammar
     */
    protected JPAVersion getJPAVersion() {
        return getRoot().getJPAVersion();
    }

    @Override
    public final int getLength() {
        return toActualText().length();
    }

    @Override
    public final int getOffset() {
        if (offset == -1) {
            offset = calculatePosition(this, 0);
        }
        return offset;
    }

    @Override
    public final AbstractExpression getParent() {
        return parent;
    }

    /**
     * Retrieves the BNF object that was registered for the given unique identifier.
     *
     * @param queryBNFID The unique identifier of the {@link JPQLQueryBNF} to retrieve
     * @return The {@link JPQLQueryBNF} representing a section of the grammar
     */
    public JPQLQueryBNF getQueryBNF(String queryBNFID) {
        return getExpressionRegistry().getQueryBNF(queryBNFID);
    }

    @Override
    public final JPQLExpression getRoot() {
        return (parent == null) ? (JPQLExpression) this : parent.getRoot();
    }

    /**
     * Returns the encapsulated text of this {@link AbstractExpression}, which can be used in various
     * ways, it can be a keyword, a literal, etc.
     *
     * @return Either the JPQL identifier for this {@link AbstractExpression}, the literal it
     * encapsulates or an empty string
     */
    protected String getText() {
        return text;
    }

    /**
     * Determines whether the given {@link JPQLQueryBNF} handles aggregate expressions.
     *
     * @param queryBNF The {@link JPQLQueryBNF} used to determine if the parsing should handle
     * aggregate expressions
     * @return <code>true</code> if the given BNF handles aggregate expressions; <code>false</code>
     * otherwise
     */
    protected boolean handleAggregate(JPQLQueryBNF queryBNF) {
        return queryBNF.handleAggregate();
    }

    /**
     * Determines whether the given {@link JPQLQueryBNF} handles a collection of sub-expressions that
     * are separated by commas.
     *
     * @param queryBNF The {@link JPQLQueryBNF} used to determine if the parsing should handle
     * collection of sub-expressions
     * @return <code>true</code> if the sub-expression to parse might have several sub-expressions
     * separated by commas; <code>false</code> otherwise
     */
    protected boolean handleCollection(JPQLQueryBNF queryBNF) {
        return queryBNF.handleCollection();
    }

    @Override
    public boolean isAncestor(Expression expression) {

        if (expression == this) {
            return true;
        }

        if (expression == null) {
            return false;
        }

        return isAncestor(expression.getParent());
    }

    /**
     * Determines if the given word is a JPQL identifier. The check is case insensitive.
     *
     * @param word The word to test if it is a JPQL identifier
     * @return <code>true</code> if the word is an identifier, <code>false</code> otherwise
     * @see ExpressionRegistry#isIdentifier(String)
     */
    protected final boolean isIdentifier(String word) {
        return getExpressionRegistry().isIdentifier(word);
    }

    /**
     * Determines whether this expression is a <code>null</code> {@link Expression} or any other subclass.
     *
     * @return <code>false</code> by default
     */
    protected boolean isNull() {
        return false;
    }

    /**
     * Determines whether the parsing is complete based on what is left in the given text. The text
     * is never empty.
     *
     * @param wordParser The text to parse based on the current position of the cursor
     * @param word The word that was retrieved from the given text, which is the first word in the text
     * @param expression The {@link Expression} that has already been parsed
     * @return <code>true</code> if the text no longer can't be parsed by the current expression;
     * <code>false</code> if more can be parsed
     */
    protected boolean isParsingComplete(WordParser wordParser, String word, Expression expression) {
        // TODO: MAYBE MOVE THIS TO THE JPQL GRAMMAR SO GENERIC JPA DOES
        //       NOT HAVE KNOWLEDGE OF ECLIPSELINK SPECIFIC FUNCTIONS
        return word.equalsIgnoreCase(FROM)                        ||
               word.equalsIgnoreCase(WHERE)                       ||
               word.equalsIgnoreCase(HAVING)                      ||
               wordParser.startsWithIdentifier(GROUP_BY)          ||
               wordParser.startsWithIdentifier(ORDER_BY)          ||
               wordParser.startsWithIdentifier(AS_OF)             ||
               wordParser.startsWithIdentifier(START_WITH)        ||
               wordParser.startsWithIdentifier(CONNECT_BY)        ||
               wordParser.startsWithIdentifier(ORDER_SIBLINGS_BY) ||
               word.equalsIgnoreCase(UNION)                       ||
               word.equalsIgnoreCase(INTERSECT)                   ||
               word.equalsIgnoreCase(EXCEPT);
    }

    /**
     * Determines if the parser is in tolerant mode or is in fast mode. When the tolerant is turned
     * on, it means the parser will attempt to parse incomplete or invalid queries.
     *
     * @return <code>true</code> if the parsing system should parse invalid or incomplete queries;
     * <code>false</code> when the query is well-formed and valid
     */
    protected boolean isTolerant() {
        return getRoot().isTolerant();
    }

    /**
     * Determines whether this expression is an unknown {@link Expression} or any other subclass.
     *
     * @return <code>false</code> by default
     */
    protected boolean isUnknown() {
        return false;
    }

    /**
     * Determines whether this {@link AbstractExpression} is virtual, meaning it's not part of the
     * query but is required for proper navigability.
     *
     * @return <code>true</code> if this {@link AbstractExpression} was virtually created to fully
     * qualify path expression; <code>false</code> if it was parsed
     */
    protected boolean isVirtual() {
        return false;
    }

    @Override
    public final ListIterable<Expression> orderedChildren() {
        if (orderedChildren == null) {
            orderedChildren = new LinkedList<>();
            addOrderedChildrenTo(orderedChildren);
        }
        return new SnapshotCloneListIterable<>(orderedChildren);
    }

    /**
     * Parses the query by starting at the current position, which is part of the given {@link WordParser}.
     *
     * @param wordParser The text to parse based on the current position of the cursor
     * @param tolerant Determines whether the parsing system should be tolerant, meaning if it should
     * try to parse invalid or incomplete queries
     */
    protected abstract void parse(WordParser wordParser, boolean tolerant);

    /**
     * Parses the given text by using the specified BNF.
     *
     * @param wordParser The text to parse based on the current position of the cursor
     * @param queryBNFId The unique identifier of the {@link JPQLQueryBNF} that is used to determine
     * how to parse the text at the current cursor position within the JPQL query
     * @param tolerant Determines whether the parsing system should be tolerant, meaning if it should
     * try to parse invalid or incomplete queries
     * @return The {@link Expression} representing the given sub-query
     */
    @SuppressWarnings("null")
    protected AbstractExpression parse(WordParser wordParser, String queryBNFId, boolean tolerant) {

        // Quick check so we don't create some objects for no reasons
        if (tolerant && wordParser.isTail()) {
            return null;
        }

        //
        // NOTE: This method could look better but for performance reason, it is a single method,
        //       which reduces the number of objects created and methods called.
        //

        JPQLQueryBNF queryBNF = getQueryBNF(queryBNFId);

        int count = 0;
        boolean beginning = !tolerant;
        char character = wordParser.character();

        AbstractExpression child;
        AbstractExpression expression = null;

        Info rootInfo = null;
        Info currentInfo = null;

        // Parse the string until the position of the cursor is at
        // the end of the string or until the parsing is complete
        while (!wordParser.isTail()) {

            child = null;

            //
            // Step 1
            //
            // Right away create a SubExpression and parse the encapsulated expression
            if (character == LEFT_PARENTHESIS) {

                // If the JPQLQueryBNF handles parsing the sub-expression, then delegate the parsing
                // to its fallback ExpressionFactory
                if (queryBNF.handleSubExpression()) {
                    expression = buildExpressionFromFallingBack(
                        wordParser,
                        ExpressionTools.EMPTY_STRING,
                        queryBNF,
                        expression,
                        tolerant
                    );
                }
                else {
                    expression = new SubExpression(this, queryBNF);
                    expression.parse(wordParser, tolerant);

                    // Make sure this is not the root and if the parent handles parsing the sub-
                    // expression, then the Expression needs to be returned without further parsing
                    if ((parent != null) && parent.getQueryBNF().handleSubExpression()) {
                        return expression;
                    }
                }

                // Something has been parsed, which means it's not the beginning anymore
                beginning = false;

                // Continue to the next character/word
                count     = wordParser.skipLeadingWhitespace();
                character = wordParser.character();

                // Store the SubExpression
                currentInfo = (rootInfo == null) ? (rootInfo = new Info()) : (currentInfo.next = new Info(currentInfo));
                currentInfo.expression = expression;
                currentInfo.space = count > 0;
            }

            // Retrieve the next word, including any arithmetic symbols
            String word = wordParser.word();

            // A word was parsed, attempt to parse it using first the factory, then the fallback factory
            if (word.length() > 0) {

                // Nothing more to parse
                if (!tolerant && !beginning && isParsingComplete(wordParser, word, expression) ||
                     tolerant &&               isParsingComplete(wordParser, word, expression)) {

                    break;
                }

                //
                // Step 2
                //
                // Parse using the ExpressionFactory that is mapped with a JPQL identifier (word)
                if (shouldParseWithFactoryFirst() &&
                    (wordParser.getWordType() == WordType.WORD)) {

                    ExpressionFactory factory = queryBNF.getExpressionFactory(word);

                    if (factory != null) {
                        child = factory.buildExpression(this, wordParser, word, queryBNF, expression, tolerant);

                        if (child != null) {

                            // The new expression is a child of the previous expression,
                            // remove it from the collection since it's already parented
                            if ((expression != null) && child.isAncestor(expression)) {
                                if (currentInfo == rootInfo) {
                                    rootInfo = null;
                                    currentInfo = null;
                                }
                                else if (currentInfo != null) {
                                    currentInfo = currentInfo.previous;
                                }
                            }

                            // Something has been parsed, which means it's not the beginning anymore
                            beginning = false;

                            // Continue with the next character/word
                            count     = wordParser.skipLeadingWhitespace();
                            character = wordParser.character();

                            // The new expression becomes the previous expression
                            expression = child;
                        }
                    }
                }

                //
                // Step 3
                //
                // No factories could be used, use the fall back ExpressionFactory
                if (child == null) {
                    child = buildExpressionFromFallingBack(wordParser, word, queryBNF, expression, tolerant);

                    if (child != null) {

                        // The new expression is a child of the previous expression,
                        // remove it from the collection since it's already parented
                        if ((expression != null) && child.isAncestor(expression)) {
                            if (currentInfo == rootInfo) {
                                rootInfo = null;
                                currentInfo = null;
                            }
                            else if (currentInfo != null) {
                                currentInfo = currentInfo.previous;
                            }
                        }

                        // Something has been parsed, which means it's not the beginning anymore
                        beginning = false;

                        // Continue with the next character/word
                        count     = wordParser.skipLeadingWhitespace();
                        character = wordParser.character();

                        // The new expression becomes the previous expression
                        expression = child;
                    }
                }

                //
                // Step 4
                //
                // If nothing was parsed, then attempt to parse the fragment by retrieving the factory
                // directory from the JPQL grammar and not from the one registered with the current BNF
                if (tolerant && (child == null)) {

                    ExpressionRegistry expressionRegistry = getExpressionRegistry();

                    if (expressionRegistry.getIdentifierRole(word) != IdentifierRole.AGGREGATE) {

                        ExpressionFactory factory = expressionRegistry.expressionFactoryForIdentifier(word);

                        if (factory != null) {
                            child = factory.buildExpression(this, wordParser, word, queryBNF, expression, tolerant);

                            if (child != null) {
                                child = new BadExpression(this, child);

                                // The new expression is a child of the previous expression,
                                // remove it from the collection since it's already parented
                                if ((expression != null) && child.isAncestor(expression)) {
                                    if (currentInfo == rootInfo) {
                                        rootInfo = null;
                                        currentInfo = null;
                                    }
                                    else if (currentInfo != null) {
                                        currentInfo = currentInfo.previous;
                                    }
                                }

                                // Something has been parsed, which means it's not the beginning anymore
                                beginning = false;

                                // Continue with the next character/word
                                count     = wordParser.skipLeadingWhitespace();
                                character = wordParser.character();

                                // The new expression becomes the previous expression
                                expression = child;
                            }
                        }
                    }
                }
            }

            // Nothing could be parsed, break here so the parent can continue parsing.
            // Example: AVG() and we're parsing what's inside the parenthesis (nothing).
            // But if it's (,), then we have to create a collection of "null" expressions
            // separated by a comma
            if ((child == null) && (character != COMMA)) {
                break;
            }

            // Store the child but skip a very special case, which happens when parsing
            // two subqueries in a collection expression. Example: (SELECT ... ), (SELECT ... )
            if ((expression == null) || (child != null)) {
                currentInfo = (rootInfo == null) ? (rootInfo = new Info()) : (currentInfo.next = new Info(currentInfo));
                currentInfo.expression = child;
                currentInfo.space = count > 1;
            }

            // Nothing else to parse
            if (wordParser.isTail()) {
                break;
            }

            // ','
            if (character == COMMA) {

                // The current expression does not handle collection, then stop the
                // parsing here so the parent can continue to parse
                if (!handleCollection(queryBNF) || tolerant && isParsingComplete(wordParser, word, expression)) {
                    break;
                }

                // Skip the comma
                wordParser.moveForward(1);
                currentInfo.comma = true;

                // Remove leading whitespace
                count = wordParser.skipLeadingWhitespace();
                currentInfo.space = count > 0;

                character = wordParser.character();
                expression = null;

                // Special case: ((), (), ())
                if (character == LEFT_PARENTHESIS) {
                    continue;
                }

                // No more text, the query ends with a comma
                word = wordParser.word();
                boolean stopParsing = tolerant && (word.length() == 0 || isParsingComplete(wordParser, word, null));

                if (wordParser.isTail() || stopParsing) {

                    // Make sure the space is not re-added at the end of the query
                    count = 0;

                    // Add a null Expression since the expression ends with a comma
                    currentInfo = (rootInfo == null) ? (rootInfo = new Info()) : (currentInfo.next = new Info(currentInfo));

                    // Nothing else to parse
                    if (stopParsing) {
                        break;
                    }
                }

                // Nothing more to parse
                if (character == RIGHT_PARENTHESIS) {
                    break;
                }
            }
            else {

                // Continue parsing the collection expression
                if (character != RIGHT_PARENTHESIS &&
                    handleAggregate(queryBNF)) {

                    currentInfo.space = count > 0;
                }
                // Nothing more to parse
                else {
                    break;
                }
            }
        }

        if (count > 0) {
            currentInfo.space = currentInfo.comma;
            if (!currentInfo.comma) {
                wordParser.moveBackward(count);
            }
        }

        // Nothing was parsed
        if (currentInfo == null) {
            return null;
        }

        // Simply return the single expression
        if (currentInfo == rootInfo &&
           !currentInfo.comma &&
           !currentInfo.space) {

            return currentInfo.expression;
        }

        // Return a collection of expressions
        return new CollectionExpression(
            this,
            rootInfo.buildChildren(),
            rootInfo.buildCommas(),
            rootInfo.buildSpaces()
        );
    }

    /**
     * Right away parses the text by retrieving the {@link ExpressionFactory} for the first word that
     * is extracted from {@link WordParser} at the current location.
     *
     * @param wordParser The text to parse based on the current position of the cursor
     * @param queryBNFId The unique identifier of the {@link JPQLQueryBNF} that is used to determine
     * how to parse the text at the current cursor position within the JPQL query
     * @param tolerant Determines whether the parsing system should be tolerant, meaning if it should
     * try to parse invalid or incomplete queries
     * @return The {@link Expression} representing the given sub-query
     */
    protected AbstractExpression parseUsingExpressionFactory(WordParser wordParser,
                                                             String queryBNFId,
                                                             boolean tolerant) {

        String word = wordParser.word();
        JPQLQueryBNF queryBNF = getQueryBNF(queryBNFId);
        ExpressionFactory factory = queryBNF.getExpressionFactory(word);

        if (factory == null) {
            return null;
        }

        return factory.buildExpression(this, wordParser, word, queryBNF, null, tolerant);
    }

    @Override
    public void populatePosition(QueryPosition queryPosition, int position) {

        queryPosition.addPosition(this, position);

        // The position is at the beginning of this expression
        if (position == 0) {
            queryPosition.setExpression(this);
        }
        else {
            // Traverse the children in order to find where the cursor is located
            for (Expression expression : orderedChildren()) {

                String expressionText = expression.toParsedText();

                // The position is in the Expression, traverse it
                if (position <= expressionText.length()) {
                    expression.populatePosition(queryPosition, position);
                    return;
                }

                // Continue with the next child by adjusting the position
                position -= expressionText.length();
            }

            throw new IllegalStateException("A problem was encountered while calculating the position.");
        }
    }

    /**
     * Rebuilds the actual parsed text if it has been cached.
     */
    protected final void rebuildActualText() {
        if (actualText != null) {
            toActualText();
        }
    }

    /**
     * Rebuilds the parsed parsed text if it has been cached.
     */
    protected final void rebuildParsedText() {
        if (parsedText != null) {
            toParsedText();
        }
    }

    /**
     * Re-parents this {@link Expression} to be a child of the given {@link Expression}.
     *
     * @param parent The new parent of this object
     */
    protected final void setParent(AbstractExpression parent) {
        this.parent = parent;
    }

    /**
     * Sets the text of this {@link Expression}.
     *
     * @param text The immutable text wrapped by this {@link Expression}, which cannot be <code>null</code>
     */
    protected final void setText(String text) {
        this.text = text;
    }

    /**
     * Determines whether the parsing of the query should be performed using the {@link ExpressionFactory
     * factories} first or it should automatically fallback to the fallback factory.
     *
     * @return <code>true</code> is returned by default so the factories are used before falling back
     */
    protected boolean shouldParseWithFactoryFirst() {
        return true;
    }

    /**
     * When parsing an invalid or incomplete query, it is possible two literals would be parsed but
     * in some cases, a CollectionExpression should not be created and the parsing should actually
     * stop here. Example: BETWEEN 10 20, when parsing 20, it should not be parsed as part of the
     * lower bound expression.
     *
     * @param expression The {@link Expression} that has just been parsed or <code>null</code>
     * @return <code>true</code>
     */
    protected boolean shouldSkipLiteral(AbstractExpression expression) {
        return (expression != null);
    }

    @Override
    public String toActualText() {
        if (actualText == null) {
            StringBuilder writer = new StringBuilder();
            toParsedText(writer, true);
            actualText = writer.toString();
        }
        return actualText;
    }

    @Override
    public String toParsedText() {
        if (parsedText == null) {
            StringBuilder writer = new StringBuilder();
            toParsedText(writer, false);
            parsedText = writer.toString();
        }
        return parsedText;
    }

    /**
     * Generates a string representation of this {@link Expression}, including its children,
     * if it has any.
     *
     * @param writer The buffer used to append this {@link Expression}'s string representation
     * @param actual Determines whether the string representation should represent what was parsed,
     * i.e. include any "virtual" whitespace (such as ending whitespace) and the actual case of the
     * JPQL identifiers
     */
    protected abstract void toParsedText(StringBuilder writer, boolean actual);

    @Override
    public final String toString() {
        // toString() should only be called during debugging, thus the cached parsed text
        // should always be recreated in order to reflect the current state while debugging
        parsedText = null;
        return toParsedText();
    }

    /**
     * Rather than creating three lists when performing a single parsing operation ({@link
     * AbstractExpression#parse(WordParser, String, boolean) parse(WordParser, String, boolean)}),
     * this class will act as a simple chained list and postponing the creation of those three lists
     * only when needed (which is when the parsed expression needs to be a collection of expressions).
     */
    private static class Info {

        /**
         * Flag indicating a comma follows the parsed {@link #expression}.
         */
        boolean comma;

        /**
         * The parsed {@link Expression}, which is the first one that was parsed if {@link #next} is
         * <code>null</code>, meaning this <code>Info</code> object is the root of the chained list.
         */
        AbstractExpression expression;

        /**
         * When more than one expression needs to be parsed, it will be chained.
         */
        Info next;

        /**
         * The parent within the chain of this one.
         */
        Info previous;

        /**
         * Flag indicating a whitespace follows the parsed {@link #expression}.
         */
        boolean space;

        /**
         * Creates a new <code>Info</code>.
         */
        Info() {
            super();
        }

        /**
         * Creates a new <code>Info</code>.
         *
         * @param previous The parent within the chain of this one
         */
        Info(Info previous) {
            super();
            this.previous = previous;
        }

        private void addChild(ArrayList<AbstractExpression> children) {
            children.add(expression);
            if (next != null) {
                next.addChild(children);
            }
        }

        private void addComma(ArrayList<Boolean> children) {
            children.add(comma);
            if (next != null) {
                next.addComma(children);
            }
        }

        private void addSpace(ArrayList<Boolean> children) {
            children.add(space);
            if (next != null) {
                next.addSpace(children);
            }
        }

        List<AbstractExpression> buildChildren() {
            ArrayList<AbstractExpression> children = new ArrayList<>();
            addChild(children);
            return children;
        }

        List<Boolean> buildCommas() {
            ArrayList<Boolean> children = new ArrayList<>();
            addComma(children);
            return children;
        }

        List<Boolean> buildSpaces() {
            ArrayList<Boolean> children = new ArrayList<>();
            addSpace(children);
            return children;
        }
    }
}
