/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    @Override
    public boolean deletesAreDetected(int type) {
        return deletesDetected;
    }

    @Override
    public boolean insertsAreDetected(int type) {
        return insertsDetected;
    }

    @Override
    public boolean othersDeletesAreVisible(int type) {
        return othersDeletesVisible;
    }

    @Override
    public boolean othersInsertsAreVisible(int type) {
        return othersInsertsVisible;
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) {
        return othersUpdatesVisible;
    }

    @Override
    public boolean ownDeletesAreVisible(int type) {
        return ownDeletesVisible;
    }

    @Override
    public boolean ownInsertsAreVisible(int type) {
        return ownInsertsVisible;
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) {
        return ownUpdatesVisible;
    }

    @Override
    public boolean supportsResultSetType(int type) {
        //For now lets just assume that we support all three types
        return true;
    }

    @Override
    public boolean supportsResultTypeConcurrency(int type, int concurrency) {
        //Assume for now that we support all combinations of types
        return true;
    }

    @Override
    public boolean updatesAreDetected(int type) {
        return false;
    }
}
