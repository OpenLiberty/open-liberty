/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.security.Principal;

import org.junit.Test;

import com.ibm.ws.security.authorization.util.classes.DenyAllOnClass;
import com.ibm.ws.security.authorization.util.classes.NoAnnotationsOnClass;
import com.ibm.ws.security.authorization.util.classes.PermitAllOnClass;
import com.ibm.ws.security.authorization.util.classes.PrincipalImpl;
import com.ibm.ws.security.authorization.util.classes.RolesAllowedOnClass;

public class RoleMethodAuthUtilTest {

    private static final Class<?> DENYALL_ON_CLASS = DenyAllOnClass.class;
    private static final Class<?> PERMITALL_ON_CLASS = PermitAllOnClass.class;
    private static final Class<?> ROLESALLOWED_ON_CLASS = RolesAllowedOnClass.class;
    private static final Class<?> NO_ANNOTATIONS_ON_CLASS = NoAnnotationsOnClass.class;

    //Unauthenticated
    @Test(expected = UnauthenticatedException.class)
    public void checkAuthentication_null_principal() throws Exception {
        RoleMethodAuthUtil.checkAuthentication(null);
    }

    @Test(expected = UnauthenticatedException.class)
    public void checkAuthentication_UNAUTHENTICATED_principal() throws Exception {
        RoleMethodAuthUtil.checkAuthentication(new PrincipalImpl("UNAUTHENTICATED"));
    }

