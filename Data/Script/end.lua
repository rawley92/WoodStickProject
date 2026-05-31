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

local function setupWorld()
    engine.assignWallTexture("Textures.Level.Wall_1")
    engine.setFloorTexture("Textures.Level.Floor_1")
    engine.setCeilingTexture("Textures.Level.Celling_1")
end

local function spawnController()
    engine.spawnEntity("End_Controller", "", 0.0, 0.0, "Script.end")
end

local function loadScene()
    _G.GameState.currentScene = "end"
    selected = 1
    enterWasDown = false
    upWasDown = false
    downWasDown = false
    pulseTimer = 0
    pendingAction = nil
    waitForEnterRelease = true

    engine.initScene("End", "Level.end.map")
    setupWorld()
    engine.setupPlayer(1.5, 1.5, -1.0, 0.0, 0.0, 0.88)
    spawnController()
end

local function choose()
    pulseTimer = 0.16
    pendingAction = selected == 1 and "title" or "exit"
end

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

local function draw()
    engine.uiClear()
    MenuUi.drawBackground(0x4A0000, 0.46, 1.0)
    MenuUi.drawTitle("TERMINATED", 180, 74, 0xFFD6D6, 1.0)

    for i, label in ipairs(BUTTONS) do
        MenuUi.drawButton(label, i, selected == i, pulseTimer, 0xFF4040, 1.0)
    end
end

function update(entity, dt, player, control)
    if pulseTimer > 0 then pulseTimer = pulseTimer - dt end
    runPendingAction()

    if control ~= nil then
        if DebugHotkey.consume(control) then
            return
        end

        if waitForEnterRelease then
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
