const config = window.problemFormConfig || {
    mode: "create",
    submitUrl: "/problem/api/problems",
    deleteUrl: null,
    problemListUrl: "/problem/problemList",
    editMethod: "POST",
    initialProblem: null
};

const normalizedInitialProblem = normalizeInitialProblem(config.initialProblem);

let tagList = [];
let sampleCount = 0;
let testCaseCount = 0;
let isDirty = false;
let isSubmitting = false;
let suppressBeforeUnload = false;

const titleInput = document.getElementById("title");
const difficultyInput = document.getElementById("difficulty");
const previewTitle = document.getElementById("previewTitle");
const previewText = document.getElementById("previewText");
const previewDifficulty = document.getElementById("previewDifficulty");
const previewTags = document.getElementById("previewTags");
const categoryInput = document.getElementById("category");
const timeLimitInput = document.getElementById("timeLimit");
const memoryLimitInput = document.getElementById("memoryLimit");
const statusInput = document.getElementById("status");
const descriptionInput = document.getElementById("description");
const modeBadge = document.getElementById("modeBadge");
const submitButton = document.getElementById("submitButton");

function markDirty() {
    if (!isSubmitting) {
        isDirty = true;
    }
}

function updatePreview() {
    const titleValue = titleInput ? titleInput.value.trim() : "";
    const descValue = descriptionInput ? descriptionInput.value.trim() : "";
    const difficultyValue = difficultyInput ? difficultyInput.value : "EASY";

    if (previewTitle) {
        previewTitle.textContent = titleValue || "문제 제목을 입력해 주세요.";
    }
    if (previewText) {
        previewText.textContent = descValue || "문제 설명을 입력하면 여기에 표시됩니다.";
    }
    if (previewDifficulty) {
        previewDifficulty.textContent = difficultyValue;
        previewDifficulty.className = "preview-difficulty " + difficultyValue.toLowerCase();
    }
    renderPreviewTags();
    markDirty();
}

