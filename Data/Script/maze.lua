-- Maze play scene controller.
-- 생성된 map.dat와 GameState.mazeObjects를 실제 플레이 씬으로 구성하고, 전투/탈출/HUD를 매 프레임 관리한다.

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

-- map/object 좌표는 타일 번호 기준 1-based이고, Java 물리는 타일 중심의 월드 좌표를 사용한다.
local function centerOf(tile)
    return tile - 0.5
end

-- 미로 씬 공통 텍스처를 Java Texture 서비스에 바인딩한다.
local function setupWorld()
    engine.assignWallTexture("Textures.Level.Wall_1")
    engine.setFloorTexture("Textures.Level.Floor_1")
    engine.setCeilingTexture("Textures.Level.Celling_1")
end

-- 이 스크립트의 update를 매 프레임 호출하게 할 controller 엔티티를 만든다.
local function spawnController()
    engine.spawnEntity("Maze_Controller", "", 0.0, 0.0, "Script.maze")
end

-- ObjectSpawner가 만든 데이터 레코드를 실제 Java Entity와 Lua scriptPath로 변환한다.
local function spawnMazeObject(object)
    local x, y = centerOf(object.x), centerOf(object.y)
    
    -- 시작점과 출구는 엔티티가 아니라 GameState 좌표로만 사용한다.
    if object.type == "player_start" or object.type == "exit" then return end

    local id = ObjectSpawn.spawn(object.type, x, y)

    -- 공격 대상 검색은 몬스터 ID만 별도 테이블에 보관해 빠르게 순회한다.
    if ObjectSpawn.isMonster(object.type) and id ~= nil then
        spawned[id] = true
    end
end

-- 미로 플레이 시작 시 플레이어 전투 상태를 초기값으로 되돌린다.
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

-- 생성된 미로 파일을 Java Scene으로 로드하고 플레이 가능한 엔티티들을 배치한다.
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

    -- start는 타일 좌표이므로 플레이어 위치에는 타일 중심 월드 좌표를 넣는다.
    engine.setupPlayer(centerOf(start.x), centerOf(start.y), -1.0, 0.0, 0.0, 0.88)

    for _, object in ipairs(_G.GameState.mazeObjects or {}) do
        spawnMazeObject(object)
    end

    spawnController()
end

-- 플레이어와 특정 타일 포인트 사이의 월드 거리 값을 계산한다.
local function distTo(player, point)
    if point == nil then return 9999 end

    local dx = player.physics.x - centerOf(point.x)
    local dy = player.physics.y - centerOf(point.y)

    return math.sqrt(dx * dx + dy * dy)
end

-- 탈출 성공 씬으로 전환한다.
local function goToEscape()
    transitioned = true

    local escape = assert(loadfile("Data/Script/escape.lua", "bt", _ENV))
    escape()
end

-- 사망/게임오버 씬으로 전환한다.
local function goToEnd()
    transitioned = true

    local ending = assert(loadfile("Data/Script/end.lua", "bt", _ENV))
    ending()
end

-- 짧게 표시할 전투/상호작용 알림을 설정한다.
local function setNotice(text, duration)
    noticeText = text
    noticeTimer = duration or 0.8
end

-- 플레이어 전방의 가장 가까운 몬스터를 공격 대상으로 찾는다.
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

            -- forward는 플레이어가 바라보는 방향으로 얼마나 앞에 있는지 나타내는 dot product다.
            local forward = dx * player.camera.dirX + dy * player.camera.dirY

            -- side는 전방 벡터에 수직인 축으로 떨어진 거리다. 값이 작을수록 조준선에 가깝다.
            local side = math.abs(dx * player.camera.dirY - dy * player.camera.dirX)

            if forward > 0 and distance <= range and side <= 1.25 and distance < bestDistance then
                -- 벽 뒤의 적은 근접/원거리 공격 대상으로 잡지 않는다.
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

-- 현재 장착 무기에 따라 탄약 소비, 공격 범위, 데미지, 피격 효과를 처리한다.
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

    -- 몬스터의 실제 HP는 monster_* 스크립트가 _G.monster_states에 저장한다.
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

-- Maze_Controller 엔티티가 매 프레임 호출하는 플레이 씬 진입점이다.
function update(entity, dt, player, control)
    if transitioned or _G.GameState.currentScene ~= "maze" then
        return
    end

    PlayerDamage.tick(dt)
    Ui.tick(dt)

    if DebugHotkey.consume(control) then
        -- 디버그 룸 전환이 발생하면 이 씬의 update가 더 이상 진행되지 않게 막는다.
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
        -- 출구는 별도 엔티티가 아니라 GameState.exit 좌표로만 판정한다.
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

        -- 아래 wasDown 플래그들은 키를 누르고 있는 동안 액션이 반복 발동되는 것을 막는다.
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
