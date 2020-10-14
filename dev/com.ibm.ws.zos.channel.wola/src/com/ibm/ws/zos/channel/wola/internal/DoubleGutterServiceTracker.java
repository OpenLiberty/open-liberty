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
package com.ibm.ws.zos.channel.wola.internal;

import java.nio.ByteBuffer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.zos.core.utils.DoubleGutter;

/**
 * OSGI service tracker for DoubleGutter.
 *
 * TODO: Why is DoubleGutter a DS anyway?
 *
 */
public class DoubleGutterServiceTracker extends ServiceTracker<DoubleGutter, DoubleGutter> {

    /**
     * Static instance for simplicity.
     */
    static DoubleGutterServiceTracker staticInstance;

    /**
     * CTOR.
     */
    public DoubleGutterServiceTracker() {
        super(getBundleContext(), DoubleGutter.class, null);
    }

    /**
     * @return The BundleContext for the SecurityService class (in the com.ibm.ws.security bundle).
     */
    private static BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(DoubleGutter.class).getBundleContext();
    }

    /**
     * Open the tracker.
     *
     * @return this
     */
    protected DoubleGutterServiceTracker openMe() {
        open();
        return this;
    }

    /**
     * @return the static singleton instance of this class.
     */
    protected static DoubleGutterServiceTracker getInstance() {
        if (staticInstance == null) {
            staticInstance = new DoubleGutterServiceTracker().openMe();
        }
        return staticInstance;
    }

    /**
     * @return the DoubleGutter service impl tracked by this tracker.
     */
    protected static DoubleGutter get() {
        DoubleGutter retMe = getInstance().getService();
        return (retMe != null) ? retMe : new NoopDoubleGutter();
    }
}

/**
 * NO-OP impl, in case the real one isn't available.
 */
class NoopDoubleGutter implements DoubleGutter {

    /** {@inheritDoc} */
    @Override
    public String asDoubleGutter(long address, byte[] data) {
        return "NoopDoubleGutter";
    }

    /** {@inheritDoc} */
    @Override
    public String asDoubleGutter(long address, ByteBuffer data) {
        return "NoopDoubleGutter";
    }

}
