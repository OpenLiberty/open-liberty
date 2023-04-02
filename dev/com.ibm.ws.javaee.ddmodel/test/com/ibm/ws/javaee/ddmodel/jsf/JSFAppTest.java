/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.jsf;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.dd.jsf.FacesConfigManagedBean;

public class JSFAppTest extends JSFAppTestBase {
    @Test
    public void testGetVersion() throws Exception {
        for ( int schemaVersion : FacesConfig.VERSIONS ) {
            String schemaVersionStr = getDottedVersionText(schemaVersion);
            
            for ( int maxSchemaVersion : FacesConfig.VERSIONS ) {
                // The Faces config parser uses a maximum schema
                // version of "max(version, FacesConfig.VERSION_2_1)".
                // Adjust the message expectations accordingly.
                //
                // See:
                // com.ibm.ws.javaee.ddmodel.ejb.FacesConfigDDParser

                int effectiveMax;
                if ( maxSchemaVersion < FacesConfig.VERSION_2_1 ) {
                    effectiveMax = FacesConfig.VERSION_2_1;
                } else {
                    effectiveMax = maxSchemaVersion;
                }

                String altMessage;
                String[] messages;
                if ( schemaVersion > effectiveMax ) {
                    altMessage = UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE;
                    messages = UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES;
                } else {
                    altMessage = null;
                    messages = null;
                }

                FacesConfig facesConfig = parse(
                    jsf(schemaVersion, ""),
                    maxSchemaVersion,
                    altMessage, messages );

                if ( schemaVersion <= effectiveMax ) {
                    Assert.assertEquals( schemaVersionStr, facesConfig.getVersion() );
                }
            }
        }
    }

    //

