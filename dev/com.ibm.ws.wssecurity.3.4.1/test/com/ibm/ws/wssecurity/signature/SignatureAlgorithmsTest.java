/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wssecurity.signature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;


/**
*
*/
public class SignatureAlgorithmsTest {

   private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.wssecurity.*=all");
   @Rule
   public TestRule managerRule = outputMgr;

   private static final Mockery mockery = new JUnit4Mockery() {
       {
           setImposteriser(ClassImposteriser.INSTANCE);
       }
   };

   private static SignatureAlgorithms AlgorithmSuite;

   private static final AssertionInfoMap aim = mockery.mock(AssertionInfoMap.class, "aim");
   private static final SoapMessage message = mockery.mock(SoapMessage.class, "message");
   private static final AlgorithmSuite algorithm = mockery.mock(AlgorithmSuite.class, "algorithm");
   //private static final AlgorithmSuiteType algorithmsuitetype = mockery.mock(AlgorithmSuiteType.class, "algorithmsuitetype");

   @SuppressWarnings("unchecked")
   private static final Collection<AssertionInfo> ais = mockery.mock(Collection.class, "ais");
   private static final AssertionInfo ai = mockery.mock(AssertionInfo.class, "ai");
   private static final AbstractBinding transport = mockery.mock(AbstractBinding.class, "transport");
   private static final String method = "sha1";

   @Rule
   public final TestName testName = new TestName();
   
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
       outputMgr.captureStreams();
       
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception {
       outputMgr.dumpStreams();
       outputMgr.restoreStreams();
   }
   

   @Before
   public void before() {
       System.out.println("Entering test: " + testName.getMethodName());
       AlgorithmSuite = new SignatureAlgorithms();
   }

   @After
   public void tearDown() throws Exception {
       System.out.println("Exiting test: " + testName.getMethodName());
       outputMgr.resetStreams();

       mockery.assertIsSatisfied();
   }

   @SuppressWarnings("static-access")
   @Test
   public void setAlgorithmTest() {

       final AlgorithmSuiteType type = new AlgorithmSuiteType(
                                                              "Basic256",
                                                              SPConstants.SHA1,
                                                              SPConstants.AES256,
                                                              SPConstants.KW_AES256,
                                                              SPConstants.KW_RSA_OAEP,
                                                              SPConstants.P_SHA1_L256,
                                                              SPConstants.P_SHA1_L192,
                                                              256, 192, 256,
                                                              256, 1024, 4096);
       mockery.checking(new Expectations() {
           {
               one(message).get(AssertionInfoMap.class);
               will(returnValue(aim));
               one(aim).get(SP12Constants.TRANSPORT_BINDING);
               will(returnValue(ais));
               one(ais).iterator();
               will(returnIterator(ai));
               one(ai).getAssertion();
               will(returnValue(transport));
               one(transport).getAlgorithmSuite();
               will(returnValue(algorithm));
               atMost(2).of(algorithm).getAlgorithmSuiteType();
               will(returnValue(type));
               //one(algorithmsuitetype).setAsymmetricSignature(method);

           }
       });

       try {
           AlgorithmSuite.setAlgorithm(message, method);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Unexpected exception was thrown: " + e.getMessage());
       }
   }

   @SuppressWarnings("static-access")
   @Test
   public void getAlgorithmSuiteTest_UsingSP12TransportBinding() {

       mockery.checking(new Expectations() {
           {
               one(aim).get(SP12Constants.TRANSPORT_BINDING);
               will(returnValue(ais));
               one(ais).iterator();
               will(returnIterator(ai));
               one(ai).getAssertion();
               will(returnValue(transport));
               one(transport).getAlgorithmSuite();
               will(returnValue(algorithm));
           }
       });
       try {

           AlgorithmSuite alg = AlgorithmSuite.getAlgorithmSuite(aim);
           assertEquals(alg, algorithm);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Unexpected exception was thrown: " + e.getMessage());
       }
   }
   
