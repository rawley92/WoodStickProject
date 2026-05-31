local Ui = {}

local SCREEN_WIDTH = 1280
local SCREEN_HEIGHT = 720
local CROSSHAIR_TEXTURE = "Textures.UI.CrossHair"
local HIT_CROSSHAIR_TEXTURE = "Textures.UI.Circle"
local CROSSHAIR_X = 624
local CROSSHAIR_Y = 344
local GUN_TEXTURE = "Textures.UI.Weapon.Gun.gun_fps"
local CLUB_TEXTURE = "Textures.UI.Weapon.club.club"
local ITEM_SLOT_TEXTURE = "Textures.UI.Item"
local ITEM_GUN_TEXTURE = "Textures.UI.Item_Gun"
local ITEM_CLUB_TEXTURE = "Textures.UI.Item_Crub"
local GUN_FIRE_FRAMES = 12
local CLUB_SWING_TIME = 0.5

local function clamp(value, minValue, maxValue)
    return math.max(minValue, math.min(maxValue, value))
end

local function drawHp()
    local state = _G.PlayerState
    local ratio = math.max(0.0, math.min(1.0, state.hp / state.maxHp))

    engine.uiRect(440, 652, 400, 28, 0x111111, 0.85)
    engine.uiRect(448, 660, math.floor(384 * ratio), 12, 0xF25F5C, 1.0)
end

local function drawWeapon()
    local state = _G.PlayerState
    local weapon = state.weapon or "None"
    local slotSize = 160
    local slotX = SCREEN_WIDTH - slotSize - 20
    local slotY = SCREEN_HEIGHT - slotSize - 20
    local iconTexture = nil

    if weapon == "Gun" then
        iconTexture = ITEM_GUN_TEXTURE
    elseif weapon == "Melee" then
        iconTexture = ITEM_CLUB_TEXTURE
    end

    engine.uiImage(ITEM_SLOT_TEXTURE, slotX, slotY, slotSize, slotSize, 0.95)

    if iconTexture ~= nil then
        engine.uiImage(iconTexture, slotX, slotY, slotSize, slotSize, 1.0)
    end

    if weapon == "Gun" then
        engine.uiTextCenter(tostring(state.ammo), slotX + math.floor(slotSize / 2) + 1, slotY + slotSize - 14 + 1, 24, 0x000000, 0.75)
        engine.uiTextCenter(tostring(state.ammo), slotX + math.floor(slotSize / 2), slotY + slotSize - 14, 24, 0xFFFFFF, 1.0)
    end
end

local function drawFpsWeapon()
    local state = _G.PlayerState or {}
    local weapon = state.weapon

    if weapon == "Gun" then
        local baseW = 405
        local baseH = 270
        local scale = (state.weaponFireFrames or 0) > 0 and 1.2 or 1.0
        local width = math.floor(baseW * scale)
        local height = math.floor(baseH * scale)
        local x = math.floor((SCREEN_WIDTH - width) / 2)
        local y = math.floor(652 - height - 4)

        engine.uiImage(GUN_TEXTURE, x, y, width, height, 1.0)
    elseif weapon == "Melee" then
        local baseW = 720
        local baseH = 528
        local timer = state.weaponSwingTimer or 0
        local duration = state.weaponSwingDuration or CLUB_SWING_TIME
        local progress = timer > 0 and (1.0 - clamp(timer / duration, 0.0, 1.0)) or 0.0
        local steppedProgress = timer > 0 and (1.0 - (math.floor(clamp(progress, 0.0, 1.0) * 5.0) / 5.0)) or 0.0
        local angle = timer > 0 and (-35.0 + 90.0 * steppedProgress) or 0.0
        local x = math.floor(644 + 28 * steppedProgress)
        local y = math.floor(246 - 34 * math.sin(steppedProgress * math.pi))

        engine.uiImageRotated(CLUB_TEXTURE, x, y, baseW, baseH, angle, 1.0)
    end
end

function Ui.startWeaponAnimation(weapon)
    local state = _G.PlayerState
    if state == nil then return end

    if weapon == "Gun" then
        state.weaponFireFrames = GUN_FIRE_FRAMES
    elseif weapon == "Melee" then
        state.weaponSwingTimer = CLUB_SWING_TIME
        state.weaponSwingDuration = CLUB_SWING_TIME
    end
end

function Ui.tick(dt)
    local state = _G.PlayerState
    if state == nil then return end

    if (state.weaponFireFrames or 0) > 0 then
        state.weaponFireFrames = math.max(0, state.weaponFireFrames - 1)
    end

    if (state.weaponSwingTimer or 0) > 0 then
        state.weaponSwingTimer = math.max(0.0, state.weaponSwingTimer - dt)
    end
end

