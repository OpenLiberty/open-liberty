/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.ejb;

import static org.junit.Assert.assertEquals;
import static org.omg.CORBA.CompletionStatus._COMPLETED_NO;
import static org.omg.CORBA.CompletionStatus._COMPLETED_YES;

import javax.ejb.Stateless;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.portable.IDLEntity;

/**
 * Basic Stateless bean for testing IDLEntity parameters and return types.
 **/
@Stateless
public class IDLEntityRemoteBean implements IDLEntityRemote {
    @Override
    public void unique_IDLEntity_Method(CompletionStatus idlEntity) {
        assertEquals("CompletionStatus value not correct:", _COMPLETED_YES, idlEntity.value());
    }

    @Override
    public void overloaded_IDLEntity_Method(CompletionStatus idlEntity) {
        assertEquals("CompletionStatus value not correct:", _COMPLETED_YES, idlEntity.value());
    }

    @Override
    public void overloaded_IDLEntity_Method(int integer, CompletionStatus idlEntity) {
        assertEquals("CompletionStatus value not correct:", _COMPLETED_YES, idlEntity.value());
    }

    @Override
    public CompletionStatus overloaded_IDLEntity_Method(CompletionStatus idlEntity, CompletionStatus idlEntity2) {
        assertEquals("CompletionStatus value not correct:", _COMPLETED_YES, idlEntity.value());
        assertEquals("CompletionStatus value not correct:", _COMPLETED_NO, idlEntity2.value());
        return idlEntity;
    }

    @Override
    public void unique_IDLEntityArray_Method(CompletionStatus[] idlEntitys) {
        for (CompletionStatus idlEntity : idlEntitys) {
            assertEquals("CompletionStatus value not correct:", _COMPLETED_YES, idlEntity.value());
        }
    }

    @Override
    public void overloaded_IDLEntityArray_Method(CompletionStatus[] idlEntitys) {
        for (CompletionStatus idlEntity : idlEntitys) {
            assertEquals("CompletionStatus value not correct:", _COMPLETED_YES, idlEntity.value());
        }
    }

    @Override
    public void overloaded_IDLEntityArray_Method(int integer, CompletionStatus[] idlEntitys) {
        for (CompletionStatus idlEntity : idlEntitys) {
            assertEquals("CompletionStatus value not correct:", _COMPLETED_YES, idlEntity.value());
        }
    }

    @Override
    public CompletionStatus[] overloaded_IDLEntityArray_Method(CompletionStatus[] idlEntitys, IDLEntity idlEntity2) {
        for (CompletionStatus idlEntity : idlEntitys) {
            assertEquals("CompletionStatus value not correct:", _COMPLETED_YES, idlEntity.value());
        }
        assertEquals("CompletionStatus value not correct:", _COMPLETED_NO, ((CompletionStatus) idlEntity2).value());
        return idlEntitys;
    }
}