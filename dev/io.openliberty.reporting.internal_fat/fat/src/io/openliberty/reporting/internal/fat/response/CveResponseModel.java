/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal.fat.response;

import java.util.ArrayList;
import java.util.List;

public class CveResponseModel {
    private static final String MODEL_VERSION = "0.1";
    private String modelVersion = MODEL_VERSION;
    private List<CveProduct> cveProducts;

    public CveResponseModel() {

    }

    public CveResponseModel(String modelVersion, List<CveProduct> cveProducts) {
        this.modelVersion = modelVersion;
        this.cveProducts = cveProducts;
    }

    public String getModelVersion() {
        return this.modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public void setCveProducts(List<CveProduct> cveProduct) {
        this.cveProducts = new ArrayList<>();
        this.cveProducts.addAll(cveProduct);
    }

    public void addCveProducts(CveProduct cveProduct) {
        this.cveProducts.add(cveProduct);
    }

    public List<CveProduct> getCveProducts() {
        return this.cveProducts;
    }

}