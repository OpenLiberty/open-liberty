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
package com.ibm.ws.app.manager.internal.monitor;

import java.util.Map;

/**
 *
 */
public class ApplicationMonitorConfig {

    private final long _pollingRate;
    private final String _location;
    private final UpdateTrigger _updateTrigger;
    private final boolean _dropinsMonitoring;

    public ApplicationMonitorConfig(ApplicationMonitorConfig prevConfig, Map<String, ?> props) {
        Object val = props.get("pollingRate");
        if (val instanceof Long) {
            _pollingRate = ((Long) val);
        } else if (prevConfig != null) {
            _pollingRate = prevConfig._pollingRate;
        } else {
            _pollingRate = 500;
        }
        val = props.get("dropins");
        if (val instanceof String) {
            // TODO: defaulting to "dropins" if value set is "".  Really should be validated
            // that a location is non-empty in config code.  Also note that because the metatype
            // has defaults, that validation is done in config code so this is the only code
            // that will ever run.
            String dropins = (String) val;
            _location = dropins.isEmpty() ? "dropins" : dropins;
        } else if (prevConfig != null) {
            _location = prevConfig._location;
        } else {
            _location = "dropins";
        }
        val = props.get("updateTrigger");
        if (val instanceof String) {
            _updateTrigger = UpdateTrigger.get((String) val);
        } else if (prevConfig != null) {
            _updateTrigger = prevConfig._updateTrigger;
        } else {
            _updateTrigger = UpdateTrigger.POLLED;
        }
        val = props.get("dropinsEnabled");
        if (val instanceof Boolean) {
            _dropinsMonitoring = ((Boolean) val);
        } else if (prevConfig != null) {
            _dropinsMonitoring = prevConfig._dropinsMonitoring;
        } else {
            _dropinsMonitoring = true;
        }
    }

    /**
     * @return the pollingRate
     */
    public long getPollingRate() {
        return _pollingRate;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return _location;
    }

    /**
     * @return the updateTrigger
     */
    public UpdateTrigger getUpdateTrigger() {
        return _updateTrigger;
    }

    /**
     * @return the _dropinsMonitoring
     */
    public boolean isDropinsMonitored() {
        return _dropinsMonitoring;
    }
}
