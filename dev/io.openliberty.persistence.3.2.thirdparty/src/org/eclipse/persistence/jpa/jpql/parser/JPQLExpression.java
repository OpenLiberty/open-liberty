/*
 * Copyright (c) 2006, 2024 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024 Contributors to the Eclipse Foundation. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.persistence.jpa.jpql.ExpressionTools;
import org.eclipse.persistence.jpa.jpql.JPAVersion;
import org.eclipse.persistence.jpa.jpql.WordParser;

/**
 * A <code>JPQLExpression</code> is the root of the parsed tree representation of a JPQL query. The
 * query is parsed based on what was registered in the {@link JPQLGrammar}'s {@link ExpressionRegistry}.
 * <p>
 * A JPQL statement may be either a <b>SELECT</b> statement, an <b>UPDATE</b> statement, or a
 * <b>DELETE FROM</b> statement.
 *
 * <div><b>BNF:</b> <code>QL_statement ::= {@link SelectStatement select_statement} |
 *                                                {@link UpdateStatement update_statement} |
 *                                                {@link DeleteStatement delete_statement}</code></div>
 * <p>
 * It is possible to parse a portion of a JPQL query. The ID of the {@link JPQLQueryBNF} is used to
 * parse that portion and {@link #getQueryStatement()} then returns only the parsed tree representation
 * of that JPQL fragment.
 *
 * @version 2.5
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public final class JPQLExpression extends AbstractExpression implements ParentExpression {

    /**
     * The JPQL grammar that defines how to parse a JPQL query.
     */
    private JPQLGrammar jpqlGrammar;

    /**
     * By default, this is {@link JPQLStatementBNF#ID} but it can be any other unique identifier of
     * a {@link JPQLQueryBNF} when a portion of a JPQL query needs to be parsed.
     */
    private String queryBNFId;

    /**
     * The tree representation of the query.
     */
    private AbstractExpression queryStatement;

    /**
     * Determines if the parsing system should be tolerant, meaning if it should try to parse invalid
     * or incomplete queries.
     */
    private boolean tolerant;

    /**
     * Determines if one or more {@link IdExpression} exist in the parsed tree.
     *
     */
    private boolean idExpression = false;

    /**
     * Determines if one or more {@link VersionExpression} exist in the parsed tree.
     *
     */
    private boolean versionExpression = false;

    /**
     * If the expression could not be fully parsed, meaning some unknown text is trailing the query,
     * this will contain it.
     */
    private AbstractExpression unknownEndingStatement;

    /**
     * Jakarta data support. e.g. generate missing aliases
     */
    private boolean jakartaData = false;

    private boolean generateImplicitThisAlias = false;

    /**
     * Creates a new <code>JPQLExpression</code>, which is the root of the JPQL parsed tree.
     *
     * @param query The string representation of the JPQL query to parse
     * @param jpqlGrammar The JPQL grammar that defines how to parse a JPQL query
     */
    public JPQLExpression(CharSequence query, JPQLGrammar jpqlGrammar) {
        this(query, jpqlGrammar, false);
    }

    /**
     * Creates a new <code>JPQLExpression</code>, which is the root of the JPQL parsed tree.
     *
     * @param query The string representation of the JPQL query to parse
     * @param jpqlGrammar The JPQL grammar that defines how to parse a JPQL query
     * @param tolerant Determines if the parsing system should be tolerant, meaning if it should try
     * to parse invalid or incomplete queries
     */
    public JPQLExpression(CharSequence query, JPQLGrammar jpqlGrammar, boolean tolerant) {
        this(query, jpqlGrammar, JPQLStatementBNF.ID, tolerant);
    }

    /**
     * Creates a new <code>JPQLExpression</code> that will parse the given fragment of a JPQL query.
     * This means {@link #getQueryStatement()} will not return a query statement (select, delete or
     * update) but only the parsed tree representation of the fragment if the query BNF can pare it.
     * If the fragment of the JPQL query could not be parsed using the given {@link JPQLQueryBNF},
     * then {@link #getUnknownEndingStatement()} will contain the non-parsable fragment.
     *
     * @param jpqlFragment A fragment of a JPQL query, which is a portion of a complete JPQL query
     * @param jpqlGrammar The JPQL grammar that defines how to parse a JPQL query
     * @param queryBNFId The unique identifier of the {@link org.eclipse.persistence.jpa.jpql.parser.JPQLQueryBNF JPQLQueryBNF}
     * @param tolerant Determines if the parsing system should be tolerant, meaning if it should try
     * to parse invalid or incomplete queries
     * @since 2.4
     */
    public JPQLExpression(CharSequence jpqlFragment,
                          JPQLGrammar jpqlGrammar,
                          String queryBNFId,
                          boolean tolerant) {

        this(jpqlGrammar, queryBNFId, tolerant, false);
        parse(new WordParser(jpqlFragment), tolerant);
    }

    /**
     * Creates a new <code>JPQLExpression</code> that will parse the given fragment of a JPQL query.
     * This means {@link #getQueryStatement()} will not return a query statement (select, delete or
     * update) but only the parsed tree representation of the fragment if the query BNF can pare it.
     * If the fragment of the JPQL query could not be parsed using the given {@link JPQLQueryBNF},
     * then {@link #getUnknownEndingStatement()} will contain the non-parsable fragment.
     *
     * @param jpqlFragment A fragment of a JPQL query, which is a portion of a complete JPQL query
     * @param jpqlGrammar The JPQL grammar that defines how to parse a JPQL query
     * @param queryBNFId The unique identifier of the {@link org.eclipse.persistence.jpa.jpql.parser.JPQLQueryBNF JPQLQueryBNF}
     * @param tolerant Determines if the parsing system should be tolerant, meaning if it should try
     * to parse invalid or incomplete queries
     * @param jakartaData Jakarta data support. Used to control to generate missing Entity alias for SELECT queries like "SELECT e FROM Entity",
     * @since 5.0
     */
    public JPQLExpression(CharSequence jpqlFragment,
                          JPQLGrammar jpqlGrammar,
                          String queryBNFId,
                          boolean tolerant,
                          boolean jakartaData) {

        this(jpqlGrammar, queryBNFId, tolerant, jakartaData);
        if (jakartaData) {
            jpqlFragment = preParse(jpqlFragment);
        }
        generateImplicitThisAlias = generateImplicitThisAliasDetection((String) jpqlFragment);
        parse(new WordParser(jpqlFragment), tolerant);
    }

    /**
     * Creates a new <code>JPQLExpression</code>, which is the root of the JPQL parsed tree.
     *
     * @param jpqlGrammar The JPQL grammar that defines how to parse a JPQL query
     * @param tolerant Determines if the parsing system should be tolerant, meaning if it should try
     * @param jakartaData Jakarta data support. Used to control to generate missing Entity alias for SELECT queries like "SELECT e FROM Entity",
     */
    private JPQLExpression(JPQLGrammar jpqlGrammar, String queryBNFId, boolean tolerant, boolean jakartaData) {
        super(null);
        this.queryBNFId  = queryBNFId;
        this.tolerant    = tolerant;
        this.jpqlGrammar = jpqlGrammar;
        this.jakartaData = jakartaData;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void acceptChildren(ExpressionVisitor visitor) {
        getQueryStatement().accept(visitor);
        getUnknownEndingStatement().accept(visitor);
    }

    @Override
    protected void addChildrenTo(Collection<Expression> children) {
        children.add(getQueryStatement());
        children.add(getUnknownEndingStatement());
    }

    @Override
    protected void addOrderedChildrenTo(List<Expression> children) {

        if (queryStatement != null) {
            children.add(queryStatement);
        }

        if (unknownEndingStatement != null) {
            children.add(unknownEndingStatement);
        }
    }

    /**
     * Creates a map of the position of the cursor within each {@link Expression} of the parsed tree.
     *
     * @param actualQuery The actual query is a string representation of the query that may contain
     * extra whitespace
     * @param position The position of the cursor in the actual query, which is used to retrieve the
     * deepest {@link Expression}. The position will be adjusted to fit into the beautified version
     * of the query
     * @return A new {@link QueryPosition}
     */
    public QueryPosition buildPosition(String actualQuery, int position) {

        // Adjust the position by not counting extra whitespace
        position = ExpressionTools.repositionCursor(actualQuery, position, toActualText());

        QueryPosition queryPosition = new QueryPosition(position);
        populatePosition(queryPosition, position);
        return queryPosition;
    }

    /**
     * Returns the deepest {@link Expression} for the given position.
     *
     * @param actualQuery The actual query is the text version of the query that may contain extra
     * whitespace and different formatting than the trim down version generated by the parsed tree
     * @param position The position in the actual query used to retrieve the {@link Expression}
     * @return The {@link Expression} located at the given position in the given query
     */
    public Expression getExpression(String actualQuery, int position) {
        QueryPosition queryPosition = buildPosition(actualQuery, position);
        return queryPosition.getExpression();
    }

    @Override
    public JPQLGrammar getGrammar() {
        return jpqlGrammar;
    }

    @Override
    public JPAVersion getJPAVersion() {
        return jpqlGrammar.getJPAVersion();
    }

    @Override
    public JPQLQueryBNF getQueryBNF() {
        return getQueryBNF(queryBNFId);
    }

    @Override
    public boolean isGenerateImplicitThisAlias() {
        return generateImplicitThisAlias;
    }

    @Override
    public void setGenerateImplicitThisAlias(boolean generateImplicitThisAlias) {
        this.generateImplicitThisAlias = generateImplicitThisAlias;
    }

    @Override
    public boolean isParentExpression() {
        return true;
    }

    public boolean isJakartaData() {
        return this.jakartaData;
    }

    /**
     * Returns the {@link Expression} representing the query, which is either a <b>SELECT</b>, a
     * <b>DELETE</b> or an <b>UPDATE</b> clause.
     *
     * @return The expression representing the Java Persistence query
     */
    public Expression getQueryStatement() {
        if (queryStatement == null) {
            queryStatement = buildNullExpression();
        }
        return queryStatement;
    }

    /**
     * Returns the {@link Expression} that may contain a portion of the query that could not be
     * parsed, this happens when the query is either incomplete or malformed.
     *
     * @return The expression used when the ending of the query is unknown or malformed
     */
    public Expression getUnknownEndingStatement() {
        if (unknownEndingStatement == null) {
            unknownEndingStatement = buildNullExpression();
        }
        return unknownEndingStatement;
    }

    /**
     * Determines whether a query was parsed. The query may be incomplete but it started with one of
     * the three clauses (<b>SELECT</b>, <b>DELETE FROM</b>, or <b>UPDATE</b>).
     *
     * @return <code>true</code> the query was parsed; <code>false</code> otherwise
     */
    public boolean hasQueryStatement() {
        return queryStatement != null &&
              !queryStatement.isNull();
    }

    /**
     * Determines whether the query that got parsed had some malformed or unknown information.
     *
     * @return <code>true</code> if the query could not be parsed correctly
     * because it is either incomplete or malformed
     */
    public boolean hasUnknownEndingStatement() {
        return unknownEndingStatement != null &&
              !unknownEndingStatement.isNull();
    }

    /**
     * Determines if one or more {@link IdExpression} exist in the parsed tree.
     *
     * @return <code>true</code> one or more {@link IdExpression} exist in the parsed tree
     * <code>false</code> if not
     */
    public boolean hasIdExpression() {
        return idExpression;
    }

    public void setIdExpression(boolean idExpression) {
        this.idExpression = idExpression;
    }

    /**
     * Determines if one or more {@link VersionExpression} exist in the parsed tree.
     *
     * @return <code>true</code> one or more {@link VersionExpression} exist in the parsed tree
     * <code>false</code> if not
     */
    public boolean hasVersionExpression() {
        return versionExpression;
    }

    public void setVersionExpression(boolean versionExpression) {
        this.versionExpression = versionExpression;
    }

    @Override
    protected boolean isTolerant() {
        return tolerant;
    }

    @Override
    protected void parse(WordParser wordParser, boolean tolerant) {

        // Skip leading whitespace
        wordParser.skipLeadingWhitespace();

        // Parse the query, which can be invalid/incomplete or complete and valid
        // Make sure to use this statement if it's a JPQL fragment as well
        if (tolerant || (queryBNFId != JPQLStatementBNF.ID)) {

            // If the query BNF is not the "root" BNF, then we need to parse
            // it with a broader check when parsing
            if (queryBNFId == JPQLStatementBNF.ID) {
                queryStatement = parseUsingExpressionFactory(wordParser, queryBNFId, tolerant);
            }
            else {
                queryStatement = parse(wordParser, queryBNFId, tolerant);
            }

            int count = wordParser.skipLeadingWhitespace();

            // The JPQL query is invalid or incomplete, the remaining will be added
            // to the unknown ending statement
            if ((queryStatement == null) || !wordParser.isTail()) {
                wordParser.moveBackward(count);
                unknownEndingStatement = buildUnknownExpression(wordParser.substring());
            }
            // The JPQL query has some ending whitespace, keep one (used by content assist)
            else if (!wordParser.isTail() || (tolerant && (count > 0))) {
                unknownEndingStatement = buildUnknownExpression(" ");
            }
            // The JPQL query or fragment is invalid
            else if (queryStatement.isUnknown()) {
                unknownEndingStatement = buildUnknownExpression(queryStatement.toParsedText());
                queryStatement = null;
            }
        }
        // Quickly parse the valid query
        else {

            switch (wordParser.character()) {
                case 'd': case 'D': queryStatement = new DeleteStatement(this); break;
                case 'u': case 'U': queryStatement = new UpdateStatement(this); break;
                case 's': case 'S': queryStatement = new SelectStatement(this); break;
            }

            if (queryStatement != null) {
                queryStatement.parse(wordParser, tolerant);
            }
            else {
                queryStatement = parse(wordParser, queryBNFId, tolerant);
            }
        }
    }

    @Override
    protected void toParsedText(StringBuilder writer, boolean actual) {

        if (queryStatement != null) {
            queryStatement.toParsedText(writer, actual);
        }

        if (unknownEndingStatement != null) {
            unknownEndingStatement.toParsedText(writer, actual);
        }
    }

    private CharSequence preParse(CharSequence jpqlFragment) {
        WordParser wordParser = new WordParser(jpqlFragment);
        wordParser.skipLeadingWhitespace();
        if (Expression.FROM.equalsIgnoreCase(wordParser.word())) {
            return Expression.SELECT + " " + Expression.THIS + " " + jpqlFragment;
        }
        return jpqlFragment;
    }

    //Quick pre-check if 'this' entity alias should be generated automatically
    //There are some additional checks later during parsing query
    private boolean generateImplicitThisAliasDetection(String jpqlFragment) {
        String jpqlFragmentLowerCase = jpqlFragment.toLowerCase(Locale.ROOT);
        String fromLowerCase = Expression.FROM.toLowerCase(Locale.ROOT);
        int count = 0;
        int fromIndex = jpqlFragmentLowerCase.indexOf(fromLowerCase);
        int formOccurrences = Collections.frequency(Arrays.asList(jpqlFragmentLowerCase.split(" ")), fromLowerCase);
        if (fromIndex >= 1 && formOccurrences == 1) {
            String jpqlFragmentFrom = jpqlFragment.substring(fromIndex);
            WordParser wordParser = new WordParser(jpqlFragmentFrom);
            wordParser.moveForwardIgnoreWhitespace(Expression.FROM);
            wordParser.skipLeadingWhitespace();
            while (!wordParser.word().equalsIgnoreCase(Expression.WHERE.toLowerCase(Locale.ROOT)) && !wordParser.word().isEmpty()) {
                count++;
                wordParser.moveForwardIgnoreWhitespace(wordParser.word());
                wordParser.skipLeadingWhitespace();
            }
        }
        return count == 1;
    }
}
