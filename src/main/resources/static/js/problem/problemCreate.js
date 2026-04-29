const config = window.problemFormConfig || {
    mode: "create",
    submitUrl: "/problem/api/problems",
    problemListUrl: "/problem/problemList",
    editMethod: "POST",
    initialProblem: null
};

let tagList = [];
let sampleCount = 0;
let testCaseCount = 0;

const titleInput = document.getElementById("title");
const shortDescInput = document.getElementById("shortDesc");
const difficultyInput = document.getElementById("difficulty");
const acceptRateInput = document.getElementById("acceptRate");
const previewTitle = document.getElementById("previewTitle");
const previewText = document.getElementById("previewText");
const previewDifficulty = document.getElementById("previewDifficulty");
const previewRate = document.getElementById("previewRate");
const previewTags = document.getElementById("previewTags");
const categoryInput = document.getElementById("category");
const timeLimitInput = document.getElementById("timeLimit");
const memoryLimitInput = document.getElementById("memoryLimit");
const statusInput = document.getElementById("status");
const descriptionInput = document.getElementById("description");
const modeBadge = document.getElementById("modeBadge");
const submitButton = document.getElementById("submitButton");

titleInput.addEventListener("input", updatePreview);
shortDescInput.addEventListener("input", updatePreview);
difficultyInput.addEventListener("change", updatePreview);
acceptRateInput.addEventListener("input", updatePreview);

function updatePreview() {
    const titleValue = titleInput.value.trim();
    const descValue = shortDescInput.value.trim();
    const difficultyValue = difficultyInput.value;
    const rateValue = normalizeDecimal(acceptRateInput.value, 0);

    previewTitle.textContent = titleValue || "문제 제목을 입력해 주세요.";
    previewText.textContent = descValue || "등록 버튼을 누르기 전에 여기에 내용을 확인할 수 있습니다.";
    previewDifficulty.textContent = difficultyValue;
    previewDifficulty.className = "preview-difficulty " + difficultyValue.toLowerCase();
    previewRate.textContent = "정답률 " + rateValue + "%";
    renderPreviewTags();
}

function addTag() {
    const tagInput = document.getElementById("tagInput");
    const value = tagInput.value.trim();

    if (!value) {
        return;
    }

    value.split(",").map(function (item) {
        return item.trim();
    }).filter(Boolean).forEach(function (item) {
        if (!tagList.includes(item)) {
            tagList.push(item);
        }
    });

    tagInput.value = "";
    renderTags();
    updatePreview();
}

function renderTags() {
    const wrap = document.getElementById("tagList");
    wrap.innerHTML = "";

    tagList.forEach(function (tag) {
        const chip = document.createElement("div");
        chip.className = "tag-chip";
        chip.innerHTML = `<span>${escapeHtml(tag)}</span><button type="button" onclick="removeTag('${escapeJs(tag)}')">x</button>`;
        wrap.appendChild(chip);
    });
}

function renderPreviewTags() {
    previewTags.innerHTML = "";

    if (tagList.length === 0) {
        const emptyTag = document.createElement("span");
        emptyTag.className = "preview-tag";
        emptyTag.textContent = "태그 없음";
        previewTags.appendChild(emptyTag);
        return;
    }

    tagList.forEach(function (tag) {
        const chip = document.createElement("span");
        chip.className = "preview-tag";
        chip.textContent = tag;
        previewTags.appendChild(chip);
    });
}

function removeTag(tag) {
    tagList = tagList.filter(function (item) {
        return item !== tag;
    });
    renderTags();
    updatePreview();
}

function addSample(inputValue, outputValue) {
    sampleCount += 1;
    appendCaseBlock({
        containerId: "sampleContainer",
        wrapperClassName: "sample-wrap",
        titleClassName: "sample-title",
        titleText: "예제 " + sampleCount,
        deleteHandler: "removeSample(this)",
        inputLabel: "예제 입력",
        outputLabel: "예제 출력",
        inputSelector: "data-sample-input",
        outputSelector: "data-sample-output",
        inputPlaceholder: "예: 3 5",
        outputPlaceholder: "예: 8",
        inputValue: inputValue,
        outputValue: outputValue,
        withTopMargin: sampleCount > 1
    });
}

