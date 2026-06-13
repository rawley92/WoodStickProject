-- Health pickup script.
-- 플레이어가 가까이 오면 HP를 회복하고 구급상자 엔티티를 비활성화한다.

local NAME = "Health"
local TEXTURE = "Textures.obj.healpack"
local SCALE = 0.45

-- 첫 update에서 Java 엔티티의 이름, assetId, render scale을 실제 아이템 값으로 맞춘다.
local function ensureVisual(entity)
    if entity.assetId == TEXTURE then return end

    entity.name = NAME
    entity.assetId = TEXTURE

    if entity.render ~= nil then
        entity.render.assetId = TEXTURE
        entity.render.scale = SCALE
    end
end

-- 회복 아이템의 실제 획득 처리를 수행한다.
function update_item_heal(entity, deltaTime, player)
    if not entity.isActive then return end
    ensureVisual(entity)

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance < 1.0 then
        -- 획득 즉시 비활성화해 중복 회복을 막는다.
        entity.isActive = false 

        _G.PlayerState = _G.PlayerState or { hp = 100, maxHp = 100, weapon = nil, ammo = 0 }

        -- maxHp를 넘지 않도록 수동 clamp한다.
        _G.PlayerState.hp = _G.PlayerState.hp + 20
        if _G.PlayerState.hp > _G.PlayerState.maxHp then
            _G.PlayerState.hp = _G.PlayerState.maxHp
        end

        print("구급상자 획득! 현재 체력: " .. _G.PlayerState.hp .. " / " .. _G.PlayerState.maxHp)
    end
end

-- ScriptManager가 찾는 표준 update 이름으로 회복 로직을 연결한다.
function update(entity, deltaTime, player, control)
    update_item_heal(entity, deltaTime, player)
end
