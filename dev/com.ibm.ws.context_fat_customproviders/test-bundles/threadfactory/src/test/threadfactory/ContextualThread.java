/*******************************************************************************
 * Copyright (c) 2012,2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.threadfactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ThreadFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.serialization.DeserializationObjectInputStream;

import test.threadfactory.internal.ThreadFactoryImpl;

public class ContextualThread extends Thread implements Serializable {
    private static final long serialVersionUID = -1341304323295245963L;

    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
                                                                                                new ObjectStreamField("contextualRunnable", byte[].class),
                                                                                                new ObjectStreamField("threadFactoryID", String.class)
    };

    private Runnable contextualRunnable;
    private String threadFactoryID;

    public ContextualThread(String id, Runnable r) {
        threadFactoryID = id;
        contextualRunnable = r;
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        GetField fields = in.readFields();
        byte[] bytes = (byte[]) fields.get("contextualRunnable", null);
        threadFactoryID = (String) fields.get("threadFactoryID", null);

        try {
            // Need to deserialize from the ContextService's class loader
            ClassLoader contextSvcClassLoader = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
                @Override
                public ClassLoader run() throws Exception {
                    BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
                    ServiceReference<ThreadFactory> threadFactoryRef = null;
                    try {
                        threadFactoryRef = bundleContext.getServiceReferences(ThreadFactory.class, "(id=" + threadFactoryID + ")").iterator().next();
                        ThreadFactoryImpl threadFactory = (ThreadFactoryImpl) bundleContext.getService(threadFactoryRef);
                        return threadFactory.contextSvc.getClass().getClassLoader();
                    } finally {
                        if (threadFactoryRef != null)
                            bundleContext.ungetService(threadFactoryRef);
                    }
                }
            });

            ObjectInputStream oin = new DeserializationObjectInputStream(new ByteArrayInputStream(bytes), contextSvcClassLoader);
            contextualRunnable = (Runnable) oin.readObject();
            oin.close();
        } catch (PrivilegedActionException x) {
            throw new IOException(x.getCause());
        }
    }

    @Override
    public void run() {
        contextualRunnable.run();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(contextualRunnable);
        oout.flush();
        byte[] bytes = bout.toByteArray();
        oout.close();

        PutField fields = out.putFields();
        fields.put("contextualRunnable", bytes);
        fields.put("threadFactoryID", threadFactoryID);
        out.writeFields();
    }
}
