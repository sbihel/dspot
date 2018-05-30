package eu.stamp_project.dspot.amplifier;

import eu.stamp_project.AbstractTest;
import eu.stamp_project.Utils;
import eu.stamp_project.utils.AmplificationHelper;
import org.junit.Test;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 11/30/16
 */
public class StatementAddTest extends AbstractTest {

    @Test
    public void testWithLoop() throws Exception {
        /*
            Test that StatementAdd amplifier is able to add statement inside a loop if this loop has not brackets
         */

        final String packageName = "fr.inria.statementadd";
        final Factory factory = Utils.getFactory();
        AmplificationHelper.setSeedRandom(32L);
        StatementAdd amplifier = new StatementAdd(packageName);
        amplifier.reset(factory.Class().get(packageName + ".ClassTarget"));

        CtMethod<?> ctMethod = Utils.findMethod(factory.Class().get(packageName + ".TestClassTarget"), "testWithLoop");
        List<CtMethod> amplifiedMethods = amplifier.apply(ctMethod);

        final String expectedTest = "{" + AmplificationHelper.LINE_SEPARATOR +
                "    java.util.ArrayList<fr.inria.statementadd.TestClassTarget.Internal> internalList = new java.util.ArrayList<>();" + AmplificationHelper.LINE_SEPARATOR +
                "    internalList.add(new fr.inria.statementadd.TestClassTarget.Internal());" + AmplificationHelper.LINE_SEPARATOR +
                "    for (fr.inria.statementadd.TestClassTarget.Internal i : internalList) {" + AmplificationHelper.LINE_SEPARATOR +
                "        int __DSPOT_i_0 = -1167796541;" + AmplificationHelper.LINE_SEPARATOR +
                "        i.compute(0);" + AmplificationHelper.LINE_SEPARATOR +
                "        // StatementAdd: add invocation of a method" + AmplificationHelper.LINE_SEPARATOR +
                "        i.compute(__DSPOT_i_0);" + AmplificationHelper.LINE_SEPARATOR +
                "    }" + AmplificationHelper.LINE_SEPARATOR +
                "}";

        String amplifiedTestString = amplifiedMethods.get(0).getBody().toString();
        Set<String> testLines = new HashSet<>(Arrays.asList(amplifiedTestString.split(AmplificationHelper.LINE_SEPARATOR)));
        Set<String> expectedLines = new HashSet<>(Arrays.asList(expectedTest.split(AmplificationHelper.LINE_SEPARATOR)));
        assertEquals(expectedLines, testLines);
    }

    @Test
    public void testOnClassWithJavaObjects() throws Exception {

        /*
            Test that the StatementAdd amplifier is able to generate, and manage Collection and Map from java.util
         */

        final String packageName = "fr.inria.statementadd";
        final Factory factory = Utils.getFactory();
        AmplificationHelper.setSeedRandom(32L);
        StatementAdd amplifier = new StatementAdd(packageName);
        amplifier.reset(factory.Class().get(packageName + ".ClassTarget"));

        CtMethod<?> ctMethod = Utils.findMethod(factory.Class().get(packageName + ".TestClassTarget"), "test");
        List<CtMethod> amplifiedMethods = amplifier.apply(ctMethod);

        assertEquals(7, amplifiedMethods.size());

        List<String> expectedCalledMethod = Arrays.asList(
                "getList",
                "getSizeOf",
                "getSizeOfTypedCollection",
                "getSizeOfTypedMap"
        );
        assertTrue(amplifiedMethods.stream()
                .allMatch(amplifiedMethod ->
                        amplifiedMethod.filterChildren(new TypeFilter<CtInvocation<?>>(CtInvocation.class) {
                            @Override
                            public boolean matches(CtInvocation<?> element) {
                                return expectedCalledMethod.contains(element.getExecutable().getSimpleName());
                            }
                        }).first() != null
                ));
    }

