/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config;

import org.omg.CSIIOP.CompositeDelegation;
import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.DelegationByClient;
import org.omg.CSIIOP.DetectMisordering;
import org.omg.CSIIOP.DetectReplay;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.IdentityAssertion;
import org.omg.CSIIOP.Integrity;
import org.omg.CSIIOP.NoDelegation;
import org.omg.CSIIOP.NoProtection;
import org.omg.CSIIOP.SimpleDelegation;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public final class ConfigUtil {

    @Trivial
    public static String flags(int flag) {
        String result = "";

        if ((NoProtection.value & flag) != 0) {
            result += "NoProtection ";
        }
        if ((Integrity.value & flag) != 0) {
            result += "Integrity ";
        }
        if ((Confidentiality.value & flag) != 0) {
            result += "Confidentiality ";
        }
        if ((DetectReplay.value & flag) != 0) {
            result += "DetectReplay ";
        }
        if ((DetectMisordering.value & flag) != 0) {
            result += "DetectMisordering ";
        }
        if ((EstablishTrustInTarget.value & flag) != 0) {
            result += "EstablishTrustInTarget ";
        }
        if ((EstablishTrustInClient.value & flag) != 0) {
            result += "EstablishTrustInClient ";
        }
        if ((NoDelegation.value & flag) != 0) {
            result += "NoDelegation ";
        }
        if ((SimpleDelegation.value & flag) != 0) {
            result += "SimpleDelegation ";
        }
        if ((CompositeDelegation.value & flag) != 0) {
            result += "CompositeDelegation ";
        }
        if ((IdentityAssertion.value & flag) != 0) {
            result += "IdentityAssertion ";
        }
        if ((DelegationByClient.value & flag) != 0) {
            result += "DelegationByClient ";
        }
        if (result.isEmpty()) {
            return "None";
        }

        return result;
    }
}
