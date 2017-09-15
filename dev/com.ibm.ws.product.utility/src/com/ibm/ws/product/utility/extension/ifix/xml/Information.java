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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class Information
{
    @XmlValue
    private String content;
    @XmlAttribute
    private String version;
    @XmlAttribute
    private String name;

    public Information()
    {
        //required blank constructor
    }

    public Information(String name, String version, String content)
    {
        this.name = name;
        this.version = version;
        this.content = content;
    }
}
