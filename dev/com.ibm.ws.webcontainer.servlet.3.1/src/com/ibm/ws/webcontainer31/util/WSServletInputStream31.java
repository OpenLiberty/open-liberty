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
package com.ibm.ws.webcontainer31.util;

import javax.servlet.ReadListener;

import com.ibm.wsspi.webcontainer.util.WSServletInputStream;


public abstract class WSServletInputStream31 extends WSServletInputStream {

    public abstract boolean isFinished();
    public abstract boolean isReady();
    public abstract void setReadListener(ReadListener readListener);

}
