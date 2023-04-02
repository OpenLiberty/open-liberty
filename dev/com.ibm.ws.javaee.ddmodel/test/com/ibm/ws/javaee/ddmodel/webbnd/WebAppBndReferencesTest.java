/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.webbnd;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.commonbnd.EJBRef;
import com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef;
import com.ibm.ws.javaee.dd.commonbnd.Property;
import com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef;
import com.ibm.ws.javaee.dd.commonbnd.ResourceRef;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;

public class WebAppBndReferencesTest extends WebAppBndTestBase {
    @Test
    public void testEJBRef() throws Exception {
        WebBnd bnd = parseWebBndXML(
                webBndXML12(
                    "<ejb-ref name=\"ref0\" binding-name=\"bn0\"/>" +
                    "<ejb-ref name=\"ref1\" binding-name=\"bn1\"/>"));
        List<EJBRef> refs = bnd.getEJBRefs();
        Assert.assertEquals(refs.toString(), 2, refs.size());
        Assert.assertEquals("ref0", refs.get(0).getName());
        Assert.assertEquals("bn0", refs.get(0).getBindingName());
        Assert.assertEquals("ref1", refs.get(1).getName());
        Assert.assertEquals("bn1", refs.get(1).getBindingName());
    }

    @Test
    public void testEJBRefXMI() throws Exception {
        WebApp webApp = parseWebApp(
                webApp(WebApp.VERSION_2_4,
                       "<ejb-ref id=\"id0\">" +
                           "<ejb-ref-name>ref0</ejb-ref-name>" +
                       "</ejb-ref>" +
                       "<ejb-ref id=\"id1\">" +
                       "    <ejb-ref-name>ref1</ejb-ref-name>" +
                        "</ejb-ref>"));

        WebBnd bnd = parseWebBndXMI(
                webBndXMI20("",
                        "<ejbRefBindings jndiName=\"bn0\">" +
                            "<bindingEjbRef href=\"WEB-INF/web.xml#id0\"/>" +
                        "</ejbRefBindings>" +
                        "<ejbRefBindings jndiName=\"bn1\">" +
                            "<bindingEjbRef href=\"WEB-INF/web.xml#id1\"/>" +
                        "</ejbRefBindings>"),
                webApp);

        List<EJBRef> refs = bnd.getEJBRefs();
        Assert.assertEquals(refs.toString(), 2, refs.size());
        Assert.assertEquals("ref0", refs.get(0).getName());
        Assert.assertEquals("bn0", refs.get(0).getBindingName());
        Assert.assertEquals("ref1", refs.get(1).getName());
        Assert.assertEquals("bn1", refs.get(1).getBindingName());
    }

    @Test
    public void testResourceRef() throws Exception {
        WebBnd bnd = parseWebBndXML(
                webBndXML12(
                    "<resource-ref name=\"ref0\" binding-name=\"bn0\"/>" +
                    "<resource-ref name=\"ref1\" binding-name=\"bn1\"/>"));

        List<ResourceRef> refs = bnd.getResourceRefs();
        Assert.assertEquals(refs.toString(), 2, refs.size());
        Assert.assertEquals("ref0", refs.get(0).getName());
        Assert.assertEquals("bn0", refs.get(0).getBindingName());
        Assert.assertEquals("ref1", refs.get(1).getName());
        Assert.assertEquals("bn1", refs.get(1).getBindingName());
    }

