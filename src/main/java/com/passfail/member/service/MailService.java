package com.passfail.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송을 비동기적으로 처리하는 서비스 클래스
 * 실제 메일 발송은 네트워크 지연이 발생할 수 있으므로 @Async를 사용하여 별도 스레드에서 실행합니다.
 */
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 메일을 비동기적으로 발송합니다.
     * @param to 수신자 이메일
     * @param subject 제목
     * @param content 내용
     */
    @Async
    public void sendMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // 비동기 작업이므로 로그만 출력
            System.err.println("메일 발송 실패: " + e.getMessage());
        }
    }
}
