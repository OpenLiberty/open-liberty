/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

import test.common.SharedOutputManager;

public class WebConfigUtilsTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();

    private final ComponentMetaData cmd = context.mock(ComponentMetaData.class);
    private final WebComponentMetaData wcmd = context.mock(WebComponentMetaData.class);
    private final WebModuleMetaData wmmd = context.mock(WebModuleMetaData.class, "wmmd");
    private final WebModuleMetaData wmmd2 = context.mock(WebModuleMetaData.class, "wmmd2");
    private final ModuleMetaData mmd = context.mock(ModuleMetaData.class);
    private final SecurityMetadata smd = context.mock(SecurityMetadata.class, "smd");
    private final SecurityMetadata smd2 = context.mock(SecurityMetadata.class, "smd2");
    private final Object key = new Object();
    private final Object key2 = new Object();

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
        WebConfigUtils.resetMetaData();
    }

    /**
     * Tests getSecurityMetadata method
     */
    @Test
    public void getSecurityMetadataFromAccessor() {
        setComponentMetaDataToAccessor();
        withWebModuleMetaData();
        assertEquals("SecurityMetadata should be returned.", smd, WebConfigUtils.getSecurityMetadata());
        resetAccessor();
    }

    /**
     * Tests getWebModuleMetadata method
     */
    @Test
    public void getSecurityMetadataFromThreadLocal() {
        setComponentMetaDataToAccessor();
        withoutWebModuleMetaData();
        setWebModuleMetaDataToLocal();

        assertEquals("SecurityMetadata should be returned.", smd2, WebConfigUtils.getSecurityMetadata());
        resetLocal();
        resetAccessor();
    }

    /**
     * Tests getSecurityMetadata method
     */
    @Test
    public void getWebModuleMetaDataNull() {
        assertNull("WebModuleMetadata should be null.", WebConfigUtils.getWebModuleMetaData());
    }

    /**
     * Tests setSecurityMetaData and getSecurityMetaData method
     */
    @Test
    public void setgetWebModuleMetaDataValid() {
        WebConfigUtils.setWebModuleMetaData(key, wmmd2);
        assertEquals("WebModuleMetadata should be valid.", wmmd2, WebConfigUtils.getWebModuleMetaData());
        WebConfigUtils.removeWebModuleMetaData(key);
    }

    /**
     * Tests removeSecurityMetadata method
     */
    @Test
    public void removeWebModuleMetaDataValid() {
        WebConfigUtils.setWebModuleMetaData(key, wmmd2);
        WebConfigUtils.removeWebModuleMetaData(key2);
        assertEquals("WebModuleMetadata should be valid.", wmmd2, WebConfigUtils.getWebModuleMetaData());
        WebConfigUtils.removeWebModuleMetaData(key);
        assertNull("WebModuleMetadata should be removed.", WebConfigUtils.getWebModuleMetaData());
    }

    private void setComponentMetaDataToAccessor() {
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);
    }

    private void resetAccessor() {
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    private void setWebModuleMetaDataToLocal() {
        WebConfigUtils.setWebModuleMetaData(key, wmmd2);
        context.checking(new Expectations() {
            {
                one(wmmd2).getSecurityMetaData();
                will(returnValue(smd2));
            }
        });
    }

    private void resetLocal() {
        WebConfigUtils.removeWebModuleMetaData(key);
    }

    private WebConfigUtilsTest withWebModuleMetaData() {
        context.checking(new Expectations() {
            {
                one(cmd).getModuleMetaData();
                will(returnValue(wmmd));
                one(wmmd).getSecurityMetaData();
                will(returnValue(smd));
            }
        });
        return this;
    }

    private WebConfigUtilsTest withoutWebModuleMetaData() {
        context.checking(new Expectations() {
            {
                one(cmd).getModuleMetaData();
                will(returnValue(mmd));
            }
        });
        return this;
    }

}
