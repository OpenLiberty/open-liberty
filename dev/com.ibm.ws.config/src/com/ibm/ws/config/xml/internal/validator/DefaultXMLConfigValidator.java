/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal.validator;

import java.io.InputStream;

import com.ibm.ws.config.xml.internal.ServerConfiguration;

/**
 * Default configuration validator for Liberty.
 * <p>
 * This validator does not perform any validation. It is not used when Liberty
 * is embedded within another product.
 * 
 * @since V8.5 feature XXXXXX.
 */
public class DefaultXMLConfigValidator implements XMLConfigValidator {

    /**
     * Class constructor.
     */
    protected DefaultXMLConfigValidator() {}

    /** {@inheritDoc} */
    @Override
    public InputStream validateResource(InputStream configDocInputStream, String docLocation) {
        return configDocInputStream;
    }

    /** {@inheritDoc} */
    @Override
    public void validateConfig(ServerConfiguration configuration) {}
}
