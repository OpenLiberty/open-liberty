/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;

import componenttest.topology.impl.LibertyServer;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 * compliance test:
 * ----------------
 * realm name
 * invalid user
 * valid user with no groups
 * valid user with one group
 * valid user with many groups
 * invalid group
 * group with no members
 * group with one member
 * group with many members
 *
 * Try boundary conditions:
 * empty strings, very long strings, etc
 */
public abstract class WebsphereUserRegistryComplianceTests {

    // All of the variables below should be set by the extending class
    WebsphereUserRegistryServletConnection servlet;
    LibertyServer server;

    String realmName;

    String invalidUserName;
    String validUserSecurityName;
    String validUserDisplayName;
    String validUserUniqueId;
    String validUserPassword;
    String validUserInvalidPassword;
    String validUserName0WithNoGroups;
    String validUserName1WithOneGroup;
    String validUser1Group;
    String validUserName2WithManyGroups;
    List<String> validUser2Groups;

    String invalidGroupName;
    String validGroupSecurityName;
    String validGroupDisplayName;
    String validGroupUniqueId;
    String validGroupName0WithNoMembers;
    String validGroupName1WithOneMember;
    String validGroup1Memeber;
    String validGroupName2WithManyMembers;
    List<String> validGroup2Members;

    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    @Test
    public void getRealm() {
        assertEquals("Did not get back expected realm name",
                     realmName, servlet.getRealm());
    }

