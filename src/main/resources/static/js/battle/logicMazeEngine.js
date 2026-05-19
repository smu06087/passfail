/**
 * Logic Maze 시뮬레이션 엔진 - 난이도별 스마트 생성기 및 코스트 시스템
 */
export class MazeEngine {
    constructor(canvasId, roomId, memberId, seed, difficulty = 'EASY') {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');
        this.roomId = roomId;
        this.memberId = memberId;
        this.seed = seed || 123;
        this.difficulty = difficulty.toUpperCase();
        this.gridSize = 50;
        this.isAnimating = false;
        this.commandBuffer = []; 
        this.playerPositions = {}; 
        
        // 코스트 시스템
        this.currentCost = 0;
        this.staticCost = 0;

        // 난이도 설정 (안개 제거, 코스트 중심)
        this.mapWidth = (this.difficulty === 'EASY' || this.difficulty === 'MEDIUM') ? 7 : 10;
        this.mapHeight = this.mapWidth;

        this.initSmartMap();
    }

    initSmartMap() {
        const random = (s) => {
            let t = s += 0x6D2B79F5;
            t = Math.imul(t ^ t >>> 15, t | 1);
            t ^= t + Math.imul(t ^ t >>> 7, t | 61);
            return ((t ^ t >>> 14) >>> 0) / 4294967296;
        };
        const rnd = () => random(this.seed++);

        this.map = {
            width: this.mapWidth, height: this.mapHeight,
            walls: [], start: {x: 0, y: 0}, exit: {x: this.mapWidth-1, y: this.mapHeight-1},
            switches: [], doors: [], items: []
        };

        const wallProb = (this.difficulty === 'EASY') ? 0.05 : (this.difficulty === 'MEDIUM') ? 0.15 : 0.25;
        for (let i = 0; i < this.mapWidth; i++) {
            for (let j = 0; j < this.mapHeight; j++) {
                if ((i === 0 && j === 0) || (i === this.mapWidth-1 && j === this.mapHeight-1)) continue;
                if (rnd() < wallProb) this.map.walls.push([i, j]);
            }
        }

        while (!this.checkPath(this.map.start, this.map.exit, true)) {
            if (this.map.walls.length === 0) break;
            this.map.walls.splice(Math.floor(rnd() * this.map.walls.length), 1);
        }

        if (this.difficulty !== 'EASY') {
            const doorPos = {x: this.mapWidth-2, y: this.mapHeight-1};
            this.map.walls = this.map.walls.filter(([wx, wy]) => !(wx === doorPos.x && wy === doorPos.y));
            this.map.doors.push({x: doorPos.x, y: doorPos.y, id: 'd1', locked: true, switchId: 's1'});

            const reachable = this.getReachableTiles(this.map.start, doorPos);
            const switchPos = reachable[Math.floor(rnd() * reachable.length)];
            this.map.switches.push({x: switchPos.x, y: switchPos.y, id: 's1', active: false});
        }

        if (this.difficulty === 'VERY_HARD') {
            const reachable = this.getReachableTiles(this.map.start, this.map.exit);
            const itemValues = [10, 30, 20].sort(() => rnd() - 0.5); 
            for (let val of itemValues) {
                if (reachable.length === 0) break;
                const pos = reachable.splice(Math.floor(rnd() * reachable.length), 1)[0];
                this.map.items.push({x: pos.x, y: pos.y, value: val, collected: false});
            }
            this.map.doors.push({x: this.mapWidth-1, y: this.mapHeight-2, id: 'item-door', locked: true});
        }

        this.resetState();
    }

