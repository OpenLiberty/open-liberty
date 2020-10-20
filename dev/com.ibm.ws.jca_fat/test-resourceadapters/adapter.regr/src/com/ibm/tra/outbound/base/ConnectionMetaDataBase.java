/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra.outbound.base;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;

class ConnectionMetaDataBase implements ConnectionMetaData {

    @Override
    public String getEISProductName() throws ResourceException {
        return "FVTDummyAdapter";
    }

    @Override
    public String getEISProductVersion() throws ResourceException {
        return "1.0";
    }

    @Override
    public String getUserName() throws ResourceException {
        return "user";
    }

    /**
     * The following data are required by Tivoli's TMTP. We return pre-defined dummy data here.
     * Real resource adapter may use a different way to get the data. However, how to get the data
     * in RA is not what we are testing for InteractionMetrics.
     *
     * AdapterName
     * AdapterShortDescription
     * AdapterVendorName
     * AdapterVersion
     * InteractionSpecsSupported
     * SpecVersion
     */
    public static String getAdapterName() {
        return "FVTDummyAdapter";
    }

    public static String getAdapterShortDescription() {
        return "A dummy CCI adapater for FVT";
    }

    public static String getAdapterVendorName() {
        return "J2C FVT";
    }

    public static String getAdapterVersion() {
        return "1.1";
    }

    public static String getInteractionSpecsSupported() {
        return "2.0";
    }

    public static String getSpecVersion() {
        return "2.0";
    }
}
