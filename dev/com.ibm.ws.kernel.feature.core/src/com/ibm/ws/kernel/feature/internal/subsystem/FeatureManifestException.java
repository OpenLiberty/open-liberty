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
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.io.IOException;

/**
 * Use common messages to denote problems with feature manifests.
 */
public class FeatureManifestException extends IOException {
    /**  */
    private static final long serialVersionUID = -4933600526165761348L;

    private final String translated = null;

    /**
     * Use this when there
     */
    public FeatureManifestException(String message, String translatedMessage) {
        super(message);
    }

    @Override
    public String getLocalizedMessage() {
        if (translated == null)
            return getMessage();

        return translated;
    }
}
