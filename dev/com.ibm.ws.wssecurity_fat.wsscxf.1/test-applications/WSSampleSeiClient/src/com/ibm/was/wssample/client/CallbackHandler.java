/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.was.wssample.client;

import java.util.concurrent.ExecutionException;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import com.ibm.was.wssample.sei.echo.EchoStringResponse;

public class CallbackHandler implements AsyncHandler<EchoStringResponse> {

    private EchoStringResponse output;

    /*
     *
     * @see javax.xml.ws.AsyncHandler#handleResponse(javax.xml.ws.Response)
     */
    @Override
    public void handleResponse(Response<EchoStringResponse> response) {
        try {
            output = response.get();
        } catch (ExecutionException e) {
            System.out.println(">> CLIENT: CALLBACK Connection Exception");
        } catch (InterruptedException e) {
            System.out.println(">> CLIENT: CALLBACK Interrupted Exception");
        }
    }

    public EchoStringResponse getResponse() {
        return output;
    }
}
