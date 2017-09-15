/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.ws.csi.DispatchEventListenerCookie;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;

public class BeanOCallDispatchToken extends Object {

    private DispatchEventListenerCookie[] dispatchEventListenerCookies = null;
    private EJBMethodMetaData methodMetaData = null;
    private boolean doAfterDispatch = false;

    /** Creates new BeanOCallDispatchToken */
    public BeanOCallDispatchToken() {}

    public void setDispatchEventListenerCookies(DispatchEventListenerCookie[] c) {
        dispatchEventListenerCookies = c;
    }

    public DispatchEventListenerCookie[] getDispatchEventListenerCookies() {
        return dispatchEventListenerCookies;
    }

    public void setMethodMetaData(EJBMethodMetaData m) {
        methodMetaData = m;
    }

    public EJBMethodMetaData getMethodMetaData() {
        return methodMetaData;
    }

    public void setDoAfterDispatch(boolean v) {
        doAfterDispatch = v;
    }

    public boolean getDoAfterDispatch() {
        return doAfterDispatch;
    }

}
