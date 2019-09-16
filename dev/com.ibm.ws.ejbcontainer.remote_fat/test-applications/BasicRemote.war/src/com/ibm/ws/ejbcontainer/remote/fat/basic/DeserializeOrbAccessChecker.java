/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.basic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.ejb.spi.HandleDelegate;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.omg.CORBA.ORB;

/**
 * Used to test that java:comp/ORB and java:comp/HandleDelegate are available
 * when marshaling an object on a remote method call.
 */
public class DeserializeOrbAccessChecker implements Serializable {

    private static final long serialVersionUID = -1496964361674188800L;

    private transient ORB orb;
    private transient HandleDelegate handleDelegate;
    private transient String exceptionMessage;

    public void verifyState() {
        if (exceptionMessage != null) {
            throw new IllegalStateException(exceptionMessage);
        }
        if (orb == null) {
            throw new IllegalStateException("ORB is null");
        }
        if (handleDelegate == null) {
            throw new IllegalStateException("HandleDelegate is null");
        }
    }

    public DeserializeOrbAccessChecker() {
        try {
            Context context = new InitialContext();
            orb = (ORB) context.lookup("java:comp/ORB");
            handleDelegate = (HandleDelegate) context.lookup("java:comp/HandleDelegate");
        } catch (NamingException ex) {
            exceptionMessage = ex.getClass().getSimpleName() + " : " + ex.getMessage();
            ex.printStackTrace(System.out);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(orb != null);
        out.writeBoolean(handleDelegate != null);
        out.writeObject(exceptionMessage);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        boolean getORB = in.readBoolean();
        boolean getHandleDelegate = in.readBoolean();
        exceptionMessage = (String) in.readObject();

        try {
            Context context = new InitialContext();
            if (getORB) {
                orb = (ORB) context.lookup("java:comp/ORB");
            }
            if (getHandleDelegate) {
                handleDelegate = (HandleDelegate) context.lookup("java:comp/HandleDelegate");
            }
        } catch (NamingException ex) {
            exceptionMessage = ex.getClass().getSimpleName() + " : " + ex.getMessage();
            ex.printStackTrace(System.out);
        }
    }

}
