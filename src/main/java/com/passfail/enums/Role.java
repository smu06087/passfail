package com.passfail.enums;

/**
 * [Role Enum - ˈroʊl ɪˈnʌm]
 * 비유: "군대의 계급처럼, 사용자가 서비스에서 어느 정도의 권한을 가졌는지 정의하는 이름표예요."
 */
public enum Role {
    ROLE_USER("USER"),
    ROLE_ADMIN("ADMIN"),
    ROLE_SUPER_ADMIN("SUPER_ADMIN");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
