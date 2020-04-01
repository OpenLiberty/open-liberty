/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.derbyra.resourceadapter;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.TransactionSupport;
import javax.security.auth.Subject;

public class DerbyManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation, TransactionSupport {
    private static final long serialVersionUID = 7834485368743035738L;

    transient DerbyResourceAdapter adapter;
    private transient boolean dissociatable;
    private transient boolean lazyEnlistable;
    transient String password; // confidential config-property
    transient String userName; // config-property
    private transient boolean exceptionOnDestroy; // config-property
    transient String qmid; // config-property
    private transient int xaSuccessLimit; // config-property
    transient AtomicInteger xaSuccessLimitCountDown;

    /** {@inheritDoc} */
    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new DerbyConnectionFactory(cm, this);
    }

    /** {@inheritDoc} */
    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        if (dissociatable)
            if (lazyEnlistable)
                return new DerbyDissociatableLazyEnlistableManagedConnection(this, (DerbyConnectionRequestInfo) cri, subject);
            else
                return new DerbyDissociatableManagedConnection(this, (DerbyConnectionRequestInfo) cri, subject);
        else if (lazyEnlistable)
            return new DerbyLazyEnlistableManagedConnection(this, (DerbyConnectionRequestInfo) cri, subject);
        else
            return new DerbyManagedConnection(this, (DerbyConnectionRequestInfo) cri, subject);
    }

    /** {@inheritDoc} */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        try {
            return adapter.xaDataSource.getLogWriter();
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    String getPassword() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    /**
     * @see javax.resource.spi.TransactionSupport#getTransactionSupport()
     */
    @Override
    public TransactionSupportLevel getTransactionSupport() {
        return TransactionSupportLevel.XATransaction;
    }

    String getUserName() {
        return userName;
    }

    public int getXaSuccessLimit() {
        return xaSuccessLimit;
    }

    public boolean isDissociatable() {
        return dissociatable;
    }

    public boolean isLazyEnlistable() {
        return lazyEnlistable;
    }

    public boolean getExceptionOnDestroy() {
        return this.exceptionOnDestroy;
    }

    private static final boolean match(Object o1, Object o2) {
        return o1 == o2 || o1 != null && o1.equals(o2);
    }

    /** {@inheritDoc} */
    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set set, Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        for (Object o : set)
            if (o instanceof DerbyManagedConnection) {
                DerbyManagedConnection m = (DerbyManagedConnection) o;
                if (match(m.cri, cri) && match(m.subject, subject))
                    return m;
            }
        return null;
    }

    public void setDissociatable(boolean dissociatable) {
        this.dissociatable = dissociatable;
    }

    public void setExceptionOnDestroy(boolean exceptionOnDestroy) {
        this.exceptionOnDestroy = exceptionOnDestroy;
    }

    public void setLazyEnlistable(boolean lazyEnlistable) {
        this.lazyEnlistable = lazyEnlistable;
    }

    /** {@inheritDoc} */
    @Override
    public void setLogWriter(PrintWriter logwriter) throws ResourceException {
        try {
            adapter.xaDataSource.setLogWriter(logwriter);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setQmid(String qmid) {
        this.qmid = qmid;
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (DerbyResourceAdapter) adapter;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setXaSuccessLimit(int xaSuccessLimit) {
        this.xaSuccessLimit = xaSuccessLimit;
        xaSuccessLimitCountDown = xaSuccessLimit >= 0 ? new AtomicInteger(xaSuccessLimit) : null;
    }
}
