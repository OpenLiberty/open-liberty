/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package configuratorApp.web.bean;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import configuratorApp.web.ConfiguratorTestBase;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/beanConfiguratorTest")
public class BeanConfiguratorTest extends ConfiguratorTestBase {

    @Test
    @Mode(TestMode.FULL)
    public void sniffBeanConfigurator() {
        Bean<Brick> brickBean = getBean(Brick.class, BuildingMaterial.BuildingMaterialLiteral.INSTANCE);
        assertEquals(brickBean.getScope(), Dependent.class);
    }
}