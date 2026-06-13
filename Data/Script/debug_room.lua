-- Debug room scene controller.
-- O 키로 진입하는 테스트용 씬이며, 몬스터/아이템 스폰과 공격 판정을 빠르게 확인하기 위해 사용한다.

local Ui = dofile("Data/Script/ui.lua")
local PlayerDamage = dofile("Data/Script/player_damage.lua")
local Combat = dofile("Data/Script/combat.lua")
local ObjectSpawn = dofile("Data/Script/object_spawn.lua")

local MAP_SIZE = 31
local WALL = 1
local PATH = 0
local SPAWN_DISTANCE = 4.0

-- 메뉴 입력은 edge-trigger 방식으로 처리하기 위해 이전 프레임 상태를 보관한다.
local enterWasDown = false
local upWasDown = false
local downWasDown = false
local uWasDown = false
local pWasDown = false
local spaceWasDown = false
local menuOpen = false
local selected = 1
local noticeText = nil
local noticeTimer = 0
local spawned = {}
local combat = nil

-- 디버그 메뉴에서 선택 가능한 스폰 타입 목록이다.
local SPAWN_ENTRIES = {
    { label = "Spider", type = "enemy_spider" },
    { label = "Bull", type = "enemy_bull" },
    { label = "Ghost", type = "enemy_ghost" },
    { label = "Heal Pack", type = "item_health" },
    { label = "Melee", type = "weapon_melee" },
    { label = "Gun", type = "weapon_gun" },
    { label = "Ammo", type = "item_ammo" }
}

-- 디버그 좌표 계산 결과를 테스트 방 안쪽으로 제한한다.
local function clamp(value, minValue, maxValue)
    return math.max(minValue, math.min(maxValue, value))
end

-- 짧은 디버그 알림 메시지를 설정한다.
local function setNotice(text, duration)
    noticeText = text
    noticeTimer = duration or 0.8
end

combat = Combat.new({
    targets = spawned,
    onNotice = function(text, duration)
        setNotice(text, duration)
    end,
    startWeaponAnimation = function(weaponName)
        Ui.startWeaponAnimation(weaponName)
    end
})

-- UI 미니맵 표시용 단순 사각형 테스트 맵 데이터를 만든다.
local function buildDebugMaze()
    local map = {}

    for y = 1, MAP_SIZE do
        map[y] = {}

        for x = 1, MAP_SIZE do
            if x == 1 or y == 1 or x == MAP_SIZE or y == MAP_SIZE then
                -- 가장자리만 벽으로 막고 내부는 모두 이동 가능한 PATH로 둔다.
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

-- 디버그 씬 렌더링에 사용할 텍스처를 바인딩한다.
local function setupWorld()
    engine.assignWallTexture("Textures.Level.Wall_1")
    engine.setFloorTexture("Textures.Level.Floor_1")
    engine.setCeilingTexture("Textures.Level.Celling_1")
end

-- 디버그 룸 진입 시 전역 게임 상태와 입력/스폰 상태를 초기화한다.
local function resetDebugState()
    _G.GameState = _G.GameState or {}
    _G.GameState.currentScene = "debug_room"
    -- 테스트 중 사망으로 씬이 끝나지 않게 PlayerDamage.minHp가 참조하는 플래그를 켠다.
    _G.GameState.debugNoDeath = true
    _G.GameState.noClip = false
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
    pWasDown = false
    spaceWasDown = false
    menuOpen = false
    selected = 1
    noticeText = nil
    noticeTimer = 0
    spawned = {}
    if combat ~= nil then
        combat:setTargets(spawned)
    end
end

-- 디버그 룸 update를 담당할 controller 엔티티를 생성한다.
local function spawnController()
    engine.spawnEntity("DebugRoom_Controller", "", 0.0, 0.0, "Script.debug_room")
end

-- Java Scene을 테스트 맵으로 초기화하고 플레이어를 중앙에 배치한다.
local function loadDebugRoom()
    resetDebugState()

    engine.initScene("Debug Room", "Level.test.map")
    setupWorld()
    engine.setupPlayer(15.5, 15.5, -1.0, 0.0, 0.0, 0.88)
    spawnController()
end

-- 디버그 무적 플래그를 끄고 타이틀로 돌아간다.
local function goToTitle()
    _G.GameState.debugNoDeath = false

    local title = assert(loadfile("Data/Script/title.lua", "bt", _ENV))
    title()
end

-- 플레이어 전방 일정 거리의 스폰 좌표를 계산한다.
local function spawnPoint(player)
    local x = player.physics.x + player.camera.dirX * SPAWN_DISTANCE
    local y = player.physics.y + player.camera.dirY * SPAWN_DISTANCE

    -- 벽 바깥에 스폰되지 않도록 테스트 맵 내부 좌표로 제한한다.
    return clamp(x, 1.5, MAP_SIZE - 1.5), clamp(y, 1.5, MAP_SIZE - 1.5)
