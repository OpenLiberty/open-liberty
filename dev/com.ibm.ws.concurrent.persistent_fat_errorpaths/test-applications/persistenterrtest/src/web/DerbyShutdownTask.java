/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

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
        if (count == 1) {
            DataSource schedDBShutdown = (DataSource) new InitialContext().lookup("jdbc/schedDBShutdown");
            schedDBShutdown.getConnection().close(); // Expected to raise SQLException
        }
        return count;
    }
}
