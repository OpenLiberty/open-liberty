/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Defines TCP options for channel framework
 *
 * @author Tim Burns
 *
 */
public class TcpOptions extends ConfigElement {

    private Boolean soReuseAddr;
    private String addressIncludeList;
    private String addressExcludeList;
    private String hostNameIncludeList;
    private String hostNameExcludeList;

    public Boolean isSoReuseAddr() {
        return this.soReuseAddr;
    }

    public String getAddressIncludeList() {
        return addressIncludeList;
    }

    public String getAddressExcludeList() {
        return addressExcludeList;
    }

    public String getHostNameIncludeList() {
        return hostNameIncludeList;
    }

    public String getHostNameExcludeList() {
        return hostNameExcludeList;
    }

    @XmlAttribute
    public void setSoReuseAddr(Boolean soReuseAddr) {
        this.soReuseAddr = soReuseAddr;
    }

    @XmlAttribute
    public void setAddressIncludeList(String addressIncludeList) {
        this.addressIncludeList = addressIncludeList;
    }

    @XmlAttribute
    public void setAddressExcludeList(String addressExcludeList) {
        this.addressExcludeList = addressExcludeList;
    }

    @XmlAttribute
    public void setHostNameIncludeList(String hostNameIncludeList) {
        this.hostNameIncludeList = hostNameIncludeList;
    }

    @XmlAttribute
    public void setHostNameExcludeList(String hostNameExcludeList) {
        this.hostNameExcludeList = hostNameExcludeList;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("TcpOptions{");
        if (getId() != null)
            buf.append("id=\"" + this.getId() + "\" ");
        if (soReuseAddr != null)
            buf.append("soReuseAddr=\"" + soReuseAddr + "\" ");
        if (getAddressIncludeList() != null)
            buf.append("addressIncludeList=\"" + addressIncludeList + "\" ");
        if (getAddressExcludeList() != null)
            buf.append("addressExcludeList=\"" + addressExcludeList + "\" ");
        if (getHostNameIncludeList() != null)
            buf.append("hostNameIncludeList=\"" + hostNameIncludeList + "\" ");
        if (getHostNameExcludeList() != null)
            buf.append("hostNameExcludeList=\"" + hostNameExcludeList + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