end

-- 현재 메뉴 선택 항목을 플레이어 앞에 스폰한다.
local function spawnSelected(player)
    local entry = SPAWN_ENTRIES[selected]
    local x, y = spawnPoint(player)
    local id = ObjectSpawn.spawn(entry.type, x, y)

    -- 몬스터만 공격 대상 추적 테이블에 등록한다.
    if ObjectSpawn.isMonster(entry.type) and id ~= nil then
        combat:trackMonster(id, entry.label)
    end

    setNotice("Spawned " .. entry.label)
end

-- 디버그 룸에서 스폰한 몬스터 중 플레이어 전방의 공격 대상을 찾는다.
--[[
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
            -- maze.lua의 공격 판정과 동일하게 전방 거리와 측면 오차를 함께 검사한다.
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

-- 디버그 룸 전용 공격 처리다.
-- 실제 플레이의 maze.lua useWeapon과 거의 같은 구조를 사용해 몬스터 테스트 결과가 일관되게 한다.
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

    -- 몬스터 HP는 각 monster_* 스크립트가 _G.monster_states에 저장한다.
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

-- 디버그 알림 타이머를 감소시킨다.
]]
local function updateNotice(dt)
    if noticeTimer > 0 then
        noticeTimer = noticeTimer - dt
    else
        noticeText = nil
    end
end

-- 디버그 룸에서는 HP가 0 이하가 되어도 계속 테스트할 수 있게 보정한다.
local function keepPlayerAlive()
    if _G.PlayerState ~= nil and _G.PlayerState.hp <= 0 then
        _G.PlayerState.hp = 1
    end
end

-- 스폰 메뉴 패널을 그린다.
local function applyNoClip(player)
    if player == nil or player.physics == nil then
        return
    end

    _G.GameState = _G.GameState or {}
    if _G.GameState.noClip == nil then
        _G.GameState.noClip = false
    end

    player.physics.noClip = _G.GameState.noClip
end

local function toggleNoClip(player)
    _G.GameState = _G.GameState or {}
    _G.GameState.noClip = not (_G.GameState.noClip == true)
    applyNoClip(player)
    setNotice(_G.GameState.noClip and "Noclip ON" or "Noclip OFF")
end

local function drawDebugMenu()
    engine.uiRect(872, 64, 340, 434, 0x050505, 0.86)
    engine.uiText("DEBUG SPAWN", 900, 92, 30, 0xFFFFFF, 1.0)

    for i, entry in ipairs(SPAWN_ENTRIES) do
        local y = 142 + (i - 1) * 42
        local color = i == selected and 0x80FF72 or 0xCCCCCC
        local prefix = i == selected and "> " or "  "

        engine.uiText(prefix .. entry.label, 904, y, 26, color, 1.0)
    end

    local noClipText = _G.GameState ~= nil and _G.GameState.noClip and "P Noclip: ON" or "P Noclip: OFF"
    local noClipColor = _G.GameState ~= nil and _G.GameState.noClip and 0x80FF72 or 0x888888

    engine.uiText(noClipText, 904, 426, 20, noClipColor, 1.0)
    engine.uiText("Up/Down select", 904, 450, 20, 0x888888, 1.0)
    engine.uiText("Enter spawn", 904, 474, 20, 0x888888, 1.0)
end

-- 기본 HUD 위에 디버그 전용 안내와 스폰 메뉴를 추가로 그린다.
local function draw(player)
    Ui.draw(player, false, false, noticeText, 1.0)
    engine.uiText("DEBUG ROOM", 48, 48, 30, 0xFFFFFF, 1.0)
    engine.uiText("U Menu", 48, 84, 22, 0xAAAAAA, 1.0)
    engine.uiText("Space Attack", 48, 110, 22, 0xAAAAAA, 1.0)

    if menuOpen then
        drawDebugMenu()
    end
end

-- DebugRoom_Controller가 매 프레임 호출하는 update다.
function update(entity, dt, player, control)
    PlayerDamage.tick(dt)
    Ui.tick(dt)
    updateNotice(dt)
    keepPlayerAlive()
    applyNoClip(player)

    if control ~= nil then
        if control.s_p_key and not pWasDown then
            toggleNoClip(player)
        end

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
            combat:useWeapon(player)
        end

        -- 입력 반복 방지를 위해 현재 프레임 키 상태를 다음 프레임 비교용으로 저장한다.
        upWasDown = control.s_menuUp
        downWasDown = control.s_menuDown
        enterWasDown = control.s_enter
        uWasDown = control.s_u_key
        pWasDown = control.s_p_key
        spaceWasDown = control.s_space
    end

    keepPlayerAlive()
    draw(player)
end

if _G.GameState == nil or _G.GameState.currentScene ~= "debug_room" then
    loadDebugRoom()
end
