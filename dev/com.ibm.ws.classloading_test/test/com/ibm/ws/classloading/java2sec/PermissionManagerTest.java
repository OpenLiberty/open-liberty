/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.java2sec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;

import javax.security.auth.AuthPermission;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.url.URLStreamHandlerService;

import com.ibm.ws.kernel.boot.security.WLPDynamicPolicy;
import com.ibm.wsspi.classloading.ClassLoadingService;

public class PermissionManagerTest {

    /**
     * The permission manager object
     */
    PermissionManager permissionManager = new PermissionManager();

    /**
     * The Mock framework
     */
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /**
     * Component Context mock object
     */
    private final ComponentContext cc = mock.mock(ComponentContext.class);

    private final BundleContext bc = mock.mock(BundleContext.class);

    /**
     * Service Reference Mock Object
     */
    @SuppressWarnings("unchecked")
    private final ServiceReference<JavaPermissionsConfiguration> permissionRef = mock.mock(ServiceReference.class, "permissionRef");

    /**
     * Permisison Object
     */
    JavaPermissionsConfiguration permission = new JavaPermissionsConfiguration();

    /**
     * Service Reference Mock Object
     */
    @SuppressWarnings("unchecked")
    private final ServiceReference<JavaPermissionsConfiguration> permission1Ref = mock.mock(ServiceReference.class, "permission1Ref");

    /**
     * Permisison Object
     */
    JavaPermissionsConfiguration permission1 = new JavaPermissionsConfiguration();

    /**
     * ClassLoading service
     * 
     * @throws IOException
     */
    private final ClassLoadingService classLoadingService = mock.mock(ClassLoadingService.class, "classLoadingService");

    @SuppressWarnings("unchecked")
    private final ServiceReference<URLStreamHandlerService> urlStreamHandlerServiceRef = mock.mock(ServiceReference.class, "urlStreamHandlerServiceRef");

    private final WLPDynamicPolicy wlpDynamicPolicy = mock.mock(WLPDynamicPolicy.class);

    private Policy savedPolicy;
    private final Permission fileReadPermission = new FilePermission("", "read");
    private final Permission propertyReadPermission = new PropertyPermission("os.name", "read");

    @Before
    public void setup() throws IOException {
        permissionManager = new PermissionManager();
        savedPolicy = Policy.getPolicy();

        mock.checking(new Expectations() {
            {
                one(classLoadingService).setSharedLibraryProtectionDomains(with(any(Map.class)));

                allowing(cc).getBundleContext();
                will(returnValue(bc));

                allowing(bc).getProperty("wlp.process.type");
                will(returnValue("server"));

            }
        });

        permissionManager.setWsjarURLStreamHandler(urlStreamHandlerServiceRef);
        permissionManager.setClassLoadingService(classLoadingService);
        permissionManager.activate(cc, null);
    }

    @After
    public void tearDown() {
        Policy.setPolicy(savedPolicy);
        permissionManager.deactivate(cc);
        mock.assertIsSatisfied();
    }

    private void grantExitVM() {
        mock.checking(new Expectations() {
            {
                allowing(permissionRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));

                allowing(permissionRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService("permission", permissionRef);
                will(returnValue(permission));
            }
        });

        HashMap<String, Object> permissionProperties = new HashMap<String, Object>();
        permissionProperties.put(JavaPermissionsConfiguration.PERMISSION, "java.lang.RuntimePermission");
        permissionProperties.put(JavaPermissionsConfiguration.TARGET_NAME, "exitVM");
        permission.activate(permissionProperties, cc);

        permissionManager.setPermission(permissionRef);
        permissionManager.deactivate(cc);
        permissionManager.activate(cc, null);
    }

    private void grantReadProperty(String codeBase, boolean restrict) {
        mock.checking(new Expectations() {
            {
                allowing(permissionRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));

                allowing(permissionRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService("permission", permissionRef);
                will(returnValue(permission));
            }
        });

        HashMap<String, Object> permissionProperties = new HashMap<String, Object>();
        permissionProperties.put(JavaPermissionsConfiguration.PERMISSION, "java.util.PropertyPermission");
        permissionProperties.put(JavaPermissionsConfiguration.TARGET_NAME, "os.name");
        permissionProperties.put(JavaPermissionsConfiguration.ACTIONS, "read");
        if (restrict) {
            permissionProperties.put(JavaPermissionsConfiguration.RESTRICTION, Boolean.TRUE);
        }
        if (codeBase != null) {
            permissionProperties.put(JavaPermissionsConfiguration.CODE_BASE, codeBase);
        }

        permission.activate(permissionProperties, cc);

        permissionManager.setPermission(permissionRef);
        permissionManager.deactivate(cc);
        permissionManager.activate(cc, null);
    }

