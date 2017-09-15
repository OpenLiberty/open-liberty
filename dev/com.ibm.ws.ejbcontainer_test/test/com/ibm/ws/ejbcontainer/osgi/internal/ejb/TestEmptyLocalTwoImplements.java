/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejb;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TimedObject;
import javax.ejb.Timer;

@Stateless
@Local
public class TestEmptyLocalTwoImplements implements Serializable, Externalizable, TimedObject, LocalIntf1, LocalIntf2 {
    @Override
    public void writeExternal(ObjectOutput out) {}

    @Override
    public void readExternal(ObjectInput in) {}

    @Override
    public void ejbTimeout(Timer timer) {}
}
