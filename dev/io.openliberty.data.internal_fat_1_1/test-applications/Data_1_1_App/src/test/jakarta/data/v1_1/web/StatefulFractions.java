/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.v1_1.web;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import jakarta.data.constraint.EqualTo;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Is;
import jakarta.data.repository.Repository;
import jakarta.data.repository.stateful.Detach;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Stateful repository for the Fraction entity
 */
@Repository(dataStore = "MyDataStore")
public interface StatefulFractions {

    @Detach
    void detach(Fraction entity);

    @Find
    Optional<Fraction> fetch//
    (@By(_Fraction.NUMERATOR) @Is(EqualTo.class) long num,
     @By(_Fraction.DENOMINATOR) @Is long den);

    default void flush() {
        manager().flush();
    }

    EntityManager manager();

    /**
     * This is a workaround for Derby, which ignores query timeout
     * and eventually the lock timeout (default 60s) applies instead.
     * Tests can use this method to set the lock timeout to the desired
     * query timeout value to make a query that involves a lock appear
     * to time out as expected if the query timeout were honored.
     */
    @Transactional
    default void setLockTimeout(int seconds) throws SQLException {
        String sql = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(?, ?)";
        EntityManager em = manager();
        em.runWithConnection((Connection con) -> {
            try {
                CallableStatement cs = con.prepareCall(sql);
                cs.setString(1, "derby.locks.waitTimeout");
                cs.setInt(2, seconds);
                cs.execute();
            } catch (SQLException x) {
                throw new RuntimeException(x);
            }
        });
    }
}
