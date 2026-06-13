-- Player damage module.
-- 몬스터 공격으로 인한 체력 감소, 무적 시간, 피격 플래시, 넉백을 공통 처리한다.

local PlayerDamage = {}

local INVINCIBLE_TIME = 2.0
local KNOCKBACK_DISTANCE = 0.2
local DAMAGE_FLASH_TIME = 0.22

-- 디버그 무적 모드에서는 HP가 1 아래로 내려가지 않게 한다.
local function minHp()
    if _G.GameState ~= nil and _G.GameState.debugNoDeath then
        return 1
    end

    return 0
end

-- 공격자에서 플레이어를 밀어내는 넉백 방향을 계산한다.
local function knockbackDirection(player, attacker)
    local dx = 1.0
    local dy = 0.0

    if player ~= nil and attacker ~= nil then
        dx = player.physics.x - attacker.physics.x
        dy = player.physics.y - attacker.physics.y
    end

    local distance = math.sqrt(dx * dx + dy * dy)

    if distance <= 0.001 then
        -- 공격자와 플레이어가 같은 좌표면 카메라 반대 방향을 fallback 넉백 방향으로 사용한다.
        if player ~= nil and player.camera ~= nil then
            dx = -player.camera.dirX
            dy = -player.camera.dirY
        else
            dx = 1.0
            dy = 0.0
        end

        distance = math.sqrt(dx * dx + dy * dy)
    end

    return dx / distance, dy / distance
end

-- 플레이어 관련 전투 피드백 타이머를 매 프레임 감소시킨다.
function PlayerDamage.tick(dt)
    if _G.PlayerState == nil then return end

    local state = _G.PlayerState

    state.invincibleTimer = math.max(0, (state.invincibleTimer or 0) - dt)
    state.damageFlashTimer = math.max(0, (state.damageFlashTimer or 0) - dt)
    state.crosshairHitTimer = math.max(0, (state.crosshairHitTimer or 0) - dt)
end

-- 플레이어에게 피해를 적용한다.
-- 무적 시간이 남아 있으면 같은 공격 범위 안에서 매 프레임 피해가 반복되는 것을 막는다.
function PlayerDamage.tryHit(player, attacker, amount)
    if _G.PlayerState == nil then return false end
    if (_G.PlayerState.invincibleTimer or 0) > 0 then return false end

    -- HP 감소와 동시에 UI가 읽을 피격 효과 타이머를 설정한다.
    _G.PlayerState.hp = math.max(minHp(), _G.PlayerState.hp - amount)
    _G.PlayerState.invincibleTimer = INVINCIBLE_TIME
    _G.PlayerState.damageFlashTimer = DAMAGE_FLASH_TIME
    _G.PlayerState.damageFlashDuration = DAMAGE_FLASH_TIME

    if player ~= nil and player.physics ~= nil then
        local dirX, dirY = knockbackDirection(player, attacker)
        -- 실제 위치 반영과 벽 충돌은 Java PhysicsCore가 다음 프레임에서 처리한다.
        player.physics.velX = player.physics.velX + dirX * KNOCKBACK_DISTANCE
        player.physics.velY = player.physics.velY + dirY * KNOCKBACK_DISTANCE
    end

    return true
end

return PlayerDamage
