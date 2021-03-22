/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.xml.namespace.QName;

import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;

import org.opensaml.core.xml.XMLObject;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class DumpData {
    @SuppressWarnings("unused")
    private static TraceComponent tc = Tr.register(DumpData.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    @Trivial
    public static StringBuffer dumpAssertion(StringBuffer sbParent, Assertion assertion, int identLevel) {
        StringBuffer sb = sbParent == null ? new StringBuffer("\n") : sbParent;
        sb.append(identString(identLevel, true))
                        .append("SubjectID(Username):").append(assertion.getSubject().getNameID().getValue()).append("\n");
        sb.append(identString(identLevel))
                        .append("Issuer:").append(assertion.getIssuer().getValue()).append("\n");
        sb.append(identString(identLevel))
                        .append("isSigned:").append(assertion.isSigned()).append(" signReferenceId:").append(assertion.getSignatureReferenceID()).append("\n");
        sb.append(identString(identLevel))
                        .append("DOM:").append(assertion.getDOM()).append(" @").append(assertion.getDOM().hashCode()).append(")\n");
        List<XMLObject> children = assertion.getOrderedChildren();
        for (XMLObject xmlObject : children) {
            if (xmlObject instanceof Assertion) {
                dumpAssertion(sb, (Assertion) xmlObject, identLevel + 1);
            }
        }
        return sb;
    }

    @Trivial
    public static StringBuffer dumpXMLObject(StringBuffer sbParent, XMLObject xmlObject, int identLevel) {
        StringBuffer sb = sbParent == null ? new StringBuffer("\n") : sbParent;
        if (xmlObject == null) {
            sb.append(identString(identLevel, false))
                            .append("found an null XMLObject\n");
            return sb;
        }
        QName qName = xmlObject.getElementQName();
        sb.append(identString(identLevel, true))
                        .append(qName.getPrefix()).append(":").append(qName.getLocalPart()).append("(").append(qName.getNamespaceURI()).append(")\n");
        Element dom = xmlObject.getDOM();
        if (dom != null) {
            sb.append(identString(identLevel))
                            .append("DOM:").append(dom).append(" @").append(dom.hashCode()).append(")\n");
        } else {
            sb.append(identString(identLevel)).append("DOM is null");
        }

        if (xmlObject instanceof SignableSAMLObject) {
            sb.append(identString(identLevel))
                            .append("isSigned:").append(((SignableSAMLObject) xmlObject).isSigned())
                            .append(" id:").append(((SignableSAMLObject) xmlObject).getSignatureReferenceID()).append(")\n");
        }
        if (xmlObject.hasChildren()) {
            List<XMLObject> children = xmlObject.getOrderedChildren();
            for (XMLObject xmlObj : children) {
                dumpXMLObject(sb, xmlObj, identLevel + 1);
            }
        }
        return sb;
    }

    @Trivial
    public static String dumpRequestInfo(HttpServletRequest request) {
        StringBuffer strBuf = new StringBuffer("");
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (int iI = 0; iI < cookies.length; iI++) {
                    strBuf.append("cookie " + cookies[iI] + "\n");
                }
            }
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames != null && headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String value = request.getHeader(headerName);
                strBuf.append("header " + headerName + ":" + value + "\n");
            }
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames != null && parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                String[] values = request.getParameterValues(parameterName);
                if (values != null && values.length > 0) {
                    for (int iI = 0; iI < values.length; iI++) {
                        strBuf.append("parameter " + parameterName + ":" + values[iI] + "\n");
                    }
                } else {
                    strBuf.append("parameter " + parameterName + ":null or empty\n");
                }
            }
            StringBuffer requestUrl = request.getRequestURL();
            strBuf.append("requestUrl:" + requestUrl.toString() + "\n");
            strBuf.append("queryString:" + request.getQueryString() + "\n");
            strBuf.append("sessionId:" + request.getRequestedSessionId() + "\n");
            strBuf.append("session:" + request.getSession(true) + "\n");
            String contentType = request.getContentType();
            strBuf.append("contentType:" + contentType + "\n");
            strBuf.append("method:" + request.getMethod() + "\n");
            if ("multipart/form-data".equalsIgnoreCase(contentType)) {
                Collection<Part> parts = request.getParts();
                for (Part part : parts) {
                    strBuf.append("part:" + part.getName() + "->" + part.getContentType() + "\n");
                }
            }
        } catch (Exception e) {
            // doing nothing since this is debugging trace
            strBuf.append("Hit unexpect exception:" + e);
        }

        return strBuf.toString();
    }

    @Trivial
    static String identString(int ident) {
        String space = "  ";
        String result = "";
        for (int iI = 0; iI < ident; iI++) {
            result = result.concat(space);
        }
        return result;
    }

    @Trivial
    static String identString(int ident, boolean bOk) {
        String space = bOk ? "**" : "==";
        if (ident == 0)
            return "";
        String result = identString(ident - 1);

        return result.concat(space);
    }

    /**
     * @param metadataProvider
     * @return
     */
    public static StringBuffer dumpMetadata(DOMMetadataResolver metadataProvider) {
        XMLObject metadata = null;
        //TODO:
//        try {
//            metadata = metadataProvider.getMetadata();
//        } catch (MetadataProviderException e) {
//            // debug only. No need to handle
//        }
        return dumpXMLObject(null, metadata, 0);
    }
}
