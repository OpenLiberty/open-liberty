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
package com.ibm.ws.artifact.url.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.aries.util.tracker.RecursiveBundleTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import com.ibm.ws.artifact.url.URLService;

/**
 *
 */
public class URLStreamHandlerServiceImpl extends AbstractURLStreamHandlerService implements URLService, BundleTrackerCustomizer<String> {
    /** {@inheritDoc} */
    @Override
    public URLConnection openConnection(URL u) throws IOException {
        return new NotABundleResourceURLConnection(u);
    }

    RecursiveBundleTracker rbt;

    public void activate(ComponentContext ctx) {
        rbt = new RecursiveBundleTracker(ctx.getBundleContext(), Bundle.STOPPING, this);
        rbt.open();
    }

    public void deactivate(ComponentContext ctx) {
        rbt.close();
    }

    public URL convertURL(URL urlToConvert, Bundle owningBundle) {
        return NotABundleResourceURLConnection.addURL(urlToConvert, owningBundle);
    }

    /** {@inheritDoc} */
    @Override
    public String addingBundle(Bundle bundle, BundleEvent event) {
        //we are invoked on addingBundle, whenever a bundle enters the STOPPING state.
        //we use this notification to remove any redirecting urls for the bundle.
        NotABundleResourceURLConnection.forgetBundle(bundle);
        return null; //don't need to track.. 
    }

    /** {@inheritDoc} */
    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, String object) {}

    /** {@inheritDoc} */
    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, String object) {}
}
