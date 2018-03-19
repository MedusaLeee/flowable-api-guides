package com.viathink.flowable;

import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

/**
 * 委派
 */
public class Part2 {
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
        String sheetName = "预算编制";
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
    // 委派任务
    @Test
    public void delegateTask() {
        String currentUser = "lijianxun";
        String delegateUser = "kangbeibei";
        TaskService taskService = processEngine.getTaskService();
        // 查找当前审批人的待审批任务
        Task task = taskService.createTaskQuery().taskAssignee(currentUser).singleResult();
        // 第一次被分配任务后被委派人为空
        assertNull(task.getOwner());
        // 执行委派任务
        taskService.delegateTask(task.getId(), delegateUser);
        // 查看数据状态
        task = taskService.createTaskQuery().taskAssignee(delegateUser).singleResult();
        System.out.println("普通任务查询 task: " + task);
        task = taskService
                .createTaskQuery()
                .taskDelegationState(DelegationState.PENDING)
                .taskAssignee(delegateUser)
                .singleResult();
        assertEquals(task.getOwner(), currentUser);
        assertEquals(task.getAssignee(), delegateUser);
        System.out.println("委派查询：id: " + task.getId() + " ,owner: " + task.getOwner() + " ,assignee: " + task.getAssignee());
    }
    // 被委派人完成任务
    @Test
    public void resolveTask() {
        String taskId = "15008";
        String currentUser = "lijianxun";
        TaskService taskService = processEngine.getTaskService();
        taskService.resolveTask(taskId); // 被委派人完成任务
        // 任务回归到委派人
        Task task = taskService
                .createTaskQuery()
                .taskDelegationState(DelegationState.RESOLVED)
                .taskAssignee(currentUser)
                .singleResult();
        // 被委派人完成任务后拥有者或办理人都为 currentUser
        System.out.println("委派查询：id: " + task.getId() + " ,owner: " + task.getOwner() + " ,assignee: " + task.getAssignee());
        assertEquals(task.getOwner(), currentUser);
        assertEquals(task.getAssignee(), currentUser);
        // 最后委派人完成任务....
    }

}
