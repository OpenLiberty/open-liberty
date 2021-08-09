/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.metadata;

/**
 * Base interface for all meta data. One feature of meta data is
 * an exensibility mechanism by which components can register a
 * location (<i>aka</i> "slot") in which to put their own specific
 * data.
 * 
 * @ibm-private-in-use
 */

public interface MetaData {

    /**
     * Gets the name associated with this meta data. For application, module
     * and component metadata, this should be the same as the corresponding
     * name in {@link #getJ2EEName}.
     */
    public String getName();

    /**
     * Sets the meta data associated with the given slot.
     * If the specified slot number is greater than highest slot number
     * currently allocated, sufficient additional slots will be allocated
     * 
     * @param slot integer slot number of desired location
     * @param metadata data to be stored
     */
    public void setMetaData(MetaDataSlot slot, Object metadata);

    /**
     * Gets the meta data associated with the given slot.
     * If the slot has been reserved, but no metadata allocated, null
     * will be returned.
     * 
     * @see com.ibm.ws.runtime.service.MetaDataService#reserveSlot(Class)
     * @param slot the desired location
     * @return null if no metadata allocated, otherwise the metadata associated
     *         with the specified slot
     */
    public Object getMetaData(MetaDataSlot slot);

    /**
     * Releases any resources that are not needed for runtime.
     * 
     * @see com.ibm.ws.runtime.service.MetaDataService#reserveSlot(Class)
     */
    public void release();
}
