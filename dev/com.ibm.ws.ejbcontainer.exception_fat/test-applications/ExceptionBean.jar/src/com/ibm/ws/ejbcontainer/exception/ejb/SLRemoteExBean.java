/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import javax.ejb.LocalHome;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Enterprise Bean: SLRemoteExBean
 */
@Stateless
@RemoteHome(SLRemoteExHome.class)
@LocalHome(SLRemoteExLocalHome.class)
public class SLRemoteExBean implements SessionBean {
    private static final long serialVersionUID = -2207952128826591889L;
    private static final String CLASS_NAME = SLRemoteExBean.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

    public void testMethodwithNoEx(String exceptionToThrow) {
        logger.info(getClass().getSimpleName() + ".testMethodwithNoEx : " + exceptionToThrow);
        if ("RuntimeException".equals(exceptionToThrow)) {
            throw new RuntimeException(exceptionToThrow);
        } else if ("IllegalStateException".equals(exceptionToThrow)) {
            throw new IllegalStateException(exceptionToThrow);
        }
    }

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
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    public void testMethodwithExceptionAndRemote(String exceptionToThrow) throws Exception, RemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithExceptionAndRemote : " + exceptionToThrow);
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
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    public void testMethodwithExceptionAndRemoteSub(String exceptionToThrow) throws Exception, SLRemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithExceptionAndRemoteSub : " + exceptionToThrow);
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
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    public void testMethodwithExceptionAndRemoteAndRemoteSub(String exceptionToThrow) throws Exception, RemoteException, SLRemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithExceptionAndRemoteAndRemoteSub : " + exceptionToThrow);
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
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
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
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    public void testMethodwithIOExceptionAndRemote(String exceptionToThrow) throws IOException, RemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithIOExceptionAndRemote : " + exceptionToThrow);
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
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    public void testMethodwithIOExceptionAndRemoteSub(String exceptionToThrow) throws IOException, SLRemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithIOExceptionAndRemoteSub : " + exceptionToThrow);
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
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
        }
    }

    public void testMethodwithIOExceptionAndRemoteAndRemoteSub(String exceptionToThrow) throws IOException, RemoteException, SLRemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithIOExceptionAndRemoteAndRemoteSub : " + exceptionToThrow);
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
        } else if ("SLRemoteException".equals(exceptionToThrow)) {
            throw new SLRemoteException(exceptionToThrow);
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

    public void testMethodwithRemoteExSub(String exceptionToThrow) throws SLRemoteException {
        logger.info(getClass().getSimpleName() + ".testMethodwithRemoteExSub : " + exceptionToThrow);
        if ("RuntimeException".equals(exceptionToThrow)) {
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
