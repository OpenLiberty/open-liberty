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
//     09/02/2019-3.0 Alexandre Jacob
//        - 527415: Fix code when locale is tr, az or lt
package org.eclipse.persistence.jpa.jpql.parser;

import java.util.List;
import java.util.Locale;

import org.eclipse.persistence.jpa.jpql.WordParser;

/**
 * An identification variable is a valid identifier declared in the <b>FROM</b> clause of a query.
 * <p>
 * Requirements:
 * <ul>
 * <li>All identification variables must be declared in the <b>FROM</b> clause. Identification
 * variables cannot be declared in other clauses.
 * <li>An identification variable must not be a reserved identifier or have the same name as any
 * entity in the same persistence unit.
 * <li>Identification variables are case insensitive.
 * <li>An identification variable evaluates to a value of the type of the expression used in
 * declaring the variable.
 * </ul>
 * <p>
 * An identification variable can range over an entity, embeddable, or basic abstract schema type.
 * An identification variable designates an instance of an abstract schema type or an element of a
 * collection of abstract schema type instances.
 * <p>
 * Note that for identification variables referring to an instance of an association or collection
 * represented as a {@link java.util.Map}, the identification variable is of the abstract schema
 * type of the map value.
 * <p>
 * An identification variable always designates a reference to a single value. It is declared in one
 * of three ways:
 * <ul>
 * <li>In a range variable declaration;
 * <li>In a join clause;
 * <li>In a collection member declaration.
 * </ul>
 * The identification variable declarations are evaluated from left to right in the <b>FROM</b>
 * clause, and an identification variable declaration can use the result of a preceding
 * identification variable declaration of the query string.
 * <p>
 * All identification variables used in the <b>SELECT</b>, <b>WHERE</b>, <b>ORDER BY</b>,
 * <b>GROUP BY</b>, or <b>HAVING</b> clause of a <b>SELECT</b> or <b>DELETE</b> statement must be
 * declared in the <b>FROM</b> clause. The identification variables used in the <b>WHERE</b> clause
 * of an <b>UPDATE</b> statement must be declared in the <b>UPDATE</b> clause.
 * <p>
 * An identification variable is scoped to the query (or subquery) in which it is defined and is
 * also visible to any subqueries within that query scope that do not define an identification
 * variable of the same name.
 *
 * @version 2.4
 * @since 2.3
 * @author Pascal Filion
 */
public final class IdentificationVariable extends AbstractExpression {

    /**
     * The virtual state field path expression having
     */
    private StateFieldPathExpression stateFieldPathExpression;

    /**
     * The uppercase version of the identification variable.
     */
    private String variableName;

    /**
     * Determines whether this identification variable is virtual, meaning it's not part of the query
     * but is required for proper navigability.
     */
    private boolean virtual;

    /**
     * Creates a new <code>IdentificationVariable</code>.
     *
     * @param parent The parent of this expression
     * @param identificationVariable The actual identification variable
     */
    public IdentificationVariable(AbstractExpression parent, String identificationVariable) {
        super(parent, identificationVariable);
        //In subqueries "this" generation is not allowed. There are expected qualified IdentificationVariable from query string
        if (!Expression.THIS.equalsIgnoreCase(identificationVariable) && getParentExpression().isGenerateImplicitThisAlias() && !isInsideSubquery()) {
            this.setVirtualIdentificationVariable(Expression.THIS);
        }
    }

    private boolean isInsideSubquery() {
        boolean result = isSubquery(this);
        return result;
    }

    private boolean isSubquery(Expression expression) {
        if (expression == null) {
            return false;
        } else if (expression instanceof AbstractSelectStatement && expression.getParent() != null && expression.getParent() instanceof SubExpression) {
            return true;
        } else {
            return isSubquery(expression.getParent());
        }
    }

    /**
     * Creates a new <code>IdentificationVariable</code>.
     *
     * @param parent The parent of this expression
     * @param identificationVariable The actual identification variable
     * @param virtual Determines whether this identification variable is virtual, meaning it's not
     * part of the query but is required for proper navigability
     */
    public IdentificationVariable(AbstractExpression parent, String identificationVariable, boolean virtual) {
        super(parent, identificationVariable);
        this.virtual = virtual;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        //In "this" (Jakarta Data) generation mode pass for a validation stateFieldPathExpression
        //generated by this.setVirtualIdentificationVariable(Expression.THIS); in constructor above
        if (getParentExpression().isGenerateImplicitThisAlias() && isVirtual() && stateFieldPathExpression != null) {
            visitor.visit(getStateFieldPathExpression());
        } else {
            visitor.visit(this);
        }
    }

    @Override
    public void acceptChildren(ExpressionVisitor visitor) {
        // Nothing to traverse
    }

    @Override
    protected void addOrderedChildrenTo(List<Expression> children) {
        children.add(buildStringExpression(getText()));
    }

    @Override
    public JPQLQueryBNF getQueryBNF() {
        return getQueryBNF(IdentificationVariableBNF.ID);
    }

    /**
     * Returns the actual representation of the parsed information. This method should only be called
     * if {@link #isVirtual()} returns <code>true</code>. This is valid in an <b>UPDATE</b> and
     * <b>DELETE</b> queries where the identification variable is not specified.
     *
     * @return The path expression that is qualified by the virtual identification variable
     * @throws IllegalAccessError If this expression does not have a virtual identification variable
     */
    @SuppressWarnings("nls")
    public StateFieldPathExpression getStateFieldPathExpression() {
        if (!virtual) {
            throw new IllegalAccessError("IdentificationVariable.getStateFieldPathExpression() can only be accessed when it represents an attribute that is not fully qualified, which can be present in an UPDATE or DELETE query.");
        }
        return stateFieldPathExpression;
    }

    @Override
    public String getText() {
        return super.getText();
    }

    /**
     * Returns the identification variable, which has been changed to be upper case.
     *
     * @return The uppercase version of the identification variable
     * @since 2.4
     */
    public String getVariableName() {
        if (variableName == null) {
            variableName = getText().toUpperCase(Locale.ROOT).intern();
        }
        return variableName;
    }

    @Override
    public boolean isVirtual() {
        return virtual;
    }

    @Override
    protected void parse(WordParser wordParser, boolean tolerant) {
        wordParser.moveForward(getText());
    }

    /**
     * Sets a virtual identification variable because the abstract schema name was parsed without
     * one. This is valid in an <b>UPDATE</b> and <b>DELETE</b> queries. This internally transforms
     * the what was thought to be an identification variable to a path expression.
     *
     * @param variableName The identification variable that was generated to identify the "root" object
     */
    public void setVirtualIdentificationVariable(String variableName) {

        virtual = true;

        stateFieldPathExpression = new StateFieldPathExpression(getParent(), getText());
        stateFieldPathExpression.setVirtualIdentificationVariable(variableName);

        rebuildActualText();
        rebuildParsedText();
    }

    @Override
    public String toParsedText() {
        return getText();
    }

    @Override
    protected void toParsedText(StringBuilder writer, boolean actual) {
        writer.append(getText());
    }
}
