/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package testhandlestypesclasses.examples;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

@HandlesTypes({MyInterface.class, AbstractClass.class})
public class TestServletContainerInitializer implements ServletContainerInitializer {

  public static final String ON_STARTUP_SET_KEY = "onStartupSet";

  @Override
  public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
    ctx.setAttribute(ON_STARTUP_SET_KEY, String.valueOf(c));
    System.out.println("Set: " + String.valueOf(c));
  }
}
