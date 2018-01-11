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
package asyncEventsApp.web;

public class CakeReport {

    private String cakeObserver = null;
    private long tid;

    public CakeReport(String obs, long tid) {
        this.cakeObserver = obs;
        this.tid = tid;
    }

    /**
     * @return the cakeObserver
     */
    public String getCakeObserver() {
        return cakeObserver;
    }

    /**
     * @return the tid
     */
    public long getTid() {
        return tid;
    }
}
