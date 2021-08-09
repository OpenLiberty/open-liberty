/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.interceptor;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class BagOfInterceptors {

    public static ThreadLocal<Set<String>> businessInterceptors = ThreadLocal.withInitial(HashSet::new);

    public static ThreadLocal<Set<String>> lifecycleInterceptors = ThreadLocal.withInitial(HashSet::new);
}
