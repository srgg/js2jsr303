package mobi.srg.js2jsr303.jsr303;

import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
*  This class was created because of "package" visibility of an AnnotationInvocationHandler class shipped with Sun/Oracle JDK.
*/
class AnnotationInvocationHandler implements InvocationHandler {
    private final Class<? extends Annotation> type;
    private final Map<String, Object> memberValues;
    private transient volatile Method[] memberMethods = null;

    public AnnotationInvocationHandler(Class<? extends Annotation> type, Map<String, Object> memberValues) {
        this.type = type;
        this.memberValues = memberValues;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String methodName = method.getName();

        Class[] var5 = method.getParameterTypes();
        if(methodName.equals("equals") && var5.length == 1 && var5[0] == Object.class) {
            return this.equalsImpl(args[0]);
        }

        switch (methodName){
            case "hashCode":
                return hashCodeImpl();
            case "toString":
                return toStringImpl();
            case "annotationType":
                return type;
            default:
                Object v = this.memberValues.get(methodName);
                if(v == null) {
                    throw new IncompleteAnnotationException(this.type, methodName);
//                    } else if(v instanceof ExceptionProxy) {
//                        throw ((ExceptionProxy)v).generateException();
                } else {
                    if(v.getClass().isArray() && Array.getLength(v) != 0) {
                        v = this.cloneArray(v);
                    }

                    return v;
                }
        }
    }

    private Object cloneArray(Object var1) {
        Class var2 = var1.getClass();
        if(var2 == byte[].class) {
            byte[] var11 = (byte[])((byte[])var1);
            return var11.clone();
        } else if(var2 == char[].class) {
            char[] var9 = (char[])((char[])var1);
            return var9.clone();
        } else if(var2 == double[].class) {
            double[] var10 = (double[])((double[])var1);
            return var10.clone();
        } else if(var2 == float[].class) {
            float[] var8 = (float[])((float[])var1);
            return var8.clone();
        } else if(var2 == int[].class) {
            int[] var7 = (int[])((int[])var1);
            return var7.clone();
        } else if(var2 == long[].class) {
            long[] var6 = (long[])((long[])var1);
            return var6.clone();
        } else if(var2 == short[].class) {
            short[] var5 = (short[])((short[])var1);
            return var5.clone();
        } else if(var2 == boolean[].class) {
            boolean[] var4 = (boolean[])((boolean[])var1);
            return var4.clone();
        } else {
            Object[] var3 = (Object[])((Object[])var1);
            return var3.clone();
        }
    }

    private String toStringImpl() {
        StringBuffer stringBuffer = new StringBuffer(128);
        stringBuffer.append('@');
        stringBuffer.append(this.type.getName());
        stringBuffer.append('(');
        boolean var2 = true;
        Iterator iterator = this.memberValues.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry e = (Map.Entry)iterator.next();
            if(var2) {
                var2 = false;
            } else {
                stringBuffer.append(", ");
            }

            stringBuffer.append((String) e.getKey());
            stringBuffer.append('=');
            stringBuffer.append(memberValueToString(e.getValue()));
        }

