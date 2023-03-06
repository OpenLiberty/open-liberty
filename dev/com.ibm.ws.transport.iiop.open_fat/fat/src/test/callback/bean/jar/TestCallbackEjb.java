package test.callback.bean.jar;

import java.rmi.RemoteException;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import test.iiop.common.ClientCallbackService;
import test.iiop.common.ServerCallbackService;
import test.iiop.notcommon.VersionedException;

@Stateless
@Remote(ServerCallbackService.class)
public class TestCallbackEjb implements ServerCallbackService {
    public TestCallbackEjb() {}

    @Override
    public void throwRuntimeException(ClientCallbackService invocand) throws RemoteException {
        if (invocand == null)
            throw new VersionedException();
        else
            invocand.throwRuntimeException();
    }

    @Override
    public String toString() {
        return "TestCallbackEjb []";
    }
}
