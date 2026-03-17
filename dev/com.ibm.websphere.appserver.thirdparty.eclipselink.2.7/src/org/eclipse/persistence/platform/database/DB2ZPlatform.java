/*
 * Copyright (c) 2015, 2023 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, 2026 IBM Corporation. All rights reserved.
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
//     03/19/2015 - Rick Curtis
//       - 462586 : Add national character support for z/OS.
package org.eclipse.persistence.platform.database;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.Calendar;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionOperator;
import org.eclipse.persistence.internal.databaseaccess.BindCallCustomParameter;
import org.eclipse.persistence.internal.databaseaccess.DatasourceCall;
import org.eclipse.persistence.internal.databaseaccess.DatasourceCall.ParameterType;
import org.eclipse.persistence.internal.expressions.CollectionExpression;
import org.eclipse.persistence.internal.expressions.ConstantExpression;
import org.eclipse.persistence.internal.expressions.ExpressionJavaPrinter;
import org.eclipse.persistence.internal.expressions.ExpressionSQLPrinter;
import org.eclipse.persistence.internal.expressions.ParameterExpression;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.security.PrivilegedGetContextClassLoader;
import org.eclipse.persistence.internal.security.PrivilegedGetMethod;
import org.eclipse.persistence.internal.security.PrivilegedMethodInvoker;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.mappings.structures.ObjectRelationalDatabaseField;
import org.eclipse.persistence.platform.database.converters.StructConverter;
import org.eclipse.persistence.queries.StoredProcedureCall;
import org.eclipse.persistence.queries.ValueReadQuery;

/**
 * <b>Purpose</b>: Provides DB2 z/OS specific behavior.
 * <p>
 * This provides for some additional compatibility in certain DB2 versions.
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li>Support creating tables that handle multibyte characters
 * </ul>
 */
public class DB2ZPlatform extends DB2Platform {
    private static String DB2_CALLABLESTATEMENT_CLASS = "com.ibm.db2.jcc.DB2CallableStatement";
    private static String DB2_PREPAREDSTATEMENT_CLASS = "com.ibm.db2.jcc.DB2PreparedStatement";

    public DB2ZPlatform() {
        super();
        this.pingSQL = "SELECT COUNT(*) from SYSIBM.SYSDUMMY1 WHERE 1 = 0";
    }

    @Override
    protected Hashtable buildFieldTypes() {
        Hashtable<Class<?>, Object> res = super.buildFieldTypes();
        if (getUseNationalCharacterVaryingTypeForString()) {
            res.put(String.class, new FieldTypeDefinition("VARCHAR", DEFAULT_VARCHAR_SIZE));
        }
        return res;
    }

    @Override
    public String getTableCreationSuffix() {
        // If we're on Z and using unicode support we need to append CCSID
        // UNICODE on the table rather than FOR MIXED DATA on each column
        if (getUseNationalCharacterVaryingTypeForString()) {
            return " CCSID UNICODE";
        }
        return super.getTableCreationSuffix();
    }

    @Override
    public String getProcedureArgument(String name, Object parameter, ParameterType parameterType, 
            StoredProcedureCall call, AbstractSession session) {
        if (name != null && shouldPrintStoredProcedureArgumentNameInCall()) {
            return ":" + name;
        }
        return "?";
    }

    /**
     * DB2 on Z uses ":" as prefix for procedure arguments.
     */
    @Override
    public String getProcedureOptionList() {
        return " DISABLE DEBUG MODE ";
    }

    /**
     * INTERNAL:
     * This method returns the query to select the timestamp from the server for
     * DB2.
     */
    @Override
    public ValueReadQuery getTimestampQuery() {
        if (timestampQuery == null) {
            if (getUseNationalCharacterVaryingTypeForString()) {
                timestampQuery = new ValueReadQuery();
                timestampQuery.setSQLString("SELECT CAST (CURRENT TIMESTAMP AS TIMESTAMP CCSID UNICODE) FROM SYSIBM.SYSDUMMY1");
                timestampQuery.setAllowNativeSQLQuery(true);
            } else {
                timestampQuery = super.getTimestampQuery();
            }
        }
        return timestampQuery;
    }

    @Override
    public boolean isDB2Z() {
        return true;
    }

