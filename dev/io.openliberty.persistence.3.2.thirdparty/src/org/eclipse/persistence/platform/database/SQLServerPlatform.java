/*
 * Copyright (c) 1998, 2024 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 1998, 2024 IBM Corporation. All rights reserved.
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
//     09/14/2011-2.3.1 Guy Pelletier
//       - 357533: Allow DDL queries to execute even when Multitenant entities are part of the PU
//     02/19/2015 - Rick Curtis
//       - 458877 : Add national character support
//     02/23/2015-2.6 Dalia Abo Sheasha
//       - 460607: Change DatabasePlatform StoredProcedur eTerminationToken to be configurable
package org.eclipse.persistence.platform.database;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionOperator;
import org.eclipse.persistence.internal.databaseaccess.DatabaseCall;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;
import org.eclipse.persistence.internal.expressions.ExpressionJavaPrinter;
import org.eclipse.persistence.internal.expressions.ExpressionSQLPrinter;
import org.eclipse.persistence.internal.expressions.ExtractOperator;
import org.eclipse.persistence.internal.expressions.RelationExpression;
import org.eclipse.persistence.internal.expressions.SQLSelectStatement;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.queries.ReadQuery;
import org.eclipse.persistence.queries.SQLCall;
import org.eclipse.persistence.queries.ValueReadQuery;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * <p><b>Purpose</b>: Provides SQL Server specific behavior.
 * <p><b>Responsibilities</b>:<ul>
 * <li> Native SQL for byte[], Date, Time, {@literal &} Timestamp.
 * <li> Native sequencing using @@IDENTITY.
 * </ul>
 *
 * @since TOPLink/Java 1.0
 */
public class SQLServerPlatform extends org.eclipse.persistence.platform.database.DatabasePlatform {
    
    /** MSSQL-specific JDBC type constants */
    private static final int DATETIMEOFFSET_TYPE = -155;
    
    /** Support for sequence objects and OFFSET FETCH NEXT added in SQL Server 2012 */
    private boolean isVersion11OrHigher;
    
    /** The official MS JDBC driver fully supports ODT since version 7.1.4 */
    private Boolean driverSupportsOffsetDateTime;
    
    private boolean isConnectionDataInitialized;

    public SQLServerPlatform(){
        super();
        this.pingSQL = "SELECT 1";
        this.storedProcedureTerminationToken = " go";
        this.supportsReturnGeneratedKeys = true;
    }

    @Override
    public Map<Object, Object> connectionProperties() {
        // All MS SQL Server properties must be of String type.
        // Send Time values as TIME type.
        return Map.of("sendTimeAsDatetime", Boolean.FALSE.toString());
    }

    @Override
    public void initializeConnectionData(Connection connection) throws SQLException {
        if (isConnectionDataInitialized) {
            return;
        }
        
        DatabaseMetaData dmd = connection.getMetaData();
        // could be using a non-MS driver (e.g. jTDS)
        boolean isMicrosoftDriver = dmd.getDriverName().startsWith("Microsoft JDBC Driver");
        int databaseVersion = dmd.getDatabaseMajorVersion();
        String driverVersion = dmd.getDriverVersion();
        isVersion11OrHigher = databaseVersion >= 11;
        if (driverSupportsOffsetDateTime == null) {
            driverSupportsOffsetDateTime = isMicrosoftDriver && Helper.compareVersions(driverVersion, "7.1.4") >= 0;
        }
        driverSupportsNationalCharacterVarying = isMicrosoftDriver && Helper.compareVersions(driverVersion, "4.0.0") >= 0;
        
        isConnectionDataInitialized = true;
    }

    /**
     * Allow user to turn off ODT support, in case they rely on the old behavior.
     */
    public void setDriverSupportsOffsetDateTime(boolean driverSupportsOffsetDateTime) {
        this.driverSupportsOffsetDateTime = driverSupportsOffsetDateTime;
    }

    /**
     * If using native SQL then print a byte[] as '0xFF...'
     */
    @Override
    protected void appendByteArray(byte[] bytes, Writer writer) throws IOException {
        if (usesNativeSQL() && (!usesByteArrayBinding())) {
            writer.write("0x");
            Helper.writeHexString(bytes, writer);
        } else {
            super.appendByteArray(bytes, writer);
        }
    }

    /**
     * Answer a platform correct string representation of a Date, suitable for SQL generation.
     * Native format: 'yyyy-mm-dd
     */
    @Override
    protected void appendDate(java.sql.Date date, Writer writer) throws IOException {
        if (usesNativeSQL()) {
            writer.write("'");
            writer.write(Helper.printDate(date));
            writer.write("'");
        } else {
            super.appendDate(date, writer);
        }
    }

