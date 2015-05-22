/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractExecTO;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.PropagationMode;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.job.TaskJob;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.job.JobInstanceLoader;
import org.apache.syncope.core.logic.notification.NotificationJob;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class TaskLogic extends AbstractJobLogic<AbstractTaskTO> {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private TaskDataBinder binder;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private NotificationJob notificationJob;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private TaskUtilsFactory taskUtilsFactory;

    @PreAuthorize("hasRole('" + Entitlement.TASK_CREATE + "')")
    public <T extends SchedTaskTO> T createSchedTask(final T taskTO) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(taskTO);

        SchedTask task = binder.createSchedTask(taskTO, taskUtils);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(), task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtils);
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_UPDATE + "')")
    public SyncTaskTO updateSync(final SyncTaskTO taskTO) {
        return updateSched(taskTO);
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_UPDATE + "')")
    public <T extends SchedTaskTO> T updateSched(final SchedTaskTO taskTO) {
        SchedTask task = taskDAO.find(taskTO.getKey());
        if (task == null) {
            throw new NotFoundException("Task " + taskTO.getKey());
        }

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        binder.updateSchedTask(task, taskTO, taskUtils);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(), task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtils);
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_LIST + "')")
    public int count(final TaskType taskType) {
        return taskDAO.count(taskType);
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_LIST + "')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(final TaskType taskType,
            final int page, final int size, final List<OrderByClause> orderByClauses) {

        final TaskUtils taskUtilss = taskUtilsFactory.getInstance(taskType);

        return CollectionUtils.collect(taskDAO.findAll(page, size, orderByClauses, taskType),
                new Transformer<Task, T>() {

                    @Override
                    public T transform(final Task task) {
                        return (T) binder.getTaskTO(task, taskUtilss);
                    }
                }, new ArrayList<T>());
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_READ + "')")
    public <T extends AbstractTaskTO> T read(final Long taskId) {
        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        return binder.getTaskTO(task, taskUtilsFactory.getInstance(task));
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_READ + "')")
    public TaskExecTO readExecution(final Long executionId) {
        TaskExec taskExec = taskExecDAO.find(executionId);
        if (taskExec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }
        return binder.getTaskExecTO(taskExec);
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_EXECUTE + "')")
    public TaskExecTO execute(final Long taskId, final boolean dryRun) {
        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        TaskExecTO result = null;
        switch (taskUtils.getType()) {
            case PROPAGATION:
                final TaskExec propExec = taskExecutor.execute((PropagationTask) task);
                result = binder.getTaskExecTO(propExec);
                break;

            case NOTIFICATION:
                final TaskExec notExec = notificationJob.executeSingle((NotificationTask) task);
                result = binder.getTaskExecTO(notExec);
                break;

            case SCHEDULED:
            case SYNCHRONIZATION:
            case PUSH:
                try {
                    jobInstanceLoader.registerJob(task,
                            ((SchedTask) task).getJobClassName(),
                            ((SchedTask) task).getCronExpression());

                    JobDataMap map = new JobDataMap();
                    map.put(TaskJob.DRY_RUN_JOBDETAIL_KEY, dryRun);

                    scheduler.getScheduler().triggerJob(
                            new JobKey(JobNamer.getJobName(task), Scheduler.DEFAULT_GROUP), map);
                } catch (Exception e) {
                    LOG.error("While executing task {}", task, e);

                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
                    sce.getElements().add(e.getMessage());
                    throw sce;
                }

                result = new TaskExecTO();
                result.setTask(taskId);
                result.setStartDate(new Date());
                result.setStatus("JOB_FIRED");
                result.setMessage("Job fired; waiting for results...");
                break;

            default:
        }

        return result;
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_READ + "')")
    public TaskExecTO report(final Long executionId, final PropagationTaskExecStatus status, final String message) {
        TaskExec exec = taskExecDAO.find(executionId);
        if (exec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPropagationTaskExecReport);

        TaskUtils taskUtils = taskUtilsFactory.getInstance(exec.getTask());
        if (TaskType.PROPAGATION == taskUtils.getType()) {
            PropagationTask task = (PropagationTask) exec.getTask();
            if (task.getPropagationMode() != PropagationMode.TWO_PHASES) {
                sce.getElements().add("Propagation mode: " + task.getPropagationMode());
            }
        } else {
            sce.getElements().add("Task type: " + taskUtils);
        }

        switch (status) {
            case SUCCESS:
            case FAILURE:
                break;

            case CREATED:
            case SUBMITTED:
            case UNSUBMITTED:
                sce.getElements().add("Execution status to be set: " + status);
                break;

            default:
        }

        if (!sce.isEmpty()) {
            throw sce;
        }

        exec.setStatus(status.toString());
        exec.setMessage(message);
        return binder.getTaskExecTO(taskExecDAO.save(exec));
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_DELETE + "')")
    public <T extends AbstractTaskTO> T delete(final Long taskId) {
        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        T taskToDelete = binder.getTaskTO(task, taskUtils);

        if (TaskType.SCHEDULED == taskUtils.getType()
                || TaskType.SYNCHRONIZATION == taskUtils.getType()
                || TaskType.PUSH == taskUtils.getType()) {

            jobInstanceLoader.unregisterJob(task);
        }

        taskDAO.delete(task);
        return taskToDelete;
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_DELETE + "')")
    public TaskExecTO deleteExecution(final Long executionId) {
        TaskExec taskExec = taskExecDAO.find(executionId);
        if (taskExec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        TaskExecTO taskExecutionToDelete = binder.getTaskExecTO(taskExec);
        taskExecDAO.delete(taskExec);
        return taskExecutionToDelete;
    }

    @Override
    protected AbstractTaskTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        Long key = null;

        if (ArrayUtils.isNotEmpty(args)
                && !"deleteExecution".equals(method.getName()) && !"readExecution".equals(method.getName())) {

            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof AbstractTaskTO) {
                    key = ((AbstractTaskTO) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
            try {
                final Task task = taskDAO.find(key);
                return binder.getTaskTO(task, taskUtilsFactory.getInstance(task));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

    @Override
    @PreAuthorize("hasRole('" + Entitlement.TASK_LIST + "')")
    public <E extends AbstractExecTO> List<E> list(final JobStatusType type, final Class<E> reference) {
        return super.list(type, reference);
    }

    @PreAuthorize("hasRole('" + Entitlement.TASK_EXECUTE + "')")
    public void process(final JobAction action, final Long taskId) {
        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        String jobName = JobNamer.getJobName(task);
        process(action, jobName);
    }

    @Override
    protected Long getKeyFromJobName(final JobKey jobKey) {
        return JobNamer.getTaskKeyFromJobName(jobKey.getName());
    }
}
