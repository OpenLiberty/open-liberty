/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation. All rights reserved.
 * Copyright (c) 1998, 2026 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2026 IBM Corporation. All rights reserved.
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
//     Oracle - initial API and implementation from Oracle TopLink
//     Phillip Ross - LIMIT/OFFSET syntax support
//     09/14/2011-2.3.1 Guy Pelletier
//       - 357533: Allow DDL queries to execute even when Multitenant entities are part of the PU
//     02/01/2022: Tomas Kraus
//       - Issue 1442: Implement New Jakarta Persistence 3.1 Features
//     01/07/2023: Maarten Mulders
//       - Issue 1771: Fix UUID handling for PostgreSQL
package org.eclipse.persistence.platform.database;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionOperator;
import org.eclipse.persistence.internal.databaseaccess.DatabaseCall;
import org.eclipse.persistence.internal.expressions.ExpressionJavaPrinter;
import org.eclipse.persistence.internal.expressions.ExpressionSQLPrinter;
import org.eclipse.persistence.internal.expressions.ExtractOperator;
import org.eclipse.persistence.internal.expressions.RelationExpression;
import org.eclipse.persistence.internal.expressions.SQLSelectStatement;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.structures.ObjectRelationalDatabaseField;
import org.eclipse.persistence.queries.Call;
import org.eclipse.persistence.queries.SQLCall;
import org.eclipse.persistence.queries.ValueReadQuery;
import org.eclipse.persistence.tools.schemaframework.FieldDefinition;

/**
 * <p>
 * <b>Purpose</b>: Provides Postgres specific behavior.
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li>Native SQL for Date, Time, {@literal &} Timestamp.</li>
 * <li>SQL time types with with fractional seconds precision support.</li>
 * <li>Native sequencing.</li>
 * <li>Mapping of class types to database types for the schema framework.</li>
 * <li>Pessimistic locking.</li>
 * <li>Platform specific operators.</li>
 * <li>LIMIT/OFFSET query syntax for select statements.</li>
 * </ul>
 *
 * @since OracleAS TopLink 10<i>g</i> (10.1.3)
 */
public class PostgreSQLPlatform extends DatabasePlatform {

    private static final String LIMIT = " LIMIT ";
    private static final String OFFSET = " OFFSET ";

    public PostgreSQLPlatform() {
        super();
        this.cursorCode = 1111; //jdbc.Types.OTHER - PostGreSQL expects this for refCursor types
        this.pingSQL = "SELECT 1";
    }

    /**
     * Return the JDBC type for the Java type. For some reason PostgreSQL does
     * not seem to like the JDBC Blob type (PostgreSQL 8.2).
     */
    @Override
    public int getJDBCType(Class<?> javaType) {
        if (javaType == ClassConstants.BLOB) {
            return Types.LONGVARBINARY;
        }
        return super.getJDBCType(javaType);
    }

    /**
     * Appends a Boolean value. Refer to :
     * http://www.postgresql.org/docs/8.0/static/datatype-boolean.html In
     * PostgreSQL the following are the values that are value for a boolean
     * field Valid literal values for the "true" state are: TRUE, 't', 'true',
     * 'y', 'yes', '1' Valid literal values for the false" state are : FALSE,
     * 'f', 'false', 'n', 'no', '0'
     * <p>
     * To be consistent with the other data platforms we are using the values
     * '1' and '0' for true and false states of a boolean field.
     */
    @Override
    protected void appendBoolean(Boolean bool, Writer writer) throws IOException {
        if (bool) {
            writer.write("\'1\'");
        } else {
            writer.write("\'0\'");
        }
    }

    /**
     * INTERNAL: Use the JDBC maxResults and firstResultIndex setting to compute
     * a value to use when limiting the results of a query in SQL. These limits
     * tend to be used in two ways.
     * <p>
     * 1. MaxRows is the index of the last row to be returned (like JDBC
     * maxResults) 2. MaxRows is the number of rows to be returned
     * <p>
     * PostGreSQL uses case #2 and therefore the maxResults has to be altered
     * based on the firstResultIndex
     *
     * @see MySQLPlatform
     */
    @Override
    public int computeMaxRowsForSQL(int firstResultIndex, int maxResults) {
        return maxResults - ((firstResultIndex >= 0) ? firstResultIndex : 0);
    }

