/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.produceConsume;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Widget {

    private String name;
    private int length;

    public Widget() {}
    public Widget(String name, int length) {
        this.name = name;
        this.length = length;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }
    @Override
    public String toString() {
        return "Widget [name=" + name + ", length=" + length + "]";
    }
}