    /**
     * Write a timestamp in Sybase specific format ( yyyy-mm-dd-hh.mm.ss.fff)
     */
    protected void appendSybaseTimestamp(java.sql.Timestamp timestamp, Writer writer) throws IOException {
        writer.write("'");
        writer.write(Helper.printTimestampWithoutNanos(timestamp));
        writer.write(':');

        // Must truncate the nanos to three decimal places,
        // it is actually a complex algorithm...
        String nanoString = Integer.toString(timestamp.getNanos());
        int numberOfZeros = 0;
        for (int num = Math.min(9 - nanoString.length(), 3); num > 0; num--) {
            writer.write('0');
            numberOfZeros++;
        }
        if ((nanoString.length() + numberOfZeros) > 3) {
            nanoString = nanoString.substring(0, (3 - numberOfZeros));
        }
        writer.write(nanoString);
        writer.write("'");
    }

    /**
     * Write a timestamp in Sybase specific format ( yyyy-mm-dd-hh.mm.ss.fff)
     */
    protected void appendSybaseCalendar(Calendar calendar, Writer writer) throws IOException {
        writer.write("'");
        writer.write(Helper.printCalendar(calendar));
        writer.write("'");
    }

    /**
    *    Answer a platform correct string representation of a Time, suitable for SQL generation.
    *    The time is printed in the ODBC platform independent format {t'hh:mm:ss'}.
    */
    @Override
    protected void appendTime(java.sql.Time time, Writer writer) throws IOException {
        if (usesNativeSQL()) {
            writer.write("'");
            writer.write(Helper.printTime(time));
            writer.write("'");
        } else {
            super.appendTime(time, writer);
        }
    }

    /**
     * Answer a platform correct string representation of a Timestamp, suitable for SQL generation.
     * The date is printed in the ODBC platform independent format {d'YYYY-MM-DD'}.
     */
    @Override
    protected void appendTimestamp(java.sql.Timestamp timestamp, Writer writer) throws IOException {
        if (usesNativeSQL()) {
            appendSybaseTimestamp(timestamp, writer);
        } else {
            super.appendTimestamp(timestamp, writer);
        }
    }

    /**
     * Answer a platform correct string representation of a Calendar, suitable for SQL generation.
     * The date is printed in the ODBC platform independent format {d'YYYY-MM-DD'}.
     */
    @Override
    protected void appendCalendar(Calendar calendar, Writer writer) throws IOException {
        if (usesNativeSQL()) {
            appendSybaseCalendar(calendar, writer);
        } else {
            super.appendCalendar(calendar, writer);
        }
    }

    @Override
    protected Map<String, Class<?>> buildClassTypes() {
        Map<String, Class<?>> classTypeMapping = super.buildClassTypes();
        classTypeMapping.put("DATETIME2", java.sql.Timestamp.class);
        return classTypeMapping;
    }

