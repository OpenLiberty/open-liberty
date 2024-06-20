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
package com.ibm.ws.zos.registration.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * z/OS Product Registration
 *
 * Provides access to the z/OS services to register products for usage based pricing.
 */
public class ProductRegistrationImpl {

    private static final TraceComponent tc = Tr.register(ProductRegistrationImpl.class);

    /**
     * Start up product registration.
     *
     * Register our native method.
     */
    public void initialize(NativeMethodManager nativeMethodManager) {

        // Attempt to load native code via the method manager.
        nativeMethodManager.registerNatives(ProductRegistrationImpl.class);
    }

    /**
     * Registers a product with z/OS
     *
     * @param p a Product object containing the registration attributes
     */
    public void registerProduct(Product p) {
        String owner = p.owner();
        String name = p.name();
        String version = p.versionRelease();

        // Get the a ebcdic byte representation of the product data needed for registration.
        // Note that the version used to process getVersionBytes is "NOTUSAGE".
        // Read getVersionBytes' description for more details.
        byte[] ownerBytes = p.getOwnerBytes();
        byte[] nameBytes = p.getNameBytes();
        byte[] versionBytes = p.getVersionBytes();
        byte[] pidBytes = p.getPidBytes();
        byte[] qualifierBytes = p.getQualifierBytes();

        // If code page conversion failed give up now
        if ((ownerBytes == null) ||
            (nameBytes == null) ||
            (versionBytes == null) ||
            (pidBytes == null) ||
            (qualifierBytes == null)) {
            Tr.error(tc, "PRODUCT_REGISTRATION_FAILED_BAD_PARM", owner, name, version);
            return;
        }

        // Go do it!
        int native_rc = ntv_registerProduct(ownerBytes, nameBytes, versionBytes, pidBytes, qualifierBytes);

        // A return code of zero or four is ok.  Anything else is a problem.
        if ((native_rc == 0) ||
            (native_rc == 4)) {
            Tr.info(tc, "PRODUCT_REGISTRATION_SUCCESSFUL", owner, name, version);
            p.setRegistered(true);
        } else {
            Tr.error(tc, "PRODUCT_REGISTRATION_UNSUCCESSFUL", owner, name, version, native_rc);
        }

    }

    /**
     * Deregisters a product from z/OS
     *
     * @param p The product to deregister.
     */
    public void deregisterProduct(Product p) {

        // If the product we are trying to deregister failed to be registered first, we are done.
        if (!p.getRegistered()) {
            return;
        }

        // Go native to process deregistration.
        int native_rc = ntv_deregisterProduct(p.getOwnerBytes(), p.getNameBytes(), p.getVersionBytes(), p.getPidBytes(), p.getQualifierBytes());

        // A return code of zero or four is ok.  Anything else is a problem.
        if ((native_rc == 0) ||
            (native_rc == 4)) {
            Tr.info(tc, "PRODUCT_DEREGISTRATION_SUCCESSFUL", p.owner(), p.name(), p.version());
        } else {
            Tr.error(tc, "PRODUCT_DEREGISTRATION_UNSUCCESSFUL", p.owner(), p.name(), p.version(), native_rc);
        }
    }

    /**
     * The native product registration method
     *
     * @param jowner     product owner in EBCDIC
     * @param jname      product name in EBCDIC
     * @param jversion   product version number
     * @param jid        product identifier
     * @param jqualifier product name qualifier
     * @return return code from IFAUSAGE API or -1 if there was a problem.
     */
    protected native int ntv_registerProduct(byte[] jowner, byte[] jname, byte[] jversion, byte[] jid, byte[] jqualifier);

    /**
     * The native product registration method
     *
     * @param owner     product owner in EBCDIC
     * @param name      product name in EBCDIC
     * @param version   product version number
     * @param id        product identifier
     * @param qualifier product name qualifier
     * @return Zero if deregistration completed successfully. A negative number to indicate an internal failure.
     *         A positive number denoting a IFAUSAGE return code.
     */
    protected native int ntv_deregisterProduct(byte[] owner, byte[] name, byte[] version, byte[] id, byte[] qualifier);
}
