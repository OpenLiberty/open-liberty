/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import javax.ejb.EJBLocalObject;

public interface StatefulPassEJBLocal extends EJBLocalObject {
    public void setStringValue(String value);

    public String getStringValue();

    public void setIntegerValue(Integer value);

    public Integer getIntegerValue();

    public void setSerObjValue(SerObj value);

    public SerObj getSerObjValue();

    public void setSerObj2Value(SerObj2 value);

    public SerObj2 getSerObj2Value();

/*
 * public void checkMySerObjStart();
 * public void checkMySerObjEnd();
 */
    public int getPassivateCount();

    public int getActivateCount();

    public void checkTimerStart();

    public void checkTimerEnd();

    public void finish();
}
