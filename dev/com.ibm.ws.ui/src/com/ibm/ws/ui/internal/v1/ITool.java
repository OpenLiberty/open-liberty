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
package com.ibm.ws.ui.internal.v1;

import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

/**
 * The generic interface of Tools. All tools have an ID and a type.
 * 
 * See implementations for type specifics.
 */
public interface ITool extends SelfValidatingPOJO {

    /**
     * Tool type: {@value #TYPE_FEATURE_TOOL}
     */
    String TYPE_FEATURE_TOOL = "featureTool";

    /**
     * Tool type: {@value #TYPE_BOOKMARK}
     */
    String TYPE_BOOKMARK = "bookmark";

    /**
     * Returns the tool's ID.
     * 
     * @return The tool ID
     */
    String getId();

    /**
     * Returns the tool's type.
     * 
     * @return The tool type
     */
    String getType();

    /**
     * Validates the Tool is complete. An incomplete Tool would
     * be one which is missing its required fields or has unsupported
     * characters in fields. The definition of a valid Tool depends on
     * the underlying implementation.
     * 
     * @throws InvalidToolException
     */
    @Override
    void validateSelf() throws InvalidToolException;

}
