package com.realtors.dashboard.service.strategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.realtors.dashboard.dto.UserRole;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleStrategy {
    UserRole value();
}
