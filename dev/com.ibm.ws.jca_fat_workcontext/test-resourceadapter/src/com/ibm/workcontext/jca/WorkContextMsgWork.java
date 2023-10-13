/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package com.ibm.workcontext.jca;

import java.util.ArrayList;
import java.util.List;

import jakarta.resource.spi.work.HintsContext;
//javax to jakarta ?
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkContext;
import jakarta.resource.spi.work.WorkContextProvider;

/**
 * Notifies message of work
 */
public class WorkContextMsgWork implements Work, WorkContextProvider {

    private final String JCA;

    public WorkContextMsgWork(String JCA) {
        this.JCA = JCA;
    }

    @Override
    public void release() {

    }

    @Override
    public void run() {

        System.out.println("run Work for JCA scheduleWork ");
        //   HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + queryString, toFind);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.work.WorkContextProvider#getWorkContexts()
     */
    @Override
    public List<WorkContext> getWorkContexts() {
        //
        System.out.println(" -- debug WorkContextMsgWork -> getWorkContexts");

        HintsContext hints = new HintsContext();
        hints.setName(JCA);
        hints.setHint("first hint", "true");
        hints.setHint("second hint", "false");
        List<WorkContext> contexts = new ArrayList<WorkContext>();
        contexts.add(hints);

        return contexts;

        // return theWorkContextList;
    }

    private List<WorkContext> theWorkContextList;

    public void setWorkContexts(List<WorkContext> inputWorkContextList) {
        //
        theWorkContextList = inputWorkContextList;

    }

}
