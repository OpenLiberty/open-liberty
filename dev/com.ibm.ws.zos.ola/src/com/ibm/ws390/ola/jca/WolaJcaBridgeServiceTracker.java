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
package com.ibm.ws390.ola.jca;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.zos.channel.wola.WolaJcaBridge;


/**
 * For getting WolaJcaBridge service references from the OSGi registry.
 */
public class WolaJcaBridgeServiceTracker extends ServiceTracker<WolaJcaBridge, WolaJcaBridge> {

    /**
     * CTOR.
     */
    public WolaJcaBridgeServiceTracker() {
        super( getBundleContext(), WolaJcaBridge.class, null);
    }
    
    /**
     * @return The BundleContext for the WolaJcaBridge class (in the wola bundle).
     */
    private static BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(WolaJcaBridge.class).getBundleContext();
    }
    
    /**
     * Open the tracker.
     * 
     * @return this
     */
    protected WolaJcaBridgeServiceTracker openMe() {
        open();
        return this;
    }
        
}
