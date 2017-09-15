/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ffdc;

/**
 * This interface is for objects that want to have control of how they are
 * dumped in the FFDC report. In the Full Profile, this was also used to
 * control FFDC reporting for objects with senstitive fields. This is not
 * required if the @Sensistive annotation is used as it will correctly
 * protect sensitive fields.
 */
public interface FFDCSelfIntrospectable {

    /**
     * Returns an array of Strings representing the object's state.
     * <p><strong>Do not return any sensitive information in the FFDC dump</strong></p>
     * <p>
     * If the object implements this interface, normal introspection dump will
     * be skipped. Ensure that all information that you want captured in the
     * FFDC is included in the return of this method.
     * </p>
     * <p>
     * The Strings can take the following format:
     * <ul>
     * <li>name=value</li>
     * <li>name=</li>
     * <li>value</li>
     * <li>null</li>
     * </ul>
     * </p>
     * <p>
     * Example implementation:
     * <pre><code>
     * public String[] introspectSelf() {
     * StringBuffer introspectBuffer = new StringBuffer();
     * String[] returnValue = new String[2];
     * introspectBuffer.append("variableName1 = ");
     * introspectBuffer.append(variableName1);
     * returnValue[0] = new String(introspectBuffer);
     * introspectBuffer.setLength(0);
     * introspectBuffer.append("variableName2 = ");
     * introspectBuffer.append(variableName2);
     * returnValue[1] = new String(introspectBuffer);
     * return returnValue;
     * }
     * </code></pre>
     * <p>
     * 
     * @return an array of Strings representing the instance variables of this
     *         object that do not contain sensitive data.
     */
    public String[] introspectSelf();

}
