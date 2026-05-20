let currentProblemId = "";
let currentSearchKeywords = [];
let searchDebounceTimer = null;

function formatDifficultyLabel(value) {
    return String(value || "EASY").toUpperCase() === "MEDIUM" ? "Normal" : String(value || "EASY");
}

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
    setText("detailDesc", card.dataset.desc || "-");
    updateAdminEditLink(card.dataset.editUrl || "");

    const difficulty = card.dataset.difficulty || "EASY";
    const difficultyEl = document.getElementById("detailDifficulty");
    if (difficultyEl) {
        difficultyEl.textContent = formatDifficultyLabel(difficulty);
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
    location.href = "/codingtest/" + currentProblemId;
}

function enterRandomProblem() {
    const visibleCards = Array.from(document.querySelectorAll(".problem-card")).filter(function(card) {
        return !card.classList.contains("is-hidden") && card.dataset.id && card.dataset.id !== "-";
    });

    if (visibleCards.length === 0) {
        return;
    }

    const randomCard = visibleCards[Math.floor(Math.random() * visibleCards.length)];
    selectProblem(randomCard);
    enterProblem();
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

    document.querySelectorAll(".filter-group input[type='text']").forEach(function(input) {
        input.value = "";
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
    const sortSelect = document.getElementById("problemSortSelect");
    const pagination = document.querySelector(".pagination");
    const difficultyTabs = Array.from(document.querySelectorAll(".tabs-row .tab-btn"));
    const tabDifficulties = ["", "EASY", "MEDIUM", "HARD"];
    const applyButton = document.querySelector(".apply-btn");
    const difficultyFilterGroup = document.querySelectorAll(".filter-group")[0];
    const categoryFilterSelect = document.querySelectorAll(".filter-group select")[0];
    const statusFilterGroup = document.querySelectorAll(".filter-group")[2];
    const tagInput = document.querySelector(".filter-group input[type='text']");
    const categoryValues = ["", "구현", "정렬", "문자열", "그래프"];
    const emptyResults = document.createElement("div");
    let currentPage = 1;
    let selectedDifficulty = "";

    if (!problemGrid || !pageSizeSelect || !pagination || !sortSelect) {
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
        const selectedStatuses = ["UNSOLVED", "SOLVED"].filter(function(status, index) {
            return statusInputs[index] && statusInputs[index].checked;
        });

        return {
            difficulties: selectedDifficulties,
            statuses: selectedStatuses,
            category: categoryFilterSelect ? categoryValues[categoryFilterSelect.selectedIndex] || "" : "",
            keyword: tagInput ? tagInput.value.trim().toLowerCase() : ""
        };
    }

    function getCardCreatedAt(card) {
        const createdAt = card.dataset.createdAt || "";
        const timestamp = Date.parse(createdAt);
        return Number.isNaN(timestamp) ? 0 : timestamp;
    }

    function getCardProblemId(card) {
        const problemId = Number(card.dataset.id || 0);
        return Number.isFinite(problemId) ? problemId : 0;
    }

    function sortCards(cards) {
        const sortValue = sortSelect.value || "latest";

        return cards.slice().sort(function(left, right) {
            if (sortValue === "number") {
                return getCardProblemId(left) - getCardProblemId(right);
            }

            const createdAtCompare = getCardCreatedAt(right) - getCardCreatedAt(left);
            if (createdAtCompare !== 0) {
                return createdAtCompare;
            }
            return getCardProblemId(right) - getCardProblemId(left);
        });
    }

    function getCardStatus(card) {
        const isSolved = card.dataset.solved === "true";
        const statuses = [];

        if (!isSolved) {
            statuses.push("UNSOLVED");
        }
        if (isSolved) {
            statuses.push("SOLVED");
        }

        return statuses;
    }

    function matchesSidebarFilters(card) {
        const filterState = getSidebarFilterState();
        const cardDifficulty = card.dataset.difficulty || "";
        const cardCategory = card.dataset.category || "";
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
        const cards = sortCards(getCards());
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
            problemGrid.appendChild(card);
        });

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

    sortSelect.addEventListener("change", function() {
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
        emptyCard.dataset.title = "검색 결과가 없습니다";
        emptyCard.dataset.difficulty = "EASY";
        emptyCard.dataset.category = "-";
        emptyCard.dataset.time = "-";
        emptyCard.dataset.memory = "-";
        emptyCard.dataset.desc = "검색 결과가 없습니다.";
        emptyCard.dataset.solved = "false";
        emptyCard.innerHTML = `
            <div class="problem-number">-</div>
            <div class="card-star-wrap"><div class="card-star gray">*</div></div>
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
        card.dataset.createdAt = problem.createdAt || "";
        card.dataset.desc = problem.description || "";
        card.dataset.editUrl = `/problem/problemEdit/${problem.problemId}`;
        card.dataset.solved = problem.solved ? "true" : "false";
        if (problem.solved) {
            card.classList.add("solved");
        }
        card.onclick = function() {
            selectProblem(card);
        };
        card.innerHTML = `
            <div class="problem-number">${problem.problemId || ""}</div>
            <div class="card-star-wrap"><div class="card-star gray">*</div></div>
            <div class="card-title-row">
                <div class="card-title">${escapeHtml(problem.title || "")}</div>
                ${problem.solved ? '<span class="solved-badge">해결</span>' : ''}
            </div>
            <div class="mini-difficulty ${String(problem.difficulty || "EASY").toLowerCase()}">${escapeHtml(formatDifficultyLabel(problem.difficulty || "EASY"))}</div>
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

async function downloadPdf() {
    if (!currentProblemId || currentProblemId === "-") {
        return;
    }

    if (typeof currentUsername === 'undefined' || currentUsername === null) {
        if (confirm('PDF 다운로드는 로그인이 필요한 서비스입니다. 로그인 하시겠습니까?')) {
            location.href = '/login';
        }
        return;
    }

    if (!confirm('문제를 PDF로 다운로드하시겠습니까? (1,000 바나나 소모)')) {
        return;
    }

    try {
        const response = await fetch(`/problem/${currentProblemId}/download`);
        
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `Problem_${currentProblemId}.pdf`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
        } else {
            const errorData = await response.json();
            alert(errorData.message || '포인트가 부족하거나 다운로드에 실패했습니다.');
        }
    } catch (error) {
        console.error('PDF download error:', error);
        alert('다운로드 중 오류가 발생했습니다.');
    }
}

document.addEventListener("DOMContentLoaded", function() {
    initFilterGroups();
    initFilterReset();
    initViewSwitch();
    initPageSizeControl();
    initSearch();

    const randomProblemButton = document.getElementById("randomProblemButton");
    if (randomProblemButton) {
        randomProblemButton.addEventListener("click", enterRandomProblem);
    }

    const firstCard = document.querySelector(".problem-card.active") || document.querySelector(".problem-card");
    if (firstCard) {
        selectProblem(firstCard);
    }
});
