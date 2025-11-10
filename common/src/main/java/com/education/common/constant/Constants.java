package com.education.common.constant;

public class Constants {
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";
    
    public static final String REDIS_TOKEN_PREFIX = "token:";
    public static final String REDIS_USER_PREFIX = "user:";
    public static final String REDIS_STUDENT_PREFIX = "student:";
    public static final String REDIS_TEACHER_PREFIX = "teacher:";
    public static final String REDIS_COURSE_PREFIX = "course:";
    
    public static final String QUEUE_SELECTION = "selection.queue";
    public static final String EXCHANGE_SELECTION = "selection.exchange";
    public static final String ROUTING_KEY_SELECTION = "selection.routing";
}

