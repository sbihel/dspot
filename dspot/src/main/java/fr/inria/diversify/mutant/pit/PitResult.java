package fr.inria.diversify.mutant.pit;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.util.List;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 1/4/17
 */
public class PitResult {

    /**
     * Possible states of a mutant.
     * <li>{@link #SURVIVED}</li>
     * <li>{@link #KILLED}</li>
     * <li>{@link #NO_COVERAGE}</li>
     * <li>{@link #TIMED_OUT}</li>
     * <li>{@link #NON_VIABLE}</li>
     * <li>{@link #MEMORY_ERROR}</li>
     */
    public enum State {
        /**
         * The mutant is live, undetected by the test suite.
         */
        SURVIVED,

        /**
         * The mutant was detected by a failing test.
         */
        KILLED,

        /**
         * The mutation has not been executed by any test.
         */
        NO_COVERAGE,

        /**
         * The execution of a test timed out.
         */
        TIMED_OUT,

        /**
         * The mutation could not be loaded into the jvm.
         */
        NON_VIABLE,

        /**
         * The jvm ran out of memory while executing the test suite.
         */
        MEMORY_ERROR
    }

    /**
     * Name of the class that contains the mutation.
     */
    private final String fullQualifiedNameOfMutatedClass;

    /**
     * Name of the mutator applied.
     */
    private final String fullQualifiedNameMutantOperator;

    /**
     * Name of the method that contains the mutation.
     */
    private final String nameOfMutatedMethod;

    /**
     *  Line number where the mutation is.
     */
    private final int lineNumber;

    /**
     * State of the mutant. See {@link State}.
     */
    private final State stateOfMutant;

    /**
     * Path of the test method that killed the mutant.
     */
    private final String fullQualifiedNameOfKiller;

    /**
     * Name of the test method that killed the mutant.
     */
    private final String simpleNameMethod;

    /**
     * Root AST node of the test method that killed the mutant.
     */
    private CtMethod testCase = null;

    public PitResult(String fullQualifiedNameOfMutatedClass, State stateOfMutant,
                     String fullQualifiedNameMutantOperator,
                     String fullQualifiedNameMethod, String fullQualifiedNameOfKiller,
                     int lineNumber,
                     String nameOfLocalisation) {
        this.fullQualifiedNameOfMutatedClass = fullQualifiedNameOfMutatedClass;
        this.stateOfMutant = stateOfMutant;
        this.fullQualifiedNameMutantOperator = fullQualifiedNameMutantOperator;
        this.fullQualifiedNameOfKiller = fullQualifiedNameOfKiller;
        String[] split = fullQualifiedNameMethod.split("\\.");
        this.simpleNameMethod = split[split.length - 1];
        this.lineNumber = lineNumber;
        this.nameOfMutatedMethod = nameOfLocalisation;
    }

    public State getStateOfMutant() {
        return stateOfMutant;
    }

    public String getFullQualifiedNameMutantOperator() {
        return fullQualifiedNameMutantOperator;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getNameOfMutatedMethod() {
        return nameOfMutatedMethod;
    }

    public String getFullQualifiedNameOfKiller() {
        return fullQualifiedNameOfKiller;
    }

    public CtMethod getMethod(CtType<?> ctClass) {
        if ("none".equals(this.simpleNameMethod)) {
            return null;
        } else {
            if (this.testCase == null) {
                List<CtMethod<?>> methodsByName = ctClass.getMethodsByName(this.simpleNameMethod);
                if (methodsByName.isEmpty()) {
                    if (ctClass.getSuperclass() != null) {
                        return getMethod(ctClass.getSuperclass().getDeclaration());
                    } else {
                        return null;
                    }
                }
                this.testCase = methodsByName.get(0);
            }
            return this.testCase;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PitResult result = (PitResult) o;

        return lineNumber == result.lineNumber &&
                fullQualifiedNameOfMutatedClass.endsWith(result.fullQualifiedNameOfMutatedClass) &&
                nameOfMutatedMethod.equals(result.nameOfMutatedMethod) &&
                fullQualifiedNameMutantOperator.equals(result.fullQualifiedNameMutantOperator);
    }

    @Override
    public int hashCode() {
        int result = stateOfMutant != null ? stateOfMutant.hashCode() : 0;
        result = 31 * result + (fullQualifiedNameMutantOperator != null ? fullQualifiedNameMutantOperator.hashCode() : 0);
        result = 31 * result + lineNumber;
        result = 31 * result + (nameOfMutatedMethod != null ? nameOfMutatedMethod.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PitResult{" +
                "fullQualifiedNameOfMutatedClass='" + fullQualifiedNameOfMutatedClass + '\'' +
                ", fullQualifiedNameMutantOperator='" + fullQualifiedNameMutantOperator + '\'' +
                ", nameOfMutatedMethod='" + nameOfMutatedMethod + '\'' +
                ", lineNumber=" + lineNumber +
                ", stateOfMutant=" + stateOfMutant +
                ", fullQualifiedNameOfKiller='" + fullQualifiedNameOfKiller + '\'' +
                ", simpleNameMethod='" + simpleNameMethod + '\'' +
                ", testCase=" + testCase +
                '}';
    }
}