    /**
     * INTERNAL:
     * Initialize any platform-specific operators
     */
    @Override
    protected void initializePlatformOperators() {
        super.initializePlatformOperators();
        addOperator(avgOperator());
        addOperator(sumOperator());

        addOperator(absOperator());
        addOperator(sqrtOperator());

        addOperator(trimOperator());
        addOperator(ltrimOperator());
        addOperator(rtrimOperator());
        addOperator(locateOperator());
        addOperator(locate2Operator());

        addOperator(equalOperator());
        addOperator(notEqualOperator());
        addOperator(lessThanOperator());
        addOperator(lessThanEqualOperator());
        addOperator(greaterThanOperator());
        addOperator(greaterThanEqualOperator());
        addOperator(isNullOperator());
        addOperator(isNotNullOperator());
        addOperator(modOperator());

        addOperator(betweenOperator());
        addOperator(notBetweenOperator());
        addOperator(inOperator());

        addOperator(likeEscapeOperator());
        addOperator(notLikeEscapeOperator());
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator avgOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.average().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator sumOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.sum().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator absOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.abs().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    @Override
    protected ExpressionOperator concatOperator() {
        ExpressionOperator operatorS = super.concatOperator();
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        operatorS.copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator equalOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.equal().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator notEqualOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.notEqual().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator greaterThanOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.greaterThan().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator greaterThanEqualOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.greaterThanEqual().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * For ALL, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator lessThanOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.lessThan().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator lessThanEqualOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.lessThanEqual().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator isNullOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.isNull().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator isNotNullOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.notNull().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator betweenOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.between().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator notBetweenOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.notBetween().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS support for binding the LIKE ESCAPE character depends on database configuration (mixed vs DBCS).
     * Since we cannot know how the database in configured, we will disable parameter binding for the ESCAPE
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? > ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator likeEscapeOperator() {
        ExpressionOperator operator = new ExpressionOperator(){
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                for (int i = 0; i < items.size(); i++) {
                    // Disable the first item, which should be <operand2> for this operator
                    if(i == (items.size() - 1)) {
                        Expression item = items.get(i);

                        if(item.isParameterExpression()) {
                            ((ParameterExpression) item).setCanBind(false);
                        } else if(item.isConstantExpression()) {
                            ((ConstantExpression) item).setCanBind(false);
                        }
                    }
                }
                super.printCollection(items, printer);
            }

            @Override
            public void printJavaCollection(Vector<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                for (int i = 0; i < items.size(); i++) {
                    // Disable the last item, which should be <escape> for this operator
                    if(i == (items.size() - 1)) {
                        Expression item = items.get(i);
                        if(item.isParameterExpression()) {
                            ((ParameterExpression) item).setCanBind(false);
                        } else if(item.isConstantExpression()) {
                            ((ConstantExpression) item).setCanBind(false);
                        }
                    }
                }
                super.printJavaCollection(items, printer);
            }
        };

        ExpressionOperator.likeEscape().copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS support for binding the LIKE ESCAPE character depends on database configuration (mixed vs DBCS).
     * Since we cannot know how the database in configured, we will disable parameter binding for the ESCAPE
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? > ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator notLikeEscapeOperator() {
    	ExpressionOperator operator = new ExpressionOperator(){
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                for (int i = 0; i < items.size(); i++) {
                    // Disable the first item, which should be <operand2> for this operator
                    if(i == (items.size() - 1)) {
                        Expression item = items.get(i);

                        if(item.isParameterExpression()) {
                            ((ParameterExpression) item).setCanBind(false);
                        } else if(item.isConstantExpression()) {
                            ((ConstantExpression) item).setCanBind(false);
                        }
                    }
                }
                super.printCollection(items, printer);
            }

            @Override
            public void printJavaCollection(Vector<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                for (int i = 0; i < items.size(); i++) {
                    // Disable the last item, which should be <escape> for this operator
                    if(i == (items.size() - 1)) {
                        Expression item = items.get(i);
                        if(item.isParameterExpression()) {
                            ((ParameterExpression) item).setCanBind(false);
                        } else if(item.isConstantExpression()) {
                            ((ConstantExpression) item).setCanBind(false);
                        }
                    }
                }
                super.printJavaCollection(items, printer);
            }
        };

        ExpressionOperator.notLikeEscape().copyTo(operator);
        return operator;
    }



    /**
     * Disable binding support.
     * <p>
     * With all binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With some binding enabled, DB2 z/OS will throw an error:
     * <pre>The data type, the length, or the value of an argument of a scalar function 
     * is incorrect. DB2 SQL Error: SQLCODE=-171, SQLSTATE=42815</pre>
     */
    protected ExpressionOperator locateOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.locate().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With all binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With some binding enabled, DB2 z/OS will throw an error:
     * <pre>The data type, the length, or the value of an argument of a scalar function 
     * is incorrect. DB2 SQL Error: SQLCODE=-171, SQLSTATE=42815</pre>
     */
    protected ExpressionOperator locate2Operator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.locate2().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With all binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With some binding enabled, DB2 z/OS will throw an error:
     * <pre>The data type, the length, or the value of an argument of a scalar function 
     * is incorrect. DB2 SQL Error: SQLCODE=-171, SQLSTATE=42815</pre>
     */
    protected ExpressionOperator modOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.mod().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator sqrtOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.sqrt().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator trimOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.trim().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The data type, the length, or the value of an argument of a scalar function 
     * is incorrect. DB2 SQL Error: SQLCODE=-171, SQLSTATE=42815</pre>
     */
    @Override
    protected ExpressionOperator trim2() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.trim2().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator ltrimOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.leftTrim().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The data type, the length, or the value of an argument of a scalar function 
     * is incorrect. DB2 SQL Error: SQLCODE=-171, SQLSTATE=42815</pre>
     */
    @Override
    protected ExpressionOperator ltrim2Operator() {
        ExpressionOperator operatorS = super.ltrim2Operator();
        ExpressionOperator operator = disableAllBindingExpression();
        operatorS.copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator rtrimOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.rightTrim().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The data type, the length, or the value of an argument of a scalar function 
     * is incorrect. DB2 SQL Error: SQLCODE=-171, SQLSTATE=42815</pre>
     */
    @Override
    protected ExpressionOperator rtrim2Operator() {
        ExpressionOperator operatorS = super.rtrim2Operator();
        ExpressionOperator operator = disableAllBindingExpression();
        operatorS.copyTo(operator);
        return operator;
    }

    /**
     * DB2 z/OS requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operator for example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     */
    protected ExpressionOperator inOperator() {
        ExpressionOperator operator = new ExpressionOperator() {
            @Override
            public void printDuo(Expression first, Expression second, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printDuo(first, second, printer);
                    return;
                }

                // If the first argument isn't a Constant/Parameter, this will suffice
                if(!first.isValueExpression() || (first.isConstantExpression() && !printer.getPlatform().shouldBindLiterals())) {
                    super.printDuo(first, second, printer);
                    return;
                }

                // Otherwise, we need to inspect the right, collection side of the IN expression
                boolean firstBound = true;
                if(second instanceof CollectionExpression) {
                    Object val = ((CollectionExpression) second).getValue();
                    if (val instanceof Collection) {
                        firstBound = false;
                        Collection values = (Collection)val;
                        for(Object value : values) {
                            // If the value isn't a Constant/Parameter, this will suffice and the first should bind
                            if(value instanceof Expression && !((Expression)value).isValueExpression()) {
                                firstBound = true;
                                break;
                            }

                            // If the value is a Constant and literal binding is disabled, this will suffice and the first should bind
                            if(value instanceof Expression && ((Expression)value).isConstantExpression() && !printer.getPlatform().shouldBindLiterals()) {
                                firstBound = true;
                                break;
                            }
                        }
                    }
                }

                if(first.isParameterExpression()) {
                    ((ParameterExpression) first).setCanBind(firstBound);
                } else if(first.isConstantExpression()) {
                    ((ConstantExpression) first).setCanBind(firstBound);
                }

                super.printDuo(first, second, printer);
            }

            @Override
            public void printJavaDuo(Expression first, Expression second, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaDuo(first, second, printer);
                    return;
                }

                // If the first argument isn't a Constant/Parameter, this will suffice
                if(!first.isValueExpression() || (first.isConstantExpression() && !printer.getPlatform().shouldBindLiterals())) {
                    super.printJavaDuo(first, second, printer);
                    return;
                }

                // Otherwise, we need to inspect the right, collection side of the IN expression
                boolean firstBound = true;
                if(second instanceof CollectionExpression) {
                    Object val = ((CollectionExpression) second).getValue();
                    if (val instanceof Collection) {
                        firstBound = false;
                        Collection values = (Collection)val;
                        for(Object value : values) {
                            // If the value isn't a Constant/Parameter, this will suffice and the first should bind
                            if(value instanceof Expression && !((Expression)value).isValueExpression()) {
                                firstBound = true;
                                break;
                            }

                            // If the value is a Constant and literal binding is disabled, this will suffice and the first should bind
                            if(value instanceof Expression && ((Expression)value).isConstantExpression() && !printer.getPlatform().shouldBindLiterals()) {
                                firstBound = true;
                                break;
                            }
                        }
                    }
                }

                if(first.isParameterExpression()) {
                    ((ParameterExpression) first).setCanBind(firstBound);
                } else if(first.isConstantExpression()) {
                    ((ConstantExpression) first).setCanBind(firstBound);
                }

                super.printJavaDuo(first, second, printer);
            }
        };
        ExpressionOperator.in().copyTo(operator);
        return operator;
    }

