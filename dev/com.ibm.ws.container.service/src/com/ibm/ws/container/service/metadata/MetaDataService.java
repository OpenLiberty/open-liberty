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
package com.ibm.ws.container.service.metadata;

import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MethodMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * Service for firing metadata events to listeners. This service should only by used by the container that creates a metadata object.
 */
public interface MetaDataService {
    /**
     * Call {@link ApplicationMetaDataListener#applicationMetaDataCreated} for all registered listeners. If any listener throws an exception, then
     * {@link #fireApplicationMetaDataDestroyed} will be called and MetaDataException will be thrown. This method must be called before any corresponding ModuleMetaData are
     * created.
     * 
     * @param metaData the metadata
     * @param container the application container
     * @throws MetaDataException if a listener threw an exception
     */
    void fireApplicationMetaDataCreated(ApplicationMetaData metaData, Container container) throws MetaDataException;

    /**
     * Call {@link ApplicationMetaDataListener#applicationMetaDestroyed} for all registered listeners. Any exceptions thrown by listeners will be ignored. This method must only be
     * called after all corresponding ModuleMetaData have been destroyed.
     * 
     * @param metaData the metadata
     */
    void fireApplicationMetaDataDestroyed(ApplicationMetaData metaData);

    /**
     * Call {@link ModuleMetaDataListener#moduleMetaDataCreated} for all registered listeners. If any listener throws an exception, then {@link #fireModuleMetaDataDestroyed} will
     * be called and MetaDataException will be thrown. This method must be called before any corresponding ComponentMetaData are created.
     * 
     * @param metaData the metadata
     * @param container the module container
     * @throws MetaDataException if a listener threw an exception
     */
    void fireModuleMetaDataCreated(ModuleMetaData metaData, Container container) throws MetaDataException;

    /**
     * Call {@link ModuleMetaDataListener#moduleMetaDestroyed} for all registered listeners. Any exceptions thrown by listeners will be ignored. This method must only be
     * called after all corresponding ComponentMetaData have been destroyed.
     * 
     * @param metaData the metadata
     */
    void fireModuleMetaDataDestroyed(ModuleMetaData metaData);

    /**
     * Call {@link ComponentMetaDataListener#componentMetaDataCreated} for all registered listeners. If any listener throws an exception, then
     * {@link #fireComponentMetaDataDestroyed} will be called and MetaDataException will be thrown. This method must be called before any corresponding MethodMetaData are created.
     * 
     * @param metaData the metadata
     * @throws MetaDataException if a listener threw an exception
     */
    void fireComponentMetaDataCreated(ComponentMetaData metaData) throws MetaDataException;

    /**
     * Call {@link ComponentMetaDataListener#componentMetaDestroyed} for all registered listeners. Any exceptions thrown by listeners will be ignored. This method must only be
     * called after all corresponding MethodMetaData have been destroyed.
     * 
     * @param metaData the metadata
     */
    void fireComponentMetaDataDestroyed(ComponentMetaData metaData);

    /**
     * Call {@link MethodMetaDataListener#methodMetaDataCreated} for all registered listeners. If any listener throws an exception, then {@link #fireMethodMetaDataDestroyed} will
     * be called and MetaDataException will be thrown.
     * 
     * @param metaData the metadata
     * @throws MetaDataException if a listener threw an exception
     */
    void fireMethodMetaDataCreated(MethodMetaData metaData) throws MetaDataException;

    /**
     * Call {@link MethodMetaDataListener#methodMetaDestroyed} for all registered listeners. Any exceptions thrown by listeners will be ignored.
     * 
     * @param metaData the metadata
     */
    void fireMethodMetaDataDestroyed(MethodMetaData metaData);
}
