/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el.fat.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Logger;

import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.MinimumJavaLevel;

/**
 * testcases for 1.20 Lambda Expressions EL3.0 specification
 *
 */
@MinimumJavaLevel(javaLevel = 7)
public class ELLambdaExpHttpUnit extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(ELLambdaExpHttpUnit.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("elServer");

    // common method
    private WebResponse getWCResponse(String URLString) throws IOException, SAXException {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);
        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, "/TestEL3.0" + URLString));
        return wc.getResponse(request);
    }

    /**
     * x->x+1
     *
     * @throws Exception
     */
    @Test
    public void Test1_LambdaParam() throws Exception {
        LOG.info("Test1_LambdaParam");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=1");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test1: 3"));
    }

    /**
     * extra param should be rejected to x->x+1
     *
     * @throws Exception
     */
    @Test
    public void Test2_RejectExtraLambdaParam() throws Exception {
        LOG.info("Test2_RejectExtraLambdaParam");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=2");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test2: 3"));

    }

    /**
     * pass two param to x,y->x+y
     *
     * @throws Exception
     */
    @Test
    public void Test3_MultipleLambdaParams() throws Exception {
        LOG.info("Test3_MultipleLambdaParams");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=3");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test3: 7"));
    }

    /**
     * throw exception pass one param to x,y->x+y
     *
     * @throws Exception
     */
    @Test
    public void Test4_CatchExeptionOnLessParam() throws Exception {
        LOG.info("Test4_CatchExeptionOnLessParam");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=4");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test4: Only [1] arguments were provided for a lambda expression that requires at least [2]"));
    }

    /**
     * Assigned Lambda expression
     * incr = x->x+1")).invoke(10)
     *
     * @throws Exception
     */
    @Test
    public void Test5_AssignedLambdaExp() throws Exception {
        LOG.info("Test5_AssignedLambdaExp");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=5");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test5: 11"));
    }

    /**
     * return the evaluated value not an expression
     * ()->64
     *
     * @throws Exception
     */
    @Test
    public void Test6_NoParam() throws Exception {
        LOG.info("Test6_NoParam");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=6");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test6: 64"));
    }

    /**
     * The parenthesis is optional if and only if there is one parameter
     * "x->64"))).invoke(3)
     *
     * @throws Exception
     */
    @Test
    public void Test7_OptionalParenthesis() throws Exception {
        LOG.info("Test7_OptionalParenthesis");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=7");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test7: 64"));
    }

    /**
     * eval will return null as println returns void, but console should have Hello World.
     * ()->System.out.println(\"Hello World\")
     *
     * @throws Exception
     */
    @Test
    public void Test8_PrintFromBody() throws Exception {
        LOG.info("Test8_PrintFromBody");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=8");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test8: null"));
    }

    /**
     * parameters passed in ,invoked directly
     * ((x,y)->x+y)(3,4))
     *
     * @throws Exception
     */
    @Test
    public void Test9_InvokeFunctionImmediate() throws Exception {
        LOG.info("Test9_InvokeFunctionImmediate");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=9");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test9: 7"));
    }

    /**
     * parameters passed in ,invoked indirectly
     * v = (x,y)->x+y; v(3,4)
     *
     * @throws Exception
     */
    @Test
    public void Test10_InvokeFunctionIndirect() throws Exception {
        LOG.info("Test10_InvokeFunctionIndirect");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=10");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test10: 7"));
    }

    /**
     * parameters passed in ,invoked indirectly and seperate
     * v = (x,y)->x+y;
     * v(3,4)
     *
     * @throws Exception
     */
    @Test
    public void Test11_InvokeFunctionIndirectSeperate() throws Exception {
        LOG.info("Test11_InvokeFunctionIndirectSeperate");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=11");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test11: 7"));
    }

    /**
     *
     * fact = n -> n==0? 1: n*fact(n-1); fact(5)
     *
     * @throws Exception
     */
    @Test
    public void Test12_InvokeFunctionIndirect2() throws Exception {
        LOG.info("Test12_InvokeFunctionIndirect2");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=12");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test12: 120"));
    }

    /**
     *
     * employees.where(e->e.firstName == ‘Charlie’)
     *
     * @throws Exception
     */
    @Test
    public void Test13_PassedAsArgumentToMethod() throws Exception {
        LOG.info("Test13_PassedAsArgumentToMethod");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=13");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test13: NAME MATCHES: Charlie"));
    }

    /**
     *
     * (elp.eval("(a1, a2) -> a1 > a2"))).invoke(2,3,4)
     *
     * @throws Exception
     */
    @Test
    public void Test14_CompareParameters() throws Exception {
        LOG.info("Test14_CompareParameters");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=14");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test14: false"));
    }

    /**
     *
     * (firstStr, secondStr)-> Integer.compare(firstStr.length(),secondStr.length())"))).invoke("First","Second")
     * should cocerce to String
     *
     * @throws Exception
     */
    @Test
    public void Test15_ParameterCocerceToString() throws Exception {
        LOG.info("Test15_ParameterCocerceToString");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=15");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test15: -1"));
    }

    /**
     *
     * ( firstInt, secondInt)-> Integer.compare(firstInt,secondInt)"))).invoke(5,6)
     * should cocerce to int
     *
     * @throws Exception
     */
    @Test
    public void Test16_ParameterCocerceToInt() throws Exception {
        LOG.info("Test16_ParameterCocerceToInt");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=16");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test16: -1"));
    }

    /**
     * ${parseMe = x -> (y -> (Integer.parseInt(y)))(x) + x ; parseMe("1234")
     *
     * @throws Exception
     */
    @Test
    public void Test18_NestedFunction1() throws Exception {
        LOG.info("Test18_NestedFunction1");
        WebResponse response = getWCResponse("/TestVariousLambdaExpression?Test=18");

        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Test18: 2468"));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

}
