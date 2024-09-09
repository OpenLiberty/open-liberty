/*
 * Copyright (c) 2006, 2024 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;

/**
 * This visitor makes sure that all path expressions are fully qualified with a "virtual"
 * identification variable if the range variable declaration does not define one. This only applies
 * to an <code><b>UPDATE</b></code> or <code><b>DELETE</b></code> queries.
 *
 * @version 2.5
 * @since 2.3
 * @author Pascal Filion
 */
public final class FullyQualifyPathExpressionVisitor extends AbstractTraverseChildrenVisitor {

    /**
     * The "virtual" identification variable if none was defined.
     */
    private String variableName;

    /**
     * Caches this visitor, which is used to determine if the general identification variable is not
     * a map key, map value or map entry expression.
     */
    private GeneralIdentificationVariableVisitor visitor;

    /**
     * Default constructor.
     */
    public FullyQualifyPathExpressionVisitor() {
    }

    private GeneralIdentificationVariableVisitor generalIdentificationVariableVisitor() {
        if (visitor == null) {
            visitor = new GeneralIdentificationVariableVisitor();
        }
        return visitor;
    }

    @Override
    public void visit(AbstractSchemaName expression) {
        // The "virtual" variable name will be "this" entity name
        variableName = Expression.THIS;
    }

    @Override
    public void visit(CollectionMemberDeclaration expression) {
        // Do nothing, prevent to do anything for invalid queries
    }

    @Override
    public void visit(CollectionValuedPathExpression expression) {
        visitAbstractPathExpression(expression);
        variableName = Expression.THIS;
    }

    @Override
    public void visit(DeleteClause expression) {
        expression.getRangeVariableDeclaration().accept(this);
    }

    @Override
    public void visit(DeleteStatement expression) {

        expression.getDeleteClause().accept(this);

        // Don't traverse the tree if the path expressions don't need to be virtually qualified
        if ((variableName != null) && expression.hasWhereClause()) {
            expression.getWhereClause().accept(this);
        }
    }

    @Override
    public void visit(IdentificationVariable expression) {

        // A null check is required because the query could be invalid/incomplete
        // The identification variable should become a state field path expression
        expression.setVirtualIdentificationVariable(variableName);
    }

    @Override
    public void visit(Join expression) {
        // Do nothing, prevent to do anything for invalid queries
    }

    @Override
    public void visit(RangeVariableDeclaration expression) {

        // The "root" object does not have an identification variable,
        // then we'll assume all path expressions are unqualified
        if (!expression.hasIdentificationVariable()) {
            expression.getRootObject().accept(this);
            expression.setVirtualIdentificationVariable(variableName);
        }
    }

    @Override
    public void visit(SelectStatement expression) {
        // Nothing to do because a SELECT query has to have its path expressions fully qualified
    }

    @Override
    public void visit(SimpleSelectStatement expression) {
        // Nothing to do because a subquery query has to have its path expressions fully qualified
    }

    @Override
    public void visit(StateFieldPathExpression expression) {
        // A null check is required because the query could be invalid/incomplete
        visitAbstractPathExpression(expression);
    }

    @Override
    public void visit(UpdateClause expression) {

        expression.getRangeVariableDeclaration().accept(this);

        // Don't traverse the tree if the path expressions don't need to be virtually qualified
        if ((variableName != null) && expression.hasUpdateItems()) {
            expression.getUpdateItems().accept(this);
        }
    }

    @Override
    public void visit(UpdateStatement expression) {

        expression.getUpdateClause().accept(this);

        // Don't traverse the tree if the path expressions don't need to be virtually qualified
        if ((variableName != null) && expression.hasWhereClause()) {
            expression.getWhereClause().accept(this);
        }
    }

    private void visitAbstractPathExpression(AbstractPathExpression expression) {

        if (!expression.startsWithDot()) {

            // Visit the general identification variable to make sure it's not a map key, map entry
            // or map value expression
            GeneralIdentificationVariableVisitor visitor = generalIdentificationVariableVisitor();
            expression.getIdentificationVariable().accept(visitor);

            if (visitor.expression == null) {
                expression.setVirtualIdentificationVariable(variableName);
            }
        }
    }

    // Made static final for performance reasons.
    private static final class GeneralIdentificationVariableVisitor extends AbstractExpressionVisitor {

        /**
         * The {@link Expression} that was visited, which is a general identification variable but is
         * not a identification variable, it's either a map value, map key or map value expression.
         */
        private Expression expression;

        @Override
        public void visit(EntryExpression expression) {
            this.expression = expression;
        }

        @Override
        public void visit(KeyExpression expression) {
            this.expression = expression;
        }

        @Override
        public void visit(ValueExpression expression) {
            this.expression = expression;
        }
    }
}
