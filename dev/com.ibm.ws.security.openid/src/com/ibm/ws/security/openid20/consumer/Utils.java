/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.consumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openid20.OpenidConstants;
import com.ibm.ws.security.openid20.OpenidClientConfig;
import com.ibm.ws.security.openid20.internal.UserInfo;
import com.ibm.ws.security.openid20.TraceConstants;

/**
 * This is the utils class the will be used by the OpenidAuthenticatorImpl.
 */
public class Utils {
    static final TraceComponent tc = Tr.register(Utils.class);

    private OpenidClientConfig openidClientConfig;
    private int maxDiscoverRetry;

    Utils(OpenidClientConfig openidClientConfig) {
        this.openidClientConfig = openidClientConfig;
        maxDiscoverRetry = openidClientConfig.getMaxDiscoverRetry();
    }

    /**
     * This method process he verification failed from the OP
     * 
     * @param verificationResult
     * @param discoveryInfo
     * @throws IOException
     */
    public void verificationFailed(VerificationResult verificationResult, DiscoveryInformation discoveryInfo) throws IOException {
        String statusMsg = verificationResult.getStatusMsg();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "verification result status message from ConsumerManager: ", statusMsg);
        }

        String identifier = null;
        Identifier claimIdentifier = discoveryInfo.getClaimedIdentifier();
        if (claimIdentifier != null)
            identifier = claimIdentifier.getIdentifier();
        if (identifier != null) {
            Tr.error(tc, "OPENID_RP_NO_RESULT_ERR", identifier);
            if (statusMsg == null) {
                statusMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         TraceConstants.MESSAGE_BUNDLE,
                                                         "OPENID_RP_NO_RESULT_ERR",
                                                         new Object[] { identifier },
                                                         "CWWKS1506E: OpenID can not get a valid result for claim identifier {0}.");
            }
        } else {
            String opEndPoint = discoveryInfo.getOPEndpoint().toString();
            Tr.error(tc, "OPENID_RP_CAN_NOT_ACCESS_OP", opEndPoint);
            if (statusMsg == null) {
                statusMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         TraceConstants.MESSAGE_BUNDLE,
                                                         "OPENID_RP_CAN_NOT_ACCESS_OP",
                                                         new Object[] { opEndPoint },
                                                         "CWWKS1511E: Cannot access the OpenID provider {0}");
            }
        }

        throw new IOException(statusMsg);
    }

    /**
     * This method will get the open ID provider end point URL as a string
     * 
     * @param discovered
     * @param authSuccess
     * @param attributes
     * @return
     */
    public String getOpEndPoint(DiscoveryInformation discovered, AuthSuccess authSuccess, Map<String, Object> attributes) {
        String opEndPoint = authSuccess.getOpEndpoint();
        if (opEndPoint == null) {
            opEndPoint = discovered.getOPEndpoint().toString();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Get OpEndPoint from discovered object =" + opEndPoint);
            }
        }
        attributes.put("OpEndPoint", opEndPoint);
        return opEndPoint;
    }

    /**
     * @param req
     * @return
     */
    public String getReceivingUrl(HttpServletRequest req) {
        StringBuffer receivingURL = req.getRequestURL();
        String queryString = req.getQueryString();
        if (queryString != null && queryString.length() > 0)
            receivingURL.append(OpenidConstants.QUESTIONMARK).append(req.getQueryString());

        return receivingURL.toString();
    }

    /**
     * This method will revolve the map user name.
     * 
     * @param authSuccess
     * @param attributes
     * @return
     */
    public String resolveMapUserName(AuthSuccess authSuccess, Map<String, Object> attributes) {

        if (openidClientConfig.isUseClientIdentity()) {
            return getIdentityOrClaimedId(authSuccess);
        }

        String alias = getUserMappingFromUserInfo(attributes);

        if (alias == null) {
            alias = getIdentityOrClaimedId(authSuccess);
        }
        return alias;
    }

    /**
     * This method will will go through the user info up to searchNumberOfUserInfoToMap to find the alias
     * that can be used to create the client subject.
     * 
     * @param attributes
     * @return
     */
    protected String getUserMappingFromUserInfo(Map<String, Object> attributes) {
        List<UserInfo> userInfoList = openidClientConfig.getUserInfo();
        if (userInfoList == null || userInfoList.isEmpty())
            return null;
        String alias = null;
        int max = openidClientConfig.getSearchNumberOfUserInfoToMap();
        for (int i = 0; i < max; i++) {
            UserInfo userInfo = userInfoList.get(i);
            String key = userInfo.getAlias();
            if (key != null) {
                ArrayList<?> value = (ArrayList<?>) attributes.get(key);
                if (value != null) {
                    alias = (String) value.get(0);
                    if (alias != null)
                        break;
                }
            }
        }

        return alias;
    }

    /**
     * This method will retrieve the openId identify. If there is no openId identify, then
     * it will retrieve the claim identify
     * 
     * @param authSuccess
     * @return
     */
    protected String getIdentityOrClaimedId(AuthSuccess authSuccess) {
        String alias = authSuccess.getIdentity();
        if (alias == null) {
            alias = authSuccess.getClaimed();
        }
        return alias;
    }

    /**
     * This method discover the openID provider
     * 
     * @param consumerManager
     * @param identifier
     * @return
     * @throws IOException
     */
    public DiscoveryInformation discoverOpenID(ConsumerManager consumerManager, String identifier) throws IOException {
        List<?> discoveries = null;

        discoveries = tryToDiscoverOpenID(consumerManager, identifier, discoveries);

        DiscoveryInformation discovered = consumerManager.associate(discoveries);

        if (openidClientConfig.ishttpsRequired()) {
            String protocol = discovered.getOPEndpoint().getProtocol();
            if (!"https".equals(protocol)) {
                Tr.error(tc, "OPENID_OP_URL_PROTOCOL_NOT_HTTPS", protocol);
                throw new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "OPENID_OP_URL_PROTOCOL_NOT_HTTPS",
                                                                   new Object[] { protocol },
                                                                   "CWWKS1510E: The relying party requires SSL but the openID provider URL protocol is {0}."));
            }
        }
        String version = discovered.getVersion();
        if (!discovered.isVersion2() && tc.isDebugEnabled()) {
            Tr.warning(tc, "OPENID_VERSION_NOT_TEST", version);
//            throw new IOException(TraceNLS.getFormattedMessage(this.getClass(),
//                                                               TraceConstants.MESSAGE_BUNDLE,
//                                                               "OPENID_VERSION_NOT_TEST",
//                                                                new Object[] {version},
//                                                               "CWWKS1505W: OpenID provider version {0} was not tested so it may not work properly."));

        }

        return discovered;
    }

    /**
     * This method try to to discover the open identifier up to number of max discover retry.
     * 
     * @param consumerManager
     * @param identifier
     * @param discoveries
     * @return
     * @throws IOException
     */
    protected List<?> tryToDiscoverOpenID(ConsumerManager consumerManager, String identifier, List<?> discoveries) throws IOException {
        int retries = 0;
        while (retries < maxDiscoverRetry) {
            try {
                retries++;
                discoveries = consumerManager.discover(identifier);
                break;
            } catch (DiscoveryException e) {
                if (retries == maxDiscoverRetry) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Hit maxDiscoverRetry (" + retries + ") allowed to discover...");
                    }

                    Tr.error(tc, "OPENID_RP_CAN_NOT_ACCESS_OP", identifier);
                    throw new IOException(e.getLocalizedMessage());
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Number call of discover(): " + retries);
        }

        return discoveries;
    }

    /**
     * This method create the return URL that the openId provider will return to after the
     * OP authentication
     * 
     * @param req
     * @param uniqueKey
     * @return
     * @throws IOException
     */
    public String createReturnToUrl(HttpServletRequest req, String uniqueKey) throws IOException {
        String result = null;
        StringBuffer builder = req.getRequestURL();
        builder.append("?");
        builder.append(OpenidConstants.RP_REQUEST_IDENTIFIER).append("=").append(uniqueKey);
        result = builder.toString();

        return result;
    }

    /**
     * This method get the openID RP realm
     * 
     * @param req
     * @return
     */
    public String getRpRealm(HttpServletRequest req) {
        StringBuilder builder = new StringBuilder(128);
        builder.append(req.getScheme());
        builder.append("://");
        builder.append(req.getServerName());
        int port = req.getServerPort();
        if (port != OpenidConstants.HTTP_STD_PORT && port != OpenidConstants.HTTPS_STD_PORT) {
            builder.append(":");
            builder.append(req.getServerPort());
        }
        builder.append(req.getContextPath());

        return builder.toString();
    }

    /**
     * This method add additional user infor attribute in the authRequest
     * 
     * @param authReq
     * @throws Exception
     */
    public void addUserInfoAttributes(AuthRequest authReq) throws Exception {
        FetchRequest fetch = FetchRequest.createFetchRequest();
        ArrayList<UserInfo> attributes = (ArrayList<UserInfo>) openidClientConfig.getUserInfo();
        if (attributes != null && !attributes.isEmpty()) {
            Iterator<UserInfo> it = attributes.iterator();
            while (it.hasNext()) {
                UserInfo att = it.next();
                fetch.addAttribute(att.getAlias(), att.getType(), att.getRequired(), att.getCount());
            }
            authReq.addExtension(fetch);
        }
    }

    /**
     * This method retrieve additional user infor from the OP response
     * 
     * @param authSuccess
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> receiveUserInfoAttributes(AuthSuccess authSuccess) throws IOException {
        Map<String, Object> result = new HashMap<String, Object>();
        if (authSuccess.hasExtension(OpenidConstants.OPENID_NS_AX)) {
            MessageExtension ext = null;
            try {
                ext = authSuccess.getExtension(OpenidConstants.OPENID_NS_AX);
            } catch (MessageException e) {
                throw new IOException(e.getLocalizedMessage());
            }
            if (ext instanceof FetchResponse) {
                FetchResponse axResponse = (FetchResponse) ext;
                result = axResponse.getAttributes();
            }
        }
        return result;
    }

    /*
     * Get the provider realm from the realmIdentifier. If the realmIdentifier is null or return value is null,
     * then we will use the openId client WebSphere user registry realm name.
     */
    public String getRealmName(OpenidClientConfig openidClientConfig, Map<String, Object> attributes) {
        String realm = null;
        String realmIdentifier = openidClientConfig.getRealmIdentifier();
        if (realmIdentifier != null && !realmIdentifier.isEmpty()) {
            ArrayList<?> values = (ArrayList<?>) attributes.get(realmIdentifier);
            if (values != null && !values.isEmpty()) {
                realm = (String) values.get(0);
            }
        }
        return realm;
    }

    /*
     * If the realm name is null, then we return the groups not unique group id. The authorization code will convert
     * the group into unique group id.
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getGroups(OpenidClientConfig openidClientConfig, Map<String, Object> attributes, String realm) {
        ArrayList<String> groups = new ArrayList<String>();
        String groupIdentifier = openidClientConfig.getGroupIdentifier();
        if (groupIdentifier != null) {
            ArrayList<?> values = (ArrayList<?>) attributes.get(groupIdentifier);
            if (realm == null || realm.isEmpty())
                return (ArrayList<String>) values;

            if (values != null && !values.isEmpty()) {
                Iterator<?> it = values.iterator();
                while (it.hasNext()) {
                    String group = new StringBuffer("group:").append(realm).append("/").append(it.next()).toString();
                    groups.add(group);
                }
            }
        }
        return groups;
    }
}
