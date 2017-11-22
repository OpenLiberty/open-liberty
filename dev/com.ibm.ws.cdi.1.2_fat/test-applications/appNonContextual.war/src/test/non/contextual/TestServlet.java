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

import java.io.IOException;

import javax.enterprise.inject.spi.Unmanaged;
import javax.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/test")
public class TestServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        Unmanaged<NonContextualBean> unmanaged = new Unmanaged<NonContextualBean>(NonContextualBean.class);
        UnmanagedInstance<NonContextualBean> instance = unmanaged.newInstance();
        NonContextualBean nonContextualBean = instance.produce().inject().postConstruct().get();

        try {
            nonContextualBean.testNonContextualEjbInjectionPointGetBean();
            nonContextualBean.testContextualEjbInjectionPointGetBean();

            resp.getWriter().print("PASSED");
        } finally {
            instance.preDestroy().dispose();
        }
    }

}