    checkPath(start, end, ignoreDoors = false) {
        const queue = [start];
        const visited = new Set([`${start.x},${start.y}`]);
        const walls = new Set(this.map.walls.map(w => `${w[0]},${w[1]}`));
        const doors = ignoreDoors ? new Set() : new Set(this.map.doors.map(d => `${d.x},${d.y}`));
        while (queue.length > 0) {
            const {x, y} = queue.shift();
            if (x === end.x && y === end.y) return true;
            const dirs = [[1,0],[-1,0],[0,1],[0,-1]];
            for (const [dx, dy] of dirs) {
                const nx = x + dx, ny = y + dy;
                const key = `${nx},${ny}`;
                if (nx >= 0 && nx < this.mapWidth && ny >= 0 && ny < this.mapHeight && !visited.has(key) && !walls.has(key) && !doors.has(key)) {
                    visited.add(key); queue.push({x: nx, y: ny});
                }
            }
        }
        return false;
    }

    getReachableTiles(start, blockedPos) {
        const queue = [start];
        const visited = new Set([`${start.x},${start.y}`]);
        const tiles = [];
        const walls = new Set(this.map.walls.map(w => `${w[0]},${w[1]}`));
        const block = `${blockedPos.x},${blockedPos.y}`;
        while (queue.length > 0) {
            const curr = queue.shift();
            tiles.push(curr);
            const dirs = [[1,0],[-1,0],[0,1],[0,-1]];
            for (const [dx, dy] of dirs) {
                const nx = curr.x + dx, ny = curr.y + dy;
                const key = `${nx},${ny}`;
                if (nx >= 0 && nx < this.mapWidth && ny >= 0 && ny < this.mapHeight && !visited.has(key) && !walls.has(key) && key !== block) {
                    visited.add(key); queue.push({x: nx, y: ny});
                }
            }
        }
        return tiles;
    }

    resetState() {
        this.robot = { ...this.map.start, dir: 0 };
        this.switches = this.map.switches.map(s => ({...s}));
        this.doors = this.map.doors.map(d => ({...d}));
        this.items = this.map.items.map(i => ({...i}));
        this.collectedOrder = [];
        this.isAnimating = false;
        this.commandBuffer = [];
        this.currentCost = 0;
        this.playerPositions[this.memberId] = { ...this.robot, isCleared: false, cost: 0 };
        const console = document.getElementById('debug-console');
        if (console) console.innerHTML = `<div class="log-entry system">[SYSTEM] 최적화 미션 (${this.difficulty}): 최소 에너지를 사용하세요.</div>`;
        this.draw();
    }

    markVisited(x, y) {
        // 안개 기능은 제거되었으나 로직 일관성을 위해 유지 (또는 제거 가능)
    }

