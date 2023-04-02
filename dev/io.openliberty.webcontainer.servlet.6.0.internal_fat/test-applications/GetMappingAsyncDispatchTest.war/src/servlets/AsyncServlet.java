/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package servlets;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test async dispatch to another servlet which will retrieve and verify the servlet mapping information.
 * 
 * Other dispatch tests in this area already covered in servlet.4.0_fat/WCGetMappingTest
 *
 */
@WebServlet(urlPatterns = "/AsyncServlet", name = "AsyncServlet", asyncSupported = true, loadOnStartup = 1)
public class AsyncServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = AsyncServlet.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    public static final String ORIGINAL_MAPPING_ATT = "AsyncServlet_Attribute_HttpServletMapping";
   


    public AsyncServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("Test AsyncServlet dispatches to another servlet.");
        
        AsyncContext asyncContext = request.startAsync();
        
        HttpServletMapping mapping = request.getHttpServletMapping();
        if (mapping != null)
            request.setAttribute(ORIGINAL_MAPPING_ATT, mapping);      //expecting this mapp to match the dispatched async target "jakarta.servlet.async.mapping"
        
        LOG.info("Test AsyncServlet , my mapping [" + mapping + "]"); //the async dispatched target should have this same mapping

        asyncContext.dispatch("/ServletMapping"); 

        LOG.info("Test AsyncServlet Done");
    }
}
