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
package com.ibm.ws.security.saml.sso20.rs;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;

/**
 *
 */
public class RsSamlConsumer<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, NameIdentifierType extends SAMLObject> {
    private static TraceComponent tc = Tr.register(RsSamlConsumer.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    @SuppressWarnings("rawtypes")
    static RsSamlConsumer<?, ?, ?> instance =
                    new RsSamlConsumer();

    public static RsSamlConsumer<?, ?, ?> getInstance() {
        return instance;
    }

    static void setInstance(RsSamlConsumer<?, ?, ?> instance) {
        RsSamlConsumer.instance = instance;
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ SamlException.class })
    public BasicMessageContext<InboundMessageType, OutboundMessageType>
                    handleSAMLResponse(HttpServletRequest req,
                                       HttpServletResponse res,
                                       SsoSamlService ssoService,
                                       SsoRequest samlRequest,
                                       String headerContent) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> messageContext = null;
        try {
            @SuppressWarnings("rawtypes")
            BasicMessageContextBuilder ctxBuilder = BasicMessageContextBuilder.getInstance();
            messageContext = ctxBuilder.buildRsSaml(req, res, ssoService, null, samlRequest);
            headerContent = headerContent.trim();
            byte[] bytes = null;
            // Plain text saml:Assertion begins with "<" and ends with ">"
            // And Base64 does not contains "<" or ">"
            if (headerContent.startsWith("<") && headerContent.endsWith(">")) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "SAML assertion in header" );
                }
                bytes = headerContent.getBytes(Constants.UTF8);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Encoded SAML assertion in header" );
                }
                //bytes = Base64.decode(headerContent);
                bytes = decodeSaml(headerContent);//Base64.decodeBase64(headerContent); //v3
            }
            if (bytes == null) {
                // the incoming saml_token is not in good shape
                //SAML_BAD_INBOUND_SAML_TOKEN=CWWKS5208E: The inbound SAML Assertion is not valid [{0}].
                throw new SamlException("SAML_BAD_INBOUND_SAML_TOKEN",
                                null,
                                new Object[] { headerContent });
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ByteArrayDecoder byteArrayDecoder = new ByteArrayDecoder();

            byteArrayDecoder.doDecode(messageContext, byteArrayInputStream); //v3

            //decrypt EncryptedAssertion, and add decrypted assertion to Assertion list
            List<Assertion> assertions = decryptEncryptedAssertion(messageContext);
            MessageContext<SAMLObject> mc = messageContext.getMessageContext(); //v3
            
            mc.getSubcontext(SAMLPeerEntityContext.class, true).setRole(IDPSSODescriptor.DEFAULT_ELEMENT_NAME); //v3
            mc.getSubcontext(SAMLProtocolContext.class, true).setProtocol(SAMLConstants.SAML20P_NS); //@v3
            //Search for first valid assertion without exception
            Assertion validatedAssertion = null;
            SamlException lastSamlException = null;
            Exception lastException = null;
            // Find the valid assertion as defined in profile
            for (Assertion assertion : assertions) {
                if (assertion.getAuthnStatements().size() > 0 && assertion.getSubject() != null) {
                    try {
                        // Issuer is needed on verify Signature
                        String issuer = assertion.getIssuer().getValue();
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Issuer from ToBeValidate-assertion:" + issuer);
                        }
                        //messageContext.setInboundMessageIssuer(issuer);
                        messageContext.setInboundSamlMessageIssuer(issuer);//v3
                        //
                        RsAssertionValidator rsAssertionValidator = new RsAssertionValidator(messageContext, assertion);

                        rsAssertionValidator.validateAssertion();
                        validatedAssertion = assertion;
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Found valid Asserion " + assertion.getID());
                        }
                        break;
                    } catch (SamlException e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Invalid Asserion " + assertion.getID());
                        }
                        lastSamlException = e;
                    } catch (Exception e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Invalid Asserion " + assertion.getID());
                        }
                        lastException = e;
                    }
                } else {
                    if (assertion.getSubject() == null) {
                        lastException = new SamlException("SAML20_ELEMENT_ERR",
                                        // SAML20_NO_SUBJECT_ERR=CWWKS5042E: The SAML Assertion MUST contain a Subject element.
                                        null,
                                        new Object[] { "Subject" });
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Assertion " + assertion.getID() + " does not contain Subject");
                        }
                    }
                }
            }

            if (validatedAssertion != null) {
                messageContext.setValidatedAssertion(validatedAssertion);
            }
            else {
                if (lastException != null)
                    throw lastException;
                if (lastSamlException != null)
                    throw lastSamlException;
            }

        } catch (SamlException e) {
            throw e; // this had been handled already
        } catch (Exception e) {
            // unexpected error handling
            throw new SamlException(e); // let the samlException handle the unexpected Exception
        }
        return messageContext;
    }

    List<Assertion> decryptEncryptedAssertion(BasicMessageContext<?, ?> context) throws SamlException {
        List<Assertion> assertionList = new ArrayList<Assertion>();
        Object objectMessage = context.getMessageContext().getMessage();
        if (objectMessage instanceof Response) {
            // We do not allow samlp:Response in the Authorization header
            // return decryptEncryptedAssertion((Response) objectMessage, context);
            SsoSamlService ssoSamlService = context.getSsoService();
            String headerName = ssoSamlService.getConfig().getHeaderName();
            throw new SamlException("RS_SAML_RESPONSE_NOT_SUPPORTED",
                            // RS_SAML_RESPONSE_NOT_SUPPORTED=CWWKS5085E: The SAML Response in the content of the header [{0}] in the HTTP request is not supported.
                            null,
                            new Object[] { headerName });
        }
        if (objectMessage instanceof EncryptedAssertion) {
            try {
                EncryptedAssertion eA = (EncryptedAssertion) objectMessage;
                Decrypter decrypter = context.getDecrypter();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "encryptedAssertion:" + eA + " decrypter:" + decrypter);
                }
                Assertion decryptedAssertion = decrypter.decrypt(eA); //decrypt it!
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "decryptedAssertion:" + decryptedAssertion);
                }
                assertionList.add(decryptedAssertion);
            } catch (Exception e) {
                throw new SamlException(e);
            }
            return assertionList;
        }
        if (objectMessage instanceof Assertion) {
            assertionList.add((Assertion) objectMessage);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "InboundMessage is:" + objectMessage.getClass().getName() +
                             "\n" + objectMessage);
            }
            // TODO error handling. Did not get SamlResponse or Assertion
        }

        return assertionList;
        //return samlResponse.getAssertions();
    }
    
    public @Sensitive byte[] decodeSaml(@Sensitive String s) /*throws DecodingException*/ {
        
        s = s.replace('_', '/');
        switch (s.length() % 4) {
          case 0: break;
          case 2: s += "=="; break;
          case 3: s += "="; break;
          default: throw new IllegalArgumentException("Illegal Base64URL string!");
        }
        String headerContent = new String(s);
        byte[] bytes = Base64.decodeBase64(headerContent);
        
        // Check to see if it's gzip-compressed
        // GZIP Magic Two-Byte Number: 0x8b1f (35615)
        if (bytes != null && bytes.length >= 4) {
            int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
            if (java.util.zip.GZIPInputStream.GZIP_MAGIC == head) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "compressed SAML assertion in header" );
                }
                java.io.ByteArrayInputStream bais = null;
                java.util.zip.GZIPInputStream gzis = null;
                java.io.ByteArrayOutputStream baos = null;
                byte[] buffer = new byte[2048];
                int length = 0;

                try {
                    baos = new java.io.ByteArrayOutputStream();
                    bais = new java.io.ByteArrayInputStream(bytes);
                    gzis = new java.util.zip.GZIPInputStream(bais);

                    while ((length = gzis.read(buffer)) >= 0) {
                        baos.write(buffer, 0, length);
                    } // end while: reading input

                    // No error? Get new bytes.
                    bytes = baos.toByteArray();

                } // end try
                catch (java.io.IOException e) {
                    // Just return originally-decoded bytes
                } // end catch
                finally {
                    try {
                        baos.close();
                    } catch (Exception e) {
                    }
                    try {
                        gzis.close();
                    } catch (Exception e) {
                    }
                    try {
                        bais.close();
                    } catch (Exception e) {
                    }
                } // end finally

            } // end if: gzipped
        } // end if: bytes.length >= 2

        return bytes;
    }

}
