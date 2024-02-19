package com.ibm.ws.request.probe.audit.rest.service;
import com.ibm.ws.security.audit.rest.MemberManagementEvent;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditService;

public interface AuditPERestService{
    void auditEventRESTAuthz(AtomicServiceReference<AuditService> auditServiceRef, Object[] methodParams);
    void auditEventMemberMgmt01(AtomicServiceReference<AuditService> auditServiceRef, Object[] methodParams);

}