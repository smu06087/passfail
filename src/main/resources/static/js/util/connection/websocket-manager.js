// websocket-manager.js 

const WSManager = {
    stompClient: null,
    isConnected: false,
    messageQueue: [],

    connect(roomId, memberId) {
        const socket = new SockJS('/ws-stomp');
        this.stompClient = Stomp.over(socket);

        const headers = {
            roomId: roomId,
            memberId: memberId
        };

        this.stompClient.connect(headers, () => {
            console.log("WS Connected");
            this.isConnected = true;
            
            // 전송 대기 중인 메시지들 처리
            while (this.messageQueue.length > 0) {
                const item = this.messageQueue.shift();
                this.stompClient.send(item.destination, {}, item.body);
            }

            // battle room page
            this.stompClient.subscribe(`/topic/room/${roomId}`, (msg) => {
                const content = JSON.parse(msg.body);
                switch (content.type) {
                    case "chat":
                        this.renderChat(content.message, content.sender);
                        break;
                    case "join":
                        console.log("입장:", content.username);
                        if (window.renderUser) window.renderUser(content);
                        if (window.checkStartButton) window.checkStartButton();
                        break;
                    case "status":
                        console.log("상태:", content.message);
                        if (content.message === "GAME_START") {
                            window.dispatchEvent(new Event('gameStart'));
                            const mode = content.mode || 'QUICK';
                            setTimeout(() => {
                                if (mode === 'ROGUE') {
                                    location.href = `/battle/mode/rogueMap?roomId=${roomId}&seed=${content.seed}`;
                                } else {
                                    location.href = `/battle/mode/editor?roomId=${roomId}&seed=${content.seed}`;
                                }
                            }, 100);
                        } else if (content.message === "READY") {
                            if (window.updateUserStatus) window.updateUserStatus(content.memberId, "READY!");
                        } else if (content.message === "UNREADY") {
                            if (window.updateUserStatus) window.updateUserStatus(content.memberId, "WAITING");
                        } else if (content.message === "SETTLED") {
                            // 전체 결과 정산됨
                            if (window.BattleResult) {
                                window.BattleResult.showTotal(content.results);
                            }
                        }
                        if (window.checkStartButton) window.checkStartButton();
                        break;
                    case "individual_result":
                        // 본인의 개인 결과 수신
                        if (content.memberId == memberId && window.BattleResult) {
                            window.BattleResult.showIndividual(content);
                        }
                        break;
                    case "leave":
                        console.log("퇴장:", content.memberId);
                        if (window.removeUser) window.removeUser(content.memberId);
                        if (window.checkStartButton) window.checkStartButton();
                        break;
                    case "position":
                        console.log("위치 업데이트:", content.memberId, content.nodeId, content.slotIndex, content.isCleared);
                        if (window.updatePlayerPosition) {
                            window.updatePlayerPosition(content.memberId, content.nodeId, content.slotIndex, content.isCleared);
                        }
                        break;
                    case "progress":
                        console.log("진행도 업데이트:", content.memberId, content.data);
                        if (window.updateBattleHUD) {
                            window.updateBattleHUD(content.memberId, content.data);
                        }
                        break;
                }
            });
        });
    },

    _send(destination, body) {
        if (this.isConnected) {
            this.stompClient.send(destination, {}, body);
        } else {
            console.warn("WS not connected yet. Queuing message...");
            this.messageQueue.push({ destination, body });
        }
    },

    sendChat(roomId, message, memberId) {
        this._send(
            "/app/chat/send",
            JSON.stringify({ roomId, message, memberId })
        );
    },

    sendStatus(roomId, message, memberId) {
        this._send(
            "/app/room/status",
            JSON.stringify({ roomId, message, memberId })
        );
    },

    sendConfirm(roomId, memberId) {
        this._send(
            "/app/room/confirm",
            JSON.stringify({ roomId, memberId })
        );
    },

    sendPosition(roomId, memberId, nodeId) {
        this._send(
            "/app/room/position",
            JSON.stringify({ roomId, message: nodeId, memberId }) // message 필드에 nodeId를 담아 보냄
        );
    },

    sendProgress(roomId, memberId, progressData) {
        this._send(
            "/app/room/progress",
            JSON.stringify({ roomId, message: progressData, memberId })
        );
    },

    sendMazeRun(roomId, memberId, code, mapData, language) {
        this._send(
            "/app/maze/run",
            JSON.stringify({ roomId, memberId, code, mapData, language: language || "JAVA" })
        );
    },

    sendMazeControl(roomId, memberId, action) {
        this._send(
            "/app/maze/control",
            JSON.stringify({ roomId, memberId, action })
        );
    },

    sendLeave(roomId, memberId) {
        // WebSocket이 끊기기 전에 명시적으로 퇴장 알림을 보낼 수도 있지만,
        // 여기서는 서버의 leave API를 호출하는 방식을 주로 사용하거나 
        // DisconnectListener에 의존합니다.
        // 명시적으로 보낼 경우:
        this._send(
            "/app/room/status",
            JSON.stringify({ roomId, message: "LEAVE", memberId })
        );
    },

    renderChat(message, sender) {
        const box = document.getElementById("chat-box");
        if (box) {
            const div = document.createElement("div");
            div.className = "text-sm mb-1";

            const nameSpan = document.createElement("span");
            nameSpan.className = "font-black text-slate-800 mr-2";
            nameSpan.textContent = sender + ":";

            const msgSpan = document.createElement("span");
            msgSpan.className = "text-slate-600 font-medium";
            msgSpan.textContent = message;

            div.appendChild(nameSpan);
            div.appendChild(msgSpan);
            box.appendChild(div);
            box.scrollTop = box.scrollHeight;
        }
    },

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect(() => {
                console.log("WS Disconnected");
                this.isConnected = false;
            });
        }
    }
};

window.addEventListener("beforeunload", () => {
    WSManager.disconnect();
});

export default WSManager;

