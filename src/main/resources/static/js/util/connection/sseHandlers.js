// handlers.js
export const sseHandlers = {
    connect: {
        fn(e) {
            console.log("sse 연결 - ", e.data);            
        }
    },
    notice: {
        ignorePages: ['/login.html', '/intro.html'], // 공지를 보여주지 않을 페이지
        fn(e) {
            console.log("📢 공지사항:", e.data);
            alert("공지: " + e.data);
        }
    },
    invite: {
        ignorePages: ['/game_playing.html'], // 게임 중엔 초대 무시
        fn(e) {
            const data = JSON.parse(e.data);

            console.log("💌 초대 도착:", e.data);
            if (confirm("게임 초대 도착! 이동할까요?")) {
                location.href = "/battle/room/join/" + data.roomId;
            }
        }
    },
    ping: {
        fn(e) {
            console.log("heartbeat:", e.data);
        }
    }
};
