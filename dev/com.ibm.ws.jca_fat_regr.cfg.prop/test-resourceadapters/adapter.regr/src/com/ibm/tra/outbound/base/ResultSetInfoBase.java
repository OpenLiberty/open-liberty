/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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

package com.ibm.tra.outbound.base;

class ResultSetInfoBase implements javax.resource.cci.ResultSetInfo {

    private boolean deletesDetected = true;
    private boolean insertsDetected = true;
    private boolean othersDeletesVisible = false;
    private boolean othersInsertsVisible = false;
    private boolean othersUpdatesVisible = false;
    private boolean ownDeletesVisible = true;
    private boolean ownInsertsVisible = true;
    private boolean ownUpdatesVisible = true;

    public ResultSetInfoBase() {

    }

    public boolean deletesAreDetected(int type) {
        return deletesDetected;
    }

    public boolean insertsAreDetected(int type) {
        return insertsDetected;
    }

    public boolean othersDeletesAreVisible(int type) {
        return othersDeletesVisible;
    }

    public boolean othersInsertsAreVisible(int type) {
        return othersInsertsVisible;
    }

    public boolean othersUpdatesAreVisible(int type) {
        return othersUpdatesVisible;
    }

    public boolean ownDeletesAreVisible(int type) {
        return ownDeletesVisible;
    }

    public boolean ownInsertsAreVisible(int type) {
        return ownInsertsVisible;
    }

    public boolean ownUpdatesAreVisible(int type) {
        return ownUpdatesVisible;
    }

    public boolean supportsResultSetType(int type) {
        //For now lets just assume that we support all three types
        return true;
    }

    public boolean supportsResultTypeConcurrency(int type, int concurrency) {
        //Assume for now that we support all combinations of types
        return true;
    }

    public boolean updatesAreDetected(int type) {
        return false;
    }
}
