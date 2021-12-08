/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.repo.workflow.activiti;

import java.util.Map;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.alfresco.repo.tenant.TenantUtil;

/**
 * @author Nick Smith
 * @since 3.4.e
 */
public class ActivitiUtil
{
    private final RepositoryService repoService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final ManagementService managementService;
    private boolean deployWorkflowsInTenant;
    private boolean retentionHistoricProcessInstance;

    public ActivitiUtil(ProcessEngine engine, boolean deployWorkflowsInTenant)
    {
        this.repoService = engine.getRepositoryService();
        this.runtimeService = engine.getRuntimeService();
        this.taskService = engine.getTaskService();
        this.historyService = engine.getHistoryService();
        this.managementService = engine.getManagementService();
        this.deployWorkflowsInTenant = deployWorkflowsInTenant;
    }

    public ActivitiUtil(ProcessEngine engine, boolean deployWorkflowsInTenant, boolean retentionHistoricProcessInstance)
    {
        this.repoService = engine.getRepositoryService();
        this.runtimeService = engine.getRuntimeService();
        this.taskService = engine.getTaskService();
        this.historyService = engine.getHistoryService();
        this.managementService = engine.getManagementService();
        this.deployWorkflowsInTenant = deployWorkflowsInTenant;
        this.retentionHistoricProcessInstance = retentionHistoricProcessInstance;
    }

    public ProcessDefinition getProcessDefinition(String definitionId)
    {
        return repoService.createProcessDefinitionQuery()
            .processDefinitionId(definitionId)
            .singleResult();
    }

    public ProcessDefinition getProcessDefinitionByKey(String processKey)
    {
        return repoService.createProcessDefinitionQuery()
            .processDefinitionKey(processKey)
            .latestVersion()
            .singleResult();
    }

    public ProcessDefinition getProcessDefinitionForDeployment(String deploymentId)
    {
        return repoService.createProcessDefinitionQuery()
            .deploymentId(deploymentId)
            .singleResult();
    }

    public ProcessInstance getProcessInstance(String id)
    {
        return runtimeService.createProcessInstanceQuery()
            .processInstanceId(id)
            .singleResult();
    }

    public Task getTaskInstance(String taskId)
    {
        TaskQuery taskQuery = taskService.createTaskQuery().taskId(taskId);
        if(!deployWorkflowsInTenant) {
        	taskQuery.processVariableValueEquals(ActivitiConstants.VAR_TENANT_DOMAIN, TenantUtil.getCurrentDomain());
        }
        return taskQuery.singleResult();
    }

    public HistoricProcessInstance getHistoricProcessInstance(String id)
    {
        return historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(id)
                    .singleResult();
    }

    public Execution getExecution(String id)
    {
        return runtimeService.createExecutionQuery()
            .executionId(id)
            .singleResult();
    }

    public ProcessDefinition getDeployedProcessDefinition(String processDefinitionId){
        return ProcessDefinitionUtil.getProcessDefinitionFromDatabase(processDefinitionId);
    }

    public String getStartFormKey(String processDefinitionId)
    {
        ProcessDefinitionEntityImpl procDef = (ProcessDefinitionEntityImpl) ProcessDefinitionUtil.getProcessDefinitionFromDatabase(processDefinitionId);
        return procDef.getKey();
    }
    
    public String getStartTaskTypeName(String processDefinitionId)
    {
        String formKey = null;
        ProcessDefinition processDefinition = repoService.getProcessDefinition(processDefinitionId);
        Process process = repoService.getBpmnModel(processDefinition.getId())
                    .getProcessById(processDefinition.getKey());
        FlowElement startElement = process.getInitialFlowElement();
        if (startElement instanceof StartEvent) {
            StartEvent startEvent = (StartEvent) startElement;
            formKey = startEvent.getFormKey();
        }
        return formKey;
    }

    public BpmnModel bpmnModel(String processDefinitionId)
    {
        return repoService.getBpmnModel(processDefinitionId);
    }

    public Map<String, Object> getExecutionVariables(String executionId)
    {
        return runtimeService.getVariables(executionId);
    }
    
    /**
     * @return the historyService
     */
    public HistoryService getHistoryService()
    {
        return historyService;
    }
    
    /**
     * @return the repoService
     */
    public RepositoryService getRepositoryService()
    {
        return repoService;
    }
    
    /**
     * @return the runtimeService
     */
    public RuntimeService getRuntimeService()
    {
        return runtimeService;
    }
    
    /**
     * @return the taskService
     */
    public TaskService getTaskService()
    {
        return taskService;
    }

    /**
     * @return ManagementService
     */
    public ManagementService getManagementService()
    {
        return managementService;
    }

    /**
     * @param localId String
     * @return HistoricTaskInstance
     */
    public HistoricTaskInstance getHistoricTaskInstance(String localId)
    {
        HistoricTaskInstanceQuery taskQuery =  historyService.createHistoricTaskInstanceQuery()
            .taskId(localId);
        if(!deployWorkflowsInTenant) {
        	taskQuery.processVariableValueEquals(ActivitiConstants.VAR_TENANT_DOMAIN, TenantUtil.getCurrentDomain());
        }
        return taskQuery.singleResult();
    }
    
    public boolean isMultiTenantWorkflowDeploymentEnabled() 
    {
		return deployWorkflowsInTenant;
	}
    
    public boolean isRetentionHistoricProcessInstanceEnabled()
    {
        return retentionHistoricProcessInstance;
    }
}