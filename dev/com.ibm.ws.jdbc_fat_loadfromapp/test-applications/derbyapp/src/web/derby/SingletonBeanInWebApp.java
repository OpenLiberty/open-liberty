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
package web.derby;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

@Singleton
public class SingletonBeanInWebApp {
    @Lock(LockType.READ)
    public <T> T invoke(Callable<T> action) {
        try {
            return action.call();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new CompletionException(x);
        }
    }
}
