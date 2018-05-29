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
package com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.omg.CORBA.CompletionStatus._COMPLETED_NO;
import static org.omg.CORBA.CompletionStatus._COMPLETED_YES;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.portable.IDLEntity;

import com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.ejb.IDLEntityRMIC;
import com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.ejb.IDLEntityRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> IDLEntityStubTest .
 *
 * <dt><b>Test Author:</b> Tracy Burroughs <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support of aggregate local bean references for JCDI. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li> testIDLEntityParametersAndReturnType
 * - verifies that an implementation of IDLEntity may be passed as
 * parameters, including overloaded methods, and returned from
 * remote methods.
 * <li> testIDLEntityParametersAndReturnTypeWithRMIC
 * - verifies that an implementation of IDLEntity may be passed as
 * parameters, including overloaded methods, and returned from
 * remote methods when the stub is generated with RMIC.
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/IDLEntityStubServlet")
public class IDLEntityStubServlet extends FATServlet {
    /**
     * Tests that an implementation of IDLEntity may be passed as parameters,
     * including overloaded methods, and returned from remote methods. <p>
     *
     * Arrays of IDLEntity will also be tested. <p>
     *
     * Both the Stub and Tie will be JITDeployed. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testIDLEntityParametersAndReturnType() throws Exception {
        IDLEntityRemote bean = (IDLEntityRemote) PortableRemoteObject.narrow(new InitialContext().lookup("java:app/JitDeployEJB/IDLEntityRemoteBean"), IDLEntityRemote.class);

        CompletionStatus idlEntity = CompletionStatus.from_int(_COMPLETED_YES);
        CompletionStatus idlEntity2 = CompletionStatus.from_int(_COMPLETED_NO);

        bean.unique_IDLEntity_Method(idlEntity);
        bean.overloaded_IDLEntity_Method(idlEntity);
        bean.overloaded_IDLEntity_Method(5, idlEntity);
        CompletionStatus rtnEntity = bean.overloaded_IDLEntity_Method(idlEntity, idlEntity2);

        assertEquals("Returned IDLEntity not corect:", idlEntity.value(), rtnEntity.value());

        CompletionStatus[] idlEntitys = new CompletionStatus[2];
        idlEntitys[0] = CompletionStatus.from_int(_COMPLETED_YES);
        idlEntitys[1] = CompletionStatus.from_int(_COMPLETED_YES);

        bean.unique_IDLEntityArray_Method(idlEntitys);
        bean.overloaded_IDLEntityArray_Method(idlEntitys);
        bean.overloaded_IDLEntityArray_Method(5, idlEntitys);
        IDLEntity[] rtnEntitys = bean.overloaded_IDLEntityArray_Method(idlEntitys, idlEntity2);

        assertNotNull("Returned IDLEntityArray is null:", rtnEntitys);
    }

    /**
     * Tests that an implementation of IDLEntity may be passed as parameters,
     * including overloaded methods, and returned from remote methods when the
     * stub is generated with RMIC. <p>
     *
     * Arrays of IDLEntity will also be tested. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testIDLEntityParametersAndReturnTypeWithRMIC() throws Exception {
        IDLEntityRMIC bean = (IDLEntityRMIC) PortableRemoteObject.narrow(new InitialContext().lookup("java:app/JitDeployEJB/IDLEntityRMICBean"), IDLEntityRMIC.class);

        CompletionStatus idlEntity = CompletionStatus.from_int(_COMPLETED_YES);
        CompletionStatus idlEntity2 = CompletionStatus.from_int(_COMPLETED_NO);

        bean.unique_IDLEntity_Method(idlEntity);
        bean.overloaded_IDLEntity_Method(idlEntity);
        bean.overloaded_IDLEntity_Method(5, idlEntity);
        CompletionStatus rtnEntity = bean.overloaded_IDLEntity_Method(idlEntity, idlEntity2);

        assertEquals("Returned IDLEntity not corect:", idlEntity.value(), rtnEntity.value());

        CompletionStatus[] idlEntitys = new CompletionStatus[2];
        idlEntitys[0] = CompletionStatus.from_int(_COMPLETED_YES);
        idlEntitys[1] = CompletionStatus.from_int(_COMPLETED_YES);

        bean.unique_IDLEntityArray_Method(idlEntitys);
        bean.overloaded_IDLEntityArray_Method(idlEntitys);
        bean.overloaded_IDLEntityArray_Method(5, idlEntitys);
        IDLEntity[] rtnEntitys = bean.overloaded_IDLEntityArray_Method(idlEntitys, idlEntity2);

        assertNotNull("Returned IDLEntityArray is null:", rtnEntitys);
    }
}