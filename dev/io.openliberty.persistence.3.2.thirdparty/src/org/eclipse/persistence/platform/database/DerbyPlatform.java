/*
 * Copyright (c) 2005, 2024 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2005, 2024 IBM Corporation. All rights reserved.
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
//     Sun Microsystems
//     09/14/2011-2.3.1 Guy Pelletier
//       - 357533: Allow DDL queries to execute even when Multitenant entities are part of the PU
//     03/18/2015-2.6.0 Joe Grassel
//       - 462498: Missing isolation level expression in SQL for Derby platform
//     02/01/2022: Tomas Kraus
//       - Issue 1442: Implement New Jakarta Persistence 3.1 Features
package org.eclipse.persistence.platform.database;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionOperator;
import org.eclipse.persistence.internal.databaseaccess.DatabaseCall;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;
import org.eclipse.persistence.internal.expressions.CollectionExpression;
import org.eclipse.persistence.internal.expressions.ConstantExpression;
import org.eclipse.persistence.internal.expressions.ExpressionJavaPrinter;
import org.eclipse.persistence.internal.expressions.ExpressionSQLPrinter;
import org.eclipse.persistence.internal.expressions.ExtractOperator;
import org.eclipse.persistence.internal.expressions.ParameterExpression;
import org.eclipse.persistence.internal.expressions.SQLSelectStatement;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.queries.Call;
import org.eclipse.persistence.queries.ValueReadQuery;

import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * <p><b>Purpose</b>: Provides Derby DBMS specific behavior.
 *
 * @since TOPLink Essentials 1.0
 */
public class DerbyPlatform extends DB2Platform {

    public static final int MAX_CLOB = 2147483647;  //The maximum clob/blob size is 2 gigs in Derby.
    public static final int MAX_BLOB = MAX_CLOB;

    /** Allow sequence support to be disabled for Derby {@literal <} 10.6.1. */
    protected boolean isSequenceSupported = false;

    /** {@literal <} = 10.6.1.0 supports parameters with OFFSET/FETCH - DERBY-4208 */
    boolean isOffsetFetchParameterSupported = false;

    protected boolean isConnectionDataInitialized;

    public DerbyPlatform() {
        super();
    }

    /**
     * INTERNAL:
     * TODO: Need to find out how can byte arrays be inlined in Derby
     */
    @Override
    protected void appendByteArray(byte[] bytes, Writer writer) throws IOException {
            super.appendByteArray(bytes, writer);
    }

    /**
     * Derby error the data type, length or value of arguments 'TIMESTAMP' and 'DATE' is incompatible.
     * Instead, use a java.sql.Date type for property {d } casting
     */
    @Override
    public Object convertToDatabaseType(Object value) {
        if (value != null && value.getClass() == ClassConstants.UTILDATE) {
            return Helper.sqlDateFromUtilDate((java.util.Date)value);
        }
        return super.convertToDatabaseType(value);
    }

    /**
     * INTERNAL:
     * This method returns the query to select the timestamp from the server
     * for Derby.
     */
    @Override
    public ValueReadQuery getTimestampQuery() {
        if (timestampQuery == null) {
            timestampQuery = new ValueReadQuery();
            timestampQuery.setSQLString("VALUES CURRENT_TIMESTAMP");
            timestampQuery.setAllowNativeSQLQuery(true);
        }
        return timestampQuery;

    }

    /**
     * INTERNAL:
     * Not currently used.
     */
    @Override
    public Vector getNativeTableInfo(String table, String creator, AbstractSession session) {
        throw new RuntimeException("Not supported");
    }

    /**
     * Used for stored procedure defs.
     */
    @Override
    public String getProcedureEndString() {
        return getBatchEndString();
    }

    /**
     * Used for stored procedure defs.
     */
    @Override
    public String getProcedureBeginString() {
        return getBatchBeginString();
    }

    /**
     * This method is used to print the output parameter token when stored
     * procedures are called
     */
    @Override
    public String getInOutputProcedureToken() {
        return "INOUT";
    }

