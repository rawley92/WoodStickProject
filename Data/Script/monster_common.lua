-- Shared monster behavior helpers.
-- 개별 몬스터 스크립트가 공통으로 쓰는 외형 초기화, 시야 감지, 방향별 스프라이트 선택을 제공한다.

local MonsterCommon = {}

local DEFAULT_FACING_X = -1.0
local DEFAULT_FACING_Y = 0.0

-- 몬스터 엔티티의 공통 외형과 memory 필드를 한 번만 초기화한다.
function MonsterCommon.ensure(entity, mem, config)
    if mem.visualInitialized then return end

    mem.monsterKind = config.kind
    mem.textures = config.textures
    mem.hitAssetId = mem.textures ~= nil and mem.textures.hit or nil
    mem.facingX = mem.facingX or DEFAULT_FACING_X
    mem.facingY = mem.facingY or DEFAULT_FACING_Y
    mem.visualInitialized = true

    -- Java Entity의 public 필드를 직접 바꿔 SpriteRenderer가 읽는 런타임 상태를 확정한다.
    if config.name ~= nil then
        entity.name = config.name
    end

    if entity.render ~= nil and config.scale ~= nil then
        entity.render.scale = config.scale
    end

    if mem.textures ~= nil and (entity.assetId == nil or entity.assetId == "") then
        entity.assetId = mem.textures.front
    end

    if entity.render ~= nil then
        entity.render.assetId = entity.assetId
    end
end

-- 몬스터가 바라보는 방향 벡터를 정규화해 memory에 저장한다.
function MonsterCommon.face(mem, dx, dy, distance)
    if mem == nil then return end

    distance = distance or math.sqrt(dx * dx + dy * dy)
    if distance <= 0.001 then return end

    mem.facingX = dx / distance
    mem.facingY = dy / distance
end

-- 거리, 시야각, 벽 차단 여부를 조합해 플레이어 감지를 판정한다.
function MonsterCommon.canDetectPlayer(entity, player, mem, distance, visionRange, fovDegrees, proximityRange)
    if entity == nil or player == nil or mem == nil then return false end

    -- 아주 가까운 플레이어는 방향과 시야각과 무관하게 감지한다.
    if distance <= (proximityRange or 0.0) then
        return true
    end

    if distance > visionRange then return false end

    if engine ~= nil and engine.hasWallBetween ~= nil then
        -- Java ScriptAPI의 타일 샘플링으로 벽 뒤 플레이어를 제외한다.
        if engine:hasWallBetween(entity.physics.x, entity.physics.y, player.physics.x, player.physics.y) then
            return false
        end
    end

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y

    if distance <= 0.001 then
        return true
    end

    local toPlayerX = dx / distance
    local toPlayerY = dy / distance
    -- dot product가 클수록 플레이어가 몬스터 정면에 가깝다.
    local dot = toPlayerX * (mem.facingX or DEFAULT_FACING_X) + toPlayerY * (mem.facingY or DEFAULT_FACING_Y)
    local minDot = math.cos(math.rad(fovDegrees or 120.0) * 0.5)

    return dot >= minDot
end

-- 플레이어가 보는 방향에 맞춰 몬스터의 front/back/left/right 이미지를 선택한다.
function MonsterCommon.updateSprite(entity, mem, player)
    if entity == nil or mem == nil or player == nil then return end
    if (mem.hitFlashTimer or 0) > 0 then return end

    local textures = mem.textures
    if textures == nil then return end

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance <= 0.001 then
        entity.assetId = textures.front
        return
    end

    local toPlayerX = dx / distance
    local toPlayerY = dy / distance
    local facingX = mem.facingX or DEFAULT_FACING_X
    local facingY = mem.facingY or DEFAULT_FACING_Y
    -- 플레이어가 몬스터의 정면을 보고 있는지, 후면을 보고 있는지 dot product로 구분한다.
    local frontDot = toPlayerX * facingX + toPlayerY * facingY
    local nextAsset = nil

    if frontDot >= 0.5 then
        nextAsset = textures.front
    elseif frontDot <= -0.5 then
        nextAsset = textures.back
    else
        -- 정면/후면이 아니면 2D cross product 부호로 좌/우를 고른다.
        local cross = facingX * toPlayerY - facingY * toPlayerX
        nextAsset = cross >= 0 and textures.left or textures.right
    end

    if nextAsset ~= nil then
        entity.assetId = nextAsset
        if entity.render ~= nil then
            entity.render.assetId = nextAsset
        end
    end
end

return MonsterCommon