function addTestCase(inputValue, outputValue) {
    testCaseCount += 1;
    appendCaseBlock({
        containerId: "testCaseContainer",
        wrapperClassName: "sample-wrap test-case-wrap",
        titleClassName: "test-case-title",
        titleText: "테스트케이스 " + testCaseCount,
        deleteHandler: "removeTestCase(this)",
        inputLabel: "테스트 입력",
        outputLabel: "기대 출력",
        inputSelector: "data-test-input",
        outputSelector: "data-test-output",
        inputPlaceholder: "숨김 테스트 입력값",
        outputPlaceholder: "숨김 테스트 기대 출력값",
        inputValue: inputValue,
        outputValue: outputValue,
        withTopMargin: testCaseCount > 1
    });
}

function appendCaseBlock(options) {
    const container = document.getElementById(options.containerId);
    const block = document.createElement("div");
    block.className = options.wrapperClassName;
    block.style.marginTop = options.withTopMargin ? "10px" : "0";
    block.innerHTML = `
        <div class="sample-head">
            <div class="${options.titleClassName}">${options.titleText}</div>
            <button type="button" class="sample-delete" onclick="${options.deleteHandler}">삭제</button>
        </div>
        <div class="sample-grid">
            <div class="form-group">
                <label>${options.inputLabel}</label>
                <textarea class="form-textarea" ${options.inputSelector} placeholder="${options.inputPlaceholder}">${escapeHtml(options.inputValue || "")}</textarea>
            </div>
            <div class="form-group">
                <label>${options.outputLabel}</label>
                <textarea class="form-textarea" ${options.outputSelector} placeholder="${options.outputPlaceholder}">${escapeHtml(options.outputValue || "")}</textarea>
            </div>
        </div>
    `;
    container.appendChild(block);
}

function removeSample(button) {
    const allSamples = document.querySelectorAll("#sampleContainer .sample-wrap");
    if (allSamples.length === 1) {
        alert("예제는 최소 1개 이상 필요합니다.");
        return;
    }

    button.closest(".sample-wrap").remove();
    resetSampleTitles();
}

function removeTestCase(button) {
    button.closest(".test-case-wrap").remove();
    resetTestCaseTitles();
}

function resetSampleTitles() {
    const titles = document.querySelectorAll(".sample-title");
    sampleCount = titles.length;
    titles.forEach(function (title, index) {
        title.textContent = "예제 " + (index + 1);
    });
}

function resetTestCaseTitles() {
    const titles = document.querySelectorAll(".test-case-title");
    testCaseCount = titles.length;
    titles.forEach(function (title, index) {
        title.textContent = "테스트케이스 " + (index + 1);
    });
}

function resetForm() {
    if (!confirm(config.mode === "edit" ? "수정 중인 내용을 초기화할까요?" : "입력한 내용을 초기화할까요?")) {
        return;
    }

    fillForm(config.initialProblem);
}

async function saveDraft() {
    statusInput.value = "DRAFT";
    await submitProblemRequest("DRAFT", "문제가 임시 저장되었습니다.");
}

async function submitProblem() {
    const successMessage = config.mode === "edit" ? "문제가 수정되었습니다." : "문제가 등록되었습니다.";
    await submitProblemRequest(statusInput.value, successMessage);
}

