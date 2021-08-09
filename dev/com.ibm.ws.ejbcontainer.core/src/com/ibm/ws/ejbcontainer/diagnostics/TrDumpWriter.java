/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.diagnostics;

import java.util.ArrayList;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * IntrospectionWriter implementation that prints all introspection data using
 * Tr.dump(). <p>
 * 
 * Introspection data written between printheader() and printfooter() (including
 * the header and footer) is batched and dumped in batch to provide improved
 * dump readability. <p>
 * 
 * Note: This class does not perform any trace guarding; use of this class should
 * be guarded.
 */
public class TrDumpWriter implements IntrospectionWriter {
    private final TraceComponent tc;

    private String ivTitle;
    private final ArrayList<String> ivDumpData = new ArrayList<String>();

    public TrDumpWriter(TraceComponent tc) {
        this.tc = tc;
    }

    private void flush() {
        if (ivTitle != null) {
            if (ivDumpData.isEmpty()) {
                Tr.dump(tc, ivTitle);
            } else {
                Tr.dump(tc, ivTitle, (Object[])ivDumpData.toArray(new String[ivDumpData.size()]));
                ivDumpData.clear();
            }
            ivTitle = null;
        }
    }

    @Override
    public void begin(String title) {
        flush();
        ivTitle = title != null ? title : "";
    }

    @Override
    public void end() {
        flush();
    }

    @Override
    public void println(String line) {
        ivDumpData.add(line);
    }

    @Override
    public void dump(String[] lines) {
        ivDumpData.addAll(Arrays.asList(lines));
    }
}
