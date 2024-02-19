package com.ibm.ws.request.probe.audit.rest.injection;
import com.ibm.ws.request.probe.audit.rest.consumer.AuditPERestConsumer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditService;

public interface AuditPERestInjector{
    public AuditPERestConsumer getConsumer();
}