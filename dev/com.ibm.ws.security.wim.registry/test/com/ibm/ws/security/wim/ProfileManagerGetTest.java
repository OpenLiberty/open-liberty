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
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
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
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.wim.adapter.file.FileAdapter;
import com.ibm.wsspi.security.wim.exception.EntityIdentifierNotSpecifiedException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.GroupMemberControl;
import com.ibm.wsspi.security.wim.model.GroupMembershipControl;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;

import test.common.SharedOutputManager;

/**
 * This class tests the ProfileManager get call.
 */
public class ProfileManagerGetTest {

    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private VMMService vmmService;
    private final ConfigManager configManager = new ConfigManager();

    private final Mockery mock = new JUnit4Mockery();
    private final ConfiguredRepository repository = mock.mock(ConfiguredRepository.class);
    private final ComponentContext cc = mock.mock(ComponentContext.class);

    private final Configuration defaultRealmConfig = mock.mock(Configuration.class, "defaultRealmConfig");

    private final Configuration baseEntryConfig = mock.mock(Configuration.class, "baseEntryConfig");

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

                String[] baseEntries = { "o=defaultWIMFileBasedRealm" };
                Hashtable<String, Object> realmConfig = new Hashtable<String, Object>();
                realmConfig.put(RealmConfig.PARTICIPATING_BASEENTRIES, baseEntries);
                realmConfig.put(RealmConfig.NAME, "defaultWIMFileBasedRealm");

                allowing(defaultRealmConfig).getProperties();
                will(returnValue(realmConfig));

                Hashtable<String, Object> baseEntryProps = new Hashtable<String, Object>();
//                baseEntryProps.put(RealmConfig.NAME, "o=defaultWIMFileBasedRealm");
                baseEntryProps.put("o=defaultWIMFileBasedRealm", RealmConfig.NAME);

                allowing(baseEntryConfig).getProperties();
                will(returnValue(baseEntryProps));

                allowing(repository).getRepositoryBaseEntries();
                will(returnValue(baseEntryProps));

