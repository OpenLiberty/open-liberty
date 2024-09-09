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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.persistence.jpa.jpql.WordParser;

/**
 * The <b>FROM</b> clause of a query defines the domain of the query by declaring identification
 * variables. An identification variable is an identifier declared in the <b>FROM</b> clause of a
 * query. The domain of the query may be constrained by path expressions. Identification variables
 * designate instances of a particular entity abstract schema type. The <b>FROM</b> clause can
 * contain multiple identification variable declarations separated by a comma (,).
 *
 * @see FromClause
 * @see SimpleFromClause
 *
 * @version 2.5
 * @since 2.3
 * @author Pascal Filion
 */
public abstract class AbstractFromClause extends AbstractExpression {

    /**
     * The {@link Expression} that represents the <code><b>AS OF</b></code> clause.
     *
     * @since 2.5
     */
    private AbstractExpression asOfClause;

    /**
     * The declaration portion of this <b>FROM</b> clause.
     */
    private AbstractExpression declaration;

    /**
     * Determines whether a whitespace was parsed after the identifier <b>FROM</b>.
     */
    private boolean hasSpace;

    /**
     * Determines whether there is a whitespace after the hierarchical query clause.
     *
     * @since 2.5
     */
    private boolean hasSpaceAfterHierarchicalQueryClause;

    /**
     * Determines whether there is a whitespace after the declaration and either the hierarchical
     * query clause or the <code><b>AS OF</b></code> clause was parsed.
     *
     * @since 2.5
     */
    private boolean hasSpaceDeclaration;

    /**
     * The hierarchical query clause, which holds onto the <code><b>START WITH</b></code> and
     * <code><b>CONNECT BY</b></code> clauses.
     *
     * @since 2.5
     */
    private AbstractExpression hierarchicalQueryClause;

    /**
     * The actual identifier found in the string representation of the JPQL query.
     */
    private String identifier;

    /**
     * Creates a new <code>AbstractFromClause</code>.
     *
     * @param parent The parent of this expression
     */
    protected AbstractFromClause(AbstractExpression parent) {
        super(parent, FROM);
    }

    @Override
    public void acceptChildren(ExpressionVisitor visitor) {
        getDeclaration().accept(visitor);
        getHierarchicalQueryClause().accept(visitor);
        getAsOfClause().accept(visitor);
    }

    @Override
    protected void addChildrenTo(Collection<Expression> children) {
        children.add(getDeclaration());
        children.add(getHierarchicalQueryClause());
        children.add(getAsOfClause());
    }

    @Override
    protected void addOrderedChildrenTo(List<Expression> children) {

        // 'FROM'
        children.add(buildStringExpression(FROM));

        // Space between FROM and the declaration
        if (hasSpace) {
            children.add(buildStringExpression(SPACE));
        }

        // Declaration
        if (declaration != null) {
            children.add(declaration);
        }

        if (hasSpaceDeclaration) {
            children.add(buildStringExpression(SPACE));
        }

        // Hierarchical query clause
        if (hierarchicalQueryClause != null) {
            children.add(hierarchicalQueryClause);
        }

        if (hasSpaceAfterHierarchicalQueryClause) {
            children.add(buildStringExpression(SPACE));
        }

        // 'AS OF' clause
        if (asOfClause != null) {
            children.add(asOfClause);
        }
    }

    /**
     * Creates a new {@link CollectionExpression} that will wrap the single declaration.
     *
     * @return The single declaration represented by a temporary collection
     */
    public final CollectionExpression buildCollectionExpression() {

        List<AbstractExpression> children = new ArrayList<>(1);
        children.add((AbstractExpression) getDeclaration());

        List<Boolean> commas = new ArrayList<>(1);
        commas.add(Boolean.FALSE);

        List<Boolean> spaces = new ArrayList<>(1);
        spaces.add(Boolean.FALSE);

        return new CollectionExpression(this, children, commas, spaces, true);
    }

    @Override
    public final JPQLQueryBNF findQueryBNF(Expression expression) {

        if ((declaration != null) && declaration.isAncestor(expression)) {
            return getQueryBNF(getDeclarationQueryBNFId());
        }

        return super.findQueryBNF(expression);
    }

    /**
     * Returns the actual <b>FROM</b> identifier found in the string representation of the JPQL
     * query, which has the actual case that was used.
     *
     * @return The <b>FROM</b> identifier that was actually parsed
     */
    public final String getActualIdentifier() {
        return identifier;
    }

    /**
     * Returns the {@link Expression} representing the <b>AS OF</b> clause.
     *
     * @return The expression representing the <b>AS OF</b> clause
     */
    public final Expression getAsOfClause() {
        if (asOfClause == null) {
            asOfClause = buildNullExpression();
        }
        return asOfClause;
    }

    /**
     * Returns the {@link Expression} that represents the declaration of this clause.
     *
     * @return The expression that was parsed representing the declaration
     */
    public final Expression getDeclaration() {
        if (declaration == null) {
            declaration = buildNullExpression();
        }
        return declaration;
    }

    /**
     * Returns the BNF of the declaration part of this clause.
     *
     * @return The BNF of the declaration part of this clause
     */
    public abstract String getDeclarationQueryBNFId();

    /**
     * Returns the {@link Expression} representing the hierarchical query clause.
     *
     * @return The expression representing the hierarchical query clause
     * @since 2.5
     */
    public final Expression getHierarchicalQueryClause() {
        if (hierarchicalQueryClause == null) {
            hierarchicalQueryClause = buildNullExpression();
        }
        return hierarchicalQueryClause;
    }

