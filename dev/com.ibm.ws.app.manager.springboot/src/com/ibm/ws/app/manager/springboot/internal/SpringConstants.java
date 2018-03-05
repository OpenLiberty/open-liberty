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
    public static final String SPRING_SHARED_LIB_CACHE_DIR = "lib.index.cache/";
    public static final String SPRING_LIB_INDEX_FILE = "META-INF/spring.lib.index";
    public static final String SPRING_THIN_APPS_DIR = WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "apps/spring.thin.apps/";
    public static final String SPRING_BOOT_CONFIG_NAMESPACE = "liberty.springboot.config";
    public static final String SPRING_BOOT_CONFIG_BUNDLE_PREFIX = "springBootVirtualHost@";
    public static final String XML_BND_NAME = "WEB-INF/ibm-web-bnd.xml";
    public static final String VIRTUAL_HOST_START = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                    "<web-bnd \n" +
                                                    "        xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                                                    "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                                    "        xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"\n"
                                                    +
                                                    "        version=\"1.0\">\n" +
                                                    "\n" +
                                                    "        <virtual-host name=\"";
    public static final String VIRTUAL_HOST_END = "\" />\n" +
                                                  "\n" +
                                                  "        <resource-ref name=\"SAMPLE\" binding-name=\"jdbc/SAMPLE\">\n" +
                                                  "                <authentication-alias name=\"USER_AUTH\" />\n" +
                                                  "        </resource-ref>\n" +
                                                  "</web-bnd>";
}
