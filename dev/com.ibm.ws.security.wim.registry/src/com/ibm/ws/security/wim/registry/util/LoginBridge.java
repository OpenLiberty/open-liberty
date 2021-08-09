/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.registry.util;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.security.wim.util.PasswordUtil;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.wim.registry.dataobject.IDAndRealm;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.LoginControl;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * Bridge class for mapping login and mapCertificate methods.
 *
 */
public class LoginBridge {

    private static final TraceComponent tc = Tr.register(LoginBridge.class);

    /**
     * Property mappings.
     */
    private TypeMappings propertyMap = null;

    /**
     * Mappings utility class.
     */
    private BridgeUtils mappingUtils = null;

    /**
     * Default constructor.
     *
     * @param mappingUtil
     */
    public LoginBridge(BridgeUtils mappingUtil) {
        this.mappingUtils = mappingUtil;
        propertyMap = new TypeMappings(mappingUtil);
    }

    @FFDCIgnore(WIMException.class)
    public String checkPassword(String inputUser, @Sensitive String inputPassword) throws RegistryException {

        // initialize the return value
        StringBuffer returnValue = new StringBuffer();
        // bridge the APIs
        try {
            // validate the id
            this.mappingUtils.validateId(inputUser);
            // separate the user and realm from the userSecurityName
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm(inputUser);
            // create the root DataObject
            Root root = this.mappingUtils.getWimService().createRootObject();
            // if realm is defined
            if (idAndRealm.isRealmDefined()) {
                // set "WIM.Realm" in the Context DataGraph to the realm
                this.mappingUtils.createRealmDataObject(root, idAndRealm.getRealm());
                List<Context> contexts = root.getContexts();
                if (contexts != null) {
                    Context ctx = new Context();
                    ctx.setKey(Service.CONFIG_PROP_ALLOW_OPERATION_IF_REPOS_DOWN);
                    ctx.setValue(Boolean.valueOf(this.mappingUtils.getCoreConfiguration().isAllowOpIfRepoDown(idAndRealm.getRealm())));
                    contexts.add(ctx);
                }
            }

            // add MAP(userSecurityName) to the return list of properties
            // f113366
            String outputAttrName = this.propertyMap.getOutputUserSecurityName(idAndRealm.getRealm());
            if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                this.mappingUtils.createLoginControlDataObject(root, outputAttrName);
            } else {
                LoginControl ctrl = new LoginControl();
                ctrl.setCountLimit(this.mappingUtils.getCoreConfiguration().getMaxSearchResults() + 1);
                root.getControls().add(ctrl);
            }

            // use the root DataGraph to create a LoginAccount DataGraph
            List<Entity> entities = root.getEntities();
            LoginAccount loginAct = new LoginAccount();
            if (entities != null) {
                entities.add(loginAct);
            }
            // set the MAP(userSecurityName) property to the user
            // f113366
            loginAct.setPrincipalName(idAndRealm.getId());
            // set the "userPassword" property to the password
            // f113179
            loginAct.setPassword(PasswordUtil.getByteArrayPassword(inputPassword));

            // invoke ProfileService.login with the input root DataGraph
            root = this.mappingUtils.getWimService().login(root);
            // set the user to the value of MAP(userUniqueId) from the output DataGraph
            List<Entity> returnList = root.getEntities();
            // the user was not authenticated
            if (returnList.isEmpty()) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(inputUser));
                if (tc.isErrorEnabled()) {
                    Tr.error(tc, msg);
                }
                throw new PasswordCheckFailedException(WIMMessageKey.ENTITY_NOT_FOUND, msg);
            }
            // the user was authenticated
            else {
                Entity entity = returnList.get(0);
                // f113366
                // add MAP(userSecurityName) to the return list of properties
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    Object value = entity.get(outputAttrName);
                    if (value instanceof List<?>) {
                        returnValue.append(BridgeUtils.getStringValue(((List<?>) value).get(0)));
                    } else {
                        returnValue.append(BridgeUtils.getStringValue(value));
                    }
                } else {
                    // d113681
                    returnValue.append(BridgeUtils.getStringValue(entity.getIdentifier().get(outputAttrName)));
                }
            }
            // if realm is defined
            // d109969
            if ((idAndRealm.isRealmDefined())
                && (!this.mappingUtils.getDefaultRealmName().equals(idAndRealm.getRealm()))) {
                returnValue.append(idAndRealm.getDelimiter() + idAndRealm.getRealm());
            }
        } catch (WIMException toCatch) {
            // log the Exception
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, toCatch.getMessage(), toCatch);
            }
            if (tc.isErrorEnabled()) {
                Tr.error(tc, toCatch.getMessage(), toCatch);
            }
            // the user/password is invalid
            //TODO:: PasswordCheckFailedException is not there in the ws.security.registry package.
            //PM34097 add message to thrown exception
            /*
             * if (toCatch instanceof com.ibm.websphere.wim.exception.PasswordCheckFailedException) {
             * throw new PasswordCheckFailedException(toCatch.getMessage(),toCatch);
             * }
             * // d110248
             * // PM34097 add message to thrown exception
             * else if (toCatch instanceof EntityNotFoundException) {
             * throw new PasswordCheckFailedException(toCatch.getMessage(),toCatch);
             * }
             * // other cases
             * else {
             * throw new RegistryException(toCatch);
             * }
             */
            throw new RegistryException(toCatch.getMessage(), toCatch);
        }
        return returnValue.toString();
    }

    @FFDCIgnore(WIMException.class)
    public String mapCertificate(X509Certificate[] chain) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {

        // initialize the return value
        StringBuffer returnValue = new StringBuffer();
        // bridge the APIs
        try {
            // validate the certificate
            this.mappingUtils.validateCertificate(chain);

            // separate the user and realm from ""
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm("");

            // create an empty root DataObject
            Root root = this.mappingUtils.getWimService().createRootObject();

            // add MAP(userSecurityName) to the return list of properties
            // f113366
            String outputAttrName = this.propertyMap.getOutputUserSecurityName(idAndRealm.getRealm());
            if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                this.mappingUtils.createLoginControlDataObject(root, outputAttrName);
            }

            // use the root DataGraph to create a LoginAccount DataGraph
            List<Entity> entities = root.getEntities();
            LoginAccount loginAct = new LoginAccount();
            if (entities != null) {
                entities.add(loginAct);
            }

            // d123655
            // set the "userCertificate" property to base64(cert[0])
            //loginAccount.getList(Service.PROP_CERTIFICATE).add(inputCertificates[0].getEncoded());
            for (X509Certificate cert : chain) {
                loginAct.getCertificate().add(cert.getEncoded());
            }

            // invoke ProfileService.login with the input root DataGraph
            root = this.mappingUtils.getWimService().login(root);

            // set the user to the value of MAP(userUniqueId) from the output DataGraph
            List<Entity> returnList = root.getEntities();

            // the user was not authenticated
            if (returnList.isEmpty()) {
                throw new com.ibm.wsspi.security.wim.exception.CertificateMapFailedException();
            }
            // the user was authenticated
            else {
                // ff113366
                Entity entity = returnList.get(0);
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    Object value = entity.get(outputAttrName);
                    if (value instanceof List<?>) {
                        returnValue.append(BridgeUtils.getStringValue(((List<?>) value).get(0)));
                    } else {
                        returnValue.append(BridgeUtils.getStringValue(value));
                    }
                } else {
                    // d113681
                    returnValue.append(BridgeUtils.getStringValue(entity.getIdentifier().get(outputAttrName)));
                }
            }
            // if realm is defined
            // d109969
            if ((idAndRealm.isRealmDefined())
                && (!this.mappingUtils.getDefaultRealmName().equals(idAndRealm.getRealm()))) {
                //user = user + realmDelimiter + realm
                returnValue.append(idAndRealm.getDelimiter() + idAndRealm.getRealm());
            }
        } catch (CertificateEncodingException toCatch) {
            // log the Exception
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, toCatch.getMessage(), toCatch);
            }
            throw new RegistryException(toCatch.getMessage(), toCatch);
        } catch (WIMException toCatch) {
            // log the Exception
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, toCatch.getMessage(), toCatch);
            }
            // certificate mapping isn't supported
            if (toCatch instanceof com.ibm.wsspi.security.wim.exception.CertificateMapNotSupportedException) {
                throw new CertificateMapNotSupportedException(toCatch.getMessage(), toCatch);
            }
            // certificate mapping failed
            else if (toCatch instanceof com.ibm.wsspi.security.wim.exception.CertificateMapFailedException) {
                throw new CertificateMapFailedException(toCatch.getMessage(), toCatch);
            }
            // other cases
            else {
                throw new RegistryException(toCatch.getMessage(), toCatch);
            }
        }
        return returnValue.toString();
    }

    public String getRealmName() throws WIMException {
        // invoke ProfileService.getRealm
        return this.mappingUtils.getWimService().getRealmName();
    }
}
