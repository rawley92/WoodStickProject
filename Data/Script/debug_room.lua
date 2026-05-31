local Ui = dofile("Data/Script/ui.lua")
local PlayerDamage = dofile("Data/Script/player_damage.lua")
local CombatEffects = dofile("Data/Script/combat_effects.lua")
local ObjectSpawn = dofile("Data/Script/object_spawn.lua")

local MAP_SIZE = 31
local WALL = 1
local PATH = 0
local SPAWN_DISTANCE = 4.0
local MELEE_DAMAGE = 5
local GUN_DAMAGE = 7

local enterWasDown = false
local upWasDown = false
local downWasDown = false
local uWasDown = false
local spaceWasDown = false
local menuOpen = false
local selected = 1
local noticeText = nil
local noticeTimer = 0
local spawned = {}

local SPAWN_ENTRIES = {
    { label = "Spider", type = "enemy_spider" },
    { label = "Bull", type = "enemy_bull" },
    { label = "Ghost", type = "enemy_ghost" },
    { label = "Heal Pack", type = "item_health" },
    { label = "Gun", type = "weapon_gun" },
    { label = "Melee", type = "weapon_melee" }
}

local function clamp(value, minValue, maxValue)
    return math.max(minValue, math.min(maxValue, value))
end

local function setNotice(text)
    noticeText = text
    noticeTimer = 0.9
end

local function buildDebugMaze()
    local map = {}

    for y = 1, MAP_SIZE do
        map[y] = {}

        for x = 1, MAP_SIZE do
            if x == 1 or y == 1 or x == MAP_SIZE or y == MAP_SIZE then
                map[y][x] = WALL
            else
                map[y][x] = PATH
            end
        end
    end

    return {
        map = map,
        width = MAP_SIZE,
        height = MAP_SIZE,
        WALL = WALL,
        PATH = PATH
    }
end

local function setupWorld()
    engine.assignWallTexture("Textures.Level.Wall_1")
    engine.setFloorTexture("Textures.Level.Floor_1")
    engine.setCeilingTexture("Textures.Level.Celling_1")
end

local function resetDebugState()
    _G.GameState = _G.GameState or {}
    _G.GameState.currentScene = "debug_room"
    _G.GameState.debugNoDeath = true
    _G.GameState.maze = buildDebugMaze()
    _G.GameState.exit = nil
    _G.monster_states = {}
    _G.PlayerState = {
        hp = 100,
        maxHp = 100,
        invincibleTimer = 0,
        damageFlashTimer = 0,
        crosshairHitTimer = 0,
        weaponFireFrames = 0,
        weaponSwingTimer = 0,
        weaponSwingDuration = 0.5,
        weapon = "Melee",
        ammo = 0
    }

    enterWasDown = false
    upWasDown = false
    downWasDown = false
    uWasDown = false
    spaceWasDown = false
    menuOpen = false
    selected = 1
    noticeText = nil
    noticeTimer = 0
    spawned = {}
end

local function spawnController()
    engine.spawnEntity("DebugRoom_Controller", "", 0.0, 0.0, "Script.debug_room")
end

local function loadDebugRoom()
    resetDebugState()

    engine.initScene("Debug Room", "Level.test.map")
    setupWorld()
    engine.setupPlayer(15.5, 15.5, -1.0, 0.0, 0.0, 0.88)
    spawnController()
end

local function goToTitle()
    _G.GameState.debugNoDeath = false

    local title = assert(loadfile("Data/Script/title.lua", "bt", _ENV))
    title()
end

local function spawnPoint(player)
    local x = player.physics.x + player.camera.dirX * SPAWN_DISTANCE
    local y = player.physics.y + player.camera.dirY * SPAWN_DISTANCE

    return clamp(x, 1.5, MAP_SIZE - 1.5), clamp(y, 1.5, MAP_SIZE - 1.5)
end

local function spawnSelected(player)
    local entry = SPAWN_ENTRIES[selected]
    local x, y = spawnPoint(player)
    local id = ObjectSpawn.spawn(entry.type, x, y)

    if ObjectSpawn.isMonster(entry.type) and id ~= nil then
        spawned[id] = entry.label
    end

    setNotice("Spawned " .. entry.label)
end

local function findAttackTarget(player, range)
    local bestId = nil
    local bestEntity = nil
    local bestDistance = range + 1.0

    for id, label in pairs(spawned) do
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

local function attack(player)
    local state = _G.PlayerState
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
        setNotice("Miss")
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
        setNotice("Destroyed")
    else
        setNotice("Hit")
    end
end

local function updateNotice(dt)
    if noticeTimer > 0 then
        noticeTimer = noticeTimer - dt
    else
        noticeText = nil
    end
end

local function keepPlayerAlive()
    if _G.PlayerState ~= nil and _G.PlayerState.hp <= 0 then
        _G.PlayerState.hp = 1
    end
end

local function drawDebugMenu()
    engine.uiRect(872, 64, 340, 392, 0x050505, 0.86)
    engine.uiText("DEBUG SPAWN", 900, 92, 30, 0xFFFFFF, 1.0)

    for i, entry in ipairs(SPAWN_ENTRIES) do
        local y = 142 + (i - 1) * 42
        local color = i == selected and 0x80FF72 or 0xCCCCCC
        local prefix = i == selected and "> " or "  "

        engine.uiText(prefix .. entry.label, 904, y, 26, color, 1.0)
    end

    engine.uiText("Up/Down select", 904, 408, 20, 0x888888, 1.0)
    engine.uiText("Enter spawn", 904, 432, 20, 0x888888, 1.0)
end

local function draw(player)
    Ui.draw(player, false, false, noticeText, 1.0)
    engine.uiText("DEBUG ROOM", 48, 48, 30, 0xFFFFFF, 1.0)
    engine.uiText("U Menu", 48, 84, 22, 0xAAAAAA, 1.0)
    engine.uiText("Space Attack", 48, 110, 22, 0xAAAAAA, 1.0)

    if menuOpen then
        drawDebugMenu()
    end
end

function update(entity, dt, player, control)
    PlayerDamage.tick(dt)
    Ui.tick(dt)
    updateNotice(dt)
    keepPlayerAlive()

    if control ~= nil then
        if control.s_u_key and not uWasDown then
            menuOpen = not menuOpen
        end

        if menuOpen then
            if control.s_menuUp and not upWasDown then
                selected = selected == 1 and #SPAWN_ENTRIES or selected - 1
            end

            if control.s_menuDown and not downWasDown then
                selected = selected == #SPAWN_ENTRIES and 1 or selected + 1
            end

            if control.s_enter and not enterWasDown then
                spawnSelected(player)
            end
        elseif control.s_enter and not enterWasDown then
            goToTitle()
            return
        end

        if control.s_space and not spaceWasDown then
            attack(player)
        end

        upWasDown = control.s_menuUp
        downWasDown = control.s_menuDown
        enterWasDown = control.s_enter
        uWasDown = control.s_u_key
        spaceWasDown = control.s_space
    end

    keepPlayerAlive()
    draw(player)
end

if _G.GameState == nil or _G.GameState.currentScene ~= "debug_room" then
    loadDebugRoom()
end
