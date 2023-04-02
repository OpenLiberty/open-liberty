/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.apps.configurator.annotatedTypeConfigurator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi20.fat.apps.configurator.ConfiguratorTestBase;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/annotatedTypeConfiguratorTest")
public class AnnotatedTypeConfiguratorTest extends ConfiguratorTestBase {

    @Inject
    BeanManager bm;

    @Test
    @Mode(TestMode.FULL)
    public void sniffAnnotatedTypeConfigurator() {
        Bean<Pen> penBean = getBean(Pen.class);
        CreationalContext<Pen> cc = bm.createCreationalContext(penBean);

        @SuppressWarnings("unused")
        Pen pen = penBean.create(cc);

        assertNotNull(penBean);
        assertEquals(penBean.getScope(), RequestScoped.class);
    }
}