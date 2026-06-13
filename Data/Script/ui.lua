-- In-game HUD module.
-- HP, 무기 슬롯, 1인칭 무기 이미지, 미니맵, 크로스헤어, 피격 플래시를 즉시 모드 UI 명령으로 그린다.

local Ui = {}

-- 현재 Java Config와 UI 에셋은 1280x720 기준으로 맞춰져 있다.
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

-- UI 애니메이션 계산값을 안전한 범위로 제한한다.
local function clamp(value, minValue, maxValue)
    return math.max(minValue, math.min(maxValue, value))
end

-- 플레이어 HP 비율을 하단 중앙 바 폭으로 변환해 표시한다.
local function drawHp()
    local state = _G.PlayerState
    local ratio = math.max(0.0, math.min(1.0, state.hp / state.maxHp))

    -- 배경 바와 실제 HP 채움 바를 분리해 현재 체력 감소를 폭으로 표현한다.
    engine.uiRect(440, 652, 400, 28, 0x111111, 0.85)
    engine.uiRect(448, 660, math.floor(384 * ratio), 12, 0xF25F5C, 1.0)
end

-- 현재 장착 무기와 탄약 수를 우하단 슬롯에 표시한다.
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
        -- 흰 글자 아래 검은 글자를 살짝 오프셋해 간단한 그림자 효과를 만든다.
        engine.uiTextCenter(tostring(state.ammo), slotX + math.floor(slotSize / 2) + 1, slotY + slotSize - 14 + 1, 24, 0x000000, 0.75)
        engine.uiTextCenter(tostring(state.ammo), slotX + math.floor(slotSize / 2), slotY + slotSize - 14, 24, 0xFFFFFF, 1.0)
    end
end

-- 현재 무기를 1인칭 화면 하단에 표시하고 공격 애니메이션을 반영한다.
local function drawFpsWeapon()
    local state = _G.PlayerState or {}
    local weapon = state.weapon

    if weapon == "Gun" then
        local baseW = 405
        local baseH = 270
        -- 발사 프레임 동안 이미지를 크게 그려 반동처럼 보이게 한다.
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
        -- 근접 무기는 연속 회전보다 계단식 진행값을 써서 프레임 애니메이션처럼 보이게 한다.
        local steppedProgress = timer > 0 and (1.0 - (math.floor(clamp(progress, 0.0, 1.0) * 5.0) / 5.0)) or 0.0
        local angle = timer > 0 and (-35.0 + 90.0 * steppedProgress) or 0.0
        local x = math.floor(644 + 28 * steppedProgress)
        local y = math.floor(246 - 34 * math.sin(steppedProgress * math.pi))

        engine.uiImageRotated(CLUB_TEXTURE, x, y, baseW, baseH, angle, 1.0)
    end
end

-- maze.lua가 공격 입력을 처리할 때 호출해 무기별 UI 애니메이션 상태를 시작한다.
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

-- 무기 애니메이션 타이머를 갱신한다.
function Ui.tick(dt)
    local state = _G.PlayerState
    if state == nil then return end

    if (state.weaponFireFrames or 0) > 0 then
        -- 총 발사 효과는 프레임 수 기반으로 빠르게 줄인다.
        state.weaponFireFrames = math.max(0, state.weaponFireFrames - 1)
    end

    if (state.weaponSwingTimer or 0) > 0 then
        state.weaponSwingTimer = math.max(0.0, state.weaponSwingTimer - dt)
    end
end

-- 플레이어 주변 고정 반경의 로컬 미니맵을 그린다.
local function drawLocalMap(player)
    local maze = _G.GameState.maze
    if maze == nil or maze.map == nil or player == nil then return end

    local radius = 6
    local tileSize = 16
    -- Java 월드 좌표는 0-based에 가깝고 Lua 미로 배열은 1-based이므로 +1 한다.
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

                -- 맵 배열 좌표를 미니맵 화면 좌표로 변환해 사각형 타일을 찍는다.
                engine.uiRect(sx, sy, tileSize - 1, tileSize - 1, color, 0.9)
            end
        end
    end

    engine.uiRect(originX + radius * tileSize, originY + radius * tileSize, tileSize - 1, tileSize - 1, 0x80FF72, 1.0)
end

-- 전체 미로를 플레이어 중심으로 확대/축소해 표시한다.
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
        -- 전체 지도는 플레이어 위치를 중앙에 두고 다른 타일을 상대 좌표로 배치한다.
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

            -- preview 영역 밖 타일은 그리지 않아 큰 zoom에서도 화면을 넘치지 않게 한다.
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

-- 플레이어 UiComponent를 이용해 크로스헤어 텍스처와 표시 여부를 갱신한다.
local function updateCrosshair(player, showFullMap)
    if player == nil then return end

    if player.ui == nil then
        -- attachUi는 Java player 엔티티에 UiComponent를 생성한다.
        engine.attachUi(CROSSHAIR_TEXTURE, true)
    end

    if player.ui == nil then return end

    player.ui.x = CROSSHAIR_X
    player.ui.y = CROSSHAIR_Y
    player.ui.visible = not showFullMap

    if showFullMap then return end

    local state = _G.PlayerState or {}
    local hitTimer = state.crosshairHitTimer or 0
    -- 공격 명중 직후에는 원형 히트 표시 텍스처를 잠깐 사용한다.
    local texture = hitTimer > 0 and HIT_CROSSHAIR_TEXTURE or CROSSHAIR_TEXTURE

    if player.ui.currentTextureId ~= texture then
        engine.updateUiTexture(texture)
    end
end

-- 피격 직후 전체 화면에 붉은 반투명 플래시를 그린다.
local function drawDamageFlash()
    local state = _G.PlayerState or {}
    local timer = state.damageFlashTimer or 0

    if timer <= 0 then return end

    local duration = state.damageFlashDuration or 0.22
    -- 남은 시간 비율로 alpha를 줄여 자연스럽게 사라지게 한다.
    local ratio = math.max(0.0, math.min(1.0, timer / duration))

    engine.uiRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0xFF3030, 0.28 * ratio)
end

-- maze.lua에서 매 프레임 호출하는 HUD 최상위 draw 함수다.
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
