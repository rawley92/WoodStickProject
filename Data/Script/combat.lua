-- Shared player combat rules.

local CombatEffects = dofile("Data/Script/combat_effects.lua")
local Weapons = dofile("Data/Script/weapons.lua")

local Combat = {}
Combat.__index = Combat

local SIDE_TOLERANCE = 1.25

function Combat.new(options)
    local self = setmetatable({}, Combat)
    options = options or {}

    self.targets = options.targets or {}
    self.onNotice = options.onNotice
    self.startWeaponAnimation = options.startWeaponAnimation

    return self
end

function Combat:setTargets(targets)
    self.targets = targets or {}
end

function Combat:trackMonster(id, value)
    if id ~= nil then
        self.targets[id] = value or true
    end
end

function Combat:untrackMonster(id)
    if id ~= nil then
        self.targets[id] = nil
    end
end

function Combat:notice(text, duration)
    if self.onNotice ~= nil then
        self.onNotice(text, duration)
    end
end

function Combat:findAttackTarget(player, range)
    if player == nil then return nil, nil end

    local bestId = nil
    local bestEntity = nil
    local bestDistance = range + 1.0

    for id, _ in pairs(self.targets) do
        local entity = engine.getEntity(id)

        if entity == nil or not entity.isActive or entity.isDestroyed then
            self.targets[id] = nil
        else
            local dx = entity.physics.x - player.physics.x
            local dy = entity.physics.y - player.physics.y
            local distance = math.sqrt(dx * dx + dy * dy)
            local forward = dx * player.camera.dirX + dy * player.camera.dirY
            local side = math.abs(dx * player.camera.dirY - dy * player.camera.dirX)

            if forward > 0 and distance <= range and side <= SIDE_TOLERANCE and distance < bestDistance then
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

function Combat:useWeapon(player)
    local state = _G.PlayerState
    if state == nil then
        self:notice("No Weapon")
        return false
    end

    local weapon = Weapons.get(state.weapon)
    if weapon == nil then
        self:notice("No Weapon")
        return false
    end

    if weapon.ammoCost ~= nil then
        if (state.ammo or 0) < weapon.ammoCost then
            self:notice("No Ammo")
            return false
        end

        state.ammo = state.ammo - weapon.ammoCost
    end

    if self.startWeaponAnimation ~= nil then
        self.startWeaponAnimation(weapon.animation)
    end

    local targetId, target = self:findAttackTarget(player, weapon.range)

    if targetId == nil then
        self:notice(weapon.missMessage, 0.4)
        return false
    end

    local mem = _G.monster_states ~= nil and _G.monster_states[targetId] or nil
    if mem == nil or mem.hp == nil then
        self:notice("No Target")
        return false
    end

    mem.hp = math.max(0, mem.hp - weapon.damage)
    CombatEffects.markEnemyHit(player, target)

    if mem.hp <= 0 then
        target.isActive = false
        target.isDestroyed = true
        target.physics.velX = 0
        target.physics.velY = 0
        self:untrackMonster(targetId)
        self:notice("Destroyed", 0.6)
    else
        self:notice("Hit", 0.45)
    end

    return true
end

return Combat
