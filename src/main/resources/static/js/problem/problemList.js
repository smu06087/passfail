function initPassfailTopbar() {
    const navItems = [
        { title: "문제", subs: ["최신 문제", "인기 문제", "기초 다지기", "심화 챌린지"] },
        { title: "게시판", subs: ["커뮤니티", "질문답변", "자유게시판", "공지사항"] },
        { title: "대회", subs: ["현재 진행", "참가 요청", "결과 조회"] },
        { title: "랭킹", subs: ["실시간 순위", "명예의 전당", "그룹 랭킹"] }
    ];

    const navCont = document.getElementById("nav-loop-container");
    const loginButton = document.getElementById("login-button");
    const memberMenu = document.getElementById("member-menu");

    if (loginButton && memberMenu) {
        loginButton.addEventListener("click", function() {
            loginButton.classList.add("hidden");
            memberMenu.classList.remove("hidden");
            memberMenu.classList.add("flex");
        });
    }

    if (navCont) {
        navItems.forEach(function(item) {
            const div = document.createElement("div");
            div.className = "nav-item";
            div.innerHTML = `<div class="nav-box">${item.title}</div>
                <div class="dropdown">${item.subs.map(function(sub) {
                    return `<div class="dropdown-item">${sub}</div>`;
                }).join("")}</div>`;
            navCont.appendChild(div);
        });
    }

    if (window.lucide) {
        lucide.createIcons();
    }
}

let currentProblemId = "";
let currentSearchKeywords = [];
let searchDebounceTimer = null;

function selectProblem(card) {
    document.querySelectorAll(".problem-card").forEach(function(item) {
        item.classList.remove("active");
    });
    card.classList.add("active");

    currentProblemId = card.dataset.id || "";

    setText("detailTitle", card.dataset.title || "-");
    setText("detailId", card.dataset.id || "-");
    setText("detailTime", card.dataset.time || "-");
    setText("detailMemory", card.dataset.memory || "-");
    setText("detailRate", card.dataset.rate || "0%");
    setText("detailRateRight", card.dataset.rate || "0%");
    setText("detailSubmission", card.dataset.submission || "0");
    setText("detailCorrect", card.dataset.correct || "0");
    setText("detailMySubmit", card.dataset.mySubmit || "-");
    setText("detailMyRate", card.dataset.myRate || "-");
    setText("detailDesc", card.dataset.desc || "-");
    updateAdminEditLink(card.dataset.editUrl || "");

    const difficulty = card.dataset.difficulty || "EASY";
    const difficultyEl = document.getElementById("detailDifficulty");
    if (difficultyEl) {
        difficultyEl.textContent = difficulty;
        difficultyEl.className = "difficulty-chip";
        difficultyEl.classList.add(difficulty.toLowerCase());
    }

    const tagWrap = document.getElementById("detailCategoryTags");
    if (!tagWrap) {
        return;
    }

    tagWrap.innerHTML = "";
    const category = card.dataset.category || "-";
    if (category && category !== "-") {
        category.split(",").forEach(function(tag) {
            const span = document.createElement("span");
            span.className = "tag-chip";
            span.textContent = tag.trim();
            tagWrap.appendChild(span);
        });
    } else {
        const span = document.createElement("span");
        span.className = "tag-chip";
        span.textContent = "미분류";
        tagWrap.appendChild(span);
    }
}

function updateAdminEditLink(editUrl) {
    [document.getElementById("detailEditLink"), document.getElementById("detailEditButton")]
        .filter(Boolean)
        .forEach(function(link) {
            if (!editUrl || currentProblemId === "-") {
                link.classList.add("is-disabled");
                link.setAttribute("aria-disabled", "true");
                link.removeAttribute("href");
                return;
            }

            link.classList.remove("is-disabled");
            link.removeAttribute("aria-disabled");
            link.setAttribute("href", editUrl);
        });
}

function enterProblem() {
    if (!currentProblemId || currentProblemId === "-") {
        return;
    }
    location.href = '/problem/' + currentProblemId;
}

