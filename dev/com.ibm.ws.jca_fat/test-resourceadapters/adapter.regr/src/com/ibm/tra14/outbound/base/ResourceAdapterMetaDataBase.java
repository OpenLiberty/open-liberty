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

package com.ibm.tra14.outbound.base;

class ResourceAdapterMetaDataBase implements javax.resource.cci.ResourceAdapterMetaData {

    private String adapterName = "FVTCCIAdapter";
    private String adapterShortDescription = "CCI implementation for J2C FVT";
    private String adapterVendorName = "J2CFVT";
    private String adapterVersion = "1.0";
    private String[] interactionSpecsSupported = { "fvt.cciadapter.CCIInteractionSpecImpl" };
    private String specVersion = "1.0";
    private boolean executeWithInputAndOutputRecord = true;
    private boolean executeWithInputRecordOnly = true;
    private boolean localTransactionDemarcation = true;

    public ResourceAdapterMetaDataBase() {

    }

    @Override
    public String getAdapterName() {
        return adapterName;
    }

    @Override
    public String getAdapterShortDescription() {
        return adapterShortDescription;
    }

    @Override
    public String getAdapterVendorName() {
        return adapterVendorName;
    }

    @Override
    public String getAdapterVersion() {
        return adapterVersion;
    }

    @Override
    public String getSpecVersion() {
        return specVersion;
    }

    @Override
    public String[] getInteractionSpecsSupported() {
        return interactionSpecsSupported;
    }

    @Override
    public boolean supportsExecuteWithInputAndOutputRecord() {
        return executeWithInputAndOutputRecord;
    }

    @Override
    public boolean supportsExecuteWithInputRecordOnly() {
        return executeWithInputRecordOnly;
    }

    @Override
    public boolean supportsLocalTransactionDemarcation() {
        return localTransactionDemarcation;
    }

}
