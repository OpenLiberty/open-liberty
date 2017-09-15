/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata.extended;

/**
 * This interface allows component metadata to customize its persistent identifier.
 */
public interface IdentifiableComponentMetaData {
    /**
     * Returns an identifier for the component metadata. The identifier must remain valid across all instances of the server,
     * and for all releases where this type of component metadata is supported.
     * The identifier must start with a prefix of alphanumeric characters followed by the # character.
     * A DeferredMetaDataFactory implementation must be registered to handle metadata with the chosen prefix.
     * The following prefixes are already taken (EJB, WEB, CONNECTOR)
     * 
     * @return an identifier for the component metadata.
     */
    String getPersistentIdentifier();
}
