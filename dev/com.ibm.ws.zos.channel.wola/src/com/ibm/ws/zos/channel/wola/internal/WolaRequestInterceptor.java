/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;

/**
 * WOLA request preInvoke/postInvoke interceptor interface.
 *
 * These interceptors are called around the invocation of the WOLA EJB request.
 */
public interface WolaRequestInterceptor {

    /**
     * @param wolaMessage - the WOLA message for the current request.
     *
     * @return an optional token value which will be passed back on postInvoke.
     */
    public Object preInvoke(WolaMessage wolaMessage);

    /**
     * @param preInvokeToken    - The token returned by preInvoke.
     * @param responseException - The exception response, or null if the request completed normally.
     */
    public void postInvoke(Object preInvokeToken, Exception responseException);
}
