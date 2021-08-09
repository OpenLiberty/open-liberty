/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

/**
 * Provides generic component context DataSource metadata. <p>
 * 
 * This 'generic' metadata implementation is returned when a JPA Persistence
 * Unit has been configured to use a resource reference that is bound into
 * the component environment context namespace (java:comp/env) and the JPA
 * Provider attempts to access the DatabaseMetaData outside the scope of a
 * Java EE componnet. Since every component may provide a different binding
 * for the java:comp name, it is not possible to determine the real datasource
 * when outside the scope of a component. <p>
 * 
 * This implementation is intended to satisfy the JPA Provider, so it will
 * successfully add a class transformer when creating the EntityManagerFactory
 * for a PersistenceUnit. Later, when the PersistenceUnit is actually used
 * within the scope of a component, a new EMF will be created (with the real
 * DataSource) and the initial one closed and discarded. <p>
 * 
 * The intent is that the JPA Provider will some level of database support,
 * and likely result in the provider defaulting to its 'generic' support,
 * rather than specific support for DB2, Derby, etc. To provide meaningful
 * metadata results, this implementation is loosely based on the values
 * returned by Derby. <p>
 **/
public final class GenericDatabaseMetaData implements DatabaseMetaData
{
    /**
     * JPAConnectionIndirector that returned this metadata. For getConnection.
     **/
    private final Connection ivConnection;

    /**
     * The java:comp/env DataSource name for trace.
     **/
    private final String ivDataSourceName;

    /**
     * Public constructor for use by JPAConnectionIndirector.
     **/
    GenericDatabaseMetaData(Connection connection,
                            String dataSourceName)
    {
        ivConnection = connection;
        ivDataSourceName = dataSourceName;
    }

    /**
     * Overridden to provide hashcode based on state.
     **/
    public int hashCode()
    {
        return ivConnection.hashCode() + ivDataSourceName.hashCode();
    }

    /**
     * Overridden to provide equality based on state.
     **/
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        if (obj instanceof GenericDatabaseMetaData)
        {
            // All instances created for a given connection are equivalent.
            if (this.ivConnection == ((GenericDatabaseMetaData) obj).ivConnection)
                return true;
        }

