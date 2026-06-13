-- Shared menu UI renderer.
-- title/escape/end 씬이 공통으로 쓰는 배경, 타이틀, 버튼 그리기 함수를 제공한다.

local MenuUi = {}

-- 메뉴 레이아웃은 1280x720 기준 고정 좌표로 배치된다.
local SCREEN_WIDTH = 1280
local SCREEN_HEIGHT = 720
local BACK_TEXTURE = "Textures.UI.Background.back"
local BUTTON_TEXTURE = "Textures.UI.Background.Button"
local BUTTON_X = 424
local BUTTON_W = 432
local BUTTON_H = 102
local BUTTON_START_Y = 412
local BUTTON_GAP = 118

-- alpha와 스케일 계산이 범위를 벗어나지 않게 제한한다.
local function clamp(value, minValue, maxValue)
    if value < minValue then return minValue end
    if value > maxValue then return maxValue end
    return value
end

-- 동일 텍스트를 검은색으로 먼저 그려 간단한 그림자 효과를 만든다.
local function textCenterShadow(text, x, y, size, color, alpha)
    engine.uiTextCenter(text, x + 4, y + 5, size, 0x000000, alpha * 0.55)
    engine.uiTextCenter(text, x, y, size, color, alpha)
end

-- 메뉴 배경 이미지와 선택적 컬러 틴트 오버레이를 그린다.
function MenuUi.drawBackground(tintColor, tintAlpha, alpha)
    alpha = alpha or 1.0
    tintAlpha = tintAlpha or 0.0

    engine.uiRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0x000000, 1.0)
    engine.uiImage(BACK_TEXTURE, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, alpha)

    if tintAlpha > 0 then
        engine.uiRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, tintColor, tintAlpha * alpha)
    end
end

-- 메뉴 타이틀 텍스트와 하단 강조선을 그린다.
function MenuUi.drawTitle(text, y, size, color, alpha)
    alpha = alpha or 1.0

    textCenterShadow(text, 640, y, size, color, alpha)
    engine.uiRect(260, y + 64, 760, 5, color, clamp(0.72 * alpha, 0.0, 1.0))
end

-- index 기반으로 버튼 위치를 계산하고 선택 상태에 따라 크기/색/alpha를 조절한다.
function MenuUi.drawButton(label, index, selected, pulseTimer, color, alpha)
    alpha = alpha or 1.0

    local y = BUTTON_START_Y + (index - 1) * BUTTON_GAP
    local scale = selected and 1.0 or 0.92

    if selected and pulseTimer > 0 then
        -- Enter 직후 짧은 pulse 동안 선택 버튼을 살짝 키운다.
        scale = 1.04
    end

    local width = math.floor(BUTTON_W * scale)
    local height = math.floor(BUTTON_H * scale)
    local x = BUTTON_X + math.floor((BUTTON_W - width) / 2)
    local drawY = y + math.floor((BUTTON_H - height) / 2)
    local overlayAlpha = selected and 0.24 or 0.08
    local borderAlpha = selected and 1.0 or 0.58
    local textColor = selected and 0xFFFFFF or 0xA5A5A5
    local textSize = selected and 42 or 38

    engine.uiImage(BUTTON_TEXTURE, x, drawY, width, height, borderAlpha * alpha)
    engine.uiRect(x + 8, drawY + 8, width - 16, height - 16, color, overlayAlpha * alpha)
    textCenterShadow(label, x + math.floor(width / 2), drawY + math.floor(height / 2) + 15, textSize, textColor, alpha)
end

return MenuUi
