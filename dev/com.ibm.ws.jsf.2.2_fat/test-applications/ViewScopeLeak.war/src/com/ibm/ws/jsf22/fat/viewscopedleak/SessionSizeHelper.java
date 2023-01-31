/*
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
 */
package com.ibm.ws.jsf22.fat.viewscopedleak;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Named
@ApplicationScoped
public class SessionSizeHelper implements java.io.Serializable {

    public static int checkSessionSizeOfWeldS() throws Exception {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                        .getExternalContext()
                        .getRequest();
        HttpSession session = request.getSession();
        for (String key : Collections.list(session.getAttributeNames())) {
            Object o = session.getAttribute(key);
            if (key.startsWith("WELD_S#")) {
                return sizeOf(o);
            }
        }
        throw new Exception("Attribute WELD_S not found in session");
    }

    private static int sizeOf(Object o) {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(byteOutputStream);
            objectOutputStream.writeObject(o);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteOutputStream.size();
    }
}
