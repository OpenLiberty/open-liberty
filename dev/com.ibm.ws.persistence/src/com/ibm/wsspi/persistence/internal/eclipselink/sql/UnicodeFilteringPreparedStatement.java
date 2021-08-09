/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence.internal.eclipselink.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.persistence.RollbackException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.persistence.internal.eclipselink.sql.delegate.DelegatingPreparedStatement;

/**
 * A PreparedStatement implementation that delegates most operations to a PreparedStatement that is
 * passed into the constructor. The one piece of functionality added by this implementation is that
 * it insures that all String parameters passed into setString(...) are values between 32 and 128.
 */
@Trivial
public class UnicodeFilteringPreparedStatement extends DelegatingPreparedStatement {
    private static final TraceComponent tc = Tr.register(UnicodeFilteringPreparedStatement.class);

    public UnicodeFilteringPreparedStatement(PreparedStatement ps) {
        super(ps);
    }

    /**
     * Need to make sure that we don't trace / log {@code parameter} as it could contain sensitive
     * information.
     */
    @Override
    public void setString(int parameterIndex, @Sensitive String parameter) throws SQLException {
        if (!isValidString(parameter)) {
            // TODO -- work with consumers to make sure they understand this exception is coming
            // and make sure that it is exposed to end users in a consumable fashion.
            String errMsg = Tr.formatMessage(tc, "INVALID_CHAR_IN_PREPSTMT_CWWKD0203E");
            throw new RollbackException(errMsg);
        }
        super.setString(parameterIndex, parameter);
    }

    private static boolean isValidString(String unscanned) {
        if (unscanned == null || unscanned.isEmpty()) {
            return true;
        }
        for (int c : unscanned.toCharArray()) {
            // Allow 0 - 127
            if (c > 127) {
                return false;
            }
        }
        return true;
    }
}
