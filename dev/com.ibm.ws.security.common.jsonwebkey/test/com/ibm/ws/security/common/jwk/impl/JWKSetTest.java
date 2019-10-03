/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.common.jwk.interfaces.JWK;

public class JWKSetTest {

    private static final String JWK_RESOURCE_NAME = "jwk_test.json";

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final String kid = "test-key-id";
    private final String x5t = "U1dkoqHSjCUk2fdBHU-qSCpQXZc=";

    private JWKSet jwkSet;
    private JWK jwk;
    private URI uri;
    private String setId;
    private PublicKey publicKey;

    @Before
    public void setUp() throws Exception {
        publicKey = mockery.mock(PublicKey.class);
        createJwk(publicKey);
        URL jwkURL = getClass().getResource(JWK_RESOURCE_NAME);
        uri = jwkURL.toURI();
        jwkSet = new JWKSet();
    }

    private void createJwk(final PublicKey publicKey) {
        jwk = mockery.mock(JWK.class);
        mockery.checking(new Expectations() {
            {
                allowing(jwk).getPublicKey();
                will(returnValue(publicKey));
                allowing(jwk).getKeyID();
                will(returnValue(kid));
                allowing(jwk).getKeyX5t();
                will(returnValue(x5t));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testGetPublicKeyByUri() throws Exception {
        setId = uri.toString();
        jwkSet.add(setId, jwk);

        PublicKey publicKey = jwkSet.getPublicKeyBySetId(setId);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyByUriAndKid() throws Exception {
        setId = uri.toString();
        jwkSet.add(setId, jwk);

        PublicKey publicKey = jwkSet.getPublicKeyBySetIdAndKid(setId, kid);

        assertNotNull("There must a public key.", publicKey);
    }


    @Test
    public void testGetPublicKeyByUriAndx5t() throws Exception {
        setId = uri.toString();
        jwkSet.add(setId, jwk);

        PublicKey publicKey = jwkSet.getPublicKeyBySetIdAndx5t(setId, x5t);

        assertNotNull("There must a public key.", publicKey);
    }
    
    @Test
    public void testRemoveStaleEntries() throws Exception {
        ArrayList<JWK> al = new ArrayList<JWK>();
        al.add(new TestJWK());
        TestJWK j2 = new TestJWK();
        j2.created =  System.currentTimeMillis();
        al.add(j2);
        al.add(new TestJWK());
        j2 = new TestJWK();
        j2.created =  System.currentTimeMillis();
        al.add(j2);
        
        JWKSet testSet = new JWKSet();
        testSet.removeStaleEntries(al);
        assertTrue("Expected two entries to be removed", al.size() == 2);
        
    }
    
    class TestJWK implements JWK{
        long created = 0l;
        @Override
        public String getKeyID() {
            return null;
        }

        @Override
        public String getKeyX5t() {
            return null;
        }

        @Override
        public String getAlgorithm() {
            return null;
        }

        @Override
        public String getKeyUse() {            
            return null;
        }

        @Override
        public String getKeyType() {
            return null;
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public PrivateKey getPrivateKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public byte[] getSharedKey() {
            return null;
        }

        @Override
        public long getCreated() {
            return created;
        }

        @Override
        public void parse() {
        }

        @Override
        public void generateKey() {
        }

        @Override
        public JSONObject getJsonObject() {
            return null;
        }
    }

}
