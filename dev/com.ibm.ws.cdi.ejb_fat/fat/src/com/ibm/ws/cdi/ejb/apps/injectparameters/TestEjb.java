/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.injectparameters;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

@Stateless
@LocalBean
public class TestEjb {

    private TestResources resources;

    @Inject
    private void testInject(@Named("resource1") String res1,
                            @Named("resource2") String res2,
                            @Named("resource3") String res3,
                            @Named("resource4") String res4,
                            @Named("resource5") String res5,
                            @Named("resource6") String res6,
                            @Named("resource7") String res7,
                            @Named("resource8") String res8,
                            @Named("resource9") String res9,
                            @Named("resource10") String res10,
                            @Named("resource11") String res11,
                            @Named("resource12") String res12,
                            @Named("resource13") String res13,
                            @Named("resource14") String res14,
                            @Named("resource15") String res15,
                            @Named("resource16") String res16) {
        resources = new TestResources(res1, res2, res3, res4, res5, res6, res7, res8, res9, res10, res11, res12, res13, res14, res15, res16);
    }

    public TestResources getResult() {
        return resources;
    }

}
