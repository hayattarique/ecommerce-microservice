package org.ecommerce.utility.config;

import java.util.List;

public interface PermissionProvider {
    List<String> getPermissionForUser(String username);
}
