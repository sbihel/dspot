package eu.stamp_project.dspot.amplifier;

import eu.stamp_project.utils.AmplificationChecker;
import eu.stamp_project.utils.AmplificationHelper;
import eu.stamp_project.utils.AmplificationLog;
import eu.stamp_project.utils.Counter;
import eu.stamp_project.utils.DSpotUtils;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.List;


public class TestMethodCallRemover implements Amplifier {

    public List<CtMethod> apply(CtMethod method) {
        List<CtMethod> methods = new ArrayList<>();

        if (method.getDeclaringType() != null) {
            //get the list of method calls
            List<CtInvocation> invocations = Query.getElements(method, new TypeFilter(CtInvocation.class));
            //this index serves to replace ith literal is replaced by zero in the ith clone of the method
            int invocation_index = 0;
            for (CtInvocation invocation : invocations) {
                try {
                    if (toRemove(invocation)
                            && !AmplificationChecker.isAssert(invocation)
                            && !inWhileLoop(invocation)
                            && !containsIteratorNext(invocation)) {
                        methods.add(apply(method, invocation_index));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                invocation_index++;
            }
        }
        return methods;
    }

    @Override
    public void reset(CtType testClass) {
        AmplificationHelper.reset();
    }

    private CtMethod apply(CtMethod method, int invocation_index) {
        //clone the method
        CtMethod<?> cloned_method = AmplificationHelper.cloneTestMethodForAmp(method, "_remove");

        //get the lit_indexth literal of the cloned method
        CtInvocation stmt = Query.getElements(cloned_method, new TypeFilter<>(CtInvocation.class)).get(invocation_index);
        CtBlock b = ((CtBlock) stmt.getParent());
        DSpotUtils.addComment(b, "removed " + stmt.toString() + " at line " + stmt.getPosition().getLine(), CtComment.CommentType.INLINE);
        b.removeStatement(stmt);

        Counter.updateInputOf(cloned_method, 1);
        AmplificationLog.logRemoveAmplification(cloned_method, b, stmt.getRoleInParent(), stmt);

        return cloned_method;
    }

    private boolean toRemove(CtInvocation invocation) {
        return invocation.getParent() instanceof CtBlock
                && invocation.getParent(CtTry.class) == null;
    }

    private boolean inWhileLoop(CtStatement stmt) {
        return stmt.getParent(CtWhile.class) != null;
    }

    private boolean containsIteratorNext(CtStatement stmt) {
        return stmt.toString().contains(".next()");
    }
}
