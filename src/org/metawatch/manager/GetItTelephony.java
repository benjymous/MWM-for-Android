package org.metawatch.manager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GetItTelephony {
    public static Object TRY() {
	try {
	    Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
	    Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
	    Object phoneService = getServiceMethod.invoke(null, "phone");
	    Class<?> ITelephonyClass = Class.forName("com.android.internal.telephony.ITelephony");
	    Class<?> ITelephonyStubClass = null;
	    for (Class<?> clazz : ITelephonyClass.getDeclaredClasses()) {
		if (clazz.getSimpleName().equals("Stub")) {
		    ITelephonyStubClass = clazz;
		    break;
		}
	    }
	    if (ITelephonyStubClass != null) {
		Class<?> IBinderClass = Class.forName("android.os.IBinder");
		Method asInterfaceMethod = ITelephonyStubClass.getDeclaredMethod("asInterface", IBinderClass);
		return asInterfaceMethod.invoke(null, phoneService);
	    } else {
		Log.d("TTT", "Unable to locate ITelephony.Stub class!");
	    }
	} catch (ClassNotFoundException ex) {
	    Log.e("TTT", "Failed to clear missed calls notification due to ClassNotFoundException!", ex);
	} catch (InvocationTargetException ex) {
	    Log.e("TTT", "Failed to clear missed calls notification due to InvocationTargetException!", ex);
	} catch (NoSuchMethodException ex) {
	    Log.e("TTT", "Failed to clear missed calls notification due to NoSuchMethodException!", ex);
	} catch (Throwable ex) {
	    Log.e("TTT", "Failed to clear missed calls notification due to Throwable!", ex);
	}
	return null;
    }
}