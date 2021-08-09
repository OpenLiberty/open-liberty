/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.outstream;

import com.ibm.wsspi.http.HttpOutputStream;

/**
 *
 */
public abstract class HttpOutputStreamConnectWeb extends HttpOutputStream {

    public abstract void setObserver(HttpOutputStreamObserver obs);

    public abstract void setWebC_headersWritten(boolean headersWritten);

    public abstract void setWC_remoteUser(String user);

}
