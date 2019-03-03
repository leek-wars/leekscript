package leekscript;

public class ErrorManager {

	public static void exception(Throwable e) {

		System.out.println(e);
		System.out.println(traceToString(e));
	}

	public static void exception(Throwable e, int ai) {

		System.out.println(e);
		System.out.println(traceToString(e));
	}
	
	public static String traceToString(Throwable throwable) {

		StringBuilder sb = new StringBuilder();
		sb.append(throwable.toString());
		int nb = 0;
		for (StackTraceElement t : throwable.getStackTrace()) {
			nb++;
			if (nb > 20)
				break;
			sb.append("\n\tat ").append(t.getClassName()).append(".").append(t.getMethodName());
			if (!t.isNativeMethod())
				sb.append("(").append(t.getFileName()).append(":").append(t.getLineNumber()).append(")");
		}
		return sb.toString();
	}
}
