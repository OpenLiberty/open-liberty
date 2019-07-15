/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.bindings.bnd.ejb;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateful;

@Stateful(name = "TargetFourBean")
@Remote({ RemoteTargetFourBiz1.class, RemoteTargetFourBiz2.class })
@Local({ LocalTargetFourBiz1.class, LocalTargetFourBiz2.class })
public class TargetFourBean {

    public String ping1() {
        return "pong";
    }

    public String ping2() {
        return "pong";
    }

    public String ping3() {
        return "pong";
    }

    public String ping4() {
        return "pong";
    }

}
