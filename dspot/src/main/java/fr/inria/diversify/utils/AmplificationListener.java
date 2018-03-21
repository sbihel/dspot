package fr.inria.diversify.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.experimental.modelobs.ActionBasedChangeListenerImpl;
import spoon.experimental.modelobs.action.AddAction;
import spoon.experimental.modelobs.action.DeleteAction;
import spoon.experimental.modelobs.action.UpdateAction;

import java.util.HashMap;
import java.util.Map;

public class AmplificationListener extends ActionBasedChangeListenerImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmplificationListener.class);

    private static final Map<Object, AmplificationCategory> AmplificationLog = new HashMap<>();

    @Override
    public void onAdd(AddAction action) {
        super.onAdd(action);
        AmplificationLog.put(action.getNewValue(), AmplificationCategory.ADD);
        LOGGER.info("ADD Amplification applied");
    }

    @Override
    public void onDelete(DeleteAction action) {
        super.onDelete(action);
        AmplificationLog.put(action.getRemovedValue(), AmplificationCategory.REMOVE);
        LOGGER.info("REMOVE Amplification applied");
    }

    @Override
    public void onUpdate(UpdateAction action) {
        super.onUpdate(action);
        AmplificationLog.put(action.getNewValue(), AmplificationCategory.MODIFY);
        LOGGER.info("MODIFY Amplification applied");
    }
}
