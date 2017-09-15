/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.location;

import java.io.IOException;

/**
 *
 */
public class ExistingResourceException extends IOException {
    private static final long serialVersionUID = 1L;
    private static final String msgString = "File operation failed, resource already exists (source=%s, target=%s)";

    private final String source;
    private final String target;

    /**
     * @param message
     */
    public ExistingResourceException(String source, String target) {
        super(String.format(msgString, source, target));
        this.source = source;
        this.target = target;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }
}
