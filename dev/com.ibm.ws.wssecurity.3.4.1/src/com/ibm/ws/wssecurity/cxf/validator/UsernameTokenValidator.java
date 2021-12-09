/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.cxf.validator;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.xml.security.utils.XMLUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;

/**
 * This class validates a processed UsernameToken, extracted from the Credential passed to
 * the validate method.
 */
public class UsernameTokenValidator implements Validator {
    protected static final TraceComponent tc = Tr.register(UsernameTokenValidator.class,
                                                           WSSecurityConstants.TR_GROUP,
                                                           WSSecurityConstants.TR_RESOURCE_BUNDLE);

    private static SecurityService securityService = null;

    public static void setSecurityService(SecurityService serv) {
        securityService = serv;
    }

    public static SecurityService getSecurityService() {
        return securityService;
    }

    /**
     * Validate the credential argument. It must contain a non-null UsernameToken. A
     * CallbackHandler implementation is also required to be set.
     * 
     * If the password type is either digest or plaintext, it extracts a password from the
     * CallbackHandler and then compares the passwords appropriately.
     * 
     * If the password is null it queries a hook to allow the user to validate UsernameTokens
     * of this type.
     * 
     * @param credential the Credential to be validated
     * @param data the RequestData associated with the request
     * @throws WSSecurityException on a failed validation
     */
    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (credential == null || credential.getUsernametoken() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCredential");
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "RequestData ClassName:" + data.getClass().getName());
            Object msg = data.getMsgContext();
            if (msg != null) {
                Tr.debug(tc, "MsgContext ClassName:" + msg.getClass().getName());
            } else {
                Tr.debug(tc, "MsgContext**** ClassName**** is null");
            }
        }

        boolean handleCustomPasswordTypes = false;
        boolean passwordsAreEncoded = false;
        String requiredPasswordType = null;
        WSSConfig wssConfig = null; //@2020
        if (data != null) {
            wssConfig = data.getWssConfig();
            handleCustomPasswordTypes = data.isHandleCustomPasswordTypes();//wssConfig.getHandleCustomPasswordTypes();
            //passwordsAreEncoded = wssConfig.getPasswordsAreEncoded();
            passwordsAreEncoded = data.isEncodePasswords();
            //requiredPasswordType = wssConfig.getRequiredPasswordType();        
            requiredPasswordType = data.getRequiredPasswordType();
        }

        org.apache.wss4j.dom.message.token.UsernameToken usernameToken = credential.getUsernametoken();
        this.validateCreated(usernameToken, data);

        usernameToken.setPasswordsAreEncoded(passwordsAreEncoded);

        String pwType = usernameToken.getPasswordType();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "UsernameToken user " + usernameToken.getName());
            Tr.debug(tc, "UsernameToken password type " + pwType);
        }

        if (requiredPasswordType != null && !requiredPasswordType.equals(pwType)) {
            Tr.error(tc, "password_type_mismatch", pwType, requiredPasswordType);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        //
        // If the UsernameToken is hashed or plaintext, then retrieve the password from the
        // callback handler and compare directly. If the UsernameToken is of some unknown type,
        // then delegate authentication to the callback handler
        //
        String password = usernameToken.getPassword();
        if (usernameToken.isHashed()) {
            verifyDigestPassword(usernameToken, data);
        } else if (WSConstants.PASSWORD_TEXT.equals(pwType)
                   || (password != null && (pwType == null || "".equals(pwType.trim())))) {
            verifyPlaintextPassword(usernameToken, data);
        } else if (password != null) {
            if (!handleCustomPasswordTypes) {
                Tr.error(tc, "cannot_handle_custom_password_types");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
            verifyCustomPassword(usernameToken, data);
        } else {
            verifyUnknownPassword(usernameToken, data);
        }
        return credential;
    }

    /**
     * Verify a UsernameToken containing a password of some unknown (but specified) password
     * type. It does this by querying a CallbackHandler instance to obtain a password for the
     * given username, and then comparing it against the received password.
     * This method currently uses the same logic as the verifyPlaintextPassword case, but it in
     * a separate protected method to allow users to override the validation of the custom
     * password type specific case.
     * 
     * @param usernameToken The UsernameToken instance to verify
     * @throws WSSecurityException on a failed authentication.
     */
    protected void verifyCustomPassword(@Sensitive org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
                                        RequestData data) throws WSSecurityException {
        verifyPlaintextCustomPassword(usernameToken, data);
    }

    /**
     * Verify a UsernameToken containing a plaintext password. It does this by querying a
     * CallbackHandler instance to obtain a password for the given username, and then comparing
     * it against the received password.
     * This method currently uses the same logic as the verifyDigestPassword case, but it in
     * a separate protected method to allow users to override the validation of the plaintext
     * password specific case.
     * 
     * @param usernameToken The UsernameToken instance to verify
     * @throws WSSecurityException on a failed authentication.
     */
    protected void verifyPlaintextPassword(@Sensitive org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
                                           RequestData data) throws WSSecurityException {

        String user = null;
        String password = null;

        user = usernameToken.getName();
        String pwType = usernameToken.getPasswordType();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "UsernameToken user " + usernameToken.getName());
            Tr.debug(tc, "UsernameToken password type " + pwType);
        }

        password = usernameToken.getPassword();

        if (!WSConstants.PASSWORD_TEXT.equals(pwType)) {
            Tr.error(tc, "password_type_mismatch", pwType, WSConstants.PASSWORD_TEXT);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        if (!(user != null && user.length() > 0 && password != null && password.length() > 0)) {
            Tr.error(tc, "empty_user_or_password");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        checkUserAndPassword(user, password);
    }

    //Checks a different type of password from Text and Digest
    protected void verifyPlaintextCustomPassword(@Sensitive org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
                                                 RequestData data) throws WSSecurityException {

        String user = null;
        String password = null;

        user = usernameToken.getName();
        String pwType = usernameToken.getPasswordType();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "UsernameToken user " + usernameToken.getName());
            Tr.debug(tc, "UsernameToken password type " + pwType);
        }

        password = usernameToken.getPassword();

        if (!(user != null && user.length() > 0 && password != null && password.length() > 0)) {
            Tr.error(tc, "empty_user_or_password");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        checkUserAndPassword(user, password);
    }

    protected String checkUserAndPassword(String user, @Sensitive String password) throws WSSecurityException {
        //System.out.println("gkuo:debug:Id:" + user + ":password:" + password );
        String authnPrincipal = null;
        try {
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            UserRegistry userRegistry = userRegistryService.getUserRegistry();
            //String realm = userRegistry.getRealm();
            authnPrincipal = userRegistry.checkPassword(user, password);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Authenticated principal for " + user + " is  " + authnPrincipal);
            }

        } catch (RegistryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id for " + user + ": " + e);
            }
            Tr.error(tc, "registry_exception_checking_password", user, e.getLocalizedMessage());
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e);
        }

        // Let's validate the user and 
        if (authnPrincipal == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User " + user + " could not be validated.");
            }
            Tr.error(tc, "check_password_failed", user);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "UsernameToken for " + user + " has been validated.");
        }

        return authnPrincipal;
    }

    protected boolean checkUser(String user) throws WSSecurityException {
        boolean isValid = false;
        try {
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            UserRegistry userRegistry = userRegistryService.getUserRegistry();
            //String realm = userRegistry.getRealm();
            isValid = userRegistry.isValidUser(user);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User " + user + " is valid " + isValid);

            }
            if (!isValid) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }

        } catch (RegistryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id for " + user + ": " + e);
            }
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, e);
        }
        return true;
    }

    /**
     * Verify a UsernameToken containing a password digest. It does this by querying a
     * CallbackHandler instance to obtain a password for the given username, and then comparing
     * it against the received password.
     * 
     * @param usernameToken The UsernameToken instance to verify
     * @throws WSSecurityException on a failed authentication.
     */
    protected void verifyDigestPassword(@Sensitive org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
                                        RequestData data) throws WSSecurityException {
        if (data.getCallbackHandler() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCallback");
        }

        String user = usernameToken.getName();
        String password = usernameToken.getPassword();
        String nonce = usernameToken.getNonce();
        String createdTime = usernameToken.getCreated();
        String pwType = usernameToken.getPasswordType();
        boolean passwordsAreEncoded = usernameToken.getPasswordsAreEncoded();
        WSPasswordCallback pwCb = new WSPasswordCallback(user, null, pwType, WSPasswordCallback.USERNAME_TOKEN);//new WSPasswordCallback(user, null, pwType, WSPasswordCallback.USERNAME_TOKEN, data); //@2020
        try {
            data.getCallbackHandler().handle(new Callback[] { pwCb });
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, e.getMessage());
            }
            //new Exception(e,WSSecurityException.FAILED_AUTHENTICATION_ERR)
            WSSecurityException wsse = new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION/*, e*/);
        } catch (UnsupportedCallbackException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, e.getMessage());
            }
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e);
        }

        String origPassword = pwCb.getPassword();
        if (origPassword == null) {
            Tr.error(tc, "no_password_returned_by_callback");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        if (usernameToken.isHashed()) {
            byte[] decodedNonce = XMLUtils.decode(nonce);
            String passDigest;
            if (passwordsAreEncoded) {
                passDigest = UsernameTokenUtil.doPasswordDigest(decodedNonce, createdTime, XMLUtils.decode(origPassword));
            } else {
                passDigest = UsernameTokenUtil.doPasswordDigest(decodedNonce, createdTime, origPassword);
            }
            if (!passDigest.equals(password)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
        } else {
            if (!origPassword.equals(password)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
        }

        this.checkUserAndPassword(user, origPassword);
    }

    /**
     * Verify a UsernameToken containing no password. This does nothing - but is in a separate
     * method to allow the end-user to override validation easily.
     * 
     * @param usernameToken The UsernameToken instance to verify
     * @throws WSSecurityException on a failed authentication.
     */
    protected void verifyUnknownPassword(@Sensitive org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
                                         RequestData data) throws WSSecurityException {

        String pwType = usernameToken.getPasswordType();
        if (pwType == null) {
            boolean bPolicyNoPasswordSet = false;
            Object msgContext = data.getMsgContext();
            if (msgContext != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "MsgContext ClassName:" + msgContext.getClass().getName());
                }
                if (msgContext instanceof org.apache.cxf.binding.soap.SoapMessage) {
                    bPolicyNoPasswordSet = Utils.checkPolicyNoPassword((org.apache.cxf.binding.soap.SoapMessage) msgContext);
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Policy NoPassword is " + bPolicyNoPasswordSet);
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "MsgContext**** is null");
                }
            }
            if (bPolicyNoPasswordSet || usernameToken.isDerivedKey()) {
                String user = usernameToken.getName();
                if (!checkUser(user)) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
                }
            } else {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }

        }
    }

    protected void validateCreated(@Sensitive org.apache.wss4j.dom.message.token.UsernameToken usernameToken, RequestData data) throws WSSecurityException {

        String created = usernameToken.getCreated();
        if (created == null || created.isEmpty()) {
            return;
        }

        if (data.getWssConfig() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE);
        }

        WSSConfig wssConfig = data.getWssConfig();

        int timeStampTTL = 300;
        int futureTimeToLive = 300;
        if (wssConfig != null) {
            //timeStampTTL = wssConfig.getTimeStampTTL(); //@2020
            timeStampTTL = data.getUtTTL(); //v3 TODO
            //futureTimeToLive = wssConfig.getTimeStampFutureTTL(); //@2020
            futureTimeToLive = data.getUtFutureTTL(); //v3 TODO
        }

        boolean isValid = verifyCreated(created, timeStampTTL, futureTimeToLive);
        if (!isValid) {
            throw new WSSecurityException(
                            WSSecurityException.ErrorCode.MESSAGE_EXPIRED);
            //"error.policy.invalidcreated",
            //new Object[] { "The security semantics of the message have expired" });
        }

    }

    protected boolean verifyCreated(String created,
                                    int timeToLive,
                                    int futureTimeToLive) throws WSSecurityException {
        Date validCreation = new Date();
        long currentTime = validCreation.getTime();
        if (futureTimeToLive > 0) {
            validCreation.setTime(currentTime + (futureTimeToLive * 1000L));
        }

        //Use UTC to convert to Date
        // 2012-10-30T19:08:28.615Z
        Date createdDate = convertDate(created);
        // Check to see if the created time is in the future
        if (createdDate != null && createdDate.after(validCreation)) {

            return false;
        }

        // Calculate the time that is allowed for the message to travel
        currentTime -= (timeToLive * 1000L);
        validCreation.setTime(currentTime);

        // Validate the time it took the message to travel
        if (createdDate != null && createdDate.before(validCreation)) {

            return false;
        }

        return true;
    }

    public static Date convertDate(String strTimeStamp) throws WSSecurityException {
        Date date;
        try {
            //System.out.println("Original string: " + strTimeStamp);

            // "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            date = dateFormat.parse(strTimeStamp);

            //System.out.println("Parsed date    : " + date.toString());
            //System.out.println("DateFormated   : " + dateFormat.format(date));
            //System.out.println("DateGMT        : " + date.toGMTString());

        } catch (ParseException pe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception while parse a timestamp as '" + strTimeStamp + "' : " + pe);
            }
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, pe);
        }
        return date;
    }

}
