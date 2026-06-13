-- Ammo pickup script.
-- 플레이어가 가까이 오면 탄약을 지급하고 아이템 엔티티를 비활성화한다.

local NAME = "Ammo"
local TEXTURE = "Textures.obj.ammo"
local SCALE = 0.30

-- ObjectSpawn은 빈 assetId로 엔티티를 만들기 때문에 첫 update에서 외형을 확정한다.
local function ensureVisual(entity)
    if entity.assetId == TEXTURE then return end

    entity.name = NAME
    entity.assetId = TEXTURE

    if entity.render ~= nil then
        entity.render.assetId = TEXTURE
        entity.render.scale = SCALE
    end
end

-- Java ScriptManager가 매 프레임 호출하는 아이템 update다.
function update(entity, deltaTime, player, control)
    if not entity.isActive then return end
    ensureVisual(entity)

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    -- 획득 반경 밖이면 아무 상태도 변경하지 않는다.
    if distance >= 1.0 then return end

    -- PlayerState가 누락된 디버그 상황에서도 최소 구조를 보장한다.
    _G.PlayerState = _G.PlayerState or { hp = 100, maxHp = 100, weapon = nil, ammo = 0 }
    _G.PlayerState.ammo = (_G.PlayerState.ammo or 0) + 8

    -- 비활성화하면 Core.update와 SpriteRenderer가 더 이상 이 아이템을 처리하지 않는다.
    entity.isActive = false
end
