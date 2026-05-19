package com.passfail.post.exception;

public class AdminOnlyException extends RuntimeException{
	
	public AdminOnlyException() {
        super("관리자만 접근할 수 있습니다.");
    }
	
}
