/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.LogoutService;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

@Component(service = LogoutService.class)
public class BackchannelLogoutService implements LogoutService {

    private static TraceComponent tc = Tr.register(BackchannelLogoutService.class);

    @Override
    public void logout(HttpServletRequest req, HttpServletResponse res, WebAppSecurityConfig config) {
        // TODO
    }

}
