package edu.cmu.jacoco;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;

public class ExecutionDataVisitor implements IExecutionDataVisitor {

    private final ExecutionDataStore executionDataStore = new ExecutionDataStore();

    private StoreStrategy storeStrategy = data -> true;

    public void setStoreStrategy(StoreStrategy storeStrategy) {
        this.storeStrategy = storeStrategy;
    }

    public ExecutionDataStore getExecutionDataStore() {
        return executionDataStore;
    }

    @Override
    public void visitClassExecution(ExecutionData data) {
        if (storeStrategy.shouldBeStored(data)) {
            executionDataStore.visitClassExecution(data);
        }
    }

    interface StoreStrategy {
        boolean shouldBeStored(ExecutionData data);
    }
}
