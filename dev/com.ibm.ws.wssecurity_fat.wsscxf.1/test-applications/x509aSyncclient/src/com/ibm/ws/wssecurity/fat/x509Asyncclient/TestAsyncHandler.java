/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.x509Asyncclient;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import test.wssecfvt.x509async.types.ResponseString;

public class TestAsyncHandler implements AsyncHandler<ResponseString> {
  private ResponseString reply;

  public void handleResponse(Response<ResponseString> response) {
    try {
      System.out.println("In ResponseHandler");
      reply = response.get();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public String getResponse() {
    System.out.println("In getResponse");
    return reply.getStringres();
  }
}