/*******************************************************************************
 * Copyright (c) 1998, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.transaction.UserTransaction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A <code>CMStatelessBeanO</code> manages the lifecycle of a
 * single stateless session enterprise bean instance. <p>
 **/
public class CMStatelessBeanO
                extends StatelessBeanO
                implements Serializable
{
    private static final long serialVersionUID = 6553421454354990950L;

    private static final TraceComponent tc = Tr.register(CMStatelessBeanO.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    // d367572 - changed signature and rewrote entire method.
    public CMStatelessBeanO(EJSContainer c, EJSHome h)
    {
        super(c, h);
    }

    /**
     * Beans using container managed transactions are not allowed to
     * call this method
     */
    @Override
    public synchronized UserTransaction getUserTransaction()
    {
        throw new IllegalStateException("UserTransaction not allowed for Stateless with container managed transactions.");
    }

    private void writeObject(ObjectOutputStream oos) // d724504
    throws IOException
    {
        // This should be very rare so the text will not be translated for now,
        // which is consistent with other message text throughout EJB Container.
        String msg = "javax.ejb.SessionContext is not serializable.";
        if (home != null) {
            msg = "A javax.ejb.SessionContext object associated with an instance of the " +
                  home.j2eeName.getComponent() + " bean in the " + home.j2eeName.getModule() +
                  " module in the " + home.j2eeName.getApplication() + " application cannot be serialized." +
                  " An EJBContext object is not serializable and cannot be passed as a parameter on a remote method." +
                  " If an instance of the " + home.j2eeName.getComponent() +
                  " bean has an instance variable of the SessionContext or EJBContext type, then the bean is also not serializable.";
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "writeObject: throw NotSerializableException : " + msg);
        throw new NotSerializableException(msg);
    }

    private void readObject(ObjectInputStream ois) // d724504
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "readObject: no-op");

        // This object implements Serializable for backwards compatibility only.
        // The BPC stack product mistakenly serialized instances of this object
        // within another object.  The outer object needs to be deserializable
        // even though the instance of this class is never used.
    }
}
