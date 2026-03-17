/*******************************************************************************
 * Copyright (c) 2024,2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * This entity has ElementCollections that fetch lazily and eagerly
 */
@Entity
public class Mobile {
    public enum OS {
        ANDROID, IOS, WINDOWS, FIRE
    }

    @Id
    UUID deviceId;

    OS operatingSystem;

    @ElementCollection(fetch = FetchType.LAZY)
    List<String> apps;

    @ElementCollection(fetch = FetchType.EAGER)
    List<String> emails;

    public static Mobile of(OS os, List<String> apps, List<String> emails) {
        Mobile inst = new Mobile();

        inst.deviceId = UUID.randomUUID();
        inst.operatingSystem = os;
        inst.apps = apps;
        inst.emails = emails;

        return inst;
    }

    public List<String> getApps() {
        return apps;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public List<String> getEmails() {
        return emails;
    }

    public OS getOperatingSystem() {
        return operatingSystem;
    }

    public void setApps(List<String> apps) {
        this.apps = apps;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public void setOperatingSystem(OS operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    @Override
    public String toString() {
        return "Mobile [deviceId=" + deviceId +
               ", operatingSystem=" + operatingSystem +
               ", apps=" + apps +
               ", emails=" + emails + "]";
    }
}
