/*******************************************************************************
 * Copyright (c) 2012, 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.urbridge;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.wim.ConfigManager;
import com.ibm.ws.security.wim.adapter.urbridge.util.DummyRegistry;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.InitializationException;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.SearchControlException;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;

import test.common.SharedOutputManager;

/**
 * This class tests that the calls made to the UR Bridge are passed on correctly to the
 * underlying user registry.
 *
 * @author Rohan Z
 */
public class URBridgeTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private URBridge urBridge = null;

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);

    @Before
    public void setup() throws InitializationException {
        UserRegistry dummyRegistry = new DummyRegistry();
        HashMap<String, Object> configProp = new HashMap<String, Object>();
        String realm = null;
        try {
            realm = dummyRegistry.getRealm();
        } catch (Exception e) {
        }

        configProp.put("id", realm);
        configProp.put("registryBaseEntry", "o=" + realm);

        URConfigManager configMgr = new URConfigManager();
        HashMap<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(ConfigManager.MAX_SEARCH_RESULTS, 1000);
        configProps.put(ConfigManager.SEARCH_TIME_OUT, 1000L);
        configProps.put(ConfigManager.PRIMARY_REALM, "defaultRealm");
        configMgr.activate(cc, configProps);

        urBridge = new URBridge(configProp, dummyRegistry, configMgr);
    }

    @After
    public void tearDown() {}

    @Test
    public void testGetRealm() {
        String realm = urBridge.getRealm();

        assertEquals("Incorrect realm returned", "dummyRealm", realm);
    }

    @Test
    public void testGetUser() {
        Root root = new Root();
        Root returnRoot = null;
        PersonAccount person = new PersonAccount();

        IdentifierType id = new IdentifierType();
        id.setUniqueName("testUser");

        person.setIdentifier(id);

        root.getEntities().add(person);

        try {
            returnRoot = urBridge.get(root);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call failed", false, true + " with " + errorMessage);
        }

        assertEquals("No user found", 1, returnRoot.getEntities().size());
    }

    @Test
    public void testGetInvalidUser() {
        Root root = new Root();
        PersonAccount person = new PersonAccount();

        IdentifierType id = new IdentifierType();
        id.setUniqueName("invalid");

        person.setIdentifier(id);

        root.getEntities().add(person);

        try {
            urBridge.get(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            assertEquals("Incorrect outer exception thrown", EntityNotFoundException.class, e.getClass());
            // assertEquals("Incorrect inner exception thrown", EntityNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetGroup() {
        Root root = new Root();
        Root returnRoot = null;
        Group group = new Group();

        IdentifierType id = new IdentifierType();
        id.setUniqueName("testGroup");

        group.setIdentifier(id);

        root.getEntities().add(group);

        try {
            returnRoot = urBridge.get(root);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call failed", false, true + " with " + errorMessage);
        }

        assertEquals("No group found", 1, returnRoot.getEntities().size());
    }

    @Test
    public void testGetInvalidGroup() {
        Root root = new Root();
        Group group = new Group();

        IdentifierType id = new IdentifierType();
        id.setUniqueName("invalid");

        group.setIdentifier(id);

        root.getEntities().add(group);

        try {
            urBridge.get(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            assertEquals("Incorrect outer exception thrown", EntityNotFoundException.class, e.getClass());
            // assertEquals("Incorrect inner exception thrown", EntityNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testSearchUsers() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("uid");
        srchCtrl.getProperties().add("cn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and uid='testUser'");
        srchCtrl.setCountLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            root = urBridge.search(root);
            List<Entity> entities = root.getEntities();
            int i = entities.size();
            int index = 0;

            assertEquals("Number of members mismatched", 1, i);

            String[] UIDs = new String[i];
            String[] expectedUIDs = { "testUser" };

            for (Entity entity : entities) {
                UIDs[index++] = ((PersonAccount) entity).getUid();
            }

            assertArrayEquals("UID Mismatched", expectedUIDs, UIDs);
        } catch (WIMException e) {
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
        }
    }

    @Test
    public void testSearchUsersInvalidExpression() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("uid");
        srchCtrl.getProperties().add("cn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and uid=testUser");
        srchCtrl.setCountLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            root = urBridge.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            assertEquals("Incorrect outer exception thrown", WIMApplicationException.class, e.getClass());
        }
    }

    @Test
    public void testSearchUsersNone() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("uid");
        srchCtrl.getProperties().add("cn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and uid='testuser'");
        srchCtrl.setCountLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            root = urBridge.search(root);
            List<Entity> entities = root.getEntities();
            int i = entities.size();

            assertEquals("Number of members mismatched", 0, i);
        } catch (WIMException e) {
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
        }
    }

    @Test
    public void testInvalidEntityLogin() {
        Root root = new Root();
        Group group = new Group();
        root.getEntities().add(group);

        try {
            urBridge.login(root);
        } catch (WIMException e) {
            assertEquals("Incorrect exception thrown", WIMApplicationException.class, e.getClass());
        }
    }

    @Test
    public void testLoginNoPassword() {
        Root root = new Root();
        PersonAccount person = new PersonAccount();
        person.setPrincipalName("testUser");
        root.getEntities().add(person);

        try {
            urBridge.login(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            System.out.println(errorMessage);
            assertEquals("Incorrect exception thrown", PasswordCheckFailedException.class, e.getClass());
        }
    }

    @Test
    public void testLoginInvalidUser() {
        Root root = new Root();
        Root returnRoot = null;
        PersonAccount person = new PersonAccount();
        person.setPrincipalName("invalid");
        person.setPassword("password".getBytes());
        root.getEntities().add(person);

        try {
            returnRoot = urBridge.login(root);
            List<Entity> entities = returnRoot.getEntities();
            int i = entities.size();

            assertEquals("Number of members mismatched", 0, i);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            System.out.println(errorMessage);
            assertEquals("Incorrect exception thrown", WIMApplicationException.class, e.getClass());
        }
    }

    @Test
    public void testLoginInvalidPassword() {
        Root root = new Root();
        Root returnRoot = null;
        PersonAccount person = new PersonAccount();
        person.setPrincipalName("testUser");
        person.setPassword("wrongPassword".getBytes());
        root.getEntities().add(person);

        try {
            returnRoot = urBridge.login(root);
            List<Entity> entities = returnRoot.getEntities();
            int i = entities.size();

            assertEquals("Number of members mismatched", 0, i);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            System.out.println(errorMessage);
            assertEquals("Incorrect exception thrown", WIMException.class, e.getClass());
        }
    }

    @Test
    public void testLogin() {
        Root root = new Root();
        Root returnRoot = null;
        PersonAccount person = new PersonAccount();
        person.setPrincipalName("testUser");
        person.setPassword("password".getBytes());
        root.getEntities().add(person);

        try {
            returnRoot = urBridge.login(root);
            List<Entity> entities = returnRoot.getEntities();
            int i = entities.size();

            assertEquals("Number of members mismatched", 1, i);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            System.out.println(errorMessage);
            assertEquals("Incorrect exception thrown", WIMApplicationException.class, e.getClass());
        }
    }
}
