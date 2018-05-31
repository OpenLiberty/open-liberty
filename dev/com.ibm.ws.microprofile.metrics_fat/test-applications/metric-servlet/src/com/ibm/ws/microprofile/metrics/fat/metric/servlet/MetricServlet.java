/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.fat.metric.servlet;

import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.metrics.classloader.utility.ClassLoaderUtils;

/**
 *
 */
@WebServlet("/metricServlet")
public class MetricServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static ClassLoader cl;

    @Inject
    MetricGenerator metricGenerator;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // TODO Auto-generated method stub
        ClassLoaderUtils classLoaderUtils = ClassLoaderUtils.getInstance();
        if (cl == null) {
            cl = this.getClass().getClassLoader();
            classLoaderUtils.setClassLoaderWeakRef(new WeakReference<ClassLoader>(cl));
        }
        classLoaderUtils = null;
        metricGenerator.getTestGaugeOne();
        metricGenerator.doSomething();
        metricGenerator.doSomethingWithTimed();
    }

}