async function submitProblemRequest(status, successMessage) {
    const payload = buildPayload(status);

    try {
        const response = await fetch(config.submitUrl, {
            method: config.mode === "edit" ? "PUT" : "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || "문제 저장에 실패했습니다.");
        }

        alert(successMessage + " 문제 번호: " + result.problemId);
        window.location.href = config.problemListUrl;
    } catch (error) {
        alert(error.message);
    }
}

function buildPayload(status) {
    return {
        problemId: config.initialProblem && config.initialProblem.problemId ? config.initialProblem.problemId : null,
        title: titleInput.value.trim(),
        shortDescription: shortDescInput.value.trim(),
        description: descriptionInput.value.trim(),
        difficulty: difficultyInput.value,
        category: categoryInput.value.trim(),
        timeLimitMs: normalizeNumber(timeLimitInput.value, 0),
        memoryLimitMb: normalizeNumber(memoryLimitInput.value, 0),
        status: status,
        acceptanceRate: normalizeDecimal(acceptRateInput.value, 0),
        tags: tagList.slice(),
        sampleInputs: collectValues("[data-sample-input]"),
        sampleOutputs: collectValues("[data-sample-output]"),
        testInputs: collectValues("[data-test-input]"),
        testOutputs: collectValues("[data-test-output]")
    };
}

function collectValues(selector) {
    return Array.from(document.querySelectorAll(selector)).map(function (item) {
        return item.value.trim();
    });
}

function normalizeNumber(value, fallbackValue) {
    const normalized = String(value || "").replace(/[^\d]/g, "");
    if (!normalized) {
        return fallbackValue;
    }
    return Number.parseInt(normalized, 10);
}

function normalizeDecimal(value, fallbackValue) {
    const normalized = String(value || "").replace(/[^\d.]/g, "");
    if (!normalized) {
        return fallbackValue;
    }
    return Number.parseFloat(normalized);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function escapeJs(value) {
    return String(value).replaceAll("\\", "\\\\").replaceAll("'", "\\'");
}

function fillForm(problem) {
    const initialProblem = problem || {};
    titleInput.value = initialProblem.title || "";
    shortDescInput.value = initialProblem.shortDescription || "";
    descriptionInput.value = initialProblem.description || "";
    categoryInput.value = initialProblem.category || "";
    difficultyInput.value = initialProblem.difficulty || "EASY";
    timeLimitInput.value = initialProblem.timeLimitMs || 2000;
    memoryLimitInput.value = initialProblem.memoryLimitMb || 256;
    statusInput.value = initialProblem.status || "DRAFT";
    acceptRateInput.value = initialProblem.acceptanceRate != null ? initialProblem.acceptanceRate : 0;

    tagList = Array.isArray(initialProblem.tags) ? initialProblem.tags.slice() : [];
    renderTags();

    const sampleContainer = document.getElementById("sampleContainer");
    sampleContainer.innerHTML = "";
    sampleCount = 0;

    const testCaseContainer = document.getElementById("testCaseContainer");
    testCaseContainer.innerHTML = "";
    testCaseCount = 0;

    const sampleInputs = Array.isArray(initialProblem.sampleInputs) ? initialProblem.sampleInputs : [];
    const sampleOutputs = Array.isArray(initialProblem.sampleOutputs) ? initialProblem.sampleOutputs : [];
    const maxSampleLength = Math.max(sampleInputs.length, sampleOutputs.length, 1);

    for (let index = 0; index < maxSampleLength; index += 1) {
        addSample(sampleInputs[index] || "", sampleOutputs[index] || "");
    }

    const testInputs = Array.isArray(initialProblem.testInputs) ? initialProblem.testInputs : [];
    const testOutputs = Array.isArray(initialProblem.testOutputs) ? initialProblem.testOutputs : [];
    const maxTestLength = Math.max(testInputs.length, testOutputs.length, 1);

    for (let index = 0; index < maxTestLength; index += 1) {
        addTestCase(testInputs[index] || "", testOutputs[index] || "");
    }

    modeBadge.textContent = config.mode === "edit" ? "수정 모드" : "신규 등록";
    submitButton.textContent = config.mode === "edit" ? "문제 수정" : "문제 등록";
    updatePreview();
}

fillForm(config.initialProblem);
