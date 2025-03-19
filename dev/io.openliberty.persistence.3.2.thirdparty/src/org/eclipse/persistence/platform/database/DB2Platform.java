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
//     02/24/2016-2.6.0 Rick Curtis
//       - 460740: Fix pessimistic locking with setFirst/Max results on DB2
//     03/13/2015 - Jody Grassel
//       - 462103 : SQL for Stored Procedure named parameter with DB2 generated with incorrect marker
//     04/15/2016 - Dalia Abo Sheasha
//       - 491824: Setting lock timeout to 0 issues a NOWAIT causing an error in DB2
//     08/22/2017 - Will Dazey
//       - 521037: DB2 default schema is doubled for sequence queries
//     12/06/2018 - Will Dazey
//       - 542491: Add new 'eclipselink.jdbc.force-bind-parameters' property to force enable binding
package org.eclipse.persistence.platform.database;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionOperator;
import org.eclipse.persistence.expressions.ListExpressionOperator;
import org.eclipse.persistence.internal.databaseaccess.DatabaseCall;
import org.eclipse.persistence.internal.databaseaccess.DatasourceCall.ParameterType;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;
import org.eclipse.persistence.internal.expressions.ConstantExpression;
import org.eclipse.persistence.internal.expressions.ExpressionJavaPrinter;
import org.eclipse.persistence.internal.expressions.ExpressionSQLPrinter;
import org.eclipse.persistence.internal.expressions.ParameterExpression;
import org.eclipse.persistence.internal.expressions.SQLSelectStatement;
import org.eclipse.persistence.internal.helper.BasicTypeHelperImpl;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.queries.StoredProcedureCall;
import org.eclipse.persistence.queries.ValueReadQuery;
import org.eclipse.persistence.tools.schemaframework.FieldDefinition;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * <p>
 * <b>Purpose</b>: Provides DB2 specific behavior.
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li>Support for schema creation.
 * <li>Native SQL for byte[], Date, Time, {@literal &} Timestamp.
 * <li>Support for table qualified names.
 * <li>Support for stored procedures.
 * <li>Support for temp tables.
 * <li>Support for casting.
 * <li>Support for database functions.
 * <li>Support for identity sequencing.
 * <li>Support for SEQUENCE sequencing.
 * </ul>
 *
 * @since TOPLink/Java 1.0
 */
public class DB2Platform extends org.eclipse.persistence.platform.database.DatabasePlatform {

    public DB2Platform() {
        super();
        //com.ibm.db2.jcc.DB2Types.CURSOR
        this.cursorCode = -100008;
        this.shouldBindPartialParameters = true;
        this.pingSQL = "VALUES(1)";
        this.supportsReturnGeneratedKeys = true;
    }

    @Override
    public void initializeConnectionData(Connection connection) throws SQLException {
        // DB2 database doesn't support NVARCHAR column types and as such doesn't support calling
        // get/setNString() on the driver.
        this.driverSupportsNationalCharacterVarying = false;
    }

    /**
     * INTERNAL:
     * Append a byte[] in native DB@ format BLOB(hexString) if usesNativeSQL(),
     * otherwise use ODBC format from DatabasePLatform.
     */
    @Override
    protected void appendByteArray(byte[] bytes, Writer writer) throws IOException {
        if (usesNativeSQL()) {
            writer.write("BLOB(x'");
            Helper.writeHexString(bytes, writer);
            writer.write("')");
        } else {
            super.appendByteArray(bytes, writer);
        }
    }

    /**
     * INTERNAL:
     * Appends the Date in native format if usesNativeSQL() otherwise use ODBC
     * format from DatabasePlatform. Native format: 'mm/dd/yyyy'
     */
    @Override
    protected void appendDate(java.sql.Date date, Writer writer) throws IOException {
        if (usesNativeSQL()) {
            appendDB2Date(date, writer);
        } else {
            super.appendDate(date, writer);
        }
    }

    /**
     * INTERNAL:
     * Write a timestamp in DB2 specific format (mm/dd/yyyy).
     */
    protected void appendDB2Date(java.sql.Date date, Writer writer) throws IOException {
        writer.write("'");
        // PERF: Avoid deprecated get methods, that are now very inefficient and
        // used from toString.
        Calendar calendar = Helper.allocateCalendar();
        calendar.setTime(date);

        if ((calendar.get(Calendar.MONTH) + 1) < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(calendar.get(Calendar.MONTH) + 1));
        writer.write('/');
        if (calendar.get(Calendar.DATE) < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(calendar.get(Calendar.DATE)));
        writer.write('/');
        writer.write(Integer.toString(calendar.get(Calendar.YEAR)));
        writer.write("'");

        Helper.releaseCalendar(calendar);
    }

