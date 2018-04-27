package fr.inria.diversify.utils;

import spoon.reflect.declaration.CtMethod;

/**
 * Produces a report of amplifications applied.
 */
public class AmplificationReport {
    public static String generateAmplificationReport(CtMethod amplifiedTest, AmplificationListener amplificationListener) {
        if (!(amplificationListener.hasLog(amplifiedTest)))
            return "";

        StringBuilder report = new StringBuilder();
        for (AmplificationListener.ActionLog action : amplificationListener.getAmplificationList(amplifiedTest)) {
            switch (action.getAmpCategory()) {
                case ADD:
                    report.append("New ");
                    break;
                case MODIFY:
                    report.append("Modified ");
                    break;
                case REMOVE:
                    report.append("Removed ");
                    break;
            }
            report.append(action.getRole()).append(" in ").append(action.getParent()).append(AmplificationHelper.LINE_SEPARATOR);
        }
        return report.toString();
    }
}
