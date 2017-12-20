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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.security.auth.Subject;

import com.ibm.websphere.security.auth.WSSubject;

/**
 *
 */
@ApplicationScoped
public class CakeArrival {

    public List<SecureCakeReport> getCakeReports() {
        return cakes;
    }

    public void addCake(String cakeObserver, long tid) {
        Subject runAsSubject = null;

        try {
            runAsSubject = WSSubject.getRunAsSubject();
        } catch (Exception e2) { // WSSecurityException
            e2.printStackTrace();
        }

        SecureCakeReport report = new SecureCakeReport(cakeObserver, tid, runAsSubject);
        this.cakes.add(report);
    }

    private List<SecureCakeReport> cakes = new CopyOnWriteArrayList<>();
}
