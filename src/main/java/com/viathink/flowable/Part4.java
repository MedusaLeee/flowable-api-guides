package com.viathink.flowable;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Before;
import org.junit.Test;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;

/**
 * 监听器
 * 监听器分为执行监听器和任务监听器
 * 审批完成
 ---------------TaskCreateListener----------------
 TaskCreate task: Task[id=35008, name=李建勋]
 TaskCreate notifyType: email
 TaskCreate task id: 35008
 TaskCreate execution id: 35005
 TaskCreate event name: complete, 审批人：lijianxun
 ---------------TaskCreateListener----------------
 ---------------TaskCompleteListener----------------
 TaskComplete task: Task[id=50008, name=李建勋]
 TaskComplete notifyType: email
 TaskComplete task id: 50008
 TaskComplete execution id: 50005
 TaskComplete event name: complete, 审批人：lijianxun
 ---------------TaskCompleteListener----------------
 */
public class Part4 {
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
        String filePath = "bpmn/listener.bpmn";
        // 获取输入流
        FileInputStream in = new FileInputStream(filePath);
        // 部署
        repositoryService
            .createDeployment()
            .name("监听器流程-0322")
            .addInputStream("listener.bpmn", in)
            .deploy();
        // 验证部署是否部署成功
        ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
        List<ProcessDefinition> pdList = pdq.processDefinitionKey("listenerProcess").list();
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
        String sheetName = "listener-1";
        String startUser = "lijianxun";
        // 指定AuthenticatedUserId后，启动流程 startUser 字段会有值
        IdentityService identityService = processEngine.getIdentityService();
        identityService.setAuthenticatedUserId(startUser);
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> variableMap = new HashMap<String, Object>();
        variableMap.put("sheetId", sheetId);
        variableMap.put("sheetName", sheetName);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("listenerProcess", sheetId, variableMap);
        assertNotNull(pi);
        System.out.println("pid: " + pi.getId()
                + " ,activitiId: " + pi.getActivityId()
                + " ,pdId: " + pi.getProcessDefinitionId()
                + " ,startUser: " + pi.getStartUserId()
                + " ,businessKey: " + pi.getBusinessKey()
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
    @Test
    public void completeTask() {
        String taskId = "52502";
        TaskService taskService = processEngine.getTaskService();
        // 对于执行完的任务，activiti将从act_ru_task表中删除该任务，下一个任务会被插入进来
        taskService.complete(taskId);
    }
    @Test
    public void completeTaskWithComment() {
        String taskId = "62504";
        String userId = "lijianxun";
        IdentityService identityService = processEngine.getIdentityService();
        identityService.setAuthenticatedUserId(userId);
        TaskService taskService = processEngine.getTaskService();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        System.out.println("processInstanceId: " + task.getProcessInstanceId());
        taskService.addComment(taskId, task.getProcessInstanceId(), "同意，审批通过");
        // 对于执行完的任务，activiti将从act_ru_task表中删除该任务，下一个任务会被插入进来
        taskService.complete(taskId);
        // 查找流程实例的审批意见 #247
        List<Comment> taskComments = taskService.getProcessInstanceComments(task.getProcessInstanceId());
        for(Comment c: taskComments) {
            System.out.printf("任务: %s, 审批人：%s， 审批意见: %s", c.getTaskId(), c.getUserId(), c.getFullMessage());
        }
    }
    // 根据目前任务的id去查找此任务同一个流程实例的历史评论 #246
    @Test
    public void getProcessHistoryComments() {
        String taskId = "62504";
        TaskService taskService = processEngine.getTaskService();
        HistoryService historyService = processEngine.getHistoryService();
        // 通过task获取流程实例的id
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        System.out.println("processInstanceId: " + processInstanceId);
        // 通过流程实例查询所有的(用户任务类型)历史活动, 也可以查活动的 # http://www.cnblogs.com/cxyj/p/3898535.html
        List<Comment> historyCommnets = new ArrayList<Comment>();
        List<HistoricActivityInstance> hais = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).activityType("userTask").list();
//       3）查询每个历史任务的批注
        for (HistoricActivityInstance hai : hais) {
            String historytaskId = hai.getTaskId();
            List<Comment> comments = taskService.getTaskComments(historytaskId);
            // 4）如果当前任务有批注信息，添加到集合中
            if(comments!=null && comments.size()>0){
                historyCommnets.addAll(comments);
            }
        }
        for (Comment c: historyCommnets) {
            System.out.println("comment: " + c.getId() + ",userId: " + c.getUserId() + ",message: " + c.getFullMessage());
        }
    }
}