    draw() {
        const {ctx, gridSize, map} = this;
        if (!ctx) return;
        this.canvas.width = map.width * gridSize;
        this.canvas.height = map.height * gridSize;
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // Grid
        ctx.strokeStyle = '#0f172a';
        for(let i=0; i<map.width; i++) {
            for(let j=0; j<map.height; j++) { ctx.strokeRect(i*gridSize, j*gridSize, gridSize, gridSize); }
        }
        ctx.strokeStyle = '#334155'; ctx.lineWidth = 2; ctx.strokeRect(0, 0, map.width*gridSize, map.height*gridSize);

        // Walls
        for(let i=0; i<map.width; i++) {
            for(let j=0; j<map.height; j++) {
                if(map.walls.some(([wx, wy]) => wx===i && wy===j)) {
                    ctx.fillStyle = '#334155'; ctx.fillRect(i*gridSize+2, j*gridSize+2, gridSize-4, gridSize-4);
                }
            }
        }

        // Items
        this.items.forEach(item => {
            if (item.collected) return;
            ctx.fillStyle = '#fbbf24'; ctx.beginPath(); ctx.arc(item.x*gridSize+gridSize/2, item.y*gridSize+gridSize/2, 12, 0, Math.PI*2); ctx.fill();
            ctx.fillStyle = 'black'; ctx.font = 'bold 12px Arial'; ctx.textAlign = 'center'; ctx.fillText(item.value, item.x*gridSize+gridSize/2, item.y*gridSize+gridSize/2+5);
        });

        // Switches & Doors
        this.switches.forEach(s => {
            ctx.fillStyle = s.active ? '#22c55e' : '#f59e0b'; ctx.beginPath(); ctx.arc(s.x*gridSize+gridSize/2, s.y*gridSize+gridSize/2, 10, 0, Math.PI*2); ctx.fill();
            ctx.fillStyle = 'white'; ctx.font = '10px bold Arial'; ctx.fillText('SW', s.x*gridSize+gridSize/2, s.y*gridSize+gridSize/2+4);
        });
        this.doors.forEach(d => {
            ctx.fillStyle = d.locked ? '#ef4444' : 'rgba(34, 197, 94, 0.2)'; ctx.fillRect(d.x*gridSize+8, d.y*gridSize+8, gridSize-16, gridSize-16);
        });

        // Exit
        ctx.fillStyle = '#10b981'; ctx.fillRect(map.exit.x*gridSize+4, map.exit.y*gridSize+4, gridSize-8, gridSize-8);
        ctx.fillStyle = 'white'; ctx.font = '10px bold Arial'; ctx.fillText('EXIT', map.exit.x*gridSize+gridSize/2, map.exit.y*gridSize+gridSize/2+4);

        // Players & Costs
        Object.entries(this.playerPositions).forEach(([pid, pos]) => {
            this.drawRobot(pos, pid == this.memberId);
            ctx.fillStyle = pid == this.memberId ? '#3b82f6' : '#94a3b8';
            ctx.font = 'bold 10px monospace';
            ctx.fillText(`COST: ${pos.cost || 0}`, pos.x*gridSize+gridSize/2, pos.y*gridSize-5);
        });
    }

    drawRobot(pos, isMe) {
        const {ctx, gridSize} = this;
        ctx.save();
        ctx.translate(pos.x*gridSize+gridSize/2, pos.y*gridSize+gridSize/2);
        ctx.rotate(pos.dir * Math.PI/2);
        ctx.fillStyle = isMe ? '#3b82f6' : 'rgba(148, 163, 184, 0.5)';
        ctx.beginPath(); ctx.moveTo(12, 0); ctx.lineTo(-8, 8); ctx.lineTo(-8, -8); ctx.closePath(); ctx.fill();
        if(isMe) { ctx.strokeStyle = 'white'; ctx.lineWidth = 2; ctx.stroke(); }
        ctx.restore();
    }

    async processCommands(onSuccess) {
        if (this.isAnimating) return;
        this.isAnimating = true;
        while (this.isAnimating) {
            if (this.commandBuffer.length > 0) {
                const cmd = this.commandBuffer.shift();
                await this.executeStep(cmd);
                this.draw();

                // [추가] 출구 도달 즉시 중단 및 성공 처리
                if (this.playerPositions[this.memberId].isCleared) {
                    this.isAnimating = false;
                    if (onSuccess) onSuccess();
                    break;
                }

                await new Promise(r => setTimeout(r, cmd.startsWith('SAY:') ? 500 : 150));
            } else { await new Promise(r => setTimeout(r, 50)); }
        }
    }

