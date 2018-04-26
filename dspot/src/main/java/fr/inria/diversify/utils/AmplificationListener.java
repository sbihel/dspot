package fr.inria.diversify.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.experimental.modelobs.ActionBasedChangeListenerImpl;
import spoon.experimental.modelobs.action.Action;
import spoon.experimental.modelobs.action.AddAction;
import spoon.experimental.modelobs.action.DeleteAction;
import spoon.experimental.modelobs.action.UpdateAction;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AmplificationListener extends ActionBasedChangeListenerImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmplificationListener.class);

    private static final ConcurrentMap<ActionLog, AmplificationCategory> AmplificationLog = new ConcurrentHashMap<>();

    private class ActionLog {
        private CtElement parent;
        private CtRole role;
        private Object value;

        public ActionLog(CtElement parent, CtRole role, Object value) {
            this.parent = parent;
            this.role = role;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ActionLog actionLog = (ActionLog) o;
            return Objects.equals(parent, actionLog.parent) &&
                    role == actionLog.role &&
                    Objects.equals(value, actionLog.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, role, value);
        }
    }

    @Override
    public void onAdd(AddAction action) {
        super.onAdd(action);
        AmplificationLog.put(new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), action.getNewValue()), AmplificationCategory.ADD);
        LOGGER.info("ADD Amplification applied");
    }

    @Override
    public void onDelete(DeleteAction action) {
        super.onDelete(action);
        AmplificationLog.put(new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), action.getRemovedValue()), AmplificationCategory.REMOVE);
        LOGGER.info("REMOVE Amplification applied");
    }

    @Override
    public void onUpdate(UpdateAction action) {
        super.onUpdate(action);
        AmplificationLog.put(new ActionLog(action.getContext().getElementWhereChangeHappens(), action.getContext().getChangedProperty(), action.getNewValue()), AmplificationCategory.MODIFY);
        LOGGER.info("MODIFY Amplification applied");
    }

    public boolean isAmplification(CtElement parent, CtRole role, Object value) {
        return AmplificationLog.containsKey(new ActionLog(parent, role, value));
    }

    public AmplificationCategory getAmplificationCategory(CtElement parent, CtRole role, Object value) {
        return AmplificationLog.get(new ActionLog(parent, role, value));
    }
}
