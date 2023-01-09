/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package com.ibm.ws.cdi.api.fat.apps.current;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

/**
 *
 */
@Dependent
public class Producer {

  @Produces
  SimpleBeanTwo getSimpleBeanTwo() {
    return new SimpleBeanTwo();
  }

  @Produces
  SimpleBeanThree getSimpleBeanThree() {
    return null;
  }

}
