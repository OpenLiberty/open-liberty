/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.exception.ejb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Remote Enterprise Bean: SLRemoteEx
 */
@Stateless
@RemoteHome(SLRemoteExHome.class)
public class SLRemoteExBean implements SessionBean {
    private static final long serialVersionUID = -2207952128826591889L;
    private static final String CLASS_NAME = SLRemoteExBean.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

    public void testMethodwithException(String exceptionToThrow) throws Exception {
        logger.info(getClass().getSimpleName() + ".testMethodwithException : " + exceptionToThrow);
        if ("Exception".equals(exceptionToThrow)) {
            throw new Exception(exceptionToThrow);
        } else if ("IOException".equals(exceptionToThrow)) {
            throw new IOException(exceptionToThrow);
        } else if ("RemoteException".equals(exceptionToThrow)) {
            throw new RemoteException(exceptionToThrow);
        } else if ("RuntimeException".equals(exceptionToThrow)) {
            throw new RuntimeException(exceptionToThrow);
        } else if ("IllegalStateException".equals(exceptionToThrow)) {
            throw new IllegalStateException(exceptionToThrow);
        } else if ("SQLException".equals(exceptionToThrow)) {
            throw new SQLException(exceptionToThrow);
        }
    }

    public void testMethodwithIOException(String exceptionToThrow) throws IOException {
        logger.info(getClass().getSimpleName() + ".testMethodwithIOException : " + exceptionToThrow);
        if ("IOException".equals(exceptionToThrow)) {
            throw new IOException(exceptionToThrow);
        } else if ("RemoteException".equals(exceptionToThrow)) {
            throw new RemoteException(exceptionToThrow);
        } else if ("RuntimeException".equals(exceptionToThrow)) {
            throw new RuntimeException(exceptionToThrow);
        } else if ("IllegalStateException".equals(exceptionToThrow)) {
            throw new IllegalStateException(exceptionToThrow);
        } else if ("FileNotFoundException".equals(exceptionToThrow)) {
            throw new FileNotFoundException(exceptionToThrow);
        }
    }

    public void testMethodwithRemoteEx(String exceptionToThrow) throws RemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithRemoteEx : " + exceptionToThrow);
        if ("RemoteException".equals(exceptionToThrow)) {
            throw new RemoteException(exceptionToThrow);
        } else if ("RuntimeException".equals(exceptionToThrow)) {
            throw new RuntimeException(exceptionToThrow);
        } else if ("IllegalStateException".equals(exceptionToThrow)) {
            throw new IllegalStateException(exceptionToThrow);
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    public void testMethodwithExceptionAndRemote(String exceptionToThrow) throws Exception, RemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithExceptionAndRemote : " + exceptionToThrow);
        if ("RemoteException".equals(exceptionToThrow)) {
            throw new RemoteException(exceptionToThrow);
        } else if ("RuntimeException".equals(exceptionToThrow)) {
            throw new RuntimeException(exceptionToThrow);
        } else if ("IllegalStateException".equals(exceptionToThrow)) {
            throw new IllegalStateException(exceptionToThrow);
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    public void testMethodwithRemoteExAndSub(String exceptionToThrow) throws RemoteException, SLRemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithRemoteExAndSub : " + exceptionToThrow);
        if ("RemoteException".equals(exceptionToThrow)) {
            throw new RemoteException(exceptionToThrow);
        } else if ("RuntimeException".equals(exceptionToThrow)) {
            throw new RuntimeException(exceptionToThrow);
        } else if ("IllegalStateException".equals(exceptionToThrow)) {
            throw new IllegalStateException(exceptionToThrow);
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
    }

    @Override
    public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {
    }
}
