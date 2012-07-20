
create table SYS_SERVER (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    ADDRESS varchar(255),
    IS_RUNNING boolean,
    --
    primary key (ID)
)^

------------------------------------------------------------------------------------------------------------

create table SYS_CONFIG (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    --
    NAME varchar(255),
    VALUE varchar(1500),
    --
    primary key (ID)
)^

create unique index IDX_SYS_CONFIG_UNIQ_NAME on SYS_CONFIG (NAME)^

------------------------------------------------------------------------------------------------------------

create table SYS_FILE (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(500),
    EXT varchar(20),
    SIZE integer,
    CREATE_DATE timestamp,
    --
    primary key (ID)
)^

------------------------------------------------------------------------------------------------------------

create table SYS_LOCK_CONFIG (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    --
    NAME varchar(100),
    TIMEOUT_SEC integer,
    --
    primary key (ID)
)^

------------------------------------------------------------------------------------------------------------

create table SYS_ENTITY_STATISTICS (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    --
    NAME varchar(50),
    INSTANCE_COUNT bigint,
    FETCH_UI integer,
    MAX_FETCH_UI integer,
    LAZY_COLLECTION_THRESHOLD integer,
    LOOKUP_SCREEN_THRESHOLD integer,
    --
    primary key (ID)
)^

create unique index IDX_SYS_ENTITY_STATISTICS_UNIQ_NAME on SYS_ENTITY_STATISTICS (NAME)^

------------------------------------------------------------------------------------------------------------

create table SYS_SCHEDULED_TASK (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    BEAN_NAME varchar(50),
    METHOD_NAME varchar(50),
    USER_NAME varchar(50),
    USER_PASSWORD varchar(50),
    IS_SINGLETON boolean,
    IS_ACTIVE boolean,
    PERIOD integer,
    TIMEOUT integer,
    START_DATE timestamp,
    TIME_FRAME integer,
    START_DELAY integer,
    PERMITTED_SERVERS varchar(500),
    LOG_START boolean,
    LOG_FINISH boolean,
    LAST_START_TIME timestamp with time zone,
    LAST_START_SERVER varchar(50),
    --
    primary key (ID)
)^

create unique index IDX_SYS_SCHEDULED_TASK_UNIQ_BEAN_METHOD on SYS_SCHEDULED_TASK (BEAN_NAME, METHOD_NAME) where DELETE_TS is null^

------------------------------------------------------------------------------------------------------------

create table SYS_SCHEDULED_EXECUTION (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    --
    TASK_ID uuid,
    SERVER varchar(50),
    START_TIME timestamp with time zone,
    FINISH_TIME timestamp with time zone,
    RESULT text,
    --
    primary key (ID),
    constraint SYS_SCHEDULED_EXECUTION_TASK foreign key (TASK_ID) references SYS_SCHEDULED_TASK(ID)
)^

create index IDX_SYS_SCHEDULED_EXECUTION_TASK_START_TIME  on SYS_SCHEDULED_EXECUTION (TASK_ID, START_TIME)^

------------------------------------------------------------------------------------------------------------

create table SEC_ROLE (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    LOC_NAME varchar(255),
    DESCRIPTION varchar(1000),
    IS_DEFAULT_ROLE boolean, 
    TYPE integer,
    --
    primary key (ID)
)^

create unique index IDX_SEC_ROLE_UNIQ_NAME on SEC_ROLE (NAME) where DELETE_TS is null^

------------------------------------------------------------------------------------------------------------

create table SEC_GROUP (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    PARENT_ID uuid,
    --
    primary key (ID),
    constraint SEC_GROUP_PARENT foreign key (PARENT_ID) references SEC_GROUP(ID)
)^

------------------------------------------------------------------------------------------------------------