    private void grantAndRestrictReadProperty() {
        mock.checking(new Expectations() {
            {
                allowing(permissionRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));

                allowing(permissionRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService("permission", permissionRef);
                will(returnValue(permission));

                allowing(permission1Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));

                allowing(permission1Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService("permission", permission1Ref);
                will(returnValue(permission1));
            }
        });

        HashMap<String, Object> permissionProperties = new HashMap<String, Object>();
        permissionProperties.put(JavaPermissionsConfiguration.PERMISSION, "java.util.PropertyPermission");
        permissionProperties.put(JavaPermissionsConfiguration.TARGET_NAME, "os.name");
        permissionProperties.put(JavaPermissionsConfiguration.ACTIONS, "read");

        permission.activate(permissionProperties, cc);

        permissionProperties = new HashMap<String, Object>();
        permissionProperties.put(JavaPermissionsConfiguration.PERMISSION, "java.util.PropertyPermission");
        permissionProperties.put(JavaPermissionsConfiguration.TARGET_NAME, "os.name");
        permissionProperties.put(JavaPermissionsConfiguration.ACTIONS, "read");
        permissionProperties.put(JavaPermissionsConfiguration.RESTRICTION, "true");

        permission1.activate(permissionProperties, cc);

        permissionManager.setPermission(permissionRef);
        permissionManager.setPermission(permission1Ref);
        permissionManager.deactivate(cc);
        permissionManager.activate(cc, null);
    }

    @Test
    public void restrictablePermissionsDefault() {
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();
        assertEquals("Number of permissions mismatched", 4, permissions.size());
    }

    @Test
    public void restrictablePermissionsWithExitVMGrant() {
        createClassLoadingServiceExpectations();
        grantExitVM();
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();
        assertEquals("Number of permissions mismatched", 4, permissions.size());
    }

    private void createClassLoadingServiceExpectations() {
        mock.checking(new Expectations() {
            {
                one(classLoadingService).setSharedLibraryProtectionDomains(with(any(Map.class)));
            }
        });
    }

    @Test
    public void restrictablePermissionsWithReadPropertyRestrict() {
        createClassLoadingServiceExpectations();
        grantReadProperty(null, true);
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();
        assertEquals("Number of permissions mismatched", 5, permissions.size());
    }

    @Test
    public void restrictablePermissionsWithReadPropertyGrantAndRestrict() {
        createClassLoadingServiceExpectations();
        grantAndRestrictReadProperty();
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();
        assertEquals("Number of permissions mismatched", 4, permissions.size());

        permissions = permissionManager.getEffectivePermissions(permissions, "dummyCodebase");
        assertEquals("Number of granted permissions mismatched", 1, permissions.size());
    }

    @Test
    public void restrictablePermissionsWithReadpropertyGrant() {
        createClassLoadingServiceExpectations();
        grantReadProperty(null, false);
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();
        assertEquals("Number of restrictable permissions mismatched", 4, permissions.size());

        permissions = permissionManager.getEffectivePermissions(permissions, "dummyCodebase");
        assertEquals("Number of granted permissions mismatched", 1, permissions.size());
    }

    @Test
    public void getEffectivePermissionsMergeAllGrantedPermissions() throws Exception {
        createClassLoadingServiceExpectations();
        grantReadProperty(null, false);
        CodeSource appCodeSource = new CodeSource(new URL("file:///aPath/appWAR"), (java.security.cert.Certificate[]) null);
        PermissionCollection staticPolicyPermissions = Policy.getPolicy().getPermissions(appCodeSource);
        permissionManager.addPermissionsXMLPermission("aPath/appWAR", fileReadPermission);
        // permissionManager.setCodeBasePermission("aPath/appWAR", propertyReadPermission);
        PermissionCollection permissions = permissionManager.getCombinedPermissions(staticPolicyPermissions, appCodeSource);
        List<Permission> permissionsList = Collections.list(permissions.elements());
        List<Permission> staticPermissionsList = Collections.list(staticPolicyPermissions.elements());

        assertTrue("The permissions must be merged with the static permissions.", permissionsList.containsAll(staticPermissionsList));
        assertTrue("The permissions must be merged with the permissions.xml permissions.", permissionsList.contains(fileReadPermission));
        assertTrue("The permissions must be merged with the server.xml permissions.", permissionsList.contains(propertyReadPermission));
        // The JSP class loader adds an additional permission and the collection cannot be read only.
        assertFalse("The permissions must not be read only.", permissions.isReadOnly());
    }

    @Test
    public void getEffectivePermissionsMergeAllGrantedPermissionsExceptStatic() throws Exception {
        createClassLoadingServiceExpectations();
        grantReadProperty(null, false);
        CodeSource appCodeSource = new CodeSource(new URL("file:///aPath/appWAR"), (java.security.cert.Certificate[]) null);
        PermissionCollection staticPolicyPermissions = Policy.getPolicy().getPermissions(appCodeSource);
        permissionManager.addPermissionsXMLPermission("aPath/appWAR", fileReadPermission);
        PermissionCollection permissions = permissionManager.getCombinedPermissions(new Permissions(), appCodeSource);
        List<Permission> permissionsList = Collections.list(permissions.elements());
        List<Permission> staticPermissionsList = Collections.list(staticPolicyPermissions.elements());

        assertFalse("The permissions must nost be merged with the static permissions.", permissionsList.containsAll(staticPermissionsList));
        assertTrue("The permissions must be merged with the permissions.xml permissions.", permissionsList.contains(fileReadPermission));
        assertTrue("The permissions must be merged with the server.xml permissions.", permissionsList.contains(propertyReadPermission));
    }