function syncFilterGroup(group) {
    const checkboxes = Array.from(group.querySelectorAll(".check-item input[type='checkbox']"));
    if (checkboxes.length < 2) {
        return;
    }

    const allCheckbox = checkboxes[0];
    const itemCheckboxes = checkboxes.slice(1);
    const checkedCount = itemCheckboxes.filter(function(checkbox) {
        return checkbox.checked;
    }).length;

    allCheckbox.checked = checkedCount === itemCheckboxes.length;
    allCheckbox.indeterminate = checkedCount > 0 && checkedCount < itemCheckboxes.length;
}

function initFilterGroups() {
    document.querySelectorAll(".filter-group").forEach(function(group) {
        const checkboxes = Array.from(group.querySelectorAll(".check-item input[type='checkbox']"));
        if (checkboxes.length < 2) {
            return;
        }

        const allCheckbox = checkboxes[0];
        const itemCheckboxes = checkboxes.slice(1);

        allCheckbox.addEventListener("change", function() {
            itemCheckboxes.forEach(function(checkbox) {
                checkbox.checked = allCheckbox.checked;
                checkbox.indeterminate = false;
            });
            allCheckbox.indeterminate = false;
        });

        itemCheckboxes.forEach(function(checkbox) {
            checkbox.addEventListener("change", function() {
                syncFilterGroup(group);
            });
        });

        syncFilterGroup(group);
    });
}

function resetFilters() {
    document.querySelectorAll(".filter-group").forEach(function(group) {
        const checkboxes = Array.from(group.querySelectorAll(".check-item input[type='checkbox']"));
        if (checkboxes.length > 0) {
            checkboxes.forEach(function(checkbox) {
                checkbox.checked = true;
                checkbox.indeterminate = false;
            });
            syncFilterGroup(group);
        }

        group.querySelectorAll("select").forEach(function(select) {
            select.selectedIndex = 0;
        });
    });

    const rateInputs = document.querySelectorAll(".rate-row input[type='text']");
    if (rateInputs.length >= 2) {
        rateInputs[0].value = "0";
        rateInputs[1].value = "100";
    }

    document.querySelectorAll(".filter-group input[type='text']").forEach(function(input) {
        if (!input.closest(".rate-row")) {
            input.value = "";
        }
    });
}

function initFilterReset() {
    const resetButton = document.querySelector(".reset-btn");
    if (!resetButton) {
        return;
    }

    resetButton.addEventListener("click", function() {
        resetFilters();
        document.dispatchEvent(new CustomEvent("problemFiltersChanged"));
    });
}

function initViewSwitch() {
    const problemGrid = document.getElementById("problemGrid");
    const viewButtons = Array.from(document.querySelectorAll(".view-btn"));

    viewButtons.forEach(function(button, index) {
        button.addEventListener("click", function() {
            viewButtons.forEach(function(item) {
                item.classList.remove("active");
            });
            button.classList.add("active");

            if (index === 1) {
                problemGrid.classList.add("list-view");
            } else {
                problemGrid.classList.remove("list-view");
            }
        });
    });
}

function getPageSizeFromSelect(select) {
    const selectedText = select.options[select.selectedIndex].textContent;
    const numberMatch = selectedText.match(/\d+/);
    return numberMatch ? Number(numberMatch[0]) : 8;
}

