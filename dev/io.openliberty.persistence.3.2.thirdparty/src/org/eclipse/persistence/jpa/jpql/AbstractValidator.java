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
package org.eclipse.persistence.jpa.jpql;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.persistence.jpa.jpql.parser.AbstractExpressionVisitor;
import org.eclipse.persistence.jpa.jpql.parser.AbstractTraverseParentVisitor;
import org.eclipse.persistence.jpa.jpql.parser.AnonymousExpressionVisitor;
import org.eclipse.persistence.jpa.jpql.parser.BadExpression;
import org.eclipse.persistence.jpa.jpql.parser.CollectionExpression;
import org.eclipse.persistence.jpa.jpql.parser.DeleteClause;
import org.eclipse.persistence.jpa.jpql.parser.DeleteStatement;
import org.eclipse.persistence.jpa.jpql.parser.Expression;
import org.eclipse.persistence.jpa.jpql.parser.ExpressionRegistry;
import org.eclipse.persistence.jpa.jpql.parser.ExpressionVisitor;
import org.eclipse.persistence.jpa.jpql.parser.FromClause;
import org.eclipse.persistence.jpa.jpql.parser.GroupByClause;
import org.eclipse.persistence.jpa.jpql.parser.HavingClause;
import org.eclipse.persistence.jpa.jpql.parser.IdentificationVariable;
import org.eclipse.persistence.jpa.jpql.parser.JPQLGrammar;
import org.eclipse.persistence.jpa.jpql.parser.JPQLQueryBNF;
import org.eclipse.persistence.jpa.jpql.parser.NullExpression;
import org.eclipse.persistence.jpa.jpql.parser.OrderByClause;
import org.eclipse.persistence.jpa.jpql.parser.SelectClause;
import org.eclipse.persistence.jpa.jpql.parser.SelectStatement;
import org.eclipse.persistence.jpa.jpql.parser.SimpleFromClause;
import org.eclipse.persistence.jpa.jpql.parser.SimpleSelectClause;
import org.eclipse.persistence.jpa.jpql.parser.SimpleSelectStatement;
import org.eclipse.persistence.jpa.jpql.parser.StateFieldPathExpression;
import org.eclipse.persistence.jpa.jpql.parser.SubExpression;
import org.eclipse.persistence.jpa.jpql.parser.UnionClause;
import org.eclipse.persistence.jpa.jpql.parser.UnknownExpression;
import org.eclipse.persistence.jpa.jpql.parser.UpdateClause;
import org.eclipse.persistence.jpa.jpql.parser.UpdateStatement;
import org.eclipse.persistence.jpa.jpql.parser.WhereClause;
import org.eclipse.persistence.jpa.jpql.utility.CollectionTools;

