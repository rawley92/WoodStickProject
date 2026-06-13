-- Legacy/test NPC script.
-- 플레이어 방향에 따라 스프라이트 방향을 바꾸고, 가까워질 때 테스트 효과음을 재생한다.

local npcStates = {}

-- 테스트 NPC 엔티티가 매 프레임 호출하는 update다.
function update(entity, dt, player)

    local id = entity.entityId

    if not npcStates[id] then
        -- baseAsset을 보관해 방향별 assetId prefix를 매 프레임 재구성한다.
        npcStates[id] = {
            baseAsset = entity.assetId
        }
    end

    local state = npcStates[id]

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y

    local angle = math.atan2(dy, dx)

    local dist = engine.distance(entity, player)

    -- 플레이어를 향한 각도를 4방향 인덱스로 양자화한다.
    local dirIndex = math.floor(((angle + math.pi) / (2 * math.pi)) * 4) % 4

    local direction
    if dirIndex == 0 then direction = "right"
    elseif dirIndex == 1 then direction = "front"
    elseif dirIndex == 2 then direction = "left"
    else direction = "back"
    end

    local prefix = string.gsub(state.baseAsset, "%.Base$", "")

    -- "Char.SomeNPC.Base" 형식을 "Char.SomeNPC.state_front_1" 같은 방향별 ID로 바꾼다.
    entity.assetId = prefix .. ".state_" .. direction .. "_1"

    if entity.sound ~= nil then
        if dist <= 3 then
            -- 거리 안에 처음 들어온 순간에만 효과음을 요청한다.
            if not state.inRange then
                entity.sound:play("Audio.SfxTest", false)
            end
            state.inRange = true
        else
            state.inRange = false
        end
    end
end
