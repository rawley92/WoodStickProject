-- Escape result scene controller.
-- 플레이어가 출구에 도달했을 때 표시되는 성공 메뉴다.

local selected = 1
local enterWasDown = false
local upWasDown = false
local downWasDown = false
local pulseTimer = 0
local pendingAction = nil
local waitForEnterRelease = true
local DebugHotkey = assert(loadfile("Data/Script/debug_hotkey.lua", "bt", _ENV))()
local MenuUi = assert(loadfile("Data/Script/menu_ui.lua", "bt", _ENV))()
local BUTTONS = { "Title", "Exit" }

-- 결과 화면 월드 배경에 사용할 기본 텍스처를 바인딩한다.
local function setupWorld()
    engine.assignWallTexture("Textures.Level.Wall_1")
    engine.setFloorTexture("Textures.Level.Floor_1")
    engine.setCeilingTexture("Textures.Level.Celling_1")
end

-- Escape 메뉴를 매 프레임 업데이트할 controller 엔티티를 생성한다.
local function spawnController()
    engine.spawnEntity("Escape_Controller", "", 0.0, 0.0, "Script.escape")
end

-- Escape 씬을 로드하고 메뉴 입력 상태를 초기화한다.
local function loadScene()
    _G.GameState.currentScene = "escape"
    selected = 1
    enterWasDown = false
    upWasDown = false
    downWasDown = false
    pulseTimer = 0
    pendingAction = nil
    waitForEnterRelease = true

    engine.initScene("Escape", "Level.escape.map")
    setupWorld()
    engine.setupPlayer(1.5, 1.5, -1.0, 0.0, 0.0, 0.88)
    spawnController()
end

-- 현재 선택된 메뉴 액션을 pulse 후 실행되도록 예약한다.
local function choose()
    pulseTimer = 0.16
    pendingAction = selected == 1 and "title" or "exit"
end

-- 예약된 액션을 타이틀 복귀 또는 프로그램 종료로 실행한다.
local function runPendingAction()
    if pendingAction == nil or pulseTimer > 0 then return end

    local action = pendingAction
    pendingAction = nil

    if action == "title" then
        local title = assert(loadfile("Data/Script/title.lua", "bt", _ENV))
        title()
    else
        engine.exit()
    end
end

-- 성공 메뉴 UI를 그린다.
local function draw()
    engine.uiClear()
    MenuUi.drawBackground(0x103B1F, 0.36, 1.0)
    MenuUi.drawTitle("ESCAPED", 180, 78, 0xD9FFE3, 1.0)

    for i, label in ipairs(BUTTONS) do
        MenuUi.drawButton(label, i, selected == i, pulseTimer, 0x4EEB78, 1.0)
    end
end

-- Escape_Controller 엔티티가 호출하는 메뉴 update 함수다.
function update(entity, dt, player, control)
    if pulseTimer > 0 then pulseTimer = pulseTimer - dt end
    runPendingAction()

    if control ~= nil then
        if DebugHotkey.consume(control) then
            return
        end

        if waitForEnterRelease then
            -- maze.lua에서 Enter로 진입했기 때문에, 같은 키 입력이 즉시 메뉴 선택으로 이어지지 않게 한다.
            if not control.s_enter then
                waitForEnterRelease = false
            end

            enterWasDown = control.s_enter
            draw()
            return
        end

        if control.s_menuUp and not upWasDown then
            selected = selected == 1 and #BUTTONS or selected - 1
        end

        if control.s_menuDown and not downWasDown then
            selected = selected == #BUTTONS and 1 or selected + 1
        end

        if control.s_enter and not enterWasDown and pendingAction == nil then
            choose()
        end

        upWasDown = control.s_menuUp
        downWasDown = control.s_menuDown
        enterWasDown = control.s_enter
    end

    draw()
end

loadScene()
