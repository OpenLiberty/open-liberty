/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.test.fat;

import java.lang.reflect.Field;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ReflectionException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.jmx.PlatformMBeanService;

/**
 *
 */
public class BeanCounter extends TestBean {

    ServiceReference<PlatformMBeanService> mbeanServerRef;
    MBeanServer mBeanServer;
    PlatformMBeanService mbeanService;

    /**
     * @param name
     */
    public BeanCounter() {
        super(BeanCounter.class.getName());

    }

    public void activate(ComponentContext compContext) {
        mbeanService = null;
        try {
            mbeanService = compContext.locateService("jmxServer", mbeanServerRef);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deactivate(ComponentContext compContext) {}

    public void setJmxServer(ServiceReference<PlatformMBeanService> mbeanServerRef) {
        this.mbeanServerRef = mbeanServerRef;
    }

    public void unsetJmxServer(ServiceReference<PlatformMBeanService> mbeanServer) {}

    public int getNonDelayedBeanCount() {
        MBeanServer server = mbeanService.getMBeanServer();
        try {
            Class<?> clazz = server.getClass();
            Field f = clazz.getDeclaredField("last");
            f.setAccessible(true);
            server = (MBeanServer) f.get(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBeanServer = server;
        return mBeanServer.getMBeanCount();
    }

    @Override
    public Object getAttribute(String s) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return "beanCount".equals(s) ? getNonDelayedBeanCount() : super.getAttribute(s);
    }

    //    @Override
    //    public MBeanInfo getMBeanInfo() {
    //        try {
    //            Class<?> clazz = this.getClass();
    //            Method getter = clazz.getDeclaredMethod("getNonDelayedBeanCount", null);
    //            MBeanAttributeInfo aInfo = new MBeanAttributeInfo("beanCount", "beanCounter.beanCount", getter, null);
    //            MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[] { aInfo };
    //            MBeanInfo info = new MBeanInfo(toString(), "bean counter", attrs, null, null, null);
    //            return info;
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //            throw new RuntimeException(e);
    //        }
    //    }
}
