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
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opensaml.Configuration;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.security.MetadataCredentialResolverFactory;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.keyinfo.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoProvider;
import org.opensaml.xml.security.keyinfo.provider.InlineX509DataProvider;
import org.opensaml.xml.security.trust.TrustEngine;
import org.opensaml.xml.security.trust.TrustedCredentialTrustEngine;
import org.opensaml.xml.security.x509.BasicPKIXValidationInformation;
import org.opensaml.xml.security.x509.PKIXValidationInformation;
import org.opensaml.xml.security.x509.StaticPKIXValidationInformationResolver;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.impl.ExplicitKeySignatureTrustEngine;
import org.opensaml.xml.signature.impl.PKIXSignatureTrustEngine;
import org.w3c.dom.Document;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

/**
 *
 */
public class MsgCtxUtil<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, NameIdentifierType extends SAMLObject> {
    private static TraceComponent tc = Tr.register(MsgCtxUtil.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    @SuppressWarnings("rawtypes")
    static MsgCtxUtil<?, ?, ?> instance =
                    new MsgCtxUtil();

    public static MsgCtxUtil<?, ?, ?> getInstance() {
        return instance;
    }

    @FFDCIgnore({ PrivilegedActionException.class, MetadataProviderException.class })
    public static AcsDOMMetadataProvider parseIdpMetadataProvider(SsoConfig samlConfig) throws SamlException {

        AcsDOMMetadataProvider acsIdpMetadataProvider = null;
        final String strIdpMetadata = samlConfig.getIdpMetadata();
        if (strIdpMetadata != null && !strIdpMetadata.isEmpty()) {
            final File fileIdpMetadata = new File(strIdpMetadata);
            InputStream inputStream = null;
            try {
                // BasicParserPool ppMgr = new BasicParserPool();
                // ppMgr.setNamespaceAware(true);  // We may need to use our own XMLParser
                StaticBasicParserPool ppMgr = (StaticBasicParserPool) Configuration.getParserPool();

                inputStream = (InputStream) AccessController.doPrivileged(
                                new PrivilegedExceptionAction<Object>() {
                                    @Override
                                    public Object run() throws Exception {
                                        if (fileIdpMetadata.exists()) {
                                            return new FileInputStream(fileIdpMetadata);
                                        } else {
                                            return null;
                                        }

                                    }
                                }
                                );
                if (inputStream != null) {
                    final Document doc = ppMgr.parse(inputStream);
                    acsIdpMetadataProvider = new AcsDOMMetadataProvider(doc.getDocumentElement(), fileIdpMetadata);
                    acsIdpMetadataProvider.initialize();
                    // debug
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "dumpData metadataProvider:" + DumpData.dumpMetadata(acsIdpMetadataProvider));
                    }
                }
            } catch (XMLParserException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Can not parse MetadataFile:" + strIdpMetadata, e);
                }
                throw new SamlException(
                                "SAML20_IDP_METADATA_PARSE_ERROR",
                                //"CWWKS5023E: The IdP Metadata file [" + strIdpMetadata +
                                //                "] in Service Provider [" + samlConfig.getProviderId() +
                                //                "] cannot be parsed as an XML file. Exception [" + e.getMessage() +
                                //                "]",
                                e, // cause
                                new Object[] { strIdpMetadata, samlConfig.getProviderId(), e.getMessage() });
            } catch (MetadataProviderException e) {
                // One of the possible cause is: The metadata file is an XML file but does not have legal SAML20 tags
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Provider error MetadataFile:" + strIdpMetadata, e);
                }
                throw new SamlException(
                                "SAML20_IDP_METADATA_PARSE_ERROR",
                                //"CWWKS5024E: The metadata file [" + strIdpMetadata +
                                //                "] of the Identity Probvider in Service Provider [" + samlConfig.getProviderId() +
                                //                " has bad data in it and cannot be handled as a metadata file. Exception message [" + e.getMessage() +
                                //                "]",
                                e, // cause
                                new Object[] { strIdpMetadata, samlConfig.getProviderId(), e.getMessage() });
            } catch (PrivilegedActionException e) {
                Exception newe = e.getException(); // the real Exception
                if (newe instanceof FileNotFoundException) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Provider error MetadataFile:" + strIdpMetadata, newe);
                    }
                    throw new SamlException(
                                    "SAML20_NO_IDP_METADATA_ERROR",
                                    //"CWWKS5025E: The metadata file [" + strIdpMetadata +
                                    //                "] of the Identity Probvider in Service Provider [" + samlConfig.getProviderId() +
                                    //                "] does not exist or cannot be accessed. Exception message[" + newe.getMessage() +
                                    //                "]",
                                    newe, // cause
                                    new Object[] { strIdpMetadata, samlConfig.getProviderId(), newe.getMessage() });
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "unexpected Provider error MetadataFile:" + strIdpMetadata, e, newe);
                    }
                    // unexpected exception. The cause may be null
                    throw new SamlException(
                                    "SAML20_IDP_METADATA_PARSE_ERROR",
                                    //"CWWKS5026E: When parsing the metadata file [" + strIdpMetadata +
                                    //                "] of the Identity Provider in Service Provider [" + samlConfig.getProviderId() +
                                    //                "], it gets an unexpected exception. Exception message [" + e.getMessage() +
                                    //                "]",
                                    newe, // cause
                                    new Object[] { strIdpMetadata, samlConfig.getProviderId(), newe.getMessage() });
                }
            } catch (NullPointerException e) {
                // One of the possible cause is: The metadata file is an XML file but does not have legal SAML20 tags
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Provider error MetadataFile:" + strIdpMetadata, e);
                }
                throw new SamlException(
                                "SAML20_IDP_METADATA_PARSE_ERROR",
                                //"CWWKS5024E: The metadata file [" + strIdpMetadata +
                                //                "] of the Identity Probvider in Service Provider [" + samlConfig.getProviderId() +
                                //                " has bad data in it and cannot be handled as a metadata file. Exception message [" + e.getMessage() +
                                //                "]",
                                e, // cause
                                new Object[] { strIdpMetadata, samlConfig.getProviderId(), e.getMessage() });
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // Error handling?
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Can not close InputStream of MetadataFile:" + strIdpMetadata, e);
                        }
                    }
                }
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The idpMetadataFile in " + samlConfig.getProviderId() + " is null. This has to define the trustStore to verify the signature in SAML Response");
            }
            // This maybe on purpose. Do not throw Exception to increase unnecessary FFDC.
            // So, we will have to use the trustStore which is defined in the server.xml to verify
        }

        return acsIdpMetadataProvider;
    }

    public static TrustEngine<Signature> getTrustedEngine(BasicMessageContext<?, ?, ?> context) throws SamlException {
        SsoConfig ssoConfig = context.getSsoConfig();
        if (!ssoConfig.isPkixTrustEngineEnabled()) {
            // no pkixTrustEngine, it's a metadata
            return getTrustedEngineFromMetadata(context);
        } else { // Constants.ENGINE_TYPE_PKIX only
            return getTrustedEngineFromPkix(context);
        }
    }

    public static TrustEngine<Signature> getTrustedEngineFromMetadata(BasicMessageContext<?, ?, ?> context) {
        MetadataProvider metadataProvider = context.getMetadataProvider();
        MetadataCredentialResolverFactory factory = MetadataCredentialResolverFactory.getFactory();

        TrustedCredentialTrustEngine<Signature> engine = new ExplicitKeySignatureTrustEngine(
                        factory.getInstance(metadataProvider),
                        getKeyInfoCredResolver()
                        );
        return engine;
    }

    public static TrustEngine<Signature> getTrustedEngineFromPkix(BasicMessageContext<?, ?, ?> context) throws SamlException {
        SsoConfig ssoConfig = context.getSsoConfig();
        Collection<X509Certificate> x509Certs = ssoConfig.getPkixTrustAnchors();
        Collection<X509CRL> x509CRLs = ssoConfig.getX509Crls(); //new ArrayList<X509CRL>();
        BasicPKIXValidationInformation pkixValidationInformation = new BasicPKIXValidationInformation(x509Certs, x509CRLs, Integer.valueOf(20));

        List<PKIXValidationInformation> infos = new ArrayList<PKIXValidationInformation>();
        Set<String> names = new HashSet<String>();
        infos.add(pkixValidationInformation);
        StaticPKIXValidationInformationResolver pkixValidationInfoResolver = new StaticPKIXValidationInformationResolver(infos, names);

        PKIXSignatureTrustEngine pkixSignatureTrustEngine = new PKIXSignatureTrustEngine(pkixValidationInfoResolver,
                        getKeyInfoCredResolver());
        return pkixSignatureTrustEngine;
    }

    /**
     * @return
     */
    static KeyInfoCredentialResolver getKeyInfoCredResolver() {
        InlineX509DataProvider x509DataProvider = new InlineX509DataProvider();
        List<KeyInfoProvider> providers = new ArrayList<KeyInfoProvider>();
        providers.add(x509DataProvider);
        BasicProviderKeyInfoCredentialResolver keyInfoCredResolver = new BasicProviderKeyInfoCredentialResolver(providers);
        return keyInfoCredResolver;
    }

    public static boolean validateIssuer(Issuer samlIssuer,
                                         BasicMessageContext<?, ?, ?> context,
                                         boolean isRsSaml)
                    throws SamlException {
        if (samlIssuer.getFormat() != null && !samlIssuer.getFormat().equals(NameIDType.ENTITY)) {
            throw new SamlException("SAML20_NO_ISSUER_ERR",
                            //"The Issuer Format attribute MUST be omitted or have a value of "+ Constants.NAME_ID_FORMAT_ENTITY + ". " +
                            //samlIssuer.getFormat() + " is not a valid format",
                            null,
                            new Object[] { Constants.NAME_ID_FORMAT_ENTITY, samlIssuer.getFormat() });
        }
        // Validate that issuer is expected peer entity
        EntityDescriptor peerEntityMetadata = isRsSaml ? null : context.getPeerEntityMetadata();
        if (peerEntityMetadata == null) {
            if (!tryTrustedIssuers(samlIssuer, context)) {
                // can not find an EntityDescriptor for the request
                throw new SamlException("SAML20_INCORRECT_ISSUER_ERR",
                                //SAML20_INCORRECT_ISSUER_ERR=CWWKS5045E: The value for the Issuer element [{0}] in the SAML assertion is not valid..
                                null,
                                new Object[] { samlIssuer.getValue() });
            } else {
                return true;
            }
        }
        if (!peerEntityMetadata.getEntityID().equals(samlIssuer.getValue())) {
            if (!tryTrustedIssuers(samlIssuer, context)) {
                throw new SamlException("SAML20_INCORRECT_ISSUER_ERR",
                                //SAML20_INCORRECT_ISSUER_ERR=CWWKS5045E: The value for the Issuer element [{0}] in the SAML assertion is not valid.
                                null,
                                new Object[] { samlIssuer.getValue() });
            }
        }
        return true;
    }

    /**
     * @param samlIssuer
     * @param context
     * @return
     */
    static boolean tryTrustedIssuers(Issuer samlIssuer, BasicMessageContext<?, ?, ?> context) {
        String issuer = samlIssuer.getValue();
        SsoConfig ssoConfig = context.getSsoConfig();
        String[] trustedIssuers = ssoConfig.getPkixTrustedIssuers();
        if (trustedIssuers != null) {
            for (String trustedIssuer : trustedIssuers) {
                if (Constants.TRUST_ALL_ISSUERS.equals(trustedIssuer)) {
                    return true;
                }
                if (trustedIssuer.equals(issuer))
                    return true;
            }
        }
        return false;
    }

}
