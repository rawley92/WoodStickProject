local NAME = "Health"
local TEXTURE = "Textures.obj.healpack"
local SCALE = 0.45

local function ensureVisual(entity)
    if entity.assetId == TEXTURE then return end

    entity.name = NAME
    entity.assetId = TEXTURE

    if entity.render ~= nil then
        entity.render.assetId = TEXTURE
        entity.render.scale = SCALE
    end
end

function update_item_heal(entity, deltaTime, player)
    if not entity.isActive then return end
    ensureVisual(entity)

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance < 1.0 then
        entity.isActive = false 

        _G.PlayerState = _G.PlayerState or { hp = 100, maxHp = 100, weapon = nil, ammo = 0 }

        _G.PlayerState.hp = _G.PlayerState.hp + 20
        if _G.PlayerState.hp > _G.PlayerState.maxHp then
            _G.PlayerState.hp = _G.PlayerState.maxHp
        end

        print("구급상자 획득! 현재 체력: " .. _G.PlayerState.hp .. " / " .. _G.PlayerState.maxHp)
    end
end

function update(entity, deltaTime, player, control)
    update_item_heal(entity, deltaTime, player)
end
