/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.metadata;

import com.ibm.websphere.csi.J2EEName;

/**
 * The interface for module level meta data.
 * 
 * @ibm-private-in-use
 */

public interface ModuleMetaData extends MetaData {

    /**
     * Gets the J2EEName associated with the module.
     */
    public J2EEName getJ2EEName();

    /**
     * Gets the application meta data object associated with
     * this module.
     */

    public ApplicationMetaData getApplicationMetaData();

    /**
     * @deprecated This method is going away with LIDB859.
     */

    public ComponentMetaData[] getComponentMetaDatas();
}
