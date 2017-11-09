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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class CakeArrival {
    public List<CakeReport> getCakeReports() {
        return cakes;
    }

    public void addCake(String cakeObserver, long tid) {
        System.out.println("addCake - " + cakeObserver + ", tid - " + tid);
        CakeReport report = new CakeReport(cakeObserver, tid);
        this.cakes.add(report);
    }

    private List<CakeReport> cakes = new CopyOnWriteArrayList<>();
}
