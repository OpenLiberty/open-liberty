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
package com.ibm.ws.javaee.dd.common;

import java.util.List;

/**
 * Represents the resourceBaseGroup type from the javaee XSD.
 */
public interface ResourceBaseGroup
                extends JNDIEnvironmentRef
{
    /**
     * @return &lt;mapped-name>, or null if unspecified
     */
    String getMappedName();

    /**
     * @return &lt;injection-target> as a read-only list
     */
    List<InjectionTarget> getInjectionTargets();
}
