/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.validator.adapter;

import javax.resource.cci.ResourceAdapterMetaData;

public class ResourceAdapterMetaDataImpl implements ResourceAdapterMetaData {
    @Override
    public String getAdapterName() {
        return "TestValidationAdapter";
    }

    @Override
    public String getAdapterShortDescription() {
        return "This tiny resource adapter doesn't do much at all.";
    }

    @Override
    public String getAdapterVendorName() {
        return "OpenLiberty";
    }

    @Override
    public String getAdapterVersion() {
        return "28.45.53";
    }

    @Override
    public String[] getInteractionSpecsSupported() {
        return new String[0];
    }

    @Override
    public String getSpecVersion() {
        return "1.7";
    }

    @Override
    public boolean supportsExecuteWithInputAndOutputRecord() {
        return false;
    }

    @Override
    public boolean supportsExecuteWithInputRecordOnly() {
        return false;
    }

    @Override
    public boolean supportsLocalTransactionDemarcation() {
        return false;
    }
}
