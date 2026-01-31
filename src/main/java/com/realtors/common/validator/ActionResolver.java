package com.realtors.common.validator;

public class ActionResolver {

    public static PermissionAction fromHttpMethod(String method) {
        return switch (method) {
            case "POST"   -> PermissionAction.CREATE;
            case "GET"    -> PermissionAction.READ;
            case "PUT",
                 "PATCH" -> PermissionAction.UPDATE;
            case "DELETE" -> PermissionAction.DELETE;
            default -> throw new IllegalStateException("Unsupported method");
        };
    }
}