    @Test
    public void checkPasswordInvalidUser() throws Exception {
        String password = "password123";
        try {
            servlet.checkPassword(invalidUserName, password);
            fail("Did not get expected PasswordCheckFailedException");
        } catch (PasswordCheckFailedException e) {

        }

        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    @Test
    public void checkPasswordValidUser() throws Exception {
        assertEquals("Valid user with valid password should be validated",
                     validUserSecurityName, servlet.checkPassword(validUserSecurityName, validUserPassword));

        passwordChecker.checkForPasswordInAnyFormat(validUserPassword);
    }

    @Test
    public void checkPasswordValidUserButInvalidPassword() throws Exception {
        try {
            servlet.checkPassword(validUserSecurityName, validUserInvalidPassword);
            fail("Did not get expected PasswordCheckFailedException");
        } catch (PasswordCheckFailedException e) {

        }

        passwordChecker.checkForPasswordInAnyFormat(validUserInvalidPassword);
    }

    @Test
    public void checkPasswordValidUserBoundryConditions() throws Exception {
        try {
            servlet.checkPassword(validUserSecurityName, " ");
            fail("Did not get expected PasswordCheckFailedException");
        } catch (PasswordCheckFailedException e) {

        }

        String password = "thisIsAVeryVeryVeryVeryVERYLongStringWithTheIntentOfDrivingPossibleProblems";
        try {
            servlet.checkPassword(validUserSecurityName, password);
            fail("Did not get expected PasswordCheckFailedException");
        } catch (PasswordCheckFailedException e) {

        }

        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    @Ignore
    @Test
    public void mapCertificate() throws Exception {
        // This is skipped as its not supported
    }

    @Test
    public void isValidUserInvalidUser() throws Exception {
        assertFalse("Invalid user should not validate",
                    servlet.isValidUser(invalidUserName));
    }

    @Test
    public void isValidUserValidUser() throws Exception {
        assertTrue("Valid user should validate",
                   servlet.isValidUser(validUserSecurityName));

        assertTrue("Valid user should validate",
                   servlet.isValidUser(validUserName0WithNoGroups));

        assertTrue("Valid user should validate",
                   servlet.isValidUser(validUserName1WithOneGroup));

        assertTrue("Valid user should validate",
                   servlet.isValidUser(validUserName2WithManyGroups));
    }

    @Test
    public void getUsersNoMatch() throws Exception {
        Result result = servlet.getUsers(invalidUserName, 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    public void getUsersOneMatch() throws Exception {
        Result result = servlet.getUsers(validUserSecurityName, 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().contains(validUserSecurityName));
    }

    @Test
    public void getUsersAllMatches() throws Exception {
        Result result = servlet.getUsers("*", 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().contains(validUserSecurityName));
        assertTrue(result.getList().contains(validUserName0WithNoGroups));
        assertTrue(result.getList().contains(validUserName1WithOneGroup));
        assertTrue(result.getList().contains(validUserName2WithManyGroups));
    }

    @Test
    public void getUsersMatchesValidUser() throws Exception {
        String validUserSecurityNameFirstChar = validUserSecurityName.substring(0, 1);
        Result result = servlet.getUsers(validUserSecurityNameFirstChar + "*", 0);
        assertTrue(result.getList().contains(validUserSecurityName));
    }

    @Ignore
    @Test
    public void getUsersUnlimitedSubsetMatch() throws Exception {
        Result result = servlet.getUsers(invalidUserName, 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().isEmpty());
    }

    @Ignore
    @Test
    public void getUsersBoundedSubsetMatch() throws Exception {
        Result result = servlet.getUsers(invalidUserName, 1);
        assertFalse(result.hasMore());
        assertTrue(result.getList().isEmpty());
    }

    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayNameEntryNotFoundException() throws Exception {
        servlet.getUserDisplayName(invalidUserName);
    }

    @Test
    public void getUserDisplayName() throws Exception {
        assertEquals(validUserDisplayName, servlet.getUserDisplayName(validUserSecurityName));
    }

    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserIdEntryNotFoundException() throws Exception {
        servlet.getUniqueUserId(invalidUserName);
    }

    @Test
    public void getUniqueUserId() throws Exception {
        assertEquals(validUserUniqueId, servlet.getUniqueUserId(validUserSecurityName));
    }

    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityNameEntryNotFoundException() throws Exception {
        servlet.getUserSecurityName(invalidUserName);
    }

    @Test
    public void getUserSecurityName() throws Exception {
        assertEquals(validUserSecurityName, servlet.getUserSecurityName(validUserUniqueId));
    }

    @Test
    public void isValidGroupInvalidGroup() throws Exception {
        assertFalse("Invalid group should not validate",
                    servlet.isValidGroup(invalidGroupName));
    }

    @Test
    public void isValidGroupValidGroup() throws Exception {
        assertTrue("Valid group should validate",
                   servlet.isValidGroup(validGroupSecurityName));

        assertTrue("Valid group should validate",
                   servlet.isValidGroup(validGroupName0WithNoMembers));

        assertTrue("Valid group should validate",
                   servlet.isValidGroup(validGroupName1WithOneMember));

        assertTrue("Valid group should validate",
                   servlet.isValidGroup(validGroupName2WithManyMembers));
    }

    @Test
    public void getGroupsNoMatch() throws Exception {
        Result result = servlet.getGroups(invalidGroupName, 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    public void getGroupsOneMatch() throws Exception {
        Result result = servlet.getGroups(validGroupSecurityName, 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().contains(validGroupSecurityName));
    }

    @Test
    public void getGroupsAllMatches() throws Exception {
        Result result = servlet.getGroups("*", 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().contains(validGroupSecurityName));
        assertTrue(result.getList().contains(validGroupName0WithNoMembers));
        assertTrue(result.getList().contains(validGroupName1WithOneMember));
        assertTrue(result.getList().contains(validGroupName2WithManyMembers));
    }

    @Test
    public void getGroupsMatchesValidGroup() throws Exception {
        String validGroupSecurityNameFirstChar = validGroupSecurityName.substring(0, 1);
        Result result = servlet.getGroups(validGroupSecurityNameFirstChar + "*", 0);
        assertTrue(result.getList().contains(validGroupSecurityName));
    }

    @Ignore
    @Test
    // TODO: figure out how to test this properly!
    public void getGroupsUnlimitedSubsetMatch() throws Exception {
        Result result = servlet.getGroups(invalidGroupName, 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().isEmpty());
    }

    @Ignore
    @Test
    // TODO: figure out how to test this properly!
    public void getGroupsBoundedSubsetMatch() throws Exception {
        Result result = servlet.getGroups(invalidGroupName, 1);
        assertFalse(result.hasMore());
        assertTrue(result.getList().isEmpty());
    }

    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayNameEntryNotFoundException() throws Exception {
        servlet.getGroupDisplayName(invalidGroupName);
    }

    @Test
    public void getGroupDisplayName() throws Exception {
        assertEquals(validGroupDisplayName, servlet.getGroupDisplayName(validGroupSecurityName));
    }

    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIdEntryNotFoundException() throws Exception {
        servlet.getUniqueGroupId(invalidGroupName);
    }

    @Test
    public void getUniqueGroupId() throws Exception {
        assertEquals(validGroupUniqueId, servlet.getUniqueGroupId(validGroupSecurityName));
    }

    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityNameEntryNotFoundException() throws Exception {
        servlet.getGroupSecurityName(invalidGroupName);
    }

    @Test
    public void getGroupSecurityName() throws Exception {
        assertEquals(validGroupSecurityName, servlet.getGroupSecurityName(validGroupUniqueId));
    }

    @Test
    public void getUniqueGroupIdsWithNoGroups() throws Exception {
        String uniqueUserId = servlet.getUniqueUserId(validUserName0WithNoGroups);
        List<String> groups = servlet.getUniqueGroupIds(uniqueUserId);
        assertTrue(groups.isEmpty());
    }

    @Test
    public void getUniqueGroupIdsWithOneGroup() throws Exception {
        String uniqueUserId = servlet.getUniqueUserId(validUserName1WithOneGroup);
        List<String> groups = servlet.getUniqueGroupIds(uniqueUserId);
        assertFalse(groups.isEmpty());
        assertEquals(1, groups.size());
        assertTrue(groups.contains(servlet.getUniqueGroupId(validUser1Group)));
    }

    @Test
    public void getUniqueGroupIdsWithManyGroups() throws Exception {
        String uniqueUserId = servlet.getUniqueUserId(validUserName2WithManyGroups);
        List<String> groups = servlet.getUniqueGroupIds(uniqueUserId);
        assertFalse(groups.isEmpty());
        for (String groupId : validUser2Groups) {
            assertTrue(groups.contains(servlet.getUniqueGroupId(groupId)));
        }
    }

    @Test
    public void getGroupsForUserWithNoMembers() throws Exception {
        List<String> groups = servlet.getGroupsForUser(validUserName0WithNoGroups);
        assertTrue(groups.isEmpty());
    }

    @Test
    public void getGroupsForUserWithOneMembers() throws Exception {
        List<String> groups = servlet.getGroupsForUser(validUserName1WithOneGroup);
        assertFalse(groups.isEmpty());
        assertEquals(1, groups.size());
        assertTrue(groups.contains(validUser1Group));
    }

    @Test
    public void getGroupsForUserWithManyMembers() throws Exception {
        List<String> groups = servlet.getGroupsForUser(validUserName2WithManyGroups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.containsAll(validUser2Groups));
    }
}
