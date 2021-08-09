/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.library.LibraryChangeListener;
import com.ibm.wsspi.library.Library;

/**
 * Configure the arrival of libraries when requested by the class loader.
 */
class GetLibraryAction implements Action {
    enum Availability {
        /** The library will be provided instantly */
        SYNC,
        /** The library will be provided as soon as the {@link LibraryChangeListener} is registered */
        ASYNC,
        /** The library will be provided only when {@link GetLibraryAction#notifyLibraryChangeListener(String)} is invoked */
        DEFERRED
    }

    static final GetLibraryAction NO_LIBS = new GetLibraryAction();

    private final List<String> privateLibs = new ArrayList<String>();
    private final List<String> commonLibs = new ArrayList<String>();

    private final Map<String, Library> libraries = new HashMap<String, Library>();
    private final Map<String, Library> asyncLibs = new HashMap<String, Library>();
    private final Map<String, LibraryChangeListener> deferredListeners = new HashMap<String, LibraryChangeListener>();

    List<String> getPrivateLibs() {
        return privateLibs;
    }

    List<String> getCommonLibs() {
        return commonLibs;
    }

    GetLibraryAction mockCommonLibrary(String id, ClassLoader loader, Availability availability) {
        commonLibs.add(id);
        switch (availability) {
            case SYNC:
                libraries.put(id, new MockSharedLibrary(id, loader));
                break;
            case DEFERRED:
                // create a slot to store the listener when it is registered
                deferredListeners.put(id, null);
                // FALLTHRU
            case ASYNC:
                asyncLibs.put(id, new MockSharedLibrary(id, loader));
                // create slot for this library in main map
                libraries.put(id, null);
                break;
        }
        return this;
    }

    void notifyLibraryChangeListener(String id) {
        // put the library in place so we get it next time.
        libraries.put(id, asyncLibs.get(id));
        LibraryChangeListener libraryChangeListener = deferredListeners.get(id);
        libraryChangeListener.libraryNotification();
    }

    @Override
    public void describeTo(Description desc) {
        desc.appendText("retrieves a library object");
    }

    /**
     * Handle the following method invocations:
     * <ul>
     * <li>{@link BundleContext#registerService(Class, Object, Dictionary)}</li>
     * <li>{@link BundleContext#getServiceReferences(Class, String)}</li>
     * <li>{@link BundleContext#getService(ServiceReference)}</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Invocation invoc) throws Throwable {
        String methodName = invoc.getInvokedMethod().getName();
        if (methodName.equals("registerService"))
            return registerService((Class<LibraryChangeListener>) invoc.getParameter(0),
                                   (LibraryChangeListener) invoc.getParameter(1),
                                   (Dictionary<String, String>) invoc.getParameter(2));

        if (methodName.equals("getServiceReferences"))
            return getServiceReferences((Class<Library>) invoc.getParameter(0),
                                        (String) invoc.getParameter(1));

        if (methodName.equals("getService"))
            return getServiceReference((ServiceReference<Library>) invoc.getParameter(0));

        throw new UnsupportedOperationException(methodName);
    }

    /** mock {@link BundleContext#registerService(Class, Object, Dictionary)} */
    private ServiceRegistration<LibraryChangeListener> registerService(Class<LibraryChangeListener> clazz, LibraryChangeListener listener, Dictionary<String, String> dict) {
        String id = dict.get("library");
        System.out.println("library change listener registered for library " + id);
        // store the listener only if it is for a deferred library
        if (deferredListeners.containsKey(id)) {
            deferredListeners.put(id, listener);
        } else {
            libraries.put(id, asyncLibs.get(id));
            listener.libraryNotification();
        }
        return new MockServiceRegistration<LibraryChangeListener>();

    }

    /** mock {@link BundleContext#getServiceReferences(Class, String)} */
    private Collection<ServiceReference<Library>> getServiceReferences(Class<Library> clazz, String filter) {
        String id = filter.substring("(id=".length(), filter.length() - ")".length());
        Library lib = libraries.get(id);
        if (lib == null) {
            System.out.println("attempt to retrieve service refs for library " + id + " failed");
            return Collections.emptyList();
        }
        ServiceReference<Library> ref = MockServiceReference.wrap(lib);
        System.out.println("attempt to retrieve service refs for library " + id + " succeeded");
        return Collections.singletonList(ref);
    }

    private Library getServiceReference(ServiceReference<Library> ref) {
        System.out.println("Getting service from service ref");
        return ((MockServiceReference<Library>) ref).unwrap();
    }
}