function initPageSizeControl() {
    const problemGrid = document.getElementById("problemGrid");
    const pageSizeSelect = document.querySelector(".page-size select");
    const pagination = document.querySelector(".pagination");
    const difficultyTabs = Array.from(document.querySelectorAll(".tabs-row .tab-btn"));
    const tabDifficulties = ["", "EASY", "MEDIUM", "HARD"];
    const applyButton = document.querySelector(".apply-btn");
    const difficultyFilterGroup = document.querySelectorAll(".filter-group")[0];
    const categoryFilterSelect = document.querySelectorAll(".filter-group select")[0];
    const statusFilterGroup = document.querySelectorAll(".filter-group")[2];
    const rateInputs = Array.from(document.querySelectorAll(".rate-row input[type='text']"));
    const tagInput = Array.from(document.querySelectorAll(".filter-group input[type='text']")).find(function(input) {
        return !input.closest(".rate-row");
    });
    const categoryValues = ["", "구현", "정렬", "문자열", "그래프"];
    const emptyResults = document.createElement("div");
    let currentPage = 1;
    let selectedDifficulty = "";

    if (!problemGrid || !pageSizeSelect || !pagination) {
        return;
    }

    emptyResults.className = "empty-results";
    emptyResults.textContent = "조건에 맞는 문제가 없습니다.";
    problemGrid.insertAdjacentElement("afterend", emptyResults);

    function getCards() {
        return Array.from(problemGrid.querySelectorAll(".problem-card"));
    }

    function getSidebarFilterState() {
        const difficultyInputs = difficultyFilterGroup
            ? Array.from(difficultyFilterGroup.querySelectorAll(".check-item input[type='checkbox']")).slice(1)
            : [];
        const selectedDifficulties = ["EASY", "MEDIUM", "HARD"].filter(function(difficulty, index) {
            return difficultyInputs[index] && difficultyInputs[index].checked;
        });

        const statusInputs = statusFilterGroup
            ? Array.from(statusFilterGroup.querySelectorAll(".check-item input[type='checkbox']")).slice(1)
            : [];
        const selectedStatuses = ["UNSOLVED", "SOLVED", "FAVORITE"].filter(function(status, index) {
            return statusInputs[index] && statusInputs[index].checked;
        });

        const minRate = rateInputs[0] ? Number(rateInputs[0].value) : 0;
        const maxRate = rateInputs[1] ? Number(rateInputs[1].value) : 100;

        return {
            difficulties: selectedDifficulties,
            statuses: selectedStatuses,
            category: categoryFilterSelect ? categoryValues[categoryFilterSelect.selectedIndex] || "" : "",
            minRate: Number.isFinite(minRate) ? minRate : 0,
            maxRate: Number.isFinite(maxRate) ? maxRate : 100,
            keyword: tagInput ? tagInput.value.trim().toLowerCase() : ""
        };
    }

    function getCardRate(card) {
        const rate = Number((card.dataset.rate || "").replace("%", ""));
        return Number.isFinite(rate) ? rate : null;
    }

    function getCardStatus(card) {
        const mySubmit = card.dataset.mySubmit || "";
        const isFavorite = Boolean(card.querySelector(".card-star.open"));
        const isSolved = mySubmit !== "" && mySubmit !== "-" && mySubmit !== "없음";
        const statuses = [];

        if (!isSolved) {
            statuses.push("UNSOLVED");
        }
        if (isSolved) {
            statuses.push("SOLVED");
        }
        if (isFavorite) {
            statuses.push("FAVORITE");
        }

        return statuses;
    }

    function matchesSidebarFilters(card) {
        const filterState = getSidebarFilterState();
        const cardDifficulty = card.dataset.difficulty || "";
        const cardCategory = card.dataset.category || "";
        const cardRate = getCardRate(card);
        const cardStatuses = getCardStatus(card);

        if (filterState.difficulties.length === 0 || !filterState.difficulties.includes(cardDifficulty)) {
            return false;
        }

        if (filterState.category && !cardCategory.split(",").map(function(category) {
            return category.trim();
        }).includes(filterState.category)) {
            return false;
        }

        if (filterState.statuses.length === 0 || !cardStatuses.some(function(status) {
            return filterState.statuses.includes(status);
        })) {
            return false;
        }

        if (cardRate === null && (filterState.minRate > 0 || filterState.maxRate < 100)) {
            return false;
        }

        if (cardRate !== null && (cardRate < filterState.minRate || cardRate > filterState.maxRate)) {
            return false;
        }

        if (filterState.keyword) {
            const searchableText = [
                card.dataset.title,
                card.dataset.category,
                card.dataset.desc
            ].join(" ").toLowerCase();

            if (!searchableText.includes(filterState.keyword)) {
                return false;
            }
        }

        return true;
    }

    function updatePagination(totalPages) {
        pagination.innerHTML = "";

        const prevButton = document.createElement("button");
        prevButton.className = "page-btn";
        prevButton.textContent = "<";
        prevButton.disabled = currentPage === 1;
        prevButton.addEventListener("click", function() {
            if (currentPage > 1) {
                currentPage -= 1;
                renderPage();
            }
        });
        pagination.appendChild(prevButton);

        for (let page = 1; page <= totalPages; page += 1) {
            const pageButton = document.createElement("button");
            pageButton.className = "page-btn";
            pageButton.textContent = page;
            if (page === currentPage) {
                pageButton.classList.add("active");
            }
            pageButton.addEventListener("click", function() {
                currentPage = page;
                renderPage();
            });
            pagination.appendChild(pageButton);
        }

        const nextButton = document.createElement("button");
        nextButton.className = "page-btn";
        nextButton.textContent = ">";
        nextButton.disabled = currentPage === totalPages;
        nextButton.addEventListener("click", function() {
            if (currentPage < totalPages) {
                currentPage += 1;
                renderPage();
            }
        });
        pagination.appendChild(nextButton);
    }

    function renderPage() {
        const cards = getCards();
        const filteredCards = selectedDifficulty
            ? cards.filter(function(card) {
                return card.dataset.difficulty === selectedDifficulty && matchesSidebarFilters(card);
            })
            : cards.filter(matchesSidebarFilters);
        const pageSize = getPageSizeFromSelect(pageSizeSelect);
        const totalPages = Math.max(1, Math.ceil(filteredCards.length / pageSize));
        currentPage = Math.min(currentPage, totalPages);

        const startIndex = (currentPage - 1) * pageSize;
        const endIndex = startIndex + pageSize;
        const visibleCards = filteredCards.slice(startIndex, endIndex);
        emptyResults.classList.toggle("is-visible", filteredCards.length === 0);

        cards.forEach(function(card) {
            card.classList.toggle("is-hidden", !visibleCards.includes(card));
        });

        const activeCard = problemGrid.querySelector(".problem-card.active");
        if (!activeCard || activeCard.classList.contains("is-hidden")) {
            const firstVisibleCard = visibleCards[0];
            if (firstVisibleCard) {
                selectProblem(firstVisibleCard);
            } else if (activeCard) {
                activeCard.classList.remove("active");
            }
        }

        updatePagination(totalPages);
    }

    pageSizeSelect.addEventListener("change", function() {
        currentPage = 1;
        renderPage();
    });

    difficultyTabs.forEach(function(tab, index) {
        tab.addEventListener("click", function() {
            difficultyTabs.forEach(function(item) {
                item.classList.remove("active");
            });
            tab.classList.add("active");
            selectedDifficulty = tabDifficulties[index] || "";
            currentPage = 1;
            renderPage();
        });
    });

    if (applyButton) {
        applyButton.addEventListener("click", function() {
            currentPage = 1;
            renderPage();
        });
    }

    document.addEventListener("problemFiltersChanged", function() {
        currentPage = 1;
        renderPage();
    });

    document.addEventListener("problemCardsUpdated", function() {
        currentPage = 1;
        renderPage();
    });

    renderPage();
}

