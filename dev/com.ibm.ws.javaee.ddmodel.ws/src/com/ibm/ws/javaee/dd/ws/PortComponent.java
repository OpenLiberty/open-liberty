/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ws;

import java.util.List;

import javax.xml.namespace.QName;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.wsclient.Addressing;
import com.ibm.ws.javaee.dd.common.wsclient.Handler;
import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.javaee.dd.common.wsclient.RespectBinding;

public interface PortComponent {

    /**
     * @return &lt;description>, or null if unspecified
     */
    public Description getDescription();

    /**
     * @return &lt;display-name>, or null if unspecified
     */
    public DisplayName getDisplayName();

    /**
     * @return &lt;icon>, or null if unspecified
     */
    public Icon getIcon();

    public String getPortComponentName();

    public QName getWSDLService();

    public QName getWSDLPort();

    public boolean isSetEnableMTOM();

    public boolean isSetMTOMThreshold();

    public boolean isEnableMTOM();

    public int getMTOMThreshold();

    public Addressing getAddressing();

    public RespectBinding getRespectBinding();

    public String getProtocolBinding();

    public String getServiceEndpointInterface();

    public ServiceImplBean getServiceImplBean();

    public List<HandlerChain> getHandlerChains();

    public List<Handler> getHandlers();
}
