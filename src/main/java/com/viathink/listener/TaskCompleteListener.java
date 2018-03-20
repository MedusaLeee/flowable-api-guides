package com.viathink.listener;

import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

import java.io.Serializable;

// https://www.flowable.org/docs/userguide/index.html#springExpressions
public class TaskCompleteListener implements TaskListener, Serializable {
    private Expression notifyType;
    private Expression task;
    public void notify(DelegateTask delegateTask) {
        try {
            System.out.println("---------------TaskCompleteListener----------------");
            System.out.printf("TaskComplete task: %s \n", task.getValue(delegateTask));
            System.out.printf("TaskComplete notifyType: %s \n", notifyType.getValue(delegateTask));
            System.out.printf("TaskComplete task id: %s \n", delegateTask.getId());
            System.out.printf("TaskComplete execution id: %s \n", delegateTask.getExecutionId());
            System.out.printf("TaskComplete event name: %s, 审批人：%s \n", delegateTask.getEventName(), delegateTask.getAssignee());
            System.out.println("---------------TaskCompleteListener----------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Expression getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(Expression notifyType) {
        this.notifyType = notifyType;
    }

    public Expression getTask() {
        return task;
    }

    public void setTask(Expression task) {
        this.task = task;
    }
}
