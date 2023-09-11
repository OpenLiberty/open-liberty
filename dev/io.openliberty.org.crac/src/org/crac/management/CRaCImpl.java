// Copyright 2022 Azul Systems, Inc.
// Copyright (c) 2023 IBM Corporation and others.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
// this list of conditions and the following disclaimer in the documentation
// and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package org.crac.management;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

import io.openliberty.checkpoint.spi.CheckpointPhase;

class CRaCImpl implements CRaCMXBean {
    private final AtomicLong restoreTime = new AtomicLong(-1);

    CRaCImpl() {
    }

    @Override
    public long getUptimeSinceRestore() {
        if (CheckpointPhase.getPhase().restored()) {
            long upTime = (System.nanoTime() - TimestampUtils.getStartTimeNano()) / 1000000L;
            return upTime;
        }
        return -1;
    }

    @Override
    public long getRestoreTime() {
        if (CheckpointPhase.getPhase().restored()) {
            return restoreTime.updateAndGet((x) -> (x == -1) ? System.currentTimeMillis() - getUptimeSinceRestore() : x);
        }
        return -1;
    }

    @Override
    public ObjectName getObjectName() {
        // TODO need name here
        return null;
    }
}