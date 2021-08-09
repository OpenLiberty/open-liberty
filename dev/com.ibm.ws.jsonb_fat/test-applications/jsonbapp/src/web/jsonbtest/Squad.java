/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.jsonbtest;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

/**
 * Application class that can be marshalled/unmarshalled to/from JSON.
 */
public class Squad {
    private String name;
    private Pod pod;
    private byte size;
    private float storyPointsPerIteration;

    @JsonbCreator
    public Squad(@JsonbProperty("name") String n,
                 @JsonbProperty("size") byte s,
                 @JsonbProperty("pod") Pod p,
                 @JsonbProperty("velocity") float v) {
        name = n;
        size = s;
        pod = p;
        storyPointsPerIteration = v;
    }

    @Override
    public String toString() {
        return name + " [" + size + "] velocity " + storyPointsPerIteration + " @ " + pod;
    }

    public String getName() {
        return name;
    }

    public Pod getPod() {
        return pod;
    }

    public byte getSize() {
        return size;
    }

    public float getVelocity() {
        return storyPointsPerIteration;
    }
}
