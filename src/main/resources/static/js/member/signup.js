$(document).ready(function() {
    let isUsernameOk = false;
    let isEmailOk = false;
    let isPasswordOk = false;

    function updateSubmitBtn() {
        const privacyOk = $('#privacyPolicyAgreed').is(':checked');
        $('#submitBtn').prop('disabled', !(isUsernameOk && isEmailOk && isPasswordOk && privacyOk));
    }

    // 아이디 입력 시 상태 리셋
    $('#username').on('input', function() {
        isUsernameOk = false;
        $('#usernameMsg').text('아이디 중복 확인이 필요합니다.').css('color', 'orange');
        updateSubmitBtn();
    });

    // 아이디 중복 확인
    $('#checkUsernameBtn').click(function() {
        const username = $('#username').val();
        if (username.length < 7) {
            $('#usernameMsg').text('아이디는 7자 이상이어야 합니다.').css('color', 'red');
            return;
        }

        $.get('/api/member/check-username', { username: username }, function(data) {
            if (data.available) {
                $('#usernameMsg').text('사용 가능한 아이디입니다.').css('color', 'green');
                isUsernameOk = true;
            } else {
                $('#usernameMsg').text('이미 사용 중인 아이디입니다.').css('color', 'red');
                isUsernameOk = false;
            }
            updateSubmitBtn();
        });
    });

    // 비밀번호 유효성 검사
    $('#password').on('input', function() {
        const password = $(this).val();
        // 최소 8자, 특수문자 최소 1개 포함
        const regex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/;
        if (password.length >= 8 && regex.test(password)) {
            $('#passwordMsg').text('사용 가능한 비밀번호입니다.').css('color', 'green');
            isPasswordOk = true;
        } else {
            $('#passwordMsg').text('8자 이상, 특수문자를 최소 1개 포함해야 합니다.').css('color', 'red');
            isPasswordOk = false;
        }
        updateSubmitBtn();
    });

    // 이메일 입력 시 상태 리셋
    $('#email').on('input', function() {
        if (!$(this).prop('readonly')) {
            isEmailOk = false;
            $('#emailMsg').text('');
            updateSubmitBtn();
        }
    });

    // 인증번호 발송
    $('#sendVerificationBtn').click(function() {
        const email = $('#email').val();
        if (!email) {
            alert('이메일을 입력해주세요.');
            return;
        }

        // [화면 체감 성능 개선] 즉시 UI 반응
        const $btn = $(this);
        const originalText = $btn.text();
        
        $btn.prop('disabled', true).text('발송 중...');
        $('#verificationGroup').fadeIn(); // 즉시 인증번호 입력창 표시
        $('#emailMsg').text('인증번호를 발송했습니다. 잠시만 기다려주세요.').css('color', 'blue');

        $.post('/api/member/send-verification', { email: email }, function(data) {
            if (data.status === 'success') {
                $btn.text('재발송');
                $btn.prop('disabled', false);
                $('#emailMsg').text('인증번호가 발송되었습니다. 메일함을 확인해주세요.').css('color', 'green');
            } else {
                alert(data.message);
                $btn.text(originalText);
                $btn.prop('disabled', false);
                $('#emailMsg').text(data.message).css('color', 'red');
            }
        }).fail(function() {
            alert('서버 오류가 발생했습니다. 다시 시도해주세요.');
            $btn.text(originalText);
            $btn.prop('disabled', false);
        });
    });

    // 인증번호 확인
    $('#verifyEmailBtn').click(function() {
        const email = $('#email').val();
        const code = $('#verificationCode').val();

        if (!code) {
            alert('인증번호를 입력해주세요.');
            return;
        }

        $.post('/api/member/verify-email', { email: email, code: code }, function(data) {
            if (data.verified) {
                $('#emailMsg').text('이메일 인증이 완료되었습니다.').css('color', 'green');
                isEmailOk = true;
                $('#email').prop('readonly', true);
                $('#sendVerificationBtn').prop('disabled', true);
                $('#verificationCode').prop('readonly', true);
                $('#verifyEmailBtn').prop('disabled', true);
            } else {
                $('#emailMsg').text('인증번호가 일치하지 않습니다.').css('color', 'red');
                isEmailOk = false;
            }
            updateSubmitBtn();
        });
    });

    $('#privacyPolicyAgreed').change(function() {
        updateSubmitBtn();
    });

    // 폼 제출 시 최종 확인
    $('#signupForm').on('submit', function(e) {
        if (!(isUsernameOk && isEmailOk && isPasswordOk && $('#privacyPolicyAgreed').is(':checked'))) {
            e.preventDefault();
            alert('모든 가입 조건을 충족해야 합니다.');
        }
    });
});
