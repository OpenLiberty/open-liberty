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
package com.ibm.ws.cdi20.fat.apps.configurator.beanAttributes;

import static org.junit.Assert.assertTrue;

import javax.enterprise.inject.spi.Bean;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi20.fat.apps.configurator.ConfiguratorTestBase;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/beanAttributesConfiguratorTest")
public class BeanAttributesConfiguratorTest extends ConfiguratorTestBase {

    @Test
    @Mode(TestMode.FULL)
    public void sniffBeanAttributesConfigurator() {
        Bean<Square> bean = getBean(Square.class, Quadrilateral.QuadrilateralLiteral.INSTANCE);

        assertTrue(bean.getQualifiers().contains(Quadrilateral.QuadrilateralLiteral.INSTANCE));
        assertTrue(bean.getTypes().contains(Shape.class));
    }
}
