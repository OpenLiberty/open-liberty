/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

public class MpOpenAPIElement extends ConfigElement {

    private String docPath;
    private String uiPath;

    /**
     * @return the docPath
     */
    public String getDocPath() {
        return docPath;
    }

    /**
     * @param docPath the docPath to set
     */
    @XmlAttribute(name = "docPath")
    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    /**
     * @return the uiPath
     */
    public String getUiPath() {
        return uiPath;
    }

    /**
     * @param uiPath the uiPath to set
     */
    @XmlAttribute(name = "uiPath")
    public void setUiPath(String uiPath) {
        this.uiPath = uiPath;
    }

    @Override
    public String toString() {
        return "MpOpenAPIElement [docPath=" + docPath + ", uiPath=" + uiPath + "]";
    }

}
