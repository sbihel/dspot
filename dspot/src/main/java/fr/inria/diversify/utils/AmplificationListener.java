package fr.inria.diversify.utils;

import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.SpoonException;
import spoon.experimental.modelobs.ActionBasedChangeListenerImpl;
import spoon.experimental.modelobs.action.AddAction;
import spoon.experimental.modelobs.action.DeleteAction;
import spoon.experimental.modelobs.action.UpdateAction;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtReference;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;

/**
 * Listener for changes (i.e. amplifications) applied.
 */
public class AmplificationListener extends ActionBasedChangeListenerImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmplificationListener.class);

    /**
     * Maps modified ast nodes (in the form of triplet parent+role+value) to the amplification applied.
     */
    public final ConcurrentMap<CtMethod, ArrayList<ActionLog>> amplificationLog;

    /**
     * Bundles keys for the amplification map.
     * <p>
     * Similar to a tuple in its usage.
     */
    public class ActionLog {
        public String parent;
        public int parentLine;
        public String role;
        public String oldValue;
        public String newValue;
        public String ampCategory;

        private ActionLog(CtElement parent, CtRole role, Object oldValue, Object newValue, fr.inria.diversify.utils.AmplificationCategory ampCategory) {
            if (parent != null) {
                try {
                    if (parent instanceof CtReference) {
                        this.parent = ((CtReference) parent).getSimpleName();
                    } else if (parent instanceof CtComment) {
                        this.parent = "COMMENT:" + ((CtComment) parent).getContent();
                    } else {
                        this.parent = parent.toString();
                    }
                } catch (SpoonException e) {
                    this.parent = parent.getClass().toString();
                }
            } else {
                this.parent = "null";
            }
            this.parentLine = (parent != null)? parent.getPosition().getLine(): -1;
            this.role = role.toString();
            this.oldValue = (oldValue != null)? oldValue.toString(): "null";
            this.newValue = (newValue != null)? newValue.toString(): "null";
            this.ampCategory = ampCategory.toString();
        }
    }

    public AmplificationListener() {
        super();
        amplificationLog = new MapMaker().weakKeys().concurrencyLevel(4).makeMap();
    }

    private void addAmpToLog(CtMethod testMethod, ActionLog newAction) {
        if (testMethod == null)
            return;
        if (amplificationLog.containsKey(testMethod)) {
            amplificationLog.get(testMethod).add(newAction);
        } else {
            ArrayList<ActionLog> newAmpArray = new ArrayList<>();
            newAmpArray.add(newAction);
            amplificationLog.put(testMethod, newAmpArray);
        }
    }

    private CtMethod getRootMethod(CtElement changeParent) {
        CtMethod testMethodRoot;
        if (changeParent instanceof CtMethod) {
            testMethodRoot = (CtMethod) changeParent;
        } else if (changeParent.getParent(CtMethod.class) != null) {
            testMethodRoot = changeParent.getParent(CtMethod.class);
        } else { // not related to amplification
            testMethodRoot = null;
        }
        return testMethodRoot;
    }

    @Override
    public void onAdd(AddAction action) {
        LOGGER.info("ADD Amplification applied");
        super.onAdd(action);
        final ActionLog actionLog = new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), null, action.getNewValue(), AmplificationCategory.ADD);
        CtElement parent = action.getContext().getElementWhereChangeHappens();
        CtMethod testMethodRoot = getRootMethod(parent);
        if (testMethodRoot == null)
            return;
        addAmpToLog(testMethodRoot, actionLog);
    }

    @Override
    public void onDelete(DeleteAction action) {
        LOGGER.info("REMOVE Amplification applied");
        super.onDelete(action);
        final ActionLog actionLog = new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), action.getRemovedValue(), null, AmplificationCategory.REMOVE);
        CtElement parent = action.getContext().getElementWhereChangeHappens();
        CtMethod testMethodRoot = getRootMethod(parent);
        if (testMethodRoot == null)
            return;
        addAmpToLog(testMethodRoot, actionLog);
    }

    @Override
    public void onUpdate(UpdateAction action) {
        LOGGER.info("MODIFY Amplification applied");
        super.onUpdate(action);
        final ActionLog actionLog = new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), action.getOldValue(), action.getNewValue(), AmplificationCategory.MODIFY);
        CtElement parent = action.getContext().getElementWhereChangeHappens();
        CtMethod testMethodRoot = getRootMethod(parent);
        if (testMethodRoot == null)
            return;
        addAmpToLog(testMethodRoot, actionLog);
    }

    public ArrayList<ActionLog> getAmplificationList(CtMethod<?> testMethod) {
        return amplificationLog.get(testMethod);
    }

    public boolean hasLog(CtMethod<?> testMethod) {
        return amplificationLog.containsKey(testMethod);
    }
}
