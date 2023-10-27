/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import com.ibm.ws.cdi.CDIException;

/**
 * In multiple separate places CDI needs to set some context on the thread, perform an operation like fire observer methods, and unset the context afterwards.
 * Each case has subtly different requirements, so this class serves as a central hub where you can list all the context that needs to be set in one object,
 * then use that object to both set and unset the context.
 *
 * <p>
 *
 * CDIRuntime provides a factory method for creating objects that implement ContextBeginnerEnder
 *
 * <p>
 *
 * You should not use a second ContextBeginnerEnder before closing the first one, nor should you modify any of the context handled by this object before you
 * have finished with it.
 *
 * <p>
 *
 * Example of use:
 *
 * <pre>
 * try (ContextBeginnerEnder contextBeginnerEnder = cdiRuntime.createContextBeginnerEnder().extractComponentMetaData(bda.getArchive()).extractTCCL(application).beginContext()) {
 *     eventManager.fireStartupEvent(module);
 * }
 * </pre>
 */
public interface ContextBeginnerEnder extends AutoCloseable {

    /**
     * Extracts the TCCL stored in the application. When beginContext is called this will be set as the TCCL
     *
     * @param application
     * @return this
     */
    public ContextBeginnerEnder extractTCCL(Application application);

    /**
     * Creates an arbitary componentMetaData using data extracted from the application. Only use this if you
     * cannot begin and end a context for individual modules
     *
     * The metadata will be set as the component metadata when beginContext is called
     *
     * @param application
     * @return this
     */
    public ContextBeginnerEnder extractComponentMetaData(Application application) throws CDIException;

    /**
     * Creates an arbitary componentMetaData using data extracted from a module. If the module contains application metadata search through the application for anything that
     * contains module meta data
     *
     * The metadata will be set as the component metadata when beginContext is called
     *
     * @param application
     * @param archive
     * @return this
     */
    ContextBeginnerEnder extractComponentMetaData(CDIArchive archive) throws CDIException;

    /**
     * Sets the TCCL if one is ready, and sets the componentMetaData if one is ready.
     *
     * Throws an IllegalStateException if called twice
     *
     * @param archive
     * @return this
     */
    public ContextBeginnerEnder beginContext();

    /**
     * Unsets the componentMetaData if one was set, and sets the TCCL back to whatever it was before if it changed.
     * no-op if called twice
     *
     * @throws IllegalStateException if beginContext was not called
     *
     * @return this
     */
    @Override
    public void close();

}
