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
package com.ibm.ws.zos.wlm;

import java.io.IOException;
import java.io.OutputStream;

/**
 * WLM Configuration data manager
 */
public interface WLMConfigManager {

    /**
     * Subsystem to use on WLM native services
     */
    String getSubSystem();

    /**
     * Subsystem name to use on WLM native services
     */
    String getSubSystemName();

    /**
     * WLM Create function name used on native services
     */
    String getCreateFunctionName();

    /**
     * Collection name to use for WLM native service invocations
     */
    String getClassifyCollectionName();

    /**
     * Format and Write diagnostic information
     */
    void writeDiagnostics(OutputStream out) throws IOException;

    /**
     * Add service class token to map.
     *
     * @param serviceClassTokenKey
     * @param serviceClassToken
     * @return
     */
    void putServiceClassToken(String serviceClassTokenKey, int serviceClassToken);

    /**
     * Get service class token.
     *
     * @param serviceClassTokenKey
     * @return
     */
    int getServiceClassToken(String serviceClassTokenKey);
}
