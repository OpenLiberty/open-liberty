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
package configuratorApp.web.tests.extensions.configurators.bean;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import configuratorApp.web.ConfiguratorTestBase;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/beanConfiguratorTest")
public class BeanConfiguratorTest extends ConfiguratorTestBase {

    @Test
    public void sniffBeanConfigurator() {
        Bean<Brick> brickBean = getBean(Brick.class, BuildingMaterial.BuildingMaterialLiteral.INSTANCE);
        assertEquals(brickBean.getScope(), Dependent.class);
    }
}