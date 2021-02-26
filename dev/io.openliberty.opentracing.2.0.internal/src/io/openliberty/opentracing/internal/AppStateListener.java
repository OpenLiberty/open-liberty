/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.library.LibraryChangeListener;

@Component(service = { ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = "library=unbound")
public class AppStateListener implements ApplicationStateListener, LibraryChangeListener {

    private static final String LIBRARY = "library";
    private volatile ConcurrentServiceReferenceSet<Library> librarySet = new ConcurrentServiceReferenceSet<Library>(LIBRARY);

    protected void activate(ComponentContext context, Map<String, Object> props) {
        librarySet.activate(context);
        for (ServiceReference<Library> ref : librarySet.references()) {
            registerLibraryListener(context.getBundleContext(), (String) ref.getProperty("id"));
        }
    }

    protected void deactivate(ComponentContext context) {
        librarySet.deactivate(context);
    }

    @Reference(service = Library.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setLibrary(ServiceReference<Library> ref) {
        librarySet.addReference(ref);
    }

    protected void unsetLibrary(ServiceReference<Library> ref) {
        librarySet.removeReference(ref);
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String appName = appInfo.getName();
        OpentracingTracerManager.removeTracer(appName);
    }

    private void registerLibraryListener(BundleContext ctx, String id) {
        Dictionary<String, Object> listenerProps = new Hashtable<String, Object>(1);
        listenerProps.put(LIBRARY, id);
        ctx.registerService(LibraryChangeListener.class, this, listenerProps);
    }

    /** {@inheritDoc} */
    @Override
    public void libraryNotification() {
        OpentracingTracerManager.clearTracers();
    }
}
