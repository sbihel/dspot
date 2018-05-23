package eu.stamp_project.dspot.assertGenerator;

import eu.stamp_project.testrunner.runner.test.Failure;
import eu.stamp_project.AbstractTest;
import eu.stamp_project.Utils;
import org.junit.Test;
import spoon.reflect.declaration.CtMethod;

import static org.junit.Assert.assertNull;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 13/05/18
 */
public class TryCatchFailGeneratorTest extends AbstractTest {

    @Test
    public void testAssertionErrorFailureReturnNull() throws Exception {

        /*
            When the cause of the failure is an AssertionError, it means that there is still
            an assertions inside the test, while it shouldn't
            In this case, we return null, since surrounding with try/catch such error is
            meaningless.
         */

        final String testName = "testAssertionError";
        final String testClassName = "fr.inria.filter.failing.FailingTest";
        final CtMethod testAssertionError = Utils.findMethod(testClassName, testName);
        final TryCatchFailGenerator tryCatchFailGenerator = new TryCatchFailGenerator();
        final CtMethod<?> ctMethod = tryCatchFailGenerator
                .surroundWithTryCatchFail(testAssertionError,
                        new Failure(testName, testClassName, new AssertionError())
                );
        assertNull(ctMethod);
    }

    @Test
    public void testSurroundWithTryCatchFail() throws Exception {

        final String testName = "testFailingWithException";
        final String testClassName = "fr.inria.filter.failing.FailingTest";
        final CtMethod testAssertionError = Utils.findMethod(testClassName, testName);
        final TryCatchFailGenerator tryCatchFailGenerator = new TryCatchFailGenerator();
        CtMethod<?> ctMethod = tryCatchFailGenerator
                .surroundWithTryCatchFail(testAssertionError,
                        new Failure(testName, testClassName, new NullPointerException())
                );
        ctMethod = tryCatchFailGenerator
                .surroundWithTryCatchFail(ctMethod,
                        new Failure(testName, testClassName, new ArrayIndexOutOfBoundsException(-100))
                );
        System.out.println(ctMethod);
    }
}
