/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ConfigDropinRootTests30 extends AbstractSpringTests {
    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.DROPINS_ROOT;
    }

    //

    @Test
    public void testDropinsRoot() throws Exception {
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }
}