        stringBuffer.append(')');
        return stringBuffer.toString();
    }

    private static String memberValueToString(Object value) {
        Class clazz = value.getClass();
        return !clazz.isArray()?value.toString():(clazz == byte[].class? Arrays.toString((byte[]) ((byte[]) value)):(clazz == char[].class?Arrays.toString((char[])((char[])value)):(clazz == double[].class?Arrays.toString((double[])((double[])value)):(clazz == float[].class?Arrays.toString((float[])((float[])value)):(clazz == int[].class?Arrays.toString((int[])((int[])value)):(clazz == long[].class?Arrays.toString((long[])((long[])value)):(clazz == short[].class?Arrays.toString((short[])((short[])value)):(clazz == boolean[].class?Arrays.toString((boolean[])((boolean[])value)):Arrays.toString((Object[])((Object[])value))))))))));
    }

    private int hashCodeImpl() {
        int code = 0;

        Map.Entry e;
        for(Iterator iterator = this.memberValues.entrySet().iterator(); iterator.hasNext(); code += 127 * ((String)e.getKey()).hashCode() ^ memberValueHashCode(e.getValue())) {
            e = (Map.Entry)iterator.next();
        }

        return code;
    }

    private static int memberValueHashCode(Object value) {
        Class clazz = value.getClass();
        return !clazz.isArray()?value.hashCode():(clazz == byte[].class?Arrays.hashCode((byte[])((byte[])value)):(clazz == char[].class?Arrays.hashCode((char[])((char[])value)):(clazz == double[].class?Arrays.hashCode((double[])((double[])value)):(clazz == float[].class?Arrays.hashCode((float[])((float[])value)):(clazz == int[].class?Arrays.hashCode((int[])((int[])value)):(clazz == long[].class?Arrays.hashCode((long[])((long[])value)):(clazz == short[].class?Arrays.hashCode((short[])((short[])value)):(clazz == boolean[].class?Arrays.hashCode((boolean[])((boolean[])value)):Arrays.hashCode((Object[])((Object[])value))))))))));
    }

    private Boolean equalsImpl(Object var1) {
        if(var1 == this) {
            return Boolean.valueOf(true);
        } else if(!this.type.isInstance(var1)) {
            return Boolean.valueOf(false);
        } else {
            Method[] var2 = this.getMemberMethods();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Method var5 = var2[var4];
                String var6 = var5.getName();
                Object var7 = this.memberValues.get(var6);
                Object var8 = null;
                AnnotationInvocationHandler var9 = this.asOneOfUs(var1);
                if(var9 != null) {
                    var8 = var9.memberValues.get(var6);
                } else {
                    try {
                        var8 = var5.invoke(var1, new Object[0]);
                    } catch (InvocationTargetException var11) {
                        return Boolean.valueOf(false);
                    } catch (IllegalAccessException var12) {
                        throw new AssertionError(var12);
                    }
                }

                if(!memberValueEquals(var7, var8)) {
                    return Boolean.valueOf(false);
                }
            }

            return Boolean.valueOf(true);
        }
    }

    private Method[] getMemberMethods() {
        if(this.memberMethods == null) {
            this.memberMethods = (Method[]) AccessController.doPrivileged(new PrivilegedAction() {
                public Method[] run() {
                    Method[] var1 = AnnotationInvocationHandler.this.type.getDeclaredMethods();
                    AccessibleObject.setAccessible(var1, true);
                    return var1;
                }
            });
        }

        return this.memberMethods;
    }
    private AnnotationInvocationHandler asOneOfUs(Object var1) {
        if(Proxy.isProxyClass(var1.getClass())) {
            InvocationHandler var2 = Proxy.getInvocationHandler(var1);
            if(var2 instanceof AnnotationInvocationHandler) {
                return (AnnotationInvocationHandler)var2;
            }
        }

        return null;
    }

    private static boolean memberValueEquals(Object v1, Object v2) {
        Class c1 = v1.getClass();
        if(!c1.isArray()) {
            return v1.equals(v2);
        } else if(v1 instanceof Object[] && v2 instanceof Object[]) {
            return Arrays.equals((Object[])((Object[])v1), (Object[])((Object[])v2));
        } else if(v2.getClass() != c1) {
            return false;
        } else if(c1 == byte[].class) {
            return Arrays.equals((byte[])((byte[])v1), (byte[])((byte[])v2));
        } else if(c1 == char[].class) {
            return Arrays.equals((char[])((char[])v1), (char[])((char[])v2));
        } else if(c1 == double[].class) {
            return Arrays.equals((double[])((double[])v1), (double[])((double[])v2));
        } else if(c1 == float[].class) {
            return Arrays.equals((float[])((float[])v1), (float[])((float[])v2));
        } else if(c1 == int[].class) {
            return Arrays.equals((int[])((int[])v1), (int[])((int[])v2));
        } else if(c1 == long[].class) {
            return Arrays.equals((long[])((long[])v1), (long[])((long[])v2));
        } else if(c1 == short[].class) {
            return Arrays.equals((short[])((short[])v1), (short[])((short[])v2));
        } else {
            assert c1 == boolean[].class;

            return Arrays.equals((boolean[])((boolean[])v1), (boolean[])((boolean[])v2));
        }
    }

    public static <A extends Annotation> A create( Class<A> annotationClass, Map<String, Object> values){
        return (A) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{annotationClass}, new AnnotationInvocationHandler(annotationClass, values));
    }
}
