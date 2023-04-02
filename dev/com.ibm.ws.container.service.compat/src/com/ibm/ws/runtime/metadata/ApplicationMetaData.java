/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
package com.ibm.ws.runtime.metadata;

import com.ibm.websphere.csi.J2EEName;

/**
 * The interface for application level meta data.
 * 
 * @ibm-private-in-use
 */

public interface ApplicationMetaData extends MetaData {

    /**
     * ???
     */
    boolean createComponentMBeans();

    /**
     * Gets the J2EEName associated with this application.
     */

    J2EEName getJ2EEName();
}