    /**
    * INTERNAL: Used for sp calls.  PostGreSQL uses a different method for executing StoredProcedures than other platforms.
    */
    @Override
    public String buildProcedureCallString(StoredProcedureCall call, AbstractSession session, AbstractRecord row) {
        StringWriter writer = new StringWriter();
        writer.write(call.getCallHeader(this));
        writer.write(call.getProcedureName());
        if (requiresProcedureCallBrackets()) {
            writer.write("(");
        } else {
            writer.write(" ");
        }

        int indexFirst = call.getFirstParameterIndexForCallString();
        int size = call.getParameters().size();
        for (int index = indexFirst; index < size; index++) {
            String name = call.getProcedureArgumentNames().get(index);
            Object parameter = call.getParameters().get(index);
            ParameterType parameterType = call.getParameterTypes().get(index);
            // If the argument is optional and null, ignore it.
            if (!call.hasOptionalArguments() || !call.getOptionalArguments().contains(parameter) || (row.get(parameter) != null)) {

                writer.write(getProcedureArgument(name, parameter, parameterType, call, session));

                if (DatasourceCall.isOutputParameterType(parameterType)) {
                    if (requiresProcedureCallOuputToken()) {
                        writer.write(" ");
                        writer.write(getOutputProcedureToken());
                    }
                }
                if ((index + 1) < call.getParameters().size()) {
                    writer.write(", ");
                }
            }
        }

        if (requiresProcedureCallBrackets()) {
            writer.write(")");
        }
        writer.write(getProcedureCallTail());

        return writer.toString();
    }

