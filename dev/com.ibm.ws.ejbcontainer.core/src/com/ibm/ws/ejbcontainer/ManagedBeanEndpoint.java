/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

import com.ibm.websphere.csi.J2EEName;

/**
 * Basic information about a managed bean.
 */
public interface ManagedBeanEndpoint {

    /**
     * @return the Java EE name for managed bean.
     */
    J2EEName getJ2EEName();

    /**
     * @return the managed bean name, unique within the module; may be null.
     */
    String getName();

    /**
     * @return the class name of this managed bean.
     */
    String getClassName();
}
