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
package com.ibm.ws.security.wim.xpath.util;

import com.ibm.ws.security.wim.xpath.mapping.datatype.XPathNode;
import com.ibm.wsspi.security.wim.exception.WIMException;

public interface XPathTranslateHelper {

    void genSearchString(StringBuffer searchExpBuffer, XPathNode node) throws WIMException;
}
