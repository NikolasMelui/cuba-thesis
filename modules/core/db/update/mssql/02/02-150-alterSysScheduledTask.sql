-- $Id$

alter table SYS_SCHEDULED_TASK add METHOD_PARAMS varchar(1000);
drop index IDX_SYS_SCHEDULED_TASK_UNIQ_BEAN_METHOD on SYS_SCHEDULED_TASK;
create unique index IDX_SYS_SCHEDULED_TASK_UNIQ_BEAN_METHOD on SYS_SCHEDULED_TASK (BEAN_NAME, METHOD_NAME, METHOD_PARAMS, DELETE_TS);