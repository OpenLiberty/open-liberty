/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.library.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.config.FilesetChangeListener;
import org.osgi.framework.ServiceRegistration;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

final class FilesetListener implements FilesetChangeListener {
    private static final TraceComponent tc = Tr.register(FilesetListener.class);
    private final SharedLibraryImpl library;
    private final LibraryGeneration generation;
    private final Collection<Fileset> filesets;
    private final Collection<ServiceRegistration<?>> filesetListenerRegs;

    /** The list of PIDs for filesets that have not arrived */
    private volatile Set<? extends String> outstandingPids;

    /** The number of filesets still outstanding */
    // We use a separate count to ensure cleanup on moving to zero happens only once
    private final AtomicInteger numOutstanding;

    @FFDCIgnore(IllegalStateException.class)
    FilesetListener(SharedLibraryImpl library,
                    LibraryGeneration generation,
                    List<? extends String> filesetRefs,
                    Collection<Fileset> filesets,
                    Collection<ServiceRegistration<?>> filesetListenerRegs) {
        this.library = library;
        this.generation = generation;
        this.filesets = filesets;
        this.filesetListenerRegs = filesetListenerRegs;
        outstandingPids = new ConcurrentSkipListSet<String>(filesetRefs);
        numOutstanding = new AtomicInteger(filesetRefs.size());

        // Register this instance as a FilesetChangeListener for all filesets in this library
        for (String filesetPid : outstandingPids) {
            if (generation.isCancelled()) {
                return;
            }
            final Dictionary<String, Object> listenerProps = new Hashtable<String, Object>(1);
            listenerProps.put("fileset", filesetPid);
            final ServiceRegistration<FilesetChangeListener> reg;
            try {
                reg = library.ctx.registerService(FilesetChangeListener.class, this, listenerProps);
                if (generation.isCancelled()) {
                    reg.unregister();
                    return;
                }
            } catch (IllegalStateException e) {
                // This can happen if the classloading bundle is stopped by another thread
                // before registering or unregistering the service above;
                // Ignore this exception and return
                return;
            }
            filesetListenerRegs.add(reg);
        }
    }

    @Override
    public void filesetNotification(String pid, Fileset fileset) {
        if (generation.isCancelled()) {
            return;
        }

        if (numOutstanding.get() == 0) {
            // initial notifications have already arrived
            // so this is just an update to be pushed
            library.notifyListeners();
            return;
        }

        // Still in startup phase, so this is probably an initial notification.

        // Remove the received pid from the list we are still waiting to hear about.
        if (outstandingPids.remove(pid)) {
            // This is an initial notification, so add the fileset to the list.
            filesets.add(fileset);

            // If the filesets are all here, publish this generation.
            if (numOutstanding.decrementAndGet() == 0) {
                outstandingPids = Collections.emptySet();
                library.publishGeneration(generation);
            }
        } else {
            // This was an additional notification, but something was still outstanding
            // so do nothing - the new state will be observed when this method invokes publishGeneration().
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "received additional notification for fileset " + pid + " while still waiting for filesets " + outstandingPids);
            }
        }
    }
}
