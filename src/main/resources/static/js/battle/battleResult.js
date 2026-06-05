/**
 * BattleResult UI Module
 * Handles individual and total result displays.
 */
export const BattleResult = {
    modalId: 'battle-result-modal',
    
    init() {
        // Any specific initialization if needed
    },

    showIndividual(data) {
        const modal = document.getElementById(this.modalId);
        if (!modal) return;

        document.getElementById('individual-result-view').style.display = 'block';
        document.getElementById('total-result-view').style.display = 'none';
        
        document.getElementById('my-rank').innerText = data.rank;
        document.getElementById('total-count').innerText = data.totalParticipants;
        document.getElementById('my-score').innerText = data.score;
        
        const finisherList = document.getElementById('finisher-list');
        finisherList.innerHTML = '';
        
        if (data.finishers && data.finishers.length > 0) {
            data.finishers.forEach((f, idx) => {
                const li = document.createElement('li');
                li.className = 'finisher-item';
                li.innerHTML = `
                    <span class="f-rank">${idx + 1}</span>
                    <span class="f-name">${f.username}</span>
                    <span class="f-score">${f.score}</span>
                `;
                finisherList.appendChild(li);
            });
        }

        modal.style.display = 'flex';
    },

    showTotal(results) {
        const modal = document.getElementById(this.modalId);
        if (!modal) return;

        document.getElementById('individual-result-view').style.display = 'none';
        document.getElementById('total-result-view').style.display = 'block';
        document.getElementById('result-title').innerText = '최종 대결 결과';
        document.getElementById('wait-msg').innerText = '모든 대결이 종료되었습니다.';
        document.getElementById('result-stay-btn').style.display = 'none'; // 더이상 관전할 필요 없음

        const tbody = document.getElementById('total-rank-body');
        tbody.innerHTML = '';
        
        results.forEach(res => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${res.rank}</td>
                <td>${res.username}</td>
                <td>${res.score}</td>
            `;
            tbody.appendChild(tr);
        });

        modal.style.display = 'flex';
    },

    hide() {
        const modal = document.getElementById(this.modalId);
        if (modal) modal.style.display = 'none';
    }
};

window.BattleResult = BattleResult;
