/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.rmi.CORBA;

/**
 * One of our tests needs to override a system class. {@link Util} is a pretty safe bet to be present on all JDKs, since it is a required API.
 * Also, this can safely be overridden since it is not in a <code>java.*</code> package.
 */

public class Util {
    public static final boolean IMPOSTER = true;
}