        return false;
    }

    /**
     * Overridden to provide meaningful trace.
     **/
    public String toString()
    {
        String identity = Integer.toHexString(System.identityHashCode(this));
        return "GenericDatabaseMetaData@" + identity + "[" + ivDataSourceName + "]";
    }

    // --------------------------------------------------------------------------
    //
    // java.sql.Wrapper  -  interface methods
    //
    // --------------------------------------------------------------------------

    public <T> T unwrap(Class<T> iface)
                    throws SQLException
    {
        try
        {
            return iface.cast(this);
        } catch (Throwable ex)
        {
            // Intentionally blank - SQLException thrown below
        }

        throw new SQLException(getClass().getName() + " does not implement " +
                               iface);
    }

    public boolean isWrapperFor(Class<?> iface)
                    throws SQLException
    {
        if (iface != null &&
            iface.isInstance(this))
        {
            return true;
        }

        return false;
    }

    // --------------------------------------------------------------------------
    //
    // java.sql.DatabaseMetaData  -  interface methods
    //
    // --------------------------------------------------------------------------

    public boolean allProceduresAreCallable()
                    throws SQLException
    {
        return true;
    }

    public boolean allTablesAreSelectable()
                    throws SQLException
    {
        return true;
    }

    public boolean dataDefinitionCausesTransactionCommit()
                    throws SQLException
    {
        return false;
    }

    public boolean dataDefinitionIgnoredInTransactions()
                    throws SQLException
    {
        return false;
    }

    public boolean deletesAreDetected(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean doesMaxRowSizeIncludeBlobs()
                    throws SQLException
    {
        return true;
    }

    public ResultSet getAttributes(String s, String s1, String s2, String s3)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getBestRowIdentifier(String s, String s1, String s2, int i, boolean flag)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getCatalogs()
                    throws SQLException
    {
        return null;
    }

    public String getCatalogSeparator()
                    throws SQLException
    {
        return "";
    }

    public String getCatalogTerm()
                    throws SQLException
    {
        return "CATALOG";
    }

    public ResultSet getColumnPrivileges(String s, String s1, String s2, String s3)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getColumns(String s, String s1, String s2, String s3)
                    throws SQLException
    {
        return null;
    }

    public Connection getConnection()
                    throws SQLException
    {
        return ivConnection;
    }

    public ResultSet getCrossReference(String s, String s1, String s2, String s3, String s4, String s5)
                    throws SQLException
    {
        return null;
    }

    public int getDatabaseMajorVersion()
                    throws SQLException
    {
        return 0;
    }

    public int getDatabaseMinorVersion()
                    throws SQLException
    {
        return 0;
    }

    public String getDatabaseProductName()
                    throws SQLException
    {
        // Provide a value that will be meaningful in trace.
        return "Generic Component Context DataSource"; // d508455.2
    }

    public String getDatabaseProductVersion()
                    throws SQLException
    {
        // Provide a value that will be meaningful in trace.
        return "Generic Component Context DataSource : " + ivDataSourceName;
    }

    public int getDefaultTransactionIsolation()
                    throws SQLException
    {
        return 2;
    }

    public int getDriverMajorVersion()
    {
        return 0;
    }

    public int getDriverMinorVersion()
    {
        return 0;
    }

    public String getDriverName()
                    throws SQLException
    {
        // Provide a value that will be meaningful in trace.
        return "Generic Component Context DataSource"; // d508455.2
    }

    public String getDriverVersion()
                    throws SQLException
    {
        // Provide a value that will be meaningful in trace.
        return "Generic Component Context DataSource : " + ivDataSourceName;
    }

    public ResultSet getExportedKeys(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public String getExtraNameCharacters()
                    throws SQLException
    {
        return "";
    }

    public String getIdentifierQuoteString()
                    throws SQLException
    {
        return "\"";
    }

    public ResultSet getImportedKeys(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getIndexInfo(String s, String s1, String s2, boolean flag, boolean flag1)
                    throws SQLException
    {
        return null;
    }

    public int getJDBCMajorVersion()
                    throws SQLException
    {
        return 3;
    }

    public int getJDBCMinorVersion()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxBinaryLiteralLength()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxCatalogNameLength()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxCharLiteralLength()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxColumnNameLength()
                    throws SQLException
    {
        return 128;
    }

    public int getMaxColumnsInGroupBy()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxColumnsInIndex()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxColumnsInOrderBy()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxColumnsInSelect()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxColumnsInTable()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxConnections()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxCursorNameLength()
                    throws SQLException
    {
        return 128;
    }

    public int getMaxIndexLength()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxProcedureNameLength()
                    throws SQLException
    {
        return 128;
    }

    public int getMaxRowSize()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxSchemaNameLength()
                    throws SQLException
    {
        return 128;
    }

    public int getMaxStatementLength()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxStatements()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxTableNameLength()
                    throws SQLException
    {
        return 128;
    }

    public int getMaxTablesInSelect()
                    throws SQLException
    {
        return 0;
    }

    public int getMaxUserNameLength()
                    throws SQLException
    {
        return 30;
    }

    public String getNumericFunctions()
                    throws SQLException
    {
        return "ABS,ACOS,ASIN,ATAN,CEILING,COS,COT,DEGREES,EXP,FLOOR,LOG,LOG10,MOD,PI,RADIANS,RAND,SIGN,SIN,SQRT,TAN";
    }

    public ResultSet getPrimaryKeys(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getProcedureColumns(String s, String s1, String s2, String s3)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getProcedures(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public String getProcedureTerm()
                    throws SQLException
    {
        return "PROCEDURE";
    }

    public int getResultSetHoldability()
                    throws SQLException
    {
        return 1;
    }

    public ResultSet getSchemas()
                    throws SQLException
    {
        return null;
    }

    public String getSchemaTerm()
                    throws SQLException
    {
        return "SCHEMA";
    }

    public String getSearchStringEscape()
                    throws SQLException
    {
        return "";
    }

    public String getSQLKeywords()
                    throws SQLException
    {
        return "ALIAS,BIGINT,BOOLEAN,CALL,CLASS,COPY,EXECUTE,EXPLAIN,FILE,FILTER,GETCURRENTCONNECTION,INDEX,INSTANCEOF,METHOD,NEW,OFF,PROPERTIES,RECOMPILE,RENAME,RUNTIMESTATISTICS,STATEMENT,STATISTICS,TIMING,WAIT";
    }

    public int getSQLStateType()
                    throws SQLException
    {
        return 2;
    }

    public String getStringFunctions()
                    throws SQLException
    {
        return "CONCAT,LENGTH,LCASE,LOCATE,LTRIM,RTRIM,SUBSTRING,UCASE";
    }

    public ResultSet getSuperTables(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getSuperTypes(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public String getSystemFunctions()
                    throws SQLException
    {
        return "USER";
    }

    public ResultSet getTablePrivileges(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getTables(String s, String s1, String s2, String as[])
                    throws SQLException
    {
        return null;
    }

    public ResultSet getTableTypes()
                    throws SQLException
    {
        return null;
    }

    public String getTimeDateFunctions()
                    throws SQLException
    {
        return "CURDATE,CURTIME,HOUR,MINUTE,MONTH,SECOND,TIMESTAMPADD,TIMESTAMPDIFF,YEAR";
    }

    public ResultSet getTypeInfo()
                    throws SQLException
    {
        return null;
    }

    public ResultSet getUDTs(String s, String s1, String s2, int ai[])
                    throws SQLException
    {
        return null;
    }

    public String getURL()
                    throws SQLException
    {
        return "";
    }

    public String getUserName()
                    throws SQLException
    {
        return "";
    }

    public ResultSet getVersionColumns(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public boolean insertsAreDetected(int i)
                    throws SQLException
    {
        return false;
    }

    public boolean isCatalogAtStart()
                    throws SQLException
    {
        return false;
    }

    public boolean isReadOnly()
                    throws SQLException
    {
        return false;
    }

    public boolean locatorsUpdateCopy()
                    throws SQLException
    {
        return true;
    }

    public boolean nullPlusNonNullIsNull()
                    throws SQLException
    {
        return true;
    }

    public boolean nullsAreSortedAtEnd()
                    throws SQLException
    {
        return false;
    }

    public boolean nullsAreSortedAtStart()
                    throws SQLException
    {
        return false;
    }

    public boolean nullsAreSortedHigh()
                    throws SQLException
    {
        return true;
    }

    public boolean nullsAreSortedLow()
                    throws SQLException
    {
        return false;
    }

    public boolean othersDeletesAreVisible(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean othersInsertsAreVisible(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean othersUpdatesAreVisible(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean ownDeletesAreVisible(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean ownInsertsAreVisible(int i)
                    throws SQLException
    {
        return false;
    }

    public boolean ownUpdatesAreVisible(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean storesLowerCaseIdentifiers()
                    throws SQLException
    {
        return false;
    }

    public boolean storesLowerCaseQuotedIdentifiers()
                    throws SQLException
    {
        return false;
    }

    public boolean storesMixedCaseIdentifiers()
                    throws SQLException
    {
        return false;
    }

    public boolean storesMixedCaseQuotedIdentifiers()
                    throws SQLException
    {
        return true;
    }

    public boolean storesUpperCaseIdentifiers()
                    throws SQLException
    {
        return true;
    }

    public boolean storesUpperCaseQuotedIdentifiers()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsAlterTableWithAddColumn()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsAlterTableWithDropColumn()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsANSI92EntryLevelSQL()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsANSI92FullSQL()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsANSI92IntermediateSQL()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsBatchUpdates()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsCatalogsInDataManipulation()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsCatalogsInIndexDefinitions()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsCatalogsInPrivilegeDefinitions()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsCatalogsInProcedureCalls()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsCatalogsInTableDefinitions()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsColumnAliasing()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsConvert()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsConvert(int i, int j)
                    throws SQLException
    {
        return true;
    }

    public boolean supportsCoreSQLGrammar()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsCorrelatedSubqueries()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsDataManipulationTransactionsOnly()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsDifferentTableCorrelationNames()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsExpressionsInOrderBy()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsExtendedSQLGrammar()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsFullOuterJoins()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsGetGeneratedKeys()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsGroupBy()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsGroupByBeyondSelect()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsGroupByUnrelated()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsIntegrityEnhancementFacility()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsLikeEscapeClause()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsLimitedOuterJoins()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsMinimumSQLGrammar()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsMixedCaseIdentifiers()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsMixedCaseQuotedIdentifiers()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsMultipleOpenResults()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsMultipleResultSets()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsMultipleTransactions()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsNamedParameters()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsNonNullableColumns()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsOpenCursorsAcrossCommit()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsOpenCursorsAcrossRollback()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsOpenStatementsAcrossCommit()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsOpenStatementsAcrossRollback()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsOrderByUnrelated()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsOuterJoins()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsPositionedDelete()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsPositionedUpdate()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsResultSetConcurrency(int i, int j)
                    throws SQLException
    {
        return true;
    }

    public boolean supportsResultSetHoldability(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean supportsResultSetType(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSavepoints()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSchemasInDataManipulation()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSchemasInIndexDefinitions()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSchemasInPrivilegeDefinitions()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSchemasInProcedureCalls()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSchemasInTableDefinitions()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSelectForUpdate()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsStatementPooling()
                    throws SQLException
    {
        return false;
    }

    public boolean supportsStoredProcedures()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSubqueriesInComparisons()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSubqueriesInExists()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSubqueriesInIns()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsSubqueriesInQuantifieds()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsTableCorrelationNames()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsTransactionIsolationLevel(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean supportsTransactions()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsUnion()
                    throws SQLException
    {
        return true;
    }

    public boolean supportsUnionAll()
                    throws SQLException
    {
        return true;
    }

    public boolean updatesAreDetected(int i)
                    throws SQLException
    {
        return true;
    }

    public boolean usesLocalFilePerTable()
                    throws SQLException
    {
        return true;
    }

    public boolean usesLocalFiles()
                    throws SQLException
    {
        return true;
    }

    public RowIdLifetime getRowIdLifetime()
                    throws SQLException
    {
        return RowIdLifetime.ROWID_UNSUPPORTED;
    }

    public ResultSet getSchemas(String s, String s1)
                    throws SQLException
    {
        return null;
    }

    public boolean supportsStoredFunctionsUsingCallSyntax()
                    throws SQLException
    {
        return true;
    }

    public boolean autoCommitFailureClosesAllResultSets()
                    throws SQLException
    {
        return true;
    }

    public ResultSet getClientInfoProperties()
                    throws SQLException
    {
        return null;
    }

    public ResultSet getFunctions(String s, String s1, String s2)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getFunctionColumns(String s, String s1, String s2, String s3)
                    throws SQLException
    {
        return null;
    }

    public ResultSet getPseudoColumns(String s, String s1, String s2, String s3)
                    throws SQLException
    {
        return null;
    }

    public boolean generatedKeyAlwaysReturned()
    {
        return false;
    }
}
