package org.ecommerce.utility.constants;

public interface UserMappingConstant {
    String BASE = "/api/v1/user";
    String LOGIN =  "/login";
    String REGISTER =  "/register";

    String GET_PERMISSIONS_BY_USERNAME_OR_EMAIL =  "/permissions/{email}";
    String GET_USER_BY_EMAIL =  "/{email}";

}
