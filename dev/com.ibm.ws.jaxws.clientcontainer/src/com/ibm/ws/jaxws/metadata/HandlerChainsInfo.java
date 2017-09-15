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
package com.ibm.ws.jaxws.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class HandlerChainsInfo implements Serializable {

    private static final long serialVersionUID = -3595218623301350295L;

    private final List<HandlerChainInfo> handlerChainInfos = new ArrayList<HandlerChainInfo>();

    public List<HandlerChainInfo> getHandlerChainInfos() {
        return Collections.unmodifiableList(handlerChainInfos);
    }

    public boolean addHandlerChainInfo(HandlerChainInfo handlerChainInfo) {
        return handlerChainInfos.add(handlerChainInfo);
    }

    public boolean removeHandlerChainInfo(HandlerChainInfo handlerChainInfo) {
        return handlerChainInfos.remove(handlerChainInfo);
    }

    public List<HandlerInfo> getAllHandlerInfos() {
        List<HandlerInfo> handlerInfos = new ArrayList<HandlerInfo>();

        for (HandlerChainInfo chainInfo : handlerChainInfos) {
            handlerInfos.addAll(chainInfo.getHandlerInfos());
        }
        return handlerInfos;
    }

}
