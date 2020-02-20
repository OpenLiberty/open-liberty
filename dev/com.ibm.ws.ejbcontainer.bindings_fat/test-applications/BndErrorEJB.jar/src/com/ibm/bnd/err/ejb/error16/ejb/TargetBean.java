/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.bnd.err.ejb.error16.ejb;

public class TargetBean implements javax.ejb.SessionBean, RemoteTargetBiz {
    private static final long serialVersionUID = 1181647011303722826L;
    private javax.ejb.SessionContext mySessionCtx;

    public javax.ejb.SessionContext getSessionContext() {
        return mySessionCtx;
    }

    @Override
    public void setSessionContext(javax.ejb.SessionContext ctx) {
        mySessionCtx = ctx;
    }

    public void ejbCreate() throws javax.ejb.CreateException {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void ejbRemove() {}

    @Override
    public String echo(String message) {
        return message;
    }

}