function initSearch() {
    const searchInput = document.getElementById("problemSearchInput");
    const relatedBox = document.getElementById("searchRelatedBox");
    const relatedKeywords = document.getElementById("searchRelatedKeywords");

    if (!searchInput || !relatedBox || !relatedKeywords) {
        return;
    }

    function renderRelatedKeywords(keywords) {
        relatedKeywords.innerHTML = "";
        relatedBox.classList.toggle("is-visible", keywords.length > 0);

        keywords.forEach(function(keyword) {
            const chip = document.createElement("button");
            chip.type = "button";
            chip.className = "search-related-chip";
            chip.textContent = keyword;
            chip.addEventListener("click", function() {
                searchInput.value = keyword;
                runSearch(keyword);
            });
            relatedKeywords.appendChild(chip);
        });
    }

    async function runSearch(query) {
        try {
            const response = await fetch(`/problem/api/problems/search?q=${encodeURIComponent(query)}`);
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.message || "검색에 실패했습니다.");
            }

            currentSearchKeywords = Array.isArray(result.keywords) ? result.keywords : [];
            renderRelatedKeywords(currentSearchKeywords);
            updateProblemCount(result.problemCount || 0);
            replaceProblemCards(Array.isArray(result.problems) ? result.problems : []);
            document.dispatchEvent(new CustomEvent("problemCardsUpdated"));
        } catch (error) {
            console.error(error);
        }
    }

    searchInput.addEventListener("input", function() {
        const query = searchInput.value.trim();
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(function() {
            runSearch(query);
        }, 250);
    });
}

