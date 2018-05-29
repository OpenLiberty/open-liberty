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

import javax.ejb.Remote;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.portable.IDLEntity;

/**
 * Basic Stateless bean remote interface for testing IDLEntity parameters and
 * return types.
 **/
@Remote
public interface IDLEntityRemote {
    public void unique_IDLEntity_Method(CompletionStatus idlEntity);

    public void overloaded_IDLEntity_Method(CompletionStatus idlEntity);

    public void overloaded_IDLEntity_Method(int integer, CompletionStatus idlEntity);

    public CompletionStatus overloaded_IDLEntity_Method(CompletionStatus idlEntity, CompletionStatus idlEntity2);

    public void unique_IDLEntityArray_Method(CompletionStatus[] idlEntitys);

    public void overloaded_IDLEntityArray_Method(CompletionStatus[] idlEntitys);

    public void overloaded_IDLEntityArray_Method(int integer, CompletionStatus[] idlEntitys);

    public CompletionStatus[] overloaded_IDLEntityArray_Method(CompletionStatus[] idlEntitys, IDLEntity idlEntity);
}