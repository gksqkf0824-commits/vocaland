# 🎮 보카랜드 (VocaLand)

> 게임처럼 즐기며 반복하는 영어 단어 학습 & 퀴즈 프로그램  
> 객체지향프로그래밍 팀 프로젝트 · K조

영단어 학습의 핵심은 반복이지만, 단어장을 넘기는 방식은 쉽게 지루해집니다.  
보카랜드는 게임 요소와 즉각적인 보상을 활용해 학습 → 퀴즈 → 결과 확인 → 복습이 자연스럽게 이어지도록 만든 영어 단어 학습 프로그램입니다.

<br>

## 📌 주요 기능

| 화면 | 설명 |
|---|---|
| 메인화면 | 단어 학습, 게임, 스코어보드, 상점 등 주요 기능으로 이동 |
| 단어 학습 | 테마별 영어와 뜻을 확인하고, 뜻을 가리며 반복 암기 |
| 카드 짝 맞추기 | 카드를 뒤집어 단어와 뜻의 짝을 찾는 복습 게임 |
| 산성비 게임 | 떨어지는 단어의 뜻을 입력해 점수와 코인 획득 |
| 농구 게임 | 단어 뜻에 해당하는 골대로 공을 던져 점수와 코인 획득 |
| 상점 | 게임으로 모은 코인으로 캐릭터, 농구공, 배경 구매 |
| 스코어보드 | 같은 PC에 저장된 사용자별 최고 점수를 게임별로 비교 |

<br>

## 🛠 기술 스택

- **Language** : Java (JDK 17)
- **GUI** : Java Swing
- **Data** : 텍스트 파일 입출력 (`|` 구분자)
- **IDE** : Visual Studio Code
- **Collaboration** : Git, GitHub

<br>

## 📁 프로젝트 구조

```
vocaland/
├── data/                  # 텍스트 데이터 파일
├── images/                # 캐릭터, 농구공, 배경 등 이미지
└── src/vocaland/
    ├── Main.java          # 프로그램 시작점
    ├── model/             # 데이터 클래스 (Word, User, Record ...)
    ├── repository/        # 파일 읽기 / 쓰기
    ├── service/           # 퀴즈 출제, 점수·코인, 랭킹, 구매 로직
    └── ui/                # Swing 화면 (JFrame, JPanel)
```

<br>

## 🗂 데이터 구성

| 파일 | 내용 | 예시 |
|---|---|---|
| `words.txt` | 단어 ID, 영어, 뜻, 테마, 난이도 | `W001\|apple\|사과\|음식\|초급` |
| `records.txt` | 사용자 ID, 단어 ID, 정답 여부, 날짜 | `U001\|W001\|오답\|2026-11-01` |
| `users.txt` | 사용자 ID, 닉네임, 누적 경험치 | `U001\|조용현\|120` |
| `scores.txt` | 사용자 ID, 게임 점수, 플레이 날짜 | |
| `items.txt` | 아이템 ID, 이름, 적용 대상, 가격, 이미지 경로 | |
| `inventory.txt` | 사용자 ID, 보유 아이템 ID | |

> 모든 파일은 **UTF-8** 인코딩, **상대경로(`data/...`)** 로 접근합니다.

<br>

## ▶ 실행 방법

1. 레포지토리 클론
   ```bash
   git clone https://github.com/<계정>/vocaland.git
   ```
2. VS Code에서 `vocaland` 폴더 열기  
   (확장 프로그램 **Extension Pack for Java** 설치 필요)
3. `src/vocaland/Main.java` 열고 `Run` 클릭

> 인코딩, 소스 경로 등 공통 설정은 `.vscode/settings.json`에 포함되어 있어 별도 설정이 필요 없습니다.

<br>

## 👥 팀원 및 역할

| 이름 | 역할 |
|---|---|
| 권소윤 | 팀장, 백엔드 |
| 고희건 | 백엔드 |
| 백재현 | 백엔드 |
| 양준혁 | 프론트엔드, 발표 |
| 이의준 | 프론트엔드 |
| 조용현 | 제안서 작성, 프론트엔드, PPT 제작 |
| 박지환 | 프론트엔드, 발표 |

<br>

## 🤝 협업 규칙

### 브랜치
- `main` : 항상 실행 가능한 상태 유지, **직접 push 금지**
- `feature/기능명` : 기능별 작업 브랜치  
  예) `feature/be-core`, `feature/fe-basketball`

### 프론트엔드 브랜치별 작업 범위

| 브랜치 | 작업 범위 |
|---|---|
| `feature/main_ui` | 메인 화면, 화면 전환 |
| `feature/study` | 단어 학습 및 카드 짝 맞추기 복습 |
| `feature/game` | 산성비 게임 및 농구 게임 |
| `feature/store` | 상점 및 스킨 선택·적용 |
| `feature/scoreboard` | 게임별 최고 점수 및 사용자 순위 표시 |

### 작업 흐름
1. 작업 전 `git pull origin main` 으로 최신화
2. `git checkout -b feature/기능명` 으로 브랜치 생성
3. 작업 후 커밋 → `git push origin feature/기능명`
4. GitHub에서 Pull Request 생성 → 리뷰 후 `main`에 merge

### 커밋 메시지
| 태그 | 의미 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 구조 개선 |
| `design` | UI 수정 |
| `docs` | 문서 수정 |
| `chore` | 설정, 기타 |

예) `feat: 농구 게임 골대 판정 구현`
