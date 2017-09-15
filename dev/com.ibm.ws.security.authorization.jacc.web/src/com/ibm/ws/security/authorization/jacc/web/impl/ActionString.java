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
package com.ibm.ws.security.authorization.jacc.web.impl;

/*
 *   This is a helper class used by the URLMap class
 *   to convey actions String (i.e., "GET,PUT" or "!POST")which is used to construct
 *   WebResourcePermission or WebUserDataPermission.
 */

public class ActionString
{
    private String _actions = null;

    public ActionString() {}

    public ActionString(String actions) {
        setActions(actions);
    }

    public void setActions(String actions) {
        if (actions != null && actions.length() == 0) {
            _actions = null;
        } else {
            _actions = actions;
        }
    }

    public String getActions() {
        return _actions;
    }

    public String getReverseActions() {
        String output = null;
        if (_actions != null) {
            if (_actions.startsWith("!")) {
                output = _actions.substring(1);
            } else {
                output = "!" + _actions;
            }
        }
        return output;
    }

    @Override
    public String toString() {
        if (_actions == null || _actions.length() == 0) {
            return "<null>";
        } else {
            return _actions;
        }
    }
}
