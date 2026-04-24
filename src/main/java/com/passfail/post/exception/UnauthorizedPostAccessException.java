package com.passfail.post.exception;

public class UnauthorizedPostAccessException extends RuntimeException{
	
	public UnauthorizedPostAccessException() {
		super("해당 게시글에 대한 권한이 없습니다.");
	}
	
}
