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
package configuratorApp.web.injectionPoint;

import static org.junit.Assert.assertEquals;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import configuratorApp.web.ConfiguratorTestBase;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/injectionPointConfiguratorTest")
public class InjectionPointConfiguratorTest extends ConfiguratorTestBase {

    @Test
    @Mode(TestMode.FULL)
    public void sniffInjectionPointConfigurator() {
        Bean<Sandwich> buttyBean = getBean(Sandwich.class);
        assertEquals(buttyBean.getInjectionPoints().size(), 1);
        InjectionPoint ip = buttyBean.getInjectionPoints().iterator().next();
        assertEquals(ip.isTransient(), true);
    }
}