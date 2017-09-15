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
package com.ibm.ws.javaee.dd.web.common;

import java.util.List;

/**
 *
 */
public interface AbsoluteOrdering {

    /**
     * @return true if &lt;others> is specified
     */
    boolean isSetOthers();

    /**
     * @return &lt;name>, appearing before &lt;others> if specified, as a read-only list
     */
    List<String> getNamesBeforeOthers();

    /**
     * @return &lt;name>, appearing after &lt;others> if specified, as a read-only list
     */
    List<String> getNamesAfterOthers();

}
