package org.baanet.baaapp.api;

public final class ApiEndpoint {

    private ApiEndpoint() {}

    public static final String AUTH_ME = "/auth/me";
    public static final String LOGIN = "/auth/login";
    public static final String REGISTER = "/auth/register";
    public static final String CHANGE_PASSWORD = "/auth/password/change";
    public static final String LOCATIONS_SYNC = "/locations/sync";

    public static String locationPhoto(long serverLocationId) {
        return "/locations/" + serverLocationId + "/photo";
    }

}