    /**
     * This is required in the construction of the stored procedures with
     * output parameters
     */
    @Override
    public boolean shouldPrintOutputTokenAtStart() {
        //TODO: Check with the reviewer where this is used
        return false;
    }

    /**
     * INTERNAL:
     * Answers whether platform is Derby
     */
    @Override
    public boolean isDerby() {
        return true;
    }

    @Override
    public boolean isDB2() {
        //This class inherits from DB2. But it is not DB2
        return false;
    }

    @Override
    public String getSelectForUpdateString() {
        return " FOR UPDATE WITH RS";
    }


    /**
     * Allow for the platform to ignore exceptions.
     */
    @Override
    public boolean shouldIgnoreException(SQLException exception) {
        // Nothing is ignored.
        return false;
    }


    /**
     * INTERNAL:
     */
    @Override
    protected String getCreateTempTableSqlSuffix() {
        return " ON COMMIT DELETE ROWS NOT LOGGED";
    }

    /**
     * INTERNAL:
     * Build the identity query for native sequencing.
     */
    @Override
    public ValueReadQuery buildSelectQueryForIdentity() {
        ValueReadQuery selectQuery = new ValueReadQuery();
        selectQuery.setSQLString("values IDENTITY_VAL_LOCAL()");
        return selectQuery;
    }

    /**
     * INTERNAL:
     * Indicates whether temporary table can specify primary keys (some platforms don't allow that).
     * Used by writeCreateTempTableSql method.
     */
    @Override
    protected boolean shouldTempTableSpecifyPrimaryKeys() {
        return false;
    }

    /**
     * INTERNAL:
     */
    @Override
    protected String getCreateTempTableSqlBodyForTable(DatabaseTable table) {
        // returning null includes fields of the table in body
        // see javadoc of DatabasePlatform#getCreateTempTableSqlBodyForTable(DataBaseTable)
        // for details
        return null;
    }

