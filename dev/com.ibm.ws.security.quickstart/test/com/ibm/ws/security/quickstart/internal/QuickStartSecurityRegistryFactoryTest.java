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
package com.ibm.ws.security.quickstart.internal;

import java.util.Dictionary;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import test.common.SharedOutputManager;

/**
 *
 */
public class QuickStartSecurityRegistryFactoryTest {
    private final QuickStartSecurity factory = new QuickStartSecurity();
    private final Mockery mock = new JUnit4Mockery();
    private final BundleContext bc = mock.mock(BundleContext.class);

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private QuickStartSecurityConfig config(final String user, final String pw, final String[] urs, final String[] ars) {
        return new QuickStartSecurityConfig() {

            //osgi object converter converts null to "" for known types.
            @Override
            public String userName() {
                return user == null ? "" : user;
            }

            @Override
            public SerializableProtectedString userPassword() {
                return pw == null ? null : new SerializableProtectedString(pw.toCharArray());
            }

            @Override
            public String[] UserRegistry() {
                return urs == null ? new String[] {} : urs;
            }

            @Override
            public String[] ManagementRole() {
                return ars == null ? new String[] {} : ars;
            }
        };
    }

    //no problem, no quickstart registry.
    @Test
    public void getUserRegistry_noUserOrPassword() throws Exception {
        factory.activate(bc, config(null, null, null, null));
    }

    @Test
    public void getUserRegistry_noUser() throws Exception {
        factory.activate(bc, config(null, "pwd", null, null));
        outputMgr.checkForLiteralStandardErr("[ERROR   ] CWWKS0900E: The <quickStartSecurity> element is missing required attributes: userName. Define the missing attributes.");
    }

    @Test
    public void getUserRegistry_noPassword() throws Exception {
        factory.activate(bc, config("user", null, null, null));
        outputMgr.checkForLiteralStandardErr("[ERROR   ] CWWKS0900E: The <quickStartSecurity> element is missing required attributes: userPassword. Define the missing attributes.");
    }

    @Test
    public void getUserRegistry_emptyUserAndPassword() throws Exception {
        factory.activate(bc, config("", "", null, null));
        outputMgr.checkForLiteralStandardErr("[ERROR   ] CWWKS0900E: The <quickStartSecurity> element is missing required attributes: userName. Define the missing attributes.");
        outputMgr.checkForLiteralStandardErr("[ERROR   ] CWWKS0900E: The <quickStartSecurity> element is missing required attributes: userPassword. Define the missing attributes.");
    }

    @Test
    public void getUserRegistry_emptyUser() throws Exception {
        factory.activate(bc, config("", "pwd", null, null));
        outputMgr.checkForLiteralStandardErr("[ERROR   ] CWWKS0900E: The <quickStartSecurity> element is missing required attributes: userName. Define the missing attributes.");
    }

    @Test
    public void getUserRegistry_emptyPassword() throws Exception {
        factory.activate(bc, config("user", "", null, null));
        outputMgr.checkForLiteralStandardErr("[ERROR   ] CWWKS0900E: The <quickStartSecurity> element is missing required attributes: userPassword. Define the missing attributes.");
    }

    @Test
    public void getUserRegistry_setUserAndPassword() throws Exception {
        mock.checking(new Expectations() {
            {
                one(bc).registerService(with(UserRegistry.class), with(any(QuickStartSecurityRegistry.class)), with(any(Dictionary.class)));
                will(returnValue(null));
                one(bc).registerService(with(ManagementRole.class), with(any(QuickStartSecurityAdministratorRole.class)), with(any(Dictionary.class)));
                will(returnValue(null));
            }
        });
        factory.activate(bc, config("user", "pwd", null, null));
    }
}