    async executeStep(action) {
        const {robot} = this;
        if (action.startsWith('STATIC_COST:')) {
            this.staticCost = parseInt(action.split(':')[1]);
            this.currentCost = this.staticCost;
            this.playerPositions[this.memberId].cost = this.currentCost;
            this.logMessage(`작성 비용: ${this.staticCost}`);
            return;
        }
        if (action.startsWith('COST:')) {
            this.currentCost = this.staticCost + parseInt(action.split(':')[1]);
            this.playerPositions[this.memberId].cost = this.currentCost;
            return;
        }
        if (action.startsWith('SAY:')) {
            const msg = action.replace('SAY:', '');
            this.logMessage(msg); this.showSpeechBubble(msg); return;
        }
        if (action === 'PICKUP') {
            const item = this.items.find(i => i.x === robot.x && i.y === robot.y && !i.collected);
            if (item) {
                item.collected = true; this.collectedOrder.push(item.value);
                const sorted = [...this.collectedOrder].sort((a,b) => a-b);
                if (this.items.every(i => i.collected) && JSON.stringify(this.collectedOrder) === JSON.stringify(sorted)) {
                    this.doors.forEach(d => d.locked = false);
                }
            }
        } else if (action === 'MOVE') {
            let nx = robot.x + (robot.dir === 0 ? 1 : robot.dir === 2 ? -1 : 0);
            let ny = robot.y + (robot.dir === 1 ? 1 : robot.dir === 3 ? -1 : 0);
            if (nx >= 0 && nx < this.mapWidth && ny >= 0 && ny < this.mapHeight) {
                if (!this.map.walls.some(([wx, wy]) => wx === nx && wy === ny) && !this.doors.some(d => d.x === nx && d.y === ny && d.locked)) {
                    robot.x = nx; robot.y = ny;
                }
            }
        } else if (action === 'LEFT') { robot.dir = (robot.dir + 3) % 4; }
        else if (action === 'RIGHT') { robot.dir = (robot.dir + 1) % 4; }
        else if (action === 'USE') {
            this.switches.forEach(s => {
                if (s.x === robot.x && s.y === robot.y) {
                    s.active = !s.active;
                    this.doors.forEach(d => { if(d.switchId === s.id) d.locked = !s.active; });
                }
            });
        }
        
        // 상태 갱신 및 동기화
        this.playerPositions[this.memberId] = { 
            ...this.robot, 
            isCleared: (this.robot.x === this.map.exit.x && this.robot.y === this.map.exit.y),
            cost: this.currentCost 
        };

        const now = Date.now();
        if (!this.lastSyncTime || now - this.lastSyncTime > 200 || this.playerPositions[this.memberId].isCleared) {
            if (window.sendMazePosition) {
                window.sendMazePosition(robot.x, robot.y, robot.dir, this.currentCost, this.playerPositions[this.memberId].isCleared);
            }
            this.lastSyncTime = now;
        }
    }

    logMessage(msg) {
        const console = document.getElementById('debug-console');
        if (!console) return;
        const entry = document.createElement('div');
        entry.className = 'log-entry';
        entry.innerText = `[ROBOT] ${msg}`;
        console.appendChild(entry);
        console.scrollTop = console.scrollHeight;
    }

    showSpeechBubble(msg) {
        const container = document.getElementById('speech-bubble-container');
        if (!container) return;
        const bubble = document.createElement('div');
        bubble.className = 'speech-bubble';
        bubble.innerText = msg;
        const rx = this.robot.x * this.gridSize + this.gridSize / 2;
        const ry = this.robot.y * this.gridSize;
        bubble.style.left = `${this.canvas.offsetLeft + rx - 30}px`;
        bubble.style.top = `${this.canvas.offsetTop + ry - 40}px`;
        container.appendChild(bubble);
        setTimeout(() => { bubble.style.opacity = '0'; setTimeout(() => bubble.remove(), 500); }, 1500);
    }

    serializeMap() {
        let data = `${this.map.width} ${this.map.height}\n`;
        data += `START ${this.map.start.x} ${this.map.start.y}\n`;
        data += `EXIT ${this.map.exit.x} ${this.map.exit.y}\n`;
        this.map.walls.forEach(([wx, wy]) => data += `WALL ${wx} ${wy}\n`);
        this.map.switches.forEach(s => data += `SWITCH ${s.id} ${s.x} ${s.y}\n`);
        this.map.doors.forEach(d => data += `DOOR ${d.id} ${d.x} ${d.y} ${d.switchId}\n`);
        this.map.items.forEach(i => data += `ITEM ${i.value} ${i.x} ${i.y}\n`);
        data += "END\n";
        return data;
    }
}
