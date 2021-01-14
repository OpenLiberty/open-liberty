/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.jndi;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.ws.cdi12.test.jndi.observer.ObserverBean;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.Produces;
import javax.enterprise.context.ApplicationScoped;

import javax.inject.Named;

@ApplicationScoped
public class BeanManagerP  {

    @Produces
    @Resource(lookup = "java:comp/BeanManager")
    private String manager;

}