    @Test
    public void testStatementAddOnArrayObjects() throws Exception {
        final String packageName = "fr.inria.statementaddarray";
        final Factory factory = Utils.getFactory();
        AmplificationHelper.setSeedRandom(32L);
        StatementAdd amplifier = new StatementAdd(packageName);
        amplifier.reset(factory.Class().get(packageName + ".ClassTargetAmplify"));

        CtMethod<?> ctMethod = Utils.findMethod(factory.Class().get(packageName + ".TestClassTargetAmplify"), "test");
        List<CtMethod> amplifiedMethods = amplifier.apply(ctMethod);

        System.out.println(amplifiedMethods);

        assertEquals(5, amplifiedMethods.size());

        List<String> expectedCalledMethod = Arrays.asList(
                "methodWithArrayParatemeter",
                "methodWithArrayParatemeterFromDomain",
                "methodWithDomainParameter",
                "methodWithReturn",
                "method1"
        );
        assertTrue(amplifiedMethods.stream()
                .allMatch(amplifiedMethod ->
                        amplifiedMethod.filterChildren(new TypeFilter<CtInvocation<?>>(CtInvocation.class) {
                            @Override
                            public boolean matches(CtInvocation<?> element) {
                                return expectedCalledMethod.contains(element.getExecutable().getSimpleName());
                            }
                        }).first() != null
                ));
    }

    @Test
    public void testStatementAddOnUnderTest() throws Exception {
        Factory factory = Utils.getFactory();
        CtClass<Object> ctClass = factory.Class().get("fr.inria.mutation.ClassUnderTestTest");
        AmplificationHelper.setSeedRandom(23L);

        StatementAdd amplificator = new StatementAdd();
        amplificator.reset(ctClass);

        CtMethod originalMethod = Utils.findMethod(ctClass, "testLit");

        List<CtMethod> amplifiedMethods = amplificator.apply(originalMethod);

        System.out.println(amplifiedMethods);

        assertEquals(2, amplifiedMethods.size());

        List<String> expectedCalledMethod = Arrays.asList(
                "plusOne",
                "minusOne"
        );
        assertTrue(amplifiedMethods.stream()
                .allMatch(amplifiedMethod ->
                        amplifiedMethod.filterChildren(new TypeFilter<CtInvocation<?>>(CtInvocation.class) {
                            @Override
                            public boolean matches(CtInvocation<?> element) {
                                return expectedCalledMethod.contains(element.getExecutable().getSimpleName());
                            }
                        }).first() != null
                ));
    }

    @Test
    public void testStatementAdd() throws Exception {

        /*
            Test the StatementAdd amplifier. It reuse existing object to add method call of accessible method.
            It can reuse return value to add method call. It results here with 7 new test cases.
         */

        final String packageName = "fr.inria.statementadd";
        final Factory factory = Utils.getFactory();
        AmplificationHelper.setSeedRandom(42L);
        StatementAdd amplifier = new StatementAdd(packageName);
        amplifier.reset(factory.Class().get(packageName + ".ClassTargetAmplify"));

        CtMethod<?> ctMethod = Utils.findMethod(factory.Class().get(packageName + ".TestClassTargetAmplify"), "test");
        List<CtMethod> amplifiedMethods = amplifier.apply(ctMethod);

        System.out.println(amplifiedMethods);

        assertEquals(6, amplifiedMethods.size());

        List<String> expectedCalledMethod = Arrays.asList(
                "method",
                "methodWithDomainParameter",
                "methodWithPrimitifParameters",
                "methodWithPrimitifParameters",
                "methodWithReturn",
                "method1"
        );
        assertTrue(amplifiedMethods.stream()
                .allMatch(amplifiedMethod ->
                        amplifiedMethod.filterChildren(new TypeFilter<CtInvocation<?>>(CtInvocation.class) {
                            @Override
                            public boolean matches(CtInvocation<?> element) {
                                return expectedCalledMethod.contains(element.getExecutable().getSimpleName());
                            }
                        }).first() != null
                ));
    }

}
