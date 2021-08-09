/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * This interface is internal API and is being maintained for strict API
 * compatibility only. It will be removed in a subsequent release.
 */
@Deprecated
public interface EJBModuleInitializationCollaborator {

    public void starting(EJBModuleMetaData collabData);

    public void started(EJBModuleMetaData collabData);

    public void stopping(EJBModuleMetaData collabData);

    public void stopped(EJBModuleMetaData collabData);

}
