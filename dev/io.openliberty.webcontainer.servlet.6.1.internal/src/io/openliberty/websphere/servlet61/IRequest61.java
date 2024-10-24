/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.websphere.servlet61;

import io.openliberty.websphere.servlet60.IRequest60;

public interface IRequest61 extends IRequest60 {

    /**
     * Support request attribute "jakarta.servlet.request.secure_protocol"
     *
     * @return (String) secure protocol for this request; i.e TLSv1.3, TLSv1.2 ....
     */
    public String getSecureProtocol();
}
