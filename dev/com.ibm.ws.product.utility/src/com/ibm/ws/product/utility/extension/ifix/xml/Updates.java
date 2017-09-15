/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension.ifix.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

public class Updates {
    @XmlElement(name = "file")
    private Set<UpdatedFile> files;

    public Updates() {
        //required empty constructor for jaxb
    }

    public Updates(Set<UpdatedFile> files) {
        this.files = files;
    }

    /**
     * @return the files
     */
    public Set<UpdatedFile> getFiles() {
        return files;
    }
}
