/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.csi;

import com.ibm.ws.ejbcontainer.EJBMethodMetaData;

public interface DispatchEventListener {

    public final int BEGIN_DISPATCH = 1;
    public final int BEFORE_EJBACTIVATE = 2;
    public final int AFTER_EJBACTIVATE = 3;
    public final int BEFORE_EJBLOAD = 4;
    public final int AFTER_EJBLOAD = 5;
    public final int BEFORE_EJBMETHOD = 6;
    public final int AFTER_EJBMETHOD = 7;
    public final int BEFORE_EJBSTORE = 8;
    public final int AFTER_EJBSTORE = 9;
    public final int BEFORE_EJBPASSIVATE = 10;
    public final int AFTER_EJBPASSIVATE = 11;
    public final int END_DISPATCH = 12;
    // following array must be ordered according to preceeding list of integers 
    public final String[] METHOD_NAME = { "beginDispatch", "beforeEjbActivate", "afterEjbActivate", "beforeEjbLoad", "afterEjbLoad", "beforeEjbMethod", "afterEjbMethod",
                                         "beforeEjbStore", "afterEjbStore", "beforeEjbPassivate", "afterEjbPassivate", "endDispatch" };

    public boolean initialize() throws Exception; /* true= recorder enabled; false=recorder disabled - if disabled, recorder will not be called again */

    public DispatchEventListenerCookie beginDispatch(EJBMethodMetaData info) throws Exception;

    public void beforeEjbActivate(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void afterEjbActivate(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void beforeEjbLoad(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void afterEjbLoad(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void beforeEjbMethod(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void afterEjbMethod(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void beforeEjbStore(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void afterEjbStore(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void beforeEjbPassivate(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void afterEjbPassivate(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

    public void endDispatch(EJBMethodMetaData info, DispatchEventListenerCookie cookie) throws Exception;

}
