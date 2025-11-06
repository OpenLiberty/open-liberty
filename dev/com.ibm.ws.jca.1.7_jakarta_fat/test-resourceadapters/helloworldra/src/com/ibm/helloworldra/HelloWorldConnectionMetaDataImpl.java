/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.ConnectionMetaData;

public class HelloWorldConnectionMetaDataImpl implements ConnectionMetaData {

    private static final String PRODUCT_NAME = "Hello World EIS";
    private static final String PRODUCT_VERSION = "1.0";
    private static final String USER_NAME = "Not applicable";

    /**
     * Constructor for HelloWorldConnectionMetaDataImpl
     */
    public HelloWorldConnectionMetaDataImpl() {

        super();
    }

    /**
     * @see ConnectionMetaData#getEISProductName()
     */
    @Override
    public String getEISProductName() throws ResourceException {

        return PRODUCT_NAME;
    }

    /**
     * @see ConnectionMetaData#getEISProductVersion()
     */
    @Override
    public String getEISProductVersion() throws ResourceException {

        return PRODUCT_VERSION;
    }

    /**
     * @see ConnectionMetaData#getUserName()
     */
    @Override
    public String getUserName() throws ResourceException {

        return USER_NAME;
    }

}