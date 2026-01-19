package testservlet40.jar.jandex_v3;

public abstract sealed public class SealedClass permits SealedClass_SubclassA, SealedClass_SubclassB, SealedClass_SubclassC {
	
	public abstract String getMsg();
}

final class SealedClass_SubclassA extends SealedClass {
	public String getMsg() {
		return "A";
	}
}

sealed class SealedClass_SubclassB extends SealedClass permits SealedClass_SubclassB2 {
	public String getMsg() {
		return "B";
	}
}

final class SealedClass_SubclassB2 extends SealedClass {
	public String getMsg() {
		return "B2";
	}
}

non-sealed class SealedClass_SubclassBC extends SealedClass {
	public String getMsg() {
		return "C";
	}
}
