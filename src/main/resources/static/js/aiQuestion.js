(function () {
    const TEXT = {
        noRoomsToDelete: "삭제할 채팅방이 없습니다.",
        confirmDeleteAll: "전체 채팅방을 삭제할까요?",
        deletingAll: "전체 채팅방을 삭제하는 중입니다.",
        deletedAll: "전체 채팅방을 삭제했습니다.",
        deleteAllFailed: "전체 채팅방 삭제에 실패했습니다.",
        loginRequiredToUse: "로그인 후 채팅을 사용할 수 있습니다.",
        loginRequired: "로그인이 필요합니다.",
        roomListLoadFailed: "채팅방 목록을 불러오지 못했습니다.",
        creatingRoom: "새 채팅방을 만드는 중입니다.",
        createRoomFailed: "채팅방을 생성하지 못했습니다.",
        enterQuestion: "질문을 입력해 보세요.",
        loadingConversation: "대화 내용을 불러오는 중입니다.",
        loadConversationFailed: "대화 내용을 불러오지 못했습니다.",
        sending: "AI가 답변을 작성하는 중입니다.",
        chatRequestFailed: "AI 답변 요청에 실패했습니다.",
        checkingNetwork: "네트워크 상태를 확인해 주세요.",
        noRoomsYet: "아직 생성된 채팅방이 없습니다.",
        firstQuestion: "첫 질문을 시작해 보세요.",
        roomActions: "채팅방 메뉴",
        deleteLabel: "삭제",
        deletingRoom: "채팅방을 삭제하는 중입니다.",
        deletedRoom: "채팅방을 삭제했습니다.",
        deleteRoomFailed: "채팅방 삭제에 실패했습니다.",
        loginRequiredToManage: "로그인 후 채팅방을 관리할 수 있습니다.",
        imageAttached: "[이미지 첨부]",
        supportListEmpty: "대기 중인 상담 요청이 없습니다.",
        supportLoading: "상담 요청을 불러오는 중입니다.",
        supportListLoadFailed: "상담 요청 목록을 불러오지 못했습니다.",
        supportReplyPlaceholder: "상담 답변을 입력하세요.",
        supportReplySending: "상담 답변을 전송하는 중입니다.",
        supportReplyFailed: "상담 답변 전송에 실패했습니다.",
        supportChatTitle: "상담 대화",
        supportChatSubtitle: "회원과 실시간 상담을 진행할 수 있습니다.",
        supportMemberPrefix: "회원",
        supportOpenPrefix: "상담 요청",
        supportRequestedStatus: "상담 요청 접수",
        supportHandledStatus: "답변 완료",
        supportModeStatus: "실시간 상담 모드",
        aiModeStatus: "AI 상담 도우미",
        supportAgentBadge: "상담원",
        supportAgentDefaultName: "상담원"
    };

    function initChatbot() {
        const widget = document.getElementById("chatbotWidget");
        if (!widget || widget.dataset.initialized === "true") {
            return;
        }

        widget.dataset.initialized = "true";

        const isAdmin = widget.dataset.isAdmin === "true";
        const panel = document.getElementById("chatbotPanel");
        const fab = document.getElementById("chatbotFab");
        const fabNotice = document.getElementById("chatbotFabNotice");
        const closeButton = document.getElementById("chatbotClose");
        const startButton = document.getElementById("chatbotStartButton");
        const newChatButton = document.getElementById("chatbotNewChatButton");
        const roomList = document.getElementById("chatbotRoomList");
        const supportList = document.getElementById("chatbotSupportList");
        const messageArea = document.getElementById("chatbotMessageArea");
        const form = document.getElementById("chatbotForm");
        const input = document.getElementById("chatbotInput");
        const imageInput = document.getElementById("chatbotImageInput");
        const attachmentButton = document.getElementById("chatbotAttachmentButton");
        const attachmentPreview = document.getElementById("chatbotAttachmentPreview");
        const attachmentName = document.getElementById("chatbotAttachmentName");
        const attachmentRemove = document.getElementById("chatbotAttachmentRemove");
        const sendButton = document.getElementById("chatbotSendButton");
        const status = document.getElementById("chatbotStatus");
        const backButton = document.getElementById("chatbotBackButton");
        const resetButton = document.getElementById("chatbotResetRooms");
        const headerStatus = document.getElementById("chatbotHeaderStatus");
        const chatTitle = document.getElementById("chatbotChatTitle");
        const chatSubtitle = document.getElementById("chatbotChatSubtitle");
        const supportCloseButton = document.getElementById("chatbotSupportCloseButton");
        const supportTab = document.getElementById("chatbotSupportTab");
        const tabbar = document.getElementById("chatbotTabbar");
        const tabButtons = Array.from(document.querySelectorAll(".chatbot-tab"));

        const screens = {
            home: document.getElementById("chatbotHomeScreen"),
            messages: document.getElementById("chatbotMessagesScreen"),
            support: document.getElementById("chatbotSupportScreen"),
            chat: document.getElementById("chatbotChatScreen")
        };

        if (!panel || !fab || !fabNotice || !closeButton || !startButton || !newChatButton || !roomList ||
            !supportList || !messageArea || !form || !input || !imageInput || !attachmentButton ||
            !attachmentPreview || !attachmentName || !attachmentRemove || !sendButton || !status ||
            !backButton || !resetButton || !headerStatus || !chatTitle || !chatSubtitle ||
            !supportCloseButton || !tabbar) {
            return;
        }

        const state = {
            sessions: [],
            supportSessions: [],
            activeSessionId: null,
            activeSupportSessionId: null,
            pending: false,
            openMenuSessionId: null,
            attachment: null,
            chatMode: "member",
            pollId: null,
            activeSupportStatus: null,
            memberPollId: null,
            hasUnreadNotice: false,
            knownSupportSessionIds: []
        };

        if (isAdmin && supportTab) {
            supportTab.classList.remove("chatbot-screen-hidden");
            tabbar.classList.add("chatbot-tabbar-admin");
        }

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
                stopPolling();
                updateFabNotice();
                return;
            }

            await refreshSessions();
            state.hasUnreadNotice = false;
            persistUnreadNoticeState();
            updateFabNotice();
            if (isAdmin) {
                await refreshSupportSessions();
            }
            switchView(state.sessions.length > 0 ? "messages" : "home");
        });

        closeButton.addEventListener("click", function () {
            panel.classList.remove("is-open");
            panel.setAttribute("aria-hidden", "true");
            stopPolling();
            updateFabNotice();
        });

        startButton.addEventListener("click", createSession);
        newChatButton.addEventListener("click", createSession);

        backButton.addEventListener("click", async function () {
            if (state.chatMode === "support") {
                await refreshSupportSessions();
                switchView("support");
                return;
            }

            await refreshSessions();
            switchView("messages");
        });

        resetButton.addEventListener("click", async function () {
            if (state.chatMode === "support") {
                status.textContent = "상담 대화에서는 전체 삭제를 사용할 수 없습니다.";
                return;
            }

            if (state.sessions.length === 0) {
                status.textContent = TEXT.noRoomsToDelete;
                return;
            }

            if (!window.confirm(TEXT.confirmDeleteAll)) {
                return;
            }

            setPending(true, TEXT.deletingAll);
            try {
                for (const session of state.sessions) {
                    await deleteSessionById(session.sessionId, true);
                }
                state.activeSessionId = null;
                await refreshSessions();
                switchView(state.sessions.length > 0 ? "messages" : "home");
                status.textContent = TEXT.deletedAll;
            } catch (error) {
                status.textContent = error.message || TEXT.deleteAllFailed;
            } finally {
                setPending(false);
            }
        });

        tabButtons.forEach(function (button) {
            button.addEventListener("click", async function () {
                const view = button.dataset.view;
                if (view === "messages") {
                    await refreshSessions();
                }
                if (view === "support" && isAdmin) {
                    await refreshSupportSessions();
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

        attachmentButton.addEventListener("click", function () {
            if (!state.pending && state.chatMode === "member") {
                imageInput.click();
            }
        });

        imageInput.addEventListener("change", function () {
            state.attachment = imageInput.files && imageInput.files[0] ? imageInput.files[0] : null;
            renderAttachment();
        });

        attachmentRemove.addEventListener("click", clearAttachment);
        supportCloseButton.addEventListener("click", closeSupportSession);

        switchView("home");
        autoResize(input);
        renderAttachment();
        loadUnreadNoticeState();
        updateFabNotice();

        if (isAdmin) {
            startAdminPolling();
        } else {
            startMemberPolling();
        }

        function switchView(view) {
            Object.keys(screens).forEach(function (key) {
                screens[key].classList.toggle("chatbot-screen-hidden", key !== view);
            });

            tabButtons.forEach(function (button) {
                button.classList.toggle("is-active", button.dataset.view === view);
            });

            headerStatus.textContent = state.chatMode === "support" ? TEXT.supportModeStatus : TEXT.aiModeStatus;
            resetButton.style.display = state.chatMode === "support" ? "none" : "";
            supportCloseButton.classList.toggle("chatbot-screen-hidden", !(view === "chat" && state.chatMode === "support"));
            supportCloseButton.disabled = state.pending || state.activeSupportStatus === "CLOSED";

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
                    roomList.innerHTML = '<div class="chatbot-room-empty">' + TEXT.loginRequiredToUse + "</div>";
                    status.textContent = TEXT.loginRequired;
                    switchView("home");
                    return;
                }
                if (!response.ok) {
                    throw new Error(TEXT.roomListLoadFailed);
                }

                const data = await response.json();
                state.sessions = Array.isArray(data.sessions) ? data.sessions : [];
                reconcileUnreadSessions(state.sessions);
                renderRooms();
            } catch (error) {
                roomList.innerHTML = '<div class="chatbot-room-empty">' + TEXT.roomListLoadFailed + "</div>";
                status.textContent = error.message || TEXT.roomListLoadFailed;
            }
        }

        async function refreshSupportSessions(silent) {
            if (!isAdmin) {
                return;
            }

            try {
                const response = await fetch("/api/admin/ai-handoffs");
                if (!response.ok) {
                    throw new Error(TEXT.supportListLoadFailed);
                }
                state.supportSessions = await response.json();
                reconcileSupportUnread(state.supportSessions);
                renderSupportRooms();
            } catch (error) {
                if (!silent) {
                    supportList.innerHTML = '<div class="chatbot-room-empty">' + TEXT.supportListLoadFailed + "</div>";
                    status.textContent = error.message || TEXT.supportListLoadFailed;
                }
            }
        }

        async function createSession() {
            setPending(true, TEXT.creatingRoom);
            try {
                const response = await fetch("/ai/session", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({})
                });

                if (response.status === 401) {
                    window.location.href = "/login";
                    return;
                }
                if (!response.ok) {
                    throw new Error(TEXT.createRoomFailed);
                }

                const data = await response.json();
                if (!data.success || !data.sessionId) {
                    throw new Error(data.message || TEXT.createRoomFailed);
                }

                state.chatMode = "member";
                state.activeSessionId = data.sessionId;
                clearAttachment();
                renderMessages([]);
                chatTitle.textContent = "passfail";
                chatSubtitle.textContent = TEXT.enterQuestion;
                input.placeholder = "AI에게 질문을 입력하세요";
                attachmentButton.style.display = "";
                markSessionAsSeen(data.sessionId);
                switchView("chat");
                await openMemberSession(data.sessionId);
                await refreshSessions();
            } catch (error) {
                status.textContent = error.message || TEXT.createRoomFailed;
            } finally {
                setPending(false);
            }
        }

        async function openMemberSession(sessionId) {
            state.chatMode = "member";
            state.activeSessionId = sessionId;
            state.activeSupportStatus = null;
            stopPolling();
            setPending(true, TEXT.loadingConversation);

            try {
                const response = await fetch("/ai/session/" + sessionId);
                if (!response.ok) {
                    throw new Error(TEXT.loadConversationFailed);
                }

                const data = await response.json();
                renderMessages(Array.isArray(data.messages) ? data.messages : []);
                chatTitle.textContent = "passfail";
                chatSubtitle.textContent = data.title || TEXT.enterQuestion;
                input.placeholder = "AI에게 질문을 입력하세요";
                attachmentButton.style.display = "";
                switchView("chat");
                status.textContent = "";
            } catch (error) {
                status.textContent = error.message || TEXT.loadConversationFailed;
            } finally {
                setPending(false);
            }
        }

        async function openSupportSession(sessionId) {
            state.chatMode = "support";
            state.activeSupportSessionId = sessionId;
            state.activeSupportStatus = null;
            clearAttachment();
            input.placeholder = TEXT.supportReplyPlaceholder;
            attachmentButton.style.display = "none";
            chatSubtitle.textContent = TEXT.supportChatSubtitle;
            switchView("chat");
            await loadSupportDetail(sessionId, false);
            startPolling();
        }

        async function loadSupportDetail(sessionId, silent) {
            try {
                if (!silent) {
                    setPending(true, TEXT.loadingConversation);
                }

                const response = await fetch("/api/admin/ai-handoffs/" + sessionId);
                if (!response.ok) {
                    throw new Error(TEXT.loadConversationFailed);
                }

                const data = await response.json();
                const current = state.supportSessions.find(function (item) {
                    return item.sessionId === sessionId;
                });
                state.activeSupportStatus = data.handoffStatus || null;

                chatTitle.textContent = current && current.memberName
                    ? TEXT.supportMemberPrefix + " · " + current.memberName
                    : TEXT.supportChatTitle;
                chatSubtitle.textContent = resolveSupportSubtitle(data.handoffStatus);
                renderMessages(Array.isArray(data.messages) ? data.messages : []);
                supportCloseButton.disabled = state.pending || data.handoffStatus === "CLOSED";
                status.textContent = "";
            } catch (error) {
                status.textContent = error.message || TEXT.loadConversationFailed;
            } finally {
                if (!silent) {
                    setPending(false);
                }
            }
        }

        function startPolling() {
            stopPolling();
            if (!isAdmin) {
                return;
            }

            state.pollId = window.setInterval(async function () {
                if (!panel.classList.contains("is-open")) {
                    return;
                }

                await refreshSupportSessions();
                if (state.chatMode === "support" && state.activeSupportSessionId) {
                    await loadSupportDetail(state.activeSupportSessionId, true);
                }
            }, 3000);
        }

        function stopPolling() {
            if (state.pollId) {
                window.clearInterval(state.pollId);
                state.pollId = null;
            }
        }

        function startMemberPolling() {
            if (state.memberPollId) {
                window.clearInterval(state.memberPollId);
            }

            state.memberPollId = window.setInterval(async function () {
                await refreshSessions();
            }, 15000);
        }

        function startAdminPolling() {
            if (state.memberPollId) {
                window.clearInterval(state.memberPollId);
            }

            state.memberPollId = window.setInterval(async function () {
                await refreshSupportSessions(true);
            }, 10000);
        }

        function getSeenSessionMap() {
            try {
                const raw = window.localStorage.getItem("passfail-ai-seen-sessions");
                return raw ? JSON.parse(raw) : {};
            } catch (error) {
                return {};
            }
        }

        function setSeenSessionMap(map) {
            try {
                window.localStorage.setItem("passfail-ai-seen-sessions", JSON.stringify(map));
            } catch (error) {
                return;
            }
        }

        function markSessionAsSeen(sessionId) {
            if (!sessionId) {
                return;
            }

            const session = state.sessions.find(function (item) {
                return item.sessionId === sessionId;
            });
            const seenMap = getSeenSessionMap();
            seenMap[String(sessionId)] = session && session.updatedAt ? session.updatedAt : new Date().toISOString();
            setSeenSessionMap(seenMap);
            state.hasUnreadNotice = hasUnreadAssistantUpdate(state.sessions, seenMap);
            persistUnreadNoticeState();
            updateFabNotice();
        }

        function reconcileUnreadSessions(sessions) {
            const seenMap = getSeenSessionMap();
            state.hasUnreadNotice = hasUnreadAssistantUpdate(sessions, seenMap);
            persistUnreadNoticeState();
            updateFabNotice();
        }

        function reconcileSupportUnread(sessions) {
            if (!isAdmin) {
                return;
            }

            const currentIds = (sessions || []).map(function (session) {
                return String(session.sessionId);
            });

            if (state.knownSupportSessionIds.length === 0) {
                state.knownSupportSessionIds = currentIds;
                return;
            }

            const knownIds = new Set(state.knownSupportSessionIds);
            const hasNewSupport = currentIds.some(function (id) {
                return !knownIds.has(id);
            });

            if (hasNewSupport && !panel.classList.contains("is-open")) {
                state.hasUnreadNotice = true;
                persistUnreadNoticeState();
                updateFabNotice();
            }

            state.knownSupportSessionIds = currentIds;
        }

        function hasUnreadAssistantUpdate(sessions, seenMap) {
            return (sessions || []).some(function (session) {
                if (String(session.latestRole || "").toUpperCase() !== "ASSISTANT") {
                    return false;
                }

                const updatedAt = Date.parse(session.updatedAt || "");
                const seenAt = Date.parse(seenMap[String(session.sessionId)] || "");
                if (Number.isNaN(updatedAt)) {
                    return false;
                }
                if (Number.isNaN(seenAt)) {
                    return true;
                }
                return updatedAt > seenAt;
            });
        }

        function loadUnreadNoticeState() {
            try {
                state.hasUnreadNotice = window.localStorage.getItem("passfail-ai-unread-notice") === "true";
            } catch (error) {
                state.hasUnreadNotice = false;
            }
        }

        function persistUnreadNoticeState() {
            try {
                window.localStorage.setItem("passfail-ai-unread-notice", state.hasUnreadNotice ? "true" : "false");
            } catch (error) {
                return;
            }
        }

        function updateFabNotice() {
            fabNotice.classList.toggle("chatbot-screen-hidden", !state.hasUnreadNotice || panel.classList.contains("is-open"));
        }

        async function submitMessage() {
            const content = input.value.trim();
            if ((!content && !state.attachment) || state.pending) {
                return;
            }

            if (state.chatMode === "support") {
                await submitSupportReply(content);
                return;
            }

            if (!state.activeSessionId) {
                await createSession();
                return;
            }

            const attachment = state.attachment;
            appendMessage({
                role: "USER",
                content: buildUserPreviewMessage(content, attachment),
                sentAt: new Date().toISOString()
            });

            input.value = "";
            autoResize(input);
            clearAttachment();
            setPending(true, TEXT.sending);

            try {
                const formData = new FormData();
                formData.append("sessionId", String(state.activeSessionId));
                formData.append("content", content);
                if (attachment) {
                    formData.append("image", attachment);
                }

                const response = await fetch("/ai/chat", {
                    method: "POST",
                    body: formData
                });
                if (!response.ok) {
                    const errorData = await safeReadJson(response);
                    throw new Error(errorData && errorData.message ? errorData.message : TEXT.chatRequestFailed);
                }

                const data = await response.json();
                appendMessage({
                    role: "ASSISTANT",
                    content: data.answer || "응답을 받지 못했습니다.",
                    sentAt: new Date().toISOString()
                });
                await refreshSessions();
                markSessionAsSeen(state.activeSessionId);
                status.textContent = "";
            } catch (error) {
                appendMessage({
                    role: "ASSISTANT",
                    content: error.message || "현재 응답을 가져오지 못했습니다.",
                    sentAt: new Date().toISOString()
                });
                status.textContent = TEXT.checkingNetwork;
            } finally {
                setPending(false);
            }
        }

        async function submitSupportReply(content) {
            if (!content) {
                return;
            }

            appendMessage({
                role: "ASSISTANT",
                content: buildAdminPreviewMessage(content),
                sentAt: new Date().toISOString()
            });

            input.value = "";
            autoResize(input);
            setPending(true, TEXT.supportReplySending);

            try {
                const response = await fetch("/api/admin/ai-handoffs/" + state.activeSupportSessionId + "/reply", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ reply: content })
                });
                if (!response.ok) {
                    throw new Error(TEXT.supportReplyFailed);
                }

                await refreshSupportSessions();
                await loadSupportDetail(state.activeSupportSessionId, true);
                status.textContent = "";
            } catch (error) {
                status.textContent = error.message || TEXT.supportReplyFailed;
            } finally {
                setPending(false);
            }
        }

        async function closeSupportSession() {
            if (!state.activeSupportSessionId || state.pending || state.activeSupportStatus === "CLOSED") {
                return;
            }

            setPending(true, "상담을 종료하는 중입니다.");
            try {
                const response = await fetch("/api/admin/ai-handoffs/" + state.activeSupportSessionId + "/close", {
                    method: "POST"
                });
                if (!response.ok) {
                    throw new Error("상담 종료에 실패했습니다.");
                }

                state.activeSupportSessionId = null;
                state.activeSupportStatus = null;
                stopPolling();
                await refreshSupportSessions();
                switchView("support");
                status.textContent = "상담이 종료되었습니다.";
            } catch (error) {
                status.textContent = error.message || "상담 종료에 실패했습니다.";
            } finally {
                setPending(false);
            }
        }

        function renderRooms() {
            roomList.innerHTML = "";

            if (state.sessions.length === 0) {
                roomList.innerHTML = '<div class="chatbot-room-empty">' + TEXT.noRoomsYet + "</div>";
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
                    "<strong>" + escapeHtml(session.title || "새 채팅") + "</strong>" +
                    "</span>" +
                    '<span class="chatbot-room-preview">' + escapeHtml(session.preview || TEXT.firstQuestion) + "</span>" +
                    '<span class="chatbot-room-date">' + escapeHtml(formatRoomDate(session.updatedAt)) + "</span>" +
                    "</span>";

                mainButton.addEventListener("click", function () {
                    openMemberSession(session.sessionId);
                });

                const menuWrap = document.createElement("div");
                menuWrap.className = "chatbot-room-menu-wrap";

                const menuTrigger = document.createElement("button");
                menuTrigger.type = "button";
                menuTrigger.className = "chatbot-room-menu-trigger";
                menuTrigger.setAttribute("aria-label", TEXT.roomActions);
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
                    leaveButton.textContent = TEXT.deleteLabel;
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

        function renderSupportRooms() {
            supportList.innerHTML = "";

            if (state.supportSessions.length === 0) {
                supportList.innerHTML = '<div class="chatbot-room-empty">' + TEXT.supportListEmpty + "</div>";
                return;
            }

            state.supportSessions.forEach(function (session) {
                const card = document.createElement("article");
                card.className = "chatbot-room-card" + (session.sessionId === state.activeSupportSessionId ? " is-active" : "");

                const mainButton = document.createElement("button");
                mainButton.type = "button";
                mainButton.className = "chatbot-room-main";
                mainButton.innerHTML =
                    '<span class="chatbot-room-avatar">CS</span>' +
                    '<span class="chatbot-room-meta">' +
                    '<span class="chatbot-room-topline">' +
                    "<strong>" + escapeHtml((session.memberName || TEXT.supportMemberPrefix) + "님의 요청") + "</strong>" +
                    "</span>" +
                    '<span class="chatbot-room-preview">' + escapeHtml(session.latestQuestion || TEXT.supportOpenPrefix) + "</span>" +
                    '<span class="chatbot-room-date">' + escapeHtml(formatRoomDate(session.requestedAt)) + "</span>" +
                    "</span>";

                mainButton.addEventListener("click", function () {
                    openSupportSession(session.sessionId);
                });

                card.appendChild(mainButton);
                supportList.appendChild(card);
            });
        }

        async function handleLeaveSession(sessionId) {
            setPending(true, TEXT.deletingRoom);
            try {
                await deleteSessionById(sessionId, true);
                await refreshSessions();
                switchView(state.sessions.length > 0 ? "messages" : "home");
                status.textContent = TEXT.deletedRoom;
            } catch (error) {
                status.textContent = error.message || TEXT.deleteRoomFailed;
            } finally {
                setPending(false);
            }
        }

        async function deleteSessionById(sessionId, throwOnFailure) {
            const response = await fetch("/ai/session/" + sessionId, { method: "DELETE" });
            if (response.ok || !throwOnFailure) {
                return;
            }

            if (response.status === 401) {
                throw new Error(TEXT.loginRequiredToManage);
            }

            const data = await safeReadJson(response);
            throw new Error(data && data.message ? data.message : TEXT.deleteRoomFailed);
        }

        function renderMessages(messages) {
            messageArea.innerHTML = "";
            messages.forEach(appendMessage);
            scrollMessagesToBottom();
        }

        function appendMessage(message) {
            const role = String(message.role || "").toUpperCase() === "USER" ? "user" : "assistant";
            const supportAgent = role === "assistant" ? parseSupportAgentMessage(message.content) : null;
            const row = document.createElement("article");
            row.className = "chatbot-message " + role;

            const wrap = document.createElement("div");
            wrap.className = "chatbot-message-wrap";

            const avatar = document.createElement("span");
            avatar.className = "chatbot-message-avatar";
            avatar.textContent = role === "assistant" ? "PF" : "ME";

            const bubble = document.createElement("div");
            bubble.className = "chatbot-bubble";

            if (supportAgent) {
                const label = document.createElement("span");
                label.className = "chatbot-bubble-label";
                label.textContent = supportAgent.name + " (" + TEXT.supportAgentBadge + ")";
                bubble.appendChild(label);
            }

            const text = document.createElement("p");
            text.textContent = supportAgent ? supportAgent.body : (message.content || "");

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

        function resolveSupportSubtitle(statusValue) {
            if (statusValue === "CLOSED") {
                return "상담 종료";
            }

            if (statusValue === "HANDLED") {
                return TEXT.supportHandledStatus;
            }

            return TEXT.supportRequestedStatus;
        }

        function setPending(isPending, text) {
            state.pending = isPending;
            sendButton.disabled = isPending;
            attachmentButton.disabled = isPending || state.chatMode === "support";
            resetButton.disabled = isPending;
            supportCloseButton.disabled = isPending || state.activeSupportStatus === "CLOSED";
            status.textContent = text || "";
        }

        function renderAttachment() {
            if (!state.attachment) {
                attachmentPreview.classList.add("chatbot-screen-hidden");
                attachmentPreview.setAttribute("hidden", "hidden");
                attachmentName.textContent = "";
                return;
            }

            attachmentPreview.classList.remove("chatbot-screen-hidden");
            attachmentPreview.removeAttribute("hidden");
            attachmentName.textContent = state.attachment.name;
        }

        function clearAttachment() {
            state.attachment = null;
            imageInput.value = "";
            renderAttachment();
        }

        function buildAdminPreviewMessage(content) {
            return "[상담원 답변] " + "[상담원:" + TEXT.supportAgentDefaultName + "] " + content;
        }
    }

    function buildUserPreviewMessage(content, attachment) {
        if (!attachment) {
            return content;
        }

        if (!content) {
            return TEXT.imageAttached + " " + attachment.name;
        }

        return content + "\n\n" + TEXT.imageAttached + " " + attachment.name;
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

    async function safeReadJson(response) {
        try {
            return await response.json();
        } catch (error) {
            return null;
        }
    }

    function parseSupportAgentMessage(content) {
        const value = String(content || "");
        const match = value.match(/^\[상담원 답변\](?:\s*\[상담원:([^\]]+)\])?\s*(.*)$/s);
        if (!match) {
            return null;
        }

        return {
            name: (match[1] || TEXT.supportAgentDefaultName).trim(),
            body: (match[2] || "").trim()
        };
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initChatbot);
    } else {
        initChatbot();
    }
})();
