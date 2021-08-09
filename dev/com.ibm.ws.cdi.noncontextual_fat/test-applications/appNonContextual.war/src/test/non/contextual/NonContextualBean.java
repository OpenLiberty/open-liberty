/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.non.contextual;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

public class NonContextualBean {
    @Inject
    private Instance<Baz> baz;

    public void testNonContextualEjbInjectionPointGetBean() throws ServletException {
        Bar bar;
        try {
            bar = (Bar) new InitialContext().lookup("java:module/Bar");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
        if (bar == null) {
            throw new ServletException("bar is null for: java:module/Bar");
        }
        Bean<?> bean = bar.getFoo().getInjectionPoint().getBean();
        if (bean != null) {
            throw new ServletException("bean is NOT null for: " + bean);
        }
    }

    public void testContextualEjbInjectionPointGetBean() throws ServletException {
        Bean<?> bean = baz.get().getFoo().getInjectionPoint().getBean();
        if (bean == null) {
            throw new ServletException("bean is null for: " + baz.get().getFoo().getInjectionPoint());
        }
        if (!!!Baz.class.equals(bean.getBeanClass())) {
            throw new ServletException("wrong bean class type: " + bean.getBeanClass());
        }
    }

}