    @Override
    protected Hashtable<Class<?>, FieldTypeDefinition> buildFieldTypes() {
        Hashtable<Class<?>, FieldTypeDefinition> fieldTypeMapping = new Hashtable<>();
        fieldTypeMapping.put(Boolean.class, new FieldTypeDefinition("BIT default 0", false));

        fieldTypeMapping.put(Integer.class, new FieldTypeDefinition("INTEGER", false));
        fieldTypeMapping.put(Long.class, new FieldTypeDefinition("NUMERIC", 19));
        fieldTypeMapping.put(Float.class, new FieldTypeDefinition("FLOAT(16)", false));
        fieldTypeMapping.put(Double.class, new FieldTypeDefinition("FLOAT(32)", false));
        fieldTypeMapping.put(Short.class, new FieldTypeDefinition("SMALLINT", false));
        fieldTypeMapping.put(Byte.class, new FieldTypeDefinition("SMALLINT", false));
        fieldTypeMapping.put(java.math.BigInteger.class, new FieldTypeDefinition("NUMERIC", 28));
        fieldTypeMapping.put(java.math.BigDecimal.class, new FieldTypeDefinition("NUMERIC", 28).setLimits(28, -19, 19));
        fieldTypeMapping.put(Number.class, new FieldTypeDefinition("NUMERIC", 28).setLimits(28, -19, 19));
        // Create String column to support unicode characters
        if(getUseNationalCharacterVaryingTypeForString()){
            fieldTypeMapping.put(String.class, new FieldTypeDefinition("NVARCHAR", DEFAULT_VARCHAR_SIZE));
        }else {
            fieldTypeMapping.put(String.class, new FieldTypeDefinition("VARCHAR", DEFAULT_VARCHAR_SIZE));
        }

        fieldTypeMapping.put(Character.class, new FieldTypeDefinition("CHAR", 1));

        fieldTypeMapping.put(Byte[].class, new FieldTypeDefinition("IMAGE", false));
        fieldTypeMapping.put(Character[].class, new FieldTypeDefinition("TEXT", false));
        fieldTypeMapping.put(byte[].class, new FieldTypeDefinition("IMAGE", false));
        fieldTypeMapping.put(char[].class, new FieldTypeDefinition("TEXT", false));
        fieldTypeMapping.put(java.sql.Blob.class, new FieldTypeDefinition("IMAGE", false));
        fieldTypeMapping.put(java.sql.Clob.class, new FieldTypeDefinition("TEXT", false));

        fieldTypeMapping.put(java.sql.Date.class, new FieldTypeDefinition("DATE", false));
        fieldTypeMapping.put(java.sql.Time.class, new FieldTypeDefinition("TIME", false));
        fieldTypeMapping.put(java.sql.Timestamp.class, new FieldTypeDefinition("DATETIME2", false));
        fieldTypeMapping.put(java.time.LocalDate.class, new FieldTypeDefinition("DATE", false));
        fieldTypeMapping.put(java.time.LocalTime.class, new FieldTypeDefinition("TIME", false));
        fieldTypeMapping.put(java.time.LocalDateTime.class, new FieldTypeDefinition("DATETIME2", false));
        fieldTypeMapping.put(java.time.OffsetTime.class, new FieldTypeDefinition("DATETIME2", false));
        fieldTypeMapping.put(java.time.OffsetDateTime.class, new FieldTypeDefinition("DATETIME2", false));
        fieldTypeMapping.put(java.time.Instant.class, new FieldTypeDefinition("DATETIME2", false));

        return fieldTypeMapping;
    }

    /**
     * INTERNAL:
     * Build the identity query for native sequencing.
     */
    @Override
    public ValueReadQuery buildSelectQueryForIdentity() {
        ValueReadQuery selectQuery = new ValueReadQuery();
        StringWriter writer = new StringWriter();
        writer.write("SELECT @@IDENTITY");
        selectQuery.setSQLString(writer.toString());
        return selectQuery;
    }

    /**
     * INTERNAL: Produce a DataReadQuery which updates(!) the sequence number in
     * the database and returns it.
     *
     * @param qualifiedSeqName
     *            a defined database sequence
     */
    @Override
    public ValueReadQuery buildSelectQueryForSequenceObject(String qualifiedSeqName, Integer size) {
        return new ValueReadQuery("SELECT NEXT VALUE FOR " + qualifiedSeqName);
    }

    /**
     * INTERNAL:
     * In SQLServer local temporary table created by one
     * PreparedStatement can't be used in another PreparedStatement.
     * Workaround is to use Statement instead of PreparedStatement.
     */
    @Override
    public boolean dontBindUpdateAllQueryUsingTempTables() {
        return true;
    }

    /**
     * Used for batch writing and sp defs.
     */
    @Override
    public String getBatchDelimiterString() {
        return "";
    }

    /**
     * This method is used to print the required output parameter token for the
     * specific platform.  Used when stored procedures are created.
     */
    @Override
    public String getCreationInOutputProcedureToken() {
        return getInOutputProcedureToken();
    }

    /**
     * This method is used to print the required output parameter token for the
     * specific platform.  Used when stored procedures are created.
     */
    @Override
    public String getCreationOutputProcedureToken() {
        return "OUTPUT";
    }

    /**
     * This method is used to print the output parameter token when stored
     * procedures are called
     */
    @Override
    public String getInOutputProcedureToken() {
        return "OUT";
    }

    /**
     * INTERNAL:
     * returns the maximum number of characters that can be used in a field
     * name on this platform.
     */
    @Override
    public int getMaxFieldNameSize() {
        return 22;
    }

    /**
     * Return the catalog information through using the native SQL catalog selects.
     * This is required because many JDBC driver do not support meta-data.
     * Willcards can be passed as arguments.
     */
    public Vector getNativeTableInfo(String table, String creator, AbstractSession session) {
        // need to filter only tables / views
        String query = "SELECT * FROM sysobjects WHERE table_type <> 'SYSTEM_TABLE'";
        if (table != null) {
            if (table.indexOf('%') != -1) {
                query = query + " AND table_name LIKE " + table;
            } else {
                query = query + " AND table_name = " + table;
            }
        }
        if (creator != null) {
            if (creator.indexOf('%') != -1) {
                query = query + " AND table_owner LIKE " + creator;
            } else {
                query = query + " AND table_owner = " + creator;
            }
        }
        return session.executeSelectingCall(new SQLCall(query));
    }

