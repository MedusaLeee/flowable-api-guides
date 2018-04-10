package com.viathink.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

import java.io.Serializable;

public class CountersignCompleteListener implements TaskListener, Serializable {
    public void notify(DelegateTask delegateTask) {
        System.out.println("--------------CountersignCompleteListener.......");
        Long oldApprovedCounter = (Long) delegateTask.getVariable("approvedCounter");
        Long approvedCounter = oldApprovedCounter + 1;
        delegateTask.setVariable("approvedCounter", approvedCounter);
    }
}
