/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package library.listener;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.library.LibraryChangeListener;

/**
 * Keeps track of all shared libraries - registering an instance of MyLibraryChangeListener
 * when they are created.
 */
@Component(immediate = true)
public class LibraryTracker {
    private static final Logger log = Logger.getLogger(LibraryTracker.class.getName());

    private BundleContext bundleContext;
    private final Map<Library, ServiceRegistration<LibraryChangeListener>> listenerServices =
                    new HashMap<Library, ServiceRegistration<LibraryChangeListener>>();
    private final List<Library> preActivateLibraries = new ArrayList<Library>();

    @Activate
    protected synchronized void activate(ComponentContext cCtx, Map<String, Object> properties) {
        bundleContext = cCtx.getBundleContext();
        log.info("activate");
        for (Library library : preActivateLibraries) {
            setLibrary(library);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setLibrary(Library library) {
        // If this component is not yet activated, we queue up the libraries to process
        // at activation time.
        if (bundleContext != null) { //not activated yet
            LibraryChangeListener listener = new MyLibraryChangeListener(library);
            log.info("new LibraryChangeListener: " + listener);
            // Register the listener for library change notifications
            Dictionary<String, Object> listenerProps = new Hashtable<String, Object>(1);
            listenerProps.put("library", library.id());
            ServiceRegistration<LibraryChangeListener> svcReg =
                            bundleContext.registerService(LibraryChangeListener.class, listener, listenerProps);
            listenerServices.put(library, svcReg);
        } else {
            preActivateLibraries.add(library);
        }
    }

    protected synchronized void unsetLibrary(Library library) {
        if (bundleContext != null) {
            ServiceRegistration<LibraryChangeListener> svcReg = listenerServices.remove(library);
            svcReg.unregister();
            log.info("removed Library: " + library);
        } else {
            preActivateLibraries.remove(library);
        }
    }
}
