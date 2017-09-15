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
package com.ibm.wsspi.threadcontext;

/**
 * This interface describes information that is made available to a ThreadContextProvider to help deserialize a ThreadContext from its
 * serialized state.
 */
public interface ThreadContextDeserializationInfo {
    /**
     * Returns the value of a property that describes the contextual task and provides additional details
     * about how the task submitter wants it to run.
     * 
     * @param name The name of the property that should be retrieved.
     * @return The value of the property, or null if the property does not exist.
     */
    public String getExecutionProperty(String name);

    /**
     * Returns the MetaDataIdentifier of the thread from which context was captured.
     * 
     * @return the MetaDataIdentifier of the thread from which context was captured.
     */
    public String getMetadataIdentifier();
}
