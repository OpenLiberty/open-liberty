/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.archaius.impl.fat.tests;

import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.Test;

import componenttest.app.FATServlet;

//defaultsGetConfigPathSharedLib
@WebServlet("/")
public class SharedLibUserTestServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = -3242708758210608744L;

    /**
     * Tests that a config source can be loaded from within a
     * shared lib
     */
    @Test
    public void defaultsGetConfigPathSharedLib() throws Exception {
        String msg = PingableSharedLibClass.ping();
        Config c = ConfigProvider.getConfig();
        String v = c.getValue("defaultSources.sharedLib.config.properties", String.class);
        if (!("sharedLibPropertiesDefaultValue".equals(v) && msg.contains("loadable"))) {
            fail("FAILED" + v);
        }
    }
}
