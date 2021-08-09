/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.ltpakeyutil;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class LTPAKeyFileUtilityImplTest {

    @Test
    public void testLTPAKeyGeneration() throws Exception {
    	LTPAKeyFileUtilityImpl creator = new LTPAKeyFileUtilityImpl();
        Properties keyInfo = creator.generateLTPAKeys("WebAS".getBytes(), "myRealm");

        // Check the secret key.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.KEYIMPORT_SECRETKEY));

        // Check the private key.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.KEYIMPORT_PRIVATEKEY));

        // Check the public key.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.KEYIMPORT_PUBLICKEY));

        // Check the realm.
        Assert.assertEquals("myRealm", keyInfo.get(LTPAKeyFileUtility.KEYIMPORT_REALM));

        // Check the host.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.CREATION_HOST_PROPERTY));

        // Check the version.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.LTPA_VERSION_PROPERTY));

        // Check the creation date.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.CREATION_DATE_PROPERTY));
    }

}