   @SuppressWarnings("static-access")
   @Test
   public void getAlgorithmSuiteTest_UsingSP11TransportBinding() {

       mockery.checking(new Expectations() {
           {
               one(aim).get(SP12Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.TRANSPORT_BINDING);
               will(returnValue(ais));
               one(ais).iterator();
               will(returnIterator(ai));
               one(ai).getAssertion();
               will(returnValue(transport));
               one(transport).getAlgorithmSuite();
               will(returnValue(algorithm));
           }
       });
       try {

           AlgorithmSuite alg = AlgorithmSuite.getAlgorithmSuite(aim);
           assertEquals(alg, algorithm);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Unexpected exception was thrown: " + e.getMessage());
       }
   }
   
   @SuppressWarnings("static-access")
   @Test
   public void getAlgorithmSuiteTest_UsingSP12AsymmetricBinding() {

       mockery.checking(new Expectations() {
           {
               one(aim).get(SP12Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP12Constants.ASYMMETRIC_BINDING);
               will(returnValue(ais));
               one(ais).iterator();
               will(returnIterator(ai));
               one(ai).getAssertion();
               will(returnValue(transport));
               one(transport).getAlgorithmSuite();
               will(returnValue(algorithm));
           }
       });
       try {

           AlgorithmSuite alg = AlgorithmSuite.getAlgorithmSuite(aim);
           assertEquals(alg, algorithm);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Unexpected exception was thrown: " + e.getMessage());
       }
   }
   
   @SuppressWarnings("static-access")
   @Test
   public void getAlgorithmSuiteTest_UsingSP11AsymmetricBinding() {

       mockery.checking(new Expectations() {
           {
               one(aim).get(SP12Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP12Constants.ASYMMETRIC_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.ASYMMETRIC_BINDING);
               will(returnValue(ais));
               one(ais).iterator();
               will(returnIterator(ai));
               one(ai).getAssertion();
               will(returnValue(transport));
               one(transport).getAlgorithmSuite();
               will(returnValue(algorithm));
           }
       });
       try {

           AlgorithmSuite alg = AlgorithmSuite.getAlgorithmSuite(aim);
           assertEquals(alg, algorithm);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Unexpected exception was thrown: " + e.getMessage());
       }
   }

   @SuppressWarnings("static-access")
   @Test
   public void getAlgorithmSuiteTest_UsingSP12SymmetricBinding() {

       mockery.checking(new Expectations() {
           {

               one(aim).get(SP12Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP12Constants.ASYMMETRIC_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.ASYMMETRIC_BINDING);
               will(returnValue(null));
               one(aim).get(SP12Constants.SYMMETRIC_BINDING);
               will(returnValue(ais));
               one(ais).iterator();
               will(returnIterator(ai));
               one(ai).getAssertion();
               will(returnValue(transport));
               one(transport).getAlgorithmSuite();
               will(returnValue(algorithm));
           }
       });
       try {

           AlgorithmSuite alg = AlgorithmSuite.getAlgorithmSuite(aim);
           assertEquals(alg, algorithm);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Unexpected exception was thrown: " + e.getMessage());
       }
   }
   
   @SuppressWarnings("static-access")
   @Test
   public void getAlgorithmSuiteTest_UsingSP11SymmetricBinding() {

       mockery.checking(new Expectations() {
           {

               one(aim).get(SP12Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.TRANSPORT_BINDING);
               will(returnValue(null));
               one(aim).get(SP12Constants.ASYMMETRIC_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.ASYMMETRIC_BINDING);
               will(returnValue(null));
               one(aim).get(SP12Constants.SYMMETRIC_BINDING);
               will(returnValue(null));
               one(aim).get(SP11Constants.SYMMETRIC_BINDING);
               will(returnValue(ais));
               one(ais).iterator();
               will(returnIterator(ai));
               one(ai).getAssertion();
               will(returnValue(transport));
               one(transport).getAlgorithmSuite();
               will(returnValue(algorithm));
           }
       });
       try {

           AlgorithmSuite alg = AlgorithmSuite.getAlgorithmSuite(aim);
           assertEquals(alg, algorithm);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Unexpected exception was thrown: " + e.getMessage());
       }
   }
}



