/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
package com.ibm.tra.outbound.base;

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

    public String getAdapterName() {
        return adapterName;
    }

    public String getAdapterShortDescription() {
        return adapterShortDescription;
    }

    public String getAdapterVendorName() {
        return adapterVendorName;
    }

    public String getAdapterVersion() {
        return adapterVersion;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String[] getInteractionSpecsSupported() {
        return interactionSpecsSupported;
    }

    public boolean supportsExecuteWithInputAndOutputRecord() {
        return executeWithInputAndOutputRecord;
    }

    public boolean supportsExecuteWithInputRecordOnly() {
        return executeWithInputRecordOnly;
    }

    public boolean supportsLocalTransactionDemarcation() {
        return localTransactionDemarcation;
    }

}