function updateProblemCount(problemCount) {
    const countBadge = document.getElementById("problemCountBadge");
    if (countBadge) {
        countBadge.textContent = `총 ${problemCount}개`;
    }
}

function replaceProblemCards(problems) {
    const problemGrid = document.getElementById("problemGrid");
    if (!problemGrid) {
        return;
    }

    problemGrid.innerHTML = "";

    if (!problems.length) {
        const emptyCard = document.createElement("div");
        emptyCard.className = "problem-card active";
        emptyCard.dataset.id = "-";
        emptyCard.dataset.title = "등록된 문제가 없습니다";
        emptyCard.dataset.difficulty = "EASY";
        emptyCard.dataset.category = "-";
        emptyCard.dataset.time = "-";
        emptyCard.dataset.memory = "-";
        emptyCard.dataset.rate = "0%";
        emptyCard.dataset.submission = "0";
        emptyCard.dataset.correct = "0";
        emptyCard.dataset.mySubmit = "-";
        emptyCard.dataset.myRate = "-";
        emptyCard.dataset.desc = "검색 결과가 없습니다.";
        emptyCard.innerHTML = `
            <div class="problem-number">-</div>
            <div class="card-star-wrap"><div class="card-star gray">☆</div></div>
            <div class="card-title">검색 결과가 없습니다</div>
            <div class="mini-difficulty easy">EASY</div>
        `;
        problemGrid.appendChild(emptyCard);
        selectProblem(emptyCard);
        return;
    }

    problems.forEach(function(problem, index) {
        const card = document.createElement("div");
        card.className = "problem-card" + (index === 0 ? " active" : "");
        card.dataset.id = String(problem.problemId || "");
        card.dataset.title = problem.title || "";
        card.dataset.difficulty = problem.difficulty || "EASY";
        card.dataset.category = problem.category || "-";
        card.dataset.time = `${problem.timeLimitMs || "-"} ms`;
        card.dataset.memory = `${problem.memoryLimitMb || "-"} MB`;
        card.dataset.rate = `${problem.acceptanceRate ?? 0}%`;
        card.dataset.submission = String(problem.submissionCount ?? 0);
        card.dataset.correct = String(problem.acceptedCount ?? 0);
        card.dataset.mySubmit = "-";
        card.dataset.myRate = "-";
        card.dataset.desc = problem.description || "";
        card.dataset.editUrl = `/problem/problemEdit/${problem.problemId}`;
        card.onclick = function() {
            selectProblem(card);
        };
        card.innerHTML = `
            <div class="problem-number">${problem.problemId || ""}</div>
            <div class="card-star-wrap"><div class="card-star gray">☆</div></div>
            <div class="card-title">${escapeHtml(problem.title || "")}</div>
            <div class="mini-difficulty ${String(problem.difficulty || "EASY").toLowerCase()}">${problem.difficulty || "EASY"}</div>
        `;
        problemGrid.appendChild(card);
    });

    const firstCard = problemGrid.querySelector(".problem-card");
    if (firstCard) {
        selectProblem(firstCard);
    }
}

function setText(id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = value;
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

document.addEventListener("DOMContentLoaded", function() {
    initPassfailTopbar();
    initFilterGroups();
    initFilterReset();
    initViewSwitch();
    initPageSizeControl();
    initSearch();

    const firstCard = document.querySelector(".problem-card.active") || document.querySelector(".problem-card");
    if (firstCard) {
        selectProblem(firstCard);
    }
});
