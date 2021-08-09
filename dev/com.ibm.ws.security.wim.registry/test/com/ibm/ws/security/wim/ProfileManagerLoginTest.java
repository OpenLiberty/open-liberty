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
package com.ibm.ws.security.wim;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.wim.adapter.file.FileAdapter;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.OperationNotSupportedException;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;

import test.common.SharedOutputManager;

/**
 * This class tests the ProfileManager Login call.
 *
 * TODO:: Certificate Login related test cases
 * TODO:: Multiple Principal Found related test cases
 */
@SuppressWarnings("restriction")
public class ProfileManagerLoginTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private VMMService vmmService;
    private final ConfigManager configManager = new ConfigManager();

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);

    private static class FA extends FileAdapter {

        @Override
        protected void activate(Map<String, Object> properties, ComponentContext cc) {
            super.activate(properties, cc);
        }
    }

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
            }
        });

        Map<String, Object> fileConfigProps = new HashMap<String, Object>();
        fileConfigProps.put(MessageFormat.format("{0}.0.{1}", BaseRepository.BASE_ENTRY, RealmConfig.NAME), "o=defaultWIMFileBasedRealm");
        fileConfigProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        fileConfigProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        FA fa = new FA();
        fa.activate(fileConfigProps, cc);

        HashMap<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        configProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        configProps.put(ConfigManager.MAX_SEARCH_RESULTS, 1000);
        configProps.put(ConfigManager.SEARCH_TIME_OUT, 1000L);
        configProps.put(MessageFormat.format("{0}.0.{1}", ConfigManager.PRIMARY_REALM, RealmConfig.NAME), "defaultWIMFileBasedRealm");
        configProps.put(MessageFormat.format("{0}.0.{1}", ConfigManager.PRIMARY_REALM, RealmConfig.ALLOW_IF_REPODOWN), false);
        configProps.put(MessageFormat.format("{0}.0.{1}.0.{2}", ConfigManager.PRIMARY_REALM, RealmConfig.PARTICIPATING_BASEENTRIES, RealmConfig.NAME),
                        "o=defaultWIMFileBasedRealm");
        configManager.activate(cc, configProps);

        vmmService = new VMMService();
        vmmService.configMgr = configManager;
        vmmService.setConfiguredRepository(fa, fileConfigProps);
        vmmService.activate(cc);
    }

    @After
    public void tearDown() {}

    @Test
    public void testMissingEntity() {
        Root root = new Root();
        try {
            vmmService.login(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
//            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntityNotFoundException.class, e.getClass());
            // assertEquals("The error code for EntityNotFoundException", "CWIML1030E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testMultipleEntities() {
        Root root = new Root();

        PersonAccount user1 = new PersonAccount();
        root.getEntities().add(user1);
        user1.setPrincipalName("user1");
        user1.setPassword("loginpassword".getBytes());

        PersonAccount admin = new PersonAccount();
        root.getEntities().add(admin);
        admin.setPrincipalName("admin");
        admin.setPassword("loginpassword".getBytes());

        try {
            vmmService.login(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
//            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", OperationNotSupportedException.class, e.getClass());
            // assertEquals("The error code for OperationNotSupportedException", "CWIML1016E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testMissingPrincipal() {
        Root root = new Root();

        PersonAccount user1 = new PersonAccount();
        root.getEntities().add(user1);
        user1.setPassword("loginpassword".getBytes());

        try {
            vmmService.login(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
//            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", PasswordCheckFailedException.class, e.getClass());
            // assertEquals("The error code for PasswordCheckFailedException", "CWIML4536E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testLogin() {
        Root root = new Root();

        PersonAccount user1 = new PersonAccount();
        root.getEntities().add(user1);
        user1.setPrincipalName("user1");
        user1.setPassword("loginpassword".getBytes());

        try {
            root = vmmService.login(root);
            String cn = ((PersonAccount) root.getEntities().get(0)).getCn();
            assertEquals("CN Mismatched", "user1", cn);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    @Ignore
    public void testInvalidPassword() {
        Root root = new Root();

        PersonAccount user1 = new PersonAccount();
        root.getEntities().add(user1);
        user1.setPrincipalName("user1");
        user1.setPassword("loginpassword".getBytes());

        try {
            vmmService.login(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
//            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", PasswordCheckFailedException.class, e.getClass());
            // assertEquals("The error code for PasswordCheckFailedException", "CWIML4536E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testPrincipalNotFound() {
        Root root = new Root();

        PersonAccount user1 = new PersonAccount();
        root.getEntities().add(user1);
        user1.setPrincipalName("user11");
        user1.setPassword("loginpassword".getBytes());

        try {
            vmmService.login(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
//            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", PasswordCheckFailedException.class, e.getClass());
            // assertEquals("The error code for PasswordCheckFailedException", "CWIML4537E", errorMessage.substring(0, 10));
        }
    }
}
