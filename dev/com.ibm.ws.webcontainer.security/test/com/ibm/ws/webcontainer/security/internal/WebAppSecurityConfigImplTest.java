/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class WebAppSecurityConfigImplTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final WebAppSecurityConfig mockedConfig = mock.mock(WebAppSecurityConfig.class);
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = mock.mock(AtomicServiceReference.class, "locationAdminRef");
    private final AtomicServiceReference<SecurityService> securityServiceRef = mock.mock(AtomicServiceReference.class, "securityServiceRef");
    private final WsLocationAdmin locateService = mock.mock(WsLocationAdmin.class);

    private final String USER_DIR = "userDir";
    private final String SERVER_NAME = "serverName";

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(locationAdminRef).getService();
                will(returnValue(locateService));
                allowing(locateService).resolveString(WebAppSecurityConfigImpl.WLP_USER_DIR);
                will(returnValue(USER_DIR));
                allowing(locateService).getServerName();
                will(returnValue(SERVER_NAME));
            }
        });
    }

    @Test
    public void testGetSSODomainList_noUseDomainFromURL() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        mockCookie(cfg, false);
        cfg.put("ssoDomainNames", "austin.ibm.com|raleigh.ibm.com");

        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        List<String> webCfgList = webCfg.getSSODomainList();
        assertTrue(webCfgList.contains("austin.ibm.com"));
        assertTrue(webCfgList.contains("raleigh.ibm.com"));
    }

    @Test
    public void testGetSSODomainList_with_UseDomainFromURL() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        mockCookie(cfg, false);
        cfg.put("ssoDomainNames", "austin.ibm.com|raleigh.ibm.com|useDomainFromURL");

        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        List<String> webCfgList = webCfg.getSSODomainList();
        assertTrue(webCfgList.contains("austin.ibm.com"));
        assertTrue(webCfgList.contains("raleigh.ibm.com"));
    }

    /**
     * If the implementation of the WebAppSecurityConfig is not an
     * WebAppSecurityConfigImpl, an empty String should be returned.
     */
    @Test
    public void getChangedProperties_wrongImplementation() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        mockCookie(cfg, false);
        cfg.put("ssoDomainNames", "austin.ibm.com|raleigh.ibm.com|useDomainFromURL");

        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("When the object is not the same implementation type, an empty String should be returned",
                     "", webCfg.getChangedProperties(mockedConfig));
    }

    /**
     * If the implementation of the WebAppSecurityConfig the same as the one
     * being compared to, an empty String should be returned.
     */
    @Test
    public void getChangedProperties_noChange() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        mockCookie(cfg, false);
        cfg.put("ssoDomainNames", "austin.ibm.com|raleigh.ibm.com|useDomainFromURL");

        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("When the object is the same, an empty String should be returned",
                     "", webCfg.getChangedProperties(webCfg));
    }

    /**
     * Ensure that when only one property changes, only that property should be
     * returned as modified.
     */
    @Test
    public void getChangedProperties_oneChange() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        mockCookie(cfg, false);
        cfg.put("ssoCookieName", "webSSOCookie");
        cfg.put("ssoDomainNames", "austin.ibm.com|raleigh.ibm.com|useDomainFromURL");
        WebAppSecurityConfig webCfgOld = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);

        String newValue = "austin.ibm.com|raleigh.ibm.com";
        // Intentionally causing a new String to be created to guard against
        // accidentally doing instance comparison
        cfg.put("ssoCookieName", new String("webSSOCookie"));
        cfg.put("ssoDomainNames", newValue);
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("When one setting has changed, that new value should be returned",
                     "ssoDomainNames=" + newValue, webCfg.getChangedProperties(webCfgOld));
    }

    /**
     * When multiple properties change, all modified properties should be listed.
     */
    @Test
    public void getChangedProperties_fewChanges() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        cfg.put("allowFailOverToBasicAuth", Boolean.FALSE);
        cfg.put("displayAuthenticationRealm", Boolean.FALSE);
        cfg.put("ssoCookieName", "webSSOCookie");
        cfg.put("autoGenSsoCookieName", Boolean.FALSE);
        cfg.put("ssoDomainNames", "austin.ibm.com|raleigh.ibm.com|useDomainFromURL");
        cfg.put("webAlwaysLogin", Boolean.TRUE);
        WebAppSecurityConfig webCfgOld = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);

        String newCookieValue = "mySSOCookie";
        cfg.put("ssoCookieName", newCookieValue);
        String newDomainValue = "";
        cfg.put("ssoDomainNames", newDomainValue);
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("Only the settings that changed should be listed",
                     "ssoCookieName=" + newCookieValue + ",ssoDomainNames=" + newDomainValue,
                     webCfg.getChangedProperties(webCfgOld));
    }

    @Test
    public void getChangedProperties_allChanged() {

        WebAppSecurityConfig webCfgOld = new WebAppSecurityConfigImpl(createMapOriginal(), locationAdminRef, securityServiceRef);
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(createMapChanged(), locationAdminRef, securityServiceRef);

        assertEquals("When all settings have changed, all should be listed",
                     "allowFailOverToBasicAuth=false,autoGenSsoCookieName=true,basicAuthenticationMechanismRealmName=newRealm,contextRootForFormAuthenticationMechanism=/modified,displayAuthenticationRealm=true,loginErrorURL=modifiedLoginError,overrideHttpAuthMethod=modified,ssoCookieName=mySSOCookie,ssoDomainNames=,webAlwaysLogin=false",
                     webCfg.getChangedProperties(webCfgOld));
    }

    @Test
    public void getChangedPropertiesMap_allChanged() {

        WebAppSecurityConfig webCfgOld = new WebAppSecurityConfigImpl(createMapOriginal(), locationAdminRef, securityServiceRef);
        Map<String, Object> changed = createMapChanged();
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(changed, locationAdminRef, securityServiceRef);
        Map<String, String> expected = convertValues(changed);
        assertTrue("When all settings have changed, all should be listed", expected.equals(webCfg.getChangedPropertiesMap(webCfgOld)));
    }

    private Map<String, Object> createMapOriginal() {
        Map<String, Object> cfg = new HashMap<String, Object>() {
            {
                put("allowFailOverToBasicAuth", Boolean.TRUE);
                put("displayAuthenticationRealm", Boolean.FALSE);
                put("ssoCookieName", "webSSOCookie");
                put("autoGenSsoCookieName", Boolean.FALSE);
                put("ssoDomainNames", "austin.ibm.com|raleigh.ibm.com|useDomainFromURL");
                put("webAlwaysLogin", Boolean.TRUE);
                put("loginErrorURL", "originalLoginError");
                put("contextRootForFormAuthenticationMechanism", "/original");
                put("basicAuthenticationMechanismRealmName", "realm");
                put("overrideHttpAuthMethod", "original");
            }
        };
        return cfg;
    }

    private Map<String, Object> createMapChanged() {
        Map<String, Object> cfg = new HashMap<String, Object>() {
            {
                put("allowFailOverToBasicAuth", Boolean.FALSE);
                put("displayAuthenticationRealm", Boolean.TRUE);
                put("ssoCookieName", "mySSOCookie");
                put("autoGenSsoCookieName", Boolean.TRUE);
                put("ssoDomainNames", "");
                put("webAlwaysLogin", Boolean.FALSE);
                put("loginErrorURL", "modifiedLoginError");
                put("contextRootForFormAuthenticationMechanism", "/modified");
                put("basicAuthenticationMechanismRealmName", "newRealm");
                put("overrideHttpAuthMethod", "modified");
            }
        };
        return cfg;
    }

    private Map<String, String> convertValues(Map<String, Object> original) {
        Map<String, String> output = new TreeMap<String, String>();
        for (Entry<String, Object> entry : original.entrySet()) {
            output.put(entry.getKey(), entry.getValue().toString());
        }
        return output;
    }

    private void driveSingleAttributeTest(String name, Object oldValue, Object newValue) {
        Map<String, Object> cfg = new TreeMap<String, Object>();
        cfg.put(name, oldValue);
        WebAppSecurityConfig webCfgOld = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);

        cfg.put(name, newValue);
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("Did not get expected name value pair for attribute " + name,
                     name + "=" + newValue, webCfg.getChangedProperties(webCfgOld));

        assertEquals("Did not get expected result from getChangedPropertiesMap. attribute name : " + name, convertValues(cfg), webCfg.getChangedPropertiesMap(webCfgOld));
    }

    @Test
    public void getChangedProperties_allowFailOverToBasicAuth() {
        driveSingleAttributeTest("allowFailOverToBasicAuth",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_allowLogoutPageRedirectToAnyHost() {
        driveSingleAttributeTest("allowLogoutPageRedirectToAnyHost",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_displayAuthenticationRealm() {
        driveSingleAttributeTest("displayAuthenticationRealm",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_httpOnlyCookies() {
        driveSingleAttributeTest("httpOnlyCookies",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_logoutOnHttpSessionExpire() {
        driveSingleAttributeTest("logoutOnHttpSessionExpire",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_logoutPageRedirectDomainNames() {
        driveSingleAttributeTest("logoutPageRedirectDomainNames",
                                 "abc", "abc|123");
    }

    @Test
    public void getChangedProperties_preserveFullyQualifiedReferrerUrl() {
        driveSingleAttributeTest("preserveFullyQualifiedReferrerUrl",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_postParamCookieSize() {
        driveSingleAttributeTest("postParamCookieSize",
                                 1024, 2048);
    }

    @Test
    public void getChangedProperties_postParamSaveMethod() {
        driveSingleAttributeTest("postParamSaveMethod",
                                 "NONE", "COOKIE");
    }

    @Test
    public void getChangedProperties_singleSignonEnabled() {
        driveSingleAttributeTest("singleSignonEnabled",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_ssoCookieName() {
        driveSingleAttributeTest("ssoCookieName",
                                 "webSSOCookie", "mySSOCookie");
    }

    @Test
    public void getChangedProperties_useOnlyCustomCookieName() {
        driveSingleAttributeTest("useOnlyCustomCookieName",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_autoGenSsoCookieName() {
        driveSingleAttributeTest("autoGenSsoCookieName",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_ssoDomainNames() {
        driveSingleAttributeTest("ssoDomainNames",
                                 "ibm.com", "google.com");
    }

    @Test
    public void getChangedProperties_ssoRequiresSSL() {
        driveSingleAttributeTest("ssoRequiresSSL",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_ssoUseDomainFromURL() {
        driveSingleAttributeTest("ssoUseDomainFromURL",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_useLtpaSSOForJaspic() {
        driveSingleAttributeTest("useLtpaSSOForJaspic",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_useAuthenticationDataForUnprotectedResource() {
        driveSingleAttributeTest("useAuthenticationDataForUnprotectedResource",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_webAlwaysLogin() {
        driveSingleAttributeTest("webAlwaysLogin",
                                 Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void getChangedProperties_loginErrorURL() {
        driveSingleAttributeTest("loginErrorURL",
                                 "", "/new");
    }

    @Test
    public void getChangedProperties_overrideHttpAuthMethod() {
        driveSingleAttributeTest("overrideHttpAuthMethod",
                                 "BASIC", "FORM");
    }

    @Test
    public void getChangedProperties_contextRootForFormAuthenticationMechanism() {
        driveSingleAttributeTest("contextRootForFormAuthenticationMechanism",
                                 "someValue", "");
    }

    @Test
    public void getChangedProperties_basicAuthenticationMechanismRealmName() {
        driveSingleAttributeTest("basicAuthenticationMechanismRealmName",
                                 "old", "new");
    }

    @Test
    public void testSetSsoCookieName_autoGenSsoCookieName_true_defaultSsoCookieName() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        mockCookie(cfg, true);
        cfg.put("ssoCookieName", WebAppSecurityConfigImpl.DEFAULT_SSO_COOKIE_NAME);
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        String expectCookieName = createExpectSsoCookieName(webCfg);
        assertEquals("Did not get expected ssoCookieName " + expectCookieName, expectCookieName, webCfg.getSSOCookieName());
    }

    @Test
    public void testSetSsoCookieName_autoGenSsoCookieName_true_notDefaultSsoCookieName() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        mockCookie(cfg, true);
        cfg.put("ssoCookieName", "myCookieName");
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("Did not get expected ssoCookieName myCookieName", "myCookieName", webCfg.getSSOCookieName());
    }

    @Test
    public void testGetLoginErrorURL_NotSet() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertNull("Null should be get since the value is not set.", webCfg.getLoginErrorURL());
    }

    @Test
    public void testGetLoginErrorURL_Valid() {
        final String ERROR_URL = "/globalLogin/errorPage.html";
        Map<String, Object> cfg = new HashMap<String, Object>();
        cfg.put("loginErrorURL", ERROR_URL);
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("Vallid value should be returned.", ERROR_URL, webCfg.getLoginErrorURL());
    }

    @Test
    public void testGetLoginFormContextRoot_NotSet() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertNull("Null should be get since the value is not set.", webCfg.getLoginFormContextRoot());
    }

    @Test
    public void testGetLoginFormContextRoot_Valid() {
        final String CONTEXT_ROOT = "/globalLogin";
        Map<String, Object> cfg = new HashMap<String, Object>();
        cfg.put("contextRootForFormAuthenticationMechanism", CONTEXT_ROOT);
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("Vallid value should be returned.", CONTEXT_ROOT, webCfg.getLoginFormContextRoot());
    }

    @Test
    public void testGetBasicAuthRealmName_NotSet() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertNull("Null should be get since the value is not set.", webCfg.getBasicAuthRealmName());
    }

    @Test
    public void testGetBasicAuthRealmName_Valid() {
        final String REALM_NAME = "realmName";
        Map<String, Object> cfg = new HashMap<String, Object>();
        cfg.put("basicAuthenticationMechanismRealmName", REALM_NAME);
        WebAppSecurityConfig webCfg = new WebAppSecurityConfigImpl(cfg, locationAdminRef, securityServiceRef);
        assertEquals("Vallid value should be returned.", REALM_NAME, webCfg.getBasicAuthRealmName());
    }

    /**
     * @return
     */
    private String createExpectSsoCookieName(WebAppSecurityConfig webCfg) {

        String slash = USER_DIR.endsWith("/") ? "" : "/";
        String serverUniq = getHostName() + "_" + USER_DIR + slash + "servers/" + SERVER_NAME;
        return "WAS_" + hash(serverUniq);
    }

    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
        } catch (java.net.UnknownHostException e) {
            return "localhost";
        }
    }

    /**
     * @param cfg
     * @param autoGenCookieName TODO
     */
    private void mockCookie(Map<String, Object> cfg, Boolean autoGenCookieName) {
        if (!autoGenCookieName) {
            cfg.put("ssoCookieName", "webSSOCookie");
        }
        cfg.put("autoGenSsoCookieName", autoGenCookieName);
    }

    static String hash(String stringToEncrypt) {
        int hashCode = stringToEncrypt.hashCode();
        if (hashCode < 0) {
            hashCode = hashCode * -1;
            return "n" + hashCode;
        } else {
            return "p" + hashCode;
        }
    }
}
