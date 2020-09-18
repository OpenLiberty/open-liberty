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
package org.gjwatts.liberty;

import java.security.KeyFactory;

public class EdDSATest {

    public static String test() throws Exception {
        // The EdDSA algorithm is only available in JDK 15
        // JDK 14 or earlier will fail with - java.security.NoSuchAlgorithmException: EdDSA KeyFactory not available
        KeyFactory kf = null;

        try {
            kf = KeyFactory.getInstance("EdDSA");
            return "Successfully created an EdDSA KeyFactory";
        } catch (Exception e) {
            return "Failed to create an EdDSA KeyFactory";
        }
    }
}
