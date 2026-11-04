/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.odi;

import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.interrupt.InterruptObject;
import com.ibm.websphere.interrupt.InterruptibleThreadInfrastructure;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.zos.channel.wola.WolaInterruptObjectBridge;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * When both requestTiming-1.0 and zosLocalAdapters-1.0 are configured, this
 * class will create and register InterruptObject instances for WOLA requests.
 */
@Component(name = "com.ibm.ws.zos.channel.wola.odi.WolaInterruptObjectBridgeImpl", configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class WolaInterruptObjectBridgeImpl implements WolaInterruptObjectBridge {

    private InterruptibleThreadInfrastructure iti = null;
    private NativeMethodManager nativeMethodManager = null;

    @Reference
    protected void setInterruptibleThreadInfrastructure(InterruptibleThreadInfrastructure iti) {
        this.iti = iti;
    }

    protected void unsetInterruptibleThreadInfrastructure(InterruptibleThreadInfrastructure iti) {
        this.iti = null;
    }

    @Reference
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = null;
    }

    @Activate
    protected void activate() {
        // -------------------------------------------------------------------
        // Register the native methods that we'll call when we cancel a WOLA
        // request.
        // -------------------------------------------------------------------
        nativeMethodManager.registerNatives(WolaInterruptObjectBridgeImpl.class);
    }

    /**
     * The native code calls this method to register an InterruptObject with the ITI
     * just before it calls otma_send_receivex. After calling otma_send_receivex, it
     * will wait on an ECB for IMS to finish, and then call deregister on this class.
     * There is an expectation that either the IMS request will finish and we'll be
     * able to deregister the InterruptObject, or that the InterruptObject will be
     * driven for interrupt and will cancel the IMS request. Part of cancelling the IMS
     * request involves posting the ECB. Since we provide the storage for the ECB,
     * this introduces some timing windows. We must be sure that the memory used for
     * the ECB remains allocated so that either IMS or the InterruptObject can post it.
     *
     * Since all access to and from the InterruptObject happen thru this class, it will
     * be the serialization point to try and make sure someone isn't using the ECB after
     * it's been deallocated. Calling this method creates the InterruptObject and gives
     * it access to the ECB. Calling deregister will serialize on the InterruptObject
     * and attempt to cancel it internally before calling deregister on the ITI. This is
     * so that if the ITI has just driven the InterruptObject, we will wait until the
     * interrupt is finished before driving cancel, and we'll see that it's already been
     * driven so there is nothing to do. Similarly, the InterruptObject, when driven by
     * the ITI, can see that it's cancelled and return immediately if it fires just after
     * it was cancelled.
     *
     * Once the InterruptObject is cancelled, the ECB can be freed.
     */
    @Override
    public Object registerOtma(long otmaAnchor, long sessionId, int ecbPtr) {
        InterruptObject odi = null;

        if (iti.isODISupported()) {
            odi = new WolaOtmaInterruptObjectImpl(this, otmaAnchor, sessionId, ecbPtr);

            try {
                iti.register(odi);
            } catch (Throwable t) {
                FFDCFilter.processException(t, this.getClass().getName(), "74");
                odi = null;
            }
        }

        return odi;
    }

    /** {@inheritDoc} */
    @Override
    public Object register(byte[] wolaGroupBytes, byte[] registerNameBytes, long waiterToken) {
        InterruptObject odi = null;

        if (iti.isODISupported()) {
            odi = new WolaGetClientServiceInterruptObjectImpl(this, wolaGroupBytes, registerNameBytes, waiterToken);

            try {
                iti.register(odi);
            } catch (Throwable t) {
                FFDCFilter.processException(t, this.getClass().getName(), "92");
                odi = null;
            }
        }

        return odi;
    }

    /** {@inheritDoc} */
    @Override
    public Object register(Future<?> responseFuture) {
        InterruptObject odi = null;

        if (iti.isODISupported()) {
            odi = new WolaResponseInterruptObjectImpl(responseFuture);

            try {
                iti.register(odi);
            } catch (Throwable t) {
                FFDCFilter.processException(t, this.getClass().getName(), "136");
                odi = null;
            }
        }

        return odi;
    }

    /** {@inheritDoc} */
    @Override
    public void deregister(Object token) {
        if ((token != null) && (token instanceof WolaInterruptObject)) {
            WolaInterruptObject odi = (WolaInterruptObject) token;
            odi.cancel();
            if (iti.isODISupported()) {
                iti.deregister(odi);
            }
        }
    }

    native void ntv_cancelOtmaRequest(long anchor, long sessionId, int ecbPtr);

    native void ntv_cancelWolaClientWaiter(byte[] wolaGroupBytes, byte[] registerNameBytes, long waiterToken);
}
