# Interpreter_Java_To_Python



# 개요
Interpreter_Java_To_Python은 Java로 작성된 코드를 Python 코드로 자동으로 해석하는 인터프리터입니다. 이 프로젝트는 Java와 Python 간의 문법적 차이를 극복하고, 사용자들이 Java로 작성한 코드를 Python으로 실행할 수 있도록 지원합니다. 이를 통해 Java 기반의 프로젝트를 Python 환경에서도 실행 및 테스트할 수 있게 됩니다.

# 주요기능
:one: :Python 코드를 파싱하고 결과를 Java console창에 띄움.

:two: :print, Input, while, If, Elif, Else를 구현.

# 알고리즘
![프언3](https://github.com/ilovegalio/Interpreter_Java_To_Python/assets/77008882/786c01a3-9c04-4cd8-80b8-e0cf2add03a6)
하나의 단어를 TOKEN으로 받아드려 TOKEN이 Delimiter인지 variable인지 number인지 비교하는 알고리즘.

![프언4](https://github.com/ilovegalio/Interpreter_Java_To_Python/assets/77008882/78a677f5-e8cd-4fea-9dc1-bb39d600b417)
수식을 처리해야하는 경우 사칙연산 순서에 맞게 괄호 ➡️ 지수 ➡️ 곱셈과 나눗셈 ➡️ 덧셈과 뺄셈 순으로 계산하게 된다.

![프언5](https://github.com/ilovegalio/Interpreter_Java_To_Python/assets/77008882/7d792201-4cb8-43af-98f7-4bd74a596591)
TOKEN이 PRINT일 경우 Delimiter로 받아드려지고 공백을 처리한 후 콘솔에 출력함.
![프언6](https://github.com/ilovegalio/Interpreter_Java_To_Python/assets/77008882/6d2e4b7e-67f7-4c91-9bf7-60cc42349713)
TOEKN이 INPUT일 경우 다음 토큰들을 가져와 변수를 얻고 조건문 연산을 진행함.

# 결과
![프언1](https://github.com/ilovegalio/Interpreter_Java_To_Python/assets/77008882/00be0759-0fc5-4891-ad15-f15d7e669abc) ![프언2](https://github.com/ilovegalio/Interpreter_Java_To_Python/assets/77008882/ccafdbed-9633-4cfb-a289-2ee464b4dfe8)

