/*******************************************************************************
 * Copyright (c) 1998 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer;

public interface EJBPMICollaboratorFactory {
    /**
     * Create an instance of PmiBean
     */
    public EJBPMICollaborator createPmiBean(EJBComponentMetaData data, String containerName);

    /**
     * Return a previously created object
     */
    public EJBPMICollaborator getPmiBean(String uniqueJ2eeName, String containerName);

    public void removePmiModule(Object mod);

}
