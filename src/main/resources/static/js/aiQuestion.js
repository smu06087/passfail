(function () {
    function initChatbot() {
        const widget = document.getElementById("chatbotWidget");
        if (!widget || widget.dataset.initialized === "true") {
            return;
        }

        widget.dataset.initialized = "true";

        const panel = document.getElementById("chatbotPanel");
        const fab = document.getElementById("chatbotFab");
        const closeButton = document.getElementById("chatbotClose");
        const settingsButton = document.getElementById("chatbotHeaderSettings");
        const startButton = document.getElementById("chatbotStartButton");
        const newChatButton = document.getElementById("chatbotNewChatButton");
        const roomList = document.getElementById("chatbotRoomList");
        const messageArea = document.getElementById("chatbotMessageArea");
        const form = document.getElementById("chatbotForm");
        const input = document.getElementById("chatbotInput");
        const sendButton = document.getElementById("chatbotSendButton");
        const status = document.getElementById("chatbotStatus");
        const backButton = document.getElementById("chatbotBackButton");
        const resetButton = document.getElementById("chatbotResetRooms");
        const headerTitle = document.getElementById("chatbotHeaderTitle");
        const chatSubtitle = document.getElementById("chatbotChatSubtitle");
        const tabButtons = Array.from(document.querySelectorAll(".chatbot-tab"));

        const screens = {
            home: document.getElementById("chatbotHomeScreen"),
            messages: document.getElementById("chatbotMessagesScreen"),
            settings: document.getElementById("chatbotSettingsScreen"),
            chat: document.getElementById("chatbotChatScreen")
        };

        if (!panel || !fab || !closeButton || !settingsButton || !startButton || !roomList || !messageArea ||
            !form || !input || !sendButton || !status || !backButton || !resetButton || !headerTitle ||
            !chatSubtitle || Object.values(screens).some(function (screen) { return !screen; })) {
            return;
        }

        const state = {
            sessions: [],
            activeSessionId: null,
            pending: false,
            openMenuSessionId: null
        };

        document.addEventListener("click", function () {
            if (state.openMenuSessionId !== null) {
                state.openMenuSessionId = null;
                renderRooms();
            }
        });

        fab.addEventListener("click", async function () {
            const shouldOpen = !panel.classList.contains("is-open");
            panel.classList.toggle("is-open", shouldOpen);
            panel.setAttribute("aria-hidden", String(!shouldOpen));

            if (!shouldOpen) {
                return;
            }

            await refreshSessions();
            switchView(state.sessions.length > 0 ? "messages" : "home");
        });

        closeButton.addEventListener("click", function () {
            panel.classList.remove("is-open");
            panel.setAttribute("aria-hidden", "true");
        });

        settingsButton.addEventListener("click", function () {
            switchView("settings");
        });

        startButton.addEventListener("click", async function () {
            await createSession();
        });

        if (newChatButton) {
            newChatButton.addEventListener("click", async function () {
                await createSession();
            });
        }

        backButton.addEventListener("click", async function () {
            await refreshSessions();
            switchView("messages");
        });

        resetButton.addEventListener("click", async function () {
            if (state.sessions.length === 0) {
                status.textContent = "삭제할 채팅방이 없습니다.";
                return;
            }

            if (!window.confirm("채팅방을 모두 삭제할까요?")) {
                return;
            }

            setPending(true, "채팅방을 삭제하는 중입니다.");

            try {
                for (const session of state.sessions) {
                    await deleteSessionById(session.sessionId, true);
                }
                state.activeSessionId = null;
                state.openMenuSessionId = null;
                await refreshSessions();
                switchView(state.sessions.length > 0 ? "messages" : "home");
                status.textContent = "채팅방을 모두 삭제했습니다.";
            } catch (error) {
                status.textContent = error.message || "채팅방 삭제에 실패했습니다.";
            } finally {
                setPending(false);
            }
        });

        tabButtons.forEach(function (button) {
            button.addEventListener("click", async function () {
                const view = button.dataset.view;
                if (!view) {
                    return;
                }

                if (view === "messages") {
                    await refreshSessions();
                }

                switchView(view);
            });
        });

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            submitMessage();
        });

        input.addEventListener("keydown", function (event) {
            if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                submitMessage();
            }
        });

        input.addEventListener("input", function () {
            autoResize(input);
        });

        switchView("home");
        autoResize(input);

        function switchView(view) {
            Object.keys(screens).forEach(function (key) {
                screens[key].classList.toggle("chatbot-screen-hidden", key !== view);
            });

            tabButtons.forEach(function (button) {
                button.classList.toggle("is-active", button.dataset.view === view);
            });

            headerTitle.textContent = "passfail";

            if (view === "chat") {
                requestAnimationFrame(function () {
                    input.focus();
                    scrollMessagesToBottom();
                });
            }
        }

        async function refreshSessions() {
            try {
                const response = await fetch("/ai/session");
                if (response.status === 401) {
                    state.sessions = [];
                    state.activeSessionId = null;
                    roomList.innerHTML = '<div class="chatbot-room-empty">로그인 후 채팅방을 사용할 수 있습니다.</div>';
                    status.textContent = "로그인이 필요합니다.";
                    switchView("home");
                    return;
                }
                if (!response.ok) {
                    throw new Error("채팅방 목록을 불러오지 못했습니다.");
                }

                const data = await response.json();
                state.sessions = Array.isArray(data.sessions) ? data.sessions : [];

                if (state.activeSessionId != null) {
                    const exists = state.sessions.some(function (session) {
                        return session.sessionId === state.activeSessionId;
                    });
                    if (!exists) {
                        state.activeSessionId = state.sessions.length > 0 ? state.sessions[0].sessionId : null;
                    }
                } else if (state.sessions.length > 0) {
                    state.activeSessionId = state.sessions[0].sessionId;
                }

                renderRooms();
            } catch (error) {
                state.sessions = [];
                roomList.innerHTML = '<div class="chatbot-room-empty">채팅방 목록을 불러오지 못했습니다.</div>';
                status.textContent = error.message || "채팅방 목록 조회에 실패했습니다.";
            }
        }

        async function createSession() {
            setPending(true, "새 채팅방을 만드는 중입니다.");

            try {
                const response = await fetch("/ai/session", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({})
                });

                if (response.status === 401) {
                    window.location.href = "/login";
                    return;
                }
                if (!response.ok) {
                    throw new Error("채팅방을 생성하지 못했습니다.");
                }

                const data = await response.json();
                if (!data.success || !data.sessionId) {
                    throw new Error(data.message || "채팅방을 생성하지 못했습니다.");
                }

                state.activeSessionId = data.sessionId;
                state.openMenuSessionId = null;
                renderMessages([]);
                chatSubtitle.textContent = "질문을 바로 입력해보세요.";
                switchView("chat");
                await openSession(data.sessionId);
                await refreshSessions();
                status.textContent = "";
            } catch (error) {
                status.textContent = error.message || "채팅방 생성에 실패했습니다.";
            } finally {
                setPending(false);
            }
        }

        async function openSession(sessionId) {
            state.activeSessionId = sessionId;
            state.openMenuSessionId = null;
            setPending(true, "대화 내용을 불러오는 중입니다.");

            try {
                const response = await fetch("/ai/session/" + sessionId);
                if (response.status === 401) {
                    throw new Error("로그인 후 채팅방을 조회할 수 있습니다.");
                }
                if (!response.ok) {
                    throw new Error("대화 내용을 불러오지 못했습니다.");
                }

                const data = await response.json();
                renderMessages(Array.isArray(data.messages) ? data.messages : []);
                chatSubtitle.textContent = data.title || "질문을 이어서 입력해보세요.";
                renderRooms();
                switchView("chat");
                status.textContent = "";
            } catch (error) {
                status.textContent = error.message || "채팅방 조회에 실패했습니다.";
            } finally {
                setPending(false);
            }
        }

        async function submitMessage() {
            const content = input.value.trim();
            if (!content || state.pending) {
                return;
            }

            if (!state.activeSessionId) {
                await createSession();
                if (!state.activeSessionId) {
                    return;
                }
            }

            appendMessage({
                role: "USER",
                content: content,
                sentAt: new Date().toISOString()
            });

            input.value = "";
            autoResize(input);
            setPending(true, "AI가 답변을 작성 중입니다.");

            try {
                const response = await fetch("/ai/chat", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        sessionId: state.activeSessionId,
                        content: content
                    })
                });

                if (response.status === 401) {
                    throw new Error("로그인 후 AI 채팅을 사용할 수 있습니다.");
                }
                if (!response.ok) {
                    throw new Error("AI 응답 요청에 실패했습니다.");
                }

                const data = await response.json();
                if (!data.success) {
                    throw new Error(data.message || "AI 응답 요청에 실패했습니다.");
                }

                appendMessage({
                    role: "ASSISTANT",
                    content: data.answer || "답변을 받지 못했습니다.",
                    sentAt: new Date().toISOString()
                });

                await refreshSessions();
                status.textContent = "";
            } catch (error) {
                appendMessage({
                    role: "ASSISTANT",
                    content: error.message || "현재 답변을 가져오지 못했습니다.",
                    sentAt: new Date().toISOString()
                });
                status.textContent = "네트워크 상태를 확인해주세요.";
            } finally {
                setPending(false);
            }
        }

        function renderRooms() {
            roomList.innerHTML = "";

            if (state.sessions.length === 0) {
                roomList.innerHTML = '<div class="chatbot-room-empty">아직 생성된 채팅방이 없습니다.</div>';
                return;
            }

            state.sessions.forEach(function (session) {
                const card = document.createElement("article");
                card.className = "chatbot-room-card" + (session.sessionId === state.activeSessionId ? " is-active" : "");

                const mainButton = document.createElement("button");
                mainButton.type = "button";
                mainButton.className = "chatbot-room-main";
                mainButton.innerHTML =
                    '<span class="chatbot-room-avatar">PF</span>' +
                    '<span class="chatbot-room-meta">' +
                    '<span class="chatbot-room-topline">' +
                    "<strong>" + escapeHtml(session.title || "새로운 채팅") + "</strong>" +
                    "</span>" +
                    '<span class="chatbot-room-preview">' + escapeHtml(session.preview || "새로운 질문을 시작해보세요.") + "</span>" +
                    '<span class="chatbot-room-date">' + escapeHtml(formatRoomDate(session.updatedAt)) + "</span>" +
                    "</span>";

                mainButton.addEventListener("click", function () {
                    openSession(session.sessionId);
                });

                const menuWrap = document.createElement("div");
                menuWrap.className = "chatbot-room-menu-wrap";

                const menuTrigger = document.createElement("button");
                menuTrigger.type = "button";
                menuTrigger.className = "chatbot-room-menu-trigger";
                menuTrigger.setAttribute("aria-label", "Room actions");
                menuTrigger.textContent = "...";
                menuTrigger.addEventListener("click", function (event) {
                    event.stopPropagation();
                    state.openMenuSessionId = state.openMenuSessionId === session.sessionId ? null : session.sessionId;
                    renderRooms();
                });

                menuWrap.appendChild(menuTrigger);

                if (state.openMenuSessionId === session.sessionId) {
                    const menu = document.createElement("div");
                    menu.className = "chatbot-room-menu";
                    menu.addEventListener("click", function (event) {
                        event.stopPropagation();
                    });

                    const leaveButton = document.createElement("button");
                    leaveButton.type = "button";
                    leaveButton.className = "chatbot-room-menu-action";
                    leaveButton.textContent = "방 나가기";
                    leaveButton.addEventListener("click", async function (event) {
                        event.stopPropagation();
                        await handleLeaveSession(session.sessionId);
                    });

                    menu.appendChild(leaveButton);
                    menuWrap.appendChild(menu);
                }

                card.appendChild(mainButton);
                card.appendChild(menuWrap);
                roomList.appendChild(card);
            });
        }

        async function handleLeaveSession(sessionId) {
            setPending(true, "채팅방을 삭제하는 중입니다.");

            try {
                await deleteSessionById(sessionId, true);
                state.openMenuSessionId = null;
                if (state.activeSessionId === sessionId) {
                    state.activeSessionId = null;
                }
                await refreshSessions();
                switchView(state.sessions.length > 0 ? "messages" : "home");
                status.textContent = "채팅방을 삭제했습니다.";
            } catch (error) {
                status.textContent = error.message || "채팅방 삭제에 실패했습니다.";
            } finally {
                setPending(false);
            }
        }

        async function deleteSessionById(sessionId, throwOnFailure) {
            const response = await fetch("/ai/session/" + sessionId, {
                method: "DELETE"
            });

            if (response.ok) {
                return;
            }

            if (!throwOnFailure) {
                return;
            }

            if (response.status === 401) {
                throw new Error("로그인 후 채팅방을 관리할 수 있습니다.");
            }

            let message = "채팅방 삭제에 실패했습니다.";
            try {
                const data = await response.json();
                if (data && data.message) {
                    message = data.message;
                }
            } catch (error) {
                // Ignore response parsing errors for deletion.
            }
            throw new Error(message);
        }

        function renderMessages(messages) {
            messageArea.innerHTML = "";
            messages.forEach(appendMessage);
            scrollMessagesToBottom();
        }

        function appendMessage(message) {
            const role = String(message.role || "").toUpperCase() === "USER" ? "user" : "assistant";
            const row = document.createElement("article");
            row.className = "chatbot-message " + role;

            const wrap = document.createElement("div");
            wrap.className = "chatbot-message-wrap";

            const avatar = document.createElement("span");
            avatar.className = "chatbot-message-avatar";
            avatar.textContent = role === "assistant" ? "PF" : "ME";

            const bubble = document.createElement("div");
            bubble.className = "chatbot-bubble";

            const text = document.createElement("p");
            text.textContent = message.content || "";

            const time = document.createElement("span");
            time.className = "chatbot-bubble-time";
            time.textContent = formatTimestamp(message.sentAt);

            bubble.appendChild(text);
            bubble.appendChild(time);
            wrap.appendChild(avatar);
            wrap.appendChild(bubble);
            row.appendChild(wrap);
            messageArea.appendChild(row);

            scrollMessagesToBottom();
        }

        function setPending(isPending, text) {
            state.pending = isPending;
            sendButton.disabled = isPending;
            status.textContent = text || "";
        }
    }

    function autoResize(input) {
        input.style.height = "24px";
        input.style.height = Math.min(input.scrollHeight, 96) + "px";
    }

    function scrollMessagesToBottom() {
        const messageArea = document.getElementById("chatbotMessageArea");
        if (messageArea) {
            messageArea.scrollTop = messageArea.scrollHeight;
        }
    }

    function formatTimestamp(value) {
        if (!value) {
            return "";
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "";
        }

        return date.toLocaleTimeString("ko-KR", {
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function formatRoomDate(value) {
        if (!value) {
            return "";
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "";
        }

        return date.toLocaleDateString("ko-KR", {
            month: "2-digit",
            day: "2-digit"
        });
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initChatbot);
    } else {
        initChatbot();
    }
})();
