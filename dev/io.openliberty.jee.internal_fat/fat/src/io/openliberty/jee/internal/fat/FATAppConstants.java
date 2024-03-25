/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jee.internal.fat;

public interface FATAppConstants {

    String SIMPLE_EAR_SERVER_NAME = "simpleEar";
    String SIMPLE_WAR_SERVER_NAME = "simpleWar";

    String SIMPLE_EAR_NAME = "simpleEar.ear";
    boolean SIMPLE_EAR_ADD_RESOURCES = FATServerHelper.DO_ADD_RESOURCES;

    String SIMPLE_WAR_NAME = "simpleWar.war";
    String[] SIMPLE_WAR_PACKAGE_NAMES = new String[] { "io.openliberty.testservlet.internal.war.servlets" };
    boolean SIMPLE_WAR_ADD_RESOURCES = FATServerHelper.DO_ADD_RESOURCES;

    String SIMPLE_WAR_CONTEXT_ROOT = "simpleWar";

    String SIMPLE_WAR_SERVLET_NAME = "SimpleClassesServlet";
    // io.openliberty.testservlet.internal.war.servlets.SimpleClassesServlet

    String SIMPLE_JAR_NAME = "simpleJar.jar";
    String[] SIMPLE_JAR_PACKAGE_NAMES = new String[] { "io.openliberty.testservlet.internal.jar.servlets" };
    boolean SIMPLE_JAR_ADD_RESOURCES = FATServerHelper.DO_NOT_ADD_RESOURCES;

    String SIMPLE_JAR_SERVLET_NAME = "SimpleFragmentServlet";
    // io.openliberty.testservlet.internal.jar.servlets.SimpleFragmentServlet
}