    /**
     * This method is used to print the output parameter token when stored
     * procedures are called
     */
    @Override
    public String getOutputProcedureToken() {
        return "";
    }

    /**
     * Used for sp defs.
     */
    @Override
    public String getProcedureArgumentString() {
        return "@";
    }

    /**
     * Used for sp calls.
     */
    @Override
    public String getProcedureCallHeader() {
        return "EXECUTE ";
    }

    @Override
    public String getStoredProcedureParameterPrefix() {
        return "@";
    }

    /**
     * PUBLIC:
     * This method returns the query to select the timestamp
     * from the server for SQLServer.
     */
    @Override
    public ValueReadQuery getTimestampQuery() {
        if (timestampQuery == null) {
            timestampQuery = new ValueReadQuery();
            timestampQuery.setSQLString("SELECT GETDATE()");
            timestampQuery.setAllowNativeSQLQuery(true);
        }
        return timestampQuery;
    }

    /**
     * INTERNAL:
     * Lock.
     * UPDLOCK seems like the correct table hint to use:
     * HOLDLOCK is too weak - doesn't lock out another read with HOLDLOCK,
     * XLOCK is too strong - locks out another read which doesn't use any locks.
     * UPDLOCK seems to behave exactly like Oracle's FOR UPDATE:
     * locking out updates and other reads with FOR UPDATE but allowing other reads without locks.
     * SQLServer seems to decide itself on the granularity of the lock - it could lock more than
     * the returned rows (for instance a page). It could be forced to obtain
     * to make sure to obtain row level lock:
     * WITH (UPDLOCK, ROWLOCK)
     * However this approach is strongly discouraged because it can consume too
     * much resources: selecting 900 rows from and requiring a "personal"
     * lock for each row may not be feasible because of not enough memory available at the moment -
     * in that case SQLServer will wait until the resource becomes available.
     *
     */
    @Override
    public String getSelectForUpdateString() {
        return " WITH (UPDLOCK)";
    }

    /**
     * INTERNAL:
     * This syntax does no wait on the lock.
     */
    @Override
    public String getSelectForUpdateNoWaitString() {
        return " WITH (UPDLOCK, NOWAIT)";
    }

    /**
     * INTERNAL:
     * Indicates whether locking clause should be printed after where clause by SQLSelectStatement.
     * Example:
     *   on Oracle platform (method returns true):
     *     SELECT ADDRESS_ID, ... FROM ADDRESS WHERE (ADDRESS_ID = ?) FOR UPDATE
     *   on SQLServer platform (method returns false):
     *     SELECT ADDRESS_ID, ... FROM ADDRESS WITH (UPDLOCK) WHERE (ADDRESS_ID = ?)
     */
    @Override
    public boolean shouldPrintLockingClauseAfterWhereClause() {
        return false;
    }

