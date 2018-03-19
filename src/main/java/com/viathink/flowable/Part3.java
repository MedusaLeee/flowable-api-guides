package com.viathink.flowable;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.*;

/**
 * 解除审批、回退
 * 如果上一个节点是开始节点则不能回退
 * 分支节点还需要根据需求详细测试判断条件，如是否可以回退，分支节点如何判断来源
 */
public class Part3 {
    private ProcessEngine processEngine = null;
    // 初始化流程引擎
    @Before
    public void createDeployment() {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration();
        cfg.setJdbcDriver("com.mysql.cj.jdbc.Driver");
        cfg.setJdbcUrl("jdbc:mysql://localhost:3306/flowable621?useUnicode=true&characterEncoding=utf8&useSSL=false");
        cfg.setJdbcUsername("root");
        cfg.setJdbcPassword("rootroot");
        cfg.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        processEngine = cfg.buildProcessEngine();
    }

    // 部署流程
    @Test
    public void deployProcess() throws Exception {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        String filePath = "bpmn/develop.bpmn";
        // 获取输入流
        FileInputStream in = new FileInputStream(filePath);
        // 部署
        repositoryService
                .createDeployment()
                .name("流程定义1")
                .addInputStream("develop.bpmn", in)
                .deploy();
        // 验证部署是否部署成功
        ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
        List<ProcessDefinition> pdList = pdq.processDefinitionKey("myProcess").list();
        assertNotNull(pdList);
        for (ProcessDefinition pd: pdList) {
            System.out.println("id: " + pd.getId());
            System.out.println("name: " + pd.getName());
            System.out.println("version: " + pd.getVersion());
            System.out.println("deploymentId: " + pd.getDeploymentId());
        }
    }
    // 启动流程实例
    @Test
    public void startProcess() {
        String sheetId = "12345";
        String sheetName = "预算编制-1";
        String startUser = "lijianxun";
        // 指定AuthenticatedUserId后，启动流程 startUser 字段会有值
        IdentityService identityService = processEngine.getIdentityService();
        identityService.setAuthenticatedUserId(startUser);
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> variableMap = new HashMap<String, Object>();
        variableMap.put("sheetId", sheetId);
        variableMap.put("sheetName", sheetName);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("myProcess", variableMap);
        assertNotNull(pi);
        System.out.println("pid: " + pi.getId()
                + " ,activitiId: " + pi.getActivityId()
                + " ,pdId: " + pi.getProcessDefinitionId()
                + " ,startUser: " + pi.getStartUserId()
        );
    }
    @Test
    public void queryPersonalTask() {
        String assignee = "lijianxun";
        TaskService taskService = processEngine.getTaskService();
        List<Task> taskList = taskService
                .createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();
        for(Task task: taskList) {
            System.out.println("pId:" + task.getProcessInstanceId());
            System.out.println("id: " + task.getId());
            System.out.println("name: " + task.getName());
            System.out.println("crateTime: " + task.getCreateTime());
            System.out.println("assignee: " + task.getAssignee());
        }
    }
    // 获取上一个审批节点
    @Test
    public void getPreviousNode() {
        Task task = processEngine.getTaskService().createTaskQuery().taskId("12502").singleResult();
        System.out.println("task:" + task);
        Execution ee = processEngine.getRuntimeService().createExecutionQuery()
                .executionId(task.getExecutionId()).singleResult();
        // 当前审批节点
        String currentActivityId = ee.getActivityId();
        System.out.println("currentActivityId: " + currentActivityId);
        // 获取当前节点的 flowNode对象
        BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(task.getProcessDefinitionId());
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(currentActivityId);
        // 获得指向该节点的连线
        List<SequenceFlow> incomeFlows = flowNode.getIncomingFlows();
        for (SequenceFlow incomeFlow : incomeFlows) {
            System.out.println("incomeFlow: " + incomeFlow.getSourceFlowElement().getId());
        }
    }
    // 如果在第一个节点撤回会怎么样？回退不了！！！
    @Test
    public void withdrawTask() {
        String previousActivityId = "startevent1";
        String currentActivityId = "usertask1";
        String processInstanceId = "7501";
        processEngine.getRuntimeService().createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .cancelActivityId(currentActivityId)
                .startActivityId(previousActivityId)
                .changeState();
    }
    // 先完成任务，再回退
    @Test
    public void completeGroupTask() {
        String taskId = "10004";
        TaskService taskService = processEngine.getTaskService();
        // 对于执行完的任务，activiti将从act_ru_task表中删除该任务，下一个任务会被插入进来
        taskService.complete(taskId);
    }
    // 撤回任务后需要再完成一下任务看是否还能继续
    @Test
    public void withdrawTask2() {
        String previousActivityId = "usertask1";
        String currentActivityId = "userTask2";
        String processInstanceId = "7501";
        processEngine.getRuntimeService().createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .cancelActivityId(currentActivityId)
                .startActivityId(previousActivityId)
                .changeState();
    }
    // 回退任务后再完成，如果没有指定审批人也能完成任务，！！！需要检查！！！
    @Test
    public void completeGroupTask2() {
        String taskId = "17502";
        TaskService taskService = processEngine.getTaskService();
        taskService.complete(taskId);
    }
}
