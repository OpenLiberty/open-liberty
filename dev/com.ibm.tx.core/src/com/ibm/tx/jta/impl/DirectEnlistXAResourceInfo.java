/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.impl;

import java.io.Serializable;

import javax.transaction.xa.XAResource;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

public class DirectEnlistXAResourceInfo implements Serializable {
    private static final long serialVersionUID = -6971618853657075544L;

    private static final TraceComponent tc = Tr.register(DirectEnlistXAResourceInfo.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private final transient XAResource _xaResource;
    private final Serializable serializableXaResource;
    private final String rmName = "Directly enlisted XA Resource";

    public DirectEnlistXAResourceInfo(XAResource xaResource) {
        _xaResource = xaResource;
        // If our resource can be serialized, make sure it is, 
        // but otherwise allow serialization to happen without exception
        if (_xaResource instanceof Serializable) {
            serializableXaResource = (Serializable) _xaResource;
        } else {
            serializableXaResource = null;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Transaction recovery is not possible because the resource {0} is not serializable.", xaResource);
            }
        }
    }

    public String getRMName() {
        return rmName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof DirectEnlistXAResourceInfo)) {
            return false;
        } else {
            DirectEnlistXAResourceInfo otherDirectEnlistXAResource = (DirectEnlistXAResourceInfo) o;
            if (!rmName.equals(otherDirectEnlistXAResource.rmName)) {
                return false;
            }
            // Only compare the serializable xa resource, since the other 
            // won't get reconstituted after deserialization, and we want to 
            // claim to be the same after serialization as before
            if (serializableXaResource != null && !(serializableXaResource.equals(otherDirectEnlistXAResource.serializableXaResource))) {
                return false;
            }
        }

        return true;

    }

    @Override
    public int hashCode() {
        // Having over-ridden equals we should also override hashCode such that if a.equals(b) then
        // a.hashCode() == b.hashCode(). However, this is easier said than done as this class uses
        // the isSameRM method of the wrapped XAResource instance to determine equality and there's
        // no guarantee that if a.isSameRM(b) returns true that a.hashCode() == b.hashCode().
        //
        // We could probably jump through lots of hoops and have a complex hashing algorithm to 
        // work around this problem but this class is internal to the TM and as such we can be 
        // certain that it'll never be used as a key in e.g. a HashMap so I don't believe it's
        // worth it.
        //
        // I (Andy) am overriding this method simply to show that this has been thought about and,
        // hopefully, make problem determination easier in the future should this class ever wind
        // up being used as a key in a HashMap.
        return super.hashCode();
    }

    public XAResource getXAResource() {
        // Return the thing which survives serialization if we can
        if (serializableXaResource != null) {
            return (XAResource) serializableXaResource;
        } else {
            // Otherwise return the thing we get initialized with
            return _xaResource;
        }
    }

}
