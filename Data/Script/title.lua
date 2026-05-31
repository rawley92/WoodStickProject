local selected = 1
local fadeTimer = 0
local enterWasDown = false
local upWasDown = false
local downWasDown = false
local pulseTimer = 0
local pendingAction = nil
local DebugHotkey = assert(loadfile("Data/Script/debug_hotkey.lua", "bt", _ENV))()
local MenuUi = assert(loadfile("Data/Script/menu_ui.lua", "bt", _ENV))()

local BUTTONS = { "Start", "Exit" }

local function setupWorld()
    engine.assignWallTexture("Textures.Level.Wall_1")
    engine.setFloorTexture("Textures.Level.Floor_1")
    engine.setCeilingTexture("Textures.Level.Celling_1")
end

local function spawnController()
    engine.spawnEntity("Title_Controller", "", 0.0, 0.0, "Script.title")
end

local function loadTitle()
    _G.GameState.currentScene = "title"
    _G.GameState.debugNoDeath = false

    engine.initScene("Title", "Level.title.map")
    setupWorld()
    engine.setupPlayer(3.5, 3.5, -1.0, 0.0, 0.0, 0.88)
    spawnController()
end

local function choose()
    pulseTimer = 0.16

    if selected == 1 then
        pendingAction = "start"
    else
        pendingAction = "exit"
    end
end

local function runPendingAction()
    if pendingAction == nil or pulseTimer > 0 then return end

    local action = pendingAction
    pendingAction = nil

    if action == "start" then
        local loading = assert(loadfile("Data/Script/loading.lua", "bt", _ENV))
        loading()
    elseif action == "exit" then
        engine.exit()
    end
end

local function draw()
    local alpha = math.min(1.0, fadeTimer / 1.5)

    engine.uiClear()
    MenuUi.drawBackground(0x0A2C57, 0.34, alpha)
    MenuUi.drawTitle("ENTROPISM", 180, 78, 0xD8E6FF, alpha)

    for i, label in ipairs(BUTTONS) do
        MenuUi.drawButton(label, i, selected == i, pulseTimer, 0x4EB3FF, alpha)
    end

    engine.uiTextCenter("ENTER YOUR ADVENTURE", 640, 675, 24, 0x7DBAFF, alpha)
end

function update(entity, dt, player, control)
    fadeTimer = fadeTimer + dt

    if pulseTimer > 0 then
        pulseTimer = pulseTimer - dt
    end

    runPendingAction()

    if control ~= nil then
        if DebugHotkey.consume(control) then
            return
        end

        if control.s_menuUp and not upWasDown then
            selected = selected - 1
            if selected < 1 then selected = #BUTTONS end
        end

        if control.s_menuDown and not downWasDown then
            selected = selected + 1
            if selected > #BUTTONS then selected = 1 end
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

if _G.GameState.currentScene ~= "title" then
    loadTitle()
end
