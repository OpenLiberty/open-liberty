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
package com.ibm.ws.security.wim.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.wim.ConfigManager;
import com.ibm.ws.security.wim.CoreSetup;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;

public class WIMUserRegistryTest {

    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final Mockery mock = new JUnit4Mockery();

    private UserRegistry UR;

    private final CoreSetup core = new CoreSetup();

    private final ComponentContext cc = mock.mock(ComponentContext.class);

    private final ServiceReference<ConfigManager> configManagerRef = mock.mock(ServiceReference.class, "configManagerRef");

    private final ServiceReference<VMMService> VMMServiceRef = mock.mock(ServiceReference.class, "VMMServiceRef");

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private UserRegistry newWIMUR(Map<String, Object> urProps) throws IOException {
        core.setup(urProps);
        WIMUserRegistry ur = new WIMUserRegistry();
        ur.configManager = core.getConfigManager();
        ur.vmmService = core.getVMMService();
        ur.activate();
        return ur;
    }

    @Test
    public void testGetUsers() throws Exception {
        Map<String, Object> urProps = new HashMap<String, Object>();
        UR = newWIMUR(urProps);

        SearchResult result = UR.getUsers("user1", 10);
        List<String> resultList = result.getList();

        int i = resultList.size();

        assertEquals("Number of members mismatched", 1, i);
        assertEquals("CN Mismatched", "uid=user1,o=defaultWIMFileBasedRealm", resultList.get(0));
    }

