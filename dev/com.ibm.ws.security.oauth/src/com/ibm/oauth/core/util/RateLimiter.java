/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.util;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  Provide some rudimentary rate limiting.  The intent is to call limit when
 *  invalid credentials are supplied to prevent brute force guessing of
 *  credentials. Credentials might be userids, clientids, passwords, etc.
 */
public class RateLimiter {
    private static TraceComponent tc = Tr.register(RateLimiter.class);

    /**
     * delay for a while. Simple fixed limit for now.
     */
    public static void limit() {

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            // ffdc
        }
    }

}
