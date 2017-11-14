/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.mechanisms.autoapplysession;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

import web.war.mechanisms.BaseAuthMech;

//TODO: Remove implements HttpAuthenticationMechanism after fixing CDI extension to recognize mechanisms due to extends.
@ApplicationScoped
@AutoApplySession
public class AutoApplySessionAuthMech extends BaseAuthMech implements HttpAuthenticationMechanism {

    public AutoApplySessionAuthMech() {
        sourceClass = AutoApplySessionAuthMech.class.getName();
    }

}