    @Test
    public void testGetUsersNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUsers(null, 10);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", null, errorMessage);
        }
    }

    @Test
    @Ignore
    public void testGetUsersInvalidLimit() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUsers("user1", -2);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", "CWIML1022E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetUsersInvalid() throws Exception {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            SearchResult result = UR.getUsers("user11", 10);
            List<String> resultList = result.getList();

            int i = resultList.size();

            assertEquals("Number of members mismatched", 0, i);
        } catch (RegistryException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroups() throws Exception {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            SearchResult result = UR.getGroups("group1", 10);
            List<String> resultList = result.getList();

            int i = resultList.size();

            assertEquals("Number of members mismatched", 1, i);
            // assertEquals("CN Mismatched", "group1", resultList.get(0));
            assertEquals("CN Mismatched", "cn=group1,o=defaultWIMFileBasedRealm", resultList.get(0));
        } catch (RegistryException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupsNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getGroups(null, 10);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", null, errorMessage);
        }
    }

    @Test
    @Ignore
    public void testGetGroupsInvalidLimit() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getGroups("group1", -4);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", "CWIML1022E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetGroupsInvalid() throws Exception {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            SearchResult result = UR.getGroups("group11", 10);
            List<String> resultList = result.getList();

            int i = resultList.size();

            assertEquals("Number of members mismatched", 0, i);
        } catch (RegistryException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupsForUserLevel1() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            urProps.put("com.ibm.ws.wim.registry.grouplevel", "1");
            UR = newWIMUR(urProps);

            List<String> resultList = UR.getGroupsForUser("user1");

            int i = resultList.size();

            assertEquals("Number of members mismatched", 1, i);
            // assertEquals("CN Mismatched", "nestedGroup1", resultList.get(0));
            assertEquals("CN Mismatched", "cn=nestedGroup1,o=defaultWIMFileBasedRealm", resultList.get(0));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupsForUserLevel0() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            urProps.put("com.ibm.ws.wim.registry.grouplevel", "0");
            UR = newWIMUR(urProps);

            List<String> resultList = UR.getGroupsForUser("user1");

            int i = resultList.size();

            assertEquals("Number of members mismatched", 2, i);

            int index = 0;

            String[] cns = new String[i];
            // String[] expectedcns = {"nestedGroup1", "group1"};
            String[] expectedcns = { "cn=nestedGroup1,o=defaultWIMFileBasedRealm", "cn=group1,o=defaultWIMFileBasedRealm" };

            for (String result : resultList) {
                cns[index++] = result;
            }

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupsForUserDefaultLevel() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            List<String> resultList = UR.getGroupsForUser("user1");

            int i = resultList.size();

            assertEquals("Number of members mismatched", 1, i);

            // assertEquals("CN Mismatched", "nestedGroup1", resultList.get(0));
            assertEquals("CN Mismatched", "cn=nestedGroup1,o=defaultWIMFileBasedRealm", resultList.get(0));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupsForUserNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            urProps.put("com.ibm.ws.wim.registry.grouplevel", "1");
            UR = newWIMUR(urProps);

            UR.getGroupsForUser(null);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", null, errorMessage);
        }
    }

    @Test
    public void testGetGroupsForInvalidUserLevel1() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            urProps.put("com.ibm.ws.wim.registry.grouplevel", "1");
            UR = newWIMUR(urProps);

            UR.getGroupsForUser("user11");

            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetGroupsForInvalidUserLevel0() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            urProps.put("com.ibm.ws.wim.registry.grouplevel", "0");
            UR = newWIMUR(urProps);

            UR.getGroupsForUser("user11");

            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetGroupsForInvalidUserDefaultLevel() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getGroupsForUser("user11");
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetGroupDisplayNameNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getGroupDisplayName(null);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", null, errorMessage);
        }
    }

    @Test
    public void testGetGroupDisplayNameInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getGroupDisplayName("group11");
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetGroupDisplayName() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            assertEquals("CN Mismatched", "group1", UR.getGroupDisplayName("group1"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetUniqueGroupIdNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUniqueGroupId(null);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", null, errorMessage);
        }
    }

    @Test
    public void testGetUniqueGroupIdInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUniqueGroupId("group11");
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetUniqueGroupId() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            assertEquals("CN Mismatched", "cn=group1,o=defaultWIMFileBasedRealm", UR.getUniqueGroupId("group1"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetUserDisplayNameNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUserDisplayName("<Unknown exception>");
            fail("expected fail");
        } catch (Exception e) {
//            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
//            assertEquals("The error code for RegistryException", null, errorMessage);
            assertEquals("Incorrect cause", EntityNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetUserDisplayNameInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUserDisplayName("user11");
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetUserDisplayName() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            assertEquals("CN Mismatched", "user1", UR.getUserDisplayName("user1"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetUserSecurityNameNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUserSecurityName("<Unknown exception>");
            fail("expected fail");
        } catch (Exception e) {
//          String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
//          assertEquals("The error code for RegistryException", null, errorMessage);
            assertEquals("Incorrect cause", EntityNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetUserSecurityNameInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUserSecurityName("user11");
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            // assertEquals("The error code for EntryNotFoundException", "CWIML1011E", errorMessage.substring(0, 10));
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetUserSecurityName() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            // assertEquals("CN Mismatched", "user1", UR.getUserSecurityName("uid=user1,o=defaultWIMFileBasedRealm"));
            assertEquals("CN Mismatched", "user1", UR.getUserSecurityName("uid=user1,o=defaultWIMFileBasedRealm"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetUniqueGroupIdsNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUniqueGroupIdsForUser("<Unknown exception>");
            fail("expected fail");
        } catch (Exception e) {
//          String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
//          assertEquals("The error code for RegistryException", null, errorMessage);
            assertEquals("Incorrect cause", EntityNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetUniqueGroupIdsInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUniqueGroupIdsForUser("user11");
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetUniqueGroupIds() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            List<String> resultList = UR.getUniqueGroupIdsForUser("uid=user1,o=defaultWIMFileBasedRealm");

            int i = resultList.size();

            assertEquals("Number of members mismatched", 1, i);

            assertEquals("CN Mismatched", "cn=nestedGroup1,o=defaultWIMFileBasedRealm", resultList.get(0));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testIsValidGroupNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.isValidGroup(null);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", null, errorMessage);
        }
    }

    @Test
    public void testIsValidGroupInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            assertEquals("IsValid call failed", false, UR.isValidGroup("group11"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testIsValidGroup() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            assertEquals("IsValid call failed", true, UR.isValidGroup("group1"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testIsValidUserNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.isValidUser(null);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", null, errorMessage);
        }
    }

    @Test
    public void testIsValidUserInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            assertEquals("IsValid call failed", false, UR.isValidUser("user11"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testIsValidUser() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            assertEquals("IsValid call failed", true, UR.isValidUser("user1"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetGroupSecurityNameNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getGroupSecurityName(null);
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", RegistryException.class, e.getClass());
            assertEquals("The error code for RegistryException", null, errorMessage);
        }
    }

    @Test
    public void testGetGroupSecurityNameInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getGroupSecurityName("group11");
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            // assertEquals("The error code for EntryNotFoundException", "CWIML1011E", errorMessage.substring(0, 10));
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetGroupSecurityName() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            // assertEquals("CN Mismatched", "group1", UR.getGroupSecurityName("cn=group1,o=defaultWIMFileBasedRealm"));
            assertEquals("CN Mismatched", "cn=group1,o=defaultWIMFileBasedRealm", UR.getGroupSecurityName("group1"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testGetUniqueUserIdNull() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUniqueUserId("<Unknown exception>");
            fail("expected fail");
        } catch (Exception e) {
//          String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
//          assertEquals("The error code for RegistryException", null, errorMessage);
            assertEquals("Incorrect cause", EntityNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetUniqueUserIdInvalid() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            UR.getUniqueUserId("user11");
            fail("expected fail");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntryNotFoundException.class, e.getClass());
            assertEquals("The error code for EntryNotFoundException", "CWIML4001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testGetUniqueUserId() {
        try {
            Map<String, Object> urProps = new HashMap<String, Object>();
            UR = newWIMUR(urProps);

            assertEquals("CN Mismatched", "uid=user1,o=defaultWIMFileBasedRealm", UR.getUniqueUserId("user1"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }
}
