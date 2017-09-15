/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import javax.ejb.EJBObject;
import javax.ejb.Handle;

import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.StatefulSessionHandleFactory;
import com.ibm.ws.ejb.portable.HandleImpl;

public class SessionHandleFactoryImpl implements StatefulSessionHandleFactory {
    @Override
    public Handle create(EJBObject object) throws CSIException {
        return new HandleImpl(object);
    }
}
