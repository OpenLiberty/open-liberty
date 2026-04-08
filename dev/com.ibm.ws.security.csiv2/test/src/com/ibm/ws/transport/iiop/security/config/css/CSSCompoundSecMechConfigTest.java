/*
 * Copyright 2014, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.security.config.css;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSSASMechConfig;

public class CSSCompoundSecMechConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private CSSASMechConfig as_mech;
    private CSSSASMechConfig sasMechConfig;
    private final byte[] contextData = "Some test context data".getBytes();

    private Codec codec;
    private TSSCompoundSecMechConfig tssCompoundSecMechConfig;
    private ClientRequestInfo ri;
    private TSSASMechConfig tssasMechConfig;
    private TSSSASMechConfig tssSASMechConfig;

    @Before
    public void setUp() throws Exception {
        as_mech = mockery.mock(CSSASMechConfig.class);
        mockery.mock(CSSSASMechConfig.class);
        tssasMechConfig = mockery.mock(TSSASMechConfig.class);
        sasMechConfig = new CSSSASMechConfig();
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        codec = mockery.mock(Codec.class);
        tssCompoundSecMechConfig = mockery.mock(TSSCompoundSecMechConfig.class);
        ri = mockery.mock(ClientRequestInfo.class);
        tssSASMechConfig = new TSSSASMechConfig();
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechConfig#generateServiceContext(org.omg.IOP.Codec)}.
     */
    @Test
    public void testGenerateServiceContext() throws Exception {
        createTSSCompoundSecMechConfigExpectations();
        createASMechConfigExpectations();
        createEncodingExpectations();

        CSSCompoundSecMechConfig cssCompoundSecMechConfig = new CSSCompoundSecMechConfig();
        cssCompoundSecMechConfig.setAs_mech(as_mech);
        cssCompoundSecMechConfig.setSas_mech(sasMechConfig);
        ServiceContext serviceContext = cssCompoundSecMechConfig.generateServiceContext(codec, tssCompoundSecMechConfig, ri);

        assertNotNull("There must be a service context.", serviceContext);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechConfig#generateServiceContext(org.omg.IOP.Codec)}.
     */
    @Test
    public void testGenerateServiceContextNoAuthAndITTAbsent() throws Exception {
        createTSSCompoundSecMechConfigWithAbsentExpectation();
        CSSCompoundSecMechConfig cssCompoundSecMechConfig = new CSSCompoundSecMechConfig();
        cssCompoundSecMechConfig.setAs_mech(new CSSNULLASMechConfig());
        cssCompoundSecMechConfig.setSas_mech(sasMechConfig);
        ServiceContext serviceContext = cssCompoundSecMechConfig.generateServiceContext(codec, tssCompoundSecMechConfig, ri);

        assertNull("There must not be a service context.", serviceContext);
    }

    private void createASMechConfigExpectations() {
        mockery.checking(new Expectations() {
            {
                one(as_mech).getSupports();
                will(returnValue(EstablishTrustInClient.value));
                one(as_mech).getRequires();
                will(returnValue((short) 0));
                // Contents do not matter since we are testing the collaboration.
                one(as_mech).encode(tssasMechConfig, sasMechConfig, ri, codec);
                will(returnValue(new byte[0]));
            }
        });
    }

    private void createEncodingExpectations() throws InvalidTypeForEncoding {
        mockery.checking(new Expectations() {
            {
                one(codec).encode_value(with(any(org.omg.CORBA.Any.class)));
                will(returnValue(contextData));
            }
        });
    }

    private void createTSSCompoundSecMechConfigExpectations() {
        mockery.checking(new Expectations() {
            {
                one(tssCompoundSecMechConfig).getAs_mech();
                will(returnValue(tssasMechConfig));
                one(tssCompoundSecMechConfig).getSas_mech();
                will(returnValue(tssSASMechConfig));
            }
        });
    }

    private void createTSSCompoundSecMechConfigWithAbsentExpectation() {
        mockery.checking(new Expectations() {
            {
                one(tssCompoundSecMechConfig).getSas_mech();
                will(returnValue(tssSASMechConfig));
            }
        });
    }

}
