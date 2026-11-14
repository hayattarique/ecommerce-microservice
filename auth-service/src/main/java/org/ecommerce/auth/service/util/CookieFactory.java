package org.ecommerce.auth.service.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.ecommerce.auth.service.config.CookiePropertiesConfig;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Component
public class CookieFactory {

    private static final String SECURE_PREFIX = "__Secure-";

    private final CookiePropertiesConfig propertiesConfig;
    private final JWTPropertiesConfig jwtPropertiesConfig;
    private final Duration maxAge;

    public CookieFactory(CookiePropertiesConfig propertiesConfig, JWTPropertiesConfig jwtPropertiesConfig) {
            /*
             Cookies prefixed with __Secure- require the Secure attribute; otherwise,
             browsers may reject or discard them, causing the authentication cookie to be lost on page refresh.
             */
        if (!propertiesConfig.secure() && propertiesConfig.name().startsWith(SECURE_PREFIX)) {
            throw new IllegalStateException("auth.refresh-cookie.name starts with " + SECURE_PREFIX
                    + " but auth.refresh-cookie.secure=false; browsers would reject the cookie");
        }
        // SameSite=None cookies must have Secure enabled; otherwise, browsers will reject them,
        // preventing the cookie from being stored and sent with subsequent requests.
        if (!propertiesConfig.secure() && "None".equalsIgnoreCase(propertiesConfig.sameSite())) {
            throw new IllegalStateException("auth.refresh-cookie.same-site=None requires " + "auth.refresh-cookie.secure=true; browsers would reject the cookie");
        }
        this.propertiesConfig = propertiesConfig;
        this.jwtPropertiesConfig = jwtPropertiesConfig;
        this.maxAge = Duration.ofMillis(jwtPropertiesConfig.getRefreshExpiration());
    }

    public ResponseCookie build(String refreshToken) {
        return base(refreshToken).maxAge(maxAge).build();
    }

    public String readCookie(HttpServletRequest request){
        Cookie cookie = WebUtils.getCookie(request, propertiesConfig.name());
        return cookie != null ? cookie.getValue() : null;
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(propertiesConfig.name(), value)
                .httpOnly(true).secure(propertiesConfig.secure())
                .sameSite(propertiesConfig.sameSite())
                .path(propertiesConfig.path());
    }

}
