/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package ra.cf;

import jakarta.resource.cci.ResourceAdapterMetaData;

public class DummyResourceAdapterMetaDataImpl implements ResourceAdapterMetaData {

    @Override
    public String getAdapterName() {
        return "DUMMY_NAME";
    }

    @Override
    public String getAdapterShortDescription() {
        return null;
    }

    @Override
    public String getAdapterVendorName() {
        return null;
    }

    @Override
    public String getAdapterVersion() {
        return null;
    }

    @Override
    public String[] getInteractionSpecsSupported() {
        return null;
    }

    @Override
    public String getSpecVersion() {
        return null;
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
