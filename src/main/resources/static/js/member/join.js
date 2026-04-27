document.addEventListener('DOMContentLoaded', () => {
    const joinForm = document.getElementById('joinForm');
    const providerIdInput = document.getElementById('providerId');
    const passwordInput = document.getElementById('password'); 
    const checkIdBtn = document.getElementById('checkIdBtn');
    const idCheckMsg = document.getElementById('idCheckMsg');
    const pwMsg = document.getElementById('pwMsg'); // 비밀번호 메시지 공간

    let isIdChecked = false;

    // 정규표현식 규칙
    const idRegex = /^[a-zA-Z0-9]{7,}$/; // 영어+숫자, 7글자 이상
    const pwRegex = /^(?=.*[!@#$%^&*()_+])[A-Za-z\d!@#$%^&*()_+]{8,16}$/; // 8~16자, 특수문자 포함

    // 1. [아이디] 중복 확인 로직 (버튼 클릭 시 alert)
    checkIdBtn.addEventListener('click', async () => {
        const providerId = providerIdInput.value.trim();
        if (!providerId) {
            alert("아이디를 입력해주세요.");
            return;
        }
        
        if (!idRegex.test(providerId)) {
            alert("아이디는 영어와 숫자로만 구성된 7글자 이상이어야 합니다.");
            return;
        }

        try {
            const response = await fetch(`/member/check-id?providerId=${providerId}`);
            const isDuplicated = await response.json();

            if (isDuplicated) {
                idCheckMsg.innerText = "이미 사용 중인 아이디입니다.";
                idCheckMsg.style.color = "red";
                isIdChecked = false;
            } else {
                idCheckMsg.innerText = "사용 가능한 아이디입니다.";
                idCheckMsg.style.color = "green";
                isIdChecked = true;
            }
        } catch (e) {
            console.error("중복 체크 오류:", e);
        }
    });

    // 2. [비밀번호] 실시간 형식 검사 (입력할 때마다 색상 변경)
    passwordInput.addEventListener('input', () => {
        const password = passwordInput.value;

        if (password === "") {
            pwMsg.innerText = "8~16자, 특수문자를 반드시 포함해주세요.";
            pwMsg.style.color = "#777";
            return;
        }

        if (pwRegex.test(password)) {
            pwMsg.innerText = "사용 가능한 비밀번호입니다.";
            pwMsg.style.color = "green"; // 조건 충족 시 초록색
        } else {
            pwMsg.innerText = "8~16자, 특수문자를 포함해야 합니다.";
            pwMsg.style.color = "red"; // 조건 미달 시 빨간색
        }
    });

    // 3. [회원가입] 버튼 클릭 시 최종 제출 전 검증
    joinForm.addEventListener('submit', (e) => {
        const providerId = providerIdInput.value.trim();
        const password = passwordInput.value;

        // 아이디 규칙 검사
        if (!idRegex.test(providerId)) {
            e.preventDefault();
            alert("아이디 형식이 맞지 않습니다.\n(영어+숫자 조합, 7자 이상)");
            providerIdInput.focus();
            return;
        }

        // 아이디 중복 체크 여부 검사
        if (!isIdChecked) {
            e.preventDefault();
            alert("아이디 중복 확인을 해주세요.");
            return;
        }

        // 비밀번호 규칙 검사
        if (!pwRegex.test(password)) {
            e.preventDefault();
            alert("비밀번호 형식이 맞지 않습니다.\n(8~16자, 특수문자 반드시 포함)");
            passwordInput.focus();
            return;
        }
    });

    // 아이디 입력창 수정 시 중복확인 상태 리셋
    providerIdInput.addEventListener('input', () => {
        isIdChecked = false;
        idCheckMsg.innerText = "";
    });
	
	// join.js 하단에 추가
	const checkAll = document.getElementById('checkAll');
	const termChecks = document.querySelectorAll('.term-check');
	const term1 = document.getElementById('term1');
	const term2 = document.getElementById('term2');

	// 1. 전체 동의 로직
	checkAll.addEventListener('change', () => {
	    termChecks.forEach(check => {
	        check.checked = checkAll.checked;
	    });
	    // 선택 항목도 포함하고 싶다면 아래와 같이 작성
	    document.getElementById('term3').checked = checkAll.checked;
	});

	// 2. 폼 제출 시 약관 확인 (submit 이벤트 리스너 안에 추가)
	joinForm.addEventListener('submit', (e) => {
	    // ... 기존 아이디/비번 검증 로직 생략 ...

	    if (!term1.checked || !term2.checked) {
	        e.preventDefault();
	        alert("필수 약관에 모두 동의해주세요.");
	        return;
	    }
	});
});