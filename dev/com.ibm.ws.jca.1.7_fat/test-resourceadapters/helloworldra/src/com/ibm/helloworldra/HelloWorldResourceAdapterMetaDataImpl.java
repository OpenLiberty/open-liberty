/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import javax.resource.cci.ResourceAdapterMetaData;

public class HelloWorldResourceAdapterMetaDataImpl implements ResourceAdapterMetaData {

    private static final String ADAPTER_VERSION = "1.0";
    private static final String ADAPTER_VENDOR_NAME = "Willy Farrell";
    private static final String ADAPTER_NAME = "Hello World Resource Adapter";
    private static final String ADAPTER_DESCRIPTION = "A simple sample resource adapter";
    private static final String SPEC_VERSION = "1.0";
    private static final String[] INTERACTION_SPECS_SUPPORTED = { "com.ibm.helloworldra.HelloWorldInteractionSpecImpl" };

    /**
     * Constructor for HelloWorldResourceAdapterMetaDataImpl
     */
    public HelloWorldResourceAdapterMetaDataImpl() {

        super();
    }

    /**
     * @see ResourceAdapterMetaData#getAdapterVersion()
     */
    @Override
    public String getAdapterVersion() {

        return ADAPTER_VERSION;
    }

    /**
     * @see ResourceAdapterMetaData#getAdapterVendorName()
     */
    @Override
    public String getAdapterVendorName() {

        return ADAPTER_VENDOR_NAME;
    }

    /**
     * @see ResourceAdapterMetaData#getAdapterName()
     */
    @Override
    public String getAdapterName() {

        return ADAPTER_NAME;
    }

    /**
     * @see ResourceAdapterMetaData#getAdapterShortDescription()
     */
    @Override
    public String getAdapterShortDescription() {

        return ADAPTER_DESCRIPTION;
    }

    /**
     * @see ResourceAdapterMetaData#getSpecVersion()
     */
    @Override
    public String getSpecVersion() {

        return SPEC_VERSION;
    }

    /**
     * @see ResourceAdapterMetaData#getInteractionSpecsSupported()
     */
    @Override
    public String[] getInteractionSpecsSupported() {

        return INTERACTION_SPECS_SUPPORTED;
    }

    /**
     * @see ResourceAdapterMetaData#supportsExecuteWithInputAndOutputRecord()
     */
    @Override
    public boolean supportsExecuteWithInputAndOutputRecord() {

        return true;
    }

    /**
     * @see ResourceAdapterMetaData#supportsExecuteWithInputRecordOnly()
     */
    @Override
    public boolean supportsExecuteWithInputRecordOnly() {

        return false;
    }

    /**
     * @see ResourceAdapterMetaData#supportsLocalTransactionDemarcation()
     */
    @Override
    public boolean supportsLocalTransactionDemarcation() {

        return false;
    }

}