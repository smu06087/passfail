// sse-manager.js

import { sseHandlers } from './sseHandlers.js';

const SSEManager = {
    eventSource: null,
    init() {
        this.eventSource = new EventSource(`/sseapi/subscribe`);
		
		console.log(`[SSEManager] init `);
		
        this.eventSource.onopen = () => {
            console.log("SSE Connected");
        };

        this.eventSource.onerror = () => {
            console.log("SSE Reconnecting...");
        };

        //add event lestener from handler.js 
        Object.entries(sseHandlers).forEach(([eventName, config]) => {
            this.eventSource.addEventListener(eventName, (e) => {

                // ignorePages에 현재 페이지path가 포함되어 있다면 return
                const currentPath = window.location.pathname;
                if (config.ignorePages?.includes(currentPath)) {
                    console.log(`[SSEManager] ${eventName} is ignore in this page`);
                    return;
                }

                config.fn(e);
            });
        });
    },

    close() {
        if (this.eventSource) {
            this.eventSource.close();
			this.eventSource = null;
        }
    }
};

window.addEventListener("beforeunload", () => {
	///alert("beforeunload");
    //SSEManager.close();
});

//자동 시작 
// SSEManager.init();

export default SSEManager;
