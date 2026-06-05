package com.passfail.codingtest.util;

import org.springframework.stereotype.Component;

/**
 * 현재 실행 환경의 OS를 감지하는 유틸리티 클래스
 */
@Component
public class JudgeEnvironmentProvider {

    private final String osName = System.getProperty("os.name").toLowerCase();

    public boolean isLinux() {
        return osName.contains("linux");
    }

    public boolean isWindows() {
        return osName.contains("win");
    }

    public String getOsInfo() {
        return System.getProperty("os.name");
    }
}
