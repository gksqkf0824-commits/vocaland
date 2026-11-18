# VocaLand — 단어 학습·개인별 복습 백엔드

**담당:** 단어 학습 데이터, '다시 볼 단어' 체크, 카드 짝맞추기 단어 선정·정답 기록, 산성비 개인별 단어 가중 출제·오답 기록  
**브랜치:** `feature/be-study` → PR 대상 `develop`  
**환경:** Java 17 / UTF-8 / 텍스트 파일 구분자 `|`

## 1. 구현 범위

원본 VocaLand 프로토타입에 맞춰 **별도의 일반 객관식 단어 퀴즈는 제공하지 않습니다.** 단어 학습 화면에서는 단어·뜻·테마를 확인하고 '다시 볼 단어 표시'를 체크합니다. 출제·복습 기능은 카드 짝맞추기 및 산성비 게임에 연결합니다. 농구 게임은 현재 기획대로 개인 가중치 없이 일반 단어 목록을 사용합니다.

```text
src/vocaland/
├── model/
│   ├── WordCard.java          # 카드 한 장: 단어 ID, 표시 문자, 영어/한글 구분
│   └── StudyRecord.java       # 사용자별 정답·오답·복습 체크 기록
├── repository/
│   └── RecordRepository.java  # records.txt, reviews.txt 읽기/쓰기
└── service/
    ├── StudyChannel.java      # GUI 연동 인터페이스
    └── StudyService.java      # 단어 조회, 가중치 계산, 카드/산성비 연동

tests/vocaland/BeStudyTest.java  # 백엔드/현재 develop 연동 테스트
```

팀장이 담당하는 `Word.java`, `WordRepository.java`, `WordService.java`, `TextFileStore.java`, `data/words.txt`, 사용자 관리 클래스는 이 패치에 포함하지 않으며 수정하지 않습니다. 기존 GUI 클래스와 게임 규칙도 이 패치에서 수정하지 않습니다.

**예전 be-study 패치를 이미 복사했다면:** 일반 퀴즈에 사용하던 `src/vocaland/model/Quiz.java`는 직접 삭제해 주세요. 이 배포본 ZIP에는 그 파일을 포함하지 않으므로 압축 해제만으로 기존 파일이 자동 삭제되지는 않습니다. 옛 `tests/vocaland/BeStudyV7Test.java`와 `docs/BE_STUDY_V7_GUIDE.md`가 남아 있다면 더 이상 필요하지 않습니다.

## 2. 단어 가중치 규칙

가중치는 **사용자 ID와 단어 ID별**로 독립 적용되며, 카드 짝맞추기·산성비에서 **동일한 계산 결과**를 사용합니다.

### 2-1. 오답으로 인한 기본 가중치

| 발생한 일 | 기본 가중치 |
|---|---|
| 처음 시작 | 1배 |
| 산성비에서 단어를 놓침 | +1배(최대 3배), 연속 정답 횟수 초기화 |
| 카드 짝맞추기에서 카드 두 장을 잘못 선택 | 변화 없음. 어느 단어를 모르는지 단정할 수 없음 |
| 산성비 정답 또는 카드 한 쌍 완성 | 정답 1회 기록 |
| **같은 단어를 연속 2회 정답** | -1배(최소 1배), 연속 정답 횟수 초기화 |

정답은 산성비와 카드 짝맞추기를 합쳐 누적합니다. 예를 들어 산성비에서 `apple`을 놓쳐 기본 가중치가 2가 되고, 이후 카드에서 `apple–사과`를 두 번 성공적으로 맞히면 기본 가중치는 1로 돌아갑니다. 카드 한 쌍을 성공적으로 맞힐 때 **1회만 기록**해야 합니다.

### 2-2. '다시 볼 단어 표시' 수동 체크

단어 학습 화면에서 단어를 **'다시 볼 단어'로 체크하면 기본 가중치에 +1을 추가**합니다. 단, 최종 가중치는 **최대 3배**입니다.

```text
최종 가중치 = min(3, 오답 기록에 따른 기본 가중치 + (수동 복습 체크 ? 1 : 0))
```

