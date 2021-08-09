/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.mechanisms.scoped.request;

import javax.enterprise.context.RequestScoped;

import web.war.mechanisms.BaseAuthMech;

@RequestScoped
public class RequestScopedAuthMech extends BaseAuthMech {

    public RequestScopedAuthMech() {
        sourceClass = RequestScopedAuthMech.class.getName();
    }

}
