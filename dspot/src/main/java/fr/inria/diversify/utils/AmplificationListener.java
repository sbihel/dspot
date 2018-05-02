package fr.inria.diversify.utils;

import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.experimental.modelobs.ActionBasedChangeListenerImpl;
import spoon.experimental.modelobs.action.AddAction;
import spoon.experimental.modelobs.action.DeleteAction;
import spoon.experimental.modelobs.action.UpdateAction;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.path.CtRole;

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
    private static final ConcurrentMap<CtMethod<?>, ArrayList<ActionLog>> AmplificationLog = new MapMaker().weakKeys().concurrencyLevel(4).makeMap();

    /**
     * Bundles keys for the amplification map.
     * <p>
     * Similar to a tuple in its usage.
     */
    public class ActionLog {
        public CtElement getParent() {
            return parent;
        }

        public CtRole getRole() {
            return role;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public AmplificationCategory getAmpCategory() {
            return ampCategory;
        }

        private CtElement parent;
        private CtRole role;
        private Object oldValue;
        private Object newValue;
        private AmplificationCategory ampCategory;

        private ActionLog(CtElement parent, CtRole role, Object oldValue, Object newValue, fr.inria.diversify.utils.AmplificationCategory ampCategory) {
            this.parent = parent;
            this.role = role;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.ampCategory = ampCategory;
        }
    }

    private void addAmpToLog(CtMethod testMethod, ActionLog newAction) {
        if (AmplificationLog.containsKey(testMethod)) {
            AmplificationLog.get(testMethod).add(newAction);
        } else {
            ArrayList<ActionLog> newAmpArray = new ArrayList<>();
            newAmpArray.add(newAction);
            AmplificationLog.put(testMethod, newAmpArray);
        }
    }

    @Override
    public void onAdd(AddAction action) {
        super.onAdd(action);
        final ActionLog actionLog = new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), null, action.getNewValue(), AmplificationCategory.ADD);
        CtElement parent = action.getContext().getElementWhereChangeHappens();
        while (!(parent instanceof CtMethod))
            parent = parent.getParent();
        CtMethod testMethodRoot = (CtMethod) parent;
        addAmpToLog(testMethodRoot, actionLog);
        LOGGER.info("ADD Amplification applied");
    }

    @Override
    public void onDelete(DeleteAction action) {
        super.onDelete(action);
        final ActionLog actionLog = new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), action.getRemovedValue(), null, AmplificationCategory.REMOVE);
        CtElement parent = action.getContext().getElementWhereChangeHappens();
        while (!(parent instanceof CtMethod))
            parent = parent.getParent();
        CtMethod testMethodRoot = (CtMethod) parent;
        addAmpToLog(testMethodRoot, actionLog);
        LOGGER.info("REMOVE Amplification applied");
    }

    @Override
    public void onUpdate(UpdateAction action) {
        super.onUpdate(action);
        final ActionLog actionLog = new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), action.getOldValue(), action.getNewValue(), AmplificationCategory.MODIFY);
        CtElement parent = action.getContext().getElementWhereChangeHappens();
        while (!(parent instanceof CtMethod))
            parent = parent.getParent();
        CtMethod testMethodRoot = (CtMethod) parent;
        addAmpToLog(testMethodRoot, actionLog);
        LOGGER.info("MODIFY Amplification applied");
    }

    public ArrayList<ActionLog> getAmplificationList(CtMethod<?> testMethod) {
        return AmplificationLog.get(testMethod);
    }

    public boolean hasLog(CtMethod<?> testMethod) {
        return AmplificationLog.containsKey(testMethod);
    }
}