    /*
     * TODO: For some reason the URL cannot be created when adding the code base.
     * This test is kept because it tests adding a permission with code base and ensuring that it is granted.
     */
    @Test
    public void getEffectivePermissionsAddCodebaseWithDoubleSlashesMustCanonicalize() throws Exception {
        File[] roots = File.listRoots();
        String codeBase = "aPath//toThe/appWAR.jar";
        if (roots != null && roots.length > 0) {
            codeBase = roots[0].getCanonicalPath() + codeBase;
        }
        final String normalizedCodebase = codeBase.replace("\\", "/").replace("//", "/");

        permissionManager.setClassLoadingService(classLoadingService);
        mock.checking(new Expectations() {
            {
                one(classLoadingService).setSharedLibraryProtectionDomains(with(new BaseMatcher<Map<String, ProtectionDomain>>() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public boolean matches(Object arg0) {
                        if (arg0 instanceof Map<?, ?>) {
                            Map<String, ProtectionDomain> protectionDomainMap = (Map<String, ProtectionDomain>) arg0;
                            return protectionDomainMap.containsKey(normalizedCodebase);
                        }
                        return false;
                    }

                    @Override
                    public void describeTo(Description arg0) {}
                }));
            }
        });

        grantReadProperty(codeBase, false);
        CodeSource appCodeSource = new CodeSource(new URL("file:///aPath/toThe/appWAR.jar"), (java.security.cert.Certificate[]) null);
        PermissionCollection staticPolicyPermissions = Policy.getPolicy().getPermissions(appCodeSource);
        permissionManager.addPermissionsXMLPermission("aPath/toThe/appWAR.jar", fileReadPermission);
        PermissionCollection permissions = permissionManager.getCombinedPermissions(staticPolicyPermissions, appCodeSource);
        List<Permission> permissionsList = Collections.list(permissions.elements());
        List<Permission> staticPermissionsList = Collections.list(staticPolicyPermissions.elements());

        assertTrue("The permissions must be merged with the static permissions.", permissionsList.containsAll(staticPermissionsList));
        assertTrue("The permissions must be merged with the permissions.xml permissions.", permissionsList.contains(fileReadPermission));
        assertTrue("The permissions must be merged with the server.xml permissions.", permissionsList.contains(propertyReadPermission));
        // The JSP class loader adds an additional permission and the collection cannot be read only.
        assertFalse("The permissions must not be read only.", permissions.isReadOnly());
    }

    @Test
    public void activateDeactivateSetsAndRemovesSelfInWLPDynamicPolicy() throws Exception {
        final PermissionManager anotherPermissionManager = new PermissionManager();
        Policy.setPolicy(wlpDynamicPolicy);
        mock.checking(new Expectations() {
            {
                one(wlpDynamicPolicy).setPermissionsCombiner(anotherPermissionManager);
                one(wlpDynamicPolicy).setPermissionsCombiner(null);
            }
        });

        createClassLoadingServiceExpectations();
        anotherPermissionManager.setClassLoadingService(classLoadingService);
        anotherPermissionManager.activate(cc, null);
        anotherPermissionManager.deactivate(cc);
    }

    @Test
    public void wsjarUrlStreamHandlerRemoved() throws Exception {
        createClassLoadingServiceExpectations();
        grantReadProperty(null, false);

        permissionManager.unsetWsjarURLStreamHandler(urlStreamHandlerServiceRef);
        permissionManager.unsetPermission(permissionRef);
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();
        permissions = permissionManager.getEffectivePermissions(permissions, "dummyCodebase");
        assertEquals("Number of granted permissions mismatched", 1, permissions.size());

    }

    @Test
    public void getEffectivePermissionLongPaths() throws Exception {
        String earCodeBase = "/home/Jazz_Build/_FsFcIKC-Eea1sp7ekxxX3Q-EBC.PROD.WASRTC-9J5-000-00-00/jbe/build/image/output/wlp/usr/servers/com.ibm.ws.webcontainer.security.fat.delegation/apps/delegationXML.ear";
        String codeBase = "/home/Jazz_Build/_FsFcIKC-Eea1sp7ekxxX3Q-EBC.PROD.WASRTC-9J5-000-00-00/jbe/build/image/output/wlp/usr/servers/com.ibm.ws.webcontainer.security.fat.delegation/apps/delegation.war";
        permissionManager.addPermissionsXMLPermission(earCodeBase, new AuthPermission("wssecurity.getRunAsSubject"));
        permissionManager.addPermissionsXMLPermission(codeBase, new AuthPermission("wssecurity.getRunAsSubject"));
        List<Permission> permissions = permissionManager.getEffectivePermissions(codeBase);
        assertTrue(new AuthPermission("wssecurity.getRunAsSubject").implies(permissions.get(0)));
    }
}
