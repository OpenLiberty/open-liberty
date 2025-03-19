/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.vistest.war.servlet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.visibility.tests.vistest.framework.TargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.framework.VisTester;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/moduletest")
public class ModuleTestServlet extends FATServlet {

    public static final Logger LOGGER = Logger.getLogger(ModuleTestServlet.class.getName());

    @Resource(lookup = "java:module/ModuleName")
    String moduleName;

    @Inject
    BeanManager bm;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String location = req.getParameter("location");

        try {
            Annotation qualifier = VisTester.getQualiferForLocation(location);
            Set<Bean<?>> beans = bm.getBeans(TargetBean.class, qualifier);

            if (beans.size() != 1) {
                throw new RuntimeException("Found " + beans.size() + " beans for location " + location);
            }

            Bean<?> bean = beans.iterator().next();
            Class<?> clazz = bean.getBeanClass();

            LOGGER.info("Found class " + clazz.getName() + " for location " + location);
            LOGGER.info("My module name is " + moduleName);

            String module = VisTester.getModuleForClass(clazz)
                                     .orElse("NONE");

            resp.getOutputStream().print(module);
        } catch (Exception e) {
            resp.getOutputStream().print("ERROR: " + e);
        }
    }

}