    /**
     * Determines whether the <b>AS OF</b> clause is defined.
     *
     * @return <code>true</code> if the query that got parsed had the <b>AS OF</b> clause
     */
    public final boolean hasAsOfClause() {
        return asOfClause != null &&
              !asOfClause.isNull();
    }

    /**
     * Determines whether the declaration of this clause was parsed.
     *
     * @return <code>true</code> if the declaration of this clause was parsed; <code>false</code> if
     * it was not parsed
     */
    public final boolean hasDeclaration() {
        return declaration != null &&
              !declaration.isNull();
    }

    /**
     * Determines whether the hierarchical query clause was parsed or not.
     *
     * @return <code>true</code> if the query that got parsed had the hierarchical query clause
     * @since 2.5
     */
    public final boolean hasHierarchicalQueryClause() {
        return hierarchicalQueryClause != null &&
              !hierarchicalQueryClause.isNull();
    }

    /**
     * Determines whether a whitespace was found after the declaration query clause, which will be
     * <code>true</code> if it's followed by either the hierarchical query clause or the <code><b>AS
     * OF</b></code> clause.
     *
     * @return <code>true</code> if there was a whitespace after the declaration; <code>false</code> otherwise
     * @since 2.5
     */
    public final boolean hasSpaceAfterDeclaration() {
        return hasSpaceDeclaration;
    }

    /**
     * Determines whether a whitespace was parsed after the <b>FROM</b> identifier.
     *
     * @return <code>true</code> if a whitespace was parsed after the <b>FROM</b> identifier;
     * <code>false</code> otherwise
     */
    public final boolean hasSpaceAfterFrom() {
        return hasSpace;
    }

    /**
     * Determines whether a whitespace was found after the hierarchical query clause. In some cases,
     * the space is owned by a child of the hierarchical query clause.
     *
     * @return <code>true</code> if there was a whitespace after the hierarchical query clause and
     * owned by this expression; <code>false</code> otherwise
     * @since 2.5
     */
    public final boolean hasSpaceAfterHierarchicalQueryClause() {
        return hasSpaceAfterHierarchicalQueryClause;
    }

    @Override
    protected boolean isParsingComplete(WordParser wordParser, String word, Expression expression) {

        char character = wordParser.character();

        // TODO: Add parameter tolerance and check for these 4 signs if tolerant is turned on only
        //       this could happen while parsing an invalid query
        return wordParser.isArithmeticSymbol(character) ||
               super.isParsingComplete(wordParser, word, expression);
    }

    @Override
    protected void parse(WordParser wordParser, boolean tolerant) {

        // Parse 'FROM'
        identifier = wordParser.moveForward(FROM);

        hasSpace = wordParser.skipLeadingWhitespace() > 0;

        // Parse the declaration
        declaration = parse(wordParser, getDeclarationQueryBNFId(), tolerant);

        int count = wordParser.skipLeadingWhitespace();
        hasSpaceDeclaration = (count > 0);

        // Parse hierarchical query clause
        if (wordParser.startsWithIdentifier(START_WITH) ||
            wordParser.startsWithIdentifier(CONNECT_BY) ||
            wordParser.startsWithIdentifier(ORDER_SIBLINGS_BY)) {

            hierarchicalQueryClause = new HierarchicalQueryClause(this);
            hierarchicalQueryClause.parse(wordParser, tolerant);

            count = wordParser.skipLeadingWhitespace();
            hasSpaceAfterHierarchicalQueryClause = (count > 0);
        }

        // AS OF clause
        if (wordParser.startsWithIdentifier(AS_OF)) {
            asOfClause = new AsOfClause(this);
            asOfClause.parse(wordParser, tolerant);
        }
        else if (hasSpaceAfterHierarchicalQueryClause) {
            hasSpaceAfterHierarchicalQueryClause = false;
            wordParser.moveBackward(count);
        }
        else if (hierarchicalQueryClause == null) {
            hasSpaceDeclaration = false;
            wordParser.moveBackward(count);
        }
        //Check if from part is in "FROM Entity this" form with
        if (this.hasDeclaration() && this.getDeclaration() instanceof IdentificationVariableDeclaration identificationVariableDeclaration &&
            identificationVariableDeclaration.hasRangeVariableDeclaration() && identificationVariableDeclaration.getRangeVariableDeclaration() instanceof RangeVariableDeclaration rangeVariableDeclaration &&
            rangeVariableDeclaration.hasIdentificationVariable() && rangeVariableDeclaration.getIdentificationVariable() instanceof IdentificationVariable identificationVariable &&
            Expression.THIS.equals(identificationVariable.getText())) {
            this.getParentExpression().setGenerateImplicitThisAlias(true);
        }
    }

    @Override
    protected boolean shouldParseWithFactoryFirst() {
        return true;
    }

    @Override
    protected void toParsedText(StringBuilder writer, boolean actual) {

        // 'FROM'
        writer.append(actual ? identifier : FROM);

        if (hasSpace) {
            writer.append(SPACE);
        }

        // Declaration
        if (declaration != null) {
            declaration.toParsedText(writer, actual);
        }

        if (hasSpaceDeclaration) {
            writer.append(SPACE);
        }

        // Hierarchical query clause
        if (hierarchicalQueryClause != null) {
            hierarchicalQueryClause.toParsedText(writer, actual);
        }

        if (hasSpaceAfterHierarchicalQueryClause) {
            writer.append(SPACE);
        }

        // AS OF clause
        if (asOfClause != null) {
            asOfClause.toParsedText(writer, actual);
        }
    }
}
