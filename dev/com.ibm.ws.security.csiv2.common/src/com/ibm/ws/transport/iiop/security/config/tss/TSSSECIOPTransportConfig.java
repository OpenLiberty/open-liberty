/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.tss;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CSIIOP.SECIOP_SEC_TRANS;
import org.omg.CSIIOP.SECIOP_SEC_TRANSHelper;
import org.omg.CSIIOP.TAG_SECIOP_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import org.omg.CSIIOP.TransportAddress;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.config.ConfigUtil;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

/**
 * TODO: this class needs to be revisited.
 * 
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSSECIOPTransportConfig extends TSSTransportMechConfig {

    private short supports;
    private short requires;
    private String mechOID;
    private String targetName;
    private final List addresses = new ArrayList(1);

    public TSSSECIOPTransportConfig() {}

    public TSSSECIOPTransportConfig(TaggedComponent component, Codec codec) throws Exception {
        Any any = codec.decode_value(component.component_data, TLS_SEC_TRANSHelper.type());
        SECIOP_SEC_TRANS tst = SECIOP_SEC_TRANSHelper.extract(any);

        supports = tst.target_supports;
        requires = tst.target_requires;
        mechOID = Util.decodeOID(tst.mech_oid);
        targetName = new String(tst.target_name);

        for (int i = 0; i < tst.addresses.length; i++) {
            addresses.add(new TSSTransportAddressConfig(tst.addresses[i].port, tst.addresses[i].host_name));
        }
    }

    @Override
    public short getSupports() {
        return supports;
    }

    public void setSupports(short supports) {
        this.supports = supports;
    }

    @Override
    public short getRequires() {
        return requires;
    }

    public void setRequires(short requires) {
        this.requires = requires;
    }

    public String getMechOID() {
        return mechOID;
    }

    public void setMechOID(String mechOID) {
        this.mechOID = mechOID;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public List getAddresses() {
        return addresses;
    }

    @Override
    public TaggedComponent encodeIOR(Codec codec) throws Exception {
        TaggedComponent result = new TaggedComponent();

        SECIOP_SEC_TRANS sst = new SECIOP_SEC_TRANS();

        sst.target_supports = supports;
        sst.target_requires = requires;
        sst.mech_oid = Util.encodeOID(mechOID);
        sst.target_name = targetName.getBytes();

        sst.addresses = new TransportAddress[addresses.size()];

        int i = 0;
        TSSTransportAddressConfig transportConfig;
        for (Iterator iter = addresses.iterator(); iter.hasNext();) {
            transportConfig = (TSSTransportAddressConfig) iter.next();
            sst.addresses[i++] = new TransportAddress(transportConfig.getHostname(), transportConfig.getPort());
        }

        Any any = ORB.init().create_any();
        SECIOP_SEC_TRANSHelper.insert(any, sst);

        result.tag = TAG_SECIOP_SEC_TRANS.value;
        result.component_data = codec.encode_value(any);

        return result;
    }

    @Override
    public Subject check(SSLSession session) throws SASException {
        return new Subject();
    }

    @Override
    @Trivial
    void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSSASMechConfig: [\n");
        buf.append(moreSpaces).append("SUPPORTS  : ").append(ConfigUtil.flags(supports)).append("\n");
        buf.append(moreSpaces).append("REQUIRES  : ").append(ConfigUtil.flags(requires)).append("\n");
        buf.append(moreSpaces).append("mechOID   : ").append(mechOID).append("\n");
        buf.append(moreSpaces).append("targetName: ").append(targetName).append("\n");
        for (Iterator iterator = addresses.iterator(); iterator.hasNext();) {
            TSSTransportAddressConfig tssTransportAddressConfig = (TSSTransportAddressConfig) iterator.next();
            tssTransportAddressConfig.toString(moreSpaces, buf);
        }
        buf.append(spaces).append("]\n");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTrusted(TrustedIDEvaluator trustedIDEvaluator, SSLSession session) {
        // TODO Auto-generated method stub
        return false;
    }

}
