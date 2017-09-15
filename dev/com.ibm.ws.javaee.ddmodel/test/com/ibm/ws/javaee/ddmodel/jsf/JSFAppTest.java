/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.jsf;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.ddmodel.DDParser;

/**
 * This test will check if parsing faces-config.xml is correct and validate them.
 * 
 */
public class JSFAppTest extends JSFAppTestBase {

    @Test
    public void testJSF22() throws Exception {
        parse(jsf22() + "</faces-config>");
    }

    @Test
    public void testJSF22_Contract1() throws Exception {
        parse(jsf22() +
              "<application>" +
              "<resource-library-contracts>" +
              "<contract-mapping>" +
              "<url-pattern>/user/*</url-pattern>" +
              "<contracts>testContract</contracts>" +
              "</contract-mapping>" +
              "</resource-library-contracts>" +
              "</application>" +
              "</faces-config>");

    }

    @Test
    public void testJSF22_Contract2() throws Exception {
        parse(jsf22() +
              "<application>" +
              "<resource-library-contracts>" +
              "<contract-mapping>" +
              "<url-pattern>/user/*</url-pattern>" +
              "<url-pattern>/user1/*</url-pattern>" +
              "<contracts>testContract</contracts>" +
              "</contract-mapping>" +
              "</resource-library-contracts>" +
              "</application>" +
              "</faces-config>");

    }

    @Test
    public void testJSF22_Contract3() throws Exception {
        parse(jsf22() +
              "<application>" +
              "<resource-library-contracts>" +
              "<contract-mapping>" +
              "<url-pattern>/user/*</url-pattern>" +
              "<url-pattern>/user1/*</url-pattern>" +
              "<contracts>testContract</contracts>" +
              "<contracts>testContract2</contracts>" +
              "</contract-mapping>" +
              "</resource-library-contracts>" +
              "</application>" +
              "</faces-config>");

    }

    @Test
    public void testJSF22_Contract4() throws Exception {
        parse(jsf22() +
              "<application>" +
              "<resource-library-contracts>" +
              "<contract-mapping>" +
              "<url-pattern>/user/*</url-pattern>" +
              "<url-pattern>/user1/*</url-pattern>" +
              "<contracts>testContract</contracts>" +
              "<contracts>testContract2</contracts>" +
              "</contract-mapping>" +
              "</resource-library-contracts>" +
              "</application>" +
              "</faces-config>");

    }

    @Test
    public void testJSF22_Contract5() throws Exception {
        parse(jsf22() +
              "<application>" +
              "<resource-library-contracts>" +
              "<contract-mapping>" +
              "<url-pattern>/user/*</url-pattern>" +
              "<url-pattern>/user1/*</url-pattern>" +
              "<contracts>testContract</contracts>" +
              "<contracts>testContract2</contracts>" +
              "</contract-mapping>" +
              "<contract-mapping>" +
              "<url-pattern>/user2/*</url-pattern>" +
              "<url-pattern>/user4/*</url-pattern>" +
              "<contracts>testContract4</contracts>" +
              "<contracts>testContract5</contracts>" +
              "</contract-mapping>" +
              "</resource-library-contracts>" +
              "</application>" +
              "</faces-config>");

    }

    /*
     * This test should fail as <contract-map> is not valid
     * Assert on exception
     */
    @Test
    public void testJSF22_ContractFail() throws Exception {
        try {
            parse(jsf22() +
                  "<application>" +
                  "<resource-library-contracts>" +
                  "<contract-map>" +
                  "<url-pattern>/user/*</url-pattern>" +
                  "<url-pattern>/user2/*</url-pattern>" +
                  "<contracts>testContract</contracts>" +
                  "<contracts>testContract2</contracts>" +
                  "</contract-map>" +
                  "</resource-library-contracts>" +
                  "</application>" +
                  "</faces-config>");

        } catch (DDParser.ParseException ex) {
            if (ex.getMessage().contains("unexpected.child.element")) {
                Assert.assertNotNull(ex);
            }
        }
    }

    // The following snippet examples for Flow
    /*
     * This test for
     * <flow-definition>
     * <start-node>
     * <view>
     * <vdl-document>
     * <flow-return>
     * <from-outcome>
     */
    @Test
    public void testJSF22_Flow1() throws Exception {
        parse(jsf22() +
              "<flow-definition id=\"simpleFacesConfig\">" +
              "<start-node>simple-facesConfig</start-node>" +
              "<view id=\"simple-facesConfig\">" +
              "<vdl-document>/simpleFacesConfig/simple-facesConfig.xhtml</vdl-document>" +
              "</view>" +
              "<view id=\"simple-facesConfig\">" +
              "<vdl-document>/simpleFacesConfig/simple-facesConfig2.xhtml</vdl-document>" +
              "</view>" +
              "<flow-return id=\"goIndex-FacesConfig\">" +
              "<from-outcome>/JSF22Flows_index</from-outcome>" +
              "</flow-return>" +
              "<flow-return id=\"goReturn-FacesConfig\">" +
              "<from-outcome>/JSF22Flows_return</from-outcome>" +
              "</flow-return>" +
              "</flow-definition>" +
              "</faces-config>");

    }

