/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

/**
 * This interface will be implemented by components needing to be
 * notified upon the creation of InjectionMetaData. The InjectionMetaData
 * is available only after 'populateJavaNameSpace' has been called
 * for a given module or component.
 *
 */
public interface InjectionMetaDataListener {

    /**
     * This method will be called when InjectionMetaData has been created
     * for a module or component.
     */
    public void injectionMetaDataCreated(InjectionMetaData injectionMetaData) throws InjectionException;

}
