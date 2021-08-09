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
 * Represents the jndiEnvironmentRefsGroup type from the javaee XSD.
 */
public interface JNDIEnvironmentRefsGroup
                extends JNDIEnvironmentRefs
{
    /**
     * @return &lt;post-construct> as a read-only list
     */
    List<LifecycleCallback> getPostConstruct();

    /**
     * @return &lt;pre-destroy> as a read-only list
     */
    List<LifecycleCallback> getPreDestroy();
}
