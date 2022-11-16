/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package userinfo.servlets;

import jakarta.servlet.annotation.WebServlet;

@WebServlet("/JsonUserInfoOtherCallerNameClaimGoodCaller")
public class JsonUserInfoOtherCallerNameClaimGoodCallerServlet extends JsonUserInfoServlet {

    private static final long serialVersionUID = -145839375682343L;

    @Override
    protected String getSub() {
        return "badCallerName";
    }

}
