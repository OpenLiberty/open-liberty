/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates. All rights reserved.
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
//     06/02/2023: Radek Felcman
//       - Issue 1885: Implement new JPQLGrammar for upcoming Jakarta Persistence 3.2
package org.eclipse.persistence.jpa.jpql.parser;

import org.eclipse.persistence.jpa.jpql.ExpressionTools;
import org.eclipse.persistence.jpa.jpql.JPAVersion;

import static org.eclipse.persistence.jpa.jpql.parser.Expression.CAST;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.CONCAT_PIPES;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.ID;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.EXCEPT;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.INTERSECT;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.LEFT;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.REPLACE;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.RIGHT;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.UNION;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.VERSION;

/**
 * This {@link JPQLGrammar} provides support for parsing JPQL queries defined in Jakarta Persistence 3.2.
 * <pre><code> select_statement ::= select_clause from_clause [where_clause] [groupby_clause] [having_clause] [orderby_clause] {union_clause}*
 *
 * union_clause ::= { UNION | INTERSECT | EXCEPT} [ALL] subquery
 *
 * string_expression ::= string_expression || string_term
 *
 * functions_returning_strings ::= REPLACE(string_primary, string_primary, string_primary)
 *
 * functions_returning_string ::= LEFT(string_primary, simple_arithmetic_expression})
 *
 * functions_returning_string ::= RIGHT(string_primary, simple_arithmetic_expression})
 *
 * cast_expression ::= CAST(scalar_expression [AS] database_type)
 *
 * </code></pre>
 */
public class JPQLGrammar3_2 extends AbstractJPQLGrammar {

    /**
     * The singleton instance of this {@link JPQLGrammar3_2}.
     */
    private static final JPQLGrammar INSTANCE = new JPQLGrammar3_2();

    /**
     * Creates an insance of Jakarta Persistence 3.1 JPQL grammar.
     */
    public JPQLGrammar3_2() {
        super();
    }

    /**
     * Creates an instance of Jakarta Persistence 3.2 JPQL grammar.
     *
     * @param jpqlGrammar The {@link JPQLGrammar} to extend with the content of this one without
     * instantiating the base {@link JPQLGrammar}
     */
    private JPQLGrammar3_2(AbstractJPQLGrammar jpqlGrammar) {
        super(jpqlGrammar);
    }

    /**
     * Extends the given {@link JPQLGrammar} with the information of this one without instantiating
     * the base {@link JPQLGrammar}.
     *
     * @param jpqlGrammar The {@link JPQLGrammar} to extend with the content of this one without
     * instantiating the base {@link JPQLGrammar}
     */
    public static void extend(AbstractJPQLGrammar jpqlGrammar) {
        new JPQLGrammar3_2(jpqlGrammar);
    }

    /**
     * Returns the singleton instance of the default implementation of {@link JPQLGrammar} which
     * provides support for the JPQL grammar defined in the Jakarta Persistence 3.2 functional specification.
     *
     * @return The {@link JPQLGrammar} that only has support for Jakarta Persistence 3.2
     */
    public static JPQLGrammar instance() {
        return INSTANCE;
    }

    @Override
    protected JPQLGrammar buildBaseGrammar() {
        return new JPQLGrammar3_1();
    }

    @Override
    public JPAVersion getJPAVersion() {
        return JPAVersion.VERSION_3_2;
    }

    @Override
    public String getProvider() {
        return DefaultJPQLGrammar.PROVIDER_NAME;
    }

    @Override
    public String getProviderVersion() {
        return ExpressionTools.EMPTY_STRING;
    }

