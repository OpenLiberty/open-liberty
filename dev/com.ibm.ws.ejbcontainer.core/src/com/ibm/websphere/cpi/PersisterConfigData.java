/*******************************************************************************
 * Copyright (c) 1998, 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpi;

import java.util.*;

/**
 * Persister specific config information which will be returned by
 * EJBConfigData. Can't think of anything more generic than a
 * Properties object for now.
 * 
 * @see com.ibm.websphere.csi.EJBConfigData
 */

public interface PersisterConfigData {

    /**
     * getProperties returns config properties for the Persister
     * 
     * @return Properties the properties for this persister.
     */
    public Properties getProperties();
}
