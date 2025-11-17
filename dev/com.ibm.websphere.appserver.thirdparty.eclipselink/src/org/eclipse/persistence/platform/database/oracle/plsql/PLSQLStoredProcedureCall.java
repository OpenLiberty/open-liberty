/*
 * Copyright (c) 1998, 2023 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2022 IBM Corporation. All rights reserved.
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

package org.eclipse.persistence.platform.database.oracle.plsql;

//javase imports
import static java.lang.Integer.MIN_VALUE;
import static org.eclipse.persistence.internal.helper.DatabaseType.DatabaseTypeHelper.databaseTypeHelper;
import static org.eclipse.persistence.internal.helper.Helper.INDENT;
import static org.eclipse.persistence.internal.helper.Helper.NL;
import static org.eclipse.persistence.platform.database.jdbc.JDBCTypes.getDatabaseTypeForCode;
import static org.eclipse.persistence.platform.database.oracle.plsql.OraclePLSQLType.PLSQLBoolean_IN_CONV;
import static org.eclipse.persistence.platform.database.oracle.plsql.OraclePLSQLType.PLSQLBoolean_OUT_CONV;
import static org.eclipse.persistence.platform.database.oracle.plsql.OraclePLSQLTypes.PLSQLBoolean;
import static org.eclipse.persistence.platform.database.oracle.plsql.OraclePLSQLTypes.XMLType;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

// EclipseLink imports
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.internal.databaseaccess.Accessor;
import org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor;
import org.eclipse.persistence.internal.helper.ComplexDatabaseType;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseType;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.structures.ObjectRelationalDatabaseField;
import org.eclipse.persistence.platform.database.DatabasePlatform;
import org.eclipse.persistence.platform.database.jdbc.JDBCTypes;
import org.eclipse.persistence.platform.database.oracle.jdbc.OracleArrayType;
import org.eclipse.persistence.queries.StoredProcedureCall;
import org.eclipse.persistence.sessions.DatabaseRecord;
/**
 * <b>Purpose</b>:
 * Generates an Anonymous PL/SQL block to invoke the specified Stored Procedure
 * with arguments that may or may not have JDBC equivalents.
 * This handles conversion of PLSQL Record and Table types into SQL ARRAY (VARRAY) and STRUCT (OBJECT TYPE).
 * It also handles conversion of flat PLSQL Record types and PLSQL BOOLEAN and other basic types.
 */
@SuppressWarnings("unchecked")
public class PLSQLStoredProcedureCall extends StoredProcedureCall {

    // can't use Helper.cr(), Oracle PL/SQL parser only likes Unix-style newlines '\n'
    final static String BEGIN_DECLARE_BLOCK = NL + "DECLARE" + NL;
    final static String BEGIN_BEGIN_BLOCK = "BEGIN" + NL;
    final static String END_BEGIN_BLOCK = "END;";
    final static String PL2SQL_PREFIX = "EL_PL2SQL_";
    final static String SQL2PL_PREFIX = "EL_SQL2PL_";
    final static String BEGIN_DECLARE_FUNCTION = "FUNCTION ";
    final static String RTURN = "RETURN ";

    /**
     * List of procedure IN/OUT/INOUT arguments.
     */
    protected List<PLSQLargument> arguments = new ArrayList<PLSQLargument>();

    /**
     * Keeps track of the next procedure argument index.
     */
    protected int originalIndex = 0;
    /**
     * Translation row stored after translation on the call clone, used only for logging.
     */
    protected AbstractRecord translationRow;
    /**
     * Map of conversion function routines for converting complex PLSQL types.
     */
    protected Map<String, TypeInfo> typesInfo;
    /**
     * Id used to generate unique local functions.
     */
    protected int functionId = 0;

    public PLSQLStoredProcedureCall() {
        super();
        setIsCallableStatementRequired(true);
    }