/**
 * The abstract definition of a validator, which provides helper methods and visitors.
 * <p>
 * Provisional API: This interface is part of an interim API that is still under development and
 * expected to change significantly before reaching stability. It is available at this early stage
 * to solicit feedback from pioneering adopters on the understanding that any code that uses this
 * API will almost certainly be broken (repeatedly) as the API evolves.
 *
 * @see AbstractGrammarValidator
 * @see AbstractSemanticValidator
 *
 * @version 2.5
 * @since 2.4
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public abstract class AbstractValidator extends AnonymousExpressionVisitor {

    /**
     * This visitor is responsible to traverse the children of a {@link CollectionExpression} in
     * order to properly validate the {@link Expression}.
     */
    private BypassChildCollectionExpressionVisitor bypassChildCollectionExpressionVisitor;

    /**
     * This visitor is responsible to traverse the parent hierarchy and to skip {@link SubExpression}
     * if it's a parent.
     */
    private BypassParentSubExpressionVisitor bypassParentSubExpressionVisitor;

    /**
     * This visitor gathers the children of a {@link CollectionExpression} or a single visited
     * {@link Expression}.
     */
    private ChildrenCollectorVisitor childrenCollectorVisitor;

    /**
     * This visitor is used to retrieve a variable name from various type of
     * {@link org.eclipse.persistence.jpa.jpql.parser.Expression JPQL Expression}.
     */
    private LiteralVisitor literalVisitor;

    /**
     *
     *
     * @since 2.5
     */
    private NestedArrayVisitor nestedArrayVisitor;

    /**
     * This visitor is responsible to traverse the parent hierarchy and to retrieve the owning clause
     * of the {@link Expression} being visited.
     */
    private OwningClauseVisitor owningClauseVisitor;

    /**
     * This visitor is responsible to traverse the parent hierarchy and to retrieve the owning
     * statement of the {@link Expression} being visited.
     *
     * @since 2.4
     */
    private OwningStatementVisitor owningStatementVisitor;

    /**
     * The list of {@link JPQLQueryProblem} describing grammatical and semantic issues found in the query.
     */
    private Collection<JPQLQueryProblem> problems;

    /**
     * This visitor determines whether the visited {@link Expression} is a subquery or not.
     *
     * @since 2.5
     */
    private SubqueryVisitor subqueryVisitor;

    /**
     * The {@link JPQLQueryBNFValidator} mapped by the BNF IDs.
     */
    private Map<String, JPQLQueryBNFValidator> validators;

    /**
     * Creates a new <code>AbstractValidator</code>.
     */
    @SuppressWarnings("this-escape")
    protected AbstractValidator() {
        super();
        initialize();
    }

    /**
     * Adds a new validation problem that was found in the given {@link Expression}.
     *
     * @param expression The {@link Expression} that is either not following the BNF grammar or that
     * has semantic problems
     * @param startPosition The position where the problem was encountered
     * @param endPosition The position where the problem ends, inclusively
     * @param messageKey The key used to retrieve the localized message describing the problem
     * @param messageArguments The list of arguments that can be used to format the localized
     * description of the problem
     */
    protected void addProblem(Expression expression,
                              int startPosition,
                              int endPosition,
                              String messageKey,
                              String... messageArguments) {

        problems.add(buildProblem(expression, startPosition, endPosition, messageKey, messageArguments));
    }

    /**
     * Adds a new validation problem that was found in the given {@link Expression}.
     *
     * @param expression The {@link Expression} that is either not following the BNF grammar or that
     * has semantic problems
     * @param startPosition The position where the problem was encountered
     * @param messageKey The key used to retrieve the localized message describing the problem
     * @param messageArguments The list of arguments that can be used to format the localized
     * description of the problem
     */
    protected void addProblem(Expression expression,
                              int startPosition,
                              String messageKey,
                              String... messageArguments) {

        addProblem(expression, startPosition, startPosition, messageKey, messageArguments);
    }

    /**
     * Adds a new validation problem that was found in the given {@link Expression}. The start index
     * is the position of the given {@link Expression} within the JPQL query and the end index is
     * the end position of the {@link Expression} within the JPQL query.
     *
     * @param expression The {@link Expression} that is either not following the BNF grammar or that
     * has semantic problems
     * @param messageKey The key used to retrieve the localized message describing the problem
     */
    protected void addProblem(Expression expression, String messageKey) {
        addProblem(expression, messageKey, ExpressionTools.EMPTY_STRING_ARRAY);
    }

    /**
     * Adds a new validation problem that was found in the given {@link Expression}. The start index
     * is the position of the given {@link Expression} within the JPQL query and the end index is
     * the end position of the {@link Expression} within the JPQL query.
     *
     * @param expression The {@link Expression} that is either not following the BNF grammar or that
     * has semantic problems
     * @param messageKey The key used to retrieve the localized message describing the problem
     * @param arguments The list of arguments that can be used to format the localized description of
     * the problem
     */
    protected void addProblem(Expression expression, String messageKey, String... arguments) {
        int startPosition = expression.getOffset();
        int endPosition   = startPosition + length(expression);
        addProblem(expression, startPosition, endPosition, messageKey, arguments);
    }

    protected ChildrenCollectorVisitor buildChildrenCollector() {
        return new ChildrenCollectorVisitor();
    }

    /**
     * Creates the visitor that can retrieve some information about various literal.
     *
     * @return A new {@link LiteralVisitor}
     */
    protected abstract LiteralVisitor buildLiteralVisitor();

    /**
     * Creates the visitor that traverses an {@link Expression} and determines if it's a nested array
     * or not.
     *
     * @return A new {@link NestedArrayVisitor}
     * @since 2.5
     */
    protected NestedArrayVisitor buildNestedArrayVisitor() {
        return new NestedArrayVisitor();
    }

    /**
     * Creates the visitor that traverses the parent hierarchy of any {@link Expression} and stops at
     * the first {@link Expression} that is a clause.
     *
     * @return A new {@link OwningClauseVisitor}
     */
    protected OwningClauseVisitor buildOwningClauseVisitor() {
        return new OwningClauseVisitor();
    }

    /**
     * Creates the visitor that traverses the parent hierarchy of any {@link Expression} and stops at
     * the first {@link Expression} that is a statement.
     *
     * @return A new {@link OwningStatementVisitor}
     * @since 2.4
     */
    protected OwningStatementVisitor buildOwningStatementVisitor() {
        return new OwningStatementVisitor();
    }

    /**
     * Creates a new validation problem that was found in the given {@link Expression}.
     *
     * @param expression The {@link Expression} that is either not following the BNF grammar or that
     * has semantic problems
     * @param startPosition The position where the problem was encountered
     * @param endPosition The position where the problem ends, inclusively
     * @param messageKey The key used to retrieve the localized message describing the problem
     * @param messageArguments The list of arguments that can be used to format the localized
     * description of the problem
     * @return The {@link JPQLQueryProblem} describing a problem
     */
    protected JPQLQueryProblem buildProblem(Expression expression,
                                            int startPosition,
                                            int endPosition,
                                            String messageKey,
                                            String... messageArguments) {

        return new DefaultJPQLQueryProblem(
            expression,
            startPosition,
            endPosition,
            messageKey,
            messageArguments
        );
    }

    /**
     * Creates the visitor that checks if the visited expression is a subquery or not..
     *
     * @return A new {@link SubqueryVisitor}
     * @since 2.5
     */
    protected SubqueryVisitor buildSubqueryVisitor() {
        return new SubqueryVisitor();
    }

    /**
     * Disposes this visitor.
     */
    public void dispose() {
        problems = null;
    }

    protected BypassChildCollectionExpressionVisitor getBypassChildCollectionExpressionVisitor() {
        if (bypassChildCollectionExpressionVisitor == null) {
            bypassChildCollectionExpressionVisitor = new BypassChildCollectionExpressionVisitor();
        }
        return bypassChildCollectionExpressionVisitor;
    }

    protected BypassParentSubExpressionVisitor getBypassParentSubExpressionVisitor() {
        if (bypassParentSubExpressionVisitor == null) {
            bypassParentSubExpressionVisitor = new BypassParentSubExpressionVisitor();
        }
        return bypassParentSubExpressionVisitor;
    }

    /**
     * Returns a list containing either the given {@link Expression} if it's not a {@link
     * CollectionExpression} or the children of the given {@link CollectionExpression}.
     *
     * @param expression The {@link Expression} to visit
     * @return A list containing either the given {@link Expression} or the children of {@link
     * CollectionExpression}
     */
    protected List<Expression> getChildren(Expression expression) {
        ChildrenCollectorVisitor visitor = getChildrenCollectorVisitor();
        try {
            visitor.expressions = new LinkedList<>();
            expression.accept(visitor);
            return visitor.expressions;
        }
        finally {
            visitor.expressions = null;
        }
    }

    protected ChildrenCollectorVisitor getChildrenCollectorVisitor() {
        if (childrenCollectorVisitor == null) {
            childrenCollectorVisitor = buildChildrenCollector();
        }
        return childrenCollectorVisitor;
    }

    /**
     * Returns the registry containing the {@link JPQLQueryBNF JPQLQueryBNFs} and the {@link
     * org.eclipse.persistence.jpa.jpql.parser.ExpressionFactory ExpressionFactories} that are used
     * to properly parse a JPQL query.
     *
     * @return The registry containing the information related to the JPQL grammar
     */
    protected ExpressionRegistry getExpressionRegistry() {
        return getGrammar().getExpressionRegistry();
    }

    protected JPQLQueryBNFValidator getExpressionValidator(String queryBNF) {
        JPQLQueryBNFValidator validator = validators.get(queryBNF);
        if (validator == null) {
            validator = new JPQLQueryBNFValidator(getExpressionRegistry().getQueryBNF(queryBNF));
            validators.put(queryBNF, validator);
        }
        return validator;
    }

    /**
     * Returns the {@link JPQLGrammar} that defines how the JPQL query was parsed.
     *
     * @return The {@link JPQLGrammar} that was used to parse the JPQL query
     */
    protected abstract JPQLGrammar getGrammar();

    /**
     * Returns the version of the Java Persistence this entity for which it was defined.
     *
     * @return The version of the Java Persistence being used
     */
    protected JPAVersion getJPAVersion() {
        return getGrammar().getJPAVersion();
    }

    /**
     * Returns the {@link JPQLQueryBNFValidator} that can be used to validate an {@link Expression}
     * by making sure its BNF is part of the given BNF.
     *
     * @param queryBNF The BNF used to determine the validity of an {@link Expression}
     * @return A {@link JPQLQueryBNFValidator} that can determine if an {@link Expression} follows
     * the given BNF
     */
    protected JPQLQueryBNFValidator getJPQLQueryBNFValidator(JPQLQueryBNF queryBNF) {
        JPQLQueryBNFValidator validator = validators.get(queryBNF.getId());
        if (validator == null) {
            validator = new JPQLQueryBNFValidator(queryBNF);
            validators.put(queryBNF.getId(), validator);
        }
        return validator;
    }

    /**
     * Returns the {@link JPQLQueryBNFValidator} that can be used to validate an {@link Expression}
     * by making sure its BNF is part of the given BNF.
     *
     * @param queryBNF The BNF used to determine the validity of an {@link Expression}
     * @return A {@link JPQLQueryBNFValidator} that can determine if an {@link Expression} follows
     * the given BNF
     */
    protected JPQLQueryBNFValidator getJPQLQueryBNFValidator(String queryBNF) {
        return getJPQLQueryBNFValidator(getQueryBNF(queryBNF));
    }

    /**
     * Returns the visitor that can retrieve some information about various literal.
     *
     * @return A {@link LiteralVisitor}
     */
    protected LiteralVisitor getLiteralVisitor() {
        if (literalVisitor == null) {
            literalVisitor = buildLiteralVisitor();
        }
        return literalVisitor;
    }

    /**
     * Returns the visitor that can determine if an {@link Expression} represents a nested array.
     *
     * @return A {@link NestedArrayVisitor}
     * @since 2.5
     */
    protected NestedArrayVisitor getNestedArrayVisitor() {
        if (nestedArrayVisitor == null) {
            nestedArrayVisitor = buildNestedArrayVisitor();
        }
        return nestedArrayVisitor;
    }

    /**
     * Returns the visitor that traverses the parent hierarchy of any {@link Expression} and stops at
     * the first {@link Expression} that is a clause.
     *
     * @return {@link OwningClauseVisitor}
     */
    protected OwningClauseVisitor getOwningClauseVisitor() {
        if (owningClauseVisitor == null) {
            owningClauseVisitor = buildOwningClauseVisitor();
        }
        return owningClauseVisitor;
    }

    /**
     * Returns the visitor that traverses the parent hierarchy of any {@link Expression} and stops at
     * the first {@link Expression} that is a statement.
     *
     * @return {@link OwningStatementVisitor}
     * @since 2.4
     */
    protected OwningStatementVisitor getOwningStatementVisitor() {
        if (owningStatementVisitor == null) {
            owningStatementVisitor = buildOwningStatementVisitor();
        }
        return owningStatementVisitor;
    }

    /**
     * Returns the persistence provider name.
     *
     * @return The name of the persistence provider, <code>null</code> should never be returned
     * @since 2.5
     */
    protected String getProvider() {
        return getGrammar().getProvider();
    }

    /**
     * Returns the version of the persistence provider.
     *
     * @return The version of the persistence provider, if one is extending the default JPQL grammar
     * defined in the Java Persistence specification, otherwise returns an empty string
     * @since 2.4
     */
    protected String getProviderVersion() {
        return getGrammar().getProviderVersion();
    }

    /**
     * Retrieves the BNF object that was registered for the given unique identifier.
     *
     * @param queryBNFId The unique identifier of the {@link JPQLQueryBNF} to retrieve
     * @return The {@link JPQLQueryBNF} representing a section of the grammar
     */
    protected JPQLQueryBNF getQueryBNF(String queryBNFId) {
        return getGrammar().getExpressionRegistry().getQueryBNF(queryBNFId);
    }

    /**
     * Returns the visitor that checks if the visited expression is a subquery or not.
     *
     * @return {@link SubqueryVisitor}
     * @since 2.5
     */
    protected SubqueryVisitor getSubqueryVisitor() {
        if (subqueryVisitor == null) {
            subqueryVisitor = buildSubqueryVisitor();
        }
        return subqueryVisitor;
    }

    /**
     * Initializes this validator.
     */
    protected void initialize() {
        validators = new HashMap<>();
    }

    /**
     * Determines whether the given {@link Expression} represents a nested array or not. To be a
     * nested array, the given {@link Expression} is a {@link SubExpression} and its child is a
     * {@link CollectionExpression}.
     *
     * @param expression The {@link Expression} to check its size
     * @return <code>true</code> if the given {@link Expression} is a nested array; <code>false</code> otherwise
     * @since 2.5
     */
    protected boolean isNestedArray(Expression expression) {
        return nestedArraySize(expression) > -1;
    }

    /**
     * Determines whether the given {@link Expression} is a subquery.
     *
     * @param expression The {@link Expression} to check its type
     * @return <code>true</code> if the given {@link Expression} is a subquery; <code>false</code> otherwise
     * @since 2.5
     */
    protected boolean isSubquery(Expression expression) {
        SubqueryVisitor visitor = getSubqueryVisitor();
        try {
            expression.accept(visitor);
            return visitor.expression != null;
        }
        finally {
            visitor.expression = null;
        }
    }

    /**
     * Determines whether the given {@link Expression} is valid by checking its {@link JPQLQueryBNF}
     * with the given {@link JPQLQueryBNF}.
     *
     * @param expression The {@link Expression} to validate based on the query BNF
     * @param queryBNF The {@link JPQLQueryBNF} that determines if the given {@link Expression} is valid
     * @return <code>true</code> if the {@link Expression}'s {@link JPQLQueryBNF} is either the
     * {@link JPQLQueryBNF} or a child of it; <code>false</code> otherwise
     */
    protected boolean isValid(Expression expression, JPQLQueryBNF queryBNF) {
        JPQLQueryBNFValidator validator = getJPQLQueryBNFValidator(queryBNF);
        try {
            expression.accept(validator);
            return validator.valid;
        }
        finally {
            validator.valid = false;
        }
    }

    /**
     * Determines whether the given {@link Expression} is valid by checking its {@link JPQLQueryBNF}
     * with the {@link JPQLQueryBNF} associated with the given unique identifier.
     *
     * @param expression The {@link Expression} to validate based on the query BNF
     * @param queryBNFId The unique identifier of the {@link JPQLQueryBNF} that determines if the
     * given {@link Expression} is valid
     * @return <code>true</code> if the {@link Expression}'s {@link JPQLQueryBNF} is either the
     * {@link JPQLQueryBNF} or a child of it; <code>false</code> otherwise
     */
    protected boolean isValid(Expression expression, String queryBNFId) {
        return isValid(expression, getQueryBNF(queryBNFId));
    }

    /**
     * Determines whether the given {@link Expression} is valid by checking its {@link JPQLQueryBNF}
     * with the list of {@link JPQLQueryBNF} associated with the given unique identifiers.
     *
     * @param expression The {@link Expression} to validate based on the query BNF
     * @param queryBNFIds The unique identifier of the {@link JPQLQueryBNF} that determines if the
     * given {@link Expression} is valid
     * @return <code>true</code> if the {@link Expression}'s {@link JPQLQueryBNF} is either the
     * {@link JPQLQueryBNF} or a child of it; <code>false</code> otherwise
     */
    protected boolean isValid(Expression expression, String... queryBNFIds) {

        for (String queryBNFId : queryBNFIds) {
            if (isValid(expression, queryBNFId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether the given {@link Expression} part is an expression of the given query BNF.
     * The {@link CollectionExpression} that may be the direct child of the given {@link Expression}
     * will be bypassed.
     *
     * @param expression The {@link Expression} to validate based on the query BNF
     * @param queryBNF The unique identifier of the {@link JPQLQueryBNF} that looks up 
     * the {@link JPQLQueryBNFValidator}
     * @return <code>true</code> if the {@link Expression} part is a child of the given query BNF;
     * <code>false</code> otherwise
     */
    protected boolean isValidWithChildCollectionBypass(Expression expression, String queryBNF) {
        JPQLQueryBNFValidator validator = getExpressionValidator(queryBNF);
        BypassChildCollectionExpressionVisitor bypassValidator = getBypassChildCollectionExpressionVisitor();
        try {
            bypassValidator.visitor = validator;
            expression.accept(bypassValidator);
            return validator.valid;
        }
        finally {
            bypassValidator.visitor = null;
            validator.valid = false;
        }
    }

    /**
     * Determines whether the given {@link Expression} is part of a subquery.
     *
     * @param expression The {@link Expression} to start scanning its location
     * @return <code>true</code> if the given {@link Expression} is part of a subquery; <code>false</code>
     * if it's part of the top-level query
     * @since 2.4
     */
    protected boolean isWithinSubquery(Expression expression) {
        OwningStatementVisitor visitor = getOwningStatementVisitor();
        try {
            expression.accept(visitor);
            return visitor.simpleSelectStatement != null;
        }
        finally {
            visitor.dispose();
        }
    }

    /**
     * Determines whether the given {@link Expression} is part of the top-level query.
     *
     * @param expression The {@link Expression} to start scanning its location
     * @return <code>true</code> if the given {@link Expression} is part of the top-level query;
     * <code>false</code> if it's part of a subquery
     * @since 2.4
     */
    protected boolean isWithinTopLevelQuery(Expression expression) {
        OwningStatementVisitor visitor = getOwningStatementVisitor();
        try {
            expression.accept(visitor);
            return visitor.deleteStatement != null ||
                   visitor.selectStatement != null ||
                   visitor.updateStatement != null;
        }
        finally {
            visitor.dispose();
        }
    }

    /**
     * Returns the length of the string representation of the given {@link Expression}.
     *
     * @param expression The {@link Expression} to retrieve the length of its string
     * @return The length of the string representation of the given {@link Expression}
     */
    protected int length(Expression expression) {
        return expression.getLength();
    }

    /**
     * Retrieves the "literal" from the given {@link Expression}. The literal to retrieve depends on
     * the given {@link LiteralType type}. The literal is basically a string value like an
     * identification variable name, an input parameter, a path expression, an abstract schema name,
     * etc.
     *
     * @param expression The {@link Expression} to visit
     * @param type The {@link LiteralType} helps to determine what to retrieve from the visited
     * {@link Expression}
     * @return A value from the given {@link Expression} or an empty string if the given {@link
     * Expression} and the {@link LiteralType} do not match
     */
    protected String literal(Expression expression, LiteralType type) {
        LiteralVisitor visitor = getLiteralVisitor();
        try {
            visitor.setType(type);
            expression.accept(visitor);
            return visitor.literal;
        }
        finally {
            visitor.literal = ExpressionTools.EMPTY_STRING;
        }
    }

    /**
     * Returns the number of items in the nested array if the given {@link Expression} represents one.
     * To be a nested array, the given {@link Expression} is a {@link SubExpression} and its child is
     * a {@link CollectionExpression}.
     *
     * @param expression The {@link Expression} to visit
     * @return The number of items in the array or -1 if the {@link Expression} is not a nested array
     * @since 2.5
     */
    protected int nestedArraySize(Expression expression) {
        NestedArrayVisitor visitor = getNestedArrayVisitor();
        try {
            visitor.nestedArraySize = -1;
            expression.accept(visitor);
            return visitor.nestedArraySize;
        }
        finally {
            visitor.nestedArraySize = -1;
        }
    }

    /**
     * Calculates the position of the given expression by calculating the length of what is before.
     *
     * @param expression The expression to determine its position within the parsed tree
     * @return The length of the string representation of what comes before the given expression
     */
    protected int position(Expression expression) {
        return expression.getOffset();
    }

    /**
     * Returns the current number of problems that were registered during validation.
     *
     * @return The current number of problems
     * @since 2.4
     */
    public final int problemsSize() {
        return problems.size();
    }

    /**
     * Sets the collection that will be used to store {@link JPQLQueryProblem problems} this
     * validator will find in the JPQL query.
     *
     * @param problems A non-<code>null</code> collection that will be used to store the {@link
     * JPQLQueryProblem problems} if any was found
     * @exception NullPointerException The Collection cannot be <code>null</code>
     */
    public void setProblems(Collection<JPQLQueryProblem> problems) {
        Assert.isNotNull(problems, "The Collection cannot be null");
        this.problems = problems;
    }

    @Override
    protected void visit(Expression expression) {
        expression.acceptChildren(this);
    }

    /**
     * This visitor is responsible to traverse the children of a {@link CollectionExpression} in
     * order to properly validate the {@link Expression}.
     */
    public static class BypassChildCollectionExpressionVisitor extends AnonymousExpressionVisitor {

        /**
         * The visitor that will visit the {@link Expression}.
         */
        public JPQLQueryBNFValidator visitor;

        /**
         * Creates a new <code>BypassChildCollectionExpressionVisitor</code>.
         */
        public BypassChildCollectionExpressionVisitor() {
            super();
        }

        @Override
        public void visit(CollectionExpression expression) {
            for (Expression child : expression.children()) {
                child.accept(this);
                if (!visitor.valid) {
                    break;
                }
            }
        }

        @Override
        protected void visit(Expression expression) {
            expression.accept(visitor);
        }

        @Override
        public void visit(NullExpression expression) {
            // Ignore this, it should be validated by another validator
        }
    }

    /**
     * This visitor is responsible to traverse the parent hierarchy and to skip {@link SubExpression}
     * if it's a parent.
     */
    public static class BypassParentSubExpressionVisitor extends AnonymousExpressionVisitor {

        /**
         * The {@link ExpressionVisitor} that will visit the {@link Expression}.
         */
        public ExpressionVisitor visitor;

        /**
         * Creates a new <code>BypassParentSubExpressionVisitor</code>.
         */
        public BypassParentSubExpressionVisitor() {
            super();
        }

        @Override
        protected void visit(Expression expression) {
            expression.accept(visitor);
        }

        @Override
        public void visit(SubExpression expression) {
            expression.getParent().accept(this);
        }
    }

    /**
     * This visitor gathers the children of a {@link CollectionExpression} or a single visited
     * {@link Expression}.
     */
    public static class ChildrenCollectorVisitor extends AnonymousExpressionVisitor {

        /**
         * The unique {@link Expression} that was visited or the children of {@link CollectionExpression}.
         */
        protected List<Expression> expressions;

        /**
         * Creates a new <code>ChildrenCollectorVisitor</code>.
         */
        public ChildrenCollectorVisitor() {
            super();
        }

        @Override
        public void visit(CollectionExpression expression) {
            CollectionTools.addAll(expressions, expression.children());
        }

        @Override
        protected void visit(Expression expression) {
            expressions.add(expression);
        }

        @Override
        public void visit(NullExpression expression) {
            // Don't add it
        }
    }

    /**
     * This visitor validates any {@link Expression} by checking its BNF against some BNFs.
     */
    public static class JPQLQueryBNFValidator extends AnonymousExpressionVisitor {

        /**
         *
         */
        protected boolean bypassCompound;

        /**
         * The {@link JPQLQueryBNF} used to determine if the expression's BNF is valid.
         */
        private JPQLQueryBNF queryBNF;

        /**
         * Determines whether the visited {@link Expression}'s BNF is valid based on the BNF that was
         * used for validation.
         */
        protected boolean valid;

        /**
         * Creates a new <code>JPQLQueryBNFValidator</code>.
         *
         * @param queryBNF The {@link JPQLQueryBNF} used to determine if the expression's BNF is valid
         */
        public JPQLQueryBNFValidator(JPQLQueryBNF queryBNF) {
            super();
            this.queryBNF = queryBNF;
        }

        private void allJPQLQueryBNFs(Set<String> queryBNFIds, JPQLQueryBNF queryBNF) {
            if (queryBNFIds.add(queryBNF.getId()) && (bypassCompound || !queryBNF.isCompound())) {
                for (JPQLQueryBNF childQueryBNF : queryBNF.nonCompoundChildren()) {
                    allJPQLQueryBNFs(queryBNFIds, childQueryBNF);
                }
            }
        }

        /**
         * Disposes of the internal data.
         */
        public void dispose() {
            valid = false;
            bypassCompound = false;
        }

        /**
         * Determines whether the visited {@link Expression} is valid or not based on the {@link
         * JPQLQueryBNF} that was specified.
         *
         * @return <code>true</code> if the {@link Expression} is valid; <code>false</code> otherwise
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Sets bypassCompound
         * 
         * @param bypassCompound Indicates whether a {@link JPQLQueryBNF} representing a compound
         * expression should be considered when doing the validation
         */
        public void setBypassCompound(boolean bypassCompound) {
            this.bypassCompound = bypassCompound;
        }

        /**
         * Validates the given {@link JPQLQueryBNF} by making sure it is the one expected or one of
         * the children from the "root" BNF passed to this validator's constructor.
         *
         * @param queryBNF The {@link JPQLQueryBNF} to validate
         */
        public void validate(JPQLQueryBNF queryBNF) {

            // By setting the flag to false will assure that if this validator is used for
            // more than one item, it will reflect the global validity state. If all are
            // valid, then the last expression will set the flag to true
            valid = false;

            // Quick check
            if (queryBNF.getId() == this.queryBNF.getId()) {
                valid = true;
            }
            // Retrieve all the children from the "root" JPQLQueryBNF and
            // check if the BNF to validate is one of those children
            else {
                Set<String> allQueryBNFIds = new HashSet<>();
                allJPQLQueryBNFs(allQueryBNFIds, this.queryBNF);
                valid = allQueryBNFIds.contains(queryBNF.getId());
            }
        }

        @Override
        public void visit(BadExpression expression) {
            // This is not a valid expression
        }

        @Override
        public void visit(CollectionExpression expression) {
            // A collection expression is never valid
            valid = false;
        }

        @Override
        protected void visit(Expression expression) {
            validate(expression.getQueryBNF());
        }

        @Override
        public void visit(NullExpression expression) {
            // The missing expression is validated by GrammarValidator
            valid = true;
        }

        @Override
        public void visit(StateFieldPathExpression expression) {
            JPQLQueryBNF originQueryBNF = queryBNF;
            if (Expression.THIS.equalsIgnoreCase(expression.toString()) &&
                    expression.getParentExpression().isGenerateImplicitThisAlias() &&
                    expression.getIdentificationVariable() != null &&
                    ((IdentificationVariable)(expression.getIdentificationVariable())).isVirtual()) {
                queryBNF = expression.getQueryBNF();
            }
            visit((Expression) expression);
            queryBNF = originQueryBNF;
        }

        @Override
        public void visit(SubExpression expression) {
            if (expression.hasExpression()) {
                expression.getExpression().accept(this);
            }
        }

        @Override
        public void visit(UnknownExpression expression) {
            // This is not a valid expression
        }
    }

    protected static class NestedArrayVisitor extends AbstractExpressionVisitor {

        /**
         * The number of items contained in the nested array or -1 if the {@link Expression} does not
         * represent a nested array.
         */
        public int nestedArraySize;

        /**
         * Internal flag used to determine if a sub-expression is traversed, which is required when
         * representing a nested array.
         */
        protected boolean subExpression;

        /**
         * Default constructor.
         */
        protected NestedArrayVisitor() {
        }

        @Override
        public void visit(CollectionExpression expression) {
            nestedArraySize = subExpression ? expression.childrenSize() : -1;
        }

        @Override
        public void visit(SubExpression expression) {
            subExpression = true;
            expression.getExpression().accept(this);
            subExpression = false;
        }
    }

    /**
     * This visitor retrieves the clause owning the visited {@link Expression}.
     */
    public static class OwningClauseVisitor extends AbstractTraverseParentVisitor {

        public DeleteClause deleteClause;
        public FromClause fromClause;
        public GroupByClause groupByClause;
        public HavingClause havingClause;
        public OrderByClause orderByClause;
        public SelectClause selectClause;
        public SimpleFromClause simpleFromClause;
        public SimpleSelectClause simpleSelectClause;
        public UpdateClause updateClause;
        public WhereClause whereClause;
        public UnionClause unionClause;

        /**
         * Creates a new <code>OwningClauseVisitor</code>.
         */
        public OwningClauseVisitor() {
            super();
        }

        /**
         * Disposes the internal data.
         */
        public void dispose() {
            deleteClause       = null;
            fromClause         = null;
            groupByClause      = null;
            havingClause       = null;
            orderByClause      = null;
            selectClause       = null;
            simpleFromClause   = null;
            simpleSelectClause = null;
            updateClause       = null;
            whereClause        = null;
            unionClause        = null;
        }

        @Override
        public void visit(DeleteClause expression) {
            deleteClause = expression;
        }

        @Override
        public void visit(FromClause expression) {
            fromClause = expression;
        }

        @Override
        public void visit(GroupByClause expression) {
            groupByClause = expression;
        }

        @Override
        public void visit(HavingClause expression) {
            havingClause = expression;
        }

        @Override
        public void visit(OrderByClause expression) {
            orderByClause = expression;
        }

        @Override
        public void visit(SelectClause expression) {
            selectClause = expression;
        }

        @Override
        public void visit(SimpleFromClause expression) {
            simpleFromClause = expression;
        }

        @Override
        public void visit(SimpleSelectClause expression) {
            simpleSelectClause = expression;
        }

        @Override
        public void visit(UpdateClause expression) {
            updateClause = expression;
        }

        @Override
        public void visit(WhereClause expression) {
            whereClause = expression;
        }

        @Override
        public void visit(UnionClause expression) {
            this.unionClause = expression;
        }

    }

    /**
     * This visitor retrieves the statement owning the visited {@link Expression}.
     */
    protected static class OwningStatementVisitor extends AbstractTraverseParentVisitor {

        public DeleteStatement deleteStatement;
        public SelectStatement selectStatement;
        public SimpleSelectStatement simpleSelectStatement;
        public UpdateStatement updateStatement;

        /**
         * Default constructor.
         */
        protected OwningStatementVisitor() {
        }

        /**
         * Disposes the internal data.
         */
        protected void dispose() {
            deleteStatement       = null;
            selectStatement       = null;
            simpleSelectStatement = null;
            updateStatement       = null;
        }

        @Override
        public void visit(DeleteStatement expression) {
            deleteStatement = expression;
        }

        @Override
        public void visit(SelectStatement expression) {
            selectStatement = expression;
        }

        @Override
        public void visit(SimpleSelectStatement expression) {
            simpleSelectStatement = expression;
        }

        @Override
        public void visit(UpdateStatement expression) {
            updateStatement = expression;
        }
    }

    /**
     * This visitor retrieves the statement owning the visited {@link Expression}.
     */
    protected static class SubqueryVisitor extends AbstractExpressionVisitor {

        /**
         * The subquery is the visited {@link Expression} is a subquery.
         */
        private SimpleSelectStatement expression;

        /**
         * Default constructor.
         */
        protected SubqueryVisitor() {
        }

        @Override
        public void visit(SimpleSelectStatement expression) {
            this.expression = expression;
        }
    }
}
