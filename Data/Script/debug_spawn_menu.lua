-- Shared debug spawn menu overlay.

local ObjectSpawn = dofile("Data/Script/object_spawn.lua")

local DebugSpawnMenu = {}
DebugSpawnMenu.__index = DebugSpawnMenu

local SPAWN_DISTANCE = 4.0

local SPAWN_ENTRIES = {
    { label = "Spider", type = "enemy_spider" },
    { label = "Bull", type = "enemy_bull" },
    { label = "Ghost", type = "enemy_ghost" },
    { label = "Heal Pack", type = "item_health" },
    { label = "Melee", type = "weapon_melee" },
    { label = "Gun", type = "weapon_gun" },
    { label = "Ammo", type = "item_ammo" }
}

local function clamp(value, minValue, maxValue)
    return math.max(minValue, math.min(maxValue, value))
end

local function getBounds()
    local maze = _G.GameState ~= nil and _G.GameState.maze or nil

    if maze ~= nil and maze.width ~= nil and maze.height ~= nil then
        return 1.5, maze.width - 1.5, 1.5, maze.height - 1.5
    end

    return 1.5, 79.5, 1.5, 79.5
end

function DebugSpawnMenu.new(options)
    local self = setmetatable({}, DebugSpawnMenu)
    options = options or {}

    self.onSpawned = options.onSpawned
    self.onNotice = options.onNotice
    self:reset()

    return self
end

function DebugSpawnMenu:reset()
    self.open = false
    self.selected = 1
    self.enterWasDown = false
    self.upWasDown = false
    self.downWasDown = false
    self.uWasDown = false
    self.pWasDown = false
end

function DebugSpawnMenu:isOpen()
    return self.open
end

function DebugSpawnMenu:notice(text)
    if self.onNotice ~= nil then
        self.onNotice(text)
    end
end

function DebugSpawnMenu:spawnPoint(player)
    if player == nil or player.physics == nil or player.camera == nil then
        return nil, nil
    end

    local x = player.physics.x + player.camera.dirX * SPAWN_DISTANCE
    local y = player.physics.y + player.camera.dirY * SPAWN_DISTANCE
    local minX, maxX, minY, maxY = getBounds()

    return clamp(x, minX, maxX), clamp(y, minY, maxY)
end

function DebugSpawnMenu:spawnSelected(player)
    local entry = SPAWN_ENTRIES[self.selected]
    local x, y = self:spawnPoint(player)

    if x == nil or y == nil then
        self:notice("No Player")
        return
    end

    local id = ObjectSpawn.spawn(entry.type, x, y)

    if id ~= nil and self.onSpawned ~= nil then
        self.onSpawned(id, entry.label, entry.type)
    end

    self:notice("Spawned " .. entry.label)
end

function DebugSpawnMenu:applyNoClip(player)
    if player == nil or player.physics == nil then
        return
    end

    _G.GameState = _G.GameState or {}
    if _G.GameState.noClip == nil then
        _G.GameState.noClip = false
    end

    player.physics.noClip = _G.GameState.noClip
end

function DebugSpawnMenu:toggleNoClip(player)
    _G.GameState = _G.GameState or {}
    _G.GameState.noClip = not (_G.GameState.noClip == true)
    self:applyNoClip(player)

    self:notice(_G.GameState.noClip and "Noclip ON" or "Noclip OFF")
end

function DebugSpawnMenu:update(control, player)
    if control == nil then
        return false
    end

    local consumed = false
    self:applyNoClip(player)

    if control.s_u_key and not self.uWasDown then
        self.open = not self.open
        consumed = true
    end

    if control.s_p_key and not self.pWasDown then
        self:toggleNoClip(player)
        consumed = true
    end

    if self.open then
        if control.s_menuUp and not self.upWasDown then
            self.selected = self.selected == 1 and #SPAWN_ENTRIES or self.selected - 1
            consumed = true
        end

        if control.s_menuDown and not self.downWasDown then
            self.selected = self.selected == #SPAWN_ENTRIES and 1 or self.selected + 1
            consumed = true
        end

        if control.s_enter and not self.enterWasDown then
            self:spawnSelected(player)
            consumed = true
        end
    end

    self.upWasDown = control.s_menuUp
    self.downWasDown = control.s_menuDown
    self.enterWasDown = control.s_enter
    self.uWasDown = control.s_u_key
    self.pWasDown = control.s_p_key

    return consumed
end

function DebugSpawnMenu:draw()
    if not self.open then
        return
    end

    engine.uiRect(872, 64, 340, 434, 0x050505, 0.86)
    engine.uiText("DEBUG SPAWN", 900, 92, 30, 0xFFFFFF, 1.0)

    for i, entry in ipairs(SPAWN_ENTRIES) do
        local y = 142 + (i - 1) * 42
        local color = i == self.selected and 0x80FF72 or 0xCCCCCC
        local prefix = i == self.selected and "> " or "  "

        engine.uiText(prefix .. entry.label, 904, y, 26, color, 1.0)
    end

    local noClipText = _G.GameState ~= nil and _G.GameState.noClip and "P Noclip: ON" or "P Noclip: OFF"
    local noClipColor = _G.GameState ~= nil and _G.GameState.noClip and 0x80FF72 or 0x888888

    engine.uiText(noClipText, 904, 426, 20, noClipColor, 1.0)
    engine.uiText("Up/Down select", 904, 450, 20, 0x888888, 1.0)
    engine.uiText("Enter spawn", 904, 474, 20, 0x888888, 1.0)
end

return DebugSpawnMenu
