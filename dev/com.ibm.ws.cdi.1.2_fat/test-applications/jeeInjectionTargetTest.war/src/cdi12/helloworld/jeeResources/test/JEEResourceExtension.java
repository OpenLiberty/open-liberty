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

package cdi12.helloworld.jeeResources.test;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.servlet.Servlet;
import javax.servlet.jsp.tagext.JspTag;

import cdi12.helloworld.jeeResources.ejb.ManagedBeanInterface;
import cdi12.helloworld.jeeResources.ejb.SessionBeanInterface;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyAnotherEJBInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBJARXMLDefinedInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyManagedBeanEJBInterceptor;

public class JEEResourceExtension implements Extension {

    public List<String> logger = new ArrayList<String>();

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event) {
        logger.add("BeforeBeanDiscovery!");
    }

    public void afterTypeDiscovery(@Observes AfterTypeDiscovery event) {
        logger.add("AfterTypeDiscovery!");
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        logger.add("AfterBeanDiscovery!");
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        logger.add("AfterDeploymentValidation!");
    }

    public void beforeShutdown(@Observes BeforeShutdown event) {
        logger.add("BeforeShutdown!");
    }

    public void processInjectionTarget(@Observes ProcessInjectionTarget<?> adv)
    {
        Class<?> clazz = adv.getAnnotatedType().getJavaClass();
        if (Servlet.class.isAssignableFrom(clazz)) {
            logger.add("Servlet! Injection Target Processed: " + adv.getAnnotatedType());
        }
        else if (HelloWorldExtensionBean2.class.isAssignableFrom(clazz)) {
            logger.add("CDI Bean! Injection Target Processed: " + adv.getAnnotatedType());
        }
        else if (SessionBeanInterface.class.isAssignableFrom(clazz)) {
            logger.add("Session Bean! Injection Target Processed: " + adv.getAnnotatedType());
        }
        else if (EventListener.class.isAssignableFrom(clazz)) {
            logger.add("Servlet Event Listener! Injection Target Processed: " + adv.getAnnotatedType());
        }
        else if (JspTag.class.isAssignableFrom(clazz)) {
            logger.add("JSP Tag Handler! Injection Target Processed: " + adv.getAnnotatedType());
        }
        else if (ManagedBeanInterface.class.isAssignableFrom(clazz)) {
            logger.add("Managed Bean! Injection Target Processed: " + adv.getAnnotatedType());
        }
        else if (MyMessageDrivenBean.class.isAssignableFrom(clazz)) {
            logger.add("Message Driven Bean! Injection Target Processed: " + adv.getAnnotatedType());
        }
        else if (MyServerEndpoint.class.isAssignableFrom(clazz)) {
            logger.add("Websocket Server Endpoint! Injection Target Processed: " + adv.getAnnotatedType());
        } else if (MyEJBInterceptor.class.isAssignableFrom(clazz)) {
            logger.add("NonCDIInterceptor1! Injection Target Processed: " + adv.getAnnotatedType());
        } else if (MyAnotherEJBInterceptor.class.isAssignableFrom(clazz)) {
            logger.add("NonCDIInterceptor2! Injection Target Processed: " + adv.getAnnotatedType());
        } else if (MyManagedBeanEJBInterceptor.class.isAssignableFrom(clazz)) {
            logger.add("NonCDIInterceptor3! Injection Target Processed: " + adv.getAnnotatedType());
        } else if (MyEJBJARXMLDefinedInterceptor.class.isAssignableFrom(clazz)) {
            logger.add("NON CDI Interceptor defined In ejb-jar.xml! Injection Target Processed:" + adv.getAnnotatedType());
        }
    }
}
