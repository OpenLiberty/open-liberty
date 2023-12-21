/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

import java.util.List;

/**
 * Represents &lt;context-service&gt;.
 */
public interface ContextService extends JNDIEnvironmentRef, Describable {
    /**
     * @return &lt;cleared&gt; elements as a read-only list
     */
    String[] getCleared();

    /**
     * @return &lt;propagated&gt; elements as a read-only list
     */
    String[] getPropagated();

    /**
     * @return &lt;unchanged&gt; elements as a read-only list
     */
    String[] getUnchanged();

    /**
     * @return &lt;qualifier&gt; elements as a read-only list
     */
    String[] getQualifiers();

    /**
     * @return &lt;property&gt; elements as a read-only list
     */
    List<Property> getProperties();
}