    @Test
    public void testJSF22_Contract1() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
                    "<application>" +
                        "<resource-library-contracts>" +
                            "<contract-mapping>" +
                                "<url-pattern>/user/*</url-pattern>" +
                                "<contracts>testContract</contracts>" +
                            "</contract-mapping>" +
                        "</resource-library-contracts>" +
                    "</application>"),
                FacesConfig.VERSION_2_2 );
    }

    @Test
    public void testJSF22_Contract2() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
                    "<application>" +
                        "<resource-library-contracts>" +
                            "<contract-mapping>" +
                                "<url-pattern>/user/*</url-pattern>" +
                                "<url-pattern>/user1/*</url-pattern>" +
                                "<contracts>testContract</contracts>" +
                            "</contract-mapping>" +
                        "</resource-library-contracts>" +
                    "</application>"),
                FacesConfig.VERSION_2_2 );
    }

    @Test
    public void testJSF22_Contract3() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
                    "<application>" +
                        "<resource-library-contracts>" +
                            "<contract-mapping>" +
                                "<url-pattern>/user/*</url-pattern>" +
                                "<url-pattern>/user1/*</url-pattern>" +
                                "<contracts>testContract</contracts>" +
                                "<contracts>testContract2</contracts>" +
                            "</contract-mapping>" +
                        "</resource-library-contracts>" +
                    "</application>"),
                FacesConfig.VERSION_2_2 );
    }

    @Test
    public void testJSF22_Contract4() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
                    "<application>" +
                        "<resource-library-contracts>" +
                            "<contract-mapping>" +
                                "<url-pattern>/user/*</url-pattern>" +
                                "<url-pattern>/user1/*</url-pattern>" +
                                "<contracts>testContract</contracts>" +
                                "<contracts>testContract2</contracts>" +
                            "</contract-mapping>" +
                        "</resource-library-contracts>" +
                    "</application>"),
               FacesConfig.VERSION_2_2 );
    }

    @Test
    public void testJSF22_Contract5() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
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
                    "</application>"),
               FacesConfig.VERSION_2_2 );

    }

    /*
     * This test should fail as <contract-map> is not valid.
     */
    @Test
    public void testJSF22_ContractFail() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
                    "<application>" +
                        "<resource-library-contracts>" +
                            "<contract-map>" +
                                "<url-pattern>/user/*</url-pattern>" +
                                "<url-pattern>/user2/*</url-pattern>" +
                                "<contracts>testContract</contracts>" +
                                "<contracts>testContract2</contracts>" +
                            "</contract-map>" +
                        "</resource-library-contracts>" +
                    "</application>"),
               FacesConfig.VERSION_2_2,
               "CWWKC2259E", "unexpected.child.element" );
    }

    /*
     * <flow-definition>
     * <start-node>
     * <view>
     * <vdl-document>
     * <flow-return>
     * <from-outcome>
     */
    @Test
    public void testJSF22_Flow1() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
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
                    "</flow-definition>"),
               FacesConfig.VERSION_2_2 );

    }

    /*
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
        parse( jsf( FacesConfig.VERSION_2_2,
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
                    "</flow-definition>"),
               FacesConfig.VERSION_2_2 );
    }

    /*
     * <flow-definition>
     * <flow-return>
     * <from-outcome>
     * <inbound-parameter>
     * <outbound-parameter>
     */
    @Test
    public void testJSF22_Flow3() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
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
                "</flow-definition>"),
               FacesConfig.VERSION_2_2 );
    }

    /*
     * Can have multiple <protected-views> and
     * each protected-views can have multiple <url-pattern>.
     */
    @Test
    public void testJSF22_ProtectedView1() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
                    "<protected-views>" +
                        "<url-pattern>/user/*</url-pattern>" +
                        "<url-pattern>/user2/*</url-pattern>" +
                    "</protected-views>" +
                    "<protected-views>" +
                        "<url-pattern>/user3/*</url-pattern>" +
                        "<url-pattern>/user4/*</url-pattern>" +
                    "</protected-views>"),
               FacesConfig.VERSION_2_2 );
    }

    @Test
    public void testJSF22_FlashFactory1() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
                    "<factory>" +
                        "<flash-factory>com.ibm.blah.TestFlashFactoryImpl</flash-factory>" +
                    "</factory>"),
                FacesConfig.VERSION_2_2 );
    }

    @Test
    public void testJSF22_FlowHandlerFactory1() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_2,
                    "<factory>" +
                        "<flow-handler-factory>com.ibm.blah.TestFlowHandlerFactoryImpl</flow-handler-factory>" +
                    "</factory>"),
                FacesConfig.VERSION_2_2 );
    }

    @Test
    public void testJSF23_ClientWindowFactory() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_3,
                    "<factory>" +
                        "<client-window-factory>com.ibm.blah.TestClientWindowFactoryImpl</client-window-factory>" +
                    "</factory>"),
               FacesConfig.VERSION_2_3 );
    }

    @Test
    public void testJSF23_SearchExpressionContextKitFactory() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_3,
                    "<factory>" +
                        "<search-expression-context-factory>com.ibm.blah.TestClientWindowFactoryImpl</search-expression-context-factory>" +
                    "</factory>"),
                FacesConfig.VERSION_2_3 );
    }

    @Test
    public void testJSF23_SearchExpressionHandler() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_3,
                    "<application>" +
                        "<search-expression-handler>com.ibm.blah.SearchExpressionHandlerImpl</search-expression-handler>" +
                    "</application>"),
               FacesConfig.VERSION_2_3 );
    }

    @Test
    public void testJSF23_SearchKeywordResolver() throws Exception {
        parse( jsf( FacesConfig.VERSION_2_3,
                    "<application>" +
                        "<search-keyword-resolver>com.ibm.blah.SearchKeywordResolverImpl</search-keyword-resolver>" +
                    "</application>"),
               FacesConfig.VERSION_2_3 );
    }

    // TODO: JSF 3.0 / Jakarta EE 9 cases
    //

    // Managed beans tests:
    //
    // Support for 'managed-bean' elements was removed by EE 10 / Faces 4.0:

    public static final String MANAGED_BEAN_TEXT =
        "<managed-bean>" +
            "<managed-bean-name>TestBean</managed-bean-name>" +
            "<managed-bean-class>com.Test.TestBean</managed-bean-class>" +
            "<managed-bean-scope>request</managed-bean-scope>" +
        "</managed-bean>";

    // The 'managed-bean' element should be parsed at faces 3.0:
    
    @Test
    public void testFaces30_Managedbean() throws Exception {    
        FacesConfig facesConfig = parse( jsf(FacesConfig.VERSION_3_0, MANAGED_BEAN_TEXT), FacesConfig.VERSION_3_0 );
        List<FacesConfigManagedBean> mbeans = facesConfig.getManagedBeans();

        Assert.assertNotNull("Failed to parse any managed beans", mbeans);
        Assert.assertEquals("Expected a single parsed managed bean", 1, mbeans.size());
    }
    
    // The 'managed-bean' element should *not* be parsed at faces 4_0:
    
    // CWWKC2259E: Unexpected child element managed-bean of parent element faces-config
    // encountered in the myWar.war : WEB-INF/faces-config.xml deployment descriptor on line 7.
    
    @Test
    public void testFaces40_Managedbean() throws Exception {    
        parse( jsf(FacesConfig.VERSION_4_0, MANAGED_BEAN_TEXT), FacesConfig.VERSION_4_0,
               XML_ERROR_ALT_UNEXPECTED_CHILD, XML_ERROR_UNEXPECTED_CHILD);
    }    
}
