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
package mpRestClient11.cdiPropsAndProviders;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import mpRestClient10.basicCdi.DuplicateWidgetException;

public class MyExceptionMapper implements ResponseExceptionMapper<MyException> {

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status == 409; //Conflict
    }

    @Override
    public MyException toThrowable(Response response) {
        return new MyException();
    }

}