    @Override
    protected void initializeBNFs() {
        registerBNF(new StringFactorBNF());
        registerBNF(new StringTermBNF());
        registerBNF(new SimpleStringExpressionBNF());
        registerBNF(new InternalReplacePositionExpressionBNF());
        registerBNF(new InternalReplaceStringExpressionBNF());
        registerBNF(new InternalLeftPositionExpressionBNF());
        registerBNF(new InternalLeftStringExpressionBNF());
        registerBNF(new InternalRightPositionExpressionBNF());
        registerBNF(new InternalRightStringExpressionBNF());
        registerBNF(new UnionClauseBNF());
        registerBNF(new CastExpressionBNF());
        registerBNF(new DatabaseTypeQueryBNF());
        registerBNF(new IdExpressionBNF());
        registerBNF(new VersionExpressionBNF());

        // Extend some query BNFs
        addChildBNF(StringPrimaryBNF.ID,   SimpleStringExpressionBNF.ID);
        addChildFactory(FunctionsReturningStringsBNF.ID, ReplaceExpressionFactory.ID);
        addChildFactory(FunctionsReturningStringsBNF.ID, LeftExpressionFactory.ID);
        addChildFactory(FunctionsReturningStringsBNF.ID, RightExpressionFactory.ID);

        // CAST
        addChildBNF(FunctionsReturningDatetimeBNF.ID,    CastExpressionBNF.ID);
        addChildBNF(FunctionsReturningNumericsBNF.ID,    CastExpressionBNF.ID);
        addChildBNF(FunctionsReturningStringsBNF.ID,     CastExpressionBNF.ID);

        // ID function
        addChildBNF(SelectExpressionBNF.ID,              IdExpressionBNF.ID);
        addChildBNF(ComparisonExpressionBNF.ID,          IdExpressionBNF.ID);
        addChildBNF(IdExpressionBNF.ID, GeneralIdentificationVariableBNF.ID);
        addChildBNF(IdExpressionBNF.ID, SingleValuedObjectPathExpressionBNF.ID);

        // VERSION function
        addChildBNF(SelectExpressionBNF.ID,              VersionExpressionBNF.ID);
        addChildBNF(ComparisonExpressionBNF.ID,          VersionExpressionBNF.ID);
    }

    @Override
    protected void initializeExpressionFactories() {
        registerFactory(new StringExpressionFactory());
        registerFactory(new ReplaceExpressionFactory());
        registerFactory(new LeftExpressionFactory());
        registerFactory(new RightExpressionFactory());
        registerFactory(new UnionClauseFactory());
        registerFactory(new DatabaseTypeFactory());
        registerFactory(new CastExpressionFactory());
        registerFactory(new IdExpressionFactory());
        registerFactory(new VersionExpressionFactory());
    }

    @Override
    protected void initializeIdentifiers() {
        registerIdentifierRole(CONCAT_PIPES,                  IdentifierRole.AGGREGATE);          // x || y
        registerIdentifierVersion(CONCAT_PIPES, JPAVersion.VERSION_3_2);
        registerIdentifierRole(REPLACE,             IdentifierRole.FUNCTION);           // REPLACE(x, y, z)
        registerIdentifierVersion(REPLACE, JPAVersion.VERSION_3_2);
        registerIdentifierRole(LEFT,             IdentifierRole.FUNCTION);           // LEFT(x, y)
        registerIdentifierVersion(LEFT, JPAVersion.VERSION_3_2);
        registerIdentifierRole(RIGHT,             IdentifierRole.FUNCTION);           // REPLACE(x, y)
        registerIdentifierVersion(RIGHT, JPAVersion.VERSION_3_2);
        registerIdentifierRole(UNION,          IdentifierRole.CLAUSE);
        registerIdentifierVersion(UNION,       JPAVersion.VERSION_3_2);
        registerIdentifierRole(INTERSECT,      IdentifierRole.CLAUSE);
        registerIdentifierVersion(INTERSECT,   JPAVersion.VERSION_3_2);
        registerIdentifierRole(EXCEPT,         IdentifierRole.CLAUSE);
        registerIdentifierVersion(EXCEPT,      JPAVersion.VERSION_3_2);
        registerIdentifierRole(CAST,           IdentifierRole.FUNCTION);          // FUNCTION(n, x1, ..., x2)
        registerIdentifierVersion(CAST,        JPAVersion.VERSION_3_2);
        registerIdentifierRole(ID,           IdentifierRole.FUNCTION);          // ID(x)
        registerIdentifierVersion(ID,        JPAVersion.VERSION_3_2);
        registerIdentifierRole(VERSION,           IdentifierRole.FUNCTION);          // VERSION(x)
        registerIdentifierVersion(VERSION,        JPAVersion.VERSION_3_2);
    }

    @Override
    public String toString() {
        return "JPQLGrammar 3.2";
    }
}
