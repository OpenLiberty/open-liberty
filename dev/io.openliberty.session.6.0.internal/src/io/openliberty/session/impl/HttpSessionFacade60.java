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
package io.openliberty.session.impl;

import com.ibm.ws.session.AbstractHttpSessionFacade;

import io.openliberty.session.impl.http.HttpSessionImpl60;

public class HttpSessionFacade60 extends AbstractHttpSessionFacade {

    private static final long serialVersionUID = 3108339284895967670L;

    public HttpSessionFacade60(HttpSessionImpl60 data) {
        super(data);
    }
}
