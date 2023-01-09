/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import java.lang.reflect.Method;

import javax.annotation.Resource;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.sql.DataSource;

/**
 * This interceptor class is used when testing injection into a
 * field that is annotated with @Resource to ref to a datasource resource.
 * There are 2 different datasources. One to test the scenario where the
 * ibm-ejb-jar-bnd.xml binding file has a resource-ref binding that uses
 * authentication-alias. The other datasource has a resource-ref binding that
 * uses the custom-login-configuration properties. The bindings for each
 * of these reference type is inside of an <interceptor> stanza in the
 * ibm-ejb-jar-bnd.xml binding file.
 */
public class AnnotationDSInjectionInterceptor {
    @Resource(name = "AnnotationDS/jdbc/dsAuthAlias")
    DataSource dsAuthAlias;

    @Resource(name = "AnnotationDS/jdbc/dsCustomLogin")
    DataSource dsCustomLogin;

    @SuppressWarnings("unused")
    @AroundInvoke
    private Object aroundInvoke(InvocationContext inv) throws Exception {
        Method m = inv.getMethod();
        StatelessInterceptorInjectionBean ejb = (StatelessInterceptorInjectionBean) inv.getTarget();
        ejb.setAuthAliasDS(dsAuthAlias);
        ejb.setCustomLoginDS(dsCustomLogin);
        Object rv = inv.proceed();
        return rv;
    }
}
