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
package com.ibm.ws.security.jaspi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.WebRequestImpl;
import com.ibm.ws.webcontainer.security.WebSecurityContext;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class JaspiRequestTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpServletRequest mockReq = mock.mock(HttpServletRequest.class, "mockReq");
    private final HttpServletResponse mockRsp = mock.mock(HttpServletResponse.class, "mockRsp");
    private final SecurityMetadata mockSecurityMetadata = mock.mock(SecurityMetadata.class);
    private final MatchResponse mockMatchResponse = mock.mock(MatchResponse.class, "mockMatchResponse");
    private final WebAppSecurityConfig mockWebAppSecurityConfig = mock.mock(WebAppSecurityConfig.class);
    private final WebAppConfig mockWebAppConfig = mock.mock(WebAppConfig.class);
    private final LoginConfiguration mockLoginConfiguration =
                    mock.mock(LoginConfiguration.class);
    private final WebSecurityContext mockWebSecurityContext =
                    mock.mock(WebSecurityContext.class);

    WebRequest newWebRequest() {
        WebRequest webReq = new WebRequestImpl(mockReq, mockRsp, "appName",
                        mockWebSecurityContext, mockSecurityMetadata, mockMatchResponse, mockWebAppSecurityConfig);
        return webReq;
    }

    @Test
    public void testGetAppContext() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        mock.checking(new Expectations() {
            {
                allowing(mockWebAppConfig).getContextRoot();
                will(returnValue("/bob"));
                allowing(mockWebAppConfig).getVirtualHostName();
                will(returnValue("default_host"));
            }
        });
        String appContext = jaspiRequest.getAppContext();
        assertNotNull(appContext);
        assertEquals(appContext, "default_host /bob");
    }

    @Test
    public void testGetApplicationName() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        mock.checking(new Expectations() {
            {
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bob"));
            }
        });
        String appName = jaspiRequest.getApplicationName();
        assertNotNull(appName);
        assertEquals(appName, "bob");
    }

    @Test
    public void testGetHttpServletRequest() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        HttpServletRequest req = jaspiRequest.getHttpServletRequest();
        assertNotNull(req);
        assertEquals(req, mockReq);
    }

    @Test
    public void testGetHttpServletResponse() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        HttpServletResponse res = jaspiRequest.getHttpServletResponse();
        assertNotNull(res);
        assertEquals(res, mockRsp);
    }

    @Test
    public void testGetLoginConfig() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockSecurityMetadata).getLoginConfiguration();
                will(returnValue(mockLoginConfiguration));
            }
        });
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        LoginConfiguration lc = jaspiRequest.getLoginConfig();
        assertNotNull(lc);
        assertEquals(lc, mockLoginConfiguration);
    }

    @Test
    public void testGetMessageInfo() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        MessageInfo msgInfo = new JaspiMessageInfo(null, null);
        jaspiRequest.setMessageInfo(msgInfo);
        MessageInfo mi = jaspiRequest.getMessageInfo();
        assertNotNull(mi);
        assertEquals(mi, msgInfo);
    }

    @Test
    public void testGetModuleName() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        mock.checking(new Expectations() {
            {
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bob"));
            }
        });
        String modName = jaspiRequest.getModuleName();
        assertNotNull(modName);
        assertEquals(modName, "bob");
    }

    @Test
    public void testPassword() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        jaspiRequest.setPassword("bob");
        String pw = jaspiRequest.getPassword();
        assertNotNull(pw);
        assertEquals(pw, "bob");
    }

    @Test
    public void testUserid() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        jaspiRequest.setUserid("bob");
        String uid = jaspiRequest.getUserid();
        assertNotNull(uid);
        assertEquals(uid, "bob");
    }

    @Test
    public void testGetWebSecurityContext() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        WebSecurityContext wsc = jaspiRequest.getWebSecurityContext();
        assertNotNull(wsc);
        assertEquals(wsc, mockWebSecurityContext);
    }

    @Test
    public void testLogoutMethod() throws Exception {
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        jaspiRequest.setLogoutMethod(true);
        boolean islogout = jaspiRequest.isLogoutMethod();
        assertNotNull(islogout);
        assertEquals(islogout, true);
    }

    @Test
    public void testIsProtected() throws Exception {
        final ArrayList<String> list = new ArrayList<String>();
        list.add("bob");
        mock.checking(new Expectations() {
            {
                allowing(mockMatchResponse).getRoles();
                will(returnValue(list));
            }
        });
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        boolean isprot = jaspiRequest.isProtected();
        assertNotNull(isprot);
        assertEquals(isprot, true);
    }
}
