/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty;

import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;

/**
 *
 */
public interface Netty {

    /**
     *
     */
    void destroy();

    /**
     * @return
     */
    static WsByteBufferPoolManager getBufferManager() {
        // TODO Auto-generated method stub
        return null;
    }

}
