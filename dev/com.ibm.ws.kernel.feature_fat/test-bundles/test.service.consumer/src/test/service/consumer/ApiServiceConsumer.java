/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.service.consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.kernel.feature.test.api.ApiService;

/**
 *
 */
@Component(service = {})
public class ApiServiceConsumer {
    private ApiService apiService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    @Activate
    protected void activate() {
        if (apiService == null) {
            // This is unexpected because ApiService should be accessible
            System.out.println("ApiService - FAILED");
        } else {
            System.out.println(apiService.doTest());
        }
    }
}
