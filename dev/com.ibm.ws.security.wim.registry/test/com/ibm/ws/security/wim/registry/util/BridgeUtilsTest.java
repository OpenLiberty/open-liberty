/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.registry.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.wim.CoreSetup;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.model.Root;

import test.common.SharedOutputManager;

public class BridgeUtilsTest {

    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private BridgeUtils bridge;
    private Root root;
    private final CoreSetup core = new CoreSetup();

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setup() throws Exception {
        Map<String, Object> urProps = new HashMap<String, Object>();
        core.setup(urProps);
        bridge = new BridgeUtils(core.getVMMService(), core.getConfigManager());
        root = bridge.getWimService().createRootObject();
    }

    // This tests a path that should work
    @Test
    public void testGetEntityByIdentifier() throws Exception {
        Root result = bridge.getEntityByIdentifier(root, "uniqueName", "uid=user1,o=defaultWIMFileBasedRealm", "principalName", bridge);
        assertNotNull("Did not get a result from the BridgeUtils.getEntityByIdentifier method.", result);
        assertFalse("The response should contain entities if not NULL.", result.getEntities().isEmpty());
    }

    // This should cause an exception as there is no "badUser" in this realm
    @Test
    public void getEntityByIdentifier_InvalidUser() throws Exception {
        try {
            bridge.getEntityByIdentifier(root, "uniqueName", "uid=badUser,o=defaultWIMFileBasedRealm", "principalName", bridge);
            fail();
        } catch (EntityNotFoundException e) {
            //expected
        }
    }

    // Testing a customer path. May need to consider behavior change
    // in the future, as this path should not be reachable
    @Test
    public void getEntityByIdentifier_InvalidUser2() throws Exception {
        Root result = bridge.getEntityByIdentifier(root, "uniqueName", "badUser", "principalName", bridge);
        assertNull("Expected NULL when no entity is returned.", result);
    }

}
