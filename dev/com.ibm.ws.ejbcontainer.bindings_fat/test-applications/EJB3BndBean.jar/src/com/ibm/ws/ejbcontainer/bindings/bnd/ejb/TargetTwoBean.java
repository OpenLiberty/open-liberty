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

import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless(name = "TargetTwoBean")
@Remote({ RemoteTargetTwoBiz1.class,
          RemoteTargetTwoBiz2.class,
          RemoteTargetTwoBiz3.class })
public class TargetTwoBean {

    public String ping1() {
        return "pong";
    }

    public String ping2() {
        return "pong";
    }

    public String ping3() {
        return "pong";
    }

}
