/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.user.feature;

import java.io.Serializable;

public class UserFeatureService implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String message;

    public UserFeatureService(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;
        if (o instanceof UserFeatureService) {
            UserFeatureService other = (UserFeatureService) o;
            equal = (this.getMessage().equals(other.getMessage()));
        }
        return equal;
    }

}
