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
package io.openliberty.ejbcontainer.fat.msgendpoint.ejb;

public class ConcurrencyInfo {
    private int concurrentMsgNumber;

    public ConcurrencyInfo(int n) {
        concurrentMsgNumber = n;
    }

    synchronized public void setConcurrentMsgNumber(int n) {
        concurrentMsgNumber = n;
    }

    synchronized public void decreaseConcurrentMsgNumber() {
        concurrentMsgNumber--;
    }

    synchronized public int getConcurrentMsgNumber() {
        return concurrentMsgNumber;
    }
}
