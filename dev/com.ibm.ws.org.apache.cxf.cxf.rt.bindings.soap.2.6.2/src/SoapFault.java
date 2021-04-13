/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.binding.soap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;

public class SoapFault extends Fault {
    public static final QName ATTACHMENT_IO = new QName(Soap12.SOAP_NAMESPACE, "AttachmentIOError");

    private static final long serialVersionUID = 5775857720028582429L;


    /**
     * "The message was incorrectly formed or did not contain the appropriate
     * information in order to succeed." -- SOAP 1.2 Spec
     */ 

    /**
     * A SOAP 1.2 only fault code. <p/> "The message could not be processed for
     * reasons attributable to the processing of the message rather than to the
     * contents of the message itself." -- SOAP 1.2 Spec <p/> If this message is
     * used in a SOAP 1.1 Fault it will most likely (depending on the
     * FaultHandler) be mapped to "Sender" instead.
     */ 

    private List<QName> subCodes;
    private String role;
    private String node;
    private Map<String, String> namespaces = new HashMap<String, String>();

    public SoapFault(Message message, Throwable throwable, QName faultCode) {
        super(message, throwable, faultCode);
    }

    public SoapFault(Message message, QName faultCode) {
        super(message, faultCode);
    }

    public SoapFault(String message, QName faultCode) {
        super(new Message(message, (ResourceBundle)null), faultCode);
    }
    public SoapFault(String message, ResourceBundle bundle, QName faultCode) {
        super(new Message(message, bundle), faultCode);
    }
    public SoapFault(String message, ResourceBundle bundle, Throwable t, QName faultCode) {
        super(new Message(message, bundle), t, faultCode);
    }
    public SoapFault(String message, ResourceBundle bundle, QName faultCode, Object ... params) {
        super(new Message(message, bundle, params), faultCode);
    }

    public SoapFault(String message, Throwable t, QName faultCode) {
        super(new Message(message, (ResourceBundle)null), t, faultCode);
    }

    
    public String getCodeString(String prefix, String defaultPrefix) {
        return getFaultCodeString(prefix, defaultPrefix, getFaultCode());
    }
    
    public String getSubCodeString(String prefix, String defaultPrefix) {
        return getFaultCodeString(prefix, defaultPrefix, getRootSubCode());
    }
    
    private String getFaultCodeString(String prefix, String defaultPrefix, QName fCode) {
        String codePrefix = null;
        if (StringUtils.isEmpty(prefix)) {
            codePrefix = fCode.getPrefix();
            if (StringUtils.isEmpty(codePrefix)) {
                codePrefix = defaultPrefix;
            }
        } else {
            codePrefix = prefix;
        }
        
        return codePrefix + ":" + fCode.getLocalPart();        
    }
    
    private QName getRootSubCode() {
        return subCodes != null && subCodes.size() > 0 ? subCodes.get(0) : null;
    }

    private void setRootSubCode(QName subCode) {
        if (subCodes == null) {
            subCodes = new LinkedList<QName>();
        } else {
            subCodes.clear();
        }
        subCodes.add(subCode);
    }

    public String getReason() {
        return getMessage();
    }

    /**
     * Returns the fault actor.
     * 
     * @return the fault actor.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the fault actor.
     * 
     * @param actor the actor.
     */
    public void setRole(String actor) {
        this.role = actor;
    }
    
    public String getNode() {
        return node;
    }

    public void setNode(String n) {
        this.node = n;
    }    

    /**
     * Returns the SubCode for the Fault Code. If there are more than one Subcode entries 
     * in this fault, the first Subcode is returned.
     * 
     * @return The SubCode element as detailed by the SOAP 1.2 spec.
     */
    public QName getSubCode() {
        return getRootSubCode();
    }

    /**
     * Returns the SubCode list for the Fault Code.
     * 
     * @return The SubCode element list as detailed by the SOAP 1.2 spec.
     */
    public List<QName> getSubCodes() {
        return subCodes;
    }

    /**
     * Sets the SubCode for the Fault Code. If there are more than one Subcode entries 
     * in this fault, the first Subcode is set while the other entries are removed.
     * 
     * @param subCode The SubCode element as detailed by the SOAP 1.2 spec.
     */
    public void setSubCode(QName subCode) {
        setRootSubCode(subCode);
    }

    /**
     * Sets the SubCode list for the Fault Code.
     * 
     * @param subCode The SubCode element list as detailed by the SOAP 1.2 spec.
     */
    public void setSubCodes(List<QName> subCodes) {
        this.subCodes = subCodes;
    }

    /**
     * Appends the SubCode to the SubCode list.
     * 
     * @param subCode The SubCode element as detailed by the SOAP 1.2 spec. 
     */
    public void addSubCode(QName subCode) {
        if (subCodes == null) {
            subCodes = new LinkedList<QName>();
        }
        subCodes.add(subCode);
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    public static SoapFault createFault(Fault f, SoapVersion v) {
        if (f instanceof SoapFault) {
            //make sure the fault code is per spec
            //if it's one of our internal codes, map it to the proper soap code
            if (f.getFaultCode().getNamespaceURI().equals(Fault.FAULT_CODE_CLIENT.getNamespaceURI())) {
                QName fc = f.getFaultCode();
                if (Fault.FAULT_CODE_CLIENT.equals(fc)) {
                    fc = v.getSender();
                } else if (Fault.FAULT_CODE_SERVER.equals(fc)) { 
                    fc = v.getReceiver();
                }
                f.setFaultCode(fc);
            }
            return (SoapFault)f;
        }

        QName fc = f.getFaultCode();
        if (Fault.FAULT_CODE_CLIENT.equals(fc)) {
            fc = v.getSender();
        } else if (Fault.FAULT_CODE_SERVER.equals(fc)) { 
            fc = v.getReceiver();
        }
        SoapFault soapFault = new SoapFault(new Message(f.getMessage(), (ResourceBundle)null),
                                            f.getCause(),
                                            fc);
        
        soapFault.setDetail(f.getDetail());
        return soapFault;
    }
}
