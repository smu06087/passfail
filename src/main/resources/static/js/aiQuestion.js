(function () {
    function initAiQuestion() {
        const widget = document.getElementById("aiChatWidget");
        if (!widget || widget.dataset.initialized === "true") {
            return;
        }

        widget.dataset.initialized = "true";

        const fab = document.getElementById("aiChatFab");
        const panel = document.getElementById("aiChatPanel");
        const closeButton = document.getElementById("aiChatClose");
        const form = document.getElementById("aiChatForm");
        const input = document.getElementById("aiChatInput");
        const messages = document.getElementById("aiChatMessages");
        const status = document.getElementById("aiChatStatus");
        const submit = document.getElementById("aiChatSubmit");

        if (!fab || !panel || !closeButton || !form || !input || !messages || !status || !submit) {
            return;
        }

        fab.addEventListener("click", async function () {
            const shouldOpen = !panel.classList.contains("is-open");
            panel.classList.toggle("is-open", shouldOpen);
            panel.setAttribute("aria-hidden", String(!shouldOpen));

            if (shouldOpen) {
                if (!widget.dataset.sessionId) {
                    await createSession(widget, status);
                }
                input.focus();
            }
        });

        closeButton.addEventListener("click", function () {
            panel.classList.remove("is-open");
            panel.setAttribute("aria-hidden", "true");
        });

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const content = input.value.trim();
            if (!content) {
                return;
            }

            if (!widget.dataset.sessionId) {
                await createSession(widget, status);
                if (!widget.dataset.sessionId) {
                    return;
                }
            }

            appendMessage(messages, "user", content);
            input.value = "";
            setPendingState(submit, status, true, "Waiting for AI response...");

            try {
                const response = await fetch("/ai/chat", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        sessionId: Number(widget.dataset.sessionId),
                        content: content
                    })
                });

                if (!response.ok) {
                    throw new Error("AI chat request failed");
                }

                const data = await response.json();
                if (!data.success) {
                    throw new Error(data.message || "AI chat request failed");
                }
                appendMessage(messages, "assistant", data.answer || "No answer received.");
                setPendingState(submit, status, false, "");
            } catch (error) {
                appendMessage(messages, "assistant", error.message || "An error occurred while contacting the AI endpoint.");
                setPendingState(submit, status, false, "Request failed.");
            }
        });
    }

    async function createSession(widget, status) {
        try {
            status.textContent = "Creating chat session...";
            const response = await fetch("/ai/session", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({})
            });

            if (!response.ok) {
                throw new Error("Session creation failed");
            }

            const data = await response.json();
            widget.dataset.sessionId = String(data.sessionId || "");
            status.textContent = "";
        } catch (error) {
            status.textContent = "Unable to create session.";
        }
    }

    function appendMessage(container, role, content) {
        const article = document.createElement("article");
        article.className = "ai-chat-message ai-chat-message-" + role;

        const paragraph = document.createElement("p");
        paragraph.textContent = content;

        article.appendChild(paragraph);
        container.appendChild(article);
        container.scrollTop = container.scrollHeight;
    }

    function setPendingState(submit, status, isPending, message) {
        submit.disabled = isPending;
        status.textContent = message;
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initAiQuestion);
    } else {
        initAiQuestion();
    }
})();
