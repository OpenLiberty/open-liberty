/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.param.entity;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/params/multientity")
public class MultipleEntityParamsResource {

    @POST
    public void getMultipleEntity(String s1, String s2) {
        return;
    }
}
