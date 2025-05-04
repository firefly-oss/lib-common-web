package com.catalis.common.web.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user is authenticated but not authorized to perform an action.
 * Results in a 403 FORBIDDEN response.
 */
public class AuthorizationException extends BusinessException {
    
    /**
     * The required permission that the user does not have.
     */
    private final String requiredPermission;
    
    /**
     * Creates a new AuthorizationException with the given message.
     *
     * @param message the error message
     */
    public AuthorizationException(String message) {
        this(message, null);
    }
    
    /**
     * Creates a new AuthorizationException with the given message and required permission.
     *
     * @param message the error message
     * @param requiredPermission the required permission that the user does not have
     */
    public AuthorizationException(String message, String requiredPermission) {
        super(HttpStatus.FORBIDDEN, "AUTHORIZATION_ERROR", message);
        this.requiredPermission = requiredPermission;
    }
    
    /**
     * Creates a new AuthorizationException with a code, message, and required permission.
     *
     * @param code the error code
     * @param message the error message
     * @param requiredPermission the required permission that the user does not have
     */
    public AuthorizationException(String code, String message, String requiredPermission) {
        super(HttpStatus.FORBIDDEN, code, message);
        this.requiredPermission = requiredPermission;
    }
    
    /**
     * Returns the required permission that the user does not have.
     *
     * @return the required permission, or null if not specified
     */
    public String getRequiredPermission() {
        return requiredPermission;
    }
    
    /**
     * Creates a new AuthorizationException for a missing permission.
     *
     * @param permission the permission that the user does not have
     * @return a new AuthorizationException
     */
    public static AuthorizationException missingPermission(String permission) {
        return new AuthorizationException(
                "MISSING_PERMISSION",
                String.format("You do not have the required permission: %s", permission),
                permission
        );
    }
    
    /**
     * Creates a new AuthorizationException for a missing role.
     *
     * @param role the role that the user does not have
     * @return a new AuthorizationException
     */
    public static AuthorizationException missingRole(String role) {
        return new AuthorizationException(
                "MISSING_ROLE",
                String.format("You do not have the required role: %s", role),
                "ROLE_" + role
        );
    }
    
    /**
     * Creates a new AuthorizationException for an action that is not allowed.
     *
     * @param action the action that is not allowed
     * @param resource the resource on which the action is not allowed
     * @return a new AuthorizationException
     */
    public static AuthorizationException actionNotAllowed(String action, String resource) {
        return new AuthorizationException(
                "ACTION_NOT_ALLOWED",
                String.format("You are not allowed to %s this %s", action, resource),
                action.toUpperCase() + "_" + resource.toUpperCase()
        );
    }
}