| 단어 상태 | 체크 해제 | 체크 상태 |
|---|---:|---:|
| 틀린 적 없거나 충분히 복습함 (기본 1) | 1배 | **2배** |
| 아직 오답 학습 중 (기본 2) | 2배 | **3배** |
| 오답이 많이 남음 (기본 3) | 3배 | **3배(최대)** |

- 체크 해제 시 **수동 +1만 제거**하고 기존 오답 이력은 그대로 둡니다.
- 연속 정답 2회로 감소하는 것은 **오답 기반 기본 가중치**입니다. 수동 체크가 유지되는 한 수동 +1도 계속 유지됩니다.
- '다시 볼 단어' 목록에는 **현재 기본 가중치가 2배 이상인 단어 + 수동 체크 단어**가 들어갑니다. 수동 체크는 직접 해제할 때까지 유지됩니다.
- `getWordWeight(userId, wordId)`는 위 수동 보너스를 더한 **최종 출제 가중치**를 반환합니다.

### 2-3. 두 게임에 적용되는 방식

- **산성비:** `loadWeightedWordPairs(userId)`는 `{영어, 한글 뜻}` 배열을 단어별 최종 가중치 횟수만큼 포함합니다. 기존 `RainGame`이 이 목록에서 무작위로 뽑기 때문에 체크·오답 단어가 상대적으로 더 자주 나옵니다. **게임 시작 시점의 가중치로 목록을 새로 구성해야 합니다.**
- **카드 짝맞추기:** `createMatchingCards(userId, "전체", 6)`는 가중치에 비례하여 **서로 다른 6개 단어를 뽑고 12장으로 섞습니다.** 같은 단어가 한 판에 중복 출제되지는 않습니다. 다시 볼 단어만 고르려면 `createMatchingCards(userId, "다시 볼 단어", 6)`을 사용하며, 6쌍이 부족하면 다른 단어로 채웁니다.
- **농구:** `loadBasketballWordPairs()`를 사용하며 **개인별 가중치를 적용하지 않습니다.**

## 3. 저장 파일

| 파일 | 형식 | 예시 |
|---|---|---|
| `data/words.txt` | 팀 공통 단어장 (읽기만 함) | `W001\|apple\|사과\|음식\|중급` |
| `data/records.txt` | 사용자ID·단어ID·정오답·날짜 | `U001\|W001\|오답\|2026-11-18` |
| `data/reviews.txt` | 사용자ID·단어ID·복습 체크/해제·날짜 | `U001\|W001\|복습등록\|2026-11-18` |

`records.txt`는 팀 공통 README 형식을 유지합니다. 수동 체크의 **해제 상태까지 보존하려면** `복습등록` / `복습해제` 이벤트를 저장하는 `reviews.txt`가 추가로 필요합니다. 이 파일은 팀의 기존 공통 README에 없으므로 **팀장과 사용 여부를 협의**해 주세요. 학습 기록은 파일 행 순서대로 계산하므로 같은 날짜에 여러 번 맞혀도 정답 연속 횟수가 복원됩니다. 기록은 사용 시 자동 생성될 수 있습니다.

## 4. 프론트엔드 연결 방법

공통 초기화 (프로젝트 루트에서 실행, `IOException` 처리 필요):

```java
import vocaland.service.StudyChannel;
import vocaland.service.StudyService;

StudyChannel study = new StudyService();
// userService.getCurrentUser()가 null이 아닌지 확인한 뒤 실제 사용자 ID 사용
String userId = userService.getCurrentUser().getId();
```

### 단어 학습 화면 담당

```java
// 단어 데이터: "전체", 테마명, "다시 볼 단어"
study.getStudyWords(userId, "음식");
study.getStudyWords(userId, "다시 볼 단어");

// 체크박스 현재 상태 표시
boolean checked = study.isReview(userId, word.getId());

// 사용자가 체크 또는 해제했을 때 호출 (IOException 처리)
study.setReview(userId, word.getId(), reviewCheck.isSelected());
```

**체크 자체는 정답/오답 이벤트가 아닙니다.** 단어를 넘기거나 뜻을 가리는 것만으로 `recordAnswer()`를 호출하지 않습니다.

### 카드 짝맞추기 담당

