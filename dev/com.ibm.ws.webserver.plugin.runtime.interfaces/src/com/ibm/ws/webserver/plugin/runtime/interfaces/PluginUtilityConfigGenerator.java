/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webserver.plugin.runtime.interfaces;

import java.io.File;

/**
 * The PluginUtilityConfigGenerator interface is meant to be implemented by MBEANS that will be leveraged
 * for auto generation of the plugin-cfg.xml file.
 */
public interface PluginUtilityConfigGenerator {

    /**
     * Each implementor needs to have a unique type returned when getPluginConfigType is called.
     */
    public enum Types {
        WEBCONTAINER,
        COLLECTIVE
    }

    /**
     * Generate the plugin-cfg.xml file.
     * 
     * @param name - The name of the server or collective that the plugin-cfg.xml file will be generated for.
     * @param writeDirectory - The directory to write the plugin-cfg.xml file to
     */
    public void generatePluginConfig(String name, File writeDirectory);

    /**
     * Return a unique identifier. In this case one of the values defined in the Types enum.
     * 
     * @return
     */
    public Types getPluginConfigType();
}