    /**
     * This method is used to register output parameter on Callable Statements for Stored Procedures
     * as each database seems to have a different method.
     */
    @Override
    public void registerOutputParameter(CallableStatement statement, String name, int jdbcType) throws SQLException {
        try {
            Class clazz = null;
            Method method = null;
            String methodName = "registerJccOutParameterAtName";
            Class[] methodArgs = new Class[] {String.class, int.class};
            Object[] parameters = new Object[] {name, jdbcType};
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                try {
                    ClassLoader cl = AccessController.doPrivileged(new PrivilegedGetContextClassLoader(Thread.currentThread()));
                    clazz = AccessController.doPrivileged(new PrivilegedClassForName(DB2_CALLABLESTATEMENT_CLASS, true, cl));
                    method = AccessController.doPrivileged(new PrivilegedGetMethod(clazz, methodName, methodArgs, true));
                    Object o = statement.unwrap(clazz);
                    AccessController.doPrivileged(new PrivilegedMethodInvoker(method, o, parameters));
                } catch (PrivilegedActionException ex) {
                    if (ex.getCause() instanceof ClassNotFoundException) {
                        throw (ClassNotFoundException) ex.getCause();
                    }
                    throw (RuntimeException) ex.getCause();
                }
            } else {
                ClassLoader cl = PrivilegedAccessHelper.getContextClassLoader(Thread.currentThread());
                clazz = PrivilegedAccessHelper.getClassForName(DB2_CALLABLESTATEMENT_CLASS, true, cl);
                method = PrivilegedAccessHelper.getMethod(clazz, methodName, methodArgs, true);
                Object o = statement.unwrap(clazz);
                PrivilegedAccessHelper.invokeMethod(method, o, parameters);
            }
        } catch (ReflectiveOperationException e) {
            AbstractSessionLog.getLog().logThrowable(SessionLog.WARNING, null, e);
            //Didn't work, fall back. This most likely still won't work, but the driver exception from there will be helpful.
            super.registerOutputParameter(statement, name, jdbcType);
        }
    }

    /**
     * This method is used to register output parameter on Callable Statements for Stored Procedures
     * as each database seems to have a different method.
     */
    @Override
    public void registerOutputParameter(CallableStatement statement, String name, int jdbcType, String typeName) throws SQLException {
        try {
            Class clazz = null;
            Method method = null;
            String methodName = "registerJccOutParameterAtName";
            Class[] methodArgs = new Class[] {String.class, int.class, String.class};
            Object[] parameters = new Object[] {name, jdbcType, typeName};
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                try {
                    ClassLoader cl = AccessController.doPrivileged(new PrivilegedGetContextClassLoader(Thread.currentThread()));
                    clazz = AccessController.doPrivileged(new PrivilegedClassForName(DB2_CALLABLESTATEMENT_CLASS, true, cl));
                    method = AccessController.doPrivileged(new PrivilegedGetMethod(clazz, methodName, methodArgs, true));
                    Object o = statement.unwrap(clazz);
                    AccessController.doPrivileged(new PrivilegedMethodInvoker(method, o, parameters));
                } catch (PrivilegedActionException ex) {
                    if (ex.getCause() instanceof ClassNotFoundException) {
                        throw (ClassNotFoundException) ex.getCause();
                    }
                    throw (RuntimeException) ex.getCause();
                }
            } else {
                ClassLoader cl = PrivilegedAccessHelper.getContextClassLoader(Thread.currentThread());
                clazz = PrivilegedAccessHelper.getClassForName(DB2_CALLABLESTATEMENT_CLASS, true, cl);
                method = PrivilegedAccessHelper.getMethod(clazz, methodName, methodArgs, true);
                Object o = statement.unwrap(clazz);
                PrivilegedAccessHelper.invokeMethod(method, o, parameters);
            }
        } catch (ReflectiveOperationException e) {
            AbstractSessionLog.getLog().logThrowable(SessionLog.WARNING, null, e);
            //Didn't work, fall back. This most likely still won't work, but the driver exception from there will be helpful.
            super.registerOutputParameter(statement, name, jdbcType, typeName);
        }
    }

    @Override
    public void setParameterValueInDatabaseCall(Object parameter,
            CallableStatement statement, String name, AbstractSession session)
            throws SQLException {

        String methodName = null;
        Class[] methodArgs = null;
        Object[] parameters = null;
     // Process common types first.
        if (parameter instanceof String) {
            // Check for stream binding of large strings.
            if (usesStringBinding() && (((String)parameter).length() > getStringBindingSize())) {
                CharArrayReader reader = new CharArrayReader(((String)parameter).toCharArray());
                methodName = "setJccCharacterStreamAtName";
                methodArgs = new Class[] {String.class, java.io.Reader.class, int.class};
                parameters = new Object[] {name, reader, ((String)parameter).length()};
            } else {
                //TODO find shouldUseGetSetNString() support for DB2/Z
                methodName = "setJccStringAtName";
                methodArgs = new Class[] {String.class, String.class};
                parameters = new Object[] {name, (String) parameter};
            }
        } else if (parameter instanceof Number) {
            Number number = (Number) parameter;
            if (number instanceof Integer) {
                methodName = "setJccIntAtName";
                methodArgs = new Class[] {String.class, int.class};
                parameters = new Object[] {name, number.intValue()};
            } else if (number instanceof Long) {
                methodName = "setJccLongAtName";
                methodArgs = new Class[] {String.class, long.class};
                parameters = new Object[] {name, number.longValue()};
            }  else if (number instanceof BigDecimal) {
                methodName = "setJccBigDecimalAtName";
                methodArgs = new Class[] {String.class, BigDecimal.class};
                parameters = new Object[] {name, (BigDecimal) number};
            } else if (number instanceof Double) {
                methodName = "setJccDoubleAtName";
                methodArgs = new Class[] {String.class, double.class};
                parameters = new Object[] {name, number.doubleValue()};
            } else if (number instanceof Float) {
                methodName = "setJccFloatAtName";
                methodArgs = new Class[] {String.class, float.class};
                parameters = new Object[] {name, number.floatValue()};
            } else if (number instanceof Short) {
                methodName = "setJccShortAtName";
                methodArgs = new Class[] {String.class, short.class};
                parameters = new Object[] {name, number.shortValue()};
            } else if (number instanceof Byte) {
                methodName = "setJccByteAtName";
                methodArgs = new Class[] {String.class, byte.class};
                parameters = new Object[] {name, number.byteValue()};
            } else if (number instanceof BigInteger) {
                // Convert to BigDecimal.
                methodName = "setJccBigDecimalAtName";
                methodArgs = new Class[] {String.class, BigDecimal.class};
                parameters = new Object[] {name, new BigDecimal((BigInteger) number)};
            } else {
                methodName = "setJccObjectAtName";
                methodArgs = new Class[] {String.class, Object.class};
                parameters = new Object[] {name, parameter};
            }
        } else if (parameter instanceof java.sql.Date){
            methodName = "setJccDateAtName";
            methodArgs = new Class[] {String.class, java.sql.Date.class};
            parameters = new Object[] {name, (java.sql.Date)parameter};
        } else if (parameter instanceof java.time.LocalDate){
            // Convert to java.sql.Date
            methodName = "setJccDateAtName";
            methodArgs = new Class[] {String.class, java.sql.Date.class};
            parameters = new Object[] {name, java.sql.Date.valueOf((java.time.LocalDate) parameter)};
        } else if (parameter instanceof java.sql.Timestamp){
            methodName = "setJccTimestampAtName";
            methodArgs = new Class[] {String.class, java.sql.Timestamp.class};
            parameters = new Object[] {name, (java.sql.Timestamp)parameter};
        } else if (parameter instanceof java.time.LocalDateTime){
            // Convert to java.sql.Timestamp
            methodName = "setJccTimestampAtName";
            methodArgs = new Class[] {String.class, java.sql.Timestamp.class};
            parameters = new Object[] {name, java.sql.Timestamp.valueOf((java.time.LocalDateTime) parameter)};
        } else if (parameter instanceof java.time.OffsetDateTime) {
            // Convert to java.sql.Timestamp
            methodName = "setJccTimestampAtName";
            methodArgs = new Class[] {String.class, java.sql.Timestamp.class};
            parameters = new Object[] {name, java.sql.Timestamp.from(((java.time.OffsetDateTime) parameter).toInstant())};
        } else if (parameter instanceof java.sql.Time){
            methodName = "setJccTimeAtName";
            methodArgs = new Class[] {String.class, java.sql.Time.class};
            parameters = new Object[] {name, (java.sql.Time)parameter};
        } else if (parameter instanceof java.time.LocalTime){
            java.time.LocalTime lt = (java.time.LocalTime) parameter;
            java.sql.Timestamp ts = java.sql.Timestamp.valueOf(java.time.LocalDateTime.of(java.time.LocalDate.ofEpochDay(0), lt));
            methodName = "setJccTimestampAtName";
            methodArgs = new Class[] {String.class, java.sql.Timestamp.class};
            parameters = new Object[] {name, ts};
        } else if (parameter instanceof java.time.OffsetTime) {
            java.time.OffsetTime ot = (java.time.OffsetTime) parameter;
            java.sql.Timestamp ts = java.sql.Timestamp.valueOf(java.time.LocalDateTime.of(java.time.LocalDate.ofEpochDay(0), ot.toLocalTime()));
            methodName = "setJccTimestampAtName";
            methodArgs = new Class[] {String.class, java.sql.Timestamp.class};
            parameters = new Object[] {name, ts};
        } else if (parameter instanceof Boolean) {
            methodName = "setJccBooleanAtName";
            methodArgs = new Class[] {String.class, boolean.class};
            parameters = new Object[] {name, ((Boolean) parameter).booleanValue()};
        } else if (parameter == null) {
            // Normally null is passed as a DatabaseField so the type is included, but in some case may be passed directly.
            methodName = "setJccNullAtName";
            methodArgs = new Class[] {String.class, int.class};
            parameters = new Object[] {name, getJDBCType((Class)null)};
        } else if (parameter instanceof DatabaseField) {
            setNullFromDatabaseField((DatabaseField)parameter, statement, name);
        } else if (parameter instanceof byte[]) {
            if (usesStreamsForBinding()) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream((byte[])parameter);
                methodName = "setJccBinaryStreamAtName";
                methodArgs = new Class[] {String.class, java.io.InputStream.class, int.class};
                parameters = new Object[] {name, inputStream, ((byte[])parameter).length};
            } else {
                methodName = "setJccBytesAtName";
                methodArgs = new Class[] {String.class, byte[].class};
                parameters = new Object[] {name, (byte[])parameter};
            }
        }
        // Next process types that need conversion.
        else if (parameter instanceof Calendar) {
            methodName = "setJccTimestampAtName";
            methodArgs = new Class[] {String.class, java.sql.Timestamp.class};
            parameters = new Object[] {name, Helper.timestampFromDate(((Calendar)parameter).getTime())};
        } else if (parameter.getClass() == ClassConstants.UTILDATE) {
            methodName = "setJccTimestampAtName";
            methodArgs = new Class[] {String.class, java.sql.Timestamp.class};
            parameters = new Object[] {name, Helper.timestampFromDate((java.util.Date) parameter)};
        } else if (parameter instanceof Character) {
            methodName = "setJccStringAtName";
            methodArgs = new Class[] {String.class, String.class};
            parameters = new Object[] {name, ((Character)parameter).toString()};
        } else if (parameter instanceof char[]) {
            methodName = "setJccStringAtName";
            methodArgs = new Class[] {String.class, String.class};
            parameters = new Object[] {name, new String((char[])parameter)};
        } else if (parameter instanceof Character[]) {
            methodName = "setJccStringAtName";
            methodArgs = new Class[] {String.class, String.class};
            parameters = new Object[] {name, (String)convertObject(parameter, ClassConstants.STRING)};
        } else if (parameter instanceof Byte[]) {
            methodName = "setJccBytesAtName";
            methodArgs = new Class[] {String.class, byte[].class};
            parameters = new Object[] {name, (byte[])convertObject(parameter, ClassConstants.APBYTE)};
        } else if (parameter instanceof SQLXML) {
            methodName = "setJccSQLXMLAtName";
            methodArgs = new Class[] {String.class, java.sql.SQLXML.class};
            parameters = new Object[] {name, (SQLXML) parameter};
        } else if (parameter instanceof BindCallCustomParameter) {
            ((BindCallCustomParameter)(parameter)).set(this, statement, name, session);
        } else if (typeConverters != null && typeConverters.containsKey(parameter.getClass())){
            StructConverter converter = typeConverters.get(parameter.getClass());
            parameter = converter.convertToStruct(parameter, getConnection(session, statement.getConnection()));
            methodName = "setJccObjectAtName";
            methodArgs = new Class[] {String.class, Object.class};
            parameters = new Object[] {name, parameter};
        } else {
            methodName = "setJccObjectAtName";
            methodArgs = new Class[] {String.class, Object.class};
            parameters = new Object[] {name, parameter};
        }

        if(methodName != null) {
            try {
                Class clazz = null;
                Method method = null;
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                    try {
                        ClassLoader cl = AccessController.doPrivileged(new PrivilegedGetContextClassLoader(Thread.currentThread()));
                        clazz = AccessController.doPrivileged(new PrivilegedClassForName(DB2_PREPAREDSTATEMENT_CLASS, true, cl));
                        method = AccessController.doPrivileged(new PrivilegedGetMethod(clazz, methodName, methodArgs, true));
                        Object o = statement.unwrap(clazz);
                        AccessController.doPrivileged(new PrivilegedMethodInvoker(method, o, parameters));
                    } catch (PrivilegedActionException ex) {
                        if (ex.getCause() instanceof ClassNotFoundException) {
                            throw (ClassNotFoundException) ex.getCause();
                        }
                        throw (RuntimeException) ex.getCause();
                    }
                } else {
                    ClassLoader cl = PrivilegedAccessHelper.getContextClassLoader(Thread.currentThread());
                    clazz = PrivilegedAccessHelper.getClassForName(DB2_PREPAREDSTATEMENT_CLASS, true, cl);
                    method = PrivilegedAccessHelper.getMethod(clazz, methodName, methodArgs, true);
                    Object o = statement.unwrap(clazz);
                    PrivilegedAccessHelper.invokeMethod(method, o, parameters);
                }
            } catch (ReflectiveOperationException e) {
                AbstractSessionLog.getLog().logThrowable(SessionLog.WARNING, null, e);
                //Didn't work, fall back. This most likely still won't work, but the driver exception from there will be helpful.
                super.setParameterValueInDatabaseCall(parameter, statement, name, session);
            }
        }
    }

    @Override
    protected void setNullFromDatabaseField(DatabaseField databaseField, CallableStatement statement, String name) throws SQLException {
        String methodName = null;
        Class[] methodArgs = null;
        Object[] parameters = null;
        if (databaseField instanceof ObjectRelationalDatabaseField) {
            ObjectRelationalDatabaseField field = (ObjectRelationalDatabaseField)databaseField;
            methodName = "setJccNullAtName";
            methodArgs = new Class[] {String.class, int.class, String.class};
            parameters = new Object[] {name, field.getSqlType(), field.getSqlTypeName()};
        } else {
            int jdbcType = getJDBCTypeForSetNull(databaseField);
            methodName = "setJccNullAtName";
            methodArgs = new Class[] {String.class, int.class};
            parameters = new Object[] {name, jdbcType};
        }

        try {
            Class clazz = null;
            Method method = null;
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                try {
                    ClassLoader cl = AccessController.doPrivileged(new PrivilegedGetContextClassLoader(Thread.currentThread()));
                    clazz = AccessController.doPrivileged(new PrivilegedClassForName(DB2_PREPAREDSTATEMENT_CLASS, true, cl));
                    method = AccessController.doPrivileged(new PrivilegedGetMethod(clazz, methodName, methodArgs, true));
                    Object o = statement.unwrap(clazz);
                    AccessController.doPrivileged(new PrivilegedMethodInvoker(method, o, parameters));
                } catch (PrivilegedActionException ex) {
                    if (ex.getCause() instanceof ClassNotFoundException) {
                        throw (ClassNotFoundException) ex.getCause();
                    }
                    throw (RuntimeException) ex.getCause();
                }
            } else {
                ClassLoader cl = PrivilegedAccessHelper.getContextClassLoader(Thread.currentThread());
                clazz = PrivilegedAccessHelper.getClassForName(DB2_PREPAREDSTATEMENT_CLASS, true, cl);
                method = PrivilegedAccessHelper.getMethod(clazz, methodName, methodArgs, true);
                Object o = statement.unwrap(clazz);
                PrivilegedAccessHelper.invokeMethod(method, o, parameters);
            }
        } catch (ReflectiveOperationException e) {
            AbstractSessionLog.getLog().logThrowable(SessionLog.WARNING, null, e);
            //Didn't work, fall back. This most likely still won't work, but the driver exception from there will be helpful.
            super.setNullFromDatabaseField(databaseField, statement, name);
        }
    }

    @Override
    public Object getParameterValueFromDatabaseCall(CallableStatement statement, String name, AbstractSession session)
                throws SQLException {
        String methodName = null;
        Class[] methodArgs = null;
        Object[] parameters = null;

        methodName = "getJccObjectAtName";
        methodArgs = new Class[] {String.class};
        parameters = new Object[] {name};

        if(methodName != null) {
            try {
                Class clazz = null;
                Method method = null;
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                    try {
                        ClassLoader cl = AccessController.doPrivileged(new PrivilegedGetContextClassLoader(Thread.currentThread()));
                        clazz = AccessController.doPrivileged(new PrivilegedClassForName(DB2_CALLABLESTATEMENT_CLASS, true, cl));
                        method = AccessController.doPrivileged(new PrivilegedGetMethod(clazz, methodName, methodArgs, true));
                        Object o = statement.unwrap(clazz);
                        return AccessController.doPrivileged(new PrivilegedMethodInvoker(method, o, parameters));
                    } catch (PrivilegedActionException ex) {
                        if (ex.getCause() instanceof ClassNotFoundException) {
                            throw (ClassNotFoundException) ex.getCause();
                        }
                        throw (RuntimeException) ex.getCause();
                    }
                } else {
                    ClassLoader cl = PrivilegedAccessHelper.getContextClassLoader(Thread.currentThread());
                    clazz = PrivilegedAccessHelper.getClassForName(DB2_CALLABLESTATEMENT_CLASS, true, cl);
                    method = PrivilegedAccessHelper.getMethod(clazz, methodName, methodArgs, true);
                    Object o = statement.unwrap(clazz);
                    return PrivilegedAccessHelper.invokeMethod(method, o, parameters);
                }
            } catch (ReflectiveOperationException e) {
                AbstractSessionLog.getLog().logThrowable(SessionLog.WARNING, null, e);
            }
        }
        //Didn't work, fall back. This most likely still won't work, but the driver exception from there will be helpful.
        return super.getParameterValueFromDatabaseCall(statement, name, session);
    }
}
