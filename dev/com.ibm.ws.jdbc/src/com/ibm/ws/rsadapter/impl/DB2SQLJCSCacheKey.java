/*******************************************************************************
 * Copyright (c) 2002, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.impl;

import java.lang.reflect.InvocationTargetException;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
* A key used for DB2 SQLJ CallableStatement caching.
*/
public class DB2SQLJCSCacheKey extends StatementCacheKey
{
    public static final int DB2_SQLJ_CALLABLE_STATEMENT = 4;

    /**
    * Create a new key for CallableStatement caching.
    *
    * @param theSQL the SQL for the CallableStatement.
    * @param theType the ResultSet type.
    * @param theConcurrency the ResultSet concurrency.
    * @param theHoldability the ResultSet holdability.
    * @param theSection com.ibm.db2.jcc.SQLJSection
    * @param isolation the transaction isolation level of the statement,
    *        or 0 if not supported.
    * @param schema The schema associated with the connection that created this statement
    * @param cacheKeySuffix portion of the key provided by the JCC driver, or null.
    */
    public DB2SQLJCSCacheKey(String theSQL, int theType, int theConcurrency,
           int theHoldability, Object theSection, int isolation, String dbSchema,
           String cacheKeySuffix)
    {
        type = theType;
        concurrency = theConcurrency;
        holdability = theHoldability;
        statementIsoLevel = isolation;
        schema = dbSchema;

        // DB2 SQLJ values, such as packageName and consistencyToken, will not be null.
        // The consistency token must always be an array of 8 bytes.  We convert it into a
        // single, unique long value to make comparisons more efficient.
        try {
            Class<?> SQLJSection = theSection.getClass();
            Object thePackage = SQLJSection.getMethod("getPackage").invoke(theSection);
            Class<?> SQLJPackage = thePackage.getClass();
            consistencyToken = toLong((byte[]) SQLJPackage.getMethod("getConsistencyToken").invoke(thePackage));

            hCode = (sql = theSQL).hashCode() +
                    (sectionNumber = (Integer) SQLJSection.getMethod("getSectionNumber").invoke(theSection)) +
                    (packageName = (String) SQLJPackage.getMethod("getPackageName").invoke(thePackage)).hashCode();
        } catch (Throwable x) {
            FFDCFilter.processException(x, getClass().getName(), "60", this);
            x = x instanceof InvocationTargetException ? x.getCause() : x;
            if (x instanceof RuntimeException)
                throw (RuntimeException) x;
            else if (x instanceof Error)
                throw (Error) x;
            else
                throw new RuntimeException(x);
        }

        statementType = DB2_SQLJ_CALLABLE_STATEMENT;

        this.cacheKeySuffix = cacheKeySuffix;
    }

    /**
     * @param keyToCheck the key to compare with this key.
     *
     * @return true if this key is equal to the key provided, otherwise false.
     */
    @Override
    public final boolean equals(Object keyToCheck)
    {
        try
        {
            // Try to avoid the string.equals if we can.

            StatementCacheKey k = (StatementCacheKey) keyToCheck;       

            return sql.equals(k.sql) &&
                   statementType == k.statementType &&
                   holdability == k.holdability &&
                   type == k.type &&
                   concurrency == k.concurrency &&
                   consistencyToken == k.consistencyToken &&
                   sectionNumber == k.sectionNumber &&
                   packageName.equals(k.packageName) &&
                   statementIsoLevel == k.statementIsoLevel &&
                   AdapterUtil.match(schema, k.schema) &&
                   AdapterUtil.match(cacheKeySuffix, k.cacheKeySuffix);
        }
        catch (Exception ex)
        {
            // No FFDC code needed.
            return false;
        }
    }

    /**
    * @return a nice, fancy string representing this key.
    */
    @Override
    public String toString()
    {
        return "DB2 SQLJ CSTMT: " + sql + ' ' + type + ' ' + concurrency + ' ' +
               holdability + ' ' + statementIsoLevel + ' ' + schema + ' ' + 
               sectionNumber + ' ' + packageName + " 0x" +
               Long.toHexString(consistencyToken) + ' ' +
               cacheKeySuffix;
    }
}
