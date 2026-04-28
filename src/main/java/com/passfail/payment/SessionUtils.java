package com.passfail.payment;

import java.util.Objects;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * [세션 도구함] SessionUtils (세션 유틸즈)
 * 비유: 손님의 개인 사물함을 관리하는 로봇 팔이에요.
 */
public class SessionUtils {

    /**
     * 1. 사물함에 물건 넣기 (addAttribute / 애드 어트리뷰트)
     * 비유: "이 물건(value)을 '결제번호'라는 이름(name)으로 사물함에 넣어줘!"
     */
    public static void addAttribute(String name, Object value) {
        // 🔁 [반복/과정] 1. 지금 접속한 손님이 누군지 확인하고 -> 2. 사물함을 열고 -> 3. 이름표를 붙여서 저장해요.
        Objects.requireNonNull(RequestContextHolder.getRequestAttributes())
               .setAttribute(name, value, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * 2. 사물함에서 글자 꺼내기 (getStringAttributeValue / 겟 스트링 어트리뷰트 밸류)
     * 비유: "사물함에서 물건을 꺼낸 다음, 읽기 편하게 '글자(String)'로 바꿔서 가져와!"
     */
    public static String getStringAttributeValue(String name) {
        // 🔁 [과정] 아래 3번 로봇을 시켜서 물건을 가져온 뒤, String.valueOf()로 글자로 변신시켜요.
        return String.valueOf(getAttribute(name));
    }

    /**
     * 3. 사물함에서 물건 찾기 (getAttribute / 겟 어트리뷰트)
     * 비유: "사물함에 가서 '결제번호'라고 적힌 물건 좀 다시 가져와 줄래?"
     */
    public static Object getAttribute(String name) {
        // 🔁 [과정] 손님의 사물함(SCOPE_SESSION)을 뒤져서 이름(name)이 일치하는 물건을 쏙 꺼내와요.
        return Objects.requireNonNull(RequestContextHolder.getRequestAttributes())
               .getAttribute(name, RequestAttributes.SCOPE_SESSION);
    }
}