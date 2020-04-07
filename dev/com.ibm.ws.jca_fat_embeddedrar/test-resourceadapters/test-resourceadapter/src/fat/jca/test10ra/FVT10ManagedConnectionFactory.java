/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.test10ra;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

public class FVT10ManagedConnectionFactory implements ManagedConnectionFactory {

    private static final long serialVersionUID = 199087675445433L;

    private PrintWriter writer;

    private String userName;

    private String password;

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new FVT10ConnectionFactory(cm, this);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new FVT10ConnectionFactory(null, this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject,
                                                     ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        return new FVT10ManagedConnection(this, (FVT10ConnectionRequestInfo) cxRequestInfo, subject);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet,
                                                     Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        FVT10ManagedConnection conn = null;
        Iterator iterator = connectionSet.iterator();
        while (iterator.hasNext()) {
            conn = (FVT10ManagedConnection) iterator.next();
            if (conn.mcf.userName == null && userName == null) {
                return conn;
            } else if (conn.mcf.userName != null && conn.mcf.password != null) {
                if (conn.mcf.userName.equals(userName)
                    && conn.mcf.password.equals(password)) {
                    return conn;
                }
            }
        }
        return conn;
    }

    @Override
    public void setLogWriter(PrintWriter writer) throws ResourceException {
        this.writer = writer;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return writer;
    }

    @Override
    public boolean equals(Object other) {

        if (other instanceof FVT10ManagedConnectionFactory && other != null) {
            if (((FVT10ManagedConnectionFactory) other).userName
                            .equals(userName)
                && ((FVT10ManagedConnectionFactory) other).password
                                .equals(password)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {

        return 0;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName
     *                     the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     *                     the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
