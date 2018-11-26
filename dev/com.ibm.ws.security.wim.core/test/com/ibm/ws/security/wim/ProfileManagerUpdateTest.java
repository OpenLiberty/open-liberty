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

package com.ibm.ws.security.wim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.wim.adapter.file.TestFileAdapter;
import com.ibm.wsspi.security.wim.exception.EntityIdentifierNotSpecifiedException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.EntityNotInRealmScopeException;
import com.ibm.wsspi.security.wim.exception.InvalidUniqueNameException;
import com.ibm.wsspi.security.wim.exception.OperationNotSupportedException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;

import test.common.SharedOutputManager;

/**
 * Test the update call
 *
 * @author Rohan Z
 */
public class ProfileManagerUpdateTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private VMMService vmmService;
    private final ConfigManager configManager = new ConfigManager();

    private final Mockery mock = new JUnit4Mockery();

    private final ComponentContext cc = mock.mock(ComponentContext.class);

    private final Configuration baseEntryConfig = mock.mock(Configuration.class, "baseEntryConfig");

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setup() throws IOException {
        mock.checking(new Expectations() {
            {
                Hashtable<String, Object> baseEntryProps = new Hashtable<String, Object>();
                baseEntryProps.put(RealmConfig.NAME, "o=defaultWIMFileBasedRealm");

                allowing(baseEntryConfig).getProperties();
                will(returnValue(baseEntryProps));

            }
        });

        Map<String, Object> fileConfigProps = new HashMap<String, Object>();
        fileConfigProps.put(BaseRepository.BASE_ENTRY + ".0." + RealmConfig.NAME, "o=defaultWIMFileBasedRealm");
        fileConfigProps.put(BaseRepository.BASE_ENTRY + ".1." + RealmConfig.NAME, "o=IBM");

        fileConfigProps.put(BaseRepository.BASE_ENTRY + ".0." + RealmConfig.NAME + ".0." + RealmConfig.PARTICIPATING_BASEENTRIES, "o=defaultWIMFileBasedRealm");
        fileConfigProps.put(BaseRepository.BASE_ENTRY + ".0." + RealmConfig.NAME + ".0." + RealmConfig.NAME, "testRealm");

        fileConfigProps.put(BaseRepository.KEY_ID, "file1");
//        fileConfigProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        TestFileAdapter fa = new TestFileAdapter();
        fa.activate(fileConfigProps, cc);

        HashMap<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(BaseRepository.KEY_ID, "file1");
//        configProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        configProps.put(ConfigManager.PRIMARY_REALM + ".0." + RealmConfig.PARTICIPATING_BASEENTRIES + ".0." + RealmConfig.NAME, "o=IBM");
        configProps.put(ConfigManager.PRIMARY_REALM + ".0." + RealmConfig.NAME, "testRealm");
        configProps.put(ConfigManager.PRIMARY_REALM + ".0." + RealmConfig.DEFAULT_REALM, true);
        configManager.activate(cc, configProps);

        vmmService = new VMMService();
        vmmService.configMgr = configManager;
        vmmService.setConfiguredRepository(fa, fileConfigProps);
        vmmService.activate(cc);
    }

    @After
    public void tearDown() {}

    @Test
    public void testNoEntity() {
        Root root = new Root();
        try {
            vmmService.update(root);
            fail("Call completed successfully");
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntityNotFoundException.class, e.getClass());
            assertEquals("The error code for EntityNotFoundException", "CWIML1030E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testMultipleEntity() {
        Root root = new Root();
        PersonAccount p1 = new PersonAccount();
        PersonAccount p2 = new PersonAccount();
        root.getEntities().add(p1);
        root.getEntities().add(p2);
        try {
            vmmService.update(root);
            fail("Call completed successfully");
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", OperationNotSupportedException.class, e.getClass());
            assertEquals("The error code for OperationNotSupportedException", "CWIML1016E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testNoIdentifier() {
        Root root = new Root();
        PersonAccount p1 = new PersonAccount();
        root.getEntities().add(p1);
        try {
            vmmService.update(root);
            fail("Call completed successfully");
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntityIdentifierNotSpecifiedException.class, e.getClass());
            assertEquals("The error code for EntityIdentifierNotSpecifiedException", "CWIML1009E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testInvalidUniqueName() {
        Root root = new Root();
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=admin,o=invalid");

        PersonAccount p1 = new PersonAccount();
        p1.setIdentifier(id);

        root.getEntities().add(p1);
        try {
            vmmService.update(root);
            fail("Call completed successfully");
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", InvalidUniqueNameException.class, e.getClass());
            assertEquals("The error code for InvalidUniqueNameException", "CWIML0515E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testEntityNotInRealm() {
        Root root = new Root();
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=admin,o=invalid");
        id.setRepositoryId("file1");

        Context context = new Context();
        context.set("realm", "testRealm");

        PersonAccount p1 = new PersonAccount();
        p1.setIdentifier(id);

        root.getEntities().add(p1);
        root.getContexts().add(context);

        try {
            vmmService.update(root);
            fail("Call completed successfully");
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntityNotInRealmScopeException.class, e.getClass());
            assertEquals("The error code for EntityNotInRealmScopeException", "CWIML0515E", errorMessage.substring(0, 10));
        }
    }
}