    /**
     * INTERNAL: Initialize any platform-specific operators
     */
    @Override
    protected void initializePlatformOperators() {
        super.initializePlatformOperators();
        addOperator(ExpressionOperator.simpleLogicalNoParens(ExpressionOperator.Concat, "||"));
        addOperator(ExpressionOperator.simpleTwoArgumentFunction(ExpressionOperator.Nvl, "COALESCE"));
        addOperator(operatorLocate());
        addOperator(operatorLocate2());
        addOperator(toNumberOperator());
        addOperator(regexpOperator());
        addOperator(pgsqlRoundOperator());
        addOperator(ExpressionOperator.simpleFunctionNoParentheses(ExpressionOperator.LocalTime, "LOCALTIME"));
        addOperator(ExpressionOperator.simpleFunctionNoParentheses(ExpressionOperator.LocalDateTime, "LOCALTIMESTAMP"));
        addOperator(postgreExtractOperator());
    }

    // Emulate ROUND(:x,:n) as FLOOR((:x)*10^(:n)+0.5)/10^(:n)
    private static ExpressionOperator pgsqlRoundOperator() {
        ExpressionOperator exOperator = new ExpressionOperator() {
            @Override
            public void printDuo(Expression first, Expression second, ExpressionSQLPrinter printer) {
                printer.printString(getDatabaseStrings()[0]);
                first.printSQL(printer);
                printer.printString(getDatabaseStrings()[1]);
                if (second != null) {
                    second.printSQL(printer);
                } else {
                    printer.printString("0");
                }
                printer.printString(getDatabaseStrings()[2]);
                if (second != null) {
                    second.printSQL(printer);
                } else {
                    printer.printString("0");
                }
                printer.printString(getDatabaseStrings()[3]);
            }
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if (printer.getPlatform().isDynamicSQLRequiredForFunctions() && !isBindingSupported()) {
                    printer.getCall().setUsesBinding(false);
                }
                if (!items.isEmpty()) {
                    Expression firstItem = items.get(0);
                    Expression secondItem = items.size() > 1 ? items.get(1) : null;
                    printDuo(firstItem, secondItem, printer);
                } else {
                    throw new IllegalArgumentException("List of items shall contain at least one item");
                }
            }
            @Override
            public void printJavaDuo(Expression first, Expression second, ExpressionJavaPrinter printer) {
                printer.printString(getDatabaseStrings()[0]);
                first.printJava(printer);
                printer.printString(getDatabaseStrings()[1]);
                if (second != null) {
                    second.printJava(printer);
                } else {
                    printer.printString("0");
                }
                printer.printString(getDatabaseStrings()[2]);
                if (second != null) {
                    second.printJava(printer);
                } else {
                    printer.printString("0");
                }
                printer.printString(getDatabaseStrings()[3]);
            }
            @Override
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if (!items.isEmpty()) {
                    Expression firstItem = items.get(0);
                    Expression secondItem = items.size() > 1 ? items.get(1) : null;
                    printJavaDuo(firstItem, secondItem, printer);
                } else {
                    throw new IllegalArgumentException("List of items shall contain at least one item");
                }
            }
        };
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.Round);
        exOperator.setName("ROUND");
        List<String> v = new ArrayList<>(4);
        v.add("FLOOR((");
        v.add(")*10^(");
        v.add(")+0.5)/10^(");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the ~ operator.
     * REGEXP allows for comparison through regular expression.
     */
    public static ExpressionOperator regexpOperator() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(ExpressionOperator.Regexp);
        result.setType(ExpressionOperator.FunctionOperator);
        List<String> v = new ArrayList<>(3);
        v.add("");
        v.add(" ~ ");
        v.add("");
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        v = new ArrayList<>(2);
        v.add(".regexp(");
        v.add(")");
        result.printsJavaAs(v);
        return result;
    }

    /**
     * INTERNAL: Postgres to_number has two arguments, as fix format argument.
     */
    protected ExpressionOperator toNumberOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.ToNumber);
        List<String> v = new ArrayList<>(2);
        v.add("TO_NUMBER(");
        v.add(", '999999999.9999')");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    // PostgreSQL EXTRACT operator needs emulation of DATE and TIME date/time parts.
    private static final class PostgreSQLExtractOperator extends ExtractOperator {

        // DATE emulation: DATE(:first)
        private static final String[] DATE_STRINGS = new String[] {"DATE(", ")"};
        // TIME emulation: :first::TIME
        private static final String[] TIME_STRINGS = new String[] {"::TIME"};

        private PostgreSQLExtractOperator() {
            super();
        }

        @Override
        protected void printDateSQL(final Expression first, Expression second, final ExpressionSQLPrinter printer) {
            printer.printString(DATE_STRINGS[0]);
            first.printSQL(printer);
            printer.printString(DATE_STRINGS[1]);
        }

        @Override
        protected void printDateJava(final Expression first, Expression second, final ExpressionJavaPrinter printer) {
            printer.printString(DATE_STRINGS[0]);
            first.printJava(printer);
            printer.printString(DATE_STRINGS[1]);
        }

        @Override
        protected void printTimeSQL(final Expression first, Expression second, final ExpressionSQLPrinter printer) {
            first.printSQL(printer);
            printer.printString(TIME_STRINGS[0]);
        }

        @Override
        protected void printTimeJava(final Expression first, Expression second, final ExpressionJavaPrinter printer) {
            first.printJava(printer);
            printer.printString(TIME_STRINGS[0]);
        }
    }

    // Create EXTRACT operator form Derby platform
    private static ExpressionOperator postgreExtractOperator() {
        return new PostgreSQLPlatform.PostgreSQLExtractOperator();
    }

    /**
     * INTERNAL: This method returns the query to select the timestamp from the
     * server for Postgres.
     */
    @Override
    public ValueReadQuery getTimestampQuery() {
        if (this.timestampQuery == null) {
            this.timestampQuery = new ValueReadQuery();
            this.timestampQuery.setSQLString("SELECT NOW()");
            this.timestampQuery.setAllowNativeSQLQuery(true);
        }
        return this.timestampQuery;

    }

    /**
     * INTERNAL:
     * Should the variable name of a stored procedure call be printed as part of the procedure call
     * e.g. EXECUTE PROCEDURE MyStoredProc(myvariable = ?)
     * In case of PostgreSQL NO as JDBC driver and DB doesn't support pass parameters by name.
     * It expects, that parameters are passed by index or persistence property {@code <property name="eclipselink.jpa.naming_into_indexed" value="true"/>} is used.
     */
    @Override
    public boolean shouldPrintStoredProcedureArgumentNameInCall() {
        return false;
    }

    /**
     * Calling a stored procedure query on PostgreSQL with no output parameters
     * always returns true from an execute call regardless if a result set is
     * returned or not. This flag will help avoid throwing a JPA mandated
     * exception on an executeUpdate call (which calls jdbc execute and checks
     * the return value to ensure no results sets are returned (true)).
     * PostgreSQL also doesn't support parameters passed by name in case of
     * stored procedure calls.
     * @see PostgreSQLPlatform
     */
    @Override
    public boolean isJDBCExecuteCompliant() {
        return false;
    }

    /**
     * INTERNAL: Answers whether platform is Postgres.
     */
    @Override
    public boolean isPostgreSQL() {
        return true;
    }

    /**
     * INTERNAL:
     */
    @Override
    protected String getCreateTempTableSqlSuffix() {
        return " ON COMMIT DROP";
    }

    /**
     * INTERNAL: Indicates whether locking OF clause should print alias for
     * field. Example: on Oracle platform (method returns false): SELECT
     * ADDRESS_ID, ... FROM ADDRESS T1 WHERE (T1.ADDRESS_ID = ?) FOR UPDATE OF
     * T1.ADDRESS_ID on Postgres platform (method returns true): SELECT
     * ADDRESS_ID, ... FROM ADDRESS T1 WHERE (T1.ADDRESS_ID = ?) FOR UPDATE OF
     * T1
     */
    @Override
    public boolean shouldPrintAliasForUpdate() {
        return true;
    }

    /**
     * INTERNAL: Indicates whether the platform supports identity.
     */
    @Override
    public boolean supportsIdentity() {
        return true;
    }

    /**
     * INTERNAL: Returns query used to read back the value generated by
     * Identity. This method is called when identity NativeSequence is
     * connected, the returned query used until the sequence is disconnected. If
     * the platform supportsIdentity then (at least) one of
     * buildSelectQueryForIdentity methods should return non-null query.
     */
    @Override
    public ValueReadQuery buildSelectQueryForIdentity() {
        ValueReadQuery selectQuery = new ValueReadQuery();
        selectQuery.setSQLString("select lastval()");
        return selectQuery;
    }

    /**
     * INTERNAL: Indicates whether the platform supports sequence objects.
     */
    @Override
    public boolean supportsSequenceObjects() {
        return true;
    }

    /**
     * INTERNAL: Returns query used to read value generated by sequence object
     * (like Oracle sequence). This method is called when sequence object
     * NativeSequence is connected, the returned query used until the sequence
     * is disconnected. If the platform supportsSequenceObjects then (at least)
     * one of buildSelectQueryForSequenceObject methods should return non-null
     * query.
     */
    @Override
    public ValueReadQuery buildSelectQueryForSequenceObject(String qualifiedSeqName, Integer size) {
        return new ValueReadQuery("select nextval(\'" + qualifiedSeqName + "\')");
    }

    /**
     * INTERNAL: Override the default locate operator.
     */
    protected ExpressionOperator operatorLocate() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(ExpressionOperator.Locate);
        List<String> v = new ArrayList<>(3);
        v.add("STRPOS(");
        v.add(", ");
        v.add(")");
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(RelationExpression.class);
        return result;
    }

    /**
     * INTERNAL: Override the default locate operator.
     */
    protected ExpressionOperator operatorLocate2() {
        ExpressionOperator operator = new ExpressionOperator();
        operator.setSelector(ExpressionOperator.Locate2);
        List<String> v = new ArrayList<>(2);
        v.add("COALESCE(NULLIF(STRPOS(SUBSTRING(");
        v.add(" FROM ");
        v.add("), ");
        v.add("), 0) - 1 + ");
        v.add(", 0)");
        operator.printsAs(v);
        operator.bePrefix();
        int[] argumentIndices = new int[4];
        argumentIndices[0] = 0;
        argumentIndices[1] = 2;
        argumentIndices[2] = 1;
        argumentIndices[3] = 2;
        operator.setArgumentIndices(argumentIndices);
        operator.setNodeClass(RelationExpression.class);
        return operator;
    }

    /**
     * INTERNAL:
     */
    @Override
    public boolean supportsLocalTempTables() {
        return true;
    }

    /**
     * INTERNAL:
     */
    @Override
    protected String getCreateTempTableSqlPrefix() {
        return "CREATE LOCAL TEMPORARY TABLE ";
    }

    /**
     * INTERNAL: Used for sp calls.
     */
    @Override
    public String getProcedureCallHeader() {
        return "CALL ";
    }

    /**
     * INTERNAL Used for stored function calls.
     */
    @Override
    public String getAssignmentString() {
        return ":= ";
    }

    /**
     * JDBC defines and outer join syntax, many drivers do not support this. So
     * we normally avoid it.
     */
    @Override
    public boolean shouldUseJDBCOuterJoinSyntax() {
        return false;
    }

    /**
     * Print the pagination SQL using Postgres syntax
     * " LIMIT {@literal <max> OFFSET <first>}".
     */
    @Override
    public void printSQLSelectStatement(DatabaseCall call, ExpressionSQLPrinter printer, SQLSelectStatement statement) {
        int max = 0;
        if (statement.getQuery() != null) {
            max = statement.getQuery().getMaxRows();
        }
        if (max <= 0 || !(this.shouldUseRownumFiltering())) {
            super.printSQLSelectStatement(call, printer, statement);
            return;
        }
        statement.setUseUniqueFieldAliases(true);
        call.setFields(statement.printSQL(printer));
        printer.printString(LIMIT);
        printer.printParameter(DatabaseCall.MAXROW_FIELD);
        printer.printString(OFFSET);
        printer.printParameter(DatabaseCall.FIRSTRESULT_FIELD);
        call.setIgnoreFirstRowSetting(true);
        call.setIgnoreMaxResultsSetting(true);
    }

    /**
     * INTERNAL: May need to override this method if the platform supports
     * temporary tables and the generated sql doesn't work. Write an sql string
     * for updating the original table from the temporary table. Precondition:
     * supportsTempTables() == true. Precondition: pkFields and assignFields
     * don't intersect.
     * @param writer for writing the sql
     * @param table is original table for which temp table is
     *            created.
     * @param pkFields - primary key fields for the original
     *            table.
     * @param assignedFields - fields to be assigned a new value.
     */
    @Override
    public void writeUpdateOriginalFromTempTableSql(Writer writer, DatabaseTable table, Collection<DatabaseField> pkFields, Collection<DatabaseField> assignedFields) throws IOException {
        writer.write("UPDATE ");
        String tableName = table.getQualifiedNameDelimited(this);
        writer.write(tableName);
        writer.write(" SET ");

        String tempTableName = getTempTableForTable(table).getQualifiedNameDelimited(this);
        boolean isFirst = true;
        Iterator<DatabaseField> itFields = assignedFields.iterator();
        while (itFields.hasNext()) {
            if (isFirst) {
                isFirst = false;
            } else {
                writer.write(", ");
            }
            DatabaseField field = itFields.next();
            String fieldName = field.getNameDelimited(this);
            writer.write(fieldName);
            writer.write(" = (SELECT ");
            writer.write(fieldName);
            writer.write(" FROM ");
            writer.write(tempTableName);
            writeAutoJoinWhereClause(writer, null, tableName, pkFields, this);
            writer.write(")");
        }

        writer.write(" WHERE EXISTS(SELECT ");
        writer.write(pkFields.iterator().next().getNameDelimited(this));
        writer.write(" FROM ");
        writer.write(tempTableName);
        writeAutoJoinWhereClause(writer, null, tableName, pkFields, this);
        writer.write(")");
    }

    /**
     * INTERNAL:
     * Postgres has a returning clause.
     */
    @Override
    public boolean canBuildCallWithReturning() {
        return true;
    }

    /**
     * INTERNAL:
     * Uses the returning clause on Postgres.
     */
    @Override
    public DatabaseCall buildCallWithReturning(SQLCall sqlCall, List<DatabaseField> returnFields) {
        SQLCall call = new SQLCall();
        call.setParameters(sqlCall.getParameters());
        call.setParameterTypes(sqlCall.getParameterTypes());
        call.returnOneRow();
        Writer writer = new CharArrayWriter(sqlCall.getSQLString().length() + 32);
        try {
            writer.write(sqlCall.getSQLString());
            writer.write(" RETURNING ");
            for (int i = 0; i < returnFields.size(); i++) {
                DatabaseField field = returnFields.get(i);
                writer.write(field.getNameDelimited(this));
                if ((i + 1) < returnFields.size()) {
                    writer.write(", ");
                }
            }
            call.setQueryString(writer.toString());
        } catch (IOException exception) {
            throw ValidationException.fileError(exception);
        }
        return call;
    }

    @Override
    protected void setNullFromDatabaseField(DatabaseField databaseField, PreparedStatement statement, int index) throws SQLException {
        // Substituted null value for the corresponding DatabaseField.
        // Cannot bind null through set object, so we must compute the type, this is not good.
        // Fix for bug 2730536: for ARRAY/REF/STRUCT types must pass in the
        // user defined type to setNull as well.
        if (databaseField instanceof ObjectRelationalDatabaseField field) {
            //Fix for bug 537657: Inserting empty or null varchar arrays doesn't work with PostgreSQL since driver version 42.2.4
            if (field.getSqlType() == Types.ARRAY) {
                statement.setNull(index, field.getSqlType());
            } else {
                statement.setNull(index, field.getSqlType(), field.getSqlTypeName());
            }
        } else {
            int jdbcType = getJDBCTypeForSetNull(databaseField);
            statement.setNull(index, jdbcType);
        }
    }

    /**
     * INTERNAL
     * Set the parameter in the JDBC statement at the given index in case it is a UUID value.
     *
     * @param parameter the parameter to set
     * @param statement target {@code PreparedStatement} instance
     * @param index index of the parameter in the statement
     * @param session current database session
     */
    @Override
    public void setParameterValueInDatabaseCall(
            final Object parameter, final PreparedStatement statement,
            final int index, final AbstractSession session
    ) throws SQLException {
        if (parameter instanceof UUID) {
            statement.setObject(index, parameter, Types.OTHER);
        } else {
            super.setParameterValueInDatabaseCall(parameter, statement, index, session);
        }
    }

    /**
     * INTERNAL
     * Set the parameter in the JDBC statement at the given index in case it is a UUID value.
     *
     * @param parameter the parameter to set
     * @param statement target {@code CallableStatement} instance
     * @param name name of the parameter in the statement
     * @param session current database session
     */
     @Override
    public void setParameterValueInDatabaseCall(Object parameter,
                CallableStatement statement, String name, AbstractSession session)
                throws SQLException {
        if (parameter instanceof UUID) {
            statement.setObject(name, parameter, Types.OTHER);
        } else {
            super.setParameterValueInDatabaseCall(parameter, statement, name, session);
        }
    }

    @Override
    public ValueReadQuery getUUIDQuery() {
        if (uuidQuery == null) {
            uuidQuery = new ValueReadQuery();
            uuidQuery.setSQLString("SELECT gen_random_uuid()");
            uuidQuery.setAllowNativeSQLQuery(true);
        }
        return uuidQuery;
    }

    @Override
    public int appendParameterInternal(Call call, Writer writer, Object parameter) {
        Object p = parameter;
        if (p instanceof UUID uuid) {
            p = uuid.toString();
        }
        return super.appendParameterInternal(call, writer, p);
    }

    /*
                                 ____  ____  __
                                |    \|    \|  |
                                |  |  |  |  |  |__
                                |____/|____/|_____|
     */

    private static final FieldDefinition.DatabaseType PGSQL_TYPE_BOOLEAN = new FieldDefinition.DatabaseType("BOOLEAN", false);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_BIGINT = new FieldDefinition.DatabaseType("BIGINT", false);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_INTEGER = new FieldDefinition.DatabaseType("INTEGER", false);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_FLOAT = new FieldDefinition.DatabaseType("FLOAT", false);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_SMALLINT = new FieldDefinition.DatabaseType("SMALLINT", false);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_DECIMAL = new FieldDefinition.DatabaseType("DECIMAL", 38);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_CHAR = TYPE_CHAR.ofSize(1);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_VARCHAR = TYPE_VARCHAR.ofSize(DEFAULT_VARCHAR_SIZE);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_DATE = TYPE_DATE.ofNoSize();
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_BYTEA = new FieldDefinition.DatabaseType("BYTEA");
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_BYTEA_FIXED = PGSQL_TYPE_BYTEA.ofNoSize();
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_TEXT = new FieldDefinition.DatabaseType("TEXT", false);
    private static final FieldDefinition.DatabaseType PGSQL_TYPE_UUID = new FieldDefinition.DatabaseType("UUID", false);

    @Override
    protected Map<Class<?>, FieldDefinition.DatabaseType> buildDatabaseTypes() {
        Map<Class<?>, FieldDefinition.DatabaseType> fieldTypeMapping = new HashMap<>();
        fieldTypeMapping.put(Boolean.class, PGSQL_TYPE_BOOLEAN);

        fieldTypeMapping.put(Integer.class, PGSQL_TYPE_INTEGER);
        fieldTypeMapping.put(Long.class, PGSQL_TYPE_BIGINT);
        fieldTypeMapping.put(Float.class, PGSQL_TYPE_FLOAT);
        fieldTypeMapping.put(Double.class, PGSQL_TYPE_FLOAT);
        fieldTypeMapping.put(Short.class, PGSQL_TYPE_SMALLINT);
        fieldTypeMapping.put(Byte.class, PGSQL_TYPE_SMALLINT);
        fieldTypeMapping.put(BigInteger.class, PGSQL_TYPE_BIGINT);
        fieldTypeMapping.put(BigDecimal.class, PGSQL_TYPE_DECIMAL);
        fieldTypeMapping.put(Number.class, PGSQL_TYPE_DECIMAL);

        fieldTypeMapping.put(String.class, PGSQL_TYPE_VARCHAR);
        fieldTypeMapping.put(Character.class, PGSQL_TYPE_CHAR);

        fieldTypeMapping.put(Byte[].class, PGSQL_TYPE_BYTEA_FIXED);
        fieldTypeMapping.put(Character[].class, PGSQL_TYPE_TEXT);
        fieldTypeMapping.put(byte[].class, PGSQL_TYPE_BYTEA_FIXED);
        fieldTypeMapping.put(char[].class, PGSQL_TYPE_TEXT);
        fieldTypeMapping.put(java.sql.Blob.class, PGSQL_TYPE_BYTEA);
        fieldTypeMapping.put(java.sql.Clob.class, PGSQL_TYPE_TEXT);

        fieldTypeMapping.put(java.sql.Date.class, PGSQL_TYPE_DATE);
        fieldTypeMapping.put(java.sql.Time.class, TYPE_TIME);
        fieldTypeMapping.put(java.sql.Timestamp.class, TYPE_TIMESTAMP);

        fieldTypeMapping.put(java.time.LocalDate.class, PGSQL_TYPE_DATE);
        fieldTypeMapping.put(java.time.LocalDateTime.class, TYPE_TIMESTAMP);
        fieldTypeMapping.put(java.time.LocalTime.class, TYPE_TIME);
        fieldTypeMapping.put(java.time.OffsetDateTime.class, TYPE_TIMESTAMP);
        fieldTypeMapping.put(java.time.OffsetTime.class, TYPE_TIME);
        fieldTypeMapping.put(java.time.Instant.class, TYPE_TIMESTAMP);

        fieldTypeMapping.put(java.util.UUID.class, PGSQL_TYPE_UUID);

        return fieldTypeMapping;
    }

    @Override
    public String getDropCascadeString() {
        return " CASCADE";
    }

    @Override
    public String getInOutputProcedureToken() {
        return "OUT";
    }

    @Override
    public int getMaxFieldNameSize() {
        // The system uses no more than NAMEDATALEN-1 characters of an
        // identifier; longer names can be written in commands,
        // but they will be truncated. By default, NAMEDATALEN is 64 so the
        // maximum identifier length is 63 (but at the time PostgreSQL
        // is built, NAMEDATALEN can be changed in src/include/postgres_ext.h).
        // http://www.postgresql.org/docs/7.3/interactive/sql-syntax.html#SQL-SYNTAX-IDENTIFIERS
        return 63;
    }

    // http://www.postgresql.org/docs/8.1/interactive/plpgsql-declarations.html
    @Override
    public String getProcedureBeginString() {
        return "$$  BEGIN ";
    }

    @Override
    public String getProcedureEndString() {
        return "END; $$ LANGUAGE plpgsql;";
    }

    @Override
    public boolean isAlterSequenceObjectSupported() {
        return true;
    }

    @Override
    public void printFieldTypeSize(Writer writer, FieldDefinition field, FieldDefinition.DatabaseType databaseType, boolean shouldPrintFieldIdentityClause) throws IOException {
        if (!shouldPrintFieldIdentityClause) {
            super.printFieldTypeSize(writer, field, databaseType, shouldPrintFieldIdentityClause);
        }
    }

    @Override
    public void printFieldIdentityClause(Writer writer) throws ValidationException {
        try {
            writer.write(" SERIAL");
        } catch (IOException ioException) {
            throw ValidationException.fileError(ioException);
        }
    }

    @Override
    public void printFieldUnique(Writer writer, boolean shouldPrintFieldIdentityClause) throws IOException {
        if (!shouldPrintFieldIdentityClause) {
            super.printFieldUnique(writer, shouldPrintFieldIdentityClause);
        }
    }

    @Override
    public boolean shouldPrintOutputTokenAtStart() {
        // TODO: Check with the reviewer where this is used
        return true;
    }

    @Override
    public boolean supportsFractionalTime() {
        // According to PostgreSQL manuals, support of fractional seconds precision in time SQL types
        // was added in version 7.2. This version is from 4th February 2002. Let's support it from
        // the base level of PostgreSQL platforms.
        // All time, timestamp, and interval accept an optional precision value p which specifies the number
        // of fractional digits retained in the seconds field. By default, there is no explicit bound on precision.
        // The allowed range of p is from 0 to 6 for the timestamp and interval types.
        return true;
    }

}
