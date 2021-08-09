/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.InitialContext;
import javax.resource.spi.work.Work;
import javax.sql.DataSource;

/**
 * JCA work that runs a SQL command
 */
public class FATWork implements Work {
    private final String jndiName;
    private volatile boolean released;
    private final String sql;
    private final AtomicReference<Statement> statementRef = new AtomicReference<Statement>();

    public FATWork(String jndiName, String sql) {
        this.jndiName = jndiName;
        this.sql = sql;
    }

    @Override
    public void run() {
        try {
            if (released)
                return;
            System.out.println("about to run " + sql);
            DataSource ds = (DataSource) new InitialContext().lookup(jndiName);
            Connection con = ds.getConnection();
            try {
                if (released)
                    return;
                Statement stmt = con.createStatement();
                statementRef.set(stmt);
                try {
                    if (released)
                        return;
                    int updateCount = stmt.executeUpdate(sql);
                    System.out.println("successful; update count: " + updateCount);
                } finally {
                    statementRef.set(null);
                    stmt.close();
                }
            } finally {
                con.close();
            }
        } catch (RuntimeException x) {
            x.printStackTrace(System.out);
            throw x;
        } catch (Exception x) {
            x.printStackTrace(System.out);
            throw new RuntimeException(x);
        }
    }

    @Override
    public void release() {
        released = true;
        Statement stmt = statementRef.get();
        System.out.println("release " + stmt);
        if (stmt != null)
            try {
                stmt.cancel();
            } catch (SQLException x) {
                throw new RuntimeException(x);
            }
    }
}
