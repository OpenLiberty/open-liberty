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

public class Cves {

    private String cveId;

    private String url;

    public Cves() {

    }

    public Cves(String cveId, String url) {
        this.cveId = cveId;
        this.url = url;
    }

    public String getCveId() {
        return this.cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
