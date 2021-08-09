/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.timedoperations;

import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.WSJdbcWrapper;

/**
 * This class provides externalizes to other bundles various information that is useful for recording timed JDBC operations.
 * This includes the SQL command, if any, and the identifier of the data source with which a JDBC resource is associated.
 */
public class TimedOpsAccessor {
    /**
     * Obtains the unique identifier of the data source associated with a JDBC wrapper.
     * 
     * @param jdbcWrapper proxy for a JDBC resource.
     * @return JNDI name of the data source if it has one, otherwise the config.displayId of the data source.
     */
    public static final String getDataSourceIdentifier(Object jdbcWrapper) {
        return WSJdbcUtil.getDataSourceIdentifier((WSJdbcWrapper) jdbcWrapper);
    }

    /**
     * Obtain the SQL command, if any, that is associated with a JDBC resource.
     * 
     * @param jdbcWrapper proxy for a JDBC resource.
     * @return SQL command associated with the JDBC resource. Null if not a wrapper for a PreparedStatement, CallableStatement, or ResultSet.
     */
    public static final String getSql(Object jdbcWrapper) {
        return WSJdbcUtil.getSql(jdbcWrapper);
    }
}