    /**
     * Initialize any platform-specific operators
     */
    @Override
    protected void initializePlatformOperators() {
        super.initializePlatformOperators();
        addOperator(operatorOuterJoin());
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.Today, "GETDATE"));
        // GETDATE returns both date and time. It is not the perfect match for
        // ExpressionOperator.currentDate and ExpressionOperator.currentTime
        // However, there is no known function on sql server that returns just
        // the date or just the time.
        addOperator(ExpressionOperator.simpleFunctionNoParentheses(ExpressionOperator.CurrentDate, "CONVERT(DATE, GETDATE())"));
        addOperator(ExpressionOperator.simpleFunctionNoParentheses(ExpressionOperator.CurrentTime, "CONVERT(TIME, GETDATE())"));
        addOperator(ExpressionOperator.simpleFunctionNoParentheses(ExpressionOperator.LocalDate, "CONVERT(DATE, GETDATE())"));
        addOperator(ExpressionOperator.simpleFunctionNoParentheses(ExpressionOperator.LocalTime, "CONVERT(TIME, GETDATE())"));
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.Length, "CHAR_LENGTH"));
        addOperator(ExpressionOperator.simpleThreeArgumentFunction(ExpressionOperator.Substring, "SUBSTRING"));
        addOperator(singleArgumentSubstringOperator());
        addOperator(ExpressionOperator.addDate());
        addOperator(ExpressionOperator.dateName());
        addOperator(ExpressionOperator.datePart());
        addOperator(ExpressionOperator.dateDifference());
        addOperator(ExpressionOperator.difference());
        addOperator(ExpressionOperator.charIndex());
        addOperator(ExpressionOperator.charLength());
        addOperator(ExpressionOperator.reverse());
        addOperator(ExpressionOperator.replicate());
        addOperator(ExpressionOperator.right());
        addOperator(ExpressionOperator.cot());
        addOperator(ExpressionOperator.simpleTwoArgumentFunction(ExpressionOperator.Atan2, "ATN2"));
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.Ln, "LOG"));
        addOperator(addMonthsOperator());
        addOperator(inStringOperator());
        // bug 3061144
        addOperator(ExpressionOperator.simpleTwoArgumentFunction(ExpressionOperator.Nvl, "ISNULL"));
        // CR### TO_NUMBER, TO_CHAR, TO_DATE is CONVERT(type, ?)
        addOperator(toNumberOperator());
        addOperator(toDateToStringOperator());
        addOperator(toDateOperator());
        addOperator(toCharOperator());
        addOperator(locateOperator());
        addOperator(locate2Operator());
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.Ceil, "CEILING"));
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.Length, "LEN"));
        addOperator(modOperator());
        addOperator(ExpressionOperator.simpleAggregate(ExpressionOperator.StandardDeviation, "STDEV", "standardDeviation"));
        addOperator(trimOperator());
        addOperator(trim2Operator());
        addOperator(mssqlExtractOperator());
    }

    // SQL Server EXTRACT operator needs emulation of SECOND and WEEK date/time parts.
    // EXTRACT SECOND operator need more complex statement to join values of SECOND and NANOSECOND.
    private static final class SqlServerExtractOperator extends ExtractOperator {

        // SECOND emulation: (CAST(DATEPART(NANOSECOND, date) AS FLOAT)/1000000000 + DATEPART(SECOND, date))
        private static final String[] SECOND_STRINGS = new String[] {"(CAST(DATEPART(NANOSECOND, ", ") AS FLOAT)/1000000000 + DATEPART(SECOND, ", "))"};
        // WEEK replacement: ISO_WEEK
        private static final String[] WEEK_STRINGS = new String[] {"DATEPART(ISO_WEEK,", ")"};

        // SQL Server native database Strings to be printed for EXTRACT expression
        private static List<String> mssqlDbStrings() {
            final List<String> dbStrings = new ArrayList<>(2);
            dbStrings.add("DATEPART(");
            dbStrings.add(",");
            dbStrings.add(")");
            return dbStrings;
        }

        private SqlServerExtractOperator() {
            super(mssqlDbStrings());
        }

        @Override
        protected void printSecondSQL(final Expression first, Expression second, final ExpressionSQLPrinter printer) {
            printer.printString(SECOND_STRINGS[0]);
            first.printSQL(printer);
            printer.printString(SECOND_STRINGS[1]);
            first.printSQL(printer);
            printer.printString(SECOND_STRINGS[2]);
        }

        @Override
        protected void printSecondJava(final Expression first, Expression second, final ExpressionJavaPrinter printer) {
            printer.printString(SECOND_STRINGS[0]);
            first.printJava(printer);
            printer.printString(SECOND_STRINGS[1]);
            first.printJava(printer);
            printer.printString(SECOND_STRINGS[2]);
        }

        @Override
        protected void printWeekSQL(final Expression first, Expression second, final ExpressionSQLPrinter printer) {
            printer.printString(WEEK_STRINGS[0]);
            first.printSQL(printer);
            printer.printString(WEEK_STRINGS[1]);
        }

        @Override
        protected void printWeekJava(final Expression first, Expression second, final ExpressionJavaPrinter printer) {
            printer.printString(WEEK_STRINGS[0]);
            first.printJava(printer);
            printer.printString(WEEK_STRINGS[1]);
        }

    }

    // Create EXTRACT operator form SQL Server platform
    private static ExpressionOperator mssqlExtractOperator() {
        return new SqlServerExtractOperator();
    }

    /**
     * INTERNAL:
     * Use RTRIM(LTRIM(?)) function for trim.
     */
    protected static ExpressionOperator trimOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.Trim);
        List<String> v = new ArrayList<>(2);
        v.add("RTRIM(LTRIM(");
        v.add("))");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build Trim operator.
     */
    protected static ExpressionOperator trim2Operator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.Trim2);
        List<String> v = new ArrayList<>(5);
        v.add("RTRIM(");
        v.add(" FROM LTRIM(");
        v.add(" FROM ");
        v.add("))");
        exOperator.printsAs(v);
        int[] argumentIndices = new int[3];
        argumentIndices[0] = 1;
        argumentIndices[1] = 1;
        argumentIndices[2] = 0;
        exOperator.setArgumentIndices(argumentIndices);
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     *    Builds a table of maximum numeric values keyed on java class. This is used for type testing but
     * might also be useful to end users attempting to sanitize values.
     * <p><b>NOTE</b>: BigInteger {@literal &} BigDecimal maximums are dependent upon their precision {@literal &} Scale
     */
    @Override
    public Hashtable<Class<? extends Number>, ? super Number> maximumNumericValues() {
        Hashtable<Class<? extends Number>, ? super Number> values = new Hashtable<>();
        values.put(Integer.class, Integer.MAX_VALUE);
        values.put(Long.class, Long.MAX_VALUE);
        values.put(Double.class, (double) 0);
        values.put(Short.class, Short.MAX_VALUE);
        values.put(Byte.class, Byte.MAX_VALUE);
        values.put(Float.class, (float) 0);
        values.put(java.math.BigInteger.class, new java.math.BigInteger("9999999999999999999999999999"));
        values.put(java.math.BigDecimal.class, new java.math.BigDecimal("999999999.9999999999999999999"));
        return values;
    }

    /**
     *    Builds a table of minimum numeric values keyed on java class. This is used for type testing but
     * might also be useful to end users attempting to sanitize values.
     * <p><b>NOTE</b>: BigInteger {@literal &} BigDecimal minimums are dependent upon their precision {@literal &} Scale
     */
    @Override
    public Hashtable<Class<? extends Number>, ? super Number> minimumNumericValues() {
        Hashtable<Class<? extends Number>, ? super Number> values = new Hashtable<>();
        values.put(Integer.class, Integer.MIN_VALUE);
        values.put(Long.class, Long.MIN_VALUE);
        values.put(Double.class, (double) -9);
        values.put(Short.class, Short.MIN_VALUE);
        values.put(Byte.class, Byte.MIN_VALUE);
        values.put(Float.class, (float) -9);
        values.put(java.math.BigInteger.class, new java.math.BigInteger("-9999999999999999999999999999"));
        values.put(java.math.BigDecimal.class, new java.math.BigDecimal("-999999999.9999999999999999999"));
        return values;
    }

    /**
     * Override the default MOD operator.
     */
    protected ExpressionOperator modOperator() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(ExpressionOperator.Mod);
        List<String> v = new ArrayList<>();
        v.add(" % ");
        result.printsAs(v);
        result.bePostfix();
        result.setNodeClass(org.eclipse.persistence.internal.expressions.FunctionExpression.class);
        return result;
    }

    /**
     * Override the default SubstringSingleArg operator.
     */
    protected static ExpressionOperator singleArgumentSubstringOperator() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(ExpressionOperator.SubstringSingleArg);
        result.setType(ExpressionOperator.FunctionOperator);
        List<String> v = new ArrayList<>();
        v.add("SUBSTRING(");
        v.add(",");
        v.add(", LEN(");
        v.add("))");
        result.printsAs(v);
        int[] indices = new int[3];
        indices[0] = 0;
        indices[1] = 1;
        indices[2] = 0;

        result.setArgumentIndices(indices);
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        result.bePrefix();
        return result;
    }

    /*
     *  Create the outer join operator for this platform
     */
    protected static ExpressionOperator operatorOuterJoin() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(ExpressionOperator.EqualOuterJoin);
        List<String> v = new ArrayList<>();
        v.add(" =* ");
        result.printsAs(v);
        result.bePostfix();
        result.setNodeClass(RelationExpression.class);
        return result;
    }

    /**
     * INTERNAL:
     * create the Locate2 Operator for this platform
     */
    protected static ExpressionOperator locate2Operator() {
        ExpressionOperator result = ExpressionOperator.simpleThreeArgumentFunction(ExpressionOperator.Locate2, "CHARINDEX");
        int[] argumentIndices = new int[3];
        argumentIndices[0] = 1;
        argumentIndices[1] = 0;
        argumentIndices[2] = 2;
        result.setArgumentIndices(argumentIndices);
        return result;
    }



    /**
     * INTERNAL:
     * Function, to add months to a date.
     */
    protected static ExpressionOperator addMonthsOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.AddMonths);
        List<String> v = new ArrayList<>(3);
        v.add("DATEADD(month, ");
        v.add(", ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        int[] indices = { 1, 0 };
        exOperator.setArgumentIndices(indices);
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build instring operator
     */
    protected static ExpressionOperator inStringOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.Instring);
        List<String> v = new ArrayList<>(3);
        v.add("CHARINDEX(");
        v.add(", ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        int[] indices = { 1, 0 };
        exOperator.setArgumentIndices(indices);
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build Sybase equivalent to TO_NUMBER.
     */
    protected static ExpressionOperator toNumberOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.ToNumber);
        List<String> v = new ArrayList<>(2);
        v.add("CONVERT(NUMERIC, ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build Sybase equivalent to TO_CHAR.
     */
    protected static ExpressionOperator toDateToStringOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.DateToString);
        List<String> v = new ArrayList<>(2);
        v.add("CONVERT(CHAR, ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build Sybase equivalent to TO_DATE.
     */
    protected static ExpressionOperator toDateOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.ToDate);
        List<String> v = new ArrayList<>(2);
        v.add("CONVERT(DATETIME, ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build Sybase equivalent to TO_CHAR.
     */
    protected static ExpressionOperator toCharOperator() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ExpressionOperator.FunctionOperator);
        exOperator.setSelector(ExpressionOperator.ToChar);
        List<String> v = new ArrayList<>(2);
        v.add("CONVERT(CHAR, ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build the Sybase equivalent to Locate
     */
    protected static ExpressionOperator locateOperator() {
        ExpressionOperator result = ExpressionOperator.simpleTwoArgumentFunction(ExpressionOperator.Locate, "CHARINDEX");
        int[] argumentIndices = new int[2];
        argumentIndices[0] = 1;
        argumentIndices[1] = 0;
        result.setArgumentIndices(argumentIndices);
        return result;
    }

    /**
     * INTERNAL:
     * Return true if output parameters can be built with result sets.
     */
    @Override
    public boolean isOutputAllowWithResultSet() {
        return false;
    }

    public boolean isSQLServer() {
        return true;
    }

    /**
     * INTERNAL:
     * Append the receiver's field 'identity' constraint clause to a writer.
     */
    @Override
    public void printFieldIdentityClause(Writer writer) throws ValidationException {
        try {
            writer.write(" IDENTITY");
        } catch (IOException ioException) {
            throw ValidationException.fileError(ioException);
        }
    }

    /**
     * INTERNAL:
     * Append the receiver's field 'NULL' constraint clause to a writer.
     */
    @Override
    public void printFieldNullClause(Writer writer) throws ValidationException {
        try {
            writer.write(" NULL");
        } catch (IOException ioException) {
            throw ValidationException.fileError(ioException);
        }
    }

    /**
     * USed for sp calls.
     */
    @Override
    public boolean requiresProcedureCallBrackets() {
        return false;
    }

    /**
     * Used for sp calls.  Sybase must print output after output params.
     */
    @Override
    public boolean requiresProcedureCallOuputToken() {
        return true;
    }

    /**
     * This is required in the construction of the stored procedures with
     * output parameters
     */
    @Override
    public boolean shouldPrintInOutputTokenBeforeType() {
        return false;
    }


    /**
     * This is required in the construction of the stored procedures with
     * output parameters
     */
    @Override
    public boolean shouldPrintOutputTokenBeforeType() {
        return false;
    }

    /**
     * JDBC defines and outer join syntax, many drivers do not support this. So we normally avoid it.
     */
    @Override
    public boolean shouldUseJDBCOuterJoinSyntax() {
        return false;
    }

    /**
     *  INTERNAL:
     *  Indicates whether the platform supports identity.
     *  SQLServer does through IDENTITY field types.
     *  This method is to be used *ONLY* by sequencing classes
     */
    @Override
    public boolean supportsIdentity() {
        return true;
    }

    /**
     * INTERNAL:
     */
    @Override
    public boolean supportsSequenceObjects() {
        return isVersion11OrHigher;
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
        return "CREATE TABLE ";
    }

    /**
     * INTERNAL:
     */
    @Override
    public DatabaseTable getTempTableForTable(DatabaseTable table) {
        return new DatabaseTable("#" + table.getName(), table.getTableQualifier(), table.shouldUseDelimiters(), getStartDelimiter(), getEndDelimiter());
    }

    /**
     * INTERNAL:
     */
    @Override
    public void writeUpdateOriginalFromTempTableSql(Writer writer, DatabaseTable table,
                                                    Collection<DatabaseField> pkFields,
                                                    Collection<DatabaseField> assignedFields) throws IOException
    {
        writer.write("UPDATE ");
        String tableName = table.getQualifiedNameDelimited(this);
        writer.write(tableName);
        String tempTableName = getTempTableForTable(table).getQualifiedNameDelimited(this);
        writeAutoAssignmentSetClause(writer, null, tempTableName, assignedFields, this);
        writer.write(" FROM ");
        writer.write(tableName);
        writer.write(", ");
        writer.write(tempTableName);
        writeAutoJoinWhereClause(writer, tableName, tempTableName, pkFields, this);
    }

    @Override
    public void printSQLSelectStatement(DatabaseCall call, ExpressionSQLPrinter printer, SQLSelectStatement statement) {
        ReadQuery query = statement.getQuery();
        if (query == null || !isVersion11OrHigher || !shouldUseRownumFiltering()) {
            super.printSQLSelectStatement(call, printer, statement);
            return;
        }
        
        int max = Math.max(0, query.getMaxRows());
        int first = Math.max(0, query.getFirstResult());
        
        if (max == 0 && first == 0) {
            super.printSQLSelectStatement(call, printer, statement);
            return;
        }
        
        // OFFSET + FETCH NEXT requires ORDER BY, so add an ordering if there are none
        // this SQL will satisfy the query parser without actually changing the ordering of the rows
        List<Expression> orderBy = statement.getOrderByExpressions();
        if (orderBy.isEmpty()) {
            orderBy.add(statement.getBuilder().literal("ROW_NUMBER() OVER (ORDER BY (SELECT null))"));
        }
        
        // decide exact syntax to use, depending on whether a limit is specified (could just have an offset)
        String offsetFetchSql;
        List<?> offsetFetchArgs;
        if (max == 0) {
            offsetFetchSql = "? OFFSET ? ROWS";
            offsetFetchArgs = Arrays.asList(first);
        } else {
            offsetFetchSql = "? OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            offsetFetchArgs = Arrays.asList(first, max - first);
        }
        
        // append to the last ORDER BY clause
        orderBy.add(orderBy.remove(orderBy.size() - 1).sql(offsetFetchSql, offsetFetchArgs));
        
        super.printSQLSelectStatement(call, printer, statement);
        
        call.setIgnoreFirstRowSetting(true);
        call.setIgnoreMaxResultsSetting(true);
    }

    @Override
    public Object getObjectFromResultSet(ResultSet resultSet, int columnNumber, int type, AbstractSession session)
            throws SQLException {
        if (driverSupportsOffsetDateTime && type == DATETIMEOFFSET_TYPE) {
            // avoid default logic, which would return a microsoft.sql.DateTimeOffset
            return resultSet.getObject(columnNumber, OffsetDateTime.class);
        }
        
        return super.getObjectFromResultSet(resultSet, columnNumber, type, session);
    }

    @Override
    public void setParameterValueInDatabaseCall(
            Object parameter, PreparedStatement statement, int index, AbstractSession session
    ) throws SQLException {
        // Pass date/time instance directly when JDBC driver supports it
        if (driverSupportsOffsetDateTime) {
            if (parameter instanceof OffsetDateTime || parameter instanceof OffsetTime
                    || parameter instanceof LocalTime || parameter instanceof LocalDate || parameter instanceof LocalDateTime) {
                statement.setObject(index, parameter);
                return;
            }
        // Default platform is using statement.setTimestamp(index, ...) for LocalTime and it causes cast exceptions in SQL statements
        } else if (parameter instanceof LocalTime) {
            statement.setTime(
                    index, java.sql.Time.valueOf((LocalTime) parameter));
            return;
        }
        super.setParameterValueInDatabaseCall(parameter, statement, index, session);
    }

    @Override
    public void setParameterValueInDatabaseCall(
            Object parameter, CallableStatement statement, String name, AbstractSession session
    ) throws SQLException {
        // Pass date/time instance directly when JDBC driver supports it
        if (driverSupportsOffsetDateTime) {
            if (parameter instanceof OffsetDateTime || parameter instanceof OffsetTime
                    || parameter instanceof LocalTime || parameter instanceof LocalDate || parameter instanceof LocalDateTime) {
                statement.setObject(name, parameter);
                return;
            }
        // Default platform is using statement.setTimestamp(index, ...) for LocalTime and it causes cast exceptions in SQL statements
        } else if (parameter instanceof LocalTime) {
            statement.setTime(
                    name, java.sql.Time.valueOf((LocalTime) parameter));
            return;
        }

        super.setParameterValueInDatabaseCall(parameter, statement, name, session);
    }

}
