/**
 * OnDocker 비동기 채점 및 SSE 연동을 위한 JavaScript
 * 사용자 피드백 개선 버전
 */
const EditorOnDocker = {
    eventSource: null,
    testCaseTimers: {}, // interval 저장용
    testCaseStartTime: {}, // 시작 시간 저장용

    subscribe(id) {
        if (this.eventSource) this.eventSource.close();
        this.clearTimers();
        
        console.log("[EditorOnDocker] Subscribing to SSE for ID:", id);
        this.eventSource = new EventSource(`/codingtest/subscribe/${id}`);
        
        this.eventSource.addEventListener('status', (e) => {
            const message = e.data;
            console.log("[EditorOnDocker] Received status:", message);
            
            // "specified_1 테스트 케이스 실행 중..." 형태의 메시지 파싱
            if (message.includes("테스트 케이스 실행 중")) {
                const match = message.match(/(specified|custom)_(\d+)/);
                if (match) {
                    const type = match[1];
                    const index = parseInt(match[2]);
                    this.startTestCaseLoading(type, index);
                }
            }
        });

        this.eventSource.addEventListener('complete', (e) => {
            console.log("[EditorOnDocker] Received complete. Raw data:", e.data);
            try {
                this.clearTimers();
                const finalData = JSON.parse(e.data);
                console.log("[EditorOnDocker] Parsed final data:", finalData);
                
                if (finalData.error) {
                    const errorMsg = this.getFriendlyErrorMessage(finalData.error);
                    this.updateUI(`<span style='color:red; font-weight:bold;'>${errorMsg}</span>`);
                    return;
                }

                this.renderResults(finalData.results);
                
                if (finalData.hasOwnProperty('allCorrect')) {
                    if (finalData.allCorrect) {
                        if (document.getElementById('aiReviewBtn')) document.getElementById('aiReviewBtn').style.display = 'block';
                        if (typeof EditorCore !== 'undefined') EditorCore.lastResults = finalData.results;
                        this.updateUI("<span style='color:green; font-weight:bold;'>정답입니다! (DB 기록 완료)</span>");

                        if (typeof EditorCore !== 'undefined' && EditorCore.config && EditorCore.config.onSubmitComplete) {
                            EditorCore.config.onSubmitComplete({ allCorrect: true, results: finalData.results });
                        }
                    } else {
                        this.updateUI("<span style='color:red; font-weight:bold;'>틀렸습니다. (DB 기록 완료)</span>");
                    }
                }
            } catch (err) { 
                console.error("[EditorOnDocker] Result parsing error", err);
                this.updateUI(`<span style='color:red;'>결과 처리 중 오류가 발생했습니다.</span>`);
            }
            this.eventSource.close();
        });

        this.eventSource.onerror = (e) => {
            console.error("[EditorOnDocker] SSE error:", e);
            this.clearTimers();
            this.eventSource.close();
        };
    },

    getFriendlyErrorMessage(error) {
        switch(error) {
            case 'COMPILE_ERROR': return '컴파일 에러가 발생했습니다. 코드를 확인해주세요.';
            case 'SYSTEM_ERROR': return '시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
            case 'TIMEOUT': return '시간 초과가 발생했습니다.';
            case 'MEMORY_LIMIT_EXCEEDED': return '메모리 제한을 초과했습니다.';
            default: return `실행 중 오류가 발생했습니다: ${error}`;
        }
    },

    startTestCaseLoading(type, index) {
        const display = document.getElementById('result-display');
        if (!display) return;

        const officialTCCount = parseInt(document.getElementById('problem-data')?.getAttribute('data-tc-count')) || 0;
        const isCustom = type === 'custom';
        const displayIndex = isCustom ? index : index;
        const title = isCustom ? `커스텀 테스트 ${displayIndex}` : `기본 테스트 ${displayIndex}`;
        const timerId = `${type}_${index}`;

        // 이미 로딩 중인 행이 있는지 확인
        let row = document.getElementById(`loading-${timerId}`);
        if (!row) {
            row = document.createElement('div');
            row.id = `loading-${timerId}`;
            row.className = 'test-case-row';
            display.appendChild(row);
        }

        this.testCaseStartTime[timerId] = performance.now();
        
        const updateRow = () => {
            const elapsed = ((performance.now() - this.testCaseStartTime[timerId])).toFixed(2);
            row.innerHTML = `
                <div style="font-weight:600;">${title}: <span style="color: #3b82f6;">실행 중...</span></div>
                <div style="font-size:11px;color:gray;">시간: ${elapsed}ms</div>
                <div style="visibility: hidden;">출력: </div>
            `;
            display.scrollTop = display.scrollHeight;
        };

        if (this.testCaseTimers[timerId]) clearInterval(this.testCaseTimers[timerId]);
        this.testCaseTimers[timerId] = setInterval(updateRow, 50);
        updateRow();
    },

    clearTimers() {
        Object.values(this.testCaseTimers).forEach(clearInterval);
        this.testCaseTimers = {};
        this.testCaseStartTime = {};
    },

    updateUI(message) {
        const outputEl = document.getElementById('result-display') || document.getElementById('execution-output') || document.getElementById('output');
        if (outputEl) {
            const timeStr = new Date().toLocaleTimeString();
            const div = document.createElement('div');
            div.style.marginTop = "10px";
            div.style.padding = "5px";
            if (message.includes('<span')) div.innerHTML = `[${timeStr}] ${message}`;
            else div.textContent = `[${timeStr}] ${message}`;
            outputEl.appendChild(div);
            outputEl.scrollTop = outputEl.scrollHeight;
        }
    },

    renderResults(results) {
        const display = document.getElementById('result-display');
        if (!display) return;

        // 로딩 중이던 행들 제거
        display.querySelectorAll('.test-case-row[id^="loading-"]').forEach(el => el.remove());

        if (typeof EditorCore !== 'undefined' && typeof EditorCore.displayResults === 'function') {
            EditorCore.displayResults(results);
        } else {
            if (!Array.isArray(results)) return;
            results.forEach((res, idx) => {
                const row = document.createElement('div');
                row.className = 'test-case-row';
                const statusColor = res.success ? '#28a745' : '#dc3545';
                row.innerHTML = `
                    <div style="font-weight:600;">테스트 ${idx + 1}: <span style="color:${statusColor};">${res.status}</span></div>
                    <div style="font-size:11px;color:gray;">시간: ${res.executionTime || 0}ms</div>
                    ${res.output ? `<div>출력: ${res.output}</div>` : '<div>출력: </div>'}
                    ${res.error ? `<pre style="color:red;white-space:pre-wrap;font-size:11px;margin-top:5px;">${res.error}</pre>` : ''}
                `;
                display.appendChild(row);
            });
        }
    },

    getCode() {
        if (typeof EditorCore !== 'undefined' && EditorCore.editor) return EditorCore.editor.getValue();
        return "";
    },

    getSelectedLanguage() {
        const select = document.getElementById('languageSelect');
        return select ? select.value : "java";
    },

    async changeLanguage(lang) {
        if (typeof EditorCore !== 'undefined') {
            const model = EditorCore.editor.getModel();
            monaco.editor.setModelLanguage(model, lang === 'cpp' ? 'cpp' : (lang === 'python' ? 'python' : 'java'));
            
            const isLogicMaze = (EditorCore.battleMode === 'LOGIC_MAZE');
            if (isLogicMaze) {
                try {
                    const res = await fetch(`/codingtest/template?lang=${lang}&mode=LOGIC_MAZE`);
                    const data = await res.json();
                    if (data.template) {
                        if (confirm('언어를 변경하면 작성 중인 코드가 초기화될 수 있습니다. 변경하시겠습니까?')) {
                            EditorCore.editor.setValue(data.template);
                        }
                        return;
                    }
                } catch (e) { console.error("Template load failed", e); }
            }
            
            const templates = {
                java: 'import java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 코드를 작성하세요\n    }\n}',
                python: '# 코드를 작성하세요\nimport sys\n\ndef solve():\n    pass\n\nif __name__ == "__main__":\n    solve()',
                cpp: '#include <iostream>\n\nusing namespace std;\n\nint main() {\n    // 코드를 작성하세요\n    return 0;\n}'
            };
            if (confirm('언어를 변경하면 작성 중인 코드가 초기화될 수 있습니다. 변경하시겠습니까?')) {
                EditorCore.editor.setValue(templates[lang] || "");
            }
        }
    },

    async runCodeOnDocker() {
        const dataEl = document.getElementById('problem-data');
        const problemId = dataEl.getAttribute('data-id');
        const code = this.getCode();
        const language = this.getSelectedLanguage();

        if (typeof EditorCore !== 'undefined') EditorCore.showTab('result');
        const display = document.getElementById('result-display');
        display.innerHTML = '<div style="margin-bottom:10px; font-weight:bold; color:#3b82f6;">실행 요청 중 ...</div>';

        // 커스텀 테스트 케이스 수집
        const customTC = [];
        document.querySelectorAll('.custom-case-row').forEach(row => {
            const inputVal = row.querySelector('.tc-in').value;
            const expectedVal = row.querySelector('.tc-ex').value;
            customTC.push({ input: inputVal, expected: expectedVal });
        });

        try {
            const response = await fetch(`/codingtest/${problemId}/run`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: code, customTestCases: customTC, language: language })
            });
            const result = await response.json();
            if (Array.isArray(result)) { 
                this.renderResults(result); 
            } else if (result.id) { 
                console.log("Async run started ID:", result.id);
                this.subscribe(result.id); 
            }
        } catch (e) { 
            this.updateUI(`<span style="color:red;">통신 오류: ${e.message}</span>`); 
        }
    },

    async submitCodeOnDocker() {
        const dataEl = document.getElementById('problem-data');
        const problemId = dataEl.getAttribute('data-id');
        const code = this.getCode();
        const language = this.getSelectedLanguage();
        
        if (typeof EditorCore !== 'undefined') EditorCore.showTab('result');
        const display = document.getElementById('result-display');
        display.innerHTML = '<div style="margin-bottom:10px; font-weight:bold; color:#3b82f6;">제출 요청 중 ...</div>';

        try {
            const response = await fetch(`/codingtest/${problemId}/submit`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: code, language: language })
            });
            const result = await response.json();
            if (result.results) {
                this.renderResults(result.results);
            } else if (result.id) { 
                console.log("Async submit started ID:", result.id);
                this.subscribe(result.id); 
            }
        } catch (e) { 
            this.updateUI(`<span style="color:red;">통신 오류: ${e.message}</span>`); 
        }
    },

    init() {
        console.log("Initializing EditorOnDocker (User Feedback Optimized)...");
        if (typeof EditorCore !== 'undefined') {
            EditorCore.runCode = () => this.runCodeOnDocker();
            EditorCore.submitCode = () => this.submitCodeOnDocker();
            EditorCore.changeLanguage = (lang) => this.changeLanguage(lang);
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    setTimeout(() => EditorOnDocker.init(), 600);
});
