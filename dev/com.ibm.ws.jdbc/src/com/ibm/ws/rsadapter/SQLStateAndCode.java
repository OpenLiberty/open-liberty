/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter;

/**
 * A comparable ordered pair representing a non-null (SQL state, vendor-specific error code)
 */
public class SQLStateAndCode {
    public final int errorCode;
    public final int hashCode;
    public final String sqlState;

    /**
     * Construct a new instance.
     *
     * @param sqlState non-null SQL state value
     * @param errorCode vendor-specific error code
     */
    public SQLStateAndCode(String sqlState, int errorCode) {
        this.sqlState = sqlState;
        this.errorCode = errorCode;
        hashCode = errorCode + sqlState.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        SQLStateAndCode o;
        return other instanceof SQLStateAndCode
                        && errorCode == ((o = (SQLStateAndCode) other).errorCode)
                        && sqlState.equals(o.sqlState);
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return new StringBuilder(sqlState).append('/').append(errorCode).toString();
    }
}
