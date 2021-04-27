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

package filterandwrapper.war.files;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class MyResponseWrapper extends HttpServletResponseWrapper {
    private String contentType;

    public MyResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public void setContentType(String type) {

        this.contentType = type;

        //passing through to verify that wrapped response is active
        System.out.println("MyResponseWrapper:thisContentType:contentType: " + contentType);

        super.setContentType(contentType);
    }
}