                allowing(repository).getRepositoriesForGroups();
                will(returnValue(new String[] { "InternalFileRepository" }));

            }
        });

        Hashtable<String, Object> baseEntryProps = new Hashtable<String, Object>();
        baseEntryProps.put(RealmConfig.NAME, "o=defaultWIMFileBasedRealm");

        String[] baseEntries = { "o=defaultWIMFileBasedRealm" };
        Hashtable<String, Object> realmConfig = new Hashtable<String, Object>();
        realmConfig.put(RealmConfig.PARTICIPATING_BASEENTRIES, baseEntries);
        realmConfig.put(RealmConfig.NAME, "defaultWIMFileBasedRealm");

        Map<String, Object> fileConfigProps = new HashMap<String, Object>();
        fileConfigProps.put(BaseRepository.BASE_ENTRY + ".0." + RealmConfig.NAME, "o=defaultWIMFileBasedRealm");

        fileConfigProps.put(BaseRepository.BASE_ENTRY + ".0." + RealmConfig.NAME + ".0." + RealmConfig.PARTICIPATING_BASEENTRIES, baseEntries);
        fileConfigProps.put(BaseRepository.BASE_ENTRY + ".0." + RealmConfig.NAME + ".0." + RealmConfig.NAME, "defaultWIMFileBasedRealm");

        fileConfigProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        fileConfigProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        FA fa = new FA();
        fa.activate(fileConfigProps, cc);
        HashMap<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        configProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        configProps.put(ConfigManager.MAX_SEARCH_RESULTS, 1000);
        configProps.put(ConfigManager.SEARCH_TIME_OUT, 1000L);
        configProps.put(ConfigManager.PRIMARY_REALM, "defaultRealm");
        configManager.activate(cc, configProps);

        vmmService = new VMMService();
        vmmService.configMgr = configManager;
        vmmService.setConfiguredRepository(fa, fileConfigProps);
        vmmService.activate(cc);
    }

    @After
    public void tearDown() {}

    @Test
    public void testCreate() {
        assertNotNull("Created", new VMMService());
    }

    @Test
    public void testIdentifierNotSpecified() {
        Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        try {
            vmmService.get(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntityIdentifierNotSpecifiedException.class, e.getClass());
            // assertEquals("The error code for EntityIdentifierNotSpecifiedException", "CWIML1009E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testInvalidIdentifier() {
        Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("user1");
        person.setIdentifier(id);
        try {
            root = vmmService.get(root);
            int returnedEntities = root.getEntities().size();

            assertEquals("Unexpected entity return", 0, returnedEntities);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetSinglePersonAccount() {
        final Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=user1,o=defaultWIMFileBasedRealm");
        person.setIdentifier(id);
        try {
            Root response = vmmService.get(root);
            person = (PersonAccount) response.getEntities().get(0);
            String cn = person.getCn();
            assertEquals("CN Mismatched", "user1", cn);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetUnknownPersonAccount() {
        final Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=user11,o=defaultWIMFileBasedRealm");
        person.setIdentifier(id);
        try {
            vmmService.get(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntityNotFoundException.class, e.getClass());
            // assertEquals("The error code for EntityNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    @Ignore
    //TODO:: Remove this ignore once the file adapter actual code is present.
    public void testGetMultiplePersonAccount() {
        Root root = new Root();

        PersonAccount user1 = new PersonAccount();
        root.getEntities().add(user1);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=user1,o=defaultWIMFileBasedRealm");
        user1.setIdentifier(id);

        PersonAccount admin = new PersonAccount();
        root.getEntities().add(admin);
        id = new IdentifierType();
        id.setUniqueName("uid=admin,o=defaultWIMFileBasedRealm");
        admin.setIdentifier(id);
        try {
            root = vmmService.get(root);

            int i = root.getEntities().size();
            String[] cns = new String[i];
            String[] expectedcns = { "user1", "admin" };
            int index = 0;
            for (Entity entity : root.getEntities()) {
                PersonAccount person = (PersonAccount) entity;
                String cn = person.getCn();
                cns[index++] = cn;
            }
            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetPersonAccountWithGroups() {
        final Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=user1,o=defaultWIMFileBasedRealm");
        person.setIdentifier(id);

        GroupMembershipControl grpCtrl = new GroupMembershipControl();
        grpCtrl.setLevel(1);
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            person = (PersonAccount) response.getEntities().get(0);
            String cn = person.getCn();

            assertEquals("CN Mismatched", "user1", cn);
            assertEquals("Number of Groups mismatched", 1, person.getGroups().size());
            assertEquals("Group mismatched", "cn=nestedGroup1,o=defaultWIMFileBasedRealm", person.getGroups().get(0).getIdentifier().getUniqueName());
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetPersonAccountWithGroupsDefaultLevel() {
        final Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=user1,o=defaultWIMFileBasedRealm");
        person.setIdentifier(id);

        GroupMembershipControl grpCtrl = new GroupMembershipControl();
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            person = (PersonAccount) response.getEntities().get(0);
            String cn = person.getCn();

            assertEquals("CN Mismatched", "user1", cn);
            assertEquals("Number of Groups mismatched", 1, person.getGroups().size());
            assertEquals("Group mismatched", "cn=nestedGroup1,o=defaultWIMFileBasedRealm", person.getGroups().get(0).getIdentifier().getUniqueName());
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetPersonAccountWithNestedGroups() {
        final Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=user1,o=defaultWIMFileBasedRealm");
        person.setIdentifier(id);

        GroupMembershipControl grpCtrl = new GroupMembershipControl();
        grpCtrl.setLevel(0);
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            person = (PersonAccount) response.getEntities().get(0);
            String cn = person.getCn();
            assertEquals("CN Mismatched", "user1", cn);
            int i = person.getGroups().size();
            assertEquals("Number of Groups mismatched", 2, i);

            String[] cns = new String[i];
            String[] expectedcns = { "cn=nestedGroup1,o=defaultWIMFileBasedRealm", "cn=group1,o=defaultWIMFileBasedRealm" };
            int index = 0;
            for (Group group : person.getGroups())
                cns[index++] = group.getIdentifier().getUniqueName();

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupWithGroups() {
        final Root root = new Root();
        Group group = new Group();
        root.getEntities().add(group);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("cn=nestedGroup1,o=defaultWIMFileBasedRealm");
        group.setIdentifier(id);

        GroupMembershipControl grpCtrl = new GroupMembershipControl();
        grpCtrl.setLevel(1);
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            group = (Group) response.getEntities().get(0);
            String cn = group.getCn();

            assertEquals("CN Mismatched", "nestedGroup1", cn);
            assertEquals("Number of Groups mismatched", 1, group.getGroups().size());
            assertEquals("Group mismatched", "cn=group1,o=defaultWIMFileBasedRealm", group.getGroups().get(0).getIdentifier().getUniqueName());
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupWithGroupsDefaultLevel() {
        final Root root = new Root();
        Group group = new Group();
        root.getEntities().add(group);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("cn=nestedGroup1,o=defaultWIMFileBasedRealm");
        group.setIdentifier(id);

        GroupMembershipControl grpCtrl = new GroupMembershipControl();
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            group = (Group) response.getEntities().get(0);
            String cn = group.getCn();

            assertEquals("CN Mismatched", "nestedGroup1", cn);
            assertEquals("Number of Groups mismatched", 1, group.getGroups().size());
            assertEquals("Group mismatched", "cn=group1,o=defaultWIMFileBasedRealm", group.getGroups().get(0).getIdentifier().getUniqueName());
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupWithNestedGroups() {
        final Root root = new Root();
        Group group = new Group();
        root.getEntities().add(group);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("cn=nestedGroup1,o=defaultWIMFileBasedRealm");
        group.setIdentifier(id);

        GroupMembershipControl grpCtrl = new GroupMembershipControl();
        grpCtrl.setLevel(0);
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            group = (Group) response.getEntities().get(0);
            String cn = group.getCn();
            assertEquals("CN Mismatched", "nestedGroup1", cn);
            int i = group.getGroups().size();
            assertEquals("Number of Groups mismatched", 1, i);

            String[] cns = new String[i];
            String[] expectedcns = { "cn=group1,o=defaultWIMFileBasedRealm" };
            int index = 0;
            for (Group innerGroup : group.getGroups())
                cns[index++] = innerGroup.getIdentifier().getUniqueName();

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupWithMembers() {
        final Root root = new Root();
        Group group = new Group();
        root.getEntities().add(group);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("cn=group1,o=defaultWIMFileBasedRealm");
        group.setIdentifier(id);

        GroupMemberControl grpCtrl = new GroupMemberControl();
        grpCtrl.setLevel(1);
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            group = (Group) response.getEntities().get(0);
            String cn = group.getCn();

            assertEquals("CN Mismatched", "group1", cn);
            assertEquals("Number of Groups mismatched", 1, group.getMembers().size());
            assertEquals("Group mismatched", "cn=nestedGroup1,o=defaultWIMFileBasedRealm", group.getMembers().get(0).getIdentifier().getUniqueName());
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupWithMembersDefaultLevel() {
        final Root root = new Root();
        Group group = new Group();
        root.getEntities().add(group);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("cn=group1,o=defaultWIMFileBasedRealm");
        group.setIdentifier(id);

        GroupMemberControl grpCtrl = new GroupMemberControl();
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            group = (Group) response.getEntities().get(0);
            String cn = group.getCn();

            assertEquals("CN Mismatched", "group1", cn);
            assertEquals("Number of Groups mismatched", 1, group.getMembers().size());
            assertEquals("Group mismatched", "cn=nestedGroup1,o=defaultWIMFileBasedRealm", group.getMembers().get(0).getIdentifier().getUniqueName());
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupsWithNestedMembers() {
        final Root root = new Root();
        Group group = new Group();
        root.getEntities().add(group);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("cn=group1,o=defaultWIMFileBasedRealm");
//        id.setRepositoryId("InternalFileRepository");
        group.setIdentifier(id);

        GroupMemberControl grpCtrl = new GroupMemberControl();
        grpCtrl.setLevel(0);
        root.getControls().add(grpCtrl);

        try {
            Root response = vmmService.get(root);
            group = (Group) response.getEntities().get(0);
            String cn = group.getCn();

            assertEquals("CN Mismatched", "group1", cn);
            int i = group.getMembers().size();
            assertEquals("Number of members mismatched", 2, i);

            String[] cns = new String[i];
            String[] expectedcns = { "cn=nestedGroup1,o=defaultWIMFileBasedRealm", "uid=user1,o=defaultWIMFileBasedRealm" };
            int index = 0;
            for (Entity entity : group.getMembers())
                cns[index++] = entity.getIdentifier().getUniqueName();

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    @Ignore
    // TODO:: Remove this ignore once the file adapter actual code is present.
    public void testGetPersonAccountWithMembers() {
        Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=user1,o=defaultWIMFileBasedRealm");
        person.setIdentifier(id);

        GroupMemberControl grpCtrl = new GroupMemberControl();
        grpCtrl.setLevel(0);
        root.getControls().add(grpCtrl);

        try {
            root = vmmService.get(root);
            person = (PersonAccount) root.getEntities().get(0);
            String cn = person.getCn();

            assertEquals("CN Mismatched", "user1", cn);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }
}
