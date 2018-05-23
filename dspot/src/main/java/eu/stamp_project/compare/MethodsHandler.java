package eu.stamp_project.compare;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;


public class MethodsHandler {

    private Map<Class<?>, List<Method>> cache;

    private static final List<String> forbiddenMethods;

    static {
        forbiddenMethods = new ArrayList<>();
        forbiddenMethods.add("equals");
        forbiddenMethods.add("notify");
        forbiddenMethods.add("notifyAll");
        forbiddenMethods.add("wait");
        forbiddenMethods.add("getClass");
        forbiddenMethods.add("display");
        forbiddenMethods.add("clone");
        forbiddenMethods.add("hasExtensions");
        forbiddenMethods.add("hashCode");
        forbiddenMethods.add("toString");

        // since we generate contains(), we don't need to observe iterators
        forbiddenMethods.add("iterator");
        forbiddenMethods.add("spliterator");
        forbiddenMethods.add("listIterator");
        forbiddenMethods.add("stream");
        forbiddenMethods.add("parallelStream");
    }

    public MethodsHandler() {
        this.cache = new HashMap<>();
    }

    public List<Method> getAllMethods(Class<?> clazz) {
        if (!cache.containsKey(clazz)) {
            findMethods(clazz);
        }
        return cache.get(clazz);
    }

    private void findMethods(Class<?> clazz) {
        List<Method> methodsList = new ArrayList<Method>();
        for (Method m : clazz.getMethods()) {
            if (isValidMethod(m)) {
                methodsList.add(m);
            }
        }
        cache.put(clazz, methodsList);
    }

    private boolean isValidMethod(Method m) {
        if (!Modifier.isPublic(m.getModifiers())
                || Modifier.isStatic(m.getModifiers())
                || isVoid(m.getReturnType())
                || m.getReturnType() == Class.class) {
            return false;
        }
        // we only consider methods that take no parameter
        return (!forbiddenMethods.contains(m.getName()) ||
                ( (!m.getDeclaringClass().equals(Object.class) && !m.getDeclaringClass().equals(Enum.class))
                        && (m.getName().equals("hashCode") || m.getName().equals("toString"))))
                        && m.getParameterTypes().length == 0;
    }

    private static boolean isVoid(Class<?> type) {
        return type.equals(Void.class) || type.equals(void.class);
    }

}