create table SEC_GROUP_HIERARCHY (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    --
    GROUP_ID uuid,
    PARENT_ID uuid,
    LEVEL integer,
    --
    primary key (ID),
    constraint SEC_GROUP_HIERARCHY_GROUP foreign key (GROUP_ID) references SEC_GROUP(ID),
    constraint SEC_GROUP_HIERARCHY_PARENT foreign key (PARENT_ID) references SEC_GROUP(ID)
)^

------------------------------------------------------------------------------------------------------------

create table SEC_USER (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    LOGIN varchar(50),
    LOGIN_LC varchar(50),
    PASSWORD varchar(32),
    NAME varchar(255),
    FIRST_NAME varchar(255),
    LAST_NAME varchar(255),
    MIDDLE_NAME varchar(255),
    POSITION_ varchar(255),
    EMAIL varchar(100),
    LANGUAGE_ varchar(20),
    ACTIVE boolean,
    GROUP_ID uuid,
    DEFAULT_SUBSTITUTED_USER_ID uuid,
    IP_MASK varchar(200),
    TYPE varchar(1),
    --
    primary key (ID),
    constraint SEC_USER_GROUP foreign key (GROUP_ID) references SEC_GROUP(ID),
    constraint SEC_USER_DEFAULT_SUBSTITUTED_USER foreign key (DEFAULT_SUBSTITUTED_USER_ID) references SEC_USER(ID)
)^

create unique index IDX_SEC_USER_UNIQ_LOGIN on SEC_USER (LOGIN_LC) where DELETE_TS is null^

------------------------------------------------------------------------------------------------------------

create table SEC_USER_ROLE (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    USER_ID uuid,
    ROLE_ID uuid,
    --
    primary key (ID),
    constraint SEC_USER_ROLE_PROFILE foreign key (USER_ID) references SEC_USER(ID),
    constraint SEC_USER_ROLE_ROLE foreign key (ROLE_ID) references SEC_ROLE(ID)
)^

create unique index IDX_SEC_USER_ROLE_UNIQ_ROLE on SEC_USER_ROLE (USER_ID, ROLE_ID) where DELETE_TS is null^

------------------------------------------------------------------------------------------------------------

create table SEC_PERMISSION (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    TYPE integer,
    TARGET varchar(100),
    VALUE integer,
    ROLE_ID uuid,
    --
    primary key (ID),
    constraint SEC_PERMISSION_ROLE foreign key (ROLE_ID) references SEC_ROLE(ID)
)^

create unique index IDX_SEC_PERMISSION_UNIQUE on SEC_PERMISSION (ROLE_ID, TYPE, TARGET) where DELETE_TS is null^

------------------------------------------------------------------------------------------------------------

create table SEC_CONSTRAINT (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ENTITY_NAME varchar(50),
    JOIN_CLAUSE varchar(500),
    WHERE_CLAUSE varchar(1000),
    GROUP_ID uuid,
    --
    primary key (ID),
    constraint SEC_CONSTRAINT_GROUP foreign key (GROUP_ID) references SEC_GROUP(ID)
)^

create index IDX_SEC_CONSTRAINT_GROUP on SEC_CONSTRAINT (GROUP_ID)^

------------------------------------------------------------------------------------------------------------

create table SEC_SESSION_ATTR (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(50),
    STR_VALUE varchar(1000),
    DATATYPE varchar(20),
    GROUP_ID uuid,
    --
    primary key (ID),
    constraint SEC_SESSION_ATTR_GROUP foreign key (GROUP_ID) references SEC_GROUP(ID)
)^

create index IDX_SEC_SESSION_ATTR_GROUP on SEC_SESSION_ATTR (GROUP_ID)^

------------------------------------------------------------------------------------------------------------

create table SEC_USER_SETTING (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    --
    USER_ID uuid,
    CLIENT_TYPE char(1),
    NAME varchar(255),
    VALUE text,
    --
    primary key (ID),
    constraint SEC_USER_SETTING_USER foreign key (USER_ID) references SEC_USER(ID),
    constraint SEC_USER_SETTING_UNIQ unique (USER_ID, NAME, CLIENT_TYPE)
)^

