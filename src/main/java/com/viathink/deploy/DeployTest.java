package com.viathink.deploy;

import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;

public class DeployTest {

    private ProcessEngine processEngine = null;
    @Before
    public void setUp() {
        System.out.println("before....");
    }
    @Before
    public void createDeployment() {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration();
        cfg.setJdbcDriver("com.mysql.jdbc.Driver");
        cfg.setJdbcUrl("jdbc:mysql://localhost:3306/flowable621?useUnicode=true&characterEncoding=utf8&useSSL=true");
        cfg.setJdbcUsername("root");
        cfg.setJdbcPassword("root");
        cfg.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        processEngine = cfg.buildProcessEngine();
    }
    @Test
    public void addGroupAndUser() {
        IdentityService identityService = processEngine.getIdentityService();
        //创建组
        Group developGroup = identityService.newGroup("develop");
        developGroup.setName("开发部");
        developGroup.setType("assignee");
        identityService.saveGroup(developGroup);
        Group managerGroup = identityService.newGroup("manager");
        managerGroup.setName("管理部");
        managerGroup.setType("assignee");
        identityService.saveGroup(managerGroup);
        //创建用户
        User user1 = identityService.newUser("lijianxun");
        user1.setFirstName("李建勋");
        user1.setLastName("");
        user1.setEmail("lijianxun@viathink.com");
        identityService.saveUser(user1);
        // 用户加入组
        identityService.createMembership("lijianxun", "develop");
        // 创建用户
        User user2 = identityService.newUser("guoshuang");
        user2.setFirstName("郭爽");
        user2.setLastName("");
        user2.setEmail("guoshuang@viathink.com");
        identityService.saveUser(user2);
        // 用户加入组
        identityService.createMembership("guoshuang", "develop");
        // 创建用户
        User user3 = identityService.newUser("kangbeibei");
        user3.setFirstName("康蓓蓓");
        user3.setLastName("");
        user3.setEmail("kangbeibei@viathink.com");
        identityService.saveUser(user3);
        // 用户加入组
        identityService.createMembership("kangbeibei", "manager");
    }
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
        }
    }
    // 启动流程实例
    @Test
    public void startProcess() {
        // 定义一个流程单据id，流程单据name 模拟系统内单据id和name，也可存多个如存快照等
        String sheetId = "12345";
        String sheetName = "预算编制";
        RuntimeService runtimeService = processEngine.getRuntimeService();
        // 如果多个key相同的流程定义，会启动version最高的流程
        // 操作数据库的act_ru_execution表,如果是用户任务节点，同时也会在act_ru_task添加一条记录
        // 设置流程变量的时候，会向act_ru_variable表添加数据
        // TODO 流程变量的作用域？
        Map<String, Object> variableMap = new HashMap<String, Object>();
        variableMap.put("sheetId", sheetId);
        variableMap.put("sheetName", sheetName);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("myProcess", variableMap);
        assertNotNull(pi);
        System.out.println("pid: " + pi.getId() + ",activitiId: " + pi.getActivityId() + ",pdId: " + pi.getProcessDefinitionId());
    }
    // 获取某个用户的待审批任务
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
            System.out.println("id: " + task.getId());
            System.out.println("name: " + task.getName());
            System.out.println("crateTime: " + task.getCreateTime());
            System.out.println("assignee: " + task.getAssignee());
        }
    }
    // 办理任务
    @Test
    public void completeTask() {
        String taskId = "5007";
        TaskService taskService = processEngine.getTaskService();
        // 对于执行完的任务，activiti将从act_ru_task表中删除该任务，下一个任务会被插入进来
        taskService.complete(taskId);
        // 执行完后任务将被派发给下一审批人员或组
    }
    // 查询组任务，组内的人都能查询到，谁签收，谁处理
    @Test
    public void queryGroupTask() {
        String assignee = "lijianxun";
        TaskService taskService = processEngine.getTaskService();
        List<Task> taskList = taskService
                .createTaskQuery()
                .taskCandidateUser(assignee)
                .orderByTaskCreateTime().desc()
                .list();
        for(Task task: taskList) {
            System.out.println("id: " + task.getId());
            System.out.println("name: " + task.getName());
            System.out.println("crateTime: " + task.getCreateTime());
            System.out.println("assignee: " + task.getAssignee());
            System.out.println("category: " + task.getCategory());
        }
    }
    // 签收组任务
    @Test
    public void claimTask() {
        String assignee = "guoshuang";
        String taskId = "7502";
        TaskService taskService = processEngine.getTaskService();
        taskService.claim(taskId, assignee);
        // 然后按指定人去查询是否已经签收任务成功，签收成功后组内其他人无法查询到组任务
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
    // 办理组任务，本测试完成组任务后就结束了
    @Test
    public void completeGroupTask() {
        String taskId = "7502";
        TaskService taskService = processEngine.getTaskService();
        // 对于执行完的任务，activiti将从act_ru_task表中删除该任务，下一个任务会被插入进来
        taskService.complete(taskId);
    }
}
