package com.passfail.codingtest.util;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 현재 실행 환경의 OS 및 프로파일을 감지하는 유틸리티 클래스
 */
@Component
@RequiredArgsConstructor
public class JudgeEnvironmentProvider {

    private final Environment environment;
    private final String osName = System.getProperty("os.name").toLowerCase();

    public boolean isLinux() {
        return osName.contains("linux");
    }

    public boolean isWindows() {
        return osName.contains("win");
    }

    /**
     * 현재 'prod' 프로파일이 활성화되어 있는지 확인 (Docker 사용 여부와 동일)
     */
    public boolean isUsingDocker() {
        return environment.acceptsProfiles(Profiles.of("prod"));
    }

    public String getOsInfo() {
        return System.getProperty("os.name");
    }
}