create index IDX_SEC_USER_SETTING_USER_NAME_CLIENT on SEC_USER_SETTING (USER_ID, NAME, CLIENT_TYPE)^

------------------------------------------------------------------------------------------------------------

create table SEC_USER_SUBSTITUTION (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    USER_ID uuid,
    SUBSTITUTED_USER_ID uuid,
    START_DATE timestamp,
    END_DATE timestamp,
    --
    primary key (ID),
    constraint FK_SEC_USER_SUBSTITUTION_USER foreign key (USER_ID) references SEC_USER(ID),
    constraint FK_SEC_USER_SUBSTITUTION_SUBSTITUTED_USER foreign key (SUBSTITUTED_USER_ID) references SEC_USER(ID)
)^

create index IDX_SEC_USER_SUBSTITUTION_USER on SEC_USER_SUBSTITUTION (USER_ID)^

------------------------------------------------------------------------------------------------------------

create table SEC_LOGGED_ENTITY (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    --
    NAME varchar(100),
    AUTO boolean,
    MANUAL boolean,
    --
    primary key (ID),
    constraint SEC_LOGGED_ENTITY_UNIQ_NAME unique (NAME)
)^

------------------------------------------------------------------------------------------------------------

create table SEC_LOGGED_ATTR (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    --
    ENTITY_ID uuid,
    NAME varchar(50),
    --
    primary key (ID),
    constraint FK_SEC_LOGGED_ATTR_ENTITY foreign key (ENTITY_ID) references SEC_LOGGED_ENTITY(ID),
    constraint SEC_LOGGED_ATTR_UNIQ_NAME unique (ENTITY_ID, NAME)
)^

create index IDX_SEC_LOGGED_ATTR_ENTITY on SEC_LOGGED_ATTR (ENTITY_ID)^

------------------------------------------------------------------------------------------------------------

create table SEC_ENTITY_LOG (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    --
    EVENT_TS timestamp,
    USER_ID uuid,
    TYPE char(1),
    ENTITY varchar(100),
    ENTITY_ID uuid,
    CHANGES text,
    --
    primary key (ID),
    constraint FK_SEC_ENTITY_LOG_USER foreign key (USER_ID) references SEC_USER(ID)
)^

create index IDX_SEC_ENTITY_LOG_ENTITY_ID on SEC_ENTITY_LOG (ENTITY_ID)^

------------------------------------------------------------------------------------------------------------

create table SEC_ENTITY_LOG_ATTR (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    --
    ITEM_ID uuid,
    NAME varchar(50),
    VALUE varchar(1500),
    VALUE_ID uuid,    
    MESSAGES_PACK varchar(200),
    --
    primary key (ID),
    constraint FK_SEC_ENTITY_LOG_ATTR_ITEM foreign key (ITEM_ID) references SEC_ENTITY_LOG(ID)
)^

create index IDX_SEC_ENTITY_LOG_ATTR_ITEM on SEC_ENTITY_LOG_ATTR (ITEM_ID)^

------------------------------------------------------------------------------------------------------------

create table SEC_FILTER (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    COMPONENT varchar(200),
    NAME varchar(255),
    CODE varchar(200),
    XML varchar(7000),
    USER_ID uuid,
    --
    primary key (ID),
    constraint FK_SEC_FILTER_USER foreign key (USER_ID) references SEC_USER(ID)
)^

create index IDX_SEC_FILTER_COMPONENT_USER on SEC_FILTER (COMPONENT, USER_ID)^

------------------------------------------------------------------------------------------------------------

create table SYS_FOLDER (
    ID uuid,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    TYPE char(1),
    PARENT_ID uuid,
    NAME varchar(100),
    TAB_NAME varchar(100),
    SORT_ORDER integer,
    --
    primary key (ID),
    constraint FK_SYS_FOLDER_PARENT foreign key (PARENT_ID) references SYS_FOLDER(ID)
)^

