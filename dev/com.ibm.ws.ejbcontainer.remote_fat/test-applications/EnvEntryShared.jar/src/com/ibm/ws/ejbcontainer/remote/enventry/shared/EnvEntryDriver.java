/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.enventry.shared;

/**
 * Remote interface for the EnvEntryTest client container test
 * to drive work in the server process. <p>
 **/
public interface EnvEntryDriver {
    public enum EnvEntryEnum {
        EV1,
        EV2,
        EV3
    }

    // java.lang.Class variations
    public String verifyC1ResourceEnvRefClass();

    public void verifyC1EnvEntryClass();

    public void verifyC2EnvEntryNonExistingClass() throws Exception;

    public String verifyC3EnvEntryLookupClass();

    // Enum variations
    public String verifyE1ResourceEnvRefEnum();

    public void verifyE1EnvEntryEnum();

    public void verifyE2EnvEntryNonExistingEnumType() throws Exception;

    public void verifyE3EnvEntryNonExistingEnumValue() throws Exception;

    public void verifyE4EnvEntryExistingNonEnumNonClass() throws Exception;

    public String verifyE5EnvEntryLookupEnum();

    // Primitive variations
    public void verifyP1EnvEntryInteger();

    public void verifyP2EnvEntryInt();

    public void verifyP3EnvEntryEnumQual();

    public String bindObject(String name, Object value) throws Exception;
}