    @Test
    public void unauthenticated_PermitAllOnClass_noAnnotationOnMethod() throws Exception {
        // @PermitAll on class / no annotations on method
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(PERMITALL_ON_CLASS, "unannotated"),
                                                          null,
                                                          s -> {
                                                              return true;
                                                          }));
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(PERMITALL_ON_CLASS, "unannotated"),
                                                          principal("UNAUTHENTICATED"),
                                                          s -> {
                                                              return true;
                                                          }));
    }

    @Test(expected = UnauthenticatedException.class)
    public void nullPrincipal_RolesAllowedOnClass_noAnnotationOnMethod() throws Exception {
        // @PermitAll on class / no annotations on method
        RoleMethodAuthUtil.parseMethodSecurity(method(ROLESALLOWED_ON_CLASS, "unannotated"),
                                               null,
                                               s -> {
                                                   return true;
                                               });
    }

    @Test(expected = UnauthenticatedException.class)
    public void unauthenticated_RolesAllowedOnClass_noAnnotationOnMethod() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(ROLESALLOWED_ON_CLASS, "unannotated"),
                                                           principal("UNAUTHENTICATED"),
                                                           s -> {
                                                               return true;
                                                           }));
    }

    // @DenyAll on Class:
    @Test
    public void denyAllOnClass_noAnnotationOnMethod() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(DENYALL_ON_CLASS, "unannotated"),
                                                           principal("foo"),
                                                           s -> {
                                                               return true;
                                                           }));
    }

    @Test
    public void denyAllOnClass_denyAllOnMethod() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(DENYALL_ON_CLASS, "denyAll"),
                                                           principal("foo"),
                                                           s -> {
                                                               return true;
                                                           }));
    }

    @Test
    public void denyAllOnClass_permitAllOnMethod() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(DENYALL_ON_CLASS, "permitAll"),
                                                          principal("foo"),
                                                          s -> {
                                                              return true;
                                                          }));
    }

    @Test
    public void denyAllOnClass_rolesAllowedOnMethod_inRole() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(DENYALL_ON_CLASS, "rolesAllowed"),
                                                          principal("foo"),
                                                          s -> {
                                                              return "role1".equals(s);
                                                          }));
    }

    @Test
    public void denyAllOnClass_rolesAllowedOnMethod_notInRole() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(DENYALL_ON_CLASS, "rolesAllowed"),
                                                           principal("foo"),
                                                           s -> {
                                                               return "role0".equals(s);
                                                           }));
    }

    //@PermitAll on class:
    @Test
    public void permitAllOnClass_noAnnotationOnMethod() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(PERMITALL_ON_CLASS, "unannotated"),
                                                          principal("foo"),
                                                          s -> {
                                                              return true;
                                                          }));
    }

    @Test
    public void permitAllOnClass_denyAllOnMethod() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(PERMITALL_ON_CLASS, "denyAll"),
                                                           principal("foo"),
                                                           s -> {
                                                               return true;
                                                           }));
    }

    @Test
    public void permitAllOnClass_permitAllOnMethod() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(PERMITALL_ON_CLASS, "permitAll"),
                                                          principal("foo"),
                                                          s -> {
                                                              return true;
                                                          }));
    }

    @Test
    public void permitAllOnClass_rolesAllowedOnMethod_inRole() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(PERMITALL_ON_CLASS, "rolesAllowed"),
                                                          principal("foo"),
                                                          s -> {
                                                              return "role1".equals(s);
                                                          }));
    }

    @Test
    public void permitAllOnClass_rolesAllowedOnMethod_notInRole() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(PERMITALL_ON_CLASS, "rolesAllowed"),
                                                           principal("foo"),
                                                           s -> {
                                                               return "role0".equals(s);
                                                           }));
    }

    //@RolesAllowed on class:
    @Test
    public void rolesAllowedOnClass_in_noAnnotationOnMethod() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(ROLESALLOWED_ON_CLASS, "unannotated"),
                                                          principal("foo"),
                                                          s -> {
                                                              return s.equals("role3");
                                                          }));
    }

    @Test
    public void rolesAllowedOnClass_notIn_noAnnotationOnMethod() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(ROLESALLOWED_ON_CLASS, "unannotated"),
                                                           principal("foo"),
                                                           s -> {
                                                               return s.equals("role0");
                                                           }));
    }

    @Test
    public void rolesAllowedOnClass_denyAllOnMethod() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(ROLESALLOWED_ON_CLASS, "denyAll"),
                                                           principal("foo"),
                                                           s -> {
                                                               return true;
                                                           }));
    }

    @Test
    public void rolesAllowedOnClass_permitAllOnMethod() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(ROLESALLOWED_ON_CLASS, "permitAll"),
                                                          principal("foo"),
                                                          s -> {
                                                              return false;
                                                          }));
    }

    @Test
    public void rolesAllowedOnClass_rolesAllowedOnMethod_inRole() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(ROLESALLOWED_ON_CLASS, "rolesAllowed"),
                                                          principal("foo"),
                                                          s -> {
                                                              return "role2".equals(s);
                                                          }));
    }

    @Test
    public void rolesAllowedOnClass_rolesAllowedOnMethod_notInRole() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(ROLESALLOWED_ON_CLASS, "rolesAllowed"),
                                                           principal("foo"),
                                                           s -> {
                                                               return "role3".equals(s);
                                                           }));
    }

    //No annotations on class:
    @Test
    public void noAnnotationsOnClass_noAnnotationOnMethod() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(NO_ANNOTATIONS_ON_CLASS, "unannotated"),
                                                          principal("foo"),
                                                          s -> {
                                                              return true;
                                                          }));
    }

    @Test
    public void noAnnotationsOnClass_denyAllOnMethod() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(NO_ANNOTATIONS_ON_CLASS, "denyAll"),
                                                           principal("foo"),
                                                           s -> {
                                                               return true;
                                                           }));
    }

    @Test
    public void noAnnotationsOnClass_permitAllOnMethod() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(NO_ANNOTATIONS_ON_CLASS, "permitAll"),
                                                          principal("foo"),
                                                          s -> {
                                                              return true;
                                                          }));
    }

    @Test
    public void noAnnotationsOnClass_rolesAllowedOnMethod_inRole() throws Exception {
        assertTrue(RoleMethodAuthUtil.parseMethodSecurity(method(NO_ANNOTATIONS_ON_CLASS, "rolesAllowed"),
                                                          principal("foo"),
                                                          s -> {
                                                              return "role1".equals(s);
                                                          }));
    }

    @Test
    public void noAnnotationsOnClass_rolesAllowedOnMethod_notInRole() throws Exception {
        assertFalse(RoleMethodAuthUtil.parseMethodSecurity(method(NO_ANNOTATIONS_ON_CLASS, "rolesAllowed"),
                                                           principal("foo"),
                                                           s -> {
                                                               return "role0".equals(s);
                                                           }));
    }

    private Method method(Class<?> clazz, String methodName) throws Exception {
        return clazz.getMethod(methodName);
    }

    private Principal principal(String name) {
        return new PrincipalImpl(name);
    }
}