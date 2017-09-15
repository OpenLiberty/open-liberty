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
package com.ibm.ws.jca.utils.xml.ra.v10;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
@XmlType(name = "IconType", propOrder = { "smallIcon", "largeIcon" })
public class Ra10Icon {

    @XmlElement(name = "small-icon")
    private String smallIcon;
    @XmlElement(name = "large-icon")
    private String largeIcon;

    /**
     * @return the smallIcon
     */
    public String getSmallIcon() {
        return smallIcon;
    }

    /**
     * @return the largeIcon
     */
    public String getLargeIcon() {
        return largeIcon;
    }

}
