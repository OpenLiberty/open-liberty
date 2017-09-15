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
package com.ibm.ws.webcontainer.srt;

import java.util.Enumeration;

public interface IPrivateRequestAttributes
{
    public Object getPrivateAttribute(String name);
    
    @SuppressWarnings("unchecked")
    public Enumeration getPrivateAttributeNames();

    public void setPrivateAttribute(String name, Object value);
    
    public void removePrivateAttribute(String name);
}
