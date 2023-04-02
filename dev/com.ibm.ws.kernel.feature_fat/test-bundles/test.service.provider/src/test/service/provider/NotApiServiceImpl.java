/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
