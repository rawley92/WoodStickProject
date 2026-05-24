local npcStates = {}

function update(entity, dt, player)

    local id = entity.entityId

    if not npcStates[id] then
        npcStates[id] = {
            baseAsset = entity.assetId
        }
    end

    local state = npcStates[id]

    -- 1. 상대 위치 계산 (핵심)
    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y

    -- 2. 절대 각도
    local angle = math.atan2(dy, dx)

    local dist = engine.distance(entity, player)

    -- 3. 4방향 분류 (중요: 이게 핵심 로직)
    local dirIndex = math.floor(((angle + math.pi) / (2 * math.pi)) * 4) % 4

    local direction
    if dirIndex == 0 then direction = "right"
    elseif dirIndex == 1 then direction = "front"
    elseif dirIndex == 2 then direction = "left"
    else direction = "back"
    end

    -- 4. 스프라이트 prefix
    local prefix = string.gsub(state.baseAsset, "%.Base$", "")

    -- 5. 결과 적용
    entity.assetId = prefix .. ".walk_" .. direction .. "_1"

    if entity.sound ~= nil then
        if dist <= 3 then
            if not state.inRange then
                entity.sound:play("Audio.SfxTest", false)
            end
            state.inRange = true
        else
            state.inRange = false
        end
    end
end