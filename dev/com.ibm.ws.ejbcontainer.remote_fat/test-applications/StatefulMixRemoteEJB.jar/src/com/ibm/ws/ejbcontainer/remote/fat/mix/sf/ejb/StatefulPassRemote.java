/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

public interface StatefulPassRemote {
    public void setStringValue(String value);

    public String getStringValue();

    public void setIntegerValue(Integer value);

    public Integer getIntegerValue();

    public void setSerObjValue(SerObj value);

    public SerObj getSerObjValue();

    public void setSerObj2Value(SerObj2 value);

    public SerObj2 getSerObj2Value();

    public void checkMySerObjStart();

    public void checkMySerObjEnd();

    public int getPassivateCount();

    public int getActivateCount();

    public void checkSessionContextStart();

    public void checkSessionContextEnd();

    public void checkENCEntryStart();

    public void checkENCEntryEnd();

    public void checkTimerStart();

    public void checkTimerEnd();

    public void finish();
}
