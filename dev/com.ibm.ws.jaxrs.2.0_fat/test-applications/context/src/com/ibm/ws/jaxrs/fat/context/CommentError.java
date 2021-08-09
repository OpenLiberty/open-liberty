/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.context;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "commenterror")
public class CommentError {

    private String errorMessage;

    public String getErrorMessage() {
        return errorMessage;
    }

    @XmlElement(name = "message")
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
