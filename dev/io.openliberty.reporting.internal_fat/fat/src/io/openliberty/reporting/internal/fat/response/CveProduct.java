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

public class CveProduct {
    public String productName;
    public String url;
    public List<Cves> cves;

    public CveProduct() {

    }

    public CveProduct(String productName, String url, List<Cves> cves) {
        this.productName = productName;
        this.url = url;
        this.cves = cves;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Cves> getCves() {
        return this.cves;
    }

    public void setCves(List<Cves> cves) {
        this.cves = new ArrayList<>();
        this.cves.addAll(cves);
    }

    public void addCve(Cves cve) {
        this.cves.add(cve);
    }

}
