/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer;

/**
 * The EJBStoppedException is thrown by the Enterprise Java Bean (EJB)
 * container when an attempt is made to access either an EJB instance
 * or EJB home that has been previously stopped. Once an application
 * has been stopped, references to bean instances or homes are
 * no longer valid. <p>
 * 
 * When an application is started again, any local references obtained
 * using an earlier running version of the application must be replaced.
 * Generally, every time an application is started, a new classloader
 * instance is used, so local references obtained for an application
 * that was loaded with one classloader will not be compatible with
 * references obtained for the same application started later with a
 * different classloader. <p>
 * 
 * Remote references will not work when no version of the application
 * is running, but will resume working when a new version of the application
 * is started, assume no incompatible interface changes have been made. <p>
 * 
 * Note: an EJBStoppedException would not be returned directly to an
 * application, but would be nested within the appropriate exception
 * required by the EJB Specification for the type of EJB access being
 * attempted. For example, an attempt to use a reference to an EJB local
 * business interface would result in a javax.ejb.NoSuchEJBException. <p>
 **/
public class EJBStoppedException extends RuntimeException
{
    private static final long serialVersionUID = -7810803436806975490L;

    /**
     * Constructs a new EJBStoppedException; no message.
     **/
    public EJBStoppedException()
    {
        super();
    }

    /**
     * Constructs a new EJBStoppedException with the specified detail
     * message.
     **/
    public EJBStoppedException(String detailMessage)
    {
        super(detailMessage);
    }
}
