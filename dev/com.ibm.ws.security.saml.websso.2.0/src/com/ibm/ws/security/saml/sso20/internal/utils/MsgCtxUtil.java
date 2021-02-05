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

//import org.opensaml.Configuration;
import org.opensaml.core.config.Configuration;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
//import org.opensaml.common.SAMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.PredicateRoleDescriptorResolver;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.security.impl.MetadataCredentialResolver;

//import org.opensaml.saml2.metadata.provider.MetadataProvider;
//import org.opensaml.saml2.metadata.provider.MetadataProviderException;
//import org.opensaml.security.MetadataCredentialResolverFactory;
//import org.opensaml.xml.parse.StaticBasicParserPool;
//import org.opensaml.xml.parse.XMLParserException;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.opensaml.xmlsec.keyinfo.impl.BasicProviderKeyInfoCredentialResolver; //@AV999
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.KeyInfoProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.InlineX509DataProvider;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.trust.TrustedCredentialTrustEngine;
import org.opensaml.security.x509.impl.BasicPKIXValidationInformation;
import org.opensaml.security.x509.impl.BasicX509CredentialNameEvaluator;
import org.opensaml.security.x509.PKIXValidationInformation;
import org.opensaml.security.x509.impl.StaticPKIXValidationInformationResolver;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.opensaml.xmlsec.signature.support.impl.PKIXSignatureTrustEngine;
import org.w3c.dom.Document;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

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

    //@FFDCIgnore({ PrivilegedActionException.class, MetadataProviderException.class })
    public static AcsDOMMetadataProvider parseIdpMetadataProvider(SsoConfig samlConfig) throws SamlException {

        AcsDOMMetadataProvider acsIdpMetadataProvider = null;
        final String strIdpMetadata = samlConfig.getIdpMetadata();
        if (strIdpMetadata != null && !strIdpMetadata.isEmpty()) {
            //strIdpMetadata = "/Users/aruna/libertyGit/open-liberty/dev/build.image/wlp/usr/servers/solicited/imports/localhost/entity.xml";
            final File fileIdpMetadata = new File(strIdpMetadata);
            
            InputStream inputStream = null;
            //try {
                // BasicParserPool ppMgr = new BasicParserPool();
                // ppMgr.setNamespaceAware(true);  // We may need to use our own XMLParser
                ParserPool parserPool;
                //StaticBasicParserPool ppMgr = (StaticBasicParserPool) Configuration.getParserPool(); //@AV999
                parserPool = XMLObjectProviderRegistrySupport.getParserPool();

                try {
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
                } catch (PrivilegedActionException e) {
                      Exception newe = e.getException(); // the real Exception
                      if (newe instanceof FileNotFoundException) {
                          if (tc.isDebugEnabled()) {
                              Tr.debug(tc, "Provider error MetadataFile:" + strIdpMetadata, newe);
                          }
                          throw new SamlException(
                                          "SAML20_NO_IDP_METADATA_ERROR",
//                                          "CWWKS5025E: The metadata file [" + strIdpMetadata +
//                                                          "] of the Identity Probvider in Service Provider [" + samlConfig.getProviderId() +
//                                                          "] does not exist or cannot be accessed. Exception message[" + newe.getMessage() +
//                                                          "]",
                                          newe, // cause
                                          new Object[] { strIdpMetadata, samlConfig.getProviderId(), newe.getMessage() });
                      } else {
                          if (tc.isDebugEnabled()) {
                              Tr.debug(tc, "unexpected Provider error MetadataFile:" + strIdpMetadata, e, newe);
                          }
                          // unexpected exception. The cause may be null
                          throw new SamlException(
                                          "SAML20_IDP_METADATA_PARSE_ERROR",
//                                          "CWWKS5026E: When parsing the metadata file [" + strIdpMetadata +
//                                                          "] of the Identity Provider in Service Provider [" + samlConfig.getProviderId() +
//                                                          "], it gets an unexpected exception. Exception message [" + e.getMessage() +
//                                                          "]",
                                          newe, // cause
                                          new Object[] { strIdpMetadata, samlConfig.getProviderId(), newe.getMessage() });
                      }
                }
                Document doc = null;
                if (inputStream != null) {
//                    final Document doc = ppMgr.parse(inputStream); //@AV999
                    
                    try {
                        doc = parserPool.parse(inputStream);
                    } catch (XMLParserException e) {
                        // TODO Auto-generated catch block
                        // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                        e.printStackTrace();
                    }  finally {
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
                    if (doc != null) {
                        Tr.debug(tc,  "@AV999, Babuji", SerializeSupport.nodeToString(doc));
//                        Tr.debug(tc,  "@AV999, Babuji, first child", SerializeSupport.nodeToString(doc.getFirstChild()));
//                        
//                        Tr.debug(tc,  "@AV999, Babuji root element", SerializeSupport.nodeToString(doc.getDocumentElement()));
                        acsIdpMetadataProvider = new AcsDOMMetadataProvider(doc.getDocumentElement(), fileIdpMetadata);
                        try {
                            acsIdpMetadataProvider.setId(samlConfig.getProviderId()); 
                            acsIdpMetadataProvider.initialize();
                            //acsIdpMetadataProvider.get
                            
//                            FilesystemMetadataResolver idpMetaDataProvider = null;
//                            try {
//                                idpMetaDataProvider = new FilesystemMetadataResolver( new File( strIdpMetadata ) );
//                            } catch (ResolverException e) {
//                                // TODO Auto-generated catch block
//                                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
//                                e.printStackTrace();
//                            }
//                            idpMetaDataProvider.setRequireValidMetadata( true );
//                            idpMetaDataProvider.setId( "myId" );
//                            //BasicParserPool pool = new BasicParserPool();
//
//                            //pool.initialize();
//                            idpMetaDataProvider.setParserPool( parserPool );
//                            idpMetaDataProvider.initialize();
//                            String entityID = "urn:mace:incommon:osu.edu";
//                            CriteriaSet criteriaSet = new CriteriaSet(new EntityIdCriterion(entityID));
//                            EntityDescriptor metadata2 = null;
//                            try {
//                                metadata2 = idpMetaDataProvider.resolveSingle(criteriaSet);
//                            } catch (ResolverException e) {
//                                // TODO Auto-generated catch block
//                                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
//                                e.printStackTrace();
//                            } //metadataProvider.getMetadata(); //@AV999
//                            //EntityDescriptor metadata2 = null; //@AV999
//                            Tr.debug(tc,  "@AV999, Babuji, from another md provider : ", metadata2);
//
                        } catch (ComponentInitializationException e) {
                            // TODO Auto-generated catch block
                            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                            // e.printStackTrace();
                        }
                        // debug
                        if (tc.isDebugEnabled()) {
                            //Tr.debug(tc, "dumpData metadataProvider:" + DumpData.dumpMetadata(acsIdpMetadataProvider)); //@AV999
                        }
                    }
                }
            //} catch (XMLParserException e) {
             //   if (tc.isDebugEnabled()) {
              //      Tr.debug(tc, "Can not parse MetadataFile:" + strIdpMetadata, e);
              //  }
               // throw new SamlException(
                 //               "SAML20_IDP_METADATA_PARSE_ERROR",
                                //"CWWKS5023E: The IdP Metadata file [" + strIdpMetadata +
                                //                "] in Service Provider [" + samlConfig.getProviderId() +
                                //                "] cannot be parsed as an XML file. Exception [" + e.getMessage() +
                                //                "]",
                 //               e, // cause
                 //               new Object[] { strIdpMetadata, samlConfig.getProviderId(), e.getMessage() });
            //} catch (MetadataProviderException e) {
                // One of the possible cause is: The metadata file is an XML file but does not have legal SAML20 tags
            //    if (tc.isDebugEnabled()) {
            //        Tr.debug(tc, "Provider error MetadataFile:" + strIdpMetadata, e);
            //    }
             //   throw new SamlException(
              //                  "SAML20_IDP_METADATA_PARSE_ERROR",
                                //"CWWKS5024E: The metadata file [" + strIdpMetadata +
                                //                "] of the Identity Probvider in Service Provider [" + samlConfig.getProviderId() +
                                //                " has bad data in it and cannot be handled as a metadata file. Exception message [" + e.getMessage() +
                                //                "]",
                //                e, // cause
                 //               new Object[] { strIdpMetadata, samlConfig.getProviderId(), e.getMessage() });
            //} catch (PrivilegedActionException e) {
              //  Exception newe = e.getException(); // the real Exception
              //  if (newe instanceof FileNotFoundException) {
              //      if (tc.isDebugEnabled()) {
               //         Tr.debug(tc, "Provider error MetadataFile:" + strIdpMetadata, newe);
                 //   }
                  //  throw new SamlException(
                                   // "SAML20_NO_IDP_METADATA_ERROR",
                                    //"CWWKS5025E: The metadata file [" + strIdpMetadata +
                                    //                "] of the Identity Probvider in Service Provider [" + samlConfig.getProviderId() +
                                    //                "] does not exist or cannot be accessed. Exception message[" + newe.getMessage() +
                                    //                "]",
                               //     newe, // cause
                               //     new Object[] { strIdpMetadata, samlConfig.getProviderId(), newe.getMessage() });
                //} else {
                  //  if (tc.isDebugEnabled()) {
                  //      Tr.debug(tc, "unexpected Provider error MetadataFile:" + strIdpMetadata, e, newe);
                   // }
                    // unexpected exception. The cause may be null
                    //throw new SamlException(
                      //              "SAML20_IDP_METADATA_PARSE_ERROR",
                                    //"CWWKS5026E: When parsing the metadata file [" + strIdpMetadata +
                                    //                "] of the Identity Provider in Service Provider [" + samlConfig.getProviderId() +
                                    //                "], it gets an unexpected exception. Exception message [" + e.getMessage() +
                                    //                "]",
                     //               newe, // cause
                      //              new Object[] { strIdpMetadata, samlConfig.getProviderId(), newe.getMessage() });
                //}
            //} catch (NullPointerException e) {
                // One of the possible cause is: The metadata file is an XML file but does not have legal SAML20 tags
             //   if (tc.isDebugEnabled()) {
             //       Tr.debug(tc, "Provider error MetadataFile:" + strIdpMetadata, e);
              //  }
              //  throw new SamlException(
               //                 "SAML20_IDP_METADATA_PARSE_ERROR",
                                //"CWWKS5024E: The metadata file [" + strIdpMetadata +
                                //                "] of the Identity Probvider in Service Provider [" + samlConfig.getProviderId() +
                                //                " has bad data in it and cannot be handled as a metadata file. Exception message [" + e.getMessage() +
                                //                "]",
                  //              e, // cause
                     //           new Object[] { strIdpMetadata, samlConfig.getProviderId(), e.getMessage() });
           // } finally {
             //   if (inputStream != null) {
              //      try {
              //          inputStream.close();
              //      } catch (IOException e) {
                        // Error handling?
                //        if (tc.isDebugEnabled()) {
                  //          Tr.debug(tc, "Can not close InputStream of MetadataFile:" + strIdpMetadata, e);
                   //     }
                  //  }
               // }
           // }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The idpMetadataFile in " + samlConfig.getProviderId() + " is null. This has to define the trustStore to verify the signature in SAML Response");
            }
            // This maybe on purpose. Do not throw Exception to increase unnecessary FFDC.
            // So, we will have to use the trustStore which is defined in the server.xml to verify
        }

        return acsIdpMetadataProvider;
    }

    public static TrustEngine<Signature> getTrustedEngine(BasicMessageContext<?, ?> context) throws SamlException {
        SsoConfig ssoConfig = context.getSsoConfig();
        if (!ssoConfig.isPkixTrustEngineEnabled()) {
            // no pkixTrustEngine, it's a metadata
            return getTrustedEngineFromMetadata(context);
        } else { // Constants.ENGINE_TYPE_PKIX only
            return getTrustedEngineFromPkix(context);
        }
    }

    public static TrustEngine<Signature> getTrustedEngineFromMetadata(BasicMessageContext<?, ?> context) {
        //MetadataProvider metadataProvider = context.getMetadataProvider(); //@AV999 TODO: any other way?
        DOMMetadataResolver metadataProvider = context.getMetadataProvider();
        
        //@AV999 TODO: maybe we can just do roleResolver once
        PredicateRoleDescriptorResolver roleResolver;
        roleResolver = new PredicateRoleDescriptorResolver(metadataProvider);
        try {
            roleResolver.initialize();
        } catch (ComponentInitializationException e) {
             //@AV999 TODO:
        }
        
        MetadataCredentialResolver mdCredResolver = new MetadataCredentialResolver();
        mdCredResolver.setRoleDescriptorResolver(roleResolver);
        mdCredResolver.setKeyInfoCredentialResolver(getKeyInfoCredResolver());
        try {
            mdCredResolver.initialize();
        } catch (ComponentInitializationException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // e.printStackTrace();
        }
        // MetadataCredentialResolverFactory factory = MetadataCredentialResolverFactory.getFactory(); //@AV999

//        TrustedCredentialTrustEngine<Signature> engine = new ExplicitKeySignatureTrustEngine( //@AV999
//                        factory.getInstance(metadataProvider),
//                        getKeyInfoCredResolver()
//                        );
        TrustedCredentialTrustEngine<Signature> engine = new ExplicitKeySignatureTrustEngine(mdCredResolver, getKeyInfoCredResolver());
        return engine;
    }

    public static TrustEngine<Signature> getTrustedEngineFromPkix(BasicMessageContext<?, ?> context) throws SamlException {
        SsoConfig ssoConfig = context.getSsoConfig();
        Collection<X509Certificate> x509Certs = ssoConfig.getPkixTrustAnchors();
        Collection<X509CRL> x509CRLs = ssoConfig.getX509Crls(); //new ArrayList<X509CRL>();
        BasicPKIXValidationInformation pkixValidationInformation = new BasicPKIXValidationInformation(x509Certs, x509CRLs, Integer.valueOf(20));

        List<PKIXValidationInformation> infos = new ArrayList<PKIXValidationInformation>();
        Set<String> names = new HashSet<String>(); //@AV999 TODO: can we depend on their trusted names evaluation??
        //names.add("https://witsend4.austin.ibm.com/idp/shibboleth");
        infos.add(pkixValidationInformation);
        StaticPKIXValidationInformationResolver pkixValidationInfoResolver = new StaticPKIXValidationInformationResolver(infos, names);
        //SamlSsoPKIXValidationInformationResolver pkixValidationInfoResolver = new SamlSsoPKIXValidationInformationResolver(infos, names);
        //pkixValidationInfoResolver.
        PKIXSignatureTrustEngine pkixSignatureTrustEngine = new PKIXSignatureTrustEngine(pkixValidationInfoResolver,
                        getKeyInfoCredResolver());
        BasicX509CredentialNameEvaluator x509CredentialEvaluator = (BasicX509CredentialNameEvaluator) pkixSignatureTrustEngine.getX509CredentialNameEvaluator();
        x509CredentialEvaluator.setCheckSubjectAltNames(false);
        x509CredentialEvaluator.setCheckSubjectDN(false);
        x509CredentialEvaluator.setCheckSubjectDNCommonName(false);
        //context.setPKIXTrustEngine(true);
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
                                         BasicMessageContext<?, ?> context,
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
    static boolean tryTrustedIssuers(Issuer samlIssuer, BasicMessageContext<?, ?> context) {
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
