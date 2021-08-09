/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.factory;

import java.io.Serializable;

public class EnvEntryInfo
                implements Serializable
{
    private static final long serialVersionUID = 833469063476639075L;

    /**
     * The env-entry-name.
     *
     * @since 8.0.0.1
     */
    final String ivName; // d701200

    /**
     * The declaring application display name.
     *
     * @since 8.0.0.1
     */
    final String ivApplicationName; // d701200

    /**
     * The declaring module display name.
     *
     * @since 8.0.0.1
     */
    final String ivModuleName; // d701200

    /**
     * The declaring component display name.
     *
     * @since 8.0.0.1
     */
    final String ivComponentName;

    /**
     * The class name.
     */
    final String ivClassName;

    /**
     * The enum value name, or {@code null} if this object represents a Class
     * instead of an Enum.
     */
    final String ivValueName;

    public EnvEntryInfo(String name,
                        String appName,
                        String moduleName,
                        String compName,
                        String className,
                        String valueName)
    {
        ivName = name;
        ivApplicationName = appName;
        ivModuleName = moduleName;
        ivComponentName = compName;
        ivClassName = className;
        ivValueName = valueName;
    }

    @Override
    public String toString()
    {
        return super.toString() + "[name=" + ivName +
               ", app=" + ivApplicationName +
               ", mod=" + ivModuleName +
               ", comp=" + ivComponentName +
               ", class=" + ivClassName +
               ", value=" + ivValueName + ']';
    }
}
