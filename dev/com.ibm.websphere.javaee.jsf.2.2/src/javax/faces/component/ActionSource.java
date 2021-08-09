/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.component;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public interface ActionSource
{
    /**
     * @deprecated Replaced by ActionSource2.getActionExpression
     */
    public javax.faces.el.MethodBinding getAction();

    /**
     * @deprecated Replaced by ActionSource2.setActionExpression
     */
    public void setAction(javax.faces.el.MethodBinding action);

    /**
     * @deprecated Replaced by getActionListeners
     */
    public javax.faces.el.MethodBinding getActionListener();

    public void setActionListener(javax.faces.el.MethodBinding actionListener);

    public boolean isImmediate();

    public void setImmediate(boolean immediate);

    public void addActionListener(javax.faces.event.ActionListener listener);

    public javax.faces.event.ActionListener[] getActionListeners();

    public void removeActionListener(javax.faces.event.ActionListener listener);
}