    /**
     * INTERNAL:
     * May need to override this method if the platform supports temporary tables
     * and the generated sql doesn't work.
     * Write an sql string for updating the original table from the temporary table.
     * Precondition: supportsTempTables() == true.
     * Precondition: pkFields and assignFields don't intersect.
     * @param writer for writing the sql
     * @param table is original table for which temp table is created.
     * @param pkFields - primary key fields for the original table.
     * @param assignedFields - fields to be assigned a new value.
     */
    @Override
    public void writeUpdateOriginalFromTempTableSql(Writer writer, DatabaseTable table,
                                                    Collection<DatabaseField> pkFields,
                                                    Collection<DatabaseField> assignedFields) throws IOException
    {
        writer.write("UPDATE ");
        String tableName = table.getQualifiedNameDelimited(this);
        writer.write(tableName);
        writer.write(" SET ");

        String tempTableName = getTempTableForTable(table).getQualifiedNameDelimited(this);
        boolean isFirst = true;
        Iterator<DatabaseField> itFields = assignedFields.iterator();
        while(itFields.hasNext()) {
            if(isFirst) {
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
     * Append the receiver's field 'identity' constraint clause to a writer.
     */
    @Override
    public void printFieldIdentityClause(Writer writer) throws ValidationException {
        try {
            writer.write(" GENERATED BY DEFAULT AS IDENTITY");
        } catch (IOException ioException) {
            throw ValidationException.fileError(ioException);
        }
    }

    @Override
    protected Hashtable<Class<?>, FieldTypeDefinition> buildFieldTypes() {
        Hashtable<Class<?>, FieldTypeDefinition> fieldTypeMapping = new Hashtable<>();

        fieldTypeMapping.put(Boolean.class, new FieldTypeDefinition("SMALLINT DEFAULT 0", false));

        fieldTypeMapping.put(Integer.class, new FieldTypeDefinition("INTEGER", false));
        fieldTypeMapping.put(Long.class, new FieldTypeDefinition("BIGINT", false));
        fieldTypeMapping.put(Float.class, new FieldTypeDefinition("FLOAT", false));
        fieldTypeMapping.put(Double.class, new FieldTypeDefinition("FLOAT", false));
        fieldTypeMapping.put(Short.class, new FieldTypeDefinition("SMALLINT", false));
        fieldTypeMapping.put(Byte.class, new FieldTypeDefinition("SMALLINT", false));
        fieldTypeMapping.put(java.math.BigInteger.class, new FieldTypeDefinition("BIGINT", false));
        fieldTypeMapping.put(java.math.BigDecimal.class, new FieldTypeDefinition("DECIMAL", 15));
        fieldTypeMapping.put(Number.class, new FieldTypeDefinition("DECIMAL", 15));

        fieldTypeMapping.put(String.class, new FieldTypeDefinition("VARCHAR", DEFAULT_VARCHAR_SIZE));
        fieldTypeMapping.put(Character.class, new FieldTypeDefinition("CHAR", 1));
        fieldTypeMapping.put(Byte[].class, new FieldTypeDefinition("BLOB", MAX_BLOB));
        fieldTypeMapping.put(Character[].class, new FieldTypeDefinition("CLOB", MAX_CLOB));
        fieldTypeMapping.put(byte[].class, new FieldTypeDefinition("BLOB", MAX_BLOB));
        fieldTypeMapping.put(char[].class, new FieldTypeDefinition("CLOB", MAX_CLOB));
        fieldTypeMapping.put(java.sql.Blob.class, new FieldTypeDefinition("BLOB", MAX_BLOB));
        fieldTypeMapping.put(java.sql.Clob.class, new FieldTypeDefinition("CLOB", MAX_CLOB));

        fieldTypeMapping.put(java.sql.Date.class, new FieldTypeDefinition("DATE", false));
        fieldTypeMapping.put(java.sql.Time.class, new FieldTypeDefinition("TIME", false));
        fieldTypeMapping.put(java.sql.Timestamp.class, new FieldTypeDefinition("TIMESTAMP", false));
        fieldTypeMapping.put(java.time.Instant.class, new FieldTypeDefinition("TIMESTAMP", false));

        fieldTypeMapping.put(java.time.LocalDate.class, new FieldTypeDefinition("DATE"));
        fieldTypeMapping.put(java.time.LocalDateTime.class, new FieldTypeDefinition("TIMESTAMP"));
        fieldTypeMapping.put(java.time.LocalTime.class, new FieldTypeDefinition("TIME"));
        fieldTypeMapping.put(java.time.OffsetDateTime.class, new FieldTypeDefinition("TIMESTAMP"));
        fieldTypeMapping.put(java.time.OffsetTime.class, new FieldTypeDefinition("TIMESTAMP"));

        return fieldTypeMapping;
    }


    @Override
    protected void setNullFromDatabaseField(DatabaseField databaseField, PreparedStatement statement, int index) throws SQLException {
        int jdbcType = databaseField.getSqlType();
        if (jdbcType == DatabaseField.NULL_SQL_TYPE) {
            jdbcType = statement.getParameterMetaData().getParameterType(index);
        }

        statement.setNull(index, jdbcType);
    }

    /**
     * Initialize any platform-specific operators
     */
    @Override
    protected void initializePlatformOperators() {
        super.initializePlatformOperators();
        // Derby does not support DECIMAL, but does have a DOUBLE function.
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.ToNumber, "DOUBLE"));
        addOperator(derbyExtractOperator());
        addOperator(derbyPowerOperator());
        addOperator(derbyLeftOperator());
        addOperator(derbyRightOperator());

        addOperator(avgOperator());
        addOperator(sumOperator());

        addOperator(equalOperator());
        addOperator(notEqualOperator());
        addOperator(lessThanOperator());
        addOperator(lessThanEqualOperator());
        addOperator(greaterThanOperator());
        addOperator(greaterThanEqualOperator());
        addOperator(modOperator());

        addOperator(betweenOperator());
        addOperator(notBetweenOperator());
        addOperator(inOperator());

        addOperator(addOperator());
        addOperator(subtractOperator());
        addOperator(multiplyOperator());
        addOperator(divideOperator());
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X36: The 'AVG' operator is not allowed to take a ? parameter as an operand.</pre>
     */
    protected ExpressionOperator avgOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.average().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X36: The 'SUM' operator is not allowed to take a ? parameter as an operand.</pre>
     */
    protected ExpressionOperator sumOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.sum().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '=' to be ? parameters.</pre>
     */
    protected ExpressionOperator equalOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.equal().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '&lt;&gt;' to be ? parameters.</pre>
     */
    protected ExpressionOperator notEqualOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.notEqual().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '&gt;' to be ? parameters.</pre>
     */
    protected ExpressionOperator greaterThanOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.greaterThan().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '&gt;=' to be ? parameters.</pre>
     */
    protected ExpressionOperator greaterThanEqualOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.greaterThanEqual().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '&lt;' to be ? parameters.</pre>
     */
    protected ExpressionOperator lessThanOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.lessThan().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '&lt;=' to be ? parameters.</pre>
     */
    protected ExpressionOperator lessThanEqualOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.lessThanEqual().copyTo(operator);
        return operator;
    }

    // Emulate POWER(:a,:b) as EXP((:b)*LN(:a))
    private static ExpressionOperator derbyPowerOperator() {
        ExpressionOperator operator = ExpressionOperator.power();

        List<String> v = new ArrayList<>(4);
        v.add("EXP((");
        v.add(")*LN(");
        v.add("))");
        operator.printsAs(v);
        int[] argumentIndices = new int[2];
        argumentIndices[0] = 1;
        argumentIndices[1] = 0;
        operator.setArgumentIndices(argumentIndices);
        return operator;
    }

    /**
     * INTERNAL:
     * Derby equivalent to LEFT e.g. LEFT(dname, 5) is SUBSTR e.g. SUBSTR(dname, 1, 5).
     */
    protected static ExpressionOperator derbyLeftOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.Left);
        List<String> v = new ArrayList<>(3);
        v.add("SUBSTR(");
        v.add(", 1, ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        int[] indices = { 0, 1 };
        exOperator.setArgumentIndices(indices);
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Derby equivalent to RIGHT e.g. RIGHT(dname, 5) is SUBSTR e.g. SUBSTR(dname, LENGTH(dname) +1 - 5, 5).
     */
    protected static ExpressionOperator derbyRightOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.Right);
        List<String> v = new ArrayList<>(3);
        v.add("SUBSTR(");
        v.add(", LENGTH(");
        v.add(") + 1 - ");
        v.add(", ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        int[] indices = { 0, 0, 1, 1 };
        exOperator.setArgumentIndices(indices);
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    // Derby does not suport native EXTRACT at all. There are specific functions for most of the date/time parts.
    // QUARTER part needs to be calculated from MONTH value.
    // SECOND does not return fraction part and there is no fraction specific function available.
    private static final class DerbyExtractOperator extends ExtractOperator {

        // QUARTER emulation: ((MONTH(:first)+2)/3)
        private static final String[] QUARTER_STRINGS = new String[] {"((MONTH(", ")+2)/3)"};
        // WEEK emulation: based on https://www.sqlservercentral.com/articles/a-simple-formula-to-calculate-the-iso-week-number
        private static final String[] WEEK_STRINGS = new String[] {
                "(({FN TIMESTAMPDIFF(sql_tsi_day,DATE(CHAR(YEAR(",
                "),4)||'-01-01'),{FN TIMESTAMPADD(sql_tsi_day,{FN TIMESTAMPDIFF(sql_tsi_day,{d '1753-01-01'},",
                ")}/7*7,{d '1753-01-04'})})}+7)/7)"
        };
        // SECOND emulation: CAST(SECOND(:first) AS FLOAT)
        private static final String[] SECOND_STRINGS = new String[] {"CAST(SECOND(", ") AS FLOAT)"};

        // Derby native database Strings to be printed for EXTRACT expression
        // Printing of prefix database String is set to be skipped for Derby
        private static List<String> derbyDbStrings() {
            final List<String> dbStrings = new ArrayList<>(2);
            dbStrings.add("(");
            dbStrings.add(")");
            return dbStrings;
        }

        private DerbyExtractOperator() {
            // Register custom native database Strings
            super(derbyDbStrings());
            // Skip prefix to be printed from database Strings
            bePostfix();
        }

        @Override
        protected void printQuarterSQL(final Expression first, Expression second, final ExpressionSQLPrinter printer) {
            printer.printString(QUARTER_STRINGS[0]);
            first.printSQL(printer);
            printer.printString(QUARTER_STRINGS[1]);
        }

        @Override
        protected void printQuarterJava(final Expression first, Expression second, final ExpressionJavaPrinter printer) {
            printer.printString(QUARTER_STRINGS[0]);
            first.printJava(printer);
            printer.printString(QUARTER_STRINGS[1]);
        }

        @Override
        protected void printSecondSQL(final Expression first, Expression second, final ExpressionSQLPrinter printer) {
            printer.printString(SECOND_STRINGS[0]);
            first.printSQL(printer);
            printer.printString(SECOND_STRINGS[1]);
        }

        @Override
        protected void printSecondJava(final Expression first, Expression second, final ExpressionJavaPrinter printer) {
            printer.printString(SECOND_STRINGS[0]);
            first.printJava(printer);
            printer.printString(SECOND_STRINGS[1]);
        }

        @Override
        protected void printWeekSQL(final Expression first, Expression second, final ExpressionSQLPrinter printer) {
            printer.printString(WEEK_STRINGS[0]);
            first.printSQL(printer);
            printer.printString(WEEK_STRINGS[1]);
            first.printSQL(printer);
            printer.printString(WEEK_STRINGS[2]);
        }

        @Override
        protected void printWeekJava(final Expression first, Expression second, final ExpressionJavaPrinter printer) {
            printer.printString(WEEK_STRINGS[0]);
            first.printJava(printer);
            printer.printString(WEEK_STRINGS[1]);
            first.printJava(printer);
            printer.printString(WEEK_STRINGS[2]);
        }

    }

    // Create EXTRACT operator form Derby platform
    private static ExpressionOperator derbyExtractOperator() {
        return new DerbyExtractOperator();
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '+' to be ? parameters.</pre>
     */
    protected ExpressionOperator addOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.add().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '-' to be ? parameters.</pre>
     */
    protected ExpressionOperator subtractOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.subtract().copyTo(operator);
        return operator;
    }

    /**
     * Set binding support to PARTIAL.
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '*' to be ? parameters.</pre>
     */
    protected ExpressionOperator multiplyOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.multiply().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '/' to be ? parameters.</pre>
     */
    protected ExpressionOperator divideOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.divide().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '||' to be ? parameters.</pre>
     */
    protected ExpressionOperator concatOperator() {
        ExpressionOperator operatorS = super.concatOperator();
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        operatorS.copyTo(operator);
        return operator;
    }

    /**
     * Enable binding since DB2 disables it
     * <p>
     * With binding enabled, Derby does not throw an exception
     */
    @Override
    protected ExpressionOperator trim2() {
        ExpressionOperator operatorS = super.trim2();
        ExpressionOperator operator = ExpressionOperator.trim2();
        operatorS.copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of 'mod' to be ? parameters.</pre>
     */
    protected ExpressionOperator modOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.mod().copyTo(operator);
        return operator;
    }

    /**
     * Enable binding since DB2 disables it
     * <p>
     * With binding enabled, Derby does not throw an exception
     */
    @Override
    protected ExpressionOperator ltrim2Operator() {
        ExpressionOperator operatorS = super.ltrim2Operator();
        ExpressionOperator operator = ExpressionOperator.leftTrim2();
        operatorS.copyTo(operator);
        return operator;
    }

    /**
     * Enable binding since DB2 disables it
     * <p>
     * With binding enabled, Derby does not throw an exception
     */
    @Override
    protected ExpressionOperator rtrim2Operator() {
        ExpressionOperator operatorS = super.rtrim2Operator();
        ExpressionOperator operator = ExpressionOperator.rightTrim2();
        operatorS.copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of 'BETWEEN' to be ? parameters.</pre>
     */
    protected ExpressionOperator betweenOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.between().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of 'BETWEEN' to be ? parameters.</pre>
     */
    protected ExpressionOperator notBetweenOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.notBetween().copyTo(operator);
        return operator;
    }

    /**
     * Derby requires that at least one argument be a known type
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of 'IN' to be ? parameters.</pre>
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
                    if (val instanceof Collection values) {
                        firstBound = false;
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
                    if (val instanceof Collection values) {
                        firstBound = false;
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
     * Emulate ROUND as FLOOR((:x)*1e:n+0.5)/1e:n
     */
    @Override
    protected ExpressionOperator roundOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.round().copyTo(operator);

        List<String> v = new ArrayList<>(4);
        v.add("FLOOR((");
        v.add(")*1e");
        v.add("+0.5)/1e");
        v.add("");
        operator.printsAs(v);
        int[] argumentIndices = new int[3];
        argumentIndices[0] = 0;
        argumentIndices[1] = 1;
        argumentIndices[2] = 1;
        operator.setArgumentIndices(argumentIndices);
        operator.setIsBindingSupported(false);
        return operator;
    }

