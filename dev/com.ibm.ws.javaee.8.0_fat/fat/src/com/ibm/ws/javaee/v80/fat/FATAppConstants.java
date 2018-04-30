/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.v80.fat;

public interface FATAppConstants {

    String JAVA8_FULL_SIMPLE_EAR_SERVER_NAME = "javaee8Full.simpleEar";
    String JAVA8_FULL_SIMPLE_WAR_SERVER_NAME = "javaee8Full.simpleWar";
    String JAVA8_WEB_SIMPLE_EAR_SERVER_NAME = "javaee8Web.simpleEar";
    String JAVA8_WEB_SIMPLE_WAR_SERVER_NAME = "javaee8Web.simpleWar";

    String SIMPLE_EAR_NAME = "simpleEar.ear";
    boolean SIMPLE_EAR_ADD_RESOURCES = FATServerHelper.DO_ADD_RESOURCES;

    String SIMPLE_WAR_NAME = "simpleWar.war";
    String[] SIMPLE_WAR_PACKAGE_NAMES = new String[] { "testservlet40.war.servlets" };
    boolean SIMPLE_WAR_ADD_RESOURCES = FATServerHelper.DO_ADD_RESOURCES;

    String SIMPLE_WAR_CONTEXT_ROOT = "simpleWar";

    String SIMPLE_WAR_SERVLET_NAME = "SimpleClassesServlet";
    // testservlet40.war.servlets.SimpleClassesServlet

    String SIMPLE_JAR_NAME = "simpleJar.jar";
    String[] SIMPLE_JAR_PACKAGE_NAMES = new String[] { "testservlet40.jar.servlets" };
    boolean SIMPLE_JAR_ADD_RESOURCES = FATServerHelper.DO_NOT_ADD_RESOURCES;

    String SIMPLE_JAR_SERVLET_NAME = "SimpleFragmentServlet";
    // testservlet40.jar.servlets.SimpleFragmentServlet
}
