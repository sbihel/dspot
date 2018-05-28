package eu.stamp_project.utils;

import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.path.CtRole;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class AmplificationLog {
    private static AmplificationLog _instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmplificationLog.class);

    private Map<CtMethod, ArrayList<ActionLog>> amplificationLog;

    private AmplificationLog() {
        this.amplificationLog = new IdentityHashMap<>();
    }

    private static AmplificationLog getInstance() {
        if (_instance == null) {
            _instance = new AmplificationLog();
        }
        return _instance;
    }

    public enum AmplificationCategory {
        ADD, REMOVE, MODIFY, ASSERT
    }

    public static class ActionLog {
        public String parent;
        public int parentLine;
        public String role;
        public String oldValue;
        public String newValue;
        public String ampCategory;

        private ActionLog(CtElement parent, CtRole role, Object oldValue, Object newValue, AmplificationCategory ampCategory) {
            this.parent = parent.getShortRepresentation();
            this.parentLine = parent.getPosition().getLine();
            this.role = (role == null)? "child": role.toString();
            this.oldValue = (oldValue == null)? "": oldValue.toString();
            this.newValue = (newValue == null)? "": newValue.toString();
            this.ampCategory = ampCategory.toString();
        }
    }

    public static void reset() {
        _instance = null;
    }

    private static void addAmplification(CtMethod amplifiedTest, ActionLog actionLog) {
        LOGGER.info("Amplification logged.");
        if (getInstance().amplificationLog.containsKey(amplifiedTest)) {
            getInstance().amplificationLog.get(amplifiedTest).add(actionLog);
        } else {
            ArrayList<ActionLog> newAmpArray = new ArrayList<>();
            newAmpArray.add(actionLog);
            getInstance().amplificationLog.put(amplifiedTest, newAmpArray);
        }
    }

    public static void logModifyAmplification(CtMethod amplifiedTest, CtElement parent, CtRole role, Object oldValue, Object newValue) {
        addAmplification(amplifiedTest, new ActionLog(parent, role, oldValue, newValue, AmplificationCategory.MODIFY));
    }

    public static void logAddAmplification(CtMethod amplifiedTest, CtElement parent, CtRole role, Object newValue) {
        addAmplification(amplifiedTest, new ActionLog(parent, role, "", newValue, AmplificationCategory.ADD));
    }

    public static void logRemoveAmplification(CtMethod amplifiedTest, CtElement parent, CtRole role, Object oldValue) {
        addAmplification(amplifiedTest, new ActionLog(parent, role, oldValue, "", AmplificationCategory.REMOVE));
    }

    public static void logAssertAmplification(CtMethod amplifiedTest, CtElement parent, CtRole role, Object newValue) {
        addAmplification(amplifiedTest, new ActionLog(parent, role, "", newValue, AmplificationCategory.ASSERT));
    }

    public static Set<CtMethod> getKeySet() {
        return getInstance().amplificationLog.keySet();
    }

    public static ArrayList<ActionLog> getAmplifications(CtMethod amplifiedTest) {
        return getInstance().amplificationLog.get(amplifiedTest);
    }
}
