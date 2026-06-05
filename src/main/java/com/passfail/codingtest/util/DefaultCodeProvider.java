package com.passfail.codingtest.util;

import com.passfail.enums.ProgrammingLanguage;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 언어 및 모드별 기본 코드 템플릿을 제공하는 유틸리티 클래스
 */
@Component
public class DefaultCodeProvider {

    /**
     * 표준 코딩 테스트용 기본 코드
     */
    public String getDefaultCode(ProgrammingLanguage lang) {
        return switch (lang) {
            case JAVA -> "import java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 코드를 작성하세요\n    }\n}";
            case PYTHON -> "";
            case CPP -> "#include <iostream>\n\nusing namespace std;\n\nint main() {\n    // 코드를 작성하세요\n    \n    return 0;\n}";           
        };
    }

    /**
     * Logic Maze 모드용 사용 가능한 메서드 목록 (메타데이터)
     */
    public List<Map<String, String>> getLogicMazeApiDocs() {
        return List.of(
            Map.of("name", "moveForward()", "desc", "앞으로 한 칸 이동합니다."),
            Map.of("name", "turnLeft()", "desc", "왼쪽으로 90도 회전합니다."),
            Map.of("name", "turnRight()", "desc", "오른쪽으로 90도 회전합니다."),
            Map.of("name", "getCurrentTileType()", "desc", "현재 발 밑의 타일 종류를 반환합니다. (예: 'PATH', 'WALL', 'EXIT')"),
            Map.of("name", "isWallAhead()", "desc", "정면에 벽이 있는지 확인하여 true/false를 반환합니다.")
        );
    }

    /**
     * 사용자의 에디터에 보여줄 '깨끗한' 코드 (구현부 제외)
     */
    public String getLogicMazeEditorCode(ProgrammingLanguage lang) {
        return switch (lang) {
            case JAVA -> "import java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Robot robot = new Robot();\n        // 여기에 코드를 작성하세요. (예: robot.moveForward();)\n        \n    }\n}";
            case PYTHON -> "def solve():\n    robot = Robot()\n    # 여기에 코드를 작성하세요. (예: robot.move_forward())\n    \n\nif __name__ == '__main__':\n    solve()";
            case CPP -> "#include <iostream>\n#include <string>\n\nusing namespace std;\n\nint main() {\n    Robot robot;\n    // 여기에 코드를 작성하세요. (예: robot.moveForward();)\n    \n    return 0;\n}";
            default -> getDefaultCode(lang);
        };
    }

    /**
     * 서버에서 실행 시 뒤에 붙여줄 실제 Robot 클래스 구현부
     */
    public String getLogicMazeImplementation(ProgrammingLanguage lang) {
        return switch (lang) {
            case JAVA -> "\n\n// --- Server Side Robot Implementation ---\n" +
                         "class Robot {\n" +
                         "    private java.util.Scanner sc = new java.util.Scanner(System.in);\n" +
                         "    public void moveForward() { System.out.println(\"CMD:MOVE\"); System.out.flush(); sc.next(); }\n" +
                         "    public void turnLeft() { System.out.println(\"CMD:LEFT\"); System.out.flush(); sc.next(); }\n" +
                         "    public void turnRight() { System.out.println(\"CMD:RIGHT\"); System.out.flush(); sc.next(); }\n" +
                         "    public String getCurrentTileType() { System.out.println(\"CMD:TILE\"); System.out.flush(); return sc.next(); }\n" +
                         "    public boolean isWallAhead() { System.out.println(\"CMD:WALL\"); System.out.flush(); return sc.nextBoolean(); }\n" +
                         "}";
            case PYTHON -> "\n\nimport sys\nclass Robot:\n" +
                           "    def move_forward(self): print(\"CMD:MOVE\", flush=True); sys.stdin.readline()\n" +
                           "    def turn_left(self): print(\"CMD:LEFT\", flush=True); sys.stdin.readline()\n" +
                           "    def turn_right(self): print(\"CMD:RIGHT\", flush=True); sys.stdin.readline()\n" +
                           "    def get_tile(self): print(\"CMD:TILE\", flush=True); return sys.stdin.readline().strip()\n" +
                           "    def is_wall(self): print(\"CMD:WALL\", flush=True); return sys.stdin.readline().strip() == 'true'\n";
            case CPP -> "\n\nclass Robot {\n" +
                        "public:\n" +
                        "    void moveForward() { std::cout << \"CMD:MOVE\" << std::endl; std::string res; std::cin >> res; }\n" +
                        "    void turnLeft() { std::cout << \"CMD:LEFT\" << std::endl; std::string res; std::cin >> res; }\n" +
                        "    void turnRight() { std::cout << \"CMD:RIGHT\" << std::endl; std::string res; std::cin >> res; }\n" +
                        "    std::string getCurrentTileType() { std::cout << \"CMD:TILE\" << std::endl; std::string res; std::cin >> res; return res; }\n" +
                        "    bool isWallAhead() { std::cout << \"CMD:WALL\" << std::endl; std::string res; std::cin >> res; return (res == \"true\"); }\n" +
                        "};\n";
            default -> "";
        };
    }

    /**
     * 기존 하위 호환성을 위해 유지
     */
    public String getLogicMazeCode(ProgrammingLanguage lang) {
        return getLogicMazeEditorCode(lang) + getLogicMazeImplementation(lang);
    }
}