function addTag() {
    const tagInput = document.getElementById("tagInput");
    if (!tagInput) {
        return;
    }

    const value = tagInput.value.trim();
    if (!value) {
        return;
    }

    value
        .split(",")
        .map(function(item) {
            return item.trim();
        })
        .filter(Boolean)
        .forEach(function(item) {
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
    if (!wrap) {
        return;
    }

    wrap.innerHTML = "";
    tagList.forEach(function(tag) {
        const chip = document.createElement("div");
        chip.className = "tag-chip";
        chip.innerHTML = `<span>${escapeHtml(tag)}</span><button type="button" onclick="removeTag('${escapeJs(tag)}')">x</button>`;
        wrap.appendChild(chip);
    });
}

function renderPreviewTags() {
    if (!previewTags) {
        return;
    }

    previewTags.innerHTML = "";

    if (tagList.length === 0) {
        const emptyTag = document.createElement("span");
        emptyTag.className = "preview-tag";
        emptyTag.textContent = "태그 없음";
        previewTags.appendChild(emptyTag);
        return;
    }

    tagList.forEach(function(tag) {
        const chip = document.createElement("span");
        chip.className = "preview-tag";
        chip.textContent = tag;
        previewTags.appendChild(chip);
    });
}

function removeTag(tag) {
    tagList = tagList.filter(function(item) {
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
    markDirty();
}

function addTestCase(inputValue, outputValue) {
    testCaseCount += 1;
    appendCaseBlock({
        containerId: "testCaseContainer",
        wrapperClassName: "sample-wrap test-case-wrap",
        titleClassName: "test-case-title",
        titleText: "테스트 케이스 " + testCaseCount,
        deleteHandler: "removeTestCase(this)",
        inputLabel: "테스트 입력",
        outputLabel: "기대 출력",
        inputSelector: "data-test-input",
        outputSelector: "data-test-output",
        inputPlaceholder: "채점에 사용할 입력값",
        outputPlaceholder: "채점 시 기대하는 출력값",
        inputValue: inputValue,
        outputValue: outputValue,
        withTopMargin: testCaseCount > 1
    });
    markDirty();
}

function appendCaseBlock(options) {
    const container = document.getElementById(options.containerId);
    if (!container) {
        return;
    }

    const block = document.createElement("div");
    block.className = options.wrapperClassName;
    block.style.marginTop = options.withTopMargin ? "10px" : "0";
    block.innerHTML = `
        <div class="sample-head">
            <div class="${options.titleClassName}">${options.titleText}</div>
            <button type="button" class="sample-delete" aria-label="${options.titleText} 삭제" onclick="${options.deleteHandler}">삭제</button>
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
    markDirty();
}

function removeTestCase(button) {
    button.closest(".test-case-wrap").remove();
    resetTestCaseTitles();
    markDirty();
}

function resetSampleTitles() {
    const titles = document.querySelectorAll(".sample-title");
    sampleCount = titles.length;
    titles.forEach(function(title, index) {
        title.textContent = "예제 " + (index + 1);
    });
}

function resetTestCaseTitles() {
    const titles = document.querySelectorAll(".test-case-title");
    testCaseCount = titles.length;
    titles.forEach(function(title, index) {
        title.textContent = "테스트 케이스 " + (index + 1);
    });
}

function resetForm() {
    const message = config.mode === "edit"
        ? "수정 중인 내용을 초기화할까요?"
        : "입력한 내용을 초기화할까요?";
    if (!confirm(message)) {
        return;
    }

    fillForm(normalizedInitialProblem);
}

async function saveDraft() {
    if (statusInput) {
        statusInput.value = "DRAFT";
    }
    await submitProblemRequest("DRAFT", "임시 저장되었습니다.");
}

async function submitProblem() {
    const successMessage = config.mode === "edit" ? "문제가 수정되었습니다." : "문제가 등록되었습니다.";
    await submitProblemRequest(statusInput ? statusInput.value : "DRAFT", successMessage);
}

async function deleteProblem() {
    if (!config.deleteUrl) {
        return;
    }

    if (!confirm("이 문제를 삭제할까요? 삭제 후 복구할 수 없습니다.")) {
        return;
    }

    try {
        isSubmitting = true;
        const response = await fetch(config.deleteUrl, {
            method: "DELETE"
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || "문제 삭제에 실패했습니다.");
        }

        isDirty = false;
        suppressBeforeUnload = true;
        alert(result.message || "문제가 삭제되었습니다.");
        window.location.href = config.problemListUrl;
    } catch (error) {
        alert(error.message || "문제 삭제 중 오류가 발생했습니다.");
    } finally {
        isSubmitting = false;
    }
}

async function submitProblemRequest(status, successMessage) {
    const payload = buildPayload(status);

    try {
        isSubmitting = true;
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

        isDirty = false;
        suppressBeforeUnload = true;
        alert(successMessage + " 문제 번호: " + result.problemId);
        window.location.href = config.problemListUrl;
    } catch (error) {
        alert(error.message || "문제 저장 중 오류가 발생했습니다.");
    } finally {
        isSubmitting = false;
    }
}

function buildPayload(status) {
    return {
        problemId: normalizedInitialProblem && normalizedInitialProblem.problemId ? normalizedInitialProblem.problemId : null,
        title: titleInput ? titleInput.value.trim() : "",
        description: descriptionInput ? descriptionInput.value.trim() : "",
        difficulty: difficultyInput ? difficultyInput.value : "EASY",
        category: categoryInput ? categoryInput.value.trim() : "",
        timeLimitMs: normalizeNumber(timeLimitInput ? timeLimitInput.value : 0, 0),
        memoryLimitMb: normalizeNumber(memoryLimitInput ? memoryLimitInput.value : 0, 0),
        status: status,
        acceptanceRate: normalizedInitialProblem && normalizedInitialProblem.acceptanceRate != null ? normalizedInitialProblem.acceptanceRate : null,
        tags: tagList.slice(),
        sampleInputs: collectValues("[data-sample-input]"),
        sampleOutputs: collectValues("[data-sample-output]"),
        testInputs: collectValues("[data-test-input]"),
        testOutputs: collectValues("[data-test-output]")
    };
}

function collectValues(selector) {
    return Array.from(document.querySelectorAll(selector)).map(function(item) {
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

function normalizeInitialProblem(problem) {
    if (!problem) {
        return null;
    }

    if (typeof problem === "string") {
        try {
            return JSON.parse(problem);
        } catch (error) {
            console.error("initialProblem JSON parse failed", error);
            return null;
        }
    }

    return problem;
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
    const initialProblem = normalizeInitialProblem(problem) || {};

    if (titleInput) {
        titleInput.value = initialProblem.title || "";
    }
    if (descriptionInput) {
        descriptionInput.value = initialProblem.description || "";
    }
    if (categoryInput) {
        categoryInput.value = initialProblem.category || "";
    }
    if (difficultyInput) {
        difficultyInput.value = initialProblem.difficulty || "EASY";
    }
    if (timeLimitInput) {
        timeLimitInput.value = initialProblem.timeLimitMs || 2000;
    }
    if (memoryLimitInput) {
        memoryLimitInput.value = initialProblem.memoryLimitMb || 256;
    }
    if (statusInput) {
        statusInput.value = initialProblem.status || "DRAFT";
    }
    tagList = Array.isArray(initialProblem.tags) ? initialProblem.tags.slice() : [];
    renderTags();

    const sampleContainer = document.getElementById("sampleContainer");
    if (sampleContainer) {
        sampleContainer.innerHTML = "";
    }
    sampleCount = 0;

    const testCaseContainer = document.getElementById("testCaseContainer");
    if (testCaseContainer) {
        testCaseContainer.innerHTML = "";
    }
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

    if (modeBadge) {
        modeBadge.textContent = config.mode === "edit" ? "수정 모드" : "신규 등록";
    }
    if (submitButton) {
        submitButton.textContent = config.mode === "edit" ? "문제 수정" : "문제 등록";
    }

    updatePreview();
    isDirty = false;
    isSubmitting = false;
    suppressBeforeUnload = false;
}

async function promptDraftSaveOnLeave(targetUrl) {
    if (!isDirty || isSubmitting) {
        if (targetUrl) {
            window.location.href = targetUrl;
        }
        return;
    }

    const shouldSaveDraft = confirm("입력 중입니다. 페이지를 이동하기 전에 임시 저장할까요?");
    if (shouldSaveDraft) {
        await saveDraft();
        return;
    }

    const shouldLeave = confirm("임시 저장 없이 이동하면 작성 중인 내용이 사라집니다. 이동할까요?");
    if (!shouldLeave) {
        return;
    }

    isDirty = false;
    suppressBeforeUnload = true;
    if (targetUrl) {
        window.location.href = targetUrl;
    }
}

window.addTag = addTag;
window.removeTag = removeTag;
window.addSample = addSample;
window.addTestCase = addTestCase;
window.removeSample = removeSample;
window.removeTestCase = removeTestCase;
window.resetForm = resetForm;
window.saveDraft = saveDraft;
window.submitProblem = submitProblem;
window.deleteProblem = deleteProblem;

if (titleInput) {
    titleInput.addEventListener("input", updatePreview);
}
if (descriptionInput) {
    descriptionInput.addEventListener("input", updatePreview);
}
if (difficultyInput) {
    difficultyInput.addEventListener("change", updatePreview);
}
if (categoryInput) {
    categoryInput.addEventListener("input", markDirty);
}
if (timeLimitInput) {
    timeLimitInput.addEventListener("input", markDirty);
}
if (memoryLimitInput) {
    memoryLimitInput.addEventListener("input", markDirty);
}
if (statusInput) {
    statusInput.addEventListener("change", markDirty);
}

document.addEventListener("input", function(event) {
    if (event.target.matches("[data-sample-input], [data-sample-output], [data-test-input], [data-test-output]")) {
        markDirty();
    }
});

document.addEventListener("click", function(event) {
    const link = event.target.closest("a[href]");
    const navBox = event.target.closest(".nav-box");
    const logo = event.target.closest("[onclick*=\"location.href\"]");

    if (isSubmitting) {
        return;
    }

    if (link) {
        const href = link.getAttribute("href");
        if (!href || href.startsWith("#") || href.startsWith("javascript:")) {
            return;
        }
        if (link.hasAttribute("download") || link.target === "_blank") {
            return;
        }

        event.preventDefault();
        promptDraftSaveOnLeave(link.href);
        return;
    }

    if (navBox && typeof navBox.onclick === "function") {
        event.preventDefault();
        const original = navBox.onclick;
        navBox.onclick = null;
        promptDraftSaveOnLeave(null).finally(function() {
            navBox.onclick = original;
        });
        return;
    }

    if (logo) {
        const onclickValue = logo.getAttribute("onclick") || "";
        const match = onclickValue.match(/location\.href='([^']+)'/);
        if (match && match[1]) {
            event.preventDefault();
            promptDraftSaveOnLeave(match[1]);
        }
    }
});

window.addEventListener("beforeunload", function(event) {
    if (!isDirty || isSubmitting || suppressBeforeUnload) {
        return;
    }

    event.preventDefault();
    event.returnValue = "";
});

fillForm(normalizedInitialProblem);
