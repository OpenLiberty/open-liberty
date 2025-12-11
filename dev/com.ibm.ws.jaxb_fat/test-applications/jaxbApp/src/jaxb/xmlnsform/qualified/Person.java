/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package jaxb.xmlnsform.qualified;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Person", namespace = "http://a")
@XmlAccessorType(XmlAccessType.FIELD)
public class Person {
    @XmlElement
    private String firstNameWithPrefix;

    @XmlElement
    private String LastNameWithOutPrefix;

    public String getFirstNameWithPrefix() {
        return firstNameWithPrefix;
    }

    public void setFirstNameWithPrefix(String firstNameWithPrefix) {
        this.firstNameWithPrefix = firstNameWithPrefix;
    }

    public String getLastNameWithOutPrefix() {
        return LastNameWithOutPrefix;
    }

    public void setLastNameWithOutPrefix(String lastNameWithOutPrefix) {
        LastNameWithOutPrefix = lastNameWithOutPrefix;
    }

    @Override
    public String toString() {
        return "Person{nameWithPrefix='" + firstNameWithPrefix + "', nameWithOutPrefix=" + LastNameWithOutPrefix + '}';
    }
}