/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.impl;

import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * A reusable key for CallableStatement caching.
 */
public class CSCacheKey extends StatementCacheKey {
    public static final int CALLABLE_STATEMENT = 2;

    /**
     * @param keyToCheck the key to compare with this key.
     * 
     * @return true if this key is equal to the key provided, otherwise false.
     */
    @Override
    public final boolean equals(Object keyToCheck) {
        try {
            // Try to avoid the string.equals if we can. 

            if(keyToCheck == null)
                return false;
            
            StatementCacheKey k = (StatementCacheKey) keyToCheck;

            return (sql == k.sql || sql.equals(k.sql)) &&
                   statementType == k.statementType &&
                   holdability == k.holdability && 
                   type == k.type &&
                   concurrency == k.concurrency &&
                   statementIsoLevel == k.statementIsoLevel && 
                   AdapterUtil.match(schema, k.schema);
        } catch (Exception ex) {
            // No FFDC code needed.
            return false;
        }
    }

    /**
     * Create a new key for CallableStatement caching.
     * 
     * @param theSQL the SQL for the CallableStatement.
     * @param theType the ResultSet type.
     * @param theConcurrency the ResultSet concurrency.
     * @param theHoldability the ResultSet holdability.
     * @param isolation the transaction isolation level for the statement, or 0 if not supported.
     * @param dbSchema The schema associated with the connection that created this statement.
     */
    public CSCacheKey(String theSQL, int theType, int theConcurrency, int theHoldability, int isolation, String dbSchema) { 
        type = theType;
        concurrency = theConcurrency;
        holdability = theHoldability;
        statementIsoLevel = isolation; 
        schema = dbSchema;
        hCode = (sql = theSQL).hashCode();
        statementType = CALLABLE_STATEMENT;

    }

    /**
     * @return a nice, fancy string representing this key.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("CSTMT: ").append(sql).append(' ').append(type);
        sb.append(' ').append(concurrency).append(' ').append(holdability).append(' ').append(statementIsoLevel);
        if(schema != null)
            sb.append(' ').append(schema);
        return sb.toString();
    }
}
