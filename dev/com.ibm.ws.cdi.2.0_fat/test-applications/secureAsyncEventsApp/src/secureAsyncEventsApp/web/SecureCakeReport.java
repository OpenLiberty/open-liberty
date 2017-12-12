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
package secureAsyncEventsApp.web;

import javax.security.auth.Subject;

public class SecureCakeReport {

    private String cakeObserver = null;
    private long tid;
    private Subject cakeSubject = null;

    public SecureCakeReport(String obs, long tid, Subject cakeSubject) {
        this.cakeObserver = obs;
        this.tid = tid;
        this.cakeSubject = cakeSubject;
    }

    /**
     * @return the cakeObserver
     */
    public String getCakeObserver() {
        return cakeObserver;
    }

    public Subject getCakeSubject() {
        return cakeSubject;
    }

    /**
     * @return the tid
     */
    public long getTid() {
        return tid;
    }
}
