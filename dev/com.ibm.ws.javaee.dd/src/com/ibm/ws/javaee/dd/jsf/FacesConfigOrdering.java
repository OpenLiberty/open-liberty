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
package com.ibm.ws.javaee.dd.jsf;

import java.util.List;

public interface FacesConfigOrdering {
    /**
     * @return true if &lt;after> is specified
     * @see #getAfterNames
     * @see #isSetAfterOthers
     */
    boolean isSetAfter();

    /**
     * @return &lt;name>, within &lt;after> if specified, as a read-only list
     * @see #isSetAfter
     */
    List<String> getAfterNames();

    /**
     * @return true if &lt;others>, within &lt;after> if specified, is specified
     * @see #isSetAfter
     */
    boolean isSetAfterOthers();

    /**
     * @return true if &lt;before> is specified
     * @see #getBeforeNames
     * @see #isSetBeforeOthers
     */
    boolean isSetBefore();

    /**
     * @return &lt;name>, within &lt;before> if specified, as a read-only list
     * @see #isSetBefore
     */
    List<String> getBeforeNames();

    /**
     * @return true if &lt;others>, within &lt;before> if specified, is specified
     * @see #isSetBefore
     */
    boolean isSetBeforeOthers();
}