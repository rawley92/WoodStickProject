-- Combat feedback module.
-- 공격 명중 시 크로스헤어 피드백, 적 피격 스프라이트, 짧은 넉백을 공통 처리한다.

local CombatEffects = {}

local ENEMY_HIT_FLASH_TIME = 0.14
local CROSSHAIR_HIT_TIME = 0.18
local ENEMY_KNOCKBACK_DISTANCE = 0.1

-- 플레이어에서 타겟으로 향하는 단위 벡터를 계산한다.
local function hitDirection(player, target)
    local dx = 1.0
    local dy = 0.0

    if player ~= nil and target ~= nil then
        dx = target.physics.x - player.physics.x
        dy = target.physics.y - player.physics.y
    end

    local distance = math.sqrt(dx * dx + dy * dy)

    if distance <= 0.001 then
        -- 두 좌표가 거의 같으면 방향을 계산할 수 없으므로 플레이어 시선 방향을 fallback으로 사용한다.
        if player ~= nil and player.camera ~= nil then
            dx = player.camera.dirX
            dy = player.camera.dirY
        else
            dx = 1.0
            dy = 0.0
        end

        distance = math.sqrt(dx * dx + dy * dy)
    end

    return dx / distance, dy / distance
end

-- 공격 성공 시 UI 피드백과 적 피격 상태를 기록한다.
function CombatEffects.markEnemyHit(player, target)
    if _G.PlayerState ~= nil then
        -- UI.updateCrosshair가 이 타이머를 보고 피격 크로스헤어 텍스처로 전환한다.
        _G.PlayerState.crosshairHitTimer = CROSSHAIR_HIT_TIME
        _G.PlayerState.crosshairHitDuration = CROSSHAIR_HIT_TIME
    end

    if target == nil or target.physics == nil then return end

    local mem = _G.monster_states ~= nil and _G.monster_states[target.entityId] or nil

    if mem ~= nil then
        local hitAssetId = mem.hitAssetId

        if hitAssetId ~= nil and target.assetId ~= hitAssetId then
            -- 피격 스프라이트를 잠깐 보여주기 위해 기존 assetId를 기억한다.
            mem.assetBeforeHit = mem.assetBeforeHit or target.assetId
            target.assetId = hitAssetId
            if target.render ~= nil then
                target.render.assetId = hitAssetId
            end
            mem.hitFlashTimer = ENEMY_HIT_FLASH_TIME
        end
    end

    local dirX, dirY = hitDirection(player, target)
    local nextX = target.physics.x + dirX * ENEMY_KNOCKBACK_DISTANCE
    local nextY = target.physics.y + dirY * ENEMY_KNOCKBACK_DISTANCE

    if engine ~= nil and engine.hasWallBetween ~= nil then
        -- 넉백 위치까지 벽이 있으면 적을 벽 너머로 밀지 않는다.
        if engine:hasWallBetween(target.physics.x, target.physics.y, nextX, nextY) then
            return
        end
    end

    target.physics.x = nextX
    target.physics.y = nextY
end

-- 피격 플래시 타이머를 갱신하고 시간이 끝나면 원래 스프라이트로 되돌린다.
function CombatEffects.tickEnemy(entity, mem, dt)
    if entity == nil or mem == nil then return end

    if (mem.hitFlashTimer or 0) <= 0 then return end

    mem.hitFlashTimer = math.max(0, mem.hitFlashTimer - dt)

    if mem.hitFlashTimer <= 0 and mem.assetBeforeHit ~= nil then
        -- render.assetId도 함께 되돌려 SpriteRenderer가 즉시 원래 이미지를 읽게 한다.
        entity.assetId = mem.assetBeforeHit
        if entity.render ~= nil then
            entity.render.assetId = mem.assetBeforeHit
        end
        mem.assetBeforeHit = nil
    end
end

return CombatEffects
