/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.internal;

import org.apache.yoko.orb.spi.naming.Resolver;
import org.omg.CORBA.TRANSIENT;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSHome;

public class StatefulResolverImpl extends Resolver {
    private final BeanMetaData bmd;
    private final int interfaceIndex;

    public StatefulResolverImpl(BeanMetaData bmd, int interfaceIndex) {
        this.bmd = bmd;
        this.interfaceIndex = interfaceIndex;
    }

    @Override
    public org.omg.CORBA.Object resolve() {
        EJSHome home = bmd.homeRecord.getHomeAndInitialize();
        try {
            return (org.omg.CORBA.Object) home.createRemoteBusinessObject(interfaceIndex, null);
        } catch (Exception e) {
            TRANSIENT e2 = new TRANSIENT();
            e2.initCause(e);
            throw e2;
        }
    }
}