    /**
     * PUBLIC:
     * Add a named IN argument to the stored procedure. The databaseType parameter classifies the
     * parameter (JDBCType vs. OraclePLSQLType, simple vs. complex)
     */
    public void addNamedArgument(String procedureParameterName, DatabaseType databaseType) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.IN, dt));
    }

    /**
     * PUBLIC:
     * Add a named IN argument to the stored procedure. The databaseType parameter classifies the
     * parameter (JDBCType vs. OraclePLSQLType, simple vs. complex). The extra length parameter
     * indicates that this parameter, when used in an Anonymous PL/SQL block, requires a length.
     */
    public void addNamedArgument(String procedureParameterName, DatabaseType databaseType,
        int length) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.IN, dt, length));
    }

    /**
     * PUBLIC:
     * Add a named IN argument to the stored procedure. The databaseType parameter classifies the
     * parameter (JDBCType vs. OraclePLSQLType, simple vs. complex). The extra scale and precision
     * parameters indicates that this parameter, when used in an Anonymous PL/SQL block, requires
     * scale and precision specification
     */
    public void addNamedArgument(String procedureParameterName, DatabaseType databaseType,
        int precision, int scale) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.IN, dt, precision, scale));
    }

    @Override
    public void addNamedArgument(String procedureParameterName, String argumentFieldName, int type) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.IN,
            getDatabaseTypeForCode(type))); // figure out databaseType from the sqlType
    }

    @Override
    public void addNamedArgument(String procedureParameterName, String argumentFieldName, int type,
        String typeName) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.IN,
            getDatabaseTypeForCode(type)));
    }

    /**
     * PUBLIC: Add a named IN OUT argument to the stored procedure. The databaseType parameter
     * classifies the parameter (JDBCType vs. OraclePLSQLType, simple vs. complex)
     */
    public void addNamedInOutputArgument(String procedureParameterName, DatabaseType databaseType) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.INOUT, dt));
    }

    /**
     * PUBLIC: Add a named IN OUT argument to the stored procedure. The databaseType parameter
     * classifies the parameter (JDBCType vs. OraclePLSQLType, simple vs. complex). The extra length
     * parameter indicates that this parameter, when used in an Anonymous PL/SQL block, requires a
     * length.
     */
    public void addNamedInOutputArgument(String procedureParameterName, DatabaseType databaseType,
        int length) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.INOUT, dt, length));
    }

    /**
     * PUBLIC: Add a named IN OUT argument to the stored procedure. The databaseType parameter
     * classifies the parameter (JDBCType vs. OraclePLSQLType, simple vs. complex). The extra scale
     * and precision parameters indicates that this parameter, when used in an Anonymous PL/SQL
     * block, requires scale and precision specification
     */
    public void addNamedInOutputArgument(String procedureParameterName, DatabaseType databaseType,
        int precision, int scale) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.INOUT, dt,
            precision, scale));
    }

    @Override
    public void addNamedInOutputArgument(String procedureParameterName, String inArgumentFieldName,
        String outArgumentFieldName, int type) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.INOUT,
            getDatabaseTypeForCode(type)));
    }

    @Override
    public void addNamedInOutputArgument(String procedureParameterName, String inArgumentFieldName,
        String outArgumentFieldName, int type, String typeName) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.INOUT,
            getDatabaseTypeForCode(type)));
    }

    @Override
    public void addNamedInOutputArgument(String procedureParameterName, String inArgumentFieldName,
        String outArgumentFieldName, int type, String typeName, Class classType) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.INOUT,
            getDatabaseTypeForCode(type)));
    }

    @Override
    public void addNamedInOutputArgument(String procedureParameterName, String inArgumentFieldName,
        String outArgumentFieldName, int type, String typeName, Class javaType,
        DatabaseField nestedType) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.INOUT,
            getDatabaseTypeForCode(type)));
    }

    /**
     * PUBLIC: Add a named OUT argument to the stored procedure. The databaseType parameter
     * classifies the parameter (JDBCType vs. OraclePLSQLType, simple vs. complex)
     */
    public void addNamedOutputArgument(String procedureParameterName, DatabaseType databaseType) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.OUT, dt));
    }

    /**
     * PUBLIC: Add a named OUT argument to the stored procedure. The databaseType parameter
     * classifies the parameter (JDBCType vs. OraclePLSQLType, simple vs. complex). The extra length
     * parameter indicates that this parameter, when used in an Anonymous PL/SQL block, requires a
     * length.
     */
    public void addNamedOutputArgument(String procedureParameterName, DatabaseType databaseType,
        int length) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.OUT, dt, length));
    }

    /**
     * PUBLIC: Add a named OUT argument to the stored procedure. The databaseType parameter
     * classifies the parameter (JDBCType vs. OraclePLSQLType, simple vs. complex). The extra scale
     * and precision parameters indicates that this parameter, when used in an Anonymous PL/SQL
     * block, requires scale and precision specification
     */
    public void addNamedOutputArgument(String procedureParameterName, DatabaseType databaseType,
        int precision, int scale) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.OUT, dt,
            precision, scale));
    }

    @Override
    public void addNamedOutputArgument(String procedureParameterName, String argumentFieldName,
        int jdbcType, String typeName, Class javaType) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.OUT,
            getDatabaseTypeForCode(jdbcType)));
    }

    @Override
    public void addNamedOutputArgument(String procedureParameterName, String argumentFieldName,
        int jdbcType, String typeName, Class javaType, DatabaseField nestedType) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.OUT,
            getDatabaseTypeForCode(jdbcType)));
    }

    @Override
    public void addNamedOutputArgument(String procedureParameterName, String argumentFieldName,
        int type, String typeName) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.OUT,
            getDatabaseTypeForCode(type)));
    }

    @Override
    public void addNamedOutputArgument(String procedureParameterName, String argumentFieldName,
        int type) {
        arguments.add(new PLSQLargument(procedureParameterName, originalIndex++, ParameterType.OUT,
            getDatabaseTypeForCode(type)));
    }

    // un-supported addXXX operations

    @Override
    public void addNamedArgument(String procedureParameterAndArgumentFieldName) {
        throw QueryException.addArgumentsNotSupported("named arguments without DatabaseType classification");
    }

    @Override
    public void addNamedArgumentValue(String procedureParameterName, Object argumentValue) {
        throw QueryException.addArgumentsNotSupported("named argument values without DatabaseType classification");
    }

    @Override
    public void addNamedArgument(String procedureParameterName, String argumentFieldName) {
        throw QueryException.addArgumentsNotSupported("named argument values without DatabaseType classification");
    }

    @Override
    public void addNamedInOutputArgument(String procedureParameterAndArgumentFieldName) {
        throw QueryException.addArgumentsNotSupported("named IN OUT argument without DatabaseType classification");
    }

    @Override
    public void addNamedInOutputArgument(String procedureParameterName, String argumentFieldName) {
        throw QueryException.addArgumentsNotSupported("named IN OUT arguments without DatabaseType classification");
    }

    @Override
    public void addNamedInOutputArgument(String procedureParameterName, String argumentFieldName,
        Class type) {
        throw QueryException.addArgumentsNotSupported("named IN OUT arguments without DatabaseType classification");
    }

    @Override
    public void addNamedInOutputArgument(String procedureParameterName, String inArgumentFieldName,
        String outArgumentFieldName, Class type) {
        throw QueryException.addArgumentsNotSupported("named IN OUT arguments without DatabaseType classification");
    }

    @Override
    public void addNamedInOutputArgumentValue(String procedureParameterName,
        Object inArgumentValue, String outArgumentFieldName, Class type) {
        throw QueryException.addArgumentsNotSupported("named IN OUT argument values without DatabaseType classification");
    }

    @Override
    public void addNamedOutputArgument(String procedureParameterAndArgumentFieldName) {
        throw QueryException.addArgumentsNotSupported("named OUT arguments without DatabaseType classification");
    }

    @Override
    public void addNamedOutputArgument(String procedureParameterName, String argumentFieldName) {
        throw QueryException.addArgumentsNotSupported("named OUT arguments without DatabaseType classification");
    }

    @Override
    public void addNamedOutputArgument(String procedureParameterName, String argumentFieldName,
        Class type) {
        throw QueryException.addArgumentsNotSupported("named OUT arguments without DatabaseType classification");
    }

    @Override
    public void useNamedCursorOutputAsResultSet(String argumentName) {
        throw QueryException.addArgumentsNotSupported("named OUT cursor arguments without DatabaseType classification");
    }

    // unlikely we will EVER support unnamed parameters
    @Override
    public void addUnamedArgument(String argumentFieldName, Class type) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedArgument(String argumentFieldName, int type, String typeName,
        DatabaseField nestedType) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedArgument(String argumentFieldName, int type, String typeName) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedArgument(String argumentFieldName, int type) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedArgument(String argumentFieldName) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedArgumentValue(Object argumentValue) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedInOutputArgument(String argumentFieldName, Class type) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedInOutputArgument(String inArgumentFieldName, String outArgumentFieldName,
        Class type) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedInOutputArgument(String inArgumentFieldName, String outArgumentFieldName,
        int type, String typeName, Class collection, DatabaseField nestedType) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedInOutputArgument(String inArgumentFieldName, String outArgumentFieldName,
        int type, String typeName, Class collection) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedInOutputArgument(String inArgumentFieldName, String outArgumentFieldName,
        int type, String typeName) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedInOutputArgument(String inArgumentFieldName, String outArgumentFieldName,
        int type) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedInOutputArgument(String argumentFieldName) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedInOutputArgumentValue(Object inArgumentValue, String outArgumentFieldName,
        Class type) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedOutputArgument(String argumentFieldName, Class type) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedOutputArgument(String argumentFieldName, int jdbcType, String typeName,
        Class javaType, DatabaseField nestedType) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedOutputArgument(String argumentFieldName, int jdbcType, String typeName,
        Class javaType) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedOutputArgument(String argumentFieldName, int type, String typeName) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedOutputArgument(String argumentFieldName, int type) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void addUnamedOutputArgument(String argumentFieldName) {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    @Override
    public void useUnnamedCursorOutputAsResultSet() {
        throw QueryException.unnamedArgumentsNotSupported();
    }

    /**
     * PUBLIC: Add a named OUT cursor argument to the stored procedure. The databaseType parameter
     * classifies the parameter (JDBCType vs. OraclePLSQLType, simple vs. complex).
     */
    public void useNamedCursorOutputAsResultSet(String argumentName, DatabaseType databaseType) {
        DatabaseType dt = databaseType.isComplexDatabaseType() ?
            ((ComplexDatabaseType)databaseType).clone() : databaseType;
        PLSQLargument newArg = new PLSQLargument(argumentName, originalIndex++, ParameterType.OUT, dt);
        newArg.cursorOutput = true;
        arguments.add(newArg);
    }

    /**
     * INTERNAL compute the re-ordered indices - Do the IN args first, then the
     * 'IN-half' of the INOUT args next, the OUT args, then the 'OUT-half' of
     * the INOUT args
     */
    protected void assignIndices() {
        List<PLSQLargument> inArguments = getArguments(arguments, ParameterType.IN);
        List<PLSQLargument> inOutArguments = getArguments(arguments, ParameterType.INOUT);
        DatabasePlatform platform = this.getQuery().getSession().getPlatform();
        inArguments.addAll(inOutArguments);
        int newIndex = 1;
        List<PLSQLargument> expandedArguments = new ArrayList<PLSQLargument>();
        // Must move any expanded types to the end, as they are assigned after
        // in the BEGIN clause.
        for (ListIterator<PLSQLargument> inArgsIter = inArguments.listIterator(); inArgsIter.hasNext();) {
            PLSQLargument inArg = inArgsIter.next();
            if (inArg.databaseType.isComplexDatabaseType() && (!((ComplexDatabaseType) inArg.databaseType).hasCompatibleType())) {
                expandedArguments.add(inArg);
                inArgsIter.remove();
            }
        }
        inArguments.addAll(expandedArguments);
        for (ListIterator<PLSQLargument> inArgsIter = inArguments.listIterator(); inArgsIter.hasNext();) {
            PLSQLargument inArg = inArgsIter.next();
            // delegate to arg's DatabaseType - ComplexTypes may expand arguments
            // use ListIterator so that computeInIndex can add expanded args
            newIndex = inArg.databaseType.computeInIndex(inArg, newIndex, inArgsIter);
        }
        for (PLSQLargument inArg : inArguments) {
            DatabaseType type = inArg.databaseType;
            if (platform.isOracle23() && type == OraclePLSQLTypes.PLSQLBoolean && Helper.compareVersions(platform.getDriverVersion(), "23.0.0") >= 0) {
                type = JDBCTypes.BOOLEAN_TYPE;
                inArg.databaseType = JDBCTypes.BOOLEAN_TYPE;
            }
            String inArgName = inArg.name;
            if (!type.isComplexDatabaseType()) {
                // for XMLType, we need to set type name parameter (will be "XMLTYPE")
                if (type == XMLType) {
                    super.addNamedArgument(inArgName, inArgName, type.getConversionCode(), type.getTypeName());
                } else {
                    super.addNamedArgument(inArgName, inArgName, type.getConversionCode());
                }
            } else {
                ComplexDatabaseType complexType = (ComplexDatabaseType) type;
                if (inArg.inIndex != MIN_VALUE) {
                    if (complexType.isStruct()) {
                        super.addNamedArgument(inArgName, inArgName, complexType.getSqlCode(), complexType.getTypeName());
                    } else if (complexType.isArray()) {
                        DatabaseType nestedType = ((OracleArrayType) complexType).getNestedType();
                        if (nestedType != null) {
                            ObjectRelationalDatabaseField field = new ObjectRelationalDatabaseField("");
                            field.setSqlType(nestedType.getSqlCode());
                            field.setSqlTypeName(nestedType.getTypeName());
                            super.addNamedArgument(inArgName, inArgName, complexType.getSqlCode(), complexType.getTypeName(), field);
                        } else {
                            super.addNamedArgument(inArgName, inArgName, complexType.getSqlCode(), complexType.getTypeName());
                        }
                    } else if (complexType.isCollection()) {
                        DatabaseType nestedType = ((PLSQLCollection) complexType).getNestedType();
                        if (nestedType != null) {
                            ObjectRelationalDatabaseField field = new ObjectRelationalDatabaseField("");
                            field.setSqlType(nestedType.getConversionCode());
                            if (nestedType.isComplexDatabaseType()) {
                                field.setSqlTypeName(((ComplexDatabaseType) nestedType).getCompatibleType());
                            }
                            super.addNamedArgument(inArgName, inArgName, type.getConversionCode(), complexType.getCompatibleType(), field);
                        } else {
                            super.addNamedArgument(inArgName, inArgName, type.getConversionCode(), complexType.getCompatibleType());
                        }
                    } else {
                        super.addNamedArgument(inArgName, inArgName, type.getConversionCode(), complexType.getCompatibleType());
                    }
                }
            }
        }
        List<PLSQLargument> outArguments = getArguments(arguments, ParameterType.OUT);
        outArguments.addAll(inOutArguments);
        for (ListIterator<PLSQLargument> outArgsIter = outArguments.listIterator(); outArgsIter.hasNext();) {
            PLSQLargument outArg = outArgsIter.next();
            newIndex = outArg.databaseType.computeOutIndex(outArg, newIndex, outArgsIter);
        }
        for (PLSQLargument outArg : outArguments) {
            String outArgName = outArg.name;
            if (outArg.cursorOutput) {
                super.useNamedCursorOutputAsResultSet(outArgName);
            } else {
                DatabaseType type = outArg.databaseType;
                if (platform.isOracle23() && type == OraclePLSQLTypes.PLSQLBoolean && Helper.compareVersions(platform.getDriverVersion(), "23.0.0") >= 0) {
                    type = JDBCTypes.BOOLEAN_TYPE;
                    outArg.databaseType = JDBCTypes.BOOLEAN_TYPE;
                }
                if (!type.isComplexDatabaseType()) {
                    // for XMLType, we need to set type name parameter (will be "XMLTYPE")
                    if (type == XMLType) {
                        super.addNamedOutputArgument(outArgName, outArgName, type.getConversionCode(), type.getTypeName());
                    } else {
                        super.addNamedOutputArgument(outArgName, outArgName, type.getConversionCode());
                    }
                } else {
                    ComplexDatabaseType complexType = (ComplexDatabaseType) type;
                    if (outArg.outIndex != MIN_VALUE) {
                        if (complexType.isStruct()) {
                            super.addNamedOutputArgument(outArgName, outArgName, complexType.getSqlCode(), complexType.getTypeName(), complexType.getJavaType());
                        } else if (complexType.isArray()) {
                            DatabaseType nestedType = ((OracleArrayType) complexType).getNestedType();
                            if (nestedType != null) {
                                ObjectRelationalDatabaseField nestedField = new ObjectRelationalDatabaseField("");
                                nestedField.setSqlType(nestedType.getSqlCode());
                                if (nestedType.isComplexDatabaseType()) {
                                    ComplexDatabaseType complexNestedType = (ComplexDatabaseType) nestedType;
                                    nestedField.setType(complexNestedType.getJavaType());
                                    nestedField.setSqlTypeName(complexNestedType.getCompatibleType());
                                }
                                super.addNamedOutputArgument(outArgName, outArgName, type.getSqlCode(), complexType.getTypeName(), complexType.getJavaType(), nestedField);
                            } else {
                                super.addNamedOutputArgument(outArgName, outArgName, type.getSqlCode(), complexType.getTypeName(), complexType.getJavaType());
                            }
                        } else if (complexType.isCollection()) {
                            DatabaseType nestedType = ((PLSQLCollection) complexType).getNestedType();
                            if (nestedType != null) {
                                ObjectRelationalDatabaseField nestedField = new ObjectRelationalDatabaseField(outArgName);
                                nestedField.setSqlType(nestedType.getSqlCode());
                                if (nestedType.isComplexDatabaseType()) {
                                    ComplexDatabaseType complexNestedType = (ComplexDatabaseType) nestedType;
                                    nestedField.setType(complexNestedType.getJavaType());
                                    nestedField.setSqlTypeName(complexNestedType.getCompatibleType());
                                }
                                super.addNamedOutputArgument(outArgName, outArgName, type.getSqlCode(), complexType.getCompatibleType(), complexType.getJavaType(), nestedField);
                            } else {
                                super.addNamedOutputArgument(outArgName, outArgName, type.getSqlCode(), complexType.getCompatibleType());
                            }
                        } else if (complexType.hasCompatibleType()) {
                            super.addNamedOutputArgument(outArgName, outArgName, type.getSqlCode(), complexType.getCompatibleType(), complexType.getJavaType());
                        } else {
                            // If there is no STRUCT type set, then the output is
                            // expanded, so one output for each field.
                            super.addNamedOutputArgument(outArgName, outArgName, type.getSqlCode());
                        }
                    }
                }
            }
        }
    }

    /**
     * INTERNAL
     * Generate portion of the Anonymous PL/SQL block that declares the temporary variables
     * in the DECLARE section.
     */
    protected void buildDeclareBlock(StringBuilder sb, List<PLSQLargument> arguments) {
        List<PLSQLargument> inArguments = getArguments(arguments, ParameterType.IN);
        List<PLSQLargument> inOutArguments = getArguments(arguments, ParameterType.INOUT);
        inArguments.addAll(inOutArguments);
        List<PLSQLargument> outArguments = getArguments(arguments, ParameterType.OUT);
        Collections.sort(inArguments, new InArgComparer());
        for (PLSQLargument arg : inArguments) {
            arg.databaseType.buildInDeclare(sb, arg);
        }
        Collections.sort(outArguments, new OutArgComparer());
        for (PLSQLargument arg : outArguments) {
            arg.databaseType.buildOutDeclare(sb, arg);
        }
    }

    /**
     * INTERNAL
     * Add the nested function string required for the type and its subtypes. The functions
     * must be added in inverse order to resolve dependencies.
     */
    protected void addNestedFunctionsForArgument(List functions, PLSQLargument argument,
                                                 DatabaseType databaseType, Set<DatabaseType> processed) {
        if ((databaseType == null)
                || !databaseType.isComplexDatabaseType()
                || databaseType.isJDBCType()
                || argument.cursorOutput
                || processed.contains(databaseType)) {
            return;
        }
        ComplexDatabaseType type = (ComplexDatabaseType)databaseType;
        if (!type.hasCompatibleType()) {
            return;
        }
        processed.add(type);
        boolean isNestedTable = false;
        if (type.isCollection()) {
            isNestedTable = ((PLSQLCollection)type).isNestedTable();
            DatabaseType nestedType = ((PLSQLCollection)type).getNestedType();
            addNestedFunctionsForArgument(functions, argument, nestedType, processed);
        } else if (type.isRecord()) {
            for (PLSQLargument field : ((PLSQLrecord)type).getFields()) {
                DatabaseType nestedType = field.databaseType;
                addNestedFunctionsForArgument(functions, argument, nestedType, processed);
            }
        }
        TypeInfo info = this.typesInfo.get(type.getTypeName());
        // If the info was not found in publisher, then generate it.
        if (info == null) {
            info = generateNestedFunction(type, isNestedTable);
        }
        if (type.getTypeName().equals(type.getCompatibleType())) {
            if (!functions.contains(info.pl2SqlConv)) {
                functions.add(info.pl2SqlConv);
            }
        } else {
            if (argument.pdirection == ParameterType.IN) {
                if (!functions.contains(info.sql2PlConv)) {
                    functions.add(info.sql2PlConv);
                }
            } else if (argument.pdirection == ParameterType.INOUT) {
                if (!functions.contains(info.sql2PlConv)) {
                    functions.add(info.sql2PlConv);
                }
                if (!functions.contains(info.pl2SqlConv)) {
                    functions.add(info.pl2SqlConv);
                }
            } else if (argument.pdirection == ParameterType.OUT) {
                if (!functions.contains(info.pl2SqlConv)) {
                    functions.add(info.pl2SqlConv);
                }
            }
        }
    }

    /**
     * INTERNAL: Generate the nested function to convert the PLSQL type to its compatible SQL type.
     */
    protected TypeInfo generateNestedFunction(ComplexDatabaseType type) {
        return generateNestedFunction(type, false);
    }

    /**
     * INTERNAL: Generate the nested function to convert the PLSQL type to its compatible SQL type.
     */
    protected TypeInfo generateNestedFunction(ComplexDatabaseType type, boolean isNonAssociativeCollection) {
        TypeInfo info = new TypeInfo();
        info.pl2SqlName = PL2SQL_PREFIX + (this.functionId++);
        info.sql2PlName = SQL2PL_PREFIX + (this.functionId++);
        if (type.isRecord()) {
            PLSQLrecord record = (PLSQLrecord)type;
            StringBuilder sb = new StringBuilder();
            sb.append(INDENT);
            sb.append(BEGIN_DECLARE_FUNCTION);
            sb.append(info.pl2SqlName);
            sb.append("(aPlsqlItem ");
            sb.append(record.getTypeName());
            sb.append(")");
            sb.append(NL);sb.append(INDENT);
            sb.append(RTURN);
            sb.append(record.getCompatibleType());
            sb.append(" IS");
            sb.append(NL);sb.append(INDENT);sb.append(INDENT);
            sb.append("aSqlItem ");
            sb.append(record.getCompatibleType());
            sb.append(";");
            sb.append(NL);sb.append(INDENT);
            sb.append(BEGIN_BEGIN_BLOCK);
            sb.append(INDENT);sb.append(INDENT);
            sb.append("aSqlItem := ");
            sb.append(record.getCompatibleType());
            sb.append("(");
            int size = record.getFields().size();
            for (int index = 0; index < size; index++) {
                sb.append("NULL");
                if ((index + 1) != size) {
                    sb.append(", ");
                }
            }
            sb.append(");");
            sb.append(NL);
            for (PLSQLargument argument : record.getFields()) {
                sb.append(INDENT);sb.append(INDENT);
                sb.append("aSqlItem.");
                sb.append(argument.name);
                if (argument.databaseType.isComplexDatabaseType() && !((ComplexDatabaseType)argument.databaseType).isJDBCType()) {
                    sb.append(" := ");
                    sb.append(getPl2SQLName((ComplexDatabaseType)argument.databaseType));
                    sb.append("(aPlsqlItem.");
                    sb.append(argument.name);
                    sb.append(");");
                }
                else if (argument.databaseType.equals(PLSQLBoolean)) {
                    sb.append(" := ");
                    sb.append(PLSQLBoolean_OUT_CONV);
                    sb.append("(aPlsqlItem.");
                    sb.append(argument.name);
                    sb.append(");");
                }
                else {
                    sb.append(" := aPlsqlItem.");
                    sb.append(argument.name);
                    sb.append(";");
                }
                sb.append(NL);
            }
            sb.append(INDENT);sb.append(INDENT);
            sb.append(RTURN);
            sb.append("aSqlItem;");
            sb.append(NL);
            sb.append(INDENT);
            sb.append("END ");
            sb.append(info.pl2SqlName);
            sb.append(";");
            sb.append(NL);

            info.pl2SqlConv = sb.toString();

            sb = new StringBuilder();
            sb.append(INDENT);
            sb.append(BEGIN_DECLARE_FUNCTION);
            sb.append(info.sql2PlName);
            sb.append("(aSqlItem ");
            sb.append(record.getCompatibleType());
            sb.append(") ");
            sb.append(NL);sb.append(INDENT);
            sb.append(RTURN);
            sb.append(record.getTypeName());
            sb.append(" IS");
            sb.append(NL);sb.append(INDENT);sb.append(INDENT);
            sb.append("aPlsqlItem ");
            sb.append(record.getTypeName());
            sb.append(";");
            sb.append(NL);sb.append(INDENT);
            sb.append(BEGIN_BEGIN_BLOCK);
            for (PLSQLargument argument : record.getFields()) {
                sb.append(INDENT);sb.append(INDENT);
                sb.append("aPlsqlItem.");
                sb.append(argument.name);
                if (argument.databaseType.isComplexDatabaseType() && !((ComplexDatabaseType)argument.databaseType).isJDBCType()) {
                    sb.append(" := ");
                    sb.append(getSQL2PlName((ComplexDatabaseType)argument.databaseType));
                    sb.append("(aSqlItem.");
                    sb.append(argument.name);
                    sb.append(");");
                }
                else if (argument.databaseType.equals(PLSQLBoolean)) {
                    sb.append(" := ");
                    sb.append(PLSQLBoolean_IN_CONV);
                    sb.append("(aSqlItem.");
                    sb.append(argument.name);
                    sb.append(");");
                }
                else {
                    sb.append(" := aSqlItem.");
                    sb.append(argument.name);
                    sb.append(";");
                }
                sb.append(NL);
            }
            sb.append(INDENT);sb.append(INDENT);
            sb.append(RTURN);
            sb.append("aPlsqlItem;");
            sb.append(NL);
            sb.append(INDENT);
            sb.append("END ");
            sb.append(info.sql2PlName);
            sb.append(";");
            sb.append(NL);

            info.sql2PlConv = sb.toString();
        }
        else if (type.isCollection()) {
            PLSQLCollection collection = (PLSQLCollection)type;
            StringBuilder sb = new StringBuilder();
            sb.append(INDENT);
            sb.append(BEGIN_DECLARE_FUNCTION);
            sb.append(info.pl2SqlName);
            sb.append("(aPlsqlItem ");
            sb.append(collection.getTypeName());
            sb.append(")");
            sb.append(NL);sb.append(INDENT);
            sb.append(RTURN);
            sb.append(collection.getCompatibleType());
            sb.append(" IS");
            sb.append(NL);sb.append(INDENT);sb.append(INDENT);
            sb.append("aSqlItem ");
            sb.append(collection.getCompatibleType());
            sb.append(";");
            sb.append(NL);sb.append(INDENT);
            sb.append(BEGIN_BEGIN_BLOCK);
            sb.append(INDENT);sb.append(INDENT);
            sb.append("aSqlItem := ");
            sb.append(collection.getCompatibleType());
            sb.append("();");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);
            sb.append("aSqlItem.EXTEND(aPlsqlItem.COUNT);");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);
            sb.append("IF aPlsqlItem.COUNT > 0 THEN");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);
            sb.append("FOR I IN aPlsqlItem.FIRST..aPlsqlItem.LAST LOOP");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);sb.append(INDENT);
            sb.append("aSqlItem(I + 1 - aPlsqlItem.FIRST) := ");
            if (collection.nestedType != null && (collection.nestedType.isComplexDatabaseType() && !((ComplexDatabaseType)collection.nestedType).isJDBCType())) {
                sb.append(getPl2SQLName((ComplexDatabaseType)collection.nestedType));
                sb.append("(aPlsqlItem(I));");
            }
            else if (PLSQLBoolean.equals(collection.nestedType)) {
                sb.append(PLSQLBoolean_OUT_CONV);
                sb.append("(aPlsqlItem(I));");
            }
            else {
                sb.append("aPlsqlItem(I);");
            }
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);sb.append(INDENT);
            sb.append("END LOOP;");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);
            sb.append("END IF;");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);
            sb.append(RTURN);
            sb.append("aSqlItem;");
            sb.append(NL);sb.append(INDENT);
            sb.append("END ");
            sb.append(info.pl2SqlName);
            sb.append(";");
            sb.append(NL);

            info.pl2SqlConv = sb.toString();

            sb = new StringBuilder();
            sb.append(INDENT);
            sb.append(BEGIN_DECLARE_FUNCTION);
            sb.append(info.sql2PlName);
            sb.append("(aSqlItem ");
            sb.append(collection.getCompatibleType());
            sb.append(")");
            sb.append(NL);
            sb.append(INDENT);
            sb.append(RTURN);
            sb.append(collection.getTypeName());
            sb.append(" IS");
            sb.append(NL);sb.append(INDENT);sb.append(INDENT);
            sb.append("aPlsqlItem ");
            sb.append(collection.getTypeName());
            sb.append(";");
            sb.append(NL);sb.append(INDENT);
            sb.append(BEGIN_BEGIN_BLOCK);
            sb.append(INDENT);sb.append(INDENT);
            // if the collection is non-associative we need to initialize it
            if (isNonAssociativeCollection) {
                sb.append("aPlsqlItem := ");
                sb.append(collection.getTypeName());
                sb.append("();");
                sb.append(NL);
                sb.append(INDENT);sb.append(INDENT);
                sb.append("aPlsqlItem.EXTEND(aSqlItem.COUNT);");
                sb.append(NL);
                sb.append(INDENT);sb.append(INDENT);
            }
            sb.append("IF aSqlItem.COUNT > 0 THEN");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);sb.append(INDENT);
            sb.append("FOR I IN 1..aSqlItem.COUNT LOOP");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);sb.append(INDENT);sb.append(INDENT);
            if ((collection.nestedType != null) && collection.nestedType.isComplexDatabaseType()) {
                sb.append("aPlsqlItem(I) := ");
                sb.append(getSQL2PlName((ComplexDatabaseType)collection.nestedType));
                sb.append("(aSqlItem(I));");
            }
            else if (PLSQLBoolean.equals(collection.nestedType)) {
                sb.append("aPlsqlItem(I + 1 - aSqlItem.FIRST) := ");
                sb.append(PLSQLBoolean_IN_CONV);
                sb.append("(aSqlItem(I));");
            }
            else {
                sb.append("aPlsqlItem(I) := aSqlItem(I);");
            }
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);sb.append(INDENT);
            sb.append("END LOOP;");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);
            sb.append("END IF;");
            sb.append(NL);
            sb.append(INDENT);sb.append(INDENT);
            sb.append(RTURN);
            sb.append("aPlsqlItem;");
            sb.append(NL);
            sb.append(INDENT);
            sb.append("END ");
            sb.append(info.sql2PlName);
            sb.append(";");
            sb.append(NL);

            info.sql2PlConv = sb.toString();
        }
        this.typesInfo.put(type.getTypeName(), info);
        return info;
    }

    /**
     * INTERNAL
     * Generate portion of the Anonymous PL/SQL block with PL/SQL conversion routines as
     * nested functions.
     */
    protected void buildNestedFunctions(StringBuilder stream, List<PLSQLargument> arguments) {
        List<String> nestedFunctions = new ArrayList<String>();
        Set<DatabaseType> processed = new HashSet<DatabaseType>();
        for (PLSQLargument arg : arguments) {
            DatabaseType type = arg.databaseType;
            addNestedFunctionsForArgument(nestedFunctions, arg, type, processed);
        }
        if (!nestedFunctions.isEmpty()) {
            for (String function : nestedFunctions) {
                stream.append(function);
            }
        }
    }

    /**
     * INTERNAL Generate portion of the Anonymous PL/SQL block that assigns fields at the beginning
     * of the BEGIN block (before invoking the target procedure).
     */
    protected void buildBeginBlock(StringBuilder sb, List<PLSQLargument> arguments) {
        List<PLSQLargument> inArguments = getArguments(arguments, ParameterType.IN);
        inArguments.addAll(getArguments(arguments, ParameterType.INOUT));
        for (PLSQLargument arg : inArguments) {
            arg.databaseType.buildBeginBlock(sb, arg, this);
        }
    }

    /**
     * INTERNAL Generate portion of the Anonymous PL/SQL block that invokes the target procedure.
     */
    protected void buildProcedureInvocation(StringBuilder sb, List<PLSQLargument> arguments) {
        sb.append("  ");
        sb.append(getProcedureName());
        sb.append("(");
        int size = arguments.size(), idx = 1;
        for (PLSQLargument argument : arguments) {
            sb.append(argument.name);
            sb.append("=>");
            sb.append(databaseTypeHelper.buildTarget(argument));
            if (idx < size) {
                sb.append(", ");
                idx++;
            }
        }
        sb.append(");");
        sb.append(NL);
    }

    /**
     * INTERNAL Generate portion of the Anonymous PL/SQL block after the target procedures has been
     * invoked and OUT parameters must be handled.
     */
    protected void buildOutAssignments(StringBuilder sb, List<PLSQLargument> arguments) {
        List<PLSQLargument> outArguments = getArguments(arguments, ParameterType.OUT);
        outArguments.addAll(getArguments(arguments, ParameterType.INOUT));
        for (PLSQLargument arg : outArguments) {
            arg.databaseType.buildOutAssignment(sb, arg, this);
        }
    }

    /**
     * Generate the Anonymous PL/SQL block
     */
    @Override
    protected void prepareInternal(AbstractSession session) {
        // build any and all required type conversion routines for
        // complex PL/SQL types in packages
        this.typesInfo = new HashMap<String, TypeInfo>();
        // Rest parameters to be recomputed if being reprepared.
        this.parameters = null;
        // create a copy of the arguments re-ordered with different indices
        assignIndices();

        // Filter out any optional arguments that are null.
        List<PLSQLargument> specifiedArguments = this.arguments;
        AbstractRecord row = getQuery().getTranslationRow();
        if ((row != null) && hasOptionalArguments()) {
            for (PLSQLargument argument : this.arguments) {
                DatabaseField queryArgument = new DatabaseField(argument.name);
                if (this.optionalArguments.contains(queryArgument) && (row.get(queryArgument) == null)) {
                    if (specifiedArguments == this.arguments) {
                        specifiedArguments = new ArrayList<PLSQLargument>(this.arguments);
                    }
                    specifiedArguments.remove(argument);
                }
            }
        }
        // build the Anonymous PL/SQL block in sections
        StringBuilder sb = new StringBuilder();
        if (!specifiedArguments.isEmpty()) {
            sb.append(BEGIN_DECLARE_BLOCK);
            buildDeclareBlock(sb, specifiedArguments);
            buildNestedFunctions(sb, specifiedArguments);
        }
        sb.append(BEGIN_BEGIN_BLOCK);
        buildBeginBlock(sb, specifiedArguments);
        buildProcedureInvocation(sb, specifiedArguments);
        buildOutAssignments(sb, specifiedArguments);
        sb.append(END_BEGIN_BLOCK);
        setSQLStringInternal(sb.toString());
        super.prepareInternalParameters(session);
    }

    /**
     * INTERNAL:
     * Prepare the JDBC statement, this may be parameterize or a call statement.
     * If caching statements this must check for the pre-prepared statement and re-bind to it.
     */
    @Override
    public Statement prepareStatement(DatabaseAccessor accessor, AbstractRecord translationRow, AbstractSession session) throws SQLException {
        //#Bug5200836 pass shouldUnwrapConnection flag to indicate whether or not using unwrapped connection.
        Statement statement = accessor.prepareStatement(this, session);

        // Setup the max rows returned and query timeout limit.
        if (this.queryTimeout > 0 && this.queryTimeoutUnit != null) {
            long timeout = TimeUnit.SECONDS.convert(this.queryTimeout, this.queryTimeoutUnit);

            if(timeout > Integer.MAX_VALUE){
                timeout = Integer.MAX_VALUE;
            }

            //Round up the timeout if SECONDS are larger than the given units
            if(TimeUnit.SECONDS.compareTo(this.queryTimeoutUnit) > 0 && this.queryTimeout % 1000 > 0){
                timeout += 1;
            }
            statement.setQueryTimeout((int)timeout);
        }
        if (!this.ignoreMaxResultsSetting && this.maxRows > 0) {
            statement.setMaxRows(this.maxRows);
        }
        if (this.resultSetFetchSize > 0) {
            statement.setFetchSize(this.resultSetFetchSize);
        }

        if (this.parameters == null) {
            return statement;
        }
        List parameters = getParameters();
        int size = parameters.size();
        for (int index = 0; index < size; index++) {
            session.getPlatform().setParameterValueInDatabaseCall(parameters.get(index), (PreparedStatement)statement, index+1, session);
        }

        return statement;
    }

    /**
     * Translate the PLSQL procedure translation row, into the row
     * expected by the SQL procedure.
     * This handles expanding and re-ordering parameters.
     */
    @Override
    public void translate(AbstractRecord translationRow, AbstractRecord modifyRow, AbstractSession session) {
        // re-order elements in translationRow to conform to re-ordered indices
        AbstractRecord copyOfTranslationRow = translationRow.clone();
        int len = copyOfTranslationRow.size();
        List<DatabaseField> copyOfTranslationFields = copyOfTranslationRow.getFields();
        translationRow.clear();
        Vector<DatabaseField> translationRowFields = translationRow.getFields();
        translationRowFields.setSize(len);
        Vector translationRowValues = translationRow.getValues();
        translationRowValues.setSize(len);
        for (PLSQLargument arg : arguments) {
            if (arg.pdirection == ParameterType.IN || arg.pdirection == ParameterType.INOUT) {
                arg.databaseType.translate(arg, translationRow,
                    copyOfTranslationRow, copyOfTranslationFields, translationRowFields,
                    translationRowValues, this);
            }
        }
        this.translationRow = translationRow; // save a copy for logging
        super.translate(translationRow, modifyRow, session);
    }

    /**
     * Translate the SQL procedure output row, into the row
     * expected by the PLSQL procedure.
     * This handles re-ordering parameters.
     */
    @Override
    public AbstractRecord buildOutputRow(CallableStatement statement, DatabaseAccessor accessor, AbstractSession session) throws SQLException {

        AbstractRecord outputRow = super.buildOutputRow(statement, accessor, session);
        if (!shouldBuildOutputRow) {
            outputRow.put("", 1); // fake-out Oracle executeUpdate rowCount, always 1
            return outputRow;
        }
        // re-order elements in outputRow to conform to original indices
        Vector outputRowFields = outputRow.getFields();
        Vector outputRowValues = outputRow.getValues();
        DatabaseRecord newOutputRow = new DatabaseRecord();
        List<PLSQLargument> outArguments = getArguments(arguments, ParameterType.OUT);
        outArguments.addAll(getArguments(arguments, ParameterType.INOUT));
        Collections.sort(outArguments, new Comparator<PLSQLargument>() {
            @Override
            public int compare(PLSQLargument o1, PLSQLargument o2) {
                return o1.originalIndex - o2.originalIndex;
            }
        });
        for (PLSQLargument outArg : outArguments) {
            outArg.databaseType.buildOutputRow(outArg, outputRow, newOutputRow, outputRowFields, outputRowValues);
        }
        return newOutputRow;
    }

    /**
     * INTERNAL:
     * Build the log string for the call.
     */
    @Override
    public String getLogString(Accessor accessor) {

        StringBuilder sb = new StringBuilder(getSQLString());
        sb.append(Helper.cr());
        sb.append(INDENT);
        sb.append("bind => [");
        List<PLSQLargument> specifiedArguments = this.arguments;
        AbstractRecord row = getQuery().getTranslationRow();
        if ((row != null) && hasOptionalArguments()) {
            for (PLSQLargument argument : this.arguments) {
                DatabaseField queryArgument = new DatabaseField(argument.name);
                if (this.optionalArguments.contains(queryArgument) && (row.get(queryArgument) == null)) {
                    if (specifiedArguments == this.arguments) {
                        specifiedArguments = new ArrayList<PLSQLargument>(this.arguments);
                    }
                    specifiedArguments.remove(argument);
                }
            }
        }
        List<PLSQLargument> inArguments = getArguments(specifiedArguments, ParameterType.IN);
        inArguments.addAll(getArguments(specifiedArguments, ParameterType.INOUT));
        Collections.sort(inArguments, new Comparator<PLSQLargument>() {
            @Override
            public int compare(PLSQLargument o1, PLSQLargument o2) {
                return o1.inIndex - o2.inIndex;
            }
        });
        for (Iterator<PLSQLargument> i = inArguments.iterator(); i.hasNext();) {
            PLSQLargument inArg = i.next();
            inArg.databaseType.logParameter(sb, ParameterType.IN, inArg, translationRow,
                getQuery().getSession().getPlatform());
            if (i.hasNext()) {
                sb.append(", ");
            }
        }
        List<PLSQLargument> outArguments = getArguments(specifiedArguments, ParameterType.OUT);
        outArguments.addAll(getArguments(specifiedArguments, ParameterType.INOUT));
        Collections.sort(outArguments, new Comparator<PLSQLargument>() {
            @Override
            public int compare(PLSQLargument o1, PLSQLargument o2) {
                return o1.outIndex - o2.outIndex;
            }
        });
        if (!inArguments.isEmpty() && !outArguments.isEmpty()) {
            sb.append(", ");
        }
        for (Iterator<PLSQLargument> i = outArguments.iterator(); i.hasNext();) {
            PLSQLargument outArg = i.next();
            outArg.databaseType.logParameter(sb, ParameterType.OUT, outArg, translationRow,
                getQuery().getSession().getPlatform());
            if (i.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * INTERNAL
     *
     * @param args
     * @param direction
     * @return list of arguments with the specified direction
     */
    protected static List<PLSQLargument> getArguments(List<PLSQLargument> args, Integer direction) {
        return getArguments(args, ParameterType.valueOf(direction));
    }

    /**
     * INTERNAL
     *
     * @param args
     * @param direction
     * @return list of arguments with the specified direction
     */
    protected static List<PLSQLargument> getArguments(List<PLSQLargument> args, ParameterType direction) {
        List<PLSQLargument> inArgs = new ArrayList<PLSQLargument>();
        for (PLSQLargument arg : args) {
            if (arg.pdirection == direction) {
                inArgs.add(arg);
            }
        }
        return inArgs;
    }

    // Made static for performance reasons.
    /**
     * INTERNAL:
     * Helper structure used to store the PLSQL type conversion routines.
     */
    static final class TypeInfo {
        String sql2PlName;
        String sql2PlConv;
        String pl2SqlName;
        String pl2SqlConv;
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(NL);
            sb.append(sql2PlName == null ? "" : sql2PlConv);
            sb.append(pl2SqlName == null ? "" : pl2SqlConv);
            return sb.toString();
        }
    }

    /**
     * Return the conversion function name, generate the function if missing.
     */
    public String getSQL2PlName(ComplexDatabaseType type) {
        if (typesInfo == null) {
            return null;
        }
        TypeInfo info = typesInfo.get(type.getTypeName());
        if (info == null) {
            info = generateNestedFunction(type);
        }
        return info.sql2PlName;
    }

    @Override
    public boolean isStoredPLSQLProcedureCall() {
        return true;
    }

    /**
     * Return the conversion function name, generate the function if missing.
     */
    public String getPl2SQLName(ComplexDatabaseType type) {
        if (typesInfo == null) {
            return null;
        }
        TypeInfo info = typesInfo.get(type.getTypeName());
        if (info == null) {
            info = generateNestedFunction(type);
        }
        return info.pl2SqlName;
    }

    @Override
    public Object getOutputParameterValue(CallableStatement statement, int index, AbstractSession session) throws SQLException {
        return session.getPlatform().getParameterValueFromDatabaseCall(statement, index + 1, session);
    }

    /**
     * Return the PLSQL arguments.
     */
    public List<PLSQLargument> getArguments() {
        return arguments;
    }

    /**
     * Set the PLSQL arguments.
     */
    public void setArguments(List<PLSQLargument> arguments) {
        this.arguments = arguments;
    }

    // Made static final for performance reasons.
    /**
     * Class responsible for comparing PLSQLargument instances based on
     * the inIndex property.
     *
     */
    static final class InArgComparer implements Comparator<PLSQLargument>, Serializable {
        private static final long serialVersionUID = -4182293492217092689L;
        @Override
        public int compare(PLSQLargument arg0, PLSQLargument arg1) {
            if (arg0.inIndex < arg1.inIndex) {
                return -1;
            }
            if (arg0.inIndex > arg1.inIndex) {
                return 1;
            }
            return 0;
        }
    }

    // Made static final for performance reasons.
    /**
     * Class responsible for comparing PLSQLargument instances based on
     * the outIndex property.
     *
     */
    static final class OutArgComparer implements Comparator<PLSQLargument>, Serializable {
        private static final long serialVersionUID = -4182293492217092689L;
        @Override
        public int compare(PLSQLargument arg0, PLSQLargument arg1) {
            if (arg0.inIndex < arg1.outIndex) {
                return -1;
            }
            if (arg0.inIndex > arg1.outIndex) {
                return 1;
            }
            return 0;
        }
    }
}