    /**
     * INTERNAL
     * Derby has some issues with using parameters on certain functions and relations.
     * This allows statements to disable binding, for queries, only in these cases.
     * If users set casting on, then casting is used instead of dynamic SQL.
     */
    @Override
    public boolean isDynamicSQLRequiredForFunctions() {
        if(shouldForceBindAllParameters()) {
            return false;
        }
        return !isCastRequired();
    }

    /**
     * INTERNAL:
     * Use the JDBC maxResults and firstResultIndex setting to compute a value to use when
     * limiting the results of a query in SQL.  These limits tend to be used in two ways.
     * <p>
     * 1. MaxRows is the index of the last row to be returned (like JDBC maxResults)
     * 2. MaxRows is the number of rows to be returned
     * <p>
     * Derby uses case #2 and therefore the maxResults has to be altered based on the firstResultIndex.
     */
    @Override
    public int computeMaxRowsForSQL(int firstResultIndex, int maxResults) {
        if (!isSequenceSupported) {
            return maxResults;
        }
        return maxResults - ((firstResultIndex >= 0) ? firstResultIndex : 0);
    }

    /**
     * INTERNAL:
     * Print the SQL representation of the statement on a stream, storing the fields
     * in the DatabaseCall.
     * <p>
     * Derby supports pagination through its "OFFSET n ROWS FETCH NEXT m ROWS" syntax.
     */
    @Override
    public void printSQLSelectStatement(DatabaseCall call, ExpressionSQLPrinter printer, SQLSelectStatement statement) {
        int max = 0;
        int firstRow = 0;

        if (statement.getQuery() != null) {
            max = statement.getQuery().getMaxRows();
            firstRow = statement.getQuery().getFirstResult();
        }

        if (!(shouldUseRownumFiltering()) || (!(max > 0) && !(firstRow > 0))) {
            call.setFields(statement.printSQL(printer));
            statement.appendForUpdateClause(printer);
            return;
        }

        statement.setUseUniqueFieldAliases(true);
        call.setFields(statement.printSQL(printer));

        // Derby Syntax:
        //   OFFSET { integer-literal | ? } {ROW | ROWS}
        //   FETCH { FIRST | NEXT } [integer-literal | ? ] {ROW | ROWS} ONLY
        printer.printString(" OFFSET ");

        if (isOffsetFetchParameterSupported) {
            // Parameter support added in 10.6.1
            if (max > 0) {
                printer.printParameter(DatabaseCall.FIRSTRESULT_FIELD);
                printer.printString(" ROWS FETCH NEXT ");
                printer.printParameter(DatabaseCall.MAXROW_FIELD);
                printer.printString(" ROWS ONLY ");
            } else {
                printer.printParameter(DatabaseCall.FIRSTRESULT_FIELD);
                printer.printString(" ROWS ");
            }
        } else {
            // Parameters not supported before 10.6.1
            String frStr = Integer.toString(firstRow);
            String maxStr = Integer.toString(max);

            if (max > 0) {
                printer.printString(frStr);
                printer.printString(" ROWS FETCH NEXT ");
                printer.printString(maxStr);
                printer.printString(" ROWS ONLY ");
            } else {
                printer.printString(frStr);
                printer.printString(" ROWS ");
            }
        }

        call.setIgnoreFirstRowSetting(true);
        call.setIgnoreMaxResultsSetting(true);
    }

