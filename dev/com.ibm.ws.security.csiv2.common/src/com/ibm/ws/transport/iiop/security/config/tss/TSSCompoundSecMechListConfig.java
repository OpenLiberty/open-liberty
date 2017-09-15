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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CSI.EstablishContext;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.CompoundSecMechList;
import org.omg.CSIIOP.CompoundSecMechListHelper;
import org.omg.CSIIOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.SASInvalidMechanismException;
import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSCompoundSecMechListConfig implements Serializable {
    private boolean stateful;
    private final ArrayList<TSSCompoundSecMechConfig> mechs = new ArrayList<TSSCompoundSecMechConfig>();
    private final HashMap<String, TSSCompoundSecMechConfig> mechsMap = new HashMap<String, TSSCompoundSecMechConfig>();

    public boolean isStateful() {
        return stateful;
    }

    public void setStateful(boolean stateful) {
        this.stateful = stateful;
    }

    public void add(TSSCompoundSecMechConfig mech) {
        mechs.add(mech);
    }

    public void add(TSSCompoundSecMechConfig mech, String mechOid) {
        add(mech);
        mechsMap.put(mechOid, mech);
    }

    public TSSCompoundSecMechConfig mechAt(int i) {
        return mechs.get(i);
    }

    public int size() {
        return mechs.size();
    }

    public TaggedComponent encodeIOR(Codec codec) throws Exception {
        CompoundSecMechList csml = new CompoundSecMechList();

        csml.stateful = stateful;
        csml.mechanism_list = new CompoundSecMech[mechs.size()];

        for (int i = 0; i < mechs.size(); i++) {
            csml.mechanism_list[i] = mechs.get(i).encodeIOR(codec);
        }

        Any any = ORB.init().create_any();
        CompoundSecMechListHelper.insert(any, csml);

        return new TaggedComponent(TAG_CSI_SEC_MECH_LIST.value, codec.encode_value(any));
    }

    public static TSSCompoundSecMechListConfig decodeIOR(Codec codec, TaggedComponent taggedComponent) throws Exception {
        TSSCompoundSecMechListConfig result = new TSSCompoundSecMechListConfig();

        Any any = codec.decode_value(taggedComponent.component_data, CompoundSecMechListHelper.type());
        CompoundSecMechList csml = CompoundSecMechListHelper.extract(any);

        result.setStateful(csml.stateful);

        for (int i = 0; i < csml.mechanism_list.length; i++) {
            result.add(TSSCompoundSecMechConfig.decodeIOR(codec, csml.mechanism_list[i]));
        }

        return result;
    }

    public Subject check(SSLSession session, EstablishContext msg, Codec codec) throws SASException {
        Subject result = null;
        String theOid = Util.getMechOidFromAuthToken(codec, msg);
        if (theOid != null) {
            TSSCompoundSecMechConfig tssCompoundSecMechConfig = mechsMap.get(theOid);
            if (tssCompoundSecMechConfig != null) {
                result = tssCompoundSecMechConfig.check(session, msg, codec);
            } else {
                throw new SASInvalidMechanismException();
            }
        } else { //no mech OID, loop through all server mechs
            for (int i = 0; i < mechs.size(); i++) {
                result = mechs.get(i).check(session, msg, codec);
                if (result != null)
                    break;
            }
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Trivial
    void toString(String spaces, StringBuilder buf) {
        buf.append(spaces).append("TSSCompoundSecMechListConfig: [\n");
        for (Iterator availMechs = mechs.iterator(); availMechs.hasNext();) {
            TSSCompoundSecMechConfig aConfig = (TSSCompoundSecMechConfig) availMechs.next();
            aConfig.toString(spaces + "  ", buf);
            buf.append("\n");
        }
        buf.append(spaces).append("]\n");
    }
}
