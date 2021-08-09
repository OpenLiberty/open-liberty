/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.util;

import javax.servlet.WriteListener;

import com.ibm.wsspi.webcontainer.util.WSServletOutputStream;

public abstract class WSServletOutputStream31 extends WSServletOutputStream {

    public abstract boolean isReady();
    public abstract void setWriteListener(WriteListener writeListener);
}