```java
List<WordCard> cards = study.createMatchingCards(userId, "전체", 6);

if (study.isPair(firstCard, secondCard)) {
    // 해당 한 쌍의 최초 성공 시에만 1회 호출 (IOException 처리)
    study.recordMatchedPair(userId, firstCard, secondCard);
}
```

`isPair()`는 판정만 하고 **기록을 저장하지 않으므로** 성공 시 `recordMatchedPair()` 호출이 필요합니다. 잘못 선택한 카드는 오답으로 기록하지 않습니다.

### 산성비 / 메인 통합 담당

```java
// 게임 시작마다 현재 사용자의 최신 가중치 목록을 전달
List<String[]> pairs = study.loadWeightedWordPairs(userId);
AcidRainPanel rainPanel = new AcidRainPanel(pairs);
```

**중요: 기존 산성비 GUI에는 정답 단어 콜백과 놓친 단어의 즉시 저장 콜백이 없습니다.** 산성비 담당자가 `AcidRainPanel`에 다음 두 종류의 이벤트를 연결해야 합니다.

1. **정답:** 현재 `submitAnswer()`는 `game.submit(input)`의 점수만 받습니다. 실제 제거된 `FallingWord`의 영어를 전달하고 `study.recordCorrectByEnglish(userId, removedEnglish)`를 **단어당 한 번** 호출하세요.
2. **오답:** 현재 `tick()`의 `game.update(...)`가 반환하는 `landed` 리스트에는 **방금 바닥에 닿은 단어**가 들어 있습니다. 그 자리에서 단어마다 `study.recordWrongByEnglish(userId, landedEnglish)`를 호출하는 형태를 권장합니다. **`onGameOver`에서 다시 같은 놓친 단어를 저장하면 중복 기록이 되므로 둘 중 하나의 방식만 사용하세요.**

기존 `rainPanel.setOnGameOver((score, level, missed) -> ...)` 방식으로 **게임 오버 때 한 번에 저장하는 것도 가능**하지만, 그러면 사용자가 게임 도중 **메인으로 돌아가면 이미 놓친 1~2개 단어가 기록되지 않습니다.** 따라서 완전한 연동에는 위의 **즉시 오답 기록**이 더 안전합니다. 게임 중 잘못 입력한 문자열은 어느 단어의 오답인지 특정하기 어려우므로, **바닥에 닿은 단어를 오답**으로 취급합니다.

또한 현재 `MainFrame`은 산성비 패널을 프로그램 시작 때 한 번 생성합니다. `AcidRainPanel`의 **다시하기** 버튼도 기존 `RainGame` 객체를 재사용합니다. 따라서 **메인에서 재진입할 때뿐만 아니라 '다시하기' 후 새 판이 시작될 때도** 사용자별 가중치 목록을 새로 만들어 게임에 전달해야 합니다. 패널을 새로 생성하거나 `RainGame`의 단어 목록을 재설정하는 방법을 산성비·메인 담당자와 협의해 주세요.

**사용자 ID:** 게임이 시작할 때 선택된 사용자의 ID를 해당 판의 기록 대상으로 유지하세요. 게임 도중 사용자 변경이 일어나더라도 기록이 다른 계정에 들어가지 않도록 해야 합니다.

## 5. 팀원별 남은 작업

| 담당 | 필요한 연결 |
|---|---|
| `be-core` / 메인 통합 | 현재 선택 사용자 ID 제공, **단일 `StudyService` 인스턴스 공유**, `reviews.txt` 사용 규격 협의 |
| 단어 학습 GUI | 체크박스를 `isReview`·`setReview`에 연결, 테마 및 복습 목록 연결 |
| 카드 GUI | `createMatchingCards` 사용, 짝 완성 시 `recordMatchedPair` **한 번만** 호출 |
| 산성비 GUI | **정답 단어 및 바닥에 떨어진 단어 콜백** 연결, 한 이벤트를 중복 기록하지 않기 |
| 메인 통합 | 산성비 **매 새 판(다시하기 포함)**마다 `loadWeightedWordPairs(userId)`로 목록 갱신 |
| 농구 GUI | `loadBasketballWordPairs()`로 중복 없는 일반 단어 사용; 개인 가중치 없음 |

