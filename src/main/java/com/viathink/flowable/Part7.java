package com.viathink.flowable;

import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;

// 会签
public class Part7 {

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
        String filePath = "bpmn/countersign.bpmn";
        // 获取输入流
        FileInputStream in = new FileInputStream(filePath);
        // 部署
        repositoryService
                .createDeployment()
                .name("会签")
                .addInputStream("countersign.bpmn", in)
                .deploy();
        // 验证部署是否部署成功
        ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
        List<ProcessDefinition> pdList = pdq.processDefinitionKey("countersign").list();
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
        String sheetName = "会签";
        RuntimeService runtimeService = processEngine.getRuntimeService();
        // 如果多个key相同的流程定义，会启动version最高的流程
        // 操作数据库的act_ru_execution表,如果是用户任务节点，同时也会在act_ru_task添加一条记录
        // 设置流程变量的时候，会向act_ru_variable表添加数据
        Map<String, Object> variableMap = new HashMap<String, Object>();
        variableMap.put("sheetId", sheetId);
        variableMap.put("sheetName", sheetName);
        // 通过此流程变量来指定会签的人员
        variableMap.put("assigneeList", Arrays.asList("lijianxun", "guoshuang", "kangbeibei"));
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("countersign", variableMap);
        assertNotNull(pi);
        System.out.println("pid: " + pi.getId() + ",activitiId: " + pi.getActivityId() + ",pdId: " + pi.getProcessDefinitionId());
    }
    // 获取某个用户的待审批任务及查看流程变量
    @Test
    public void queryPersonalTask() {
        String assignee = "kangbeibei";
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
            // 流程变量
            List assigneeList = (List) taskService.getVariable(task.getId(), "assigneeList");
            Long approvedCounter = (Long) taskService.getVariable(task.getId(), "approvedCounter");
            System.out.println("approvedCounter: " + approvedCounter);
            for (Object as: assigneeList) {
                System.out.println("assignee: " + as);
            }
        }
    }
    // 办理任务
    @Test
    public void completeTask() {
        String taskId = "15032";
        TaskService taskService = processEngine.getTaskService();
        // 对于执行完的任务，activiti将从act_ru_task表中删除该任务，下一个任务会被插入进来
        taskService.complete(taskId);
        // 执行完后任务将被派发给下一审批人员或组
    }
}
