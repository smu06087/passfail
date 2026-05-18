import WSManager from '../util/connection/websocket-manager.js';
import { MazeEngine } from './logicMazeEngine.js';

export const LogicMazeController = {
    engine: null,
    roomId: null,
    memberId: null,

    init(roomId, memberId, seed, problemId, difficulty) {
        this.roomId = roomId;
        this.memberId = memberId;
        this.engine = new MazeEngine('maze-canvas', roomId, memberId, seed, difficulty);

        EditorCore.init({
            onEditorLoaded: (editor) => {
                this.engine.draw();
                this.bindEvents(editor, problemId);
            }
        });

        this.setupWebSocket();
    },

    bindEvents(editor, problemId) {
        const runBtn = document.getElementById('runBtn');
        const stopBtn = document.getElementById('stopBtn');
        const exitBtn = document.getElementById('exit-battle-btn');

        runBtn.onclick = () => {
            this.engine.resetState();
            this.engine.processCommands(() => {
                const finalCost = this.engine.currentCost;
                if (confirm(`목적지에 도착했습니다! \\n사용한 에너지 비용: ${finalCost}\\n이 코드로 제출하고 대결을 종료하시겠습니까?`)) {
                    this.submitScore(finalCost, editor.getValue());
                } else {
                    this.stopExecution();
                }
            });
            
            runBtn.style.display = 'none';
            stopBtn.style.display = 'inline-block';
            WSManager.sendMazeRun(this.roomId, this.memberId, editor.getValue(), this.engine.serializeMap());
        };

        stopBtn.onclick = () => this.stopExecution();

        if (exitBtn) {
            exitBtn.onclick = (e) => {
                const isFinished = exitBtn.innerText === "방 나가기";
                const msg = isFinished ? "방을 나가시겠습니까?" : "정말 포기하시겠습니까?";
                
                if (confirm(msg)) {
                    // 명시적 퇴장/기권 신호 전송
                    WSManager.sendProgress(this.roomId, this.memberId, "0:EXITED");
                    // 약간의 시간을 주어 메시지 전송 보장 후 이동
                    setTimeout(() => location.href = "/battle/room/lobby", 100);
                } else {
                    e.preventDefault();
                }
            };
        }
    },

    stopExecution() {
        this.engine.isAnimating = false;
        document.getElementById('runBtn').style.display = 'inline-block';
        document.getElementById('stopBtn').style.display = 'none';
        WSManager.sendMazeControl(this.roomId, this.memberId, "STOP");
    },

    submitScore(cost, code) {
        // 1. 서버에 최종 점수 제출 (BattleParticipantStatus를 FINISHED로 변경하도록 요청)
        WSManager.sendProgress(this.roomId, this.memberId, `${cost}:FINISHED`);
        WSManager.sendMazeControl(this.roomId, this.memberId, "STOP");

        // 2. UI 전환: 관전 모드
        document.getElementById('runBtn').style.display = 'none';
        document.getElementById('stopBtn').style.display = 'none';
        
        // 포기 버튼을 '방 나가기'로 교체
        const exitBtn = document.getElementById('exit-battle-btn');
        if (exitBtn) {
            exitBtn.innerText = "방 나가기";
            exitBtn.classList.remove('btn-outline');
            exitBtn.classList.add('btn-blue');
            exitBtn.onclick = (e) => {
                if(!confirm("방을 나가시겠습니까? (결과는 나중에 확인 가능합니다)")) e.preventDefault();
            };
        }

        // 에디터 잠금
        if (EditorCore.editor) EditorCore.editor.updateOptions({ readOnly: true });
        
        alert("제출 완료! 다른 플레이어의 진행을 기다리거나 방을 나갈 수 있습니다.");
    },

    setupWebSocket() {
        if (this.roomId && this.memberId) {
            WSManager.connect(this.roomId, this.memberId);
            WSManager.sendConfirm(this.roomId, this.memberId);
            
            // 전역 동기화 함수 등록 (Engine 내부 호출용)
            window.sendMazePosition = (x, y, dir, cost, isCleared) => {
                WSManager.sendProgress(this.roomId, this.memberId, `${x},${y},${dir},${cost}:${isCleared ? 'FINISHED' : 'MOVING'}`);
            };

            // 타 참여자 진행도 수신 핸들러
            window.updateBattleHUD = (memberId, data) => {
                if (memberId == this.memberId) {
                    // [배치 수신] | 로 구분된 명령어 뭉치 처리
                    const commands = data.split('|');
                    commands.forEach(cmd => {
                        if (cmd.startsWith('CMD:')) {
                            this.engine.commandBuffer.push(cmd.replace('CMD:', '').trim());
                        }
                    });

                    // [프리페칭] 버퍼가 부족하면 서버에 미리 요청 (애니메이션 끊김 방지)
                    if (this.engine.commandBuffer.length < 20) {
                        WSManager.sendMazeControl(this.roomId, this.memberId, "MORE");
                    }
                    return;
                }
                
                // 상대방 로봇 위치 & 코스트 갱신 (기존 동일)
                const [pos, status] = data.split(':');
                if (pos.includes(',')) {
                    const parts = pos.split(',');
                    const x = Number(parts[0]), y = Number(parts[1]), dir = Number(parts[2]), cost = parts.length > 3 ? Number(parts[3]) : 0;
                    this.engine.playerPositions[memberId] = { x, y, dir, cost, isCleared: status === 'FINISHED' };
                    this.engine.draw();
                }
            };
        }
    }
};