**PR을 병합하는 것만으로 GUI 연동이 자동 완료되지는 않습니다.** 이 패치는 기존 Swing 화면이나 게임 로직을 변경하지 않습니다.

**연동 시 반드시 지킬 점:** `StudyService`를 화면마다 `new`로 생성하지 말고 프로그램에서 하나를 생성해 전달하세요. 이 서비스는 시작할 때 기록을 읽어 메모리에 보관하므로 다른 서비스 인스턴스의 수정은 즉시 감지하지 못합니다. 카드 한 쌍의 성공은 **한 판당 한 번만** 기록하세요. 파일의 동시 쓰기는 지원하지 않습니다.

## 6. 적용 및 테스트

1. `develop` 최신 코드를 Pull한 뒤 **`feature/be-study`** 브랜치에서 작업합니다.
2. 압축 파일의 `src/`, `tests/`, `docs/`를 프로젝트 루트에 병합 복사합니다. 팀 공통 클래스, 이미지, `data/words.txt`는 덮어쓰지 않습니다.
3. 예전 패치를 넣었다면 남아 있는 `src/vocaland/model/Quiz.java`와 `BeStudyV7Test.java`를 삭제합니다.
4. 프로젝트 루트(하위에 `data/`, `src/`가 보이는 폴더)에서 다음 테스트를 실행합니다.

```powershell
New-Item -ItemType Directory -Force build | Out-Null
$javaFiles = (Get-ChildItem -Recurse -Filter *.java -Path src,tests).FullName
javac --release 17 -encoding UTF-8 -d build $javaFiles
java -cp build vocaland.BeStudyTest
```

이번 패치의 로컬 검증에서는 **제공된 `vocaland-develop.zip` 스냅샷 + Java 17로 컴파일 성공, 667개 자동 검사 통과**했습니다. 실제 팀 GitHub `develop`은 이후 수정되었을 수 있고, GUI 연결을 끝낸 통합 실행까지 검증한 것은 아닙니다. 최신 브랜치에서 컴파일 및 충돌 여부를 다시 확인해야 합니다.

## 7. 검수 결과 및 알려진 제한

- 기존 `develop` 스냅샷에서 **Java 17 대상 컴파일 및 자동 검사 667개 통과**. 이 수치는 **GUI 종단간 테스트 667개를 의미하지 않으며**, 반복 추출 검사를 포함합니다.
- 현재 코드 안에서 잘못된 카드 출제 테마가 전체 무작위 출제로 조용히 대체되던 문제를 차단했고, `records.txt`의 잘못된 날짜를 파일 오류(`IOException`)로 안내하게 했습니다.
- **아직 미완료:** 산성비 정답·오답 즉시 콜백, 재시도 시 가중치 갱신, 단어 학습 체크박스·카드 UI 연동. 실제 GUI 담당자와 함께 동작을 확인해야 합니다.
- **저장 주의:** 학습 기록은 `records.txt`, `reviews.txt`를 파일 전체 읽기·재작성 방식으로 저장합니다. 팀 프로젝트의 작은 데이터 규모용이며 대량 기록이나 여러 서비스 인스턴스의 **동시 저장에는 적합하지 않습니다**.
- **출제 확률:** 2배/3배는 다른 단어와 비교하는 **추첨 가중치**를 말합니다. 카드가 6쌍을 중복 없이 뽑고 산성비가 화면 내 동일 단어를 피하려고 재추첨할 수 있으므로, 실제 등장 확률이 언제나 정확히 2배/3배인 것은 아닙니다.
- **설계 선택:** 카드에서 잘못된 짝을 고른 것은 오답으로 저장하지 않지만, 짝을 맞히면 정답으로 저장됩니다. 추측으로 맞혀도 숙련도가 올라갈 수 있으므로 더 엄격한 학습 평가가 필요하다면 별도 기준(예: 첫 시도에 성공한 쌍만 정답 기록)을 논의하세요.
- 사용자의 '다시 볼 단어' 체크가 너무 많으면 모든 단어의 가중치가 비슷해지므로, **체크된 단어의 상대적인 출제 우선순위가 약해지는 것은 정상 동작**입니다.
