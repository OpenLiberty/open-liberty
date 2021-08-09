/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.io.Serializable;

/**
 * Contains the data necessary to perform one client-side injection for the
 * main class of a federated client module.
 */
public class ClientInjection
                implements Serializable
{
    private static final long serialVersionUID = 1032449919194046994L;

    /**
     * The reference name.
     */
    private final String ivRefName;

    /**
     * The type of the object being injected.
     */
    private final String ivInjectionTypeName;

    /**
     * The target class name.
     */
    private final String ivTargetClassName;

    /**
     * The target name in the class. This will either be the field name, or the
     * method name with the "set" prefix removed and lower-cased.
     */
    private final String ivTargetName;

    public ClientInjection(String refName,
                           String injectionTypeName,
                           String targetClassName,
                           String targetName)
    {
        ivRefName = refName;
        ivInjectionTypeName = injectionTypeName;
        ivTargetClassName = targetClassName;
        ivTargetName = targetName;
    }

    @Override
    public String toString()
    {
        return super.toString() +
               "[refName=" + ivRefName +
               ", type=" + ivInjectionTypeName +
               ", targetClass=" + ivTargetClassName +
               ", targetName=" + ivTargetName + ']';
    }

    public String getInjectionTypeName()
    {
        return ivInjectionTypeName;
    }

    public String getTargetClassName()
    {
        return ivTargetClassName;
    }

    public String getTargetName()
    {
        return ivTargetName;
    }

    public String getRefName()
    {
        return ivRefName;
    }
}
