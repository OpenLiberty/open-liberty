/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.trm.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.comms.ClientConnectionFactory;
import com.ibm.ws.sib.comms.ConnectionProperties;
import com.ibm.ws.sib.mfp.trm.TrmClientBootstrapReply;
import com.ibm.ws.sib.mfp.trm.TrmFirstContactMessage;
import com.ibm.ws.sib.trm.TrmSICoreConnectionFactory;
import com.ibm.ws.sib.trm.impl.TrmConstantsImpl;
import com.ibm.ws.sib.utils.comms.ProviderEndPoint;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

final public class TrmSICoreConnectionFactoryImpl extends
                TrmSICoreConnectionFactory {

    private static final String className = TrmSICoreConnectionFactoryImpl.class.getName();
    private static final TraceComponent tc = SibTr.register(TrmSICoreConnectionFactoryImpl.class, TrmConstantsImpl.MSG_GROUP, TrmConstantsImpl.MSG_BUNDLE);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(TrmConstantsImpl.MSG_BUNDLE);

    @Override
    public SICoreConnection createConnection(Subject subject,
                                             Map connectionProperties) throws SIAuthenticationException,
                    SIErrorException, SIIncorrectCallException, SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException, SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createConnection ", new Object[] { subject, connectionProperties });

        SICoreConnection sc = null;

        try {
            sc = createConnection(new CredentialType((String) connectionProperties
                            .get(SibTrmConstants.BUSNAME), subject), connectionProperties);
        } catch (SINotAuthorizedException sinae) {

            throw sinae;
        } catch (SINotPossibleInCurrentConfigurationException sinpicce) {

            throw sinpicce;
        } catch (SIResourceException sire) {

            throw sire;
        } catch (SIIncorrectCallException siice) {

            throw siice;
        } catch (SIErrorException siee) {

            throw siee;
        } catch (SIAuthenticationException siae) {

            throw siae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createConnection ", sc);

        return sc;
    }

    @Override
    public SICoreConnection createConnection(String username, String password,
                                             Map connectionProperties) throws SIAuthenticationException,
                    SIErrorException, SIIncorrectCallException, SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException, SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createConnection ", new Object[] { username, password, connectionProperties });

        SICoreConnection sc = null;

        try {
            sc = createConnection(new CredentialType(username, password),
                                  connectionProperties);
        } catch (SINotAuthorizedException sinae) {

            throw sinae;
        } catch (SINotPossibleInCurrentConfigurationException sinpicce) {

            throw sinpicce;
        } catch (SIResourceException sire) {

            throw sire;
        } catch (SIIncorrectCallException siice) {

            throw siice;
        } catch (SIErrorException siee) {

            throw siee;
        } catch (SIAuthenticationException siae) {

            throw siae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createConnection ", sc);

        return sc;
    }

    private SICoreConnection createConnection(CredentialType credentialType,
                                              Map props) throws SIAuthenticationException, SIErrorException,
                    SIIncorrectCallException, SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException, SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createConnection ", new Object[] { credentialType, props });

        ClientAttachProperties cap = new ClientAttachProperties(props,
                        (credentialType.getPassword() != null && !credentialType
                                        .getPassword().trim().equals("")));

        Exception finalException = null;

        SICoreConnection sc = null;

        try {

            //if providerEndpoint(i.e remoteServerAddress) list is empty .. then try in-process
            if (cap.getProviderEPs().isEmpty()) {
                sc = getConnectionToLocalME(credentialType, props, cap);
            }
            else
            {
                sc = remoteBootstrap(credentialType, cap);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, " Create connection failed", e);
            finalException = e;
        }

        if ((null == sc) && (null != finalException)) {
            if (finalException instanceof SIAuthenticationException)
                throw (SIAuthenticationException) finalException;

            if (finalException instanceof SIErrorException)
                throw (SIErrorException) finalException;

            if (finalException instanceof SIIncorrectCallException)
                throw (SIIncorrectCallException) finalException;

            if (finalException instanceof SINotAuthorizedException)
                throw (SINotAuthorizedException) finalException;

            if (finalException instanceof SINotPossibleInCurrentConfigurationException)
                throw (SINotPossibleInCurrentConfigurationException) finalException;

            if (finalException instanceof SIResourceException)
                throw (SIResourceException) finalException;
        }

        if (null == sc && cap.getProviderEPs().isEmpty()) {
            //In this condition we are handling the local ME failures
            if (CommonServiceFacade.getJsAdminService() != null)
            {
                //If JsAdminService (which indicate local ME is configured ) is not null and we are still failing to 
                //get a connection means the ME failed to start but feature wasJmsServer-1.0 is enabled
                throw new SIResourceException(nls.getFormattedMessage(
                                                                      "LIBERTY_BINDING_FAILED_CWSIT0134", null, null));

            }
            else
            {
                //This means the JsAdminService is null ( this indicate the local ME is not configured 
                // and we are asking user to enable the feature wasJmsServer-1.0
                throw new SIResourceException(nls.getFormattedMessage(
                                                                      "LIBERTY_BINDING_FAILED_CWSIT0132", new Object[] { "wasJmsServer-1.0" }, null));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createConnection ", sc);

        return sc;
    }

    private SICoreConnection getConnectionToLocalME(CredentialType credentialType,
                                                    Map props, ClientAttachProperties cap) throws SIAuthenticationException, SIErrorException,
                    SIIncorrectCallException, SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException, SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getConnectionToLocalME ", new Object[] { credentialType, props, cap });

        SICoreConnection sc = null;

        JsAdminService admnService = CommonServiceFacade.getJsAdminService();

        JsMessagingEngine local_ME = null;

        // Liberty adminService just returns the ME, no search filter is applied. 
        // Its possible that admnService is null i.e when wasJmsServer feature is removed and
        // wasJmsClient feature is still enabled
        if (admnService != null) {
            local_ME = admnService.getMessagingEngine(
                                                      JsConstants.DEFAULT_BUS_NAME, JsConstants.DEFAULT_ME_NAME);
        }
        if (local_ME != null) {
            // only if matching ME .. proceed and get connection
            SICoreConnectionFactory scf = (SICoreConnectionFactory) local_ME
                            .getMessageProcessor();

            if (credentialType.getSubject() != null) {
                sc = scf.createConnection(credentialType.getSubject(), cap
                                .getProperties());
            } else {
                sc = scf.createConnection(credentialType.getUserid(),
                                          credentialType.getPassword(), cap.getProperties());
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.entry(tc, " local_ME is null");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getConnectionToLocalME ", sc);
        return sc;
    }

    /*
     * Create a bootstrap attachment to a remote messaging engine
     */

    private SICoreConnection remoteBootstrap(CredentialType credentialType, ClientAttachProperties cap)
                    throws SIAuthenticationException, SIErrorException, SIIncorrectCallException, SINotAuthorizedException, SINotPossibleInCurrentConfigurationException, SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "remoteBootstrap", new Object[] { credentialType, cap });

        SICoreConnection sc = null;

        ClientConnectionFactory ccf = CommonServiceFacade.getClientConnectionFactory();

        if (ccf == null) {
            throw new SIErrorException(nls.getString("NO_CCF_CWSIT0004"));
        }

        // contact bootstrap service
        ContactBootstrapServiceResponse cbsr = contactBootstrapService(credentialType, cap, ccf);

        // process bootstrap reply
        ClientConnection cc = cbsr.getClientConnection();
        TrmClientBootstrapReply cbr = cbsr.getClientBootstrapReply();
        int rc = cbsr.getReturnCode();
        boolean goodReply = cbsr.isGoodReply();
        ProviderEndPoint ep = cbsr.getEndPoint();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "cc=" + cc + ", cbr=" + cbr + ", rc=" + rc + ", goodReply=" + goodReply + ", ep=" + ep.getEndPointInfo());

        if (goodReply) { //250165
            if (rc == TrmConstantsImpl.RETURN_CODE_OK) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Staying with bootstrap messaging engine");

                sc = cc.getSICoreConnection();

            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "*** shouldn't ever get here!"); //250165
            }
        } else {
            List uncontactableBootstrapServers = cbsr.getUncontactableBootstrapServers();
            List failedBootstrapServers = cbsr.getFailedBootstrapServers();
            Exception firstMirroredFailureException = cbsr.getFirstMirroredFailureException();
            Exception firstBootstrapFailureException = cbsr.getFirstBootstrapFailureException();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "uncontactableBootstrapServers=" + uncontactableBootstrapServers +
                                ", failedBootstrapServers=" + failedBootstrapServers +
                                ", firstMirroredFailureException=" + firstMirroredFailureException +
                                ", firstBootstrapFailureException=" + firstBootstrapFailureException);

            if (null != firstMirroredFailureException) {
                if (firstMirroredFailureException instanceof SIAuthenticationException) {
                    // authentication with the bootstrap server failed
                    throw (SIAuthenticationException) firstMirroredFailureException;
                } else if (firstMirroredFailureException instanceof SINotAuthorizedException) {
                    // not authorised to use the bootstrap server
                    throw (SINotAuthorizedException) firstMirroredFailureException;
                } else {
                    // we successfully connected to a bootstrap server but got an error
                    generateException(getRC(firstMirroredFailureException),
                                      nls.getFormattedMessage("LIBERTY_BOOTSTRAP_FAILURE_CWSIT0126", new Object[] {
                                                                                                                   failedBootstrapServers.get(0).toString(),
                                                                                                                   Utils.getFailureMessage(cbr.getFailureReason()) }, null),
                                      firstMirroredFailureException, true);
                }
            } else {
                // none of the specified bootstrap endpoints could be contacted
                generateException(getRC(firstBootstrapFailureException),
                                  nls.getFormattedMessage("LIBERTY_NO_BOOTSTRAP_CWSIT0127",
                                                          new Object[] { uncontactableBootstrapServers.toString() },
                                                          null), firstBootstrapFailureException, true);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "remoteBootstrap", sc);
        return sc;
    }

    /*
     * Request to create a bootstrap attachment to a remote messaging engine
     */

    private ContactBootstrapServiceResponse contactBootstrapService(
                                                                    CredentialType credentialType, ClientAttachProperties cap, ClientConnectionFactory ccf) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "contactBootstrapService", new Object[] { credentialType, cap, ccf });

        ClientConnection cc = null;
        TrmClientBootstrapReply cbr = null;
        int rc = TrmConstantsImpl.RETURN_CODE_NOK;
        List<String> uncontactableBootstrapServers = new ArrayList<String>(); //250165
        List<String> failedBootstrapServers = new ArrayList<String>(); //250165
        Exception firstMirroredFailureException = null;
        Exception firstBootstrapFailureException = null;
        boolean goodReply = false;

        List endpoints = cap.getProviderEPs();
        Iterator i = endpoints.iterator();
        ProviderEndPoint ep = null;
        while (i.hasNext()) {
            ep = (ProviderEndPoint) i.next();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Making bootstrap request to " + ep.getEndPointInfo());

            cc = ccf.createClientConnection();

            if (cc != null) {
                ConnectionProperties cp = new ConnectionProperties(ep);
                cp.setClientConnectionPropertyMap(cap.getProperties()); // Pass comms the connection property map       LIDB3743

                ClientBootstrapHandler cbh = new ClientBootstrapHandler(cap, credentialType, ep.getChain());

                try {
                    cc.connect(cp, cbh);

                    TrmFirstContactMessage fcm = cbh.getReply();
                    cbr = fcm.makeInboundTrmClientBootstrapReply();

                    rc = cbr.getReturnCode().intValue();

                    // no point checking other bootstrap service if authentication has failed - break out of while loop
                    if (rc == TrmConstantsImpl.RETURN_CODE_SIAuthenticationException) {
                        firstMirroredFailureException = generateException(rc,
                                                                          nls.getFormattedMessage("LIBERTY_FAILED_AUTHENTICATION_CWSIT0128",
                                                                                                  new Object[] { cap.getBusName(), ep.getEndPointInfo(),
                                                                                                                Utils.getFailureMessage(cbr.getFailureReason()) }, null), null,
                                                                          false);
                        // SIAuthenticationExc is not permitted to have a linked exception, and in basically
                        // all cases the cbh.getException() will be null because it is only set by protocol
                        // exceptions.

                        // d347701 - Make sure we tidy up the connection if we aren't going to use it.
                        cc.close();
                        break;
                    }

                    // no point checking other bootstrap service if not authorised - break out of while loop
                    if (rc == TrmConstantsImpl.RETURN_CODE_SINotAuthorizedException) {
                        firstMirroredFailureException = generateException(rc,
                                                                          nls.getFormattedMessage("LIBERTY_NOT_AUTHORIZED_CWSIT0129",
                                                                                                  new Object[] { cap.getBusName(), ep.getEndPointInfo(),
                                                                                                                Utils.getFailureMessage(cbr.getFailureReason()) }, null), null,
                                                                          false);
                        // NotAuthorizedExc is not permitted to have a linked exception, and in basically
                        // all cases the cbh.getException() will be null because it is only set by protocol
                        // exceptions.

                        // d347701 - Make sure we tidy up the connection if we aren't going to use it.
                        cc.close();
                        break;
                    }

                    if (rc == TrmConstantsImpl.RETURN_CODE_OK) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Made a bootstrap request to "
                                            + ep.getEndPointInfo() + ". Response was "
                                            + (rc == TrmConstantsImpl.RETURN_CODE_OK ? "OK" : "REDIRECT"));

                        goodReply = true;

      
                        break;
                    } else {
                        Object[] objs = new Object[] {
                                                      ep.getEndPointInfo(), cap.getBusName(), rc == TrmConstantsImpl.RETURN_CODE_REDIRECT ? "REDIRECT TWAS" : Utils.getFailureMessage(cbr.getFailureReason()) };
                        //SibTr.warning(tc, Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS, "LIBERTY_MIRRORED_FAILURE_CWSIT0130", objs);
                        Exception exception = generateException(
                                                                rc, nls.getFormattedMessage("LIBERTY_MIRRORED_FAILURE_CWSIT0130", objs, null), cbh.getException(), false);
                        SibTr.exception(tc, exception);
                        // store the first Exception of this kind
                        if (null == firstMirroredFailureException) {
                            firstMirroredFailureException = exception;
                        }
                        failedBootstrapServers.add(ep.getEndPointInfo()); //250165

                        // d273578 - Close the connection if we decide it is inappropriate to use it.
                        cc.close();
                    }
                } catch (Exception e) {
                    // No FFDC code needed
                    Object[] objs = new Object[] { ep.getEndPointInfo(), e.toString() };
                    //SibTr.warning(tc, Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS, "LIBERTY_BOOTSTRAP_FAILED_CWSIT0131", objs); //248969
                    SIResourceException sire = new SIResourceException(nls.getFormattedMessage(
                                                                                               "LIBERTY_BOOTSTRAP_FAILED_CWSIT0131", objs, null), e);
                    SibTr.exception(tc, sire);
                    // store the first Exception of this kind
                    if (null == firstBootstrapFailureException) {
                        firstBootstrapFailureException = sire;
                    }
                    uncontactableBootstrapServers.add(ep.getEndPointInfo()); //250165
                } // try

            } else {
                throw new SIErrorException(nls.getString("NO_CC_CWSIT0005"));
            } // if (cc != null)
        } // while

        // create BootstrapResponse
        ContactBootstrapServiceResponse cbsr = new ContactBootstrapServiceResponse(cc, cbr, rc, uncontactableBootstrapServers, failedBootstrapServers, firstMirroredFailureException, firstBootstrapFailureException, goodReply, ep);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "contactBootstrapService", cbsr);

        return cbsr;
    }

    private int getRC(Exception linkedException) {

        int rc = TrmConstantsImpl.RETURN_CODE_SIResourceException; // default to
        // SIResourceException

        if (null != linkedException) {
            if (linkedException instanceof SIConnectionLostException) {
                rc = TrmConstantsImpl.RETURN_CODE_SIConnectionLostException;
            } else if (linkedException instanceof SILimitExceededException) {
                rc = TrmConstantsImpl.RETURN_CODE_SILimitExceededException;
            } else if (linkedException instanceof SIErrorException) {
                rc = TrmConstantsImpl.RETURN_CODE_SIErrorException;
            } else if (linkedException instanceof SINotAuthorizedException) {
                rc = TrmConstantsImpl.RETURN_CODE_SINotAuthorizedException;
            } else if (linkedException instanceof SINotPossibleInCurrentConfigurationException) {
                rc = TrmConstantsImpl.RETURN_CODE_SINotPossibleInCurrentConfigurationException;
            } else if (linkedException instanceof SIIncorrectCallException) {
                rc = TrmConstantsImpl.RETURN_CODE_SIIncorrectCallException;
            } else if (linkedException instanceof SIAuthenticationException) {
                rc = TrmConstantsImpl.RETURN_CODE_SIAuthenticationException;
            }
        }

        return rc;
    }

    // Map the return code to an exception and return/throw it as required

    private Exception generateException(int rc, String text,
                                        Exception linkedException, boolean throwIt)
                    throws SIResourceException, SIConnectionLostException,
                    SILimitExceededException, SIErrorException, SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException, SIAuthenticationException {

        Exception theException;

        if (rc == TrmConstantsImpl.RETURN_CODE_SIResourceException) {
            SIResourceException sire = new SIResourceException(text);
            if (null != linkedException) {
                sire.initCause(linkedException);
            }
            if (throwIt) {
                throw sire;
            } else {
                theException = sire;
            }
        } else if (rc == TrmConstantsImpl.RETURN_CODE_SIConnectionLostException) {
            SIConnectionLostException sicle = new SIConnectionLostException(text);
            if (null != linkedException) {
                sicle.initCause(linkedException);
            }
            if (throwIt) {
                throw sicle;
            } else {
                theException = sicle;
            }
        } else if (rc == TrmConstantsImpl.RETURN_CODE_SILimitExceededException) {
            SILimitExceededException silee = new SILimitExceededException(text);
            if (null != linkedException) {

            }
            if (throwIt) {
                throw silee;
            } else {
                theException = silee;
            }
        } else if (rc == TrmConstantsImpl.RETURN_CODE_SIErrorException) {
            SIErrorException siee = new SIErrorException(text);
            if (null != linkedException) {
                siee.initCause(linkedException);
            }
            if (throwIt) {
                throw siee;
            } else {
                theException = siee;
            }
        } else if (rc == TrmConstantsImpl.RETURN_CODE_SINotAuthorizedException) {
            SINotAuthorizedException sinae = new SINotAuthorizedException(text);
            if (null != linkedException) {
                // This is not permitted!

            }
            if (throwIt) {
                throw sinae;
            } else {
                theException = sinae;
            }
        } else if (rc == TrmConstantsImpl.RETURN_CODE_SINotPossibleInCurrentConfigurationException) {
            SINotPossibleInCurrentConfigurationException sinpicce = new SINotPossibleInCurrentConfigurationException(
                            text);
            if (null != linkedException) {
                sinpicce.initCause(linkedException);
            }
            if (throwIt) {
                throw sinpicce;
            } else {
                theException = sinpicce;
            }
        } else if (rc == TrmConstantsImpl.RETURN_CODE_SIIncorrectCallException) {
            SIIncorrectCallException siice = new SIIncorrectCallException(text);
            if (null != linkedException) {
                siice.initCause(linkedException);
            }
            if (throwIt) {
                throw siice;
            } else {
                theException = siice;
            }
        } else if (rc == TrmConstantsImpl.RETURN_CODE_SIAuthenticationException) {
            SIAuthenticationException siae = new SIAuthenticationException(text);
            if (null != linkedException) {
                // This is not permitted!
            }
            if (throwIt) {
                throw siae;
            } else {
                theException = siae;
            }
        } else {
            SIErrorException siee = new SIErrorException(text);
            if (null != linkedException) {
                siee.initCause(linkedException);
            }
            if (throwIt) {
                throw siee;
            } else {
                theException = siee;
            }
        }

        return theException;
    }

    /*
     * Inner class
     * return object for contactBootstrapService()
     */
    static class ContactBootstrapServiceResponse {
        /*
         * variables
         */
        private final ClientConnection cc;
        private final TrmClientBootstrapReply cbr;
        private final int rc;
        private final List uncontactableBootstrapServers;
        private final List failedBootstrapServers;
        private final Exception firstMirroredFailureException;
        private final Exception firstBootstrapFailureException;
        private final boolean goodReply;
        private final ProviderEndPoint ep;

        /*
         * constructor
         */
        ContactBootstrapServiceResponse(ClientConnection cc, TrmClientBootstrapReply cbr, int rc,
                                        List uncontactableBootstrapServers, List failedBootstrapServers,
                                        Exception firstMirroredFailureException, Exception firstBootstrapFailureException,
                                        boolean goodReply, ProviderEndPoint ep) {
            this.cc = cc;
            this.cbr = cbr;
            this.rc = rc;
            this.uncontactableBootstrapServers = uncontactableBootstrapServers;
            this.failedBootstrapServers = failedBootstrapServers;
            this.firstMirroredFailureException = firstMirroredFailureException;
            this.firstBootstrapFailureException = firstBootstrapFailureException;
            this.goodReply = goodReply;
            this.ep = ep;
        }

        /*
         * accessor methods
         */
        ClientConnection getClientConnection() {
            return cc;
        }

        TrmClientBootstrapReply getClientBootstrapReply() {
            return cbr;
        }

        int getReturnCode() {
            return rc;
        }

        List getFailedBootstrapServers() {
            return failedBootstrapServers;
        }

        List getUncontactableBootstrapServers() {
            return uncontactableBootstrapServers;
        }

        Exception getFirstMirroredFailureException() {
            return firstMirroredFailureException;
        }

        Exception getFirstBootstrapFailureException() {
            return firstBootstrapFailureException;
        }

        boolean isGoodReply() {
            return goodReply;
        }

        ProviderEndPoint getEndPoint() {
            return ep;
        }

        /*
         * utility methods
         */
        @Override
        public String toString()
        {
            return "cc=" + cc + ",cbr=" + cbr + ",rc=" + rc
                   + ",uncontactableBootstrapServers=" + uncontactableBootstrapServers
                   + ",failedBootstrapServers=" + failedBootstrapServers
                   + ",firstMirroredFailureException=" + firstMirroredFailureException
                   + ",firstBootstrapFailureException=" + firstBootstrapFailureException
                   + ",goodReply=" + goodReply + ",ep=" + ep.getEndPointInfo();
        }
    }

    @Override
    public SICoreConnection createConnection(ClientConnection cc,
                                             String credentialType, String userid, String password)
                    throws SIResourceException, SINotAuthorizedException,
                    SIAuthenticationException {
        // Basically this method will not be called
        // It is there in the interface just to be called by the Comms component
        // to MessageProcessor
        if (tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "createConnection", new Object[] { cc,
                                                                    credentialType, userid, password });
        }
        SibTr.error(tc, "This method should not have been called");
        if (tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "createConnection", null);
        }
        return null;
    }

}
