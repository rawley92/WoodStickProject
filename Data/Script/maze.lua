local Ui = dofile("Data/Script/ui.lua")
local PlayerDamage = dofile("Data/Script/player_damage.lua")
local CombatEffects = dofile("Data/Script/combat_effects.lua")
local ObjectSpawn = dofile("Data/Script/object_spawn.lua")
local DebugHotkey = assert(loadfile("Data/Script/debug_hotkey.lua", "bt", _ENV))()

local MELEE_DAMAGE = 5
local GUN_DAMAGE = 7
local enterWasDown = false
local spaceWasDown = false
local mWasDown = false
local menuUpWasDown = false
local menuDownWasDown = false
local showFullMap = false
local mapZoom = 1.0
local noticeTimer = 0
local noticeText = nil
local transitioned = false
local spawned = {}

local function centerOf(tile)
    return tile - 0.5
end

local function setupWorld()
    engine.assignWallTexture("Textures.Level.Wall_1")
    engine.setFloorTexture("Textures.Level.Floor_1")
    engine.setCeilingTexture("Textures.Level.Celling_1")
end

local function spawnController()
    engine.spawnEntity("Maze_Controller", "", 0.0, 0.0, "Script.maze")
end

local function spawnMazeObject(object)
    local x, y = centerOf(object.x), centerOf(object.y)
    
    if object.type == "player_start" or object.type == "exit" then return end

    local id = ObjectSpawn.spawn(object.type, x, y)

    if ObjectSpawn.isMonster(object.type) and id ~= nil then
        spawned[id] = true
    end
end

local function resetPlayerState()
    _G.GameState.debugNoDeath = false

    _G.PlayerState = {
        hp = 100,
        maxHp = 100,
        invincibleTimer = 0,
        damageFlashTimer = 0,
        crosshairHitTimer = 0,
        weaponFireFrames = 0,
        weaponSwingTimer = 0,
        weaponSwingDuration = 0.5,
        weapon = nil,
        ammo = 0
    }
end

local function loadMaze()
    _G.GameState.currentScene = "maze"
    resetPlayerState()
    spawned = {}

    engine.initScene("Maze", "Level.maze.map")
    setupWorld()

    local start = _G.GameState.start

    if start == nil or start.x == nil then
        print("[WARNING] start 좌표가 nil입니다! 기본값(2, 2)으로 강제 스폰합니다.")
        start = { x = 2, y = 2 }
        _G.GameState.start = start
    end

    -- 이제 start가 무조건 존재하므로 에러가 발생하지 않습니다.
    engine.setupPlayer(centerOf(start.x), centerOf(start.y), -1.0, 0.0, 0.0, 0.88)

    for _, object in ipairs(_G.GameState.mazeObjects or {}) do
        spawnMazeObject(object)
    end

    spawnController()
end

local function distTo(player, point)
    if point == nil then return 9999 end

    local dx = player.physics.x - centerOf(point.x)
    local dy = player.physics.y - centerOf(point.y)

    return math.sqrt(dx * dx + dy * dy)
end

local function goToEscape()
    transitioned = true

    local escape = assert(loadfile("Data/Script/escape.lua", "bt", _ENV))
    escape()
end

local function goToEnd()
    transitioned = true

    local ending = assert(loadfile("Data/Script/end.lua", "bt", _ENV))
    ending()
end

local function setNotice(text, duration)
    noticeText = text
    noticeTimer = duration or 0.8
end

local function findAttackTarget(player, range)
    if player == nil then return nil, nil end

    local bestId = nil
    local bestEntity = nil
    local bestDistance = range + 1.0

    for id, _ in pairs(spawned) do
        local entity = engine.getEntity(id)

        if entity == nil or not entity.isActive or entity.isDestroyed then
            spawned[id] = nil
        else
            local dx = entity.physics.x - player.physics.x
            local dy = entity.physics.y - player.physics.y
            local distance = math.sqrt(dx * dx + dy * dy)
            local forward = dx * player.camera.dirX + dy * player.camera.dirY
            local side = math.abs(dx * player.camera.dirY - dy * player.camera.dirX)

            if forward > 0 and distance <= range and side <= 1.25 and distance < bestDistance then
                if not engine:hasWallBetween(player.physics.x, player.physics.y, entity.physics.x, entity.physics.y) then
                    bestId = id
                    bestEntity = entity
                    bestDistance = distance
                end
            end
        end
    end

    return bestId, bestEntity
end

local function useWeapon(player)
    local state = _G.PlayerState

    if state.weapon == nil then
        setNotice("No Weapon")
        return
    end

    local damage = MELEE_DAMAGE
    local range = 2.0

    if state.weapon == "Gun" then
        if state.ammo <= 0 then
            setNotice("No Ammo")
            return
        end

        state.ammo = state.ammo - 1
        Ui.startWeaponAnimation("Gun")
        damage = GUN_DAMAGE
        range = 8.0
    else
        Ui.startWeaponAnimation("Melee")
    end

    local targetId, target = findAttackTarget(player, range)

    if targetId == nil then
        setNotice(state.weapon == "Gun" and "Miss" or "Swing", 0.4)
        return
    end

    local mem = _G.monster_states[targetId]
    if mem == nil or mem.hp == nil then
        setNotice("No Target")
        return
    end

    mem.hp = math.max(0, mem.hp - damage)
    CombatEffects.markEnemyHit(player, target)

    if mem.hp <= 0 then
        target.isActive = false
        target.isDestroyed = true
        target.physics.velX = 0
        target.physics.velY = 0
        spawned[targetId] = nil
        setNotice("Destroyed", 0.6)
    else
        setNotice("Hit", 0.45)
    end
end

function update(entity, dt, player, control)
    if transitioned or _G.GameState.currentScene ~= "maze" then
        return
    end

    PlayerDamage.tick(dt)
    Ui.tick(dt)

    if DebugHotkey.consume(control) then
        transitioned = true
        return
    end

    if _G.PlayerState.hp <= 0 then
        goToEnd()
        return
    end

    if noticeTimer > 0 then
        noticeTimer = noticeTimer - dt
    else
        noticeText = nil
    end

    local escapePrompt = false

    if player ~= nil and distTo(player, _G.GameState.exit) <= 1.0 then
        escapePrompt = true
    end

    if control ~= nil then
        if control.s_m_key and not mWasDown then
            showFullMap = not showFullMap
        end

        if showFullMap and control.s_menuUp and not menuUpWasDown then
            mapZoom = math.min(2.0, mapZoom + 0.25)
        end

        if showFullMap and control.s_menuDown and not menuDownWasDown then
            mapZoom = math.max(0.75, mapZoom - 0.25)
        end

        if control.s_space and not spaceWasDown then
            useWeapon(player)
        end

        if control.s_enter and not enterWasDown and escapePrompt then
            goToEscape()
            return
        end

        mWasDown = control.s_m_key
        menuUpWasDown = control.s_menuUp
        menuDownWasDown = control.s_menuDown
        spaceWasDown = control.s_space
        enterWasDown = control.s_enter
    end

    Ui.draw(player, showFullMap, escapePrompt, noticeText, mapZoom)
end

if _G.GameState.currentScene ~= "maze" then
    loadMaze()
end
