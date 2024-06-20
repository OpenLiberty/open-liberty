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

package com.ibm.ws.zos.channel.wola.internal.msg;

/**
 * WOLA message context IDs. A WOLA message contains 0 or more contexts
 * (e.g. for transaction, security, the service name, etc).
 *
 * All known contexts are identified by this enum.
 *
 * !!! NOTE !!!: this enum must be kept in sync with the context ids in
 * com.ibm.zos.native/include/server_wola_message.h
 *
 * Note: Just to be aware, there are two 'classes' of contexts:
 * 1) those that flow on the BBOAMSG (WolaMessage), and
 * 2) those that are returned by the BBOA1GTX API (client get context)
 *
 * The first four IDs in this part are for contexts that are returned by BBOA1GTX,
 * and the last two are contexts that flow on BBOAMSG. BBOA1GTX is an internal-only call.
 * The contexts it returns (1-4 here) are never actually included in a BBOAMSG; they
 * are created and consumed internally by the WOLA code.
 *
 */
public enum WolaMessageContextId {

    /** Context ID for transaction context. */
    BBOATXC_Identifier(1),

    /** Context ID for security context. */
    BBOASEC_Identifier(2),

    /** Context ID for WLM context. */
    BBOAWLMC_Identifier(3),

    /** Context ID for correlation context. */
    BBOACORC_Identifier(4),

    /** Context ID for service name context. */
    BBOASNC_Identifier(5),

    /** Context ID for CICS Link Server Context. */
    CicsLinkServerContextId(6);

    /**
     * The corresponding native value for the ID.
     */
    public final int nativeValue;

    /**
     * Reverse lookup of Enum value by native value.
     */
    private static WolaMessageContextId[] byNativeValue;

    /**
     * Populate the byNativeValue lookup.
     */
    static {
        byNativeValue = new WolaMessageContextId[WolaMessageContextId.class.getEnumConstants().length + 1]; // +1 since we start at 1, not 0
        for (WolaMessageContextId contextId : WolaMessageContextId.class.getEnumConstants()) {
            byNativeValue[contextId.nativeValue] = contextId;
        }
    }

    /**
     * CTOR.
     */
    private WolaMessageContextId(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    /**
     * @param nativeValue
     *
     * @return The enum value for the given nativeValue.
     *
     * @throws IllegalArgumentException if the nativeValue does not correspond to an Enum value.
     */
    public static WolaMessageContextId forNativeValue(int nativeValue) {
        if (nativeValue >= byNativeValue.length || byNativeValue[nativeValue] == null) {
            throw new IllegalArgumentException("Invalid native value (" + nativeValue + ") for WOLAMessageContextId");
        } else {
            return byNativeValue[nativeValue];
        }
    }

};