------------------------------------------------------------------------------------------------------------

create table SYS_APP_FOLDER (
    FOLDER_ID uuid,
    FILTER_COMPONENT varchar(200),
    FILTER_XML varchar(7000),
    VISIBILITY_SCRIPT text,
    QUANTITY_SCRIPT text,
    APPLY_DEFAULT boolean,
    --
    primary key (FOLDER_ID),
    constraint FK_SYS_APP_FOLDER_FOLDER foreign key (FOLDER_ID) references SYS_FOLDER(ID)
)^

------------------------------------------------------------------------------------------------------------

create table SEC_PRESENTATION (
    ID uuid,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    --
    COMPONENT varchar(200),
    NAME varchar(255),
    XML varchar(7000),
    USER_ID uuid,
    IS_AUTO_SAVE boolean,
    --
    primary key (ID),
    constraint SEC_PRESENTATION_USER foreign key (USER_ID) references SEC_USER(ID)
)^

create index IDX_SEC_PRESENTATION_COMPONENT_USER on SEC_PRESENTATION (COMPONENT, USER_ID)^

------------------------------------------------------------------------------------------------------------

create table SEC_SEARCH_FOLDER (
    FOLDER_ID uuid,
    FILTER_COMPONENT varchar(200),
    FILTER_XML varchar(7000),
    USER_ID uuid,
    PRESENTATION_ID uuid,
    APPLY_DEFAULT boolean,
    IS_SET boolean,
    ENTITY_TYPE varchar(50),
    --
    primary key (FOLDER_ID),
    constraint FK_SEC_SEARCH_FOLDER_FOLDER foreign key (FOLDER_ID) references SYS_FOLDER(ID),
    constraint FK_SEC_SEARCH_FOLDER_USER foreign key (USER_ID) references SEC_USER(ID),
    constraint FK_SEC_SEARCH_FOLDER_PRESENTATION foreign key (PRESENTATION_ID)
        references SEC_PRESENTATION(ID)
        on delete set null
)^

create index IDX_SEC_SEARCH_FOLDER_USER on SEC_SEARCH_FOLDER (USER_ID)^

------------------------------------------------------------------------------------------------------------

create table SYS_FTS_QUEUE (
    ID uuid,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    ENTITY_ID uuid,
    ENTITY_NAME varchar(200),
    CHANGE_TYPE char(1),
    SOURCE_HOST varchar(100),
    primary key (ID)
)^

create index IDX_SYS_FTS_QUEUE_CREATE_TS on SYS_FTS_QUEUE (CREATE_TS)^

------------------------------------------------------------------------------------------------------------

create table SEC_SCREEN_HISTORY (
	ID uuid,
	CREATE_TS timestamp,
	CREATED_BY varchar(50),
	USER_ID uuid,
	CAPTION varchar(255),
	URL TEXT,
	ENTITY_ID uuid,
    --
	primary key (ID),
    constraint FK_SEC_HISTORY_USER foreign key (USER_ID) references SEC_USER (ID)
)^

create index IDX_SEC_SCREEN_HISTORY_USER on SEC_SCREEN_HISTORY (USER_ID)^

------------------------------------------------------------------------------------------------------------

create table SYS_SENDING_MESSAGE (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    ADDRESS_TO varchar(500),
    ADDRESS_FROM varchar(100),
    CAPTION varchar(500),
	CONTENT_TEXT text,
	DEADLINE timestamp,
	STATUS int,
	DATE_SENT timestamp,
	ATTEMPTS_COUNT int,
	ATTEMPTS_MADE int,
	ATTACHMENTS_NAME varchar(500),
    primary key (ID)
)^

create index IDX_SYS_SENDING_MESSAGE_STATUS on SYS_SENDING_MESSAGE (STATUS)^