    /*
     * This test for
     * <flow-definition>
     * <flow-call>
     * <flow-reference>
     * <flow-id>
     * <navigation-rule>
     * <from-view-id>
     * <navigation-case>
     * <from-outcome>
     * <to-view-id>
     * <from-view-id>
     * <redirect>
     * <to-flow-document-id>
     */
    @Test
    public void testJSF22_Flow2() throws Exception {
        parse(jsf22() +
              "<flow-definition id=\"flow1\">" +
              "<flow-call id=\"flow1\">" +
              "<flow-reference>" +
              "<flow-id>flow1a</flow-id>" +
              "</flow-reference>" +
              "</flow-call>" +
              "<navigation-rule>" +
              "<from-view-id>*</from-view-id>" +
              "<navigation-case>" +
              "<from-outcome>exit</from-outcome>" +
              "<to-view-id>/main.xhtml</to-view-id>" +
              "</navigation-case>" +
              "</navigation-rule>" +
              "<navigation-rule>" +
              "<from-view-id>*</from-view-id>" +
              "<navigation-case>" +
              "<from-outcome>exit</from-outcome>" +
              "<to-view-id>/main.xhtml</to-view-id>" +
              "<redirect />" +
              "<to-flow-document-id/>" +
              "</navigation-case>" +
              "</navigation-rule>" +
              "</flow-definition>" +
              "</faces-config>");

    }

    /*
     * This test for
     * <flow-definition>
     * <flow-return>
     * <from-outcome>
     * <inbound-parameter>
     * <outbound-parameter>
     */
    @Test
    public void testJSF22_Flow3() throws Exception {
        parse(jsf22() +
              "<flow-definition id=\"flow2\">" +
              "<flow-return id=\"taskFlowReturn1\">" +
              "<from-outcome>#{bean.returnValue}</from-outcome>" +
              "</flow-return>" +
              "<inbound-parameter>" +
              "<name>param1FromFlow1</name>" +
              "<value>#{scope.param1Value}</value>" +
              "</inbound-parameter>" +
              "<inbound-parameter>" +
              "<name>param2FromFlow1</name>" +
              "<value>#{scope.param2Value}</value>" +
              "</inbound-parameter>" +
              "<flow-call id=\"callFlow1\">" +
              "<flow-reference>" +
              "<flow-id>flow1</flow-id>" +
              "</flow-reference>" +
              "<outbound-parameter>" +
              "<name>param1FromFlow2</name>" +
              "<value>param1 flow2 value</value>" +
              "</outbound-parameter>" +
              "<outbound-parameter>" +
              "<name>param2FromFlow2</name>" +
              "<value>param2 flow2 value</value>" +
              "</outbound-parameter>" +
              "</flow-call>" +
              "</flow-definition>" +
              "</faces-config>");
    }

    /*
     * Can have multiple <protected-views> and each protected-views can have multiple <url-pattern>
     */
    @Test
    public void testJSF22_ProtectedView1() throws Exception {
        parse(jsf22() +
              "<protected-views>" +
              "<url-pattern>/user/*</url-pattern>" +
              "<url-pattern>/user2/*</url-pattern>" +
              "</protected-views>" +
              "<protected-views>" +
              "<url-pattern>/user3/*</url-pattern>" +
              "<url-pattern>/user4/*</url-pattern>" +
              "</protected-views>" +
              "</faces-config>");
    }

    /*
     * test FlashFactory
     */
    @Test
    public void testJSF22_FlashFactory1() throws Exception {
        parse(jsf22() +
              "<factory>" +
              "<flash-factory>com.ibm.blah.TestFlashFactoryImpl</flash-factory>" +
              "</factory>" +
              "</faces-config>");
    }

    /*
     * test flow-handler-factory
     */
    @Test
    public void testJSF22_FlowHandlerFactory1() throws Exception {
        parse(jsf22() +
              "<factory>" +
              "<flow-handler-factory>com.ibm.blah.TestFlowHandlerFactoryImpl</flow-handler-factory>" +
              "</factory>" +
              "</faces-config>");
    }
}