    /**
     * INTERNAL:
     * Write a timestamp in DB2 specific format (yyyy-mm-dd-hh.mm.ss.ffffff).
     */
    protected void appendDB2Timestamp(java.sql.Timestamp timestamp, Writer writer) throws IOException {
        // PERF: Avoid deprecated get methods, that are now very inefficient and
        // used from toString.
        Calendar calendar = Helper.allocateCalendar();
        calendar.setTime(timestamp);

        writer.write(Helper.printDate(calendar));
        writer.write('-');
        if (calendar.get(Calendar.HOUR_OF_DAY) < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)));
        writer.write('.');
        if (calendar.get(Calendar.MINUTE) < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(calendar.get(Calendar.MINUTE)));
        writer.write('.');
        if (calendar.get(Calendar.SECOND) < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(calendar.get(Calendar.SECOND)));
        writer.write('.');

        Helper.releaseCalendar(calendar);

        // Must truncate the nanos to six decimal places,
        // it is actually a complex algorithm...
        String nanoString = Integer.toString(timestamp.getNanos());
        int numberOfZeros = 0;
        for (int num = Math.min(9 - nanoString.length(), 6); num > 0; num--) {
            writer.write('0');
            numberOfZeros++;
        }
        if ((nanoString.length() + numberOfZeros) > 6) {
            nanoString = nanoString.substring(0, (6 - numberOfZeros));
        }
        writer.write(nanoString);
    }

    /**
     * Write a timestamp in DB2 specific format (yyyy-mm-dd-hh.mm.ss.ffffff).
     */
    protected void appendDB2Calendar(Calendar calendar, Writer writer) throws IOException {
        int hour;
        int minute;
        int second;
        if (!Helper.getDefaultTimeZone().equals(calendar.getTimeZone())) {
            // Must convert the calendar to the local timezone if different, as
            // dates have no timezone (always local).
            Calendar localCalendar = Helper.allocateCalendar();
            localCalendar.setTimeInMillis(calendar.getTimeInMillis());
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minute = calendar.get(Calendar.MINUTE);
            second = calendar.get(Calendar.SECOND);
            Helper.releaseCalendar(localCalendar);
        } else {
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minute = calendar.get(Calendar.MINUTE);
            second = calendar.get(Calendar.SECOND);
        }
        writer.write(Helper.printDate(calendar));
        writer.write('-');
        if (hour < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(hour));
        writer.write('.');
        if (minute < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(minute));
        writer.write('.');
        if (second < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(second));
        writer.write('.');

        // Must truncate the nanos to six decimal places,
        // it is actually a complex algorithm...
        String millisString = Integer.toString(calendar.get(Calendar.MILLISECOND));
        int numberOfZeros = 0;
        for (int num = Math.min(3 - millisString.length(), 3); num > 0; num--) {
            writer.write('0');
            numberOfZeros++;
        }
        if ((millisString.length() + numberOfZeros) > 3) {
            millisString = millisString.substring(0, (3 - numberOfZeros));
        }
        writer.write(millisString);
    }

    /**
     * INTERNAL:
     * Append the Time in Native format if usesNativeSQL() otherwise use ODBC
     * format from DAtabasePlatform. Native Format: 'hh:mm:ss'
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
     * INTERNAL:
     * Append the Timestamp in native format if usesNativeSQL() is true
     * otherwise use ODBC format from DatabasePlatform. Native format:
     * 'YYYY-MM-DD-hh.mm.ss.SSSSSS'
     */
    @Override
    protected void appendTimestamp(java.sql.Timestamp timestamp, Writer writer) throws IOException {
        if (usesNativeSQL()) {
            writer.write("'");
            appendDB2Timestamp(timestamp, writer);
            writer.write("'");
        } else {
            super.appendTimestamp(timestamp, writer);
        }
    }

    /**
     * INTERNAL:
     * Append the Timestamp in native format if usesNativeSQL() is true
     * otherwise use ODBC format from DatabasePlatform. Native format:
     * 'YYYY-MM-DD-hh.mm.ss.SSSSSS'
     */
    @Override
    protected void appendCalendar(Calendar calendar, Writer writer) throws IOException {
        if (usesNativeSQL()) {
            writer.write("'");
            appendDB2Calendar(calendar, writer);
            writer.write("'");
        } else {
            super.appendCalendar(calendar, writer);
        }
    }

    @Override
    protected Hashtable<Class<?>, FieldTypeDefinition> buildFieldTypes() {
        Hashtable<Class<?>, FieldTypeDefinition> fieldTypeMapping = new Hashtable<>();

        fieldTypeMapping.put(Boolean.class, new FieldTypeDefinition("SMALLINT DEFAULT 0", false));

        fieldTypeMapping.put(Integer.class, new FieldTypeDefinition("INTEGER", false));
        fieldTypeMapping.put(Long.class, new FieldTypeDefinition("BIGINT", false));
        fieldTypeMapping.put(Float.class, new FieldTypeDefinition("REAL", false));
        fieldTypeMapping.put(Double.class, new FieldTypeDefinition("FLOAT", false));
        fieldTypeMapping.put(Short.class, new FieldTypeDefinition("SMALLINT", false));
        fieldTypeMapping.put(Byte.class, new FieldTypeDefinition("SMALLINT", false));
        fieldTypeMapping.put(java.math.BigInteger.class, new FieldTypeDefinition("BIGINT", false));
        fieldTypeMapping.put(java.math.BigDecimal.class, new FieldTypeDefinition("DECIMAL", 15));
        fieldTypeMapping.put(Number.class, new FieldTypeDefinition("DECIMAL", 15));
        if(getUseNationalCharacterVaryingTypeForString()){
            fieldTypeMapping.put(String.class, new FieldTypeDefinition("VARCHAR", DEFAULT_VARCHAR_SIZE, "FOR MIXED DATA"));
        }else {
            fieldTypeMapping.put(String.class, new FieldTypeDefinition("VARCHAR", DEFAULT_VARCHAR_SIZE));
        }
        fieldTypeMapping.put(Character.class, new FieldTypeDefinition("CHAR", 1));
        fieldTypeMapping.put(Byte[].class, new FieldTypeDefinition("BLOB", 64000));
        fieldTypeMapping.put(Character[].class, new FieldTypeDefinition("CLOB", 64000));
        fieldTypeMapping.put(byte[].class, new FieldTypeDefinition("BLOB", 64000));
        fieldTypeMapping.put(char[].class, new FieldTypeDefinition("CLOB", 64000));
        fieldTypeMapping.put(java.sql.Blob.class, new FieldTypeDefinition("BLOB", 64000));
        fieldTypeMapping.put(java.sql.Clob.class, new FieldTypeDefinition("CLOB", 64000));

        fieldTypeMapping.put(java.sql.Date.class, new FieldTypeDefinition("DATE", false));
        fieldTypeMapping.put(java.sql.Time.class, new FieldTypeDefinition("TIME", false));
        fieldTypeMapping.put(java.sql.Timestamp.class, new FieldTypeDefinition("TIMESTAMP", false));

        fieldTypeMapping.put(java.time.LocalDate.class, new FieldTypeDefinition("DATE"));
        fieldTypeMapping.put(java.time.LocalDateTime.class, new FieldTypeDefinition("TIMESTAMP"));
        fieldTypeMapping.put(java.time.LocalTime.class, new FieldTypeDefinition("TIME"));
        fieldTypeMapping.put(java.time.OffsetDateTime.class, new FieldTypeDefinition("TIMESTAMP"));
        fieldTypeMapping.put(java.time.OffsetTime.class, new FieldTypeDefinition("TIMESTAMP"));

        return fieldTypeMapping;
    }

    /**
     * INTERNAL: returns the maximum number of characters that can be used in a
     * field name on this platform.
     */
    @Override
    public int getMaxFieldNameSize() {
        return 128;
    }

    /**
     * INTERNAL: returns the maximum number of characters that can be used in a
     * foreign key name on this platform.
     */
    @Override
    public int getMaxForeignKeyNameSize() {
        return 18;
    }

    /**
     * INTERNAL:
     * returns the maximum number of characters that can be used in a unique key
     * name on this platform.
     */
    @Override
    public int getMaxUniqueKeyNameSize() {
        return 18;
    }

    /**
     * INTERNAL:
     * Return the catalog information through using the native SQL catalog
     * selects. This is required because many JDBC driver do not support
     * meta-data. Wildcards can be passed as arguments.
     * This is currently not used.
     */
    public Vector getNativeTableInfo(String table, String creator, AbstractSession session) {
        String query = "SELECT * FROM SYSIBM.SYSTABLES WHERE TBCREATOR NOT IN ('SYS', 'SYSTEM')";
        if (table != null) {
            if (table.indexOf('%') != -1) {
                query = query + " AND TBNAME LIKE " + table;
            } else {
                query = query + " AND TBNAME = " + table;
            }
        }
        if (creator != null) {
            if (creator.indexOf('%') != -1) {
                query = query + " AND TBCREATOR LIKE " + creator;
            } else {
                query = query + " AND TBCREATOR = " + creator;
            }
        }
        return session.executeSelectingCall(new org.eclipse.persistence.queries.SQLCall(query));
    }

    /**
     * INTERNAL:
     * Used for sp calls.
     */
    @Override
    public String getProcedureCallHeader() {
        return "CALL ";
    }

    /**
     * INTERNAL:
     * Used for pessimistic locking in DB2.
     * Without the "WITH RS" the lock is not held.
     */
    // public String getSelectForUpdateString() { return " FOR UPDATE"; }
    @Override
    public String getSelectForUpdateString() {
        return " FOR READ ONLY WITH RS USE AND KEEP UPDATE LOCKS";
        //return " FOR READ ONLY WITH RR";
        //return " FOR READ ONLY WITH RS";
        //return " FOR UPDATE WITH RS";
    }

    /**
     * INTERNAL:
     * Used for stored procedure defs.
     */
    @Override
    public String getProcedureEndString() {
        return "END";
    }

    /**
     * Used for stored procedure defs.
     */
    @Override
    public String getProcedureBeginString() {
        return "BEGIN";
    }

    /**
     * INTERNAL:
     * Used for stored procedure defs.
     */
    @Override
    public String getProcedureAsString() {
        return "";
    }

    /**
     * Obtain the platform specific argument string
     */
    @Override
    public String getProcedureArgument(String name, Object parameter, ParameterType parameterType, StoredProcedureCall call, AbstractSession session) {
        if (name != null && shouldPrintStoredProcedureArgumentNameInCall()) {
            return getProcedureArgumentString() + name + " => " + "?";
        }
        return "?";
    }

    /**
     * INTERNAL:
     * This is required in the construction of the stored procedures with output
     * parameters.
     */
    @Override
    public boolean shouldPrintOutputTokenAtStart() {
        return true;
    }

    /**
     * Used to determine if the platform should perform partial parameter binding or not
     * Enabled for DB2 and DB2 for zOS to add support for partial binding
     */
    @Override
    public boolean shouldBindPartialParameters() {
        return this.shouldBindPartialParameters;
    }

    /**
     * INTERNAL:
     * This method returns the query to select the timestamp from the server for
     * DB2.
     */
    @Override
    public ValueReadQuery getTimestampQuery() {
        if (timestampQuery == null) {
            timestampQuery = new ValueReadQuery();
            timestampQuery.setSQLString("SELECT CURRENT TIMESTAMP FROM SYSIBM.SYSDUMMY1");
            timestampQuery.setAllowNativeSQLQuery(true);
        }
        return timestampQuery;
    }

    /**
     * INTERNAL:
     * Initialize any platform-specific operators
     */
    @Override
    protected void initializePlatformOperators() {
        super.initializePlatformOperators();

        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.ToUpperCase, "UCASE"));
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.ToLowerCase, "LCASE"));
        addOperator(count());
        addOperator(max());
        addOperator(min());
        addOperator(concatOperator());
        addOperator(caseOperator());
        addOperator(caseConditionOperator());
        addOperator(distinct());
        addOperator(ExpressionOperator.simpleTwoArgumentFunction(ExpressionOperator.Instring, "Locate"));
        // CR#2811076 some missing DB2 functions added.
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.ToNumber, "DECIMAL"));
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.ToChar, "CHAR"));
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.DateToString, "CHAR"));
        addOperator(ExpressionOperator.simpleFunction(ExpressionOperator.ToDate, "DATE"));

        addOperator(ascendingOperator());
        addOperator(descendingOperator());

        addOperator(trim2());
        addOperator(ltrim2Operator());
        addOperator(rtrim2Operator());

        addOperator(lengthOperator());
        addOperator(nullifOperator());
        addOperator(coalesceOperator());

        addOperator(roundOperator());
    }

    /**
     * Create an ExpressionOperator that disables all parameter binding
     */
    protected static ExpressionOperator disableAllBindingExpression() {
        return new ExpressionOperator() {
            @Override
            public void printDuo(Expression first, Expression second, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printDuo(first, second, printer);
                    return;
                }

                if(first.isParameterExpression()) {
                    ((ParameterExpression) first).setCanBind(false);
                } else if(first.isConstantExpression()) {
                    ((ConstantExpression) first).setCanBind(false);
                }
                if(second.isParameterExpression()) {
                    ((ParameterExpression) second).setCanBind(false);
                } else if(second.isConstantExpression()) {
                    ((ConstantExpression) second).setCanBind(false);
                }
                super.printDuo(first, second, printer);
            }

            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                for(Expression item : items) {
                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(false);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(false);
                    }
                }
                super.printCollection(items, printer);
            }

            @Override
            public void printJavaDuo(Expression first, Expression second, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaDuo(first, second, printer);
                    return;
                }

                if(first.isParameterExpression()) {
                    ((ParameterExpression) first).setCanBind(false);
                } else if(first.isConstantExpression()) {
                    ((ConstantExpression) first).setCanBind(false);
                }
                if(second.isParameterExpression()) {
                    ((ParameterExpression) second).setCanBind(false);
                } else if(second.isConstantExpression()) {
                    ((ConstantExpression) second).setCanBind(false);
                }
                super.printJavaDuo(first, second, printer);
            }

            @Override
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                for(Expression item : items) {
                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(false);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(false);
                    }
                }
                super.printJavaCollection(items, printer);
            }
        };
    }

    /**
     * Create an ExpressionOperator that requires at least 1 typed argument
     */
    protected static ExpressionOperator disableAtLeast1BindingExpression() {
        return new ExpressionOperator() {
            @Override
            public void printDuo(Expression first, Expression second, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printDuo(first, second, printer);
                    return;
                }

                boolean firstBound = true;
                if(second != null) {
                    boolean secondBound = true;

                    // If both are parameters and/or constants, we need to determine which should be bound
                    if(first.isValueExpression() && second.isValueExpression()) {
                        if(printer.getPlatform().shouldBindLiterals()) {
                            // If literal binding is enabled, we should make sure parameters are favored
                            if(first.isConstantExpression() && second.isParameterExpression()) {
                                firstBound = false;
                            } else {
                                secondBound = false;
                            }
                        } else {
                            // Otherwise, we default to favor the first argument
                            if(first.isParameterExpression() && second.isParameterExpression()) {
                                secondBound = false;
                            }
                        }
                    }

                    if(second.isParameterExpression()) {
                        ((ParameterExpression) second).setCanBind(secondBound);
                    } else if(second.isConstantExpression()) {
                        ((ConstantExpression) second).setCanBind(secondBound);
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
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                int[] indices = getArgumentIndices(items.size());
                boolean allBind = true;
                for (int i = 0; i < items.size(); i++) {
                    final int index = indices[i];
                    Expression item = items.get(index);
                    boolean shouldBind = true;

                    // If the item isn't a Constant/Parameter, this will suffice and the rest should bind
                    if(!item.isValueExpression()) {
                        allBind = false;
                    }

                    if(allBind) {
                        if(printer.getPlatform().shouldBindLiterals()) {
                            if((i == (indices.length - 1))) {
                                // The last parameter has to be disabled
                                shouldBind = allBind = false;
                            }
                        } else {
                            if(item.isConstantExpression()) {
                                // The first literal has to be disabled
                                shouldBind = allBind = false;
                            } else if((i == (indices.length - 1)) && item.isParameterExpression()) {
                                // The last parameter has to be disabled
                                shouldBind = allBind = false;
                            }
                        }
                    }

                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(shouldBind);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(shouldBind);
                    }
                }
                super.printCollection(items, printer);
            }

            @Override
            public void printJavaDuo(Expression first, Expression second, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaDuo(first, second, printer);
                    return;
                }

                boolean firstBound = true;
                if(second != null) {
                    boolean secondBound = true;

                    // If both are parameters and/or constants, we need to determine which should be bound
                    if(first.isValueExpression() && second.isValueExpression()) {
                        if(printer.getPlatform().shouldBindLiterals()) {
                            // If literal binding is enabled, we should make sure parameters are favored
                            if(first.isConstantExpression() && second.isParameterExpression()) {
                                firstBound = false;
                            } else {
                                secondBound = false;
                            }
                        } else {
                            // Otherwise, we default to favor the first argument
                            if(first.isParameterExpression() && second.isParameterExpression()) {
                                secondBound = false;
                            }
                        }
                    }

                    if(second.isParameterExpression()) {
                        ((ParameterExpression) second).setCanBind(secondBound);
                    } else if(second.isConstantExpression()) {
                        ((ConstantExpression) second).setCanBind(secondBound);
                    }
                }

                if(first.isParameterExpression()) {
                    ((ParameterExpression) first).setCanBind(firstBound);
                } else if(first.isConstantExpression()) {
                    ((ConstantExpression) first).setCanBind(firstBound);
                }
                super.printJavaDuo(first, second, printer);
            }

            @Override
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                boolean allBind = true;
                for (int i = 0; i < items.size(); i++) {
                    Expression item = items.get(i);

                    boolean shouldBind = true;

                    // If the item isn't a Constant/Parameter, this will suffice and the rest should bind
                    if(!item.isValueExpression()) {
                        allBind = false;
                    }

                    if(allBind) {
                        if(printer.getPlatform().shouldBindLiterals()) {
                            if((i == (items.size() - 1))) {
                                // The last parameter has to be disabled
                                shouldBind = allBind = false;
                            }
                        } else {
                            if(item.isConstantExpression()) {
                                // The first literal has to be disabled
                                shouldBind = allBind = false;
                            } else if((i == (items.size() - 1)) && item.isParameterExpression()) {
                                // The last parameter has to be disabled
                                shouldBind = allBind = false;
                            }
                        }
                    }

                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(shouldBind);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(shouldBind);
                    }
                }
                super.printJavaCollection(items, printer);
            }
        };
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X34: There is a ? parameter in the select list. This is not allowed.</pre>
     */
    protected ExpressionOperator ascendingOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.ascending().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X34: There is a ? parameter in the select list. This is not allowed.</pre>
     */
    protected ExpressionOperator descendingOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.descending().copyTo(operator);
        return operator;
    }

    /**
     * INTERNAL:
     * The concat operator is of the form .... VARCHAR ( &lt;operand1&gt; || &lt;operand2&gt; )
     */
    protected ExpressionOperator concatOperator() {
        ExpressionOperator operator = new ExpressionOperator();
        operator.setType(ExpressionOperator.FunctionOperator);
        operator.setSelector(ExpressionOperator.Concat);
        Vector<String> v = new Vector<>(5);
        v.add("VARCHAR(");
        v.add(" || ");
        v.add(")");
        operator.printsAs(v);
        operator.bePrefix();
        operator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 will throw an error:
     * <pre>Db2 cannot determine how to implicitly cast the arguments between string and 
     * numeric data types. DB2 SQL Error: SQLCODE=-245, SQLSTATE=428F5</pre>
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X36: The 'COUNT' operator is not allowed to take a ? parameter as an operand.</pre>
     */
    protected ExpressionOperator count() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.count().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 will throw an error:
     * <pre>Db2 cannot determine how to implicitly cast the arguments between string and 
     * numeric data types. DB2 SQL Error: SQLCODE=-245, SQLSTATE=428F5</pre>
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X36: The 'MAX' operator is not allowed to take a ? parameter as an operand.</pre>
     */
    protected ExpressionOperator max() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.maximum().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 will throw an error:
     * <pre>Db2 cannot determine how to implicitly cast the arguments between string and 
     * numeric data types. DB2 SQL Error: SQLCODE=-245, SQLSTATE=428F5</pre>
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X36: The 'MIN' operator is not allowed to take a ? parameter as an operand.</pre>
     */
    protected ExpressionOperator min() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.minimum().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 will throw an error:
     * <pre>Db2 cannot determine how to implicitly cast the arguments between string and 
     * numeric data types. DB2 SQL Error: SQLCODE=-245, SQLSTATE=428F5</pre>
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X34: There is a ? parameter in the select list.  This is not allowed.</pre>
     */
    protected ExpressionOperator distinct() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.distinct().copyTo(operator);
        return operator;
    }

    /**
     * DB2 does not allow untyped parameter binding for the THEN &amp; ELSE 'result-expressions' of CASE expressions
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <b>Examples of places where parameter markers cannot be used:</b>
     * <ul>
     * <li>In a result-expression in any CASE expression when all the other result-expressions are either NULL or untyped parameter markers
     * </ul>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X87: At least one result expression (THEN or ELSE) of the CASE expression must have a known type.</pre>
     */
    protected ExpressionOperator caseOperator() {
        ListExpressionOperator operator = new ListExpressionOperator() {
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                // First, calculate all argument binding positions
                int i = 0;
                int numberOfItems = items.size();
                boolean[] argumentBinding = new boolean[numberOfItems + 1];

                // Enabled for CASE operator
                argumentBinding[i] = true;
                i++;

                // Enabled for WHEN, but not for THEN
                boolean[] separatorsBinding = new boolean[]{true, false};
                // Disable for ELSE, but not for END
                boolean[] terminationStringsBinding = new boolean[]{false, true};
                while (i < numberOfItems - (terminationStringsBinding.length - 1)) {
                    for (int j = 0; j < separatorsBinding.length; j++) {
                        argumentBinding[i] = separatorsBinding[j];
                        i++;
                    }
                }
                while (i <= numberOfItems) {
                    for (int j = 0; j < terminationStringsBinding.length; j++) {
                        argumentBinding[i] = terminationStringsBinding[j];
                        i++;
                    }
                }

                int[] indices = getArgumentIndices(items.size());
                for (int j = 0; j < items.size(); j++) {
                    final int index = indices[j];
                    Expression item = items.get(index);

                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(argumentBinding[index]);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(argumentBinding[index]);
                    }
                }
                super.printCollection(items, printer);
            }

            @Override
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                // First, calculate all argument binding positions
                int i = 0;
                int numberOfItems = items.size();
                boolean[] argumentBinding = new boolean[numberOfItems + 1];

                // Enabled for CASE operator
                argumentBinding[i] = true;
                i++;

                // Enabled for WHEN, but not for THEN
                boolean[] separatorsBinding = new boolean[]{true, false};
                // Disable for ELSE, but not for END
                boolean[] terminationStringsBinding = new boolean[]{false, true};
                while (i < numberOfItems - (terminationStringsBinding.length - 1)) {
                    for (int j = 0; j < separatorsBinding.length; j++) {
                        argumentBinding[i] = separatorsBinding[j];
                        i++;
                    }
                }
                while (i <= numberOfItems) {
                    for (int j = 0; j < terminationStringsBinding.length; j++) {
                        argumentBinding[i] = terminationStringsBinding[j];
                        i++;
                    }
                }

                for (int j = 0; j < items.size(); j++) {
                    Expression item = items.get(j);

                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(argumentBinding[j]);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(argumentBinding[j]);
                    }
                }
                super.printJavaCollection(items, printer);
            }
        };
        ExpressionOperator.caseStatement().copyTo(operator); 
        return operator;
    }

    /**
     * DB2 does not allow untyped parameter binding for the THEN &amp; ELSE 'result-expressions' of CASE expressions
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <b>Examples of places where parameter markers cannot be used:</b>
     * <ul>
     * <li>In a result-expression in any CASE expression when all the other result-expressions are either NULL or untyped parameter markers
     * </ul>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X87: At least one result expression (THEN or ELSE) of the CASE expression must have a known type.</pre>
     */
    protected ExpressionOperator caseConditionOperator() {
        ListExpressionOperator operator = new ListExpressionOperator() {
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                // First, calculate all argument binding positions
                int i = 0;
                int numberOfItems = items.size();
                boolean[] argumentBinding = new boolean[numberOfItems + 1];

                // Enabled for CASE WHEN operator
                argumentBinding[i] = true;
                i++;
                // Disabled for THEN operator
                argumentBinding[i] = false;
                i++;

                // Enabled for WHEN, but not for THEN
                boolean[] separatorsBinding = new boolean[]{true, false};
                // Disable for ELSE, but not for END
                boolean[] terminationStringsBinding = new boolean[]{false, true};
                while (i < numberOfItems - (terminationStringsBinding.length - 1)) {
                    for (int j = 0; j < separatorsBinding.length; j++) {
                        argumentBinding[i] = separatorsBinding[j];
                        i++;
                    }
                }
                while (i <= numberOfItems) {
                    for (int j = 0; j < terminationStringsBinding.length; j++) {
                        argumentBinding[i] = terminationStringsBinding[j];
                        i++;
                    }
                }

                int[] indices = getArgumentIndices(items.size());
                for (int j = 0; j < items.size(); j++) {
                    final int index = indices[j];
                    Expression item = items.get(index);

                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(argumentBinding[index]);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(argumentBinding[index]);
                    }
                }
                super.printCollection(items, printer);
            }

            @Override
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                // First, calculate all argument binding positions
                int i = 0;
                int numberOfItems = items.size();
                boolean[] argumentBinding = new boolean[numberOfItems + 1];

                // Enabled for CASE WHEN operator
                argumentBinding[i] = true;
                i++;
                // Disabled for THEN operator
                argumentBinding[i] = false;
                i++;

                // Enabled for WHEN, but not for THEN
                boolean[] separatorsBinding = new boolean[]{true, false};
                // Disable for ELSE, but not for END
                boolean[] terminationStringsBinding = new boolean[]{false, true};
                while (i < numberOfItems - (terminationStringsBinding.length - 1)) {
                    for (int j = 0; j < separatorsBinding.length; j++) {
                        argumentBinding[i] = separatorsBinding[j];
                        i++;
                    }
                }
                while (i <= numberOfItems) {
                    for (int j = 0; j < terminationStringsBinding.length; j++) {
                        argumentBinding[i] = terminationStringsBinding[j];
                        i++;
                    }
                }

                for (int j = 0; j < items.size(); j++) {
                    Expression item = items.get(j);

                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(argumentBinding[j]);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(argumentBinding[j]);
                    }
                }
                super.printJavaCollection(items, printer);
            }
        };
        ExpressionOperator.caseConditionStatement().copyTo(operator);
        return operator;
    }

    /**
     * Disable binding support.
     * <p>
     * With binding enabled, DB2 will throw an error:
     * <pre>Db2 cannot determine how to implicitly cast the arguments between string and 
     * numeric data types. DB2 SQL Error: SQLCODE=-245, SQLSTATE=428F5</pre>
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X36: The 'length' operator is not allowed to take a ? parameter as an operand.</pre>
     */
    protected ExpressionOperator lengthOperator() {
        ExpressionOperator operator = disableAllBindingExpression();
        ExpressionOperator.length().copyTo(operator);
        return operator;
    }

    /**
     * DB2 requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42X35: It is not allowed for both operands of '=' to be ? parameters.</pre>
     */
    protected ExpressionOperator nullifOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.nullIf().copyTo(operator);
        return operator;
    }

    /**
     * DB2 requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * With binding enabled, DB2 z/OS will throw an error:
     * <pre>The statement string specified as the object of a PREPARE contains a 
     * predicate or expression where parameter markers have been used as operands of 
     * the same operatorfor example: ? &gt; ?. DB2 SQL Error: SQLCODE=-417, SQLSTATE=42609</pre>
     * <p>
     * With binding enabled, Derby will throw an error:
     * <pre>ERROR 42610: All the arguments to the COALESCE/VALUE function cannot be parameters. The function needs at least one argument that is not a parameter.</pre>
     */
    protected ExpressionOperator coalesceOperator() {
        ListExpressionOperator operator = new ListExpressionOperator() {
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                int[] indices = getArgumentIndices(items.size());
                boolean allBind = true;
                for (int i = 0; i < items.size(); i++) {
                    final int index = indices[i];
                    Expression item = items.get(index);
                    boolean shouldBind = true;

                    // If the item isn't a Constant/Parameter, this will suffice and the rest should bind
                    if(!item.isValueExpression()) {
                        allBind = false;
                    }

                    if(allBind) {
                        if(printer.getPlatform().shouldBindLiterals()) {
                            if((i == (indices.length - 1))) {
                                // The last parameter has to be disabled
                                shouldBind = allBind = false;
                            }
                        } else {
                            if(item.isConstantExpression()) {
                                // The first literal has to be disabled
                                shouldBind = allBind = false;
                            } else if((i == (indices.length - 1)) && item.isParameterExpression()) {
                                // The last parameter has to be disabled
                                shouldBind = allBind = false;
                            }
                        }
                    }

                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(shouldBind);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(shouldBind);
                    }
                }
                super.printCollection(items, printer);
            }

            @Override
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                boolean allBind = true;
                for (int i = 0; i < items.size(); i++) {
                    Expression item = items.get(i);

                    boolean shouldBind = true;

                    // If the item isn't a Constant/Parameter, this will suffice and the rest should bind
                    if(!item.isValueExpression()) {
                        allBind = false;
                    }

                    if(allBind) {
                        if(printer.getPlatform().shouldBindLiterals()) {
                            if((i == (items.size() - 1))) {
                                // The last parameter has to be disabled
                                shouldBind = allBind = false;
                            }
                        } else {
                            if(item.isConstantExpression()) {
                                // The first literal has to be disabled
                                shouldBind = allBind = false;
                            } else if((i == (items.size() - 1)) && item.isParameterExpression()) {
                                // The last parameter has to be disabled
                                shouldBind = allBind = false;
                            }
                        }
                    }

                    if(item.isParameterExpression()) {
                        ((ParameterExpression) item).setCanBind(shouldBind);
                    } else if(item.isConstantExpression()) {
                        ((ConstantExpression) item).setCanBind(shouldBind);
                    }
                }
                super.printJavaCollection(items, printer);
            }
        };
        ExpressionOperator.coalesce().copyTo(operator);
        return operator;
    }

    /**
     * DB2 does not support untyped parameter binding for &lt;operand2&gt;
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator trim2() {
        ExpressionOperator operator = new ExpressionOperator(){
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                int[] indices = getArgumentIndices(items.size());
                for (int i = 0; i < items.size(); i++) {
                    final int index = indices[i];
                    Expression item = items.get(index);

                    // Disable the first item, which should be <operand2> for this operator
                    if(i == 0) {
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
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                for (int i = 0; i < items.size(); i++) {
                    Expression item = items.get(i);

                    // Disable the first item, which should be <operand2> for this operator
                    if(i == 0) {
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

        operator.setType(ExpressionOperator.FunctionOperator);
        operator.setSelector(ExpressionOperator.Trim2);
        List<String> v = new ArrayList<>(3);
        v.add("TRIM(");
        v.add(" FROM ");
        v.add(")");
        operator.printsAs(v);
        operator.bePrefix();

        // Bug 573094
        int[] indices = { 1, 0 };
        operator.setArgumentIndices(indices);

        operator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return operator;
    }

    /**
     * DB2 does not support untyped parameter binding for &lt;operand2&gt;
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator ltrim2Operator() {
        ExpressionOperator operator = new ExpressionOperator(){
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                int[] indices = getArgumentIndices(items.size());
                for (int i = 0; i < items.size(); i++) {
                    final int index = indices[i];
                    Expression item = items.get(index);

                    // Disable the first item, which should be <operand2> for this operator
                    if(i == 0) {
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
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                for (int i = 0; i < items.size(); i++) {
                    Expression item = items.get(i);

                    // Disable the first item, which should be <operand2> for this operator
                    if(i == 0) {
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

        operator.setType(ExpressionOperator.FunctionOperator);
        operator.setSelector(ExpressionOperator.LeftTrim2);
        Vector<String> v = new Vector<>(5);
        v.add("TRIM(LEADING ");
        v.add(" FROM ");
        v.add(")");
        operator.printsAs(v);
        operator.bePrefix();

        // Bug 573094
        int[] indices = { 1, 0 };
        operator.setArgumentIndices(indices);

        operator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return operator;
    }

    /**
     * DB2 does not support untyped parameter binding for &lt;operand2&gt;
     * <p>
     * With binding enabled, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     */
    protected ExpressionOperator rtrim2Operator() {
        ExpressionOperator operator = new ExpressionOperator(){
            @Override
            public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printCollection(items, printer);
                    return;
                }

                int[] indices = getArgumentIndices(items.size());
                for (int i = 0; i < items.size(); i++) {
                    final int index = indices[i];
                    Expression item = items.get(index);

                    // Disable the first item, which should be <operand2> for this operator
                    if(i == 0) {
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
            public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
                if(!printer.getPlatform().shouldBindPartialParameters()) {
                    super.printJavaCollection(items, printer);
                    return;
                }

                for (int i = 0; i < items.size(); i++) {
                    Expression item = items.get(i);

                    // Disable the first item, which should be <operand2> for this operator
                    if(i == 0) {
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

        operator.setType(ExpressionOperator.FunctionOperator);
        operator.setSelector(ExpressionOperator.RightTrim2);
        Vector<String> v = new Vector<>(5);
        v.add("TRIM(TRAILING ");
        v.add(" FROM ");
        v.add(")");
        operator.printsAs(v);
        operator.bePrefix();

        // Bug 573094
        int[] indices = { 1, 0 };
        operator.setArgumentIndices(indices);

        operator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return operator;
    }

    /**
     * DB2 requires that at least one argument be a known type
     * <p>
     * With binding enabled, DB2 will throw an error:
     * <pre>Db2 cannot determine how to implicitly cast the arguments between string and 
     * numeric data types. DB2 SQL Error: SQLCODE=-245, SQLSTATE=428F5</pre>
     */
    protected ExpressionOperator roundOperator() {
        ExpressionOperator operator = disableAtLeast1BindingExpression();
        ExpressionOperator.round().copyTo(operator);
        return operator;
    }

    @Override
    public boolean isDB2() {
        return true;
    }

    /**
     * INTERNAL:
     * Builds a table of maximum numeric values keyed on java class. This is
     * used for type testing but might also be useful to end users attempting to
     * sanitize values.
     * <p>
     * <b>NOTE</b>: BigInteger {@literal &} BigDecimal maximums are dependent upon their
     * precision {@literal &} Scale
     */
    @Override
    public Hashtable<Class<? extends Number>, ? super Number> maximumNumericValues() {
        Hashtable<Class<? extends Number>, ? super Number> values = new Hashtable<>();

        values.put(Integer.class, Integer.MAX_VALUE);
        values.put(Long.class, (long) Integer.MAX_VALUE);
        values.put(Float.class, 123456789F);
        values.put(Double.class, (double) Float.MAX_VALUE);
        values.put(Short.class, Short.MAX_VALUE);
        values.put(Byte.class, Byte.MAX_VALUE);
        values.put(java.math.BigInteger.class, new java.math.BigInteger("999999999999999"));
        values.put(java.math.BigDecimal.class, new java.math.BigDecimal("0.999999999999999"));
        return values;
    }

    /**
     * INTERNAL:
     * Builds a table of minimum numeric values keyed on java class. This is
     * used for type testing but might also be useful to end users attempting to
     * sanitize values.
     * <p>
     * <b>NOTE</b>: BigInteger {@literal &} BigDecimal minimums are dependent upon their
     * precision {@literal &} Scale
     */
    @Override
    public Hashtable<Class<? extends Number>, ? super Number> minimumNumericValues() {
        Hashtable<Class<? extends Number>, ? super Number> values = new Hashtable<>();

        values.put(Integer.class, Integer.MIN_VALUE);
        values.put(Long.class, (long) Integer.MIN_VALUE);
        values.put(Float.class, (float) -123456789);
        values.put(Double.class, (double) Float.MIN_VALUE);
        values.put(Short.class, Short.MIN_VALUE);
        values.put(Byte.class, Byte.MIN_VALUE);
        values.put(java.math.BigInteger.class, new java.math.BigInteger("-999999999999999"));
        values.put(java.math.BigDecimal.class, new java.math.BigDecimal("-0.999999999999999"));
        return values;
    }

    /**
     * INTERNAL:
     * Allow for the platform to ignore exceptions. This is required for DB2
     * which throws no-data modified as an exception.
     */
    @Override
    public boolean shouldIgnoreException(SQLException exception) {
        if (exception.getMessage().equals("No data found") || exception.getMessage().equals("No row was found for FETCH, UPDATE or DELETE; or the result of a query is an empty table")
                || (exception.getErrorCode() == 100)) {
            return true;
        }
        return super.shouldIgnoreException(exception);
    }

    /**
     * INTERNAL:
     * JDBC defines and outer join syntax, many drivers do not support this. So
     * we normally avoid it.
     */
    @Override
    public boolean shouldUseJDBCOuterJoinSyntax() {
        return false;
    }

    /**
     * INTERNAL: Build the identity query for native sequencing.
     */
    @Override
    public ValueReadQuery buildSelectQueryForIdentity() {
        ValueReadQuery selectQuery = new ValueReadQuery();
        StringWriter writer = new StringWriter();
        writer.write("SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1");

        selectQuery.setSQLString(writer.toString());
        return selectQuery;
    }

    /**
     * INTERNAL: Append the receiver's field 'identity' constraint clause to a
     * writer.
     * Used by table creation with sequencing.
     */
    @Override
    public void printFieldIdentityClause(Writer writer) throws ValidationException {
        try {
            writer.write(" GENERATED ALWAYS AS IDENTITY");
        } catch (IOException ioException) {
            throw ValidationException.fileError(ioException);
        }
    }

    @Override
    protected void printFieldTypeSize(Writer writer, FieldDefinition field, FieldTypeDefinition ftd) throws IOException {
        super.printFieldTypeSize(writer, field, ftd);
        String suffix = ftd.getTypesuffix();
        if (suffix != null) {
            writer.append(" " + suffix);
        }
    }

    /**
     * INTERNAL: Indicates whether the platform supports identity. DB2 does
     * through AS IDENTITY field types.
     * This is used by sequencing.
     */
    @Override
    public boolean supportsIdentity() {
        return true;
    }

    /**
     * INTERNAL: DB2 supports temp tables.
     * This is used by UpdateAllQuerys.
     */
    @Override
    public boolean supportsGlobalTempTables() {
        return true;
    }

    /**
     * INTERNAL: DB2 temp table syntax.
     * This is used by UpdateAllQuerys.
     */
    @Override
    protected String getCreateTempTableSqlPrefix() {
        return "DECLARE GLOBAL TEMPORARY TABLE ";
    }

    /**
     * INTERNAL: DB2 temp table syntax.
     * This is used by UpdateAllQuerys.
     */
    @Override
    public DatabaseTable getTempTableForTable(DatabaseTable table) {
        DatabaseTable tempTable = super.getTempTableForTable(table);
        tempTable.setTableQualifier("session");
        return tempTable;
    }

    /**
     * INTERNAL: DB2 temp table syntax.
     * This is used by UpdateAllQuerys.
     */
    @Override
    protected String getCreateTempTableSqlSuffix() {
        return " ON COMMIT DELETE ROWS NOT LOGGED";
    }

    /**
     * INTERNAL: DB2 allows LIKE to be used to create temp tables, which avoids having to know the types.
     * This is used by UpdateAllQuerys.
     */
    @Override
    protected String getCreateTempTableSqlBodyForTable(DatabaseTable table) {
        return " LIKE " + table.getQualifiedNameDelimited(this);
    }

    /**
     * INTERNAL: DB2 does not support NOWAIT.
     */
    @Override
    public String getNoWaitString() {
        return "";
    }

    /**
     * INTERNAL: DB2 has issues with binding with temp table queries.
     * This is used by UpdateAllQuerys.
     */
    @Override
    public boolean dontBindUpdateAllQueryUsingTempTables() {
        return true;
    }

    /**
     * INTERNAL: DB2 does not allow NULL in select clause.
     * This is used by UpdateAllQuerys.
     */
    @Override
    public boolean isNullAllowedInSelectClause() {
        return false;
    }

    /**
     * INTERNAL
     * DB2 has some issues with using parameters on certain functions and relations.
     * This allows statements to disable binding only in these cases.
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
     * INTERNAL: DB2 does not allow stand alone, untyped parameter markers in select clause.
     * @see org.eclipse.persistence.internal.expressions.ConstantExpression#writeFields(ExpressionSQLPrinter, List, SQLSelectStatement)
     * @see org.eclipse.persistence.internal.expressions.ParameterExpression#writeFields(ExpressionSQLPrinter, List, SQLSelectStatement)
     */
    @Override
    public boolean allowBindingForSelectClause() {
        return false;
    }

    /**
     * INTERNAL:
     * DB2 requires casting on certain operations, such as the CONCAT function,
     * and parameterized queries of the form, ":param = :param". This method
     * will write CAST operation to parameters if the type is known.
     * This is not used by default, only if isCastRequired is set to true,
     * by default dynamic SQL is used to avoid the issue in only the required cases.
     */
    @Override
    public void writeParameterMarker(Writer writer, ParameterExpression parameter, AbstractRecord record, DatabaseCall call) throws IOException {
        String paramaterMarker = "?";
        Object type = parameter.getType();
        // Update-all query requires casting of null parameter values in select into.
        if ((type != null) && (this.isCastRequired || ((call.getQuery() != null) && call.getQuery().isUpdateAllQuery()))) {
            BasicTypeHelperImpl typeHelper = BasicTypeHelperImpl.getInstance();
            String castType = null;
            if (typeHelper.isBooleanType(type) || typeHelper.isByteType(type) || typeHelper.isShortType(type)) {
                castType = "SMALLINT";
            } else if (typeHelper.isIntType(type)) {
                castType = "INTEGER";
            } else if (typeHelper.isLongType(type)) {
                castType = "BIGINT";
            } else if (typeHelper.isFloatType(type)) {
                castType = "REAL";
            } else if (typeHelper.isDoubleType(type)) {
                castType = "DOUBLE";
            } else if (typeHelper.isStringType(type)) {
                castType = "VARCHAR(" + getCastSizeForVarcharParameter() + ")";
            } else if (typeHelper.isCharacterType(type)) {
                castType = "CHAR";
            }

            if (castType != null) {
                paramaterMarker = "CAST (? AS " + castType + ")";
            }
        }
        writer.write(paramaterMarker);
    }

    /**
     * INTERNAL:
     * DB2 does not seem to allow FOR UPDATE on queries with multiple tables.
     * This is only used by testing to exclude these tests.
     */
    @Override
    public boolean supportsLockingQueriesWithMultipleTables() {
        return false;
    }

    /**
     * INTERNAL: DB2 added SEQUENCE support as of (I believe) v8.
     */
    @Override
    public ValueReadQuery buildSelectQueryForSequenceObject(String qualifiedSeqName, Integer size) {
        return new ValueReadQuery("VALUES(NEXT VALUE FOR " + qualifiedSeqName + ")");
    }

    /**
     * INTERNAL: DB2 added SEQUENCE support as of (I believe) v8.
     */
    @Override
    public boolean supportsSequenceObjects() {
        return true;
    }

    /**
     * DB2 disables single parameter usage in ORDER BY clause.
     * <p>
     * If a parameter marker is used, DB2 &amp; DB2 z/OS will throw an error:
     * <pre>The statement cannot be executed because a parameter marker has been used 
     * in an invalid way. DB2 SQL Error: SQLCODE=-418, SQLSTATE=42610</pre>
     * <p>
     * If a parameter marker is used, Derby will throw an error:
     * <pre>ERROR 42X34: There is a ? parameter in the select list.  This is not allowed.</pre>
     */
    @Override
    public boolean supportsOrderByParameters() {
        return false;
    }

    /**
     * INTERNAL: DB2 added SEQUENCE support as of (I believe) v8.
     */
    @Override
    public boolean isAlterSequenceObjectSupported() {
        return true;
    }

    @Override
    public boolean shouldPrintForUpdateClause() {
        return false;
    }
    /**
     * INTERNAL:
     * Print the SQL representation of the statement on a stream, storing the fields
     * in the DatabaseCall.  This implementation works MaxRows and FirstResult into the SQL using
     * DB2's ROWNUMBER() OVER() to filter values if shouldUseRownumFiltering is true.
     */
    @Override
    public void printSQLSelectStatement(DatabaseCall call, ExpressionSQLPrinter printer, SQLSelectStatement statement){
        int max = 0;
        int firstRow = 0;

        if (statement.getQuery()!=null){
            max = statement.getQuery().getMaxRows();
            firstRow = statement.getQuery().getFirstResult();
        }

        if ( !(this.shouldUseRownumFiltering()) || ( !(max>0) && !(firstRow>0) ) ){
            super.printSQLSelectStatement(call, printer, statement);
            statement.appendForUpdateClause(printer);
            return;
        } else if ( max > 0 ){
            statement.setUseUniqueFieldAliases(true);
            printer.printString("SELECT * FROM (SELECT * FROM (SELECT ");
            printer.printString("EL_TEMP.*, ROWNUMBER() OVER() AS EL_ROWNM FROM (");
            call.setFields(statement.printSQL(printer));
            printer.printString(") AS EL_TEMP) AS EL_TEMP2 WHERE EL_ROWNM <= ");
            printer.printParameter(DatabaseCall.MAXROW_FIELD);
            printer.printString(") AS EL_TEMP3 WHERE EL_ROWNM > ");
            printer.printParameter(DatabaseCall.FIRSTRESULT_FIELD);
            // If we have a ForUpdate clause, it must be on the outermost query
             statement.appendForUpdateClause(printer);
        } else {// firstRow>0
            statement.setUseUniqueFieldAliases(true);
            printer.printString("SELECT * FROM (SELECT EL_TEMP.*, ROWNUMBER() OVER() AS EL_ROWNM FROM (");
            call.setFields(statement.printSQL(printer));
            printer.printString(") AS EL_TEMP) AS EL_TEMP2 WHERE EL_ROWNM > ");
            printer.printParameter(DatabaseCall.FIRSTRESULT_FIELD);
            statement.appendForUpdateClause(printer);
        }
        call.setIgnoreFirstRowSetting(true);
        call.setIgnoreMaxResultsSetting(true);
    }

}
