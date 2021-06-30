/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.openapi20.utils.MessageConstants;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = MergeDisabledAlerter.class)
public class MergeDisabledAlerter {
    
    private static final TraceComponent tc = Tr.register(MergeDisabledAlerter.class);

    private boolean multiAppWarningGiven = false;

    /**
     * Called at the point where we notice that multiple modules are deployed but the OpenAPI config is set to only generate docs from the first module
     * @param firstModule the module which <i>is</i> being used to generate openAPI documentation
     */
    public void setUsingMultiModulesWithoutConfig(OpenAPIProvider firstModule) {
        synchronized (this) {
            if (!multiAppWarningGiven && ProductInfo.getBetaEdition()) {
                Tr.info(tc, MessageConstants.OPENAPI_MERGE_DISABLED_CWWKO1663I, firstModule);
                multiAppWarningGiven = true;
            }
        }
    }

}
