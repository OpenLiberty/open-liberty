/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import com.ibm.websphere.csi.J2EEName;

public final class J2EENameImpl implements J2EEName {
    private static final long serialVersionUID = 7488184044073147667L;

    private transient String application;
    private transient String module;
    private transient String component;

    /**
     * The A#M#C string representation. This field is lazily initialized
     * by {@link #toString}. This field does not need to be volatile since
     * String is safe for concurrent publication.
     */
    private transient String string;

    /**
     * The UTF-8 encoded bytes of {@link #string}. This field is lazily
     * initialized by {@link #getBytes}. This field must be volatile to ensure
     * that the contents are written.
     */
    private volatile byte[] j2eeNameBytes;

    public J2EENameImpl(String application, String module, String component) {
        if (application == null) {
            throw new IllegalArgumentException("application");
        }
        if (application.indexOf('#') != -1) {
            throw new IllegalArgumentException("application name must not contain '#'");
        }

        if (module == null) {
            if (component != null) {
                throw new IllegalArgumentException("module");
            }
        } else {
            if (module.indexOf('#') != -1) {
                throw new IllegalArgumentException("module name must not contain '#'");
            }

            if (component != null && component.indexOf('#') != -1) {
                throw new IllegalArgumentException("component name must not contain '#'");
            }
        }

        this.application = application;
        this.module = module;
        this.component = component;
    }

    public J2EENameImpl(byte[] bytes) {
        this.j2eeNameBytes = bytes;
        readObject(bytes);
    }

    @Override
    public String toString() {
        String string = this.string;
        if (string == null) {
            if (this.module == null) {
                string = this.application;
            } else if (this.component == null) {
                string = this.application + '#' + this.module;
            } else {
                string = this.application + '#' + this.module + '#' + this.component;
            }

            this.string = string;
        }

        return string;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o != null &&
               o.getClass() == J2EENameImpl.class &&
               toString().equals(((J2EENameImpl) o).toString());
    }

    public boolean equals(J2EENameImpl o) {
        return o != null &&
               toString().equals(o.toString());
    }

    @Override
    public String getApplication() {
        return this.application;
    }

    @Override
    public String getModule() {
        return this.module;
    }

    @Override
    public String getComponent() {
        return this.component;
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = this.j2eeNameBytes;
        if (bytes == null) {
            bytes = toString().getBytes(StandardCharsets.UTF_8);

            this.j2eeNameBytes = bytes;
        }

        return bytes;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        getBytes();
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        readObject(this.j2eeNameBytes);
    }

    private void readObject(byte[] bytes) {
        this.string = new String(bytes, StandardCharsets.UTF_8);

        int modSepIndex = string.indexOf('#');
        if (modSepIndex == -1) {
            this.application = string;
        } else {
            this.application = string.substring(0, modSepIndex);

            int compSepIndex = string.indexOf('#', modSepIndex + 1);
            if (compSepIndex == -1) {
                this.module = string.substring(modSepIndex + 1);
            } else {
                this.module = string.substring(modSepIndex + 1, compSepIndex);
                this.component = string.substring(compSepIndex + 1);
            }
        }
    }
}
