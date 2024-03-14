package com.ibm.ws.request.probe.audit.rest.consumer;
import com.ibm.ws.request.probe.audit.rest.service.AuditPERestService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditService;

public class AuditPERestConsumer{
    private AuditPERestService service;
    public AuditPERestConsumer(AuditPERestService svc){
        this.service=svc;
    }

    public void exec_auditEventMemberMgmt01(AtomicServiceReference<AuditService> auditServiceRef, Object[] methodParams){
        this.service.auditEventMemberMgmt01(auditServiceRef, methodParams);
    }

    public void exec_auditEventRESTAuthz(AtomicServiceReference<AuditService> auditServiceRef, Object[] methodParams){
        this.service.auditEventRESTAuthz(auditServiceRef, methodParams);
    }

}