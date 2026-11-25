/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.saf.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.credentials.saf.SAFCredentialsService;

/**
 * Unit test for the SAF RoleMapper service.
 * The SAFRoleMapper service maps application-defined role names
 * to SAF profile names.
 *
 * This unit test is only focused on testing the Java code.
 */
public class SAFRoleMapperTest {

    /**
     * Mock environment for NativeMethodManager and native methods.
     */
    private static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    private static int uniqueMockNameCount = 1;

    /**
     * Mock SAFCredentialsService used by SAFAuthorizationService.
     */
    protected static SAFCredentialsService mockSAFCS = null;

    /**
     * SAFRoleMapper config
     */
    protected static SAFRoleMapperImpl defaultRoleMapper = null;
    protected static Dictionary<String, Object> safRoleMapperConfig = new Hashtable<String, Object>();

    /**
     * Default value for <safAuthorization roleMapper="xx" />.
     * Note: Must be kept in sync with com.ibm.ws.security.authorization.saf/metatype.xml.
     */
    protected static final String ROLE_MAPPER_DEFAULT = "com.ibm.ws.security.authorization.saf.internal.SAFRoleMapperImpl";

    /**
     * Default value for <safRoleMapper profilePattern="xx" />.
     * Note: Must be kept in sync with com.ibm.ws.security.authorization.saf/metatype.xml.
     */
    protected static final String PROFILE_PATTERN_DEFAULT = "%profilePrefix%.%role%";

    /**
     * Create the Mockery environemnt and all the mock objects. Call this method at the
     * beginning of each test, to create a fresh isolated Mockery environment for the test.
     * This makes debugging easier when a test fails, because all the Expectations from
     * previous tests don't get dumped to the console.
     */
    private static void createMockEnv() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        ++uniqueMockNameCount;
        mockSAFCS = mockery.mock(SAFCredentialsService.class, "SAFCredentialsService" + uniqueMockNameCount);

    }

    /**
     * Preliminary setup work as class is starting, before any tests are executed.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        defaultRoleMapper = new SAFRoleMapperImpl();
    }

    /**
     * Final teardown work when class is exiting, after all tests have executed.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * Set up the SAFRoleMapper for the unit test with default config values.
     */
    public static SAFRoleMapperImpl updateSAFRoleMapper(String profilePattern, boolean isUppercase) {
        safRoleMapperConfig.put(SAFRoleMapperImpl.PROFILE_PATTERN_KEY, profilePattern);
        safRoleMapperConfig.put("toUpperCase", new Boolean(isUppercase));
        defaultRoleMapper.updateConfig((Map<String, Object>) safRoleMapperConfig);
        defaultRoleMapper.setSafCredentialsService(mockSAFCS);
        return defaultRoleMapper;
    }

    /**
     * Test for SAFRoleMapperImpl
     * default profilePattern is configured.
     */
    @Test
    public void getProfile_defaultConfig() throws Exception {
        createMockEnv();
        final String profilePrefix = "saf";
        updateSAFRoleMapper(PROFILE_PATTERN_DEFAULT, true);
        // Expectations on the mock objects...
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));
            }
        });
        assertEquals("SAF.USER", defaultRoleMapper.getProfileFromRole("someResource", "user"));
        //no changes to config should return the cached safprofile
        assertEquals("SAF.USER", defaultRoleMapper.getProfileFromRole("someResource", "user"));
    }

    /**
     * Test for SAFRoleMapperImpl
     * config to use default profilePattern and donot convert profilename to uppercase
     */
    @Test
    public void getProfile_lowercase() throws Exception {
        createMockEnv();
        final String profilePrefix = "saf";
        updateSAFRoleMapper(PROFILE_PATTERN_DEFAULT, false);

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));
            }
        });
        assertEquals("saf.user", defaultRoleMapper.getProfileFromRole("someResource", "user"));
    }

    /**
     * Test for SAFRoleMapperImpl
     * profilePattern with just resource configured
     */
    @Test
    public void getProfile_resource() throws Exception {
        createMockEnv();
        String PROFILE_PATTERN = "%resource%";
        final String profilePrefix = "";
        updateSAFRoleMapper(PROFILE_PATTERN, false);

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));
            }
        });
        assertEquals("someResource", defaultRoleMapper.getProfileFromRole("someResource", "user"));
    }

    /**
     * Test for SAFRoleMapperImpl
     * profilePattern with just role configured
     */
    @Test
    public void getProfile_role() throws Exception {
        createMockEnv();
        String PROFILE_PATTERN = "%role%";
        final String profilePrefix = "";
        updateSAFRoleMapper(PROFILE_PATTERN, false);

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));
            }
        });
        assertEquals("user", defaultRoleMapper.getProfileFromRole("someResource", "user"));
    }

    /**
     * Test for SAFRoleMapperImpl
     * when configured with characters that are not accepted(%&*<blank>) by EJBROLE class,
     * those characters are replaced with #
     */
    @Test
    public void getProfile_unAcceptedChars() throws Exception {
        createMockEnv();
        String PROFILE_PATTERN = "%resource%.%role%";
        final String profilePrefix = "";
        updateSAFRoleMapper(PROFILE_PATTERN, false);

        mockery.checking(new Expectations() {
            {
                atLeast(5).of(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));
            }
        });
        assertEquals("#some#Re#source.user", defaultRoleMapper.getProfileFromRole("&some%Re*source", "user"));
        assertEquals("#some#Resource.user", defaultRoleMapper.getProfileFromRole(" some Resource", "user"));
        assertEquals("someResource.u#ser#", defaultRoleMapper.getProfileFromRole("someResource", "u#ser*"));
        assertEquals("someResource.#user#", defaultRoleMapper.getProfileFromRole("someResource", "&user "));
        assertEquals("someResource.#user", defaultRoleMapper.getProfileFromRole("someResource", " user"));
    }

    /**
     * Test for SAFRoleMapperImpl
     * passing null for resource and role parameters, null will get substituted to ""
     */
    @Test
    public void getProfile_nullParameters() throws Exception {
        createMockEnv();
        String PROFILE_PATTERN = "%resource%.%role%";
        final String profilePrefix = "";
        updateSAFRoleMapper(PROFILE_PATTERN, false);
        mockery.checking(new Expectations() {
            {
                atLeast(3).of(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));
            }
        });
        assertEquals(".user", defaultRoleMapper.getProfileFromRole(null, "user"));
        assertEquals("someResource.", defaultRoleMapper.getProfileFromRole("someResource", null));
        assertEquals(".", defaultRoleMapper.getProfileFromRole(null, null));
    }

    /**
     * Test for SAFRoleMapperImpl
     * passing empty string "" for resource and role parameters
     */
    @Test
    public void getProfile_emptyParameters() throws Exception {
        createMockEnv();
        String PROFILE_PATTERN = "%resource%.%role%";
        final String profilePrefix = "";
        updateSAFRoleMapper(PROFILE_PATTERN, false);
        mockery.checking(new Expectations() {
            {
                atLeast(3).of(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));
            }
        });
        assertNotNull(defaultRoleMapper.getProfileFromRole("", "user"));
        assertNotNull(defaultRoleMapper.getProfileFromRole("someResource", ""));
        assertNotNull(defaultRoleMapper.getProfileFromRole("", ""));
    }

}
