package com.time_tracker.be.account.projection;

public interface AccountProjection {
    String getEmail();

    String getFullName();

    Integer getUserId();

    Role getRole();

    interface Role {
        String getRoleName();

        Integer getRoleId();
    }

    String getPhoto();
}
