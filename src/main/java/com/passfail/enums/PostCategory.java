package com.passfail.enums;

@lombok.Getter
@lombok.RequiredArgsConstructor
public enum PostCategory {
	FREE("자유"), 
	QNA("Q&A"), 
	REVIEW("후기"), 
	NOTICE("공지");
	
	private final String label;
}
