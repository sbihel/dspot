package fr.inria.diversify.utils;

import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.path.CtRole;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;

public class AmplificationLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmplificationLog.class);

    public ConcurrentMap<CtMethod, ArrayList<ActionLog>> amplificationLog = new MapMaker().weakKeys().concurrencyLevel(4).makeMap();

    public enum AmplificationCategory {
        ADD, REMOVE, MODIFY
    }

    public class ActionLog {
        public String parent;
        public int parentLine;
        public String role;
        public String oldValue;
        public String newValue;
        public String ampCategory;

        private ActionLog(CtElement parent, CtRole role, Object oldValue, Object newValue, AmplificationCategory ampCategory) {
            this.parent = parent.toString();
            this.parentLine = parent.getPosition().getLine();
            this.role = role.toString();
            this.oldValue = oldValue.toString();
            this.newValue = newValue.toString();
            this.ampCategory = ampCategory.toString();
        }
    }

    public void reset() {
        this.amplificationLog.clear();
    }

    private void addAmplification(CtMethod amplifiedTest, ActionLog actionLog) {
        if (amplificationLog.containsKey(amplifiedTest)) {
            amplificationLog.get(amplifiedTest).add(actionLog);
        } else {
            ArrayList<ActionLog> newAmpArray = new ArrayList<>();
            newAmpArray.add(actionLog);
            amplificationLog.put(amplifiedTest, newAmpArray);
        }
    }

    public void logModifyAmplification(CtMethod amplifiedTest, CtElement parent, CtRole role, Object oldValue, Object newValue) {
        addAmplification(amplifiedTest, new ActionLog(parent, role, oldValue, newValue, AmplificationCategory.MODIFY));
    }

    public void logAddAmplification(CtMethod amplifiedTest, CtElement parent, CtRole role, Object newValue) {
        addAmplification(amplifiedTest, new ActionLog(parent, role, "", newValue, AmplificationCategory.ADD));
    }

    public void logRemoveAmplification(CtMethod amplifiedTest, CtElement parent, CtRole role, Object oldValue) {
        addAmplification(amplifiedTest, new ActionLog(parent, role, oldValue, "", AmplificationCategory.REMOVE));
    }
}
