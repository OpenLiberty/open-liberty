package com.ibm.ws390.ola.unittest.util;

import com.ibm.websphere.ola.ConnectionSpecImpl;
import com.ibm.ws390.ola.jca.ConnectionRequestInfoImpl;

public class ConnectionRequestInfoImplHelper {

	static public ConnectionRequestInfoImpl create(String username, String password) {
		ConnectionSpecImpl csi = new ConnectionSpecImpl();
		csi.setUsername(username);
		csi.setPassword(password);
		return create(csi);
	}
	
	static public ConnectionRequestInfoImpl create(ConnectionSpecImpl cspecImpl) {
		return new ConnectionRequestInfoImpl(cspecImpl.getRegisterName(),
						cspecImpl.getConnectionWaitTimeout(),
						cspecImpl.getLinkTaskTranID(),
						cspecImpl.getLinkTaskReqContID(),
						cspecImpl.getLinkTaskReqContType(),
						cspecImpl.getLinkTaskRspContID(),
						cspecImpl.getLinkTaskRspContType(),
						cspecImpl.getUseCICSContainer(),
						cspecImpl.getLinkTaskChanID(),
						cspecImpl.getLinkTaskChanType(),
						cspecImpl.getUseOTMA(),
						cspecImpl.getOTMAClientName(),
						cspecImpl.getOTMAServerName(),
						cspecImpl.getOTMAGroupID(),
						cspecImpl.getOTMASyncLevel(),
						cspecImpl.getOTMAMaxSegments(),
						cspecImpl.getOTMAMaxRecvSize(),
						cspecImpl.getOTMARequestLLZZ(),
						cspecImpl.getOTMAResponseLLZZ(),
						cspecImpl.getUsername(),
						cspecImpl.getPassword(),
						cspecImpl.getRRSTransactional(),
						cspecImpl.getConnectionWaitTimeoutFromCSI(),
						cspecImpl.getlinkTaskReqContTypeFromCSI(),
						cspecImpl.getlinkTaskRspContTypeFromCSI(),
						cspecImpl.getlinkTaskChanTypeFromCSI(),
						cspecImpl.getuseCICSContainerFromCSI(),
						cspecImpl.getOTMAMaxSegmentsFromCSI(),
						cspecImpl.getOTMAMaxRecvSizeFromCSI(),
						cspecImpl.getOTMARequestLLZZFromCSI(),
						cspecImpl.getOTMAResponseLLZZFromCSI(),
						cspecImpl.getRRSTransactionalFromCSI());
	}

}
