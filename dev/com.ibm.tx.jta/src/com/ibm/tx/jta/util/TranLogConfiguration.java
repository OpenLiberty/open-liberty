package com.ibm.tx.jta.util;

/*******************************************************************************
 * Copyright (c) 2002, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.Properties;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public final class TranLogConfiguration {
    private static final TraceComponent tc = Tr.register(TranLogConfiguration.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    public static final int TYPE_NONE = 0;
    public static final int TYPE_STREAM = 1;
    public static final int TYPE_FILE = 2;
    public static final int TYPE_CUSTOM = 3;

    private String _streamName;
    private String _originalLogDirectory;
    private String _expandedLogDirectory;
    private int _logFileSize;
    private boolean _enabled = true;
    private int _type = TYPE_NONE;
    private String _customId;
    private Properties _customProps;

    public TranLogConfiguration() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "TranLogConfiguration");

        _enabled = false;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "TranLogConfiguration", this);
    }

    public TranLogConfiguration(String streamName) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "TranLogConfiguration", streamName);

        _type = TYPE_STREAM;
        _streamName = streamName;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "TranLogConfiguration", this);
    }

    public TranLogConfiguration(String original, String expanded, int logFileSize) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "TranLogConfiguration", new Object[] { original, expanded, logFileSize });

        _type = TYPE_FILE;
        _originalLogDirectory = original;
        _expandedLogDirectory = expanded;
        _logFileSize = logFileSize;
        if (original != null && ";0".equals(original.trim()))
            _enabled = false;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "TranLogConfiguration", this);
    }

    public TranLogConfiguration(String customId, Properties props) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "TranLogConfiguration", new Object[] { customId, props });

        _type = TYPE_CUSTOM;
        _customId = customId;
        _customProps = props;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "TranLogConfiguration", this);
    }

    public int type() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "type: {0}", _type);
        return _type;
    }

    public String streamName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "streamName: {0}", _streamName);
        return _streamName;
    }

    public String originalLogDirectory() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "originalLogDirectory: {0}", _originalLogDirectory);
        return _originalLogDirectory;
    }

    public String expandedLogDirectory() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "expandedLogDirectory: {0}", _expandedLogDirectory);
        return _expandedLogDirectory;
    }

    public int logFileSize() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "logFileSize: {0}", _logFileSize);
        return _logFileSize;
    }

    public boolean enabled() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "enabled: {0}", _enabled);
        return _enabled;
    }

    public String customId() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "customId: {0}", _customId);
        return _customId;
    }

    public Properties customProperties() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "customProperties: {0}", _customProps);
        return _customProps;
    }
}