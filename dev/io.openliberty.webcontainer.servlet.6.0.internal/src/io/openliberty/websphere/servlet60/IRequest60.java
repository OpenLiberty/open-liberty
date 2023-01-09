/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.websphere.servlet60;

import com.ibm.websphere.servlet40.IRequest40;

/**
 * Since: Servlet 6.0
 */
public interface IRequest60 extends IRequest40 {

    /**
     * Added to support ServletRequest.getProtocolRequestId()
     *
     * @return (int) stream id; -1 otherwise. Need to convert to String.
     */
    public int getStreamId();

    /**
     * Added to support ServletConnection.getConnectionId()
     *
     * @return (int) connection id from dispatcher link. Need to convert to String.
     */
    public int getConnectionId();
}