    @Test
    public void testResourceRefXMI() throws Exception {
        WebApp webApp = parseWebApp(
                webApp(WebApp.VERSION_2_4,
                       "<resource-ref id=\"id0\">" +
                               "<res-ref-name>ref0</res-ref-name>" +
                       "</resource-ref>" +
                       "<resource-ref id=\"id1\">" +
                           "<res-ref-name>ref1</res-ref-name>" +
                       "</resource-ref>" +
                       "<resource-ref id=\"id2\">" +
                           "<res-ref-name>ref2</res-ref-name>" +
                       "</resource-ref>"));

        WebBnd bnd = parseWebBndXMI(
                webBndXMI20("",
                        "<resRefBindings jndiName=\"bn0\">" +
                                "<bindingResourceRef href=\"WEB-INF/web.xml#id0\"/>" +
                        "</resRefBindings>" +
                        "<resRefBindings jndiName=\"bn1\" loginConfigurationName=\"lcn1\">" +
                            "<bindingResourceRef href=\"WEB-INF/web.xml#id1\"/>" +
                            "<defaultAuth xmi:type=\"commonbnd:BasicAuthData\"/>" +
                        "</resRefBindings>" +
                        "<resRefBindings jndiName=\"bn2\" loginConfigurationName=\"lcn2\">" +
                            "<bindingResourceRef href=\"WEB-INF/web.xml#id2\"/>" +
                            "<defaultAuth xmi:type=\"commonbnd:BasicAuthData\" userId=\"userid2\" password=\"password2\"/>" +
                            "<properties name=\"pn0\" value=\"pv0\" description=\"pd0\"/>" +
                            "<properties name=\"pn1\" value=\"pv1\" description=\"pd1\"/>" +
                        "</resRefBindings>"),
                webApp);
        
        List<ResourceRef> refs = bnd.getResourceRefs();
        Assert.assertEquals(refs.toString(), 3, refs.size());

        Assert.assertEquals("ref0", refs.get(0).getName());
        Assert.assertEquals("bn0", refs.get(0).getBindingName());
        Assert.assertNull(refs.get(0).getCustomLoginConfiguration());
        Assert.assertNull(refs.get(0).getDefaultAuthUserid());
        Assert.assertNull(refs.get(0).getDefaultAuthPassword());

        List<Property> props1 = refs.get(1).getCustomLoginConfiguration().getProperties();
        Assert.assertEquals("ref1", refs.get(1).getName());
        Assert.assertEquals("bn1", refs.get(1).getBindingName());
        Assert.assertEquals("lcn1", refs.get(1).getCustomLoginConfiguration().getName());
        Assert.assertTrue(props1.toString(), props1.isEmpty());
        Assert.assertNull(refs.get(1).getDefaultAuthUserid());
        Assert.assertNull(refs.get(1).getDefaultAuthPassword());

        List<Property> props2 = refs.get(2).getCustomLoginConfiguration().getProperties();
        Assert.assertEquals("ref2", refs.get(2).getName());
        Assert.assertEquals("bn2", refs.get(2).getBindingName());
        Assert.assertEquals("lcn2", refs.get(2).getCustomLoginConfiguration().getName());
        Assert.assertEquals(props2.toString(), 2, props2.size());
        Assert.assertEquals("pn0", props2.get(0).getName());
        Assert.assertEquals("pv0", props2.get(0).getValue());
        Assert.assertEquals("pd0", props2.get(0).getDescription());
        Assert.assertEquals("pn1", props2.get(1).getName());
        Assert.assertEquals("pv1", props2.get(1).getValue());
        Assert.assertEquals("pd1", props2.get(1).getDescription());
        Assert.assertEquals("userid2", refs.get(2).getDefaultAuthUserid());
        Assert.assertEquals("password2", refs.get(2).getDefaultAuthPassword());
    }

    @Test
    public void testResourceEnvRef() throws Exception {
        WebBnd bnd = parseWebBndXML(
                webBndXML12(
                        "<resource-env-ref name=\"ref0\" binding-name=\"bn0\"/>" +
                        "<resource-env-ref name=\"ref1\" binding-name=\"bn1\"/>"));

        List<ResourceEnvRef> refs = bnd.getResourceEnvRefs();
        Assert.assertEquals(refs.toString(), 2, refs.size());
        Assert.assertEquals("ref0", refs.get(0).getName());
        Assert.assertEquals("bn0", refs.get(0).getBindingName());
        Assert.assertEquals("ref1", refs.get(1).getName());
        Assert.assertEquals("bn1", refs.get(1).getBindingName());
    }

