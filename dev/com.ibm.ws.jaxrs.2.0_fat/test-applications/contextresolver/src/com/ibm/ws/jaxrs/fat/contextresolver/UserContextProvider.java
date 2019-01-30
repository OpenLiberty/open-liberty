/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.contextresolver;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

@Provider
public class UserContextProvider implements ContextResolver<JAXBContext> {

    @Override
    public JAXBContext getContext(Class<?> clazz) {
        if (clazz == User.class) {
            try {
                return JAXBContext.newInstance(com.ibm.ws.jaxrs.fat.contextresolver.jaxb.ObjectFactory.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
