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
package com.ibm.ws.security.jaas.common.internal;

import static org.junit.Assert.assertEquals;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.junit.Test;

/**
 *
 */
public class JAASLoginModuleConfigImplTest {

    @Test
    public void getFlag_OPTIONAL() throws Exception {
        assertEquals("Did not get expected OPTIONAL control flag",
                     LoginModuleControlFlag.OPTIONAL, JAASLoginModuleConfigImpl.setControlFlag("OPTIONAL"));
    }

    @Test
    public void getFlag_REQUIRED() throws Exception {
        assertEquals("Did not get expected REQUIRED control flag",
                     LoginModuleControlFlag.REQUIRED, JAASLoginModuleConfigImpl.setControlFlag("required"));
    }

    @Test
    public void getFlag_REQUISITE() throws Exception {
        assertEquals("Did not get expected REQUISITE control flag",
                     LoginModuleControlFlag.REQUISITE, JAASLoginModuleConfigImpl.setControlFlag("requisite"));
    }

    @Test
    public void getFlag_SUFFICIENT() throws Exception {
        assertEquals("Did not get expected SUFFICIENT control flag",
                     LoginModuleControlFlag.SUFFICIENT, JAASLoginModuleConfigImpl.setControlFlag("sufficient"));
    }

    @Test
    public void getFlag_default() throws Exception {
        assertEquals("Did not get expected REQUIRED control flag",
                     LoginModuleControlFlag.REQUIRED, JAASLoginModuleConfigImpl.setControlFlag("asdfi pweyrugo"));
        assertEquals("Did not get expected REQUIRED control flag",
                     LoginModuleControlFlag.REQUIRED, JAASLoginModuleConfigImpl.setControlFlag(null));
    }

}
