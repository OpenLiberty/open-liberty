/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.couchdb.fat.web;

import org.ektorp.support.CouchDbDocument;

/**
 * Simple Couch document containing a String.
 */
public class CouchDocument extends CouchDbDocument {
    /**  */
    private static final long serialVersionUID = 1L;
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String entry) {
        this.content = entry;
    }
}
