/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc22.link;

import com.ibm.ws.wsoc.link.LinkWrite;
import com.ibm.ws.wsoc.link.LinkWriteFactory;

public class LinkWriteFactory22 implements LinkWriteFactory {
    
    public LinkWrite getLinkWrite() {
        return new LinkWriteExt22();
    }
}
