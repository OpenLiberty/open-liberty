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
package com.ibm.ws.security.saml.sso20.metadata;

import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.impl.AssertionConsumerServiceBuilder;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorMarshaller;
import org.opensaml.saml.saml2.metadata.impl.KeyDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.SPSSODescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.SingleLogoutServiceBuilder;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.impl.KeyInfoBuilder;

import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;

/**
 * http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
 * section 3.4
 */
public class SpMetadataBuilder {
    public static final TraceComponent tc = Tr.register(SpMetadataBuilder.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    SsoSamlService ssoService = null;
    final static String acsStr = "/acs";
    final static String sloStr = "/slo";

    /**
     * @param service
     */
    public SpMetadataBuilder(SsoSamlService samlService) {
        ssoService = samlService;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SPMetadataBuilder(" + samlService.getProviderId() + ")");
        }
    }

    // We may want to cache the SpMetadata in case the Config has not been changed
    public String buildSpMetadata(HttpServletRequest request) throws SamlException {
        EntityDescriptor entityDescriptor = buildEntityDescriptor(request);
        String result = getEntityDescriptor(entityDescriptor);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " SpMetadataData:" + result);
        }
        return result;
    }

    /**
     * <?xml version="1.0" encoding="UTF-8"?>
     * <md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
     * entityID="https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20">
     * <md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol"
     * WantAssertionsSigned="true"
     * AuthnRequestsSigned="true">
     * <md:KeyDescriptor use="signing">
     * <KeyInfo xmlns="http://www.w3.org/2000/09/xmldsig#">
     * <X509Data>
     * <X509Certificate>
     * MIICBzCCAXCgAwIBAgIEQH26vjANBgkqhkiG9w0BAQQFADBIMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGVGl2b2xpMQ4wDAYDVQQLEwVUQU1lQjEYMBYGA1UEAxMPZmltZGVtby5pYm0uY29tMB4XDTA0MDQxNDIyMjcxMFoXDTE3MTIyMjIyMjcxMFowSDELMAkGA1UEBhMCVVMxDzANBgNVBAoTBlRpdm9saTEOMAwGA1UECxMFVEFNZUIxGDAWBgNVBAMTD2ZpbWRlbW8uaWJtLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAiZ0D1X6rk8
     * +ZwNBTVZt7C85m421a8A52Ksjw40t+jNvbLYDp/W66AMMYD7rB5qgniZ5K1p9W8ivM9WbPxc2u/60tFPg0e/Q/r/fxegW1K1umnay+5MaUvN3p4XUCRrfg79OvurvXQ7GZa1/
     * wOp5vBIdXzg6i9CVAqL29JGi6GYUCAwEAATANBgkqhkiG9w0BAQQFAAOBgQBXiAhxm91I4m
     * +g3YX+dyGc352TSKO8HvAIBkHHFFwIkzhNgO+zLhxg5UMkOg12X9ucW7leZ1IB0Z6+JXBrXIWmU3UPum+QxmlaE0OG9zhp9LEfzsE5+ff+7XpS0wpJklY6c+cqHj4aTGfOhSE6u7BLdI26cZNdzxdhikBMZPgdyQ==
     * </X509Certificate>
     * </X509Data>
     * </KeyInfo>
     * </md:KeyDescriptor>
     * <md:KeyDescriptor use="encryption">
     * <KeyInfo xmlns="http://www.w3.org/2000/09/xmldsig#">
     * <X509Data>
     * <X509Certificate>
     * MIICBzCCAXCgAwIBAgIEQH26vjANBgkqhkiG9w0BAQQFADBIMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGVGl2b2xpMQ4wDAYDVQQLEwVUQU1lQjEYMBYGA1UEAxMPZmltZGVtby5pYm0uY29tMB4XDTA0MDQxNDIyMjcxMFoXDTE3MTIyMjIyMjcxMFowSDELMAkGA1UEBhMCVVMxDzANBgNVBAoTBlRpdm9saTEOMAwGA1UECxMFVEFNZUIxGDAWBgNVBAMTD2ZpbWRlbW8uaWJtLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAiZ0D1X6rk8
     * +ZwNBTVZt7C85m421a8A52Ksjw40t+jNvbLYDp/W66AMMYD7rB5qgniZ5K1p9W8ivM9WbPxc2u/60tFPg0e/Q/r/fxegW1K1umnay+5MaUvN3p4XUCRrfg79OvurvXQ7GZa1/
     * wOp5vBIdXzg6i9CVAqL29JGi6GYUCAwEAATANBgkqhkiG9w0BAQQFAAOBgQBXiAhxm91I4m
     * +g3YX+dyGc352TSKO8HvAIBkHHFFwIkzhNgO+zLhxg5UMkOg12X9ucW7leZ1IB0Z6+JXBrXIWmU3UPum+QxmlaE0OG9zhp9LEfzsE5+ff+7XpS0wpJklY6c+cqHj4aTGfOhSE6u7BLdI26cZNdzxdhikBMZPgdyQ==
     * </X509Certificate>
     * </X509Data>
     * </KeyInfo>
     * <md:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-1_5"/>
     * </md:KeyDescriptor>
     * <md:ArtifactResolutionService isDefault="true"
     * index="0"
     * Location="https://fvttest_sp.austin.ibm.com:9444/sps/FvttestSp/saml20/soap"
     * Binding="urn:oasis:names:tc:SAML:2.0:bindings:SOAP"/>
     * <md:SingleLogoutService Location="https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20/slo"
     * Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact"/>
     * <md:SingleLogoutService Location="https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20/slo"
     * Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"/>
     * <md:SingleLogoutService Location="https://fvttest_sp.austin.ibm.com:9444/sps/FvttestSp/saml20/soap"
     * Binding="urn:oasis:names:tc:SAML:2.0:bindings:SOAP"/>
     * <md:NameIDFormat>
     * urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
     * </md:NameIDFormat>
     * <md:NameIDFormat>
     * urn:oasis:names:tc:SAML:2.0:nameid-format:transient
     * </md:NameIDFormat>
     * <md:NameIDFormat>
     * urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress
     * </md:NameIDFormat>
     * <md:NameIDFormat>
     * urn:oasis:names:tc:SAML:2.0:nameid-format:encrypted
     * </md:NameIDFormat>
     * <md:AssertionConsumerService isDefault="true"
     * index="0"
     * Location="https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20/login"
     * Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact"/>
     * <md:AssertionConsumerService index="1"
     * Location="https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20/login"
     * Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"/>
     * </md:SPSSODescriptor>
     * <md:Organization>
     * <md:OrganizationName xml:lang="en">
     * FvtTestSP
     * </md:OrganizationName>
     * <md:OrganizationDisplayName xml:lang="en">
     * FvtTestSP
     * </md:OrganizationDisplayName>
     * <md:OrganizationURL xml:lang="en"/>
     * </md:Organization>
     * <md:ContactPerson contactType="technical">
     * <md:Company>
     * FvtTestSP
     * </md:Company>
     * <md:GivenName/>
     * <md:SurName/>
     * <md:EmailAddress/>
     * <md:TelephoneNumber/>
     * </md:ContactPerson>
     * </md:EntityDescriptor>
     *
     */
    /**
     *
     * @param inResponseToId
     * @param req
     * @param basicMsgCtx
     * @return
     * @throws SamlException
     */
    EntityDescriptor buildEntityDescriptor(HttpServletRequest request) throws SamlException {
        // SsoConfig samlConfig = ssoService.getConfig();
        EntityDescriptorBuilder edBuilder = new EntityDescriptorBuilder();
        EntityDescriptor entityDescriptor = edBuilder.buildObject();

        String spURL = RequestUtil.getEntityUrl(request,
                                                Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                                ssoService.getProviderId(),
                                                ssoService.getConfig());
        entityDescriptor.setEntityID(spURL);

        // build SPSSODescriptorBuilder
        SPSSODescriptorBuilder spSSODescriptorBuilder = new SPSSODescriptorBuilder();
        SPSSODescriptor spSSODescriptor = spSSODescriptorBuilder.buildObject();

        spSSODescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        SsoConfig samlConfig = ssoService.getConfig();
        boolean wantAssertionsSignedAttr = samlConfig.isWantAssertionsSigned();
        boolean authReqSignedAttr = samlConfig.isAuthnRequestsSigned();

        spSSODescriptor.setWantAssertionsSigned(Boolean.valueOf(wantAssertionsSignedAttr));
        spSSODescriptor.setAuthnRequestsSigned(Boolean.valueOf(authReqSignedAttr));

        X509Certificate cert = null;
        try {
            cert = (X509Certificate) ssoService.getSignatureCertificate();
        } catch (KeyStoreException e) {

        } catch (CertificateException e) {

        }

        if (authReqSignedAttr && cert == null) {
            // error handling
            throw new SamlException("SAML20_NO_CERT",
                            //SAML20_NO_CERT=CWWKS5074E: Cannot find a signature certificate from Service Provider [{0}].
                            null, true, new Object[] { ssoService.getProviderId(), ssoService.getConfig().getKeyStoreRef() });
        }
        boolean includeX509 = samlConfig.isIncludeX509InSPMetadata();
        if (cert != null && includeX509) {
            // KeyDescriptor
            KeyDescriptorBuilder keyDescriptorBuilder = new KeyDescriptorBuilder();
            // KeyInfo
            KeyInfoBuilder keyInfoBuilder = new KeyInfoBuilder();
            KeyDescriptor keyDescriptor = keyDescriptorBuilder.buildObject();
            keyDescriptor.setUse(UsageType.SIGNING);
            KeyInfo keyInfo = keyInfoBuilder.buildObject();
            try {
                KeyInfoSupport.addCertificate(keyInfo, cert);
            } catch (CertificateEncodingException e) {

            }
            keyDescriptor.setKeyInfo(keyInfo);
            keyDescriptor.setParent(spSSODescriptor);
            spSSODescriptor.getKeyDescriptors().add(0, keyDescriptor);

            KeyDescriptor encKeyDescriptor = keyDescriptorBuilder.buildObject();
            //UsageType
            encKeyDescriptor.setUse(UsageType.ENCRYPTION);
            KeyInfo encKeyInfo = keyInfoBuilder.buildObject();
            try {
                KeyInfoSupport.addCertificate(encKeyInfo, cert);
            } catch (CertificateEncodingException e) {

            }
            encKeyDescriptor.setKeyInfo(encKeyInfo);
            encKeyDescriptor.setParent(spSSODescriptor);
            spSSODescriptor.getKeyDescriptors().add(1, encKeyDescriptor);
        }

        AssertionConsumerServiceBuilder assertionConsumerServiceBuilder = new AssertionConsumerServiceBuilder();
        AssertionConsumerService assertionConsumerService = assertionConsumerServiceBuilder.buildObject();
        assertionConsumerService.setIsDefault(Boolean.TRUE);
        assertionConsumerService.setIndex(0);
        assertionConsumerService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI); //urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST
        String acsLoc = spURL.concat(acsStr);
        assertionConsumerService.setLocation(acsLoc);

        spSSODescriptor.getAssertionConsumerServices().add(assertionConsumerService);

        entityDescriptor.getRoleDescriptors().add(spSSODescriptor);

        SingleLogoutService s = new SingleLogoutServiceBuilder().buildObject();
        s.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        s.setLocation(spURL + sloStr);
        spSSODescriptor.getSingleLogoutServices().add(s);

        return entityDescriptor;
    }

    String getEntityDescriptor(EntityDescriptor entityDescriptor) throws SamlException {
        String result = null;
        if (entityDescriptor != null) {
            try {
                EntityDescriptorMarshaller marshaller = new EntityDescriptorMarshaller();
                Element element = marshaller.marshall(entityDescriptor);
                //result = XMLHelper.nodeToString(element);
                result = SerializeSupport.nodeToString(element); //v3
            } catch (MarshallingException e) {
                throw new SamlException(e, true); // Let SamlException handles opensaml Exception
            }
        }
        return result;
    }
}
