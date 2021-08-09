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
package com.ibm.ws.jca.utils.xml.ra;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10Icon;

/**
 *
 */
@Trivial
@XmlType(propOrder = { "smallIcon", "largeIcon" })
public class RaIcon implements com.ibm.ws.javaee.dd.common.Icon {

    @XmlElement(name = "small-icon")
    private String smallIcon;
    @XmlElement(name = "large-icon")
    private String largeIcon;
    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    private String lang = "en";
    @XmlAttribute(name = "id")
    private String id;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.common.Icon#getSmallIcon()
     */
    @Override
    public String getSmallIcon() {
        return smallIcon;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.common.Icon#getLargeIcon()
     */
    @Override
    public String getLargeIcon() {
        return largeIcon;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.common.Icon#getLang()
     */
    @Override
    public String getLang() {
        return lang;
    }

    /**
     * get the id of the icon element
     * 
     * @return id
     */
    public String getId() {
        return id;
    }

    public void copyRa10Settings(Ra10Icon ra10Icon) {
        smallIcon = ra10Icon.getSmallIcon();
        largeIcon = ra10Icon.getLargeIcon();
    }

}
