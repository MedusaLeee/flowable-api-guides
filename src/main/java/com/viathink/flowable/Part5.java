package com.viathink.flowable;

import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;

public class Part5 {

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
    // 添加测试用户
    @Test
    public void addGroupAndUser() {
        IdentityService identityService = processEngine.getIdentityService();
        //创建用户
        User user1 = identityService.newUser("user1");
        user1.setFirstName("user1");
        user1.setLastName("");
        user1.setEmail("user1@viathink.com");
        identityService.saveUser(user1);
        // 用户加入组
        identityService.createMembership("user1", "develop");
        // 创建用户
        User user2 = identityService.newUser("user2");
        user2.setFirstName("user2");
        user2.setLastName("");
        user2.setEmail("user2@viathink.com");
        identityService.saveUser(user2);
        // 用户加入组
        identityService.createMembership("user2", "develop");
        // 创建用户
        User user3 = identityService.newUser("user3");
        user3.setFirstName("user3");
        user3.setLastName("");
        user3.setEmail("user3@viathink.com");
        identityService.saveUser(user3);
        // 用户加入组
        identityService.createMembership("user3", "develop");
        User user4 = identityService.newUser("user4");
        user4.setFirstName("user4");
        user4.setLastName("");
        user4.setEmail("user4@viathink.com");
        identityService.saveUser(user4);
        // 用户加入组
        identityService.createMembership("user4", "develop");
        User user5 = identityService.newUser("user5");
        user5.setFirstName("user5");
        user5.setLastName("");
        user5.setEmail("user5@viathink.com");
        identityService.saveUser(user5);
        // 用户加入组
        identityService.createMembership("user5", "develop");
    }
    // 部署流程
    @Test
    public void deployProcess() throws Exception {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        String filePath = "bpmn/gateway.bpmn";
        // 获取输入流
        FileInputStream in = new FileInputStream(filePath);
        // 部署
        repositoryService
                .createDeployment()
                .name("Gateway")
                .addInputStream("gateway.bpmn", in)
                .deploy();
        // 验证部署是否部署成功
        ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
        List<ProcessDefinition> pdList = pdq.processDefinitionKey("gateway").list();
        assertNotNull(pdList);
        for (ProcessDefinition pd: pdList) {
            System.out.println("id: " + pd.getId());
            System.out.println("name: " + pd.getName());
            System.out.println("version: " + pd.getVersion());
        }
    }
    // 启动流程实例
    @Test
    public void startProcess() {
        // 定义一个流程单据id，流程单据name 模拟系统内单据id和name，也可存多个如存快照等
        String sheetId = "12345";
        String sheetName = "网关测试";
        RuntimeService runtimeService = processEngine.getRuntimeService();
        // 如果多个key相同的流程定义，会启动version最高的流程
        // 操作数据库的act_ru_execution表,如果是用户任务节点，同时也会在act_ru_task添加一条记录
        // 设置流程变量的时候，会向act_ru_variable表添加数据
        Map<String, Object> variableMap = new HashMap<String, Object>();
        variableMap.put("sheetId", sheetId);
        variableMap.put("sheetName", sheetName);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("gateway", variableMap);
        assertNotNull(pi);
        System.out.println("pid: " + pi.getId() + ",activitiId: " + pi.getActivityId() + ",pdId: " + pi.getProcessDefinitionId());
    }
    @Test
    public void setVariables() {
        // 2. 通过TaskService
        String taskId = "5007";
        TaskService taskService = processEngine.getTaskService();
        // 设置的是当前任务的流程变量，任务结束后就会进入历史表
        taskService.setVariableLocal(taskId, "variable2", "variable2");
    }
    // 获取某个用户的待审批任务
    @Test
    public void queryPersonalTask() {
        String assignee = "user5";
        TaskService taskService = processEngine.getTaskService();
        List<Task> taskList = taskService
                .createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();
        for(Task task: taskList) {
            System.out.println("id: " + task.getId());
            System.out.println("name: " + task.getName());
            System.out.println("crateTime: " + task.getCreateTime());
            System.out.println("assignee: " + task.getAssignee());
        }
    }
    // 办理任务
    @Test
    public void completeTask() {
        String taskId = "75008";
        TaskService taskService = processEngine.getTaskService();
        // 对于执行完的任务，activiti将从act_ru_task表中删除该任务，下一个任务会被插入进来
        taskService.complete(taskId);
    }
    // 办理并且设置流程变量
    @Test
    public void completeTaskAndSetVariables() {
        String taskId = "70007";
        TaskService taskService = processEngine.getTaskService();
        // 对于执行完的任务，activiti将从act_ru_task表中删除该任务，下一个任务会被插入进来
        Map<String, Object> variableMap = new HashMap<String, Object>();
        variableMap.put("agree", false);
        taskService.complete(taskId, variableMap);
    }
}
