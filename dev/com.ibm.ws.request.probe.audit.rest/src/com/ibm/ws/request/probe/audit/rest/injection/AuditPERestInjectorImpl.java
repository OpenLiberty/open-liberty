package com.ibm.ws.request.probe.audit.rest.injection;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.ws.request.probe.audit.rest.consumer.AuditPERestConsumer;
import com.ibm.ws.request.probe.audit.rest.service.AuditPERestServiceImpl;
import com.ibm.ws.security.audit.rest.MemberManagementEvent;
import com.ibm.ws.security.audit.rest.RESTAuthorizationEvent;
import javax.servlet.http.HttpServletResponse;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditService;

public class AuditPERestInjectorImpl implements AuditPERestInjector {
    @Override
	public AuditPERestConsumer getConsumer(){
		return new AuditPERestConsumer(new AuditPERestServiceImpl());
	}

}