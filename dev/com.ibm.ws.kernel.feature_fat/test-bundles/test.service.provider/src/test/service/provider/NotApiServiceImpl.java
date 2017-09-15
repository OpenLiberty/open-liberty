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
package test.service.provider;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.kernel.feature.test.api.NotApiService;

/**
 *
 */
@Component(service = NotApiService.class)
public class NotApiServiceImpl implements NotApiService {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.kernel.feature.test.api.NotApiService#doTest()
     */
    @Override
    public String doTest() {
        return "NotApiService - FAILED";
    }

}
