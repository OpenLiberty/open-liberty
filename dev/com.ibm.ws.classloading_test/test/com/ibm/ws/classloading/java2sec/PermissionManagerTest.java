/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.url.URLStreamHandlerService;

import com.ibm.ws.kernel.boot.security.WLPDynamicPolicy;
import com.ibm.wsspi.classloading.ClassLoadingService;

public class PermissionManagerTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("unchecked")
    private final ServiceReference<JavaPermissionsConfiguration> permissionRef = mock.mock(ServiceReference.class, "permissionRef");

    @SuppressWarnings("unchecked")
    private final ServiceReference<URLStreamHandlerService> urlStreamHandlerServiceRef = mock.mock(ServiceReference.class, "urlStreamHandlerServiceRef");

    private PermissionManager permissionManager = new PermissionManager();
    private final ComponentContext componentContext = mock.mock(ComponentContext.class);
    private final BundleContext bundleContext = mock.mock(BundleContext.class);
    private final ClassLoadingService classLoadingService = mock.mock(ClassLoadingService.class, "classLoadingService");
    private final WLPDynamicPolicy wlpDynamicPolicy = mock.mock(WLPDynamicPolicy.class);
    private final FrameworkWiring frameworkWiring = mock.mock(FrameworkWiring.class);
    private Policy savedPolicy;
    private final Permission fileReadPermission = new FilePermission("", "read");
    private final Permission propertyReadPermission = new PropertyPermission("os.name", "read");
    private final Collection<BundleCapability> matchingCapabilities = new HashSet<BundleCapability>();

    @Before
    public void setup() throws IOException {
        inServerProcess();
        withSharedLibraryProtectionDomains();
        withSystemBundleAndCapabilities();

        savedPolicy = Policy.getPolicy();
        permissionManager = new PermissionManager();
        permissionManager.setWsjarURLStreamHandler(urlStreamHandlerServiceRef);
        permissionManager.setClassLoadingService(classLoadingService);
        permissionManager.activate(componentContext);
    }

    private void inServerProcess() {
        mock.checking(new Expectations() {
            {
                allowing(componentContext).getBundleContext();
                will(returnValue(bundleContext));
                allowing(bundleContext).getProperty("wlp.process.type");
                will(returnValue("server"));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void withSharedLibraryProtectionDomains() {
        mock.checking(new Expectations() {
            {
                one(classLoadingService).setSharedLibraryProtectionDomains(with(any(Map.class)));
            }
        });
    }

    private void withSystemBundleAndCapabilities() {
        final Bundle systemBundle = mock.mock(Bundle.class);
        mock.checking(new Expectations() {
            {
                allowing(bundleContext).getBundle(org.osgi.framework.Constants.SYSTEM_BUNDLE_LOCATION);
                will(returnValue(systemBundle));
                allowing(systemBundle).adapt(FrameworkWiring.class);
                will(returnValue(frameworkWiring));
                allowing(frameworkWiring).findProviders(with(any(org.osgi.resource.Requirement.class)));
                will(returnValue(matchingCapabilities));

            }
        });
    }

    @After
    public void tearDown() {
        Policy.setPolicy(savedPolicy);
        permissionManager.deactivate(componentContext);
        mock.assertIsSatisfied();
    }

    @Test
    public void restrictablePermissionsDefault() {
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();

        assertEquals("Number of permissions mismatched", 4, permissions.size());
    }

    @Test
    public void restrictablePermissionsWithExitVMGrant() {
        withSharedLibraryProtectionDomains();
        grantExitVM();
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();

        assertEquals("Number of permissions mismatched", 4, permissions.size());
    }

    @Test
    public void restrictablePermissionsWithReadPropertyRestrict() {
        withSharedLibraryProtectionDomains();
        grantReadProperty(null, true);
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();

        assertEquals("Number of permissions mismatched", 5, permissions.size());
    }

    @Test
    public void restrictablePermissionsWithReadPropertyGrantAndRestrict() {
        withSharedLibraryProtectionDomains();
        grantAndRestrictReadProperty();
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();

        assertEquals("Number of permissions mismatched", 4, permissions.size());

        permissions = permissionManager.getEffectivePermissions(permissions, "dummyCodebase");
        assertEquals("Number of granted permissions mismatched", 1, permissions.size());
    }

    @Test
    public void restrictablePermissionsWithReadpropertyGrant() {
        withSharedLibraryProtectionDomains();
        grantReadProperty(null, false);
        ArrayList<Permission> permissions = permissionManager.getRestrictablePermissions();

        assertEquals("Number of restrictable permissions mismatched", 4, permissions.size());

        permissions = permissionManager.getEffectivePermissions(permissions, "dummyCodebase");
        assertEquals("Number of granted permissions mismatched", 1, permissions.size());
    }

    @Test
    public void getEffectivePermissionsMergeAllGrantedPermissions() throws Exception {
        withSharedLibraryProtectionDomains();
        grantReadProperty(null, false);
        CodeSource appCodeSource = new CodeSource(new URL("file:///aPath/appWAR"), (java.security.cert.Certificate[]) null);
        PermissionCollection staticPolicyPermissions = Policy.getPolicy().getPermissions(appCodeSource);
        permissionManager.addPermissionsXMLPermission(appCodeSource, fileReadPermission);
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
        withSharedLibraryProtectionDomains();
        grantReadProperty(null, false);
        CodeSource appCodeSource = new CodeSource(new URL("file:///aPath/appWAR"), (java.security.cert.Certificate[]) null);
        PermissionCollection staticPolicyPermissions = Policy.getPolicy().getPermissions(appCodeSource);
        permissionManager.addPermissionsXMLPermission(appCodeSource, fileReadPermission);
        PermissionCollection permissions = permissionManager.getCombinedPermissions(new Permissions(), appCodeSource);
        List<Permission> permissionsList = Collections.list(permissions.elements());
        List<Permission> staticPermissionsList = Collections.list(staticPolicyPermissions.elements());

        assertFalse("The permissions must not be merged with the static permissions.", permissionsList.containsAll(staticPermissionsList));
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
                    public void describeTo(Description arg0) {
                    }
                }));
            }
        });

        grantReadProperty(codeBase, false);
        CodeSource appCodeSource = new CodeSource(new URL("file:///aPath/toThe/appWAR.jar"), (java.security.cert.Certificate[]) null);
        PermissionCollection staticPolicyPermissions = Policy.getPolicy().getPermissions(appCodeSource);
        permissionManager.addPermissionsXMLPermission(appCodeSource, fileReadPermission);
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

        withSharedLibraryProtectionDomains();
        anotherPermissionManager.setClassLoadingService(classLoadingService);
        anotherPermissionManager.activate(componentContext);
        anotherPermissionManager.deactivate(componentContext);
    }

    @Test
    public void wsjarUrlStreamHandlerRemoved() throws Exception {
        withSharedLibraryProtectionDomains();
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
        CodeSource earCodeSource = new CodeSource(new URL("file://" + earCodeBase), (java.security.cert.Certificate[]) null);
        String codeBase = "/home/Jazz_Build/_FsFcIKC-Eea1sp7ekxxX3Q-EBC.PROD.WASRTC-9J5-000-00-00/jbe/build/image/output/wlp/usr/servers/com.ibm.ws.webcontainer.security.fat.delegation/apps/delegation.war";
        CodeSource codeSource = new CodeSource(new URL("file://" + codeBase), (java.security.cert.Certificate[]) null);
        permissionManager.addPermissionsXMLPermission(earCodeSource, new AuthPermission("wssecurity.getRunAsSubject"));
        permissionManager.addPermissionsXMLPermission(codeSource, new AuthPermission("wssecurity.getRunAsSubject"));
        List<Permission> permissions = permissionManager.getEffectivePermissions(codeBase);

        assertTrue(new AuthPermission("wssecurity.getRunAsSubject").implies(permissions.get(0)));
    }

    @Test
    public void testPermissionFromBundle() throws Exception {
        withSharedLibraryProtectionDomains();
        String bundlePermissionClassName = "class.from.bundle.PermissionClass";
        usesBundleClassLoaderToLoadPermissionClass(bundlePermissionClassName);

        setupPermission(bundlePermissionClassName, "someName", null, false, null);
    }

    private void grantExitVM() {
        setupPermission("java.lang.RuntimePermission", "exitVM", null, false, null);
    }

    private void grantReadProperty(String codeBase, boolean restrict) {
        setupPermission("java.util.PropertyPermission", "os.name", "read", restrict, codeBase);
    }

    @SuppressWarnings("unchecked")
    private void grantAndRestrictReadProperty() {
        ServiceReference<JavaPermissionsConfiguration> permissionRef = createPermissionRef("java.util.PropertyPermission", "os.name", "read", false, null);
        ServiceReference<JavaPermissionsConfiguration> permission1Ref = createPermissionRef("java.util.PropertyPermission", "os.name", "read", true, null);
        setPermissions(permissionRef, permission1Ref);
    }

    @SuppressWarnings("unchecked")
    private void setupPermission(String className, String name, String actions, boolean restrict, String codeBase) {
        ServiceReference<JavaPermissionsConfiguration> permissionRef = createPermissionRef(className, name, actions, restrict, codeBase);
        setPermissions(permissionRef);
    }

    @SuppressWarnings("unchecked")
    private ServiceReference<JavaPermissionsConfiguration> createPermissionRef(String className, String name, String actions, boolean restrict, String codeBase) {
        final ServiceReference<JavaPermissionsConfiguration> permissionRef = mock.mock(ServiceReference.class,
                                                                                       "permissionRef:" + className + ":" + name + ":" + actions + ":" + restrict + ":" + codeBase);
        final JavaPermissionsConfiguration permission = new JavaPermissionsConfiguration();

        mock.checking(new Expectations() {
            {
                allowing(permissionRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));

                allowing(permissionRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(componentContext).locateService("permission", permissionRef);
                will(returnValue(permission));
            }
        });

        HashMap<String, Object> permissionProperties = createPermissionProperties(className, name, actions, restrict, codeBase);
        permission.activate(permissionProperties, componentContext);

        return permissionRef;
    }

    private HashMap<String, Object> createPermissionProperties(String className, String name, String actions, boolean restrict, String codeBase) {
        HashMap<String, Object> permissionProperties = new HashMap<String, Object>();
        permissionProperties.put(JavaPermissionsConfiguration.PERMISSION, className);
        permissionProperties.put(JavaPermissionsConfiguration.TARGET_NAME, name);
        permissionProperties.put(JavaPermissionsConfiguration.ACTIONS, actions);
        permissionProperties.put(JavaPermissionsConfiguration.RESTRICTION, restrict);
        permissionProperties.put(JavaPermissionsConfiguration.CODE_BASE, codeBase);

        return permissionProperties;
    }

    @SuppressWarnings("unchecked")
    private void setPermissions(ServiceReference<JavaPermissionsConfiguration>... permissionRefs) {
        for (ServiceReference<JavaPermissionsConfiguration> permissionRef : permissionRefs) {
            permissionManager.setPermission(permissionRef);
        }

        permissionManager.deactivate(componentContext);
        permissionManager.activate(componentContext);
    }

    private void usesBundleClassLoaderToLoadPermissionClass(final String bundlePermissionClassName) throws ClassNotFoundException {
        final BundleCapability bundleCapability = mock.mock(BundleCapability.class);
        final BundleRevision bundleRevision = mock.mock(BundleRevision.class);
        final BundleWiring providerBundleWiring = mock.mock(BundleWiring.class);
        final ClassLoader classloader = mock.mock(ClassLoader.class);
        matchingCapabilities.add(bundleCapability);

        mock.checking(new Expectations() {
            {
                one(bundleCapability).getRevision();
                will(returnValue(bundleRevision));
                one(bundleRevision).getWiring();
                will(returnValue(providerBundleWiring));
                one(providerBundleWiring).getClassLoader();
                will(returnValue(classloader));
                one(classloader).loadClass(bundlePermissionClassName);
                will(returnValue(TestBundlePermission.class));
            }
        });
    }

    private class TestBundlePermission extends Permission {

        public TestBundlePermission(String name) {
            super(name);
        }

        @Override
        public boolean implies(Permission permission) {
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String getActions() {
            return null;
        }

    }

}