    @Test
    public void testResourceEnvRefXMI() throws Exception {
        WebApp webApp = parseWebApp(
                webApp(WebApp.VERSION_2_4,
                        "<resource-env-ref id=\"id0\">" +
                                "<resource-env-ref-name>ref0</resource-env-ref-name>" +
                        "</resource-env-ref>" +
                        "<resource-env-ref id=\"id1\">" +
                            "<resource-env-ref-name>ref1</resource-env-ref-name>" +
                        "</resource-env-ref>"));

        WebBnd bnd = parseWebBndXMI(
                webBndXMI20("",
                        "<resourceEnvRefBindings jndiName=\"bn0\">" +
                            "<bindingResourceEnvRef href=\"WEB-INF/web.xml#id0\"/>" +
                        "</resourceEnvRefBindings>" +
                            "<resourceEnvRefBindings jndiName=\"bn1\">" +
                            "<bindingResourceEnvRef href=\"WEB-INF/web.xml#id1\"/>" +
                        "</resourceEnvRefBindings>"),
                webApp);

        List<ResourceEnvRef> refs = bnd.getResourceEnvRefs();
        Assert.assertEquals(refs.toString(), 2, refs.size());
        Assert.assertEquals("ref0", refs.get(0).getName());
        Assert.assertEquals("bn0", refs.get(0).getBindingName());
        Assert.assertEquals("ref1", refs.get(1).getName());
        Assert.assertEquals("bn1", refs.get(1).getBindingName());
    }

    @Test
    public void testMessageDestinationRef() throws Exception {
        WebBnd bnd = parseWebBndXML(
                webBndXML12(
                        "<message-destination-ref name=\"ref0\" binding-name=\"bn0\"/>" +
                        "<message-destination-ref name=\"ref1\" binding-name=\"bn1\"/>"));

        List<MessageDestinationRef> refs = bnd.getMessageDestinationRefs();
        Assert.assertEquals(refs.toString(), 2, refs.size());
        Assert.assertEquals("ref0", refs.get(0).getName());
        Assert.assertEquals("bn0", refs.get(0).getBindingName());
        Assert.assertEquals("ref1", refs.get(1).getName());
        Assert.assertEquals("bn1", refs.get(1).getBindingName());
    }

    @Test
    public void testMessageDestinationRefXMI() throws Exception {
        WebApp webApp = parseWebApp(
                webApp(WebApp.VERSION_2_4,
                        "<message-destination-ref id=\"id0\">" +
                            "<message-destination-ref-name>ref0</message-destination-ref-name>" +
                        "</message-destination-ref>" +
                            "<message-destination-ref id=\"id1\">" +
                            "<message-destination-ref-name>ref1</message-destination-ref-name>" +
                        "</message-destination-ref>"));

        WebBnd bnd = parseWebBndXMI(
                webBndXMI20("",
                        "<messageDestinationRefBindings jndiName=\"bn0\">" +
                            "<bindingMessageDestinationRef href=\"WEB-INF/web.xml#id0\"/>" +
                        "</messageDestinationRefBindings>" +
                        "<messageDestinationRefBindings jndiName=\"bn1\">" +
                            "<bindingMessageDestinationRef href=\"WEB-INF/web.xml#id1\"/>" +
                        "</messageDestinationRefBindings>"),
                webApp);
        
        List<MessageDestinationRef> refs = bnd.getMessageDestinationRefs();
        Assert.assertEquals(refs.toString(), 2, refs.size());
        Assert.assertEquals("ref0", refs.get(0).getName());
        Assert.assertEquals("bn0", refs.get(0).getBindingName());
        Assert.assertEquals("ref1", refs.get(1).getName());
        Assert.assertEquals("bn1", refs.get(1).getBindingName());
    }
}
