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
package com.ibm.ws.classloading.internal.providers;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.library.LibraryChangeListener;

/**
 * This listener receives notification of any changes to shared
 * libraries. It calls the classloader corresponding to this
 * listener and adds the appropriate files to the classpath.
 * 
 * To prevent this class from stopping the classloader from being
 * garbage collected we maintain a weak reference to the classloader
 * only. Since there is no specific event to signal that this class
 * is no longer needed we use Reference Queue to keep an eye on the
 * underlying classloader. When a new copy of this class is
 * requested we can check the queue to see if any classloaders
 * have been removed. If they have then we can call any other change
 * listeners that refer to that classloader and call their deregister
 * methods.
 */
abstract class AbstractLibraryListener implements LibraryChangeListener {

    private volatile ServiceRegistration<LibraryChangeListener> listenerReg;

    AbstractLibraryListener(String libraryID, String loaderID, BundleContext ctx) {
        listenerReg = registerListener(this, libraryID, loaderID, ctx);
    }

    static ServiceRegistration<LibraryChangeListener> registerListener(LibraryChangeListener listener, String libraryID, String loaderID, BundleContext ctx) {
        // Register this listener to wait for library change notifications
        Dictionary<String, Object> listenerProps = new Hashtable<String, Object>(1);
        listenerProps.put("library", libraryID);
        listenerProps.put("classloader", loaderID);
        return ctx.registerService(LibraryChangeListener.class, listener, listenerProps);
    }

    /** {@inheritDoc} */
    @Override
    public void libraryNotification() {
        update();
    }

    /**
     * This method is called by the libraryL listener when an update occurs
     * This could be the arrival of the library into the service registry.
     * Or it could be the addition or modification of files within a Fileset
     */
    abstract void update();

    void deregister() {
        Object lock = listenerReg;
        if (lock == null)
            return;
        synchronized (lock) {
            if (listenerReg == null)
                return;
            listenerReg.unregister();
            listenerReg = null;
        }
    }
}
