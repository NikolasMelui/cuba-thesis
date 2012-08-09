/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.entity;

import javax.persistence.*;
import javax.persistence.Entity;
import java.util.Date;

/**
 * Entity that reflects the fact of a {@link ScheduledTask} execution.
 *
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
@Entity(name = "sys$ScheduledExecution")
@Table(name = "SYS_SCHEDULED_EXECUTION")
public class ScheduledExecution extends BaseUuidEntity {

    private static final long serialVersionUID = -3891325977986519747L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TASK_ID")
    protected ScheduledTask task;

    @Column(name = "SERVER")
    protected String server;

    @Column(name = "START_TIME")
    protected Date startTime;

    @Column(name = "FINISH_TIME")
    protected Date finishTime;

    @Column(name = "RESULT")
    protected String result;

    public ScheduledTask getTask() {
        return task;
    }

    public void setTask(ScheduledTask task) {
        this.task = task;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ScheduledExecution{" +
                "task=" + task +
                ", host='" + server + '\'' +
                ", startTime=" + startTime +
                (finishTime != null ? ", finishTime=" : "") +
                '}';
    }
}
