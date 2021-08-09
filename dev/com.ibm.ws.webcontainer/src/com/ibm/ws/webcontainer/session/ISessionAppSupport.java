/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public interface ISessionAppSupport {
    public ObjectInputStream getObjectInputStream(InputStream in) throws IOException;
}
