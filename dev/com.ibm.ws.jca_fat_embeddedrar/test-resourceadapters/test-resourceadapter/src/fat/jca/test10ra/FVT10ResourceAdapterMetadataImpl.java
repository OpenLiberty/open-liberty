/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.test10ra;

import javax.resource.cci.ResourceAdapterMetaData;

public class FVT10ResourceAdapterMetadataImpl implements ResourceAdapterMetaData {

    @Override
    public String getAdapterName() {
        return "FVT10ResourceAdapter";
    }

    @Override
    public String getAdapterShortDescription() {
        return "1.0 Test Resource Adapter";
    }

    @Override
    public String getAdapterVendorName() {
        return "IBM";
    }

    @Override
    public String getAdapterVersion() {
        return "1.0";
    }

    @Override
    public String[] getInteractionSpecsSupported() {
        return new String[] { "fat.jca.test10ra.FVT10InteractionSpec" };
    }

    @Override
    public String getSpecVersion() {
        return "1.0";
    }

    @Override
    public boolean supportsExecuteWithInputAndOutputRecord() {
        return true;
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
