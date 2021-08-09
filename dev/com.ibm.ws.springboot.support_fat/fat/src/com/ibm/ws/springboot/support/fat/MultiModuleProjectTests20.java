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
package com.ibm.ws.springboot.support.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class MultiModuleProjectTests20 extends AbstractSpringTests {

    @Test
    public void testBeanInRootInitialized() throws Exception {
        server.findStringsInLogs("ROOT BEAN INVOKED");
    }

    @Test
    public void testBeanInModuleInitialized() throws Exception {
        server.findStringsInLogs("MODULE BEAN INVOKED");
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "jsp-2.3"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_WAR;
    }

}
