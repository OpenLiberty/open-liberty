package testservlet40.jar.jandex_v3;

//This class tests both subinterfaces and ensures we have an example of a class without a no-args constructor
public class SubinterfacesImpl implements SubInterface {
	
	String msg;
	
	public SubinterfacesImpl(String m) {
		msg = m;
	}
	
	public String getMsg() {
		return msg;
	}
	
	public String getNote( ) {
		return "note";
	}	
}

