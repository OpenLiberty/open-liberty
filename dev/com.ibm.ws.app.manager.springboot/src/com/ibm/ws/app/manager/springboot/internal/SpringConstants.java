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
package com.ibm.ws.app.manager.springboot.internal;

import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 *
 */
public final class SpringConstants {
    public static final String SPRING_APP_TYPE = "spr";
    public static final String SPRING_BOOT_SUPPORT_CAPABILITY = "spring.boot.support";
    public static final String SPRING_BOOT_SUPPORT_CAPABILITY_JARS = "jars";
    private static final String SPRING_LIB_CACHE_DIR = "lib.index.cache/";
    public static final String SPRING_SHARED_LIB_CACHE_DIR = WsLocationConstants.SYMBOL_SHARED_RESC_DIR + SPRING_LIB_CACHE_DIR;
    private static final String SPRING_WORKAREA = WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + "spring/";
    public static final String SPRING_WORKAREA_LIB_CACHE_DIR = SPRING_WORKAREA + SPRING_LIB_CACHE_DIR;
    public static final String SPRING_THIN_APPS_DIR = SPRING_WORKAREA + "spring.thin.apps/";
    public static final String SPRING_BOOT_CONFIG_NAMESPACE = "liberty.springboot.config";
    public static final String SPRING_BOOT_CONFIG_BUNDLE_PREFIX = "springBootVirtualHost@";
    public static final String XML_BND_NAME = "WEB-INF/ibm-web-bnd.xml";
    public static final String XML_VIRTUAL_HOST_START = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                        "<web-bnd \n" +
                                                        "        xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                                                        "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                                        "        xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"\n"
                                                        +
                                                        "        version=\"1.0\">\n" +
                                                        "\n" +
                                                        "        <virtual-host name=\"";
    public static final String XML_VIRTUAL_HOST_END = "\" />\n" +
                                                      "</web-bnd>";

    public static final String XMI_BND_NAME = "WEB-INF/ibm-web-bnd.xmi";
    public static final String XMI_VIRTUAL_HOST_START = "<webappbnd:WebAppBinding\n" +
                                                        "xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\"\n" +
                                                        "xmlns:webappbnd=\"webappbnd.xmi\"\n" +
                                                        "virtualHostName=\"";
    public static final String XMI_VIRTUAL_HOST_END = "\">\n" +
                                                      "</webappbnd:WebAppBinding>";
}
