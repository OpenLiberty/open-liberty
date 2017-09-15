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
 * The interface for component level meta data.
 * 
 * @ibm-private-in-use
 */

public interface ComponentMetaData extends MetaData {

    /**
     * Gets the module meta data associated with this component.
     */

    public ModuleMetaData getModuleMetaData();

    /**
     * Gets the J2EE name of this component.
     */
    public J2EEName getJ2EEName();
}
