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
package com.ibm.ws.security.wim.adapter.ldap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/******************************************************************************
 * Use an extended version of ByteArrayOutputStream in order to allow
 * custom writing to the output stream. This class will be use when JNDI BER packet writing has been enabled
 ******************************************************************************/
@Trivial
public class BEROutputStream extends ByteArrayOutputStream {
    private static final TraceComponent tc = Tr.register(BEROutputStream.class);
    @Override
    public void write(int b) {
        String METHODNAME = "write(int b)";
        super.write(b);
        try {
            if (this.toString("UTF-8").contains(System.lineSeparator())) {
                try {
                    super.writeTo(System.out);
                    super.reset();
                } catch (IOException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " Cannot write to System out: " + e.toString());
                }
            }
        } catch (UnsupportedEncodingException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " UTF-8 is not a valid encoding: " + e.toString());
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        String METHODNAME = "write(byte[] b, int off, int len)";
        super.write(b, off, len);
        try {
            if (this.toString("UTF-8").contains(System.lineSeparator())) {
                try {
                    super.writeTo(System.out);
                    super.reset();
                } catch (IOException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " Cannot write to System out: " + e.toString());
                }
            }
        } catch (UnsupportedEncodingException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " UTF-8 is not a valid encoding: " + e.toString());
        }
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }
}