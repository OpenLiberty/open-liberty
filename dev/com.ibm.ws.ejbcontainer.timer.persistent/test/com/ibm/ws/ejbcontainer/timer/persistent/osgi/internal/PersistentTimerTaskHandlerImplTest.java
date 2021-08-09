/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.osgi.internal;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;

public class PersistentTimerTaskHandlerImplTest {

    @Test
    public void testGetTaskName() {
        J2EEName j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean");
        PersistentTimerTaskHandlerImpl testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!PTimerEJB#Bean", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean_");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!PTimerEJB#Bean_", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean_");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!P_TimerEJB#Bean_", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P\\TimerEJB", "Bean%");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!P\\TimerEJB#Bean%", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P__TimerEJB", "Bean\\%");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!P__TimerEJB#Bean\\%", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P\\_%_TimerEJB", "Bean_%\\");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!P\\_%_TimerEJB#Bean_%\\", testHandler.getTaskName());
    }

    @Test
    public void testGetTaskNameModulePattern() {
        Assert.assertEquals("!EJBTimer_!PTimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("PTimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P_TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\%TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P%TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\\\TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P\\TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\_\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P__TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\_\\\\\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P_\\_TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\_\\%\\\\\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P_%\\_TimerEJB"));
    }

    @Test
    public void testGetTaskNameBeanPattern() {
        J2EEName j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean");
        Assert.assertEquals("!EJBTimer_!PTimerEJB#Bean", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean_");
        Assert.assertEquals("!EJBTimer_!PTimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean");
        Assert.assertEquals("!EJBTimer_!P\\_TimerEJB#Bean", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean_");
        Assert.assertEquals("!EJBTimer_!P\\_TimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P%TimerEJB", "Bean%");
        Assert.assertEquals("!EJBTimer_!P\\%TimerEJB#Bean\\%", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P\\%TimerEJB", "Bean\\");
        Assert.assertEquals("!EJBTimer_!P\\\\\\%TimerEJB#Bean\\\\", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P__TimerEJB", "Bean__");
        Assert.assertEquals("!EJBTimer_!P\\_\\_TimerEJB#Bean\\_\\_", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_\\_TimerEJB", "Bean_%_");
        Assert.assertEquals("!EJBTimer_!P\\_\\\\\\_TimerEJB#Bean\\_\\%\\_", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));
    }

    @Test
    public void testGetAutomaticTimerTaskNameBeanPattern() {
        J2EEName j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean");
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean_");
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean");
        Assert.assertEquals("!EJBTimerA!P\\_TimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean_");
        Assert.assertEquals("!EJBTimerA!P\\_TimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P%TimerEJB", "Bean%");
        Assert.assertEquals("!EJBTimerA!P\\%TimerEJB#Bean\\%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P\\%TimerEJB", "Bean\\");
        Assert.assertEquals("!EJBTimerA!P\\\\\\%TimerEJB#Bean\\\\", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P__TimerEJB", "Bean__");
        Assert.assertEquals("!EJBTimerA!P\\_\\_TimerEJB#Bean\\_\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_\\_TimerEJB", "Bean_%_");
        Assert.assertEquals("!EJBTimerA!P\\_\\\\\\_TimerEJB#Bean\\_\\%\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));
    }

    @Test
    public void getAutomaticTimerTaskNameModulePattern() {
        Assert.assertEquals("!EJBTimerA!PTimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("PTimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P_TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\%TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P%TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\\\TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P\\TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\_\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P__TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\_\\\\\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P_\\_TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\_\\%\\\\\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P_%\\_TimerEJB"));
    }

    @Test
    public void getAutomaticTimerPropertyName() {
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("PTimerEJB", "Bean"));
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("PTimerEJB", "Bean_"));
        Assert.assertEquals("!EJBTimerA!P_TimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("P_TimerEJB", "Bean"));
        Assert.assertEquals("!EJBTimerA!P%TimerEJB#Bean\\", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("P%TimerEJB", "Bean\\"));
        Assert.assertEquals("!EJBTimerA!P%_TimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("P%_TimerEJB", "Bean\\_"));
        Assert.assertEquals("!EJBTimerA!P__TimerEJB#Bean_%\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("P__TimerEJB", "Bean_%\\_"));
    }

    @Test
    public void getAutomaticTimerPropertyPattern() {
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("PTimerEJB", "Bean"));
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("PTimerEJB", "Bean_"));
        Assert.assertEquals("!EJBTimerA!P\\_TimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("P_TimerEJB", "Bean"));
        Assert.assertEquals("!EJBTimerA!P\\%TimerEJB#Bean\\\\", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("P%TimerEJB", "Bean\\"));
        Assert.assertEquals("!EJBTimerA!P\\%\\_TimerEJB#Bean\\\\\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("P%_TimerEJB", "Bean\\_"));
        Assert.assertEquals("!EJBTimerA!P\\_\\_TimerEJB#Bean\\_\\%\\\\\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("P__TimerEJB", "Bean_%\\_"));
    }
}
