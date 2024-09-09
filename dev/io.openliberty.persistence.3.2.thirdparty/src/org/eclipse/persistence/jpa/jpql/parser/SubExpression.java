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

import org.eclipse.persistence.jpa.jpql.ExpressionTools;

/**
 * This expression wraps a sub-expression within parenthesis.
 *
 * <div><b>BNF:</b> <code>expression ::= (sub_expression)</code></div>
 *
 * @version 2.5
 * @since 2.3
 * @author Pascal Filion
 */
public final class SubExpression extends AbstractSingleEncapsulatedExpression implements ParentExpression {

    /**
     * The {@link JPQLQueryBNF} coming from the parent that is used to parse the next portion of the query.
     */
    private JPQLQueryBNF queryBNF;

    private boolean generateThisPrefix = false;

    /**
     * Creates a new <code>SubExpression</code>.
     *
     * @param parent The parent of this expression
     * @param queryBNF The BNF coming from the parent that is used to parse the next portion of the query
     */
    public SubExpression(AbstractExpression parent, JPQLQueryBNF queryBNF) {
        super(parent, ExpressionTools.EMPTY_STRING);
        this.queryBNF = queryBNF;
    }

    @Override
    public boolean isGenerateImplicitThisAlias() {
        return generateThisPrefix;
    }

    @Override
    public void setGenerateImplicitThisAlias(boolean generateImplicitThisAlias) {
        this.generateThisPrefix = generateImplicitThisAlias;
    }

    @Override
    public boolean isParentExpression() {
        return true;
    }

    @Override
    public boolean isSubExpression() {
        return true;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected boolean areLogicalIdentifiersSupported() {
        return true;
    }

    @Override
    public String getEncapsulatedExpressionQueryBNFId() {
        return queryBNF.getId();
    }

    @Override
    public JPQLQueryBNF findQueryBNF(Expression expression) {

        if (hasExpression() && (getExpression().isAncestor(expression))) {
            return queryBNF;
        }

        return getParent().findQueryBNF(expression);
    }

    @Override
    public JPQLQueryBNF getQueryBNF() {
        return queryBNF;
    }

    @Override
    protected boolean handleCollection(JPQLQueryBNF queryBNF) {
        return true;
    }
}
