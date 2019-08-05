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
package com.ibm.ws.ssl.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 *
 */
public class KeyStoreManagerTest {

    /**
     * Test to make sure a WSKeyStore gets added the the KeyStore HashMap
     * 
     * @throws Exception
     */
    @Test
    public void testAddWSKeyStoreToCache() throws Exception {
        KeyStoreManager ksMgr = KeyStoreManager.getInstance();
        WSKeyStore wsks = new WSKeyStore();
        ksMgr.addKeyStoreToMap("testKeyStore", wsks);
        assertNotNull(ksMgr.getKeyStore("testKeyStore"));
    }

    /**
     * Test to make sure a WSKeyStore gets replaced when a WSKeyStore is added to the
     * cache with the name of an existing WSKeyStore.
     * 
     * @throws Exception
     */
    @Test
    public void testAddWSKeyStoreToCache_replaceWSKeyStoreWithSameName() throws Exception {
        KeyStoreManager ksMgr = KeyStoreManager.getInstance();
        WSKeyStore wsks = new WSKeyStore();
        ksMgr.addKeyStoreToMap("testKeyStore", wsks);
        assertNotNull(ksMgr.getKeyStore("testKeyStore"));

        //Create a second WSKeyStore add a property to make it different
        WSKeyStore wsks2 = new WSKeyStore();
        wsks2.setProperty("testProperty", "Extra Value to make WSKeyStore differnt");

        //Replace the existing testKeyStore with a new one
        ksMgr.addKeyStoreToMap("testKeyStore", wsks2);
        WSKeyStore wsks_get = ksMgr.getKeyStore("testKeyStore");
        assertNotNull(wsks_get);

        //Check to see if extra property is in the WSKeyStore
        assertEquals(wsks_get.getProperty("testProperty"), "Extra Value to make WSKeyStore differnt");

    }

    /**
     * Test to make sure a WSKeyStore gets removed the the KeyStore HashMap
     * 
     * @throws Exception
     */
    @Test
    public void testRemoveWSKeyStoreFromCache() throws Exception {
        KeyStoreManager ksMgr = KeyStoreManager.getInstance();
        WSKeyStore wsks = new WSKeyStore();
        ksMgr.addKeyStoreToMap("testKeyStore", wsks);
        assertNotNull(ksMgr.getKeyStore("testKeyStore"));

        //remove the keystore
        ksMgr.clearKeyStoreFromMap("testKeyStore");
        assertNull(ksMgr.getKeyStore("testKeyStore"));
    }
}