local function drawLocalMap(player)
    local maze = _G.GameState.maze
    if maze == nil or maze.map == nil or player == nil then return end

    local radius = 6
    local tileSize = 16
    local px = math.floor(player.physics.x) + 1
    local py = math.floor(player.physics.y) + 1
    local originX = 32
    local originY = 32

    engine.uiRect(originX - 4, originY - 4, radius * 2 * tileSize + 16, radius * 2 * tileSize + 16, 0x000000, 0.65)

    for y = py - radius, py + radius do
        for x = px - radius, px + radius do
            if maze.map[y] ~= nil and maze.map[y][x] ~= nil then
                local tile = maze.map[y][x]
                local color = tile == maze.WALL and 0xD8E6FF or 0x1B263B
                local sx = originX + (x - (px - radius)) * tileSize
                local sy = originY + (y - (py - radius)) * tileSize

                engine.uiRect(sx, sy, tileSize - 1, tileSize - 1, color, 0.9)
            end
        end
    end

    engine.uiRect(originX + radius * tileSize, originY + radius * tileSize, tileSize - 1, tileSize - 1, 0x80FF72, 1.0)
end

local function drawFullMap(player, zoom)
    local maze = _G.GameState.maze
    if maze == nil or maze.map == nil then return end

    zoom = zoom or 1.0
    local previewWidth = math.floor(520 * 1.35)
    local previewHeight = math.floor(520 * 1.35)
    local tileSize = math.floor(math.min(previewWidth / maze.width, previewHeight / maze.height))
    tileSize = math.floor(tileSize * zoom)
    tileSize = math.max(tileSize, 2)

    local originX = math.floor((1280 - previewWidth) / 2)
    local originY = math.floor((720 - previewHeight) / 2)
    local centerX = originX + math.floor(previewWidth / 2)
    local centerY = originY + math.floor(previewHeight / 2)
    local px = math.floor(maze.width / 2) + 1
    local py = math.floor(maze.height / 2) + 1

    if player ~= nil then
        px = math.floor(player.physics.x) + 1
        py = math.floor(player.physics.y) + 1
    end

    engine.uiRect(originX - 12, originY - 12, previewWidth + 24, previewHeight + 24, 0x000000, 0.78)

    for y = 1, maze.height do
        for x = 1, maze.width do
            local tile = maze.map[y][x]
            local color = tile == maze.WALL and 0xD8E6FF or 0x1B263B
            local sx = centerX + (x - px) * tileSize
            local sy = centerY + (y - py) * tileSize

            if sx >= originX and sy >= originY and sx < originX + previewWidth and sy < originY + previewHeight then
                engine.uiRect(sx, sy, tileSize - 1, tileSize - 1, color, 0.95)
            end
        end
    end

    local exit = _G.GameState.exit
    if exit ~= nil then
        local sx = centerX + (exit.x - px) * tileSize
        local sy = centerY + (exit.y - py) * tileSize

        if sx >= originX and sy >= originY and sx < originX + previewWidth and sy < originY + previewHeight then
            engine.uiRect(sx, sy, tileSize - 1, tileSize - 1, 0x80FF72, 1.0)
        end
    end

    if player ~= nil then
        engine.uiRect(centerX, centerY, tileSize - 1, tileSize - 1, 0xFF4D6D, 1.0)
    end
end

local function updateCrosshair(player, showFullMap)
    if player == nil then return end

    if player.ui == nil then
        engine.attachUi(CROSSHAIR_TEXTURE, true)
    end

    if player.ui == nil then return end

    player.ui.x = CROSSHAIR_X
    player.ui.y = CROSSHAIR_Y
    player.ui.visible = not showFullMap

    if showFullMap then return end

    local state = _G.PlayerState or {}
    local hitTimer = state.crosshairHitTimer or 0
    local texture = hitTimer > 0 and HIT_CROSSHAIR_TEXTURE or CROSSHAIR_TEXTURE

    if player.ui.currentTextureId ~= texture then
        engine.updateUiTexture(texture)
    end
end

local function drawDamageFlash()
    local state = _G.PlayerState or {}
    local timer = state.damageFlashTimer or 0

    if timer <= 0 then return end

    local duration = state.damageFlashDuration or 0.22
    local ratio = math.max(0.0, math.min(1.0, timer / duration))

    engine.uiRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0xFF3030, 0.28 * ratio)
end

function Ui.draw(player, showFullMap, escapePrompt, noticeText, mapZoom)
    engine.uiClear()
    updateCrosshair(player, showFullMap)

    if not showFullMap then
        drawFpsWeapon()
    end

    drawHp()
    drawWeapon()

    if showFullMap then
        drawFullMap(player, mapZoom)
    else
        drawLocalMap(player)
    end

    if escapePrompt then
        engine.uiTextCenter("Enter to Escape", 640, 344, 48, 0xFFFFFF, 1.0)
    end

    if noticeText ~= nil then
        engine.uiTextCenter(noticeText, 640, 420, 40, 0xFFFFFF, 1.0)
    end

    drawDamageFlash()
end

return Ui
