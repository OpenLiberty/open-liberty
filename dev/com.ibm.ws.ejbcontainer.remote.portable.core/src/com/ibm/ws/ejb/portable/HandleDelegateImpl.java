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
package com.ibm.ws.ejb.portable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.spi.HandleDelegate;
import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;

import org.omg.CORBA.ORB;
import org.omg.stub.java.rmi._Remote_Stub;

public class HandleDelegateImpl implements HandleDelegate {
    private static final String CLASS_NAME = HandleDelegateImpl.class.getName();
    private static final Logger logger = LoggerHelper.getLogger(CLASS_NAME, "EJBContainer");

    private static final HandleDelegate instance = new HandleDelegateImpl();

    public static HandleDelegate getInstance() {
        return instance;
    }

    protected ORB getORB() throws IOException {
        try {
            return HandleHelper.getORB();
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void writeObject(Object object, ObjectOutputStream out) throws IOException {
        final boolean isTraceOn = logger.isLoggable(Level.FINER);
        if (isTraceOn)
            logger.entering(CLASS_NAME, "writeObject", object);

        // If the output stream is an ORB output stream, then the ORB will
        // marshal the stub as a remote object and unmarshal it by locating a
        // stub via the context class loader.  If it is an application
        // ObjectOutputStream, then we don't know whether the corresponding
        // ObjectInputStream will override resolveClass, so our call to
        // readObject might fail since that method uses caller class loader
        // (our class loader), which won't have visibility to the application
        // stub subclass.
        //
        // Historically, traditional WAS used ORB internals to determine if the output
        // stream was an ORB stream.  If so, the stub was written directly,
        // but if not, object_to_string is used to write and string_to_object
        // is used to read.  However, when string_to_object is used to read,
        // the ORB context is lost, so "the EJSORB" is used, which might be
        // the wrong ORB, which means the object_to_string/string_to_object is
        // not reliable unless the ORB output stream can be reliably detected.
        //
        // For efficiency, we continue to write the stub directly on traditional WAS
        // where we continue to use ORB internals.  On Liberty where we don't
        // want to rely on ORB internals to detect the ORB output stream, we
        // switch to _Remote_Stub, which will maintain the ORB context when
        // writing to an ORB stream, and is a JRE class that can always be
        // loaded regardless of caller class loader when writing to an
        // application ObjectOutputStream.
        Boolean orbOut = HandleHelper.isORBOutputStream(out);
        if (orbOut == null) {
            _Remote_Stub remoteStub = new _Remote_Stub();
            remoteStub._set_delegate(((Stub) object)._get_delegate());

            if (isTraceOn && logger.isLoggable(Level.FINEST))
                logger.logp(Level.FINEST, CLASS_NAME, "writeObject", "writing _Remote_Stub: " + remoteStub);
            out.writeObject(remoteStub);
        } else if (orbOut.booleanValue()) {
            if (isTraceOn && logger.isLoggable(Level.FINEST))
                logger.logp(Level.FINEST, CLASS_NAME, "writeObject", "writing object: " + object);
            out.writeObject(object);
        } else {
            String string = getORB().object_to_string((org.omg.CORBA.Object) object);

            if (isTraceOn && logger.isLoggable(Level.FINEST))
                logger.logp(Level.FINEST, CLASS_NAME, "writeObject", "writing string: " + string);
            out.writeObject(string);
        }

        if (isTraceOn)
            logger.exiting(CLASS_NAME, "writeObject");
    }

    private Object readObject(Class klass, ObjectInputStream in) throws IOException, ClassNotFoundException {
        final boolean isTraceOn = logger.isLoggable(Level.FINER);
        if (isTraceOn)
            logger.entering(CLASS_NAME, "readObject", klass);

        Object object = in.readObject();

        if (object != null) {
            if (object instanceof String) {
                if (isTraceOn && logger.isLoggable(Level.FINEST))
                    logger.logp(Level.FINEST, CLASS_NAME, "readObject", "read string: " + object);
                object = getORB().string_to_object((String) object);
            } else {
                Stub stub = (Stub) object;
                try {
                    ORB orb = stub._orb();
                    if (isTraceOn && logger.isLoggable(Level.FINEST))
                        logger.logp(Level.FINEST, CLASS_NAME, "readObject", "read connected stub: " + orb);
                } catch (Throwable t) {
                    if (isTraceOn && logger.isLoggable(Level.FINEST))
                        logger.logp(Level.FINEST, CLASS_NAME, "readObject", "read disconnected stub: " + t);
                    stub.connect(getORB());
                }
            }

            object = PortableRemoteObject.narrow(object, klass);
        }

        if (isTraceOn)
            logger.exiting(CLASS_NAME, "readObject", object);
        return object;
    }

    public void writeEJBObject(EJBObject object, ObjectOutputStream out) throws IOException {
        writeObject(object, out);
    }

    public EJBObject readEJBObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        return (EJBObject) readObject(EJBObject.class, in);
    }

    public void writeEJBHome(EJBHome home, ObjectOutputStream out) throws IOException {
        writeObject(home, out);
    }

    public EJBHome readEJBHome(ObjectInputStream in) throws ClassNotFoundException, IOException {
        return (EJBHome) readObject(EJBHome.class, in);
    }
}
