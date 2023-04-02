/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Upon the first execution attempt, shuts down Derby.
 */
public class DerbyShutdownTask implements Callable<Integer> {
    static AtomicInteger counter = new AtomicInteger();

    @Override
    public Integer call() throws Exception {
        int count = counter.incrementAndGet();
        System.out.println("Attempt to call DerbyShutdownTask #" + count);
        if (count == 1) {
            DataSource schedDBShutdown = (DataSource) new InitialContext().lookup("jdbc/schedDBShutdown");
            try (Connection con = schedDBShutdown.getConnection()) {
            	//DO NOTHING - just creating connection to shutdown database
            } catch (SQLException e) {
            	System.out.println("DerbyShutdownTask caught and rethrew exception " + e.getMessage());
            	e.printStackTrace(System.out);
            	throw e;
            }
        }
        return count;
    }
}
