-- title.lua (타이틀/기본 레벨 스크립트)

-- 1. 월드 기하구조 로드
-- 자바의 ScriptEngine이 이 경로의 map.dat를 읽어 실시간으로 2차원 int[][] 배열 맵을 빌드합니다.
engine.initScene("Title", "Data/Level/title/map.dat")

-- 2. 맵 데이터(map.dat) 내부의 숫자 코드(1, 2, 3)에 텍스처 ID 매핑
-- 자바 단의 Texture 코어가 Data.json의 id 블록을 보고 메모리에 올려둔 픽셀들을 숫자에 링크합니다.
engine.assignWallTexture(1, "brick_red")
engine.assignWallTexture(2, "iron_gate")
engine.assignWallTexture(3, "wood_panel")

-- 3. UI 레이아웃 활성화
-- DataLoader가 생성한 "hud1" (또는 "hud") ID를 타겟팅하여 화면 최상단 0,0 고정 출력 버퍼를 켭니다.
engine.setUiVisible("hud1", true)

-- 4. 플레이어 초기 스폰 위치 및 카메라 벡터 동적 세팅 (scene.json 기반 제어 예시)
-- 스크립트에서 초기 뷰포트 방향(dirX, dirY)과 레이캐스팅 평면(planeX, planeY)을 직접 찔러줍니다.
engine.setupPlayer(3.5, 3.5, -1.0, 0.0, 0.0, 0.66)

-- 5. 월드 내 오브젝트/NPC 스폰 
engine.spawnEntity("NPC", "npc_guard", 4.5, 6.2, "default")