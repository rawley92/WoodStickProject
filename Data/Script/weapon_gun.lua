-- Gun pickup script.
-- 플레이어가 가까이 오면 장착 무기를 Gun으로 바꾸고 기본 탄약을 지급한다.

local NAME = "Weapon_Gun"
local TEXTURE = "Textures.UI.Weapon.Gun.gun_fps"
local SCALE = 0.28
local AMMO_ON_PICKUP = 12

-- 첫 update에서 빈 assetId를 실제 총 아이템 외형으로 바꾼다.
local function ensureVisual(entity)
    if entity.assetId == TEXTURE then return end

    entity.name = NAME
    entity.assetId = TEXTURE

    if entity.render ~= nil then
        entity.render.assetId = TEXTURE
        entity.render.scale = SCALE
    end
end

-- 플레이어 접근 거리 기반으로 총 획득을 처리한다.
function update(entity, dt, player, control)
    if not entity.isActive then return end
    ensureVisual(entity)

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance >= 1.0 then return end

    -- 실제 전투 판정은 maze.lua의 useWeapon에서 PlayerState.weapon을 읽어 처리한다.
    _G.PlayerState.weapon = "Gun"
    _G.PlayerState.ammo = AMMO_ON_PICKUP
    entity.isActive = false
end
