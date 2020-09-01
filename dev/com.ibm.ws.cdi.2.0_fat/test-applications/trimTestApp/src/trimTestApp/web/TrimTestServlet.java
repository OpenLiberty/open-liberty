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
package trimTestApp.web;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet(urlPatterns = "/trimTest")
public class TrimTestServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    BeanManager beanManager;

    @Test
    @Mode(TestMode.FULL)
    public void testBeans() {
        Set<Bean<?>> beans = beanManager.getBeans(Thing.class);
        assertTrue("Wrong number of beans found: " + beans.size(), beans.size() == 1);
        for (Bean<?> bean : beans) {
            assertTrue("Did not expect to find " + bean.getBeanClass().getName(), bean.getBeanClass().getName().equals(ThingOne.class.getName()));
        }
    }

    @Test
    @Mode(TestMode.FULL)
    public void testPATObservers() {
        assertTrue("ProcessAnnotatedType<ThingOne> not observed", PATObserver.observed.contains(ThingOne.class.getName()));
        assertTrue("ProcessAnnotatedType<ThingTwo> not observed", PATObserver.observed.contains(ThingTwo.class.getName()));
        assertTrue("ProcessAnnotatedType<Thing> not observed", PATObserver.observed.contains(Thing.class.getName()));
        assertTrue("Unexpected observer count: " + PATObserver.observed.size(), 3 == PATObserver.observed.size());
    }
}
