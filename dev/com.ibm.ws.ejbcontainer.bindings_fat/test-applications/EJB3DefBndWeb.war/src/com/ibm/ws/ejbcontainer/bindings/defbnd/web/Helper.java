/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.bindings.defbnd.web;

import javax.naming.NamingException;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

public class Helper {
    public static final String APPLICATION = "EJB3DefBndTestApp";

    // Names of three part component... for lookup.
    // binding file has my/test/component for a component-id
    private static final String ComponentPartA = "my";
    private static final String ComponentPartB = "test";
    private static final String ComponentPartC = "component";

    public static <T> T lookupCompLocal(Class<T> interfaceClass) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultBindingEJBLocalInterface(interfaceClass.getName(), ComponentPartA, ComponentPartB, ComponentPartC));
    }

    public static <T> T lookupDefaultLocal(Class<T> interfaceClass, String beanClassSimpleName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultBindingEJBLocalInterface(interfaceClass.getName(), APPLICATION, "EJB3DefBndBean.jar", beanClassSimpleName));
    }

    public static <T> T lookupShortLocal(Class<T> interfaceClass) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupLocalBinding(interfaceClass.getName()));
    }

    public static <T> T lookupCompRemote(Class<T> interfaceClass) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(interfaceClass.getName(), ComponentPartA, ComponentPartB, ComponentPartC));
    }

    public static <T> T lookupDefaultRemote(Class<T> interfaceClass, String beanClassSimpleName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(interfaceClass.getName(), APPLICATION, "EJB3DefBndBean.jar", beanClassSimpleName));
    }

    public static <T> T lookupDefaultJavaColonRemote(Class<T> interfaceClass, String beanClassSimpleName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultBindingsEJBRemoteInterface(interfaceClass.getName(), APPLICATION, "EJB3DefBndBean", beanClassSimpleName));
    }

    public static <T> T lookupDefaultJavaColonLocal(Class<T> interfaceClass, String beanClassSimpleName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultBindingEJBJavaGlobal(interfaceClass.getName(), APPLICATION, "EJB3DefBndBean", beanClassSimpleName));
    }

    public static <T> T lookupShortRemote(Class<T> interfaceClass) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupRemoteShortBinding(interfaceClass.getName()));
    }
}
