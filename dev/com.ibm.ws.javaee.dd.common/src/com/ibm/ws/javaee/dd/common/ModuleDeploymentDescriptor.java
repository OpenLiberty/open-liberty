/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

/**
 * Represents common elements from module deployment descriptors.
 */
public interface ModuleDeploymentDescriptor
                extends DescriptionGroup
{
    /**
     * @return &lt;module-name>, or null if unspecified
     */
    String getModuleName();
}
