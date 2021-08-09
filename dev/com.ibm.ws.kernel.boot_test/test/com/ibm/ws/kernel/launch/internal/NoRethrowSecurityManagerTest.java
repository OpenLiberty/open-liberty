/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import static com.ibm.ws.kernel.launch.internal.NoRethrowSecurityManager.lineSep;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FilePermission;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Enumeration;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for {@link NoRethrowSecurityManager} class.
 */
public class NoRethrowSecurityManagerTest {

    private static final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final Permission PERMISSION1 = new FilePermission("<<ALL FILES>>", "read");
    private static final Permission PERMISSION2 = new AllPermission();

    private static final CodeSource CODE_SOURCE2 = mock.mock(CodeSource.class, "source with no URL");

    private static final Permissions PERMISSIONS = new Permissions();

    private final ClassLoader CLASS_LOADER = ClassLoader.getSystemClassLoader();
    private final Class<?>[] CLASS_ARRAY = { String.class, NoRethrowSecurityManager.class,
                                            SecureClassLoader.class, org.osgi.service.permissionadmin.PermissionInfo.class };

    private final String CS_STR_NULL_CODE_SOURCE = "null code source";
    private final String CS_STR_NULL_URL = "null code URL";

    private final String PERMISSION_STR1 = "ClassLoader: Primordial Classloader" + lineSep +
                                           "  Permissions granted to CodeSource null" + lineSep +
                                           "  {" + lineSep +
                                           "  }";
    private String PERMISSION_STR2 = "ClassLoader: " + CLASS_LOADER.getClass().getName() + lineSep +
                                     "  Permissions granted to CodeSource " + CODE_SOURCE2 + lineSep +
                                     "  {" + lineSep +
                                     "    <perm1>;" + lineSep +
                                     "    <perm2>;" + lineSep +
                                     "  }";

    @BeforeClass
    public static void setup() {
        PERMISSIONS.add(PERMISSION1);
        PERMISSIONS.add(PERMISSION2);
    }

    @Test
    public void checkPermissionTest() {
        NoRethrowSecurityManager secManager = new NoRethrowSecurityManager();
        try {
            secManager.checkPermission(PERMISSION1);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void handleSecurityExceptionTest() {
        NoRethrowSecurityManager secManager = new NoRethrowSecurityManager();
        try {
            secManager.checkPermission(PERMISSION2);
        } catch (SecurityException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void getCodeSorce() {
        NoRethrowSecurityManager secManager = new NoRethrowSecurityManager();
        String csLocation = getClass().getProtectionDomain().getCodeSource().getLocation().toString();

        ProtectionDomain protectionDomain = new ProtectionDomain(null, null);
        String csstr = secManager.getCodeSource(protectionDomain);
        assertTrue("The result code source string is incorrect, expected '" + CS_STR_NULL_CODE_SOURCE +
                   "' and got '" + csstr + "'.", CS_STR_NULL_CODE_SOURCE.equals(csstr));

        protectionDomain = new ProtectionDomain(CODE_SOURCE2, null);
        csstr = secManager.getCodeSource(protectionDomain);
        assertTrue("The result code source string is incorrect, expected '" + CS_STR_NULL_URL +
                   "' and got '" + csstr + "'.", CS_STR_NULL_URL.equals(csstr));

        protectionDomain = getClass().getProtectionDomain();
        csstr = secManager.getCodeSource(protectionDomain);
        assertTrue("The result code source string is incorrect, expected '" + csLocation +
                   "' and got '" + csstr + "'.", csLocation.equals(csstr));
    }

    @Test
    public void permissionToStringTest() {
        NoRethrowSecurityManager secManager = new NoRethrowSecurityManager();
        String result = secManager.permissionToString(null, null, null);
        assertTrue("The result string is incorrect, should be: \n" + PERMISSION_STR1 +
                   "\n and got : " + result, PERMISSION_STR1.equals(result));

        configPermissionsString();
        result = secManager.permissionToString(CODE_SOURCE2, CLASS_LOADER, PERMISSIONS);
        assertTrue("The result string is incorrect, should be: \n" + PERMISSION_STR2 +
                   "\n and got :" + result, PERMISSION_STR2.equals(result));
    }

    @Test
    public void isOffendingClassTest() {
        NoRethrowSecurityManager secManager = new NoRethrowSecurityManager();
        ProtectionDomain domain = getClass().getProtectionDomain();
        boolean result;

        result = secManager.isOffendingClass(CLASS_ARRAY, 0, domain, PERMISSION1);
        assertFalse("The class " + CLASS_ARRAY[0] + " should not be offending class.", result);

        result = secManager.isOffendingClass(CLASS_ARRAY, 1, domain, PERMISSION1);
        assertFalse("The class " + CLASS_ARRAY[1] + " should not be offending class.", result);

        result = secManager.isOffendingClass(CLASS_ARRAY, 2, domain, PERMISSION1);
        assertFalse("The class " + CLASS_ARRAY[2] + " should not be offending class.", result);

        result = secManager.isOffendingClass(CLASS_ARRAY, 3, domain, PERMISSION1);
        assertTrue("The class " + CLASS_ARRAY[3] + " should be offending class.", result);

    }

    private void configPermissionsString() {
        Enumeration<Permission> elements = PERMISSIONS.elements();
        int cont = 1;
        while (elements.hasMoreElements()) {
            Permission p = elements.nextElement();
            PERMISSION_STR2 = PERMISSION_STR2.replaceFirst("<perm" + cont + ">", p.toString());
            cont++;
        }
    }
}
