package org.ecommerce.user.utils;

import lombok.experimental.UtilityClass;

/**
 * Lombok @UtilityClass:
 * - Makes the class final
 * - Generates a private constructor
 * - Makes fields and methods static
 * <p>
 * Note: Fields are not automatically made final.
 */
@UtilityClass
public class RoleConstants {
    public final String USER = "USER";
    public final String ADMIN = "ADMIN";
}
