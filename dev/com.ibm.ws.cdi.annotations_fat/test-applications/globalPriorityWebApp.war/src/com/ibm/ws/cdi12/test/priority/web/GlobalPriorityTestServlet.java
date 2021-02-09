/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.priority.web;

import static com.ibm.ws.cdi12.test.utils.Utils.id;
import static com.ibm.ws.cdi12.test.utils.Utils.reverse;
import static componenttest.matchers.Matchers.does;
import static componenttest.matchers.Matchers.hasSubsequence;
import static componenttest.matchers.Matchers.haveItem;
import static componenttest.matchers.Matchers.item;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi12.test.priority.lib.Bean;
import com.ibm.ws.cdi12.test.priority.lib.FromJar;
import com.ibm.ws.cdi12.test.priority.lib.JarBean;
import com.ibm.ws.cdi12.test.priority.lib.JarDecorator;
import com.ibm.ws.cdi12.test.priority.lib.JarInterceptor;
import com.ibm.ws.cdi12.test.priority.lib.LocalJarDecorator;
import com.ibm.ws.cdi12.test.priority.lib.LocalJarInterceptor;

import componenttest.app.FATServlet;

/**
 * These tests use a {@link Bean} interface, which returns lists of {@code Decorator}s and {@code Interceptor}s.
 * <p> {@code Decorator}s and {@code Interceptor}s append their own unique strings to these lists.
 * <p>
 * Returned lists are checked for the correct presence and order of these strings.
 * <p>
 * Note that this servlet extends {@link FATServlet}.
 */
@WebServlet("/testServlet")
public class GlobalPriorityTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    @FromJar
    Bean jarBean;

    @Inject
    @FromWar
    Bean warBean;

    @Inject
    @Any
    Bean highestPriorityAlternativeBean;

    @Test
    public void testDecoratedJarBean() {
        assertThat("WarDecorator should be globally enabled and should decorate JarBean.",
                   jarBean.getDecorators(),
                   hasItem(id(WarDecorator.class)));
    }

    @Test
    public void testDecoratedWarBean() {
        assertThat("JarDecorator should be globally enabled and should decorate WarBean.",
                   warBean.getDecorators(),
                   hasItem(id(JarDecorator.class)));
    }

    @Test
    public void testPrioritizedDecoratorOrder() {
        assertThat("WarDecorator should have highest priority and be called before JarDecorator.",
                   reverse(jarBean.getDecorators()),
                   item(id(WarDecorator.class)).isBefore(id(JarDecorator.class)));
    }

    @Test
    public void testLocalDecoratorEnabledForArchive() {
        assertThat("LocalJarDecorator, enabled in beans.xml, should decorate a bean in the same archive.",
                   jarBean.getDecorators(),
                   hasItem(id(LocalJarDecorator.class)));
    }

    @Test
    public void testGlobalDecoratorsAreBeforeLocalDecorator() {
        assertThat("Global decorators (enabled using @Priority) should be called before LocalJarDecorator (enabled in beans.xml).",
                   reverse(jarBean.getDecorators()),
                   hasSubsequence(id(WarDecorator.class), id(JarDecorator.class), id(LocalJarDecorator.class)));
    }

    @Test
    public void testLocalDecoratorsAreDisabledInOtherArchives() {
        assertThat("A decorator enabled in beans.xml should not decorate beans in other archives.",
                   warBean.getDecorators(),
                   does(not(haveItem(id(LocalJarDecorator.class)))));
    }

    @Test
    public void testAlternativePriority() {
        assertThat("JarBean should be globally enabled as an @Alternative, and should be chosen as it has the highest priority.",
                   highestPriorityAlternativeBean,
                   instanceOf(JarBean.class));
    }

    @Test
    public void testInterceptedFromLibJar() {
        assertThat("JarInterceptor should be globally enabled and should intercept calls to InterceptedWarBean.",
                   warBean.getInterceptors(),
                   hasItem(id(JarInterceptor.class)));
    }

    @Test
    public void testPrioritizedInterceptorOrder() {
        assertThat("WarInterceptor should have highest priority and be called before JarInterceptor.",
                   reverse(warBean.getInterceptors()),
                   item(id(WarInterceptor.class)).isBefore(id(JarInterceptor.class)));
    }

    @Test
    public void testLocalInterceptorEnabledForArchive() {
        assertThat("LocalJarInterceptor, enabled in beans.xml, should decorate a bean in the same archive.",
                   jarBean.getInterceptors(),
                   hasItem(id(LocalJarInterceptor.class)));
    }

    @Test
    public void testGlobalInterceptorsAreBeforeLocalInterceptor() {
        assertThat("Global interceptors (enabled using @Priority) should be called before LocalJarInterceptor (enabled in beans.xml).",
                   reverse(jarBean.getInterceptors()),
                   hasSubsequence(id(WarInterceptor.class), id(JarInterceptor.class), id(LocalJarInterceptor.class)));
    }

    @Test
    public void testLocalInterceptorsAreDisabledInOtherArchives() {
        assertThat("An interceptor enabled in beans.xml should not intercept beans in other archives.",
                   warBean.getInterceptors(),
                   does(not(haveItem(id(LocalJarInterceptor.class)))));
    }
}
