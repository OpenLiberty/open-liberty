/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

/**
 *
 */
public class DefaultJsonWebTokenImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.mp.jwt.*=all");

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeTest() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl#clone()}.
     */
    @Test
    public void testSerializeAndDeserialize() {
        try {
            //decoded payload claims : {"token_type":"Bearer","aud":"aud1","sub":"testuser","upn":"testuser","groups":["group1-abc","group2-def","testuser-group"],"iss":"https://9.24.8.103:8947/jwt/jwkEnabled","exp":1504212390,"iat":1504205190}
            String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
            String type = "Bearer";
            String name = "testuser";

            DefaultJsonWebTokenImpl jwttoken = new DefaultJsonWebTokenImpl(jwt, type, name);

            String sub = jwttoken.getSubject();
            String issuer = jwttoken.getIssuer();
            long exp = jwttoken.getExpirationTime();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(jwttoken);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            DefaultJsonWebTokenImpl copiedtoken = (DefaultJsonWebTokenImpl) in.readObject();

            String subfromcopy = copiedtoken.getSubject();
            Assert.assertEquals("subjects should be same", sub, subfromcopy);

        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        } catch (ClassNotFoundException e) {

        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl#claim(java.lang.String)}.
     */
    @Test
    public void testClaim() {
        String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
        String type = "Bearer";
        String name = "testuser";

        DefaultJsonWebTokenImpl jwttoken = new DefaultJsonWebTokenImpl(jwt, type, name);

        Optional<String> someclaim = jwttoken.claim("someclaim");
        boolean empty = someclaim.isPresent();
        Assert.assertFalse("someclaim should not be there in the token", someclaim.isPresent());
        //        String claim2 = "someotherclaim";
        //        Assert.assert
        //        Assert.assertEquals("subjects should be same", someclaim, claim2);
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl#getClaim(java.lang.String)}.
     */
    @Test
    public void testGetClaim() {
        String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
        String type = "Bearer";
        String name = "testuser";

        String upn = null;
        DefaultJsonWebTokenImpl jwttoken = new DefaultJsonWebTokenImpl(jwt, type, name);
        if (jwttoken.claim("upn").isPresent()) {
            upn = (String) jwttoken.getClaim("upn");
        }

        Assert.assertEquals("upn claim should be testuser", "testuser", upn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl#getClaimNames()}.
     */
    @Test
    public void testGetClaimNames() {
        String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
        String type = "Bearer";
        String name = "testuser";

        Set<String> originalclaims = new HashSet<String>(Arrays.asList("token_type", "aud", "sub", "upn", "groups", "iss", "exp", "iat", "raw_token"));

        DefaultJsonWebTokenImpl jwttoken = new DefaultJsonWebTokenImpl(jwt, type, name);
        Set<String> claims = jwttoken.getClaimNames();
        claims.removeAll(originalclaims);

        Assert.assertTrue("No extra claims should exist in the token other than the ones listed", claims.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl#getName()}.
     */
    @Test
    public void testGetName() {
        String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
        String type = "Bearer";
        String name = "user123";

        DefaultJsonWebTokenImpl jwttoken = new DefaultJsonWebTokenImpl(jwt, type, name);
        Assert.assertEquals("user123 should be the principal", "user123", jwttoken.getName());
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl#getAudience()}.
     */
    @Test
    public void testGetAudience() {
        String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
        String type = "Bearer";
        String name = "testuser";

        Set<String> originalaudiences = new HashSet<String>(Arrays.asList("aud1"));

        DefaultJsonWebTokenImpl jwttoken = new DefaultJsonWebTokenImpl(jwt, type, name);
        Set<String> audiences = jwttoken.getAudience();
        audiences.removeAll(originalaudiences);

        Assert.assertTrue("No extra audiences should exist in the token other than the ones listed", audiences.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl#getGroups()}.
     */
    @Test
    public void testGetGroups() {
        String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
        String type = "Bearer";
        String name = "user123";

        Set<String> originalgroups = new HashSet<String>(Arrays.asList("group1-abc", "group2-def", "testuser-group"));

        DefaultJsonWebTokenImpl jwttoken = new DefaultJsonWebTokenImpl(jwt, type, name);
        Set<String> groups = jwttoken.getGroups();
        groups.removeAll(originalgroups);

        Assert.assertTrue("No extra groups should exist in the token other than the ones listed", groups.isEmpty());
    }

}
