package org.example.server.annotation;

import org.example.model.enums.UserRole;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated Command requires the user to have a specific role.
 * If not present, the command is accessible to any authenticated user (or unauthenticated if handled separately).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresRole {
    UserRole value();
}
