/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.apps.servlets;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.beansxml.implicit.apps.beans.ApplicationScopedBean;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.ConversationScopedBean;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.DependentScopedBean;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.MyExtendedScopedBean;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.RequestScopedBean;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.RequestScopedButNoScan;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.SessionScopedBean;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.StereotypedBean;
import com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.ClassWithInjectButNotABean;
import com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.UnannotatedBeanInAllModeBeanArchive;
import com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.UnannotatedBeanInImplicitBeanArchive;
import com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.UnannotatedClassInAnnotatedModeBeanArchive;

import componenttest.app.FATServlet;

@WebServlet("/")
public class ImplicitBeanArchiveServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private BeanManager beanManager;

    @Test
    public void testUnannotatedBeanInAllModeBeanArchive() {
        //this one has a beans.xml with mode set to "all" so should be ok
        Set<Bean<?>> unannotatedBeanInAllModeBeanArchive = beanManager.getBeans(UnannotatedBeanInAllModeBeanArchive.class);
        assertEquals("Test Failed! - An unannotated bean in an \"all\" mode explicit archive was not found", 1, unannotatedBeanInAllModeBeanArchive.size());
    }

    @Test
    public void testApplicationScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
        Set<Bean<?>> applicationScopedBean = beanManager.getBeans(ApplicationScopedBean.class);
        assertEquals("Test Failed! - An application scoped bean in an implicit bean archive was not found", 1, applicationScopedBean.size());
    }

    @Test
    public void testConversationScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
        Set<Bean<?>> conversationScopedBean = beanManager.getBeans(ConversationScopedBean.class);
        assertEquals("Test Failed! - A conversation scoped bean in an implicit bean archive was not found", 1, conversationScopedBean.size());
    }

    @Test
    public void testNormalScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
        Set<Bean<?>> normalScopedBean = beanManager.getBeans(MyExtendedScopedBean.class);
        assertEquals("Test Failed! - A normal scoped bean in an implicit bean archive was not found", 1, normalScopedBean.size());
    }

    @Test
    public void testStereotypedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
        Set<Bean<?>> stereotypedBean = beanManager.getBeans(StereotypedBean.class);
        assertEquals("Test Failed! - A stereotyped bean in an implicit bean archive was not found", 1, stereotypedBean.size());
    }

    @Test
    public void testRequestScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
        Set<Bean<?>> requestScopedBean = beanManager.getBeans(RequestScopedBean.class);
        assertEquals("Test Failed! - A request scoped bean in an implicit bean archive was not found", 1, requestScopedBean.size());
    }

    @Test
    public void testSessionScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
        Set<Bean<?>> sessionScopedBean = beanManager.getBeans(SessionScopedBean.class);
        assertEquals("Test Failed! - A session scoped bean in an implicit bean archive was not found", 1, sessionScopedBean.size());
    }

    @Test
    public void testUnannotatedBeanInImplicitArchive() {
        //this one is NOT an implicit bean and has no beans.xml so it should be null
        Set<Bean<?>> unannotatedBeanInImplicitBeanArchive = beanManager.getBeans(UnannotatedBeanInImplicitBeanArchive.class);
        assertEquals("Test Failed! - An unannotated bean in an implicit bean archive was found", 0, unannotatedBeanInImplicitBeanArchive.size());
    }

    @Test
    public void testDependentScopedBeanInAnnotatedModeArchive() {
        //this one is an implicit bean in an "annotated" mode archive so should be ok
        Set<Bean<?>> dependentScopedBean = beanManager.getBeans(DependentScopedBean.class);
        assertEquals("Test Failed! - An implicit bean in an \"annotated\" mode explicit archive was not found", 1, dependentScopedBean.size());
    }

    @Test
    public void testUnannotatedBeanInAnnotatedModeArchive() {
        //this one is NOT an implicit bean in an "annotated" mode archive so should be null
        Set<Bean<?>> unannotatedBeanInAnnotatedModeBeanArchive = beanManager.getBeans(UnannotatedClassInAnnotatedModeBeanArchive.class);
        assertEquals("Test Failed! - An unannotated bean in an \"annotated\" mode explicit archive was found", 0, unannotatedBeanInAnnotatedModeBeanArchive.size());
    }

    @Test
    public void testRequestScopedBeanInNoneModeArchive() {
        //this one is an implicit bean in an "none" mode archive so should be null
        Set<Bean<?>> requestScopedButNoScan = beanManager.getBeans(RequestScopedButNoScan.class);
        assertEquals("Test Failed! - An implicit bean in an \"none\" mode explicit archive was found", 0, requestScopedButNoScan.size());
    }

    @Test
    public void testClassWithInjectButNotInABeanArchive() {
        //this one is not an implicit bean and has no beans.xml so it should be null
        Set<Bean<?>> classWithInjectButNotABean = beanManager.getBeans(ClassWithInjectButNotABean.class);
        assertEquals("Test Failed! - An unannotated class in an archive with no implicit beans was found", 0, classWithInjectButNotABean.size());
    }
}
