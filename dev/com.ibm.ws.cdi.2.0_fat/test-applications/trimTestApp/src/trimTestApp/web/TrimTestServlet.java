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
package trimTestApp.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/trimTest")
public class TrimTestServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    BeanManager beanManager;

    @Test
    public void testTrim() throws Exception {
        Set<Bean<?>> beans = beanManager.getBeans(Thing.class);
        assertTrue("Wrong number of beans found: " + beans.size(), beans.size() == 1);
        for (Bean<?> bean : beans) {
            assertFalse("Did not expect to find " + ThingTwo.class.getName(), bean.getBeanClass().getName().equals(ThingTwo.class.getName()));
        }
    }
}