create index IDX_SYS_SENDING_MESSAGE_DATE_SENT on SYS_SENDING_MESSAGE (DATE_SENT)^

------------------------------------------------------------------------------------------------------------

create table SYS_SENDING_ATTACHMENT(
	ID uuid,
	CREATE_TS timestamp,
	CREATED_BY varchar(50),
	--
	MESSAGE_ID uuid,
	CONTENT bytea,
	CONTENT_ID varchar(50),
	NAME varchar(500),
	--
	primary key (ID),
	constraint FK_SYS_SENDING_ATTACHMENT_SENDING_MESSAGE foreign key (MESSAGE_ID) references SYS_SENDING_MESSAGE (ID)
)^

create index SYS_SENDING_ATTACHMENT_MESSAGE_IDX on SYS_SENDING_ATTACHMENT (MESSAGE_ID)^

------------------------------------------------------------------------------------------------------------

create table SYS_ENTITY_SNAPSHOT (
    ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    ENTITY_META_CLASS varchar(50) not null,
    ENTITY_ID uuid not null,
    AUTHOR_ID uuid not null,
    VIEW_XML text not null,
    SNAPSHOT_XML text not null,
    SNAPSHOT_DATE timestamp not null,

	primary key (ID),
    constraint FK_SYS_ENTITY_SNAPSHOT_AUTHOR_ID foreign key (AUTHOR_ID) references SEC_USER(ID)
)^

create index IDX_SYS_ENTITY_SNAPSHOT_ENTITY_ID on SYS_ENTITY_SNAPSHOT (ENTITY_ID)^

-------------------------------------------------------------------------------------------------------------

create table SYS_CATEGORY (
	ID uuid not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
	SPECIAL varchar(50),
	ENTITY_TYPE varchar(30),
	IS_DEFAULT boolean,
	DISCRIMINATOR integer,
	--
	primary key (ID)
)^

-------------------------------------------------------------------------------------------------------------

create table SYS_CATEGORY_ATTR (
	ID uuid not null,
	CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
	CATEGORY_ID uuid,
	IS_ENTITY boolean,
	DATA_TYPE varchar(200),
	DEFAULT_STRING varchar,
	DEFAULT_INT integer,
	DEFAULT_DOUBLE numeric,
	DEFAULT_DATE timestamp,
	DEFAULT_DATE_IS_CURRENT boolean,
	DEFAULT_BOOLEAN boolean,
	DEFAULT_ENTITY_VALUE uuid,
	ENUMERATION varchar(500),
	ORDER_NO integer,
	SCREEN varchar(255),
	REQUIRED boolean,
	LOOKUP boolean,
	--
	primary key (ID),
	constraint SYS_CATEGORY_ATTR_CATEGORY_ID foreign key (CATEGORY_ID) references SYS_CATEGORY(ID)
)^

create index IDX_SYS_CATEGORY_ATTR_CATEGORY on SYS_CATEGORY_ATTR (CATEGORY_ID)^

-------------------------------------------------------------------------------------------------------------

create table SYS_ATTR_VALUE(
	ID uuid not null,
	CREATE_TS timestamp,
    CREATED_BY varchar(50),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CATEGORY_ATTR_ID uuid,
	ENTITY_ID uuid,
	STRING_VALUE varchar,
	INTEGER_VALUE integer,
	DOUBLE_VALUE numeric,
	DATE_VALUE timestamp,
	BOOLEAN_VALUE boolean,
	ENTITY_VALUE uuid,
	--
	primary key (ID),
	constraint SYS_ATTR_VALUE_CATEGORY_ATTR_ID foreign key (CATEGORY_ATTR_ID) references SYS_CATEGORY_ATTR(ID)
)^

create index IDX_SYS_ATTR_VALUE_ENTITY on SYS_ATTR_VALUE (ENTITY_ID)^

------------------------------------------------------------------------------------------------------------

