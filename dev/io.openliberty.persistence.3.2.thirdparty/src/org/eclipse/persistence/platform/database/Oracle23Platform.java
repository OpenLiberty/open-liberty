/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026 IBM Corporation. All rights reserved.
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
//     Oracle - initial API and implementation
package org.eclipse.persistence.platform.database;

import org.eclipse.persistence.core.sessions.CoreSession;
import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.sessions.AbstractSession;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;

public class Oracle23Platform extends Oracle21Platform {

    public Oracle23Platform() {
        super();
    }

    /**
     * INTERNAL:
     * Check whether current platform is Oracle 23c or later.
     * @return Always returns {@code true} for instances of Oracle 23c platform.
     * @since 4.0.2
     */
    @Override
    public boolean isOracle23() {
        return true;
    }

    /**
     * INTERNAL:
     * Allow for conversion from the Oracle type to the Java type. Used in cases when DB connection is needed like BLOB, CLOB.
     */
    @Override
    public <T> T convertObject(Object sourceObject, Class<T> javaClass, CoreSession<?, ?, ? ,?, ?> session) throws ConversionException, DatabaseException {
        //Handle special case when empty String ("") is passed from the entity into CLOB type column
        if (ClassConstants.CLOB.equals(javaClass) && sourceObject instanceof String && "".equals(sourceObject)) {
            AbstractSession abstractSession = (AbstractSession) session;
            try {
                abstractSession.getAccessor().incrementCallCount(abstractSession);
                Connection connection = abstractSession.getAccessor().getConnection();
                Clob clob = connection.createClob();
                clob.setString(1, (String)sourceObject);
                return (T) clob;
            } catch (SQLException e) {
                throw ConversionException.couldNotBeConvertedToClass(sourceObject, ClassConstants.CLOB, e);
            } finally {
                abstractSession.getAccessor().decrementCallCount();
            }
        }
        return super.convertObject(sourceObject, javaClass);
    }
}
