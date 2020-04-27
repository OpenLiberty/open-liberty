/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.persistence;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.ibm.websphere.jsonsupport.JSONMarshallException;

/**
 * Version agnostic persistence provider - this service is used to store
 * and retrieve POJOs.
 * <p>
 * The implementation will store and load JSON-representable POJOs. Where the
 * JSON representations are stored is handled by the underlying provider.
 * The underlying implementation details are technically unimportant for the
 * callers, however we do use Jackson underneath and we therefore expose the
 * Jackson exceptions.
 * <p>
 * Implementations must implement thread-safe methods such that the caller
 * need not perform synchronization itself.
 */
public interface IPersistenceProvider {

    /**
     * Stores the given POJO by the specified name.
     * <p>
     * This method is thread-safe. Concurrent access to the persistence layer
     * is handled transparently by the implementation.
     * 
     * @param name The name by which to store the POJO.
     * @param pojo The POJO to be stored.
     * @throws JSONMarshallException Thrown to signal fatal problems with mapping the POJO to a JSON representation.
     * @throws IOException Thrown when there was a general problem accessing the underlying persistence layer.
     */
    void store(String name, Object pojo) throws JSONMarshallException, IOException;

    /**
     * Stores the given String by the specified name.
     * <p>
     * This method is thread-safe. Concurrent access to the persistence layer
     * is handled transparently by the implementation.
     * 
     * @param name The name by which to store the string.
     * @param content The String to be stored.
     * @throws IOException Thrown when there was a general problem accessing the underlying persistence layer.
     */
    void storePlainText(String name, String content) throws JSONMarshallException, IOException;

    /**
     * Deletes the given String by the specified name.
     * <p>
     * This method is thread-safe. Concurrent access to the persistence layer
     * is handled transparently by the implementation.
     * 
     * @param name The name by which to store the string.
     * @throws IOException Thrown when there was a general problem accessing the underlying persistence layer.
     * @return <code>true</code> the file is successfully deleted.
     */
    boolean delete(String name) throws IOException;

    /**
     * Loads a POJO of the specified Class by the specified name.
     * <p>
     * This method is thread-safe. Concurrent access to the persistence layer
     * is handled transparently by the implementation.
     * 
     * @param name The name by which to find the POJO.
     * @param clazz The Class of the POJO to load.
     * @return An instance of the specified Class populated by the JSON representation
     * @throws JsonMappingException Thrown to signal fatal problems with mapping the JSON representation to a POJO.
     * @throws JsonParseException Thrown for parsing problems, used when non-well-formed content (content that does not conform to JSON syntax as per specification) is encountered.
     * @throws FileNotFoundException If the requested name to load does not exist
     * @throws IOException Thrown when there was a general problem accessing the underlying persistence layer.
     */
    <T> T load(String name, Class<T> clazz) throws JSONMarshallException, FileNotFoundException, IOException;

    /**
     * Loads content of the specified name.
     * <p>
     * This method is thread-safe. Concurrent access to the persistence layer
     * is handled transparently by the implementation.
     * 
     * @param name The name by which to find the string.
     * @return The content of the specified name
     * @throws FileNotFoundException If the requested name to load does not exist
     * @throws IOException Thrown when there was a general problem accessing the underlying persistence layer.
     */
    String loadPlainText(String name) throws FileNotFoundException, IOException;

    /**
     * Returns the time that the wrapped/proxied resource was last modified.
     * <p>
     * This method is thread-safe. Concurrent access to the persistence layer
     * is handled transparently by the implementation.
     * 
     * @return A <code>long</code> value representing the time the file was last
     *         modified, measured in milliseconds since the epoch (00:00:00 GMT,
     *         January 1, 1970), or <code>0L</code> if the file does not exist
     *         or if an I/O error occurs
     */
    long getLastModified(String name);

    /**
     * Checks if the specified name exists in the persistence layer.
     * 
     * @param name the resource to be checked.
     * @return <code>true</code> if the resource exists. Otherwise returns <code>false</code>.
     */
    boolean exists(String name);
}
