/**
 * 문제 관련 공통 유틸리티
 */
const ProblemUtils = {
    /**
     * 문제를 PDF로 다운로드합니다.
     * @param {number|string} problemId 문제 ID
     * @param {boolean} isLoggedIn 로그인 여부
     */
    async downloadPdf(problemId, isLoggedIn) {
        if (!problemId || problemId === "-") {
            return;
        }

        if (!isLoggedIn) {
            if (confirm('PDF 다운로드는 로그인이 필요한 서비스입니다. 로그인 하시겠습니까?')) {
                location.href = '/login';
            }
            return;
        }

        if (!confirm('문제를 PDF로 다운로드하시겠습니까? (1,000 바나나 소모)')) {
            return;
        }

        try {
            const response = await fetch(`/problem/${problemId}/download`);
            
            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `Problem_${problemId}.pdf`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            } else {
                const errorData = await response.json();
                alert(errorData.message || '포인트가 부족하거나 다운로드에 실패했습니다.');
            }
        } catch (error) {
            console.error('PDF download error:', error);
            alert('다운로드 중 오류가 발생했습니다.');
        }
    }
};
