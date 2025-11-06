/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.classloading.internal.providers;

import java.util.EnumSet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration.LibraryPrecedence;
import com.ibm.ws.classloading.internal.LibertyLoader;
import com.ibm.ws.classloading.internal.util.BlockingList.Listener;
import com.ibm.ws.classloading.internal.util.BlockingList.Retriever;
import com.ibm.ws.classloading.internal.util.BlockingList.Slot;
import com.ibm.ws.classloading.internal.util.ElementNotReadyException;
import com.ibm.ws.classloading.internal.util.ElementNotValidException;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.library.Library;

/**
 * This listener is static (i.e. a nested class rather than an inner class).
 * It holds no references to the AppClassLoader object, so it cannot prevent
 * the AppClassLoader from being collected.
 */
public class GetLibraryLoaders implements Retriever<String, Providers.LoaderInfo>, Listener<String, Providers.LoaderInfo> {

    static final TraceComponent tc = Tr.register(GetLibraryLoaders.class);
    private final EnumSet<ApiType> ownerAPIs;
    private final String ownerID;
    private final LibraryPrecedence precedence;

    /** Create a listener that does not listen straight away */
    GetLibraryLoaders(String ownerId, EnumSet<ApiType> ownerAPIs, LibraryPrecedence precedence) {
        this.ownerID = ownerId;
        this.ownerAPIs = ownerAPIs;
        this.precedence = precedence;
    }

    @Override
    public Providers.LoaderInfo fetch(String id) throws ElementNotReadyException, ElementNotValidException {
        Library lib = Providers.getSharedLibrary(id);
        if (lib == null)
            throw new ElementNotReadyException(id);
        if (libraryAndLoaderApiTypesDoNotMatch(lib))
            throw new ElementNotValidException();
        return new Providers.LoaderInfo((LibertyLoader) lib.getClassLoader(), precedence);
    }

    /** invoked by the blocking list when a synchronous fetch operation fails */
    @Override
    public void listenFor(final String libraryId, final Slot<? super Providers.LoaderInfo> slot) {
        // Create a shared library listener
        new AbstractLibraryListener(libraryId, ownerID, Providers.bundleContext) {
            @Override
            void update() {
                final String methodName = "update(): ";
                // the shared library has arrived! retrieve its class loader
                Library library = Providers.getSharedLibrary(libraryId);
                if (library == null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + "class loader " + ownerID + "received a notification from the shared library " + libraryId
                                     + " but the library could not be retrieved.");
                    return; // do nothing, not even deregister!
                }

                if (libraryAndLoaderApiTypesDoNotMatch(library)) {
                    // The owning class loader's declared API types do not match those for this library.
                    // We have no choice but to invalidate this element - already logged in apiTypesMatch()
                    slot.delete();
                } else {
                    final LibertyLoader libCL = (LibertyLoader) library.getClassLoader();
                    slot.fill(new Providers.LoaderInfo(libCL, precedence));
                }
                deregister();
            }
        };
    }

    private boolean libraryAndLoaderApiTypesDoNotMatch(Library sharedLibrary) {
        return !!!Providers.checkAPITypesMatch(sharedLibrary, ownerID, ownerAPIs);
    }
}