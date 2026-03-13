/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.remote.fat.crossapp.ejb;

import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

import io.openliberty.ejbcontainer.remote.fat.crossapp.shared.CrossAppBusinessRMI;
import io.openliberty.ejbcontainer.remote.fat.crossapp.shared.CrossAppBusinessRemote;
import io.openliberty.ejbcontainer.remote.fat.crossapp.shared.CrossAppEJBHome;

@Stateless
@RemoteHome(CrossAppEJBHome.class)
@Remote({ CrossAppBusinessRMI.class, CrossAppBusinessRemote.class })
public class CrossAppRemoteBean {
    public String echo(String s) {
        return s;
    }
}
