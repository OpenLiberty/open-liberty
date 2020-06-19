/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.restConsumer.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @author anupag
 *
 */
@Schema(name = "AppNameList", description = "POJO that represents the app names.")
public class AppNameList {

    @Schema(required = true)
    private List<String> appNames;

    public List<String> getAppNames() {
        return appNames;
    }

    public void setAppNames(List<String> appNames) {
        this.appNames = appNames;
    }

}