    /**
     * INTERNAL: Derby supports sequence objects as of 10.6.1.
     */
    @Override
    public boolean supportsSequenceObjects() {
        return this.isSequenceSupported;
    }

    @Override
    public boolean isAlterSequenceObjectSupported() {
        return false;
    }

    /**
     * INTERNAL: Derby supports sequence objects as of 10.6.1.
     */
    @Override
    public Writer buildSequenceObjectDeletionWriter(Writer writer, String fullSeqName) throws IOException {
        writer.write("DROP SEQUENCE ");
        writer.write(fullSeqName);
        writer.write(" RESTRICT");
        return writer;
    }

    /**
     * INTERNAL:
     */
    @Override
    public void initializeConnectionData(Connection connection) throws SQLException {
        if (isConnectionDataInitialized) {
            return;
        }

        String databaseVersion = connection.getMetaData().getDatabaseProductVersion();
        if (Helper.compareVersions(databaseVersion, "10.6.1") >= 0) {
            isSequenceSupported = true;
            isOffsetFetchParameterSupported = true;
        }

        isConnectionDataInitialized = true;
    }

    /**
     * INTERNAL
     * Set the parameter in the JDBC statement at the given index.
     * This support a wide range of different parameter types,
     * and is heavily optimized for common types.
     */
    public void setParameterValueInDatabaseCall(Object parameter,
                                                PreparedStatement statement, int index, AbstractSession session)
            throws SQLException {
        if (parameter instanceof LocalTime) {
            statement.setTime(
                    index, java.sql.Time.valueOf((LocalTime) parameter));
            return;
        }
        super.setParameterValueInDatabaseCall(parameter, statement, index, session);
    }

    /**
     * INTERNAL
     * Set the parameter in the JDBC statement with the given name.
     * This support a wide range of different parameter types,
     * and is heavily optimized for common types.
     */
    public void setParameterValueInDatabaseCall(Object parameter,
                                                CallableStatement statement, String name, AbstractSession session)
            throws SQLException {
        if (parameter instanceof LocalTime) {
            statement.setTime(
                    name, java.sql.Time.valueOf((LocalTime) parameter));
            return;
        }
        super.setParameterValueInDatabaseCall(parameter, statement, name, session);
    }

    /**
     * Returns the number of parameters that used binding.
     * Should only be called in case binding is not used.
     */
    public int appendParameterInternal(Call call, Writer writer, Object parameter) {
        try {
            int nBoundParameters = 0;
            if (parameter instanceof LocalTime) {
                appendTime(java.sql.Time.valueOf((LocalTime) parameter), writer);
                return nBoundParameters;
            }
        } catch (IOException exception) {
            throw ValidationException.fileError(exception);
        }
        return super.appendParameterInternal(call, writer, parameter);
    }

}
