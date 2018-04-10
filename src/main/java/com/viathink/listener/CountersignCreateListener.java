package com.viathink.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

import java.io.Serializable;

public class CountersignCreateListener implements TaskListener, Serializable {
    public void notify(DelegateTask delegateTask) {
        System.out.println("--------------CountersignCreateListener.......");
    }
}
