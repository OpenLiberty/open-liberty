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
package com.ibm.ws.http.channel.inputstream;

import com.ibm.wsspi.http.HttpInputStream;

/**
 *
 * The need of these APIs is to support Multiread
 *
 */
public abstract class HttpInputStreamConnectWeb extends HttpInputStream {

    public abstract void setISObserver(HttpInputStreamObserver obs);

    public abstract void restart();

    public abstract void setupforMultiRead(boolean set);

    public abstract void if_enableMultiReadofPostData_set(boolean set);

    public abstract void cleanupforMultiRead();

}