create sequence SYS_QUERY_RESULT_SEQ
^

create table SYS_QUERY_RESULT (
    ID bigint not null default nextval('SYS_QUERY_RESULT_SEQ'),
	SESSION_ID uuid not null,
	QUERY_KEY integer not null,
	ENTITY_ID uuid,
	primary key (ID)
)^

create index IDX_SYS_QUERY_RESULT_ENTITY_SESSION_KEY on SYS_QUERY_RESULT (ENTITY_ID, SESSION_ID, QUERY_KEY)^

create index IDX_SYS_QUERY_RESULT_SESSION_KEY on SYS_QUERY_RESULT (SESSION_ID, QUERY_KEY)^

--------------------------------------------------------------------------------------------------------------
create or replace function newid()
returns uuid
as '$libdir/uuid-ossp', 'uuid_generate_v1'
volatile strict language c^

--------------------------------------------------------------------------------------------------------------

insert into SEC_GROUP (ID, CREATE_TS, VERSION, NAME, PARENT_ID)
values ('0fa2b1a5-1d68-4d69-9fbd-dff348347f93', now(), 0, 'Company', null)^

insert into SEC_USER (ID, CREATE_TS, VERSION, LOGIN, LOGIN_LC, PASSWORD, NAME, GROUP_ID, ACTIVE, TYPE)
values ('60885987-1b61-4247-94c7-dff348347f93', now(), 0, 'admin', 'admin', '21232f297a57a5a743894a0e4a801fc3', 'Administrator', '0fa2b1a5-1d68-4d69-9fbd-dff348347f93', true, 'C')^

insert into SEC_USER (ID, CREATE_TS, VERSION, LOGIN, LOGIN_LC, PASSWORD, NAME, GROUP_ID, ACTIVE, TYPE)
values ('60885987-1b61-4247-94c7-dff348347f94', now(), 0, 'emailer', 'emailer', '2f22cf032e4be87de59e4e8bfd066ed1', 'User for Email sending', '0fa2b1a5-1d68-4d69-9fbd-dff348347f93', true, 'C')^

insert into SEC_USER (ID, CREATE_TS, VERSION, LOGIN, LOGIN_LC, PASSWORD, NAME, GROUP_ID, ACTIVE, TYPE)
values ('83075c20-fe23-11df-abc9-3f87313a5ebe', now(), 0, 'SchedulerUser', 'scheduleruser', '7e0ffe513f4e8c8f1376da12fe9c5561', 'SchedulerUser', '0fa2b1a5-1d68-4d69-9fbd-dff348347f93', true, 'C')^

insert into SEC_ROLE (ID, CREATE_TS, VERSION, NAME, TYPE)
values ('0c018061-b26f-4de2-a5be-dff348347f93', now(), 0, 'Administrators', 10)^

insert into SEC_FILTER (ID,CREATE_TS,CREATED_BY,VERSION,UPDATE_TS,UPDATED_BY,DELETE_TS,DELETED_BY,COMPONENT,NAME,XML,USER_ID) values
(
  'b61d18cb-e79a-46f3-b16d-eaf4aebb10dd',
  {ts '2010-03-01 11:14:06.830'},
  'admin',2,
  {ts '2010-03-01 11:52:53.170'},
  'admin',null,null,
  '[sec$User.browse].genericFilter',
  'Search by role',
  '<?xml version="1.0" encoding="UTF-8"?>\n<filter>\n  <and>\n    <c name="UrMxpkfMGn" class="com.haulmont.cuba.security.entity.Role" type="CUSTOM" locCaption="Role" entityAlias="u" join="join u.userRoles ur">ur.role.id = :component$genericFilter.UrMxpkfMGn32565\n      <param name="component$genericFilter.UrMxpkfMGn32565">NULL</param>\n    </c>\n  </and>\n</filter>\n',
  '60885987-1b61-4247-94c7-dff348347f93'
)^
