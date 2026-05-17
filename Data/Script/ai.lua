-- Data/Script/ai.lua

-- Java 메인 루프에서 매 프레임 호출해 주는 전역 훅 함수
function onEntityUpdate(entity, dt, player)
    
    -- 1. 공통 애니메이션 타이머 처리 (Java 필드 직접 수정)
    entity.animTimer = entity.animTimer + dt
    if entity.animTimer >= entity.frameDuration then
        entity.currentFrame = (entity.currentFrame + 1) % 4 -- 4프레임 루프
        entity.animTimer = 0
    end

    -- 2. 스크립트 타입별 개별 행동 제어
    if entity.scriptName == "guard" then
        handleGuardAI(entity, dt, player)
    elseif entity.scriptName == "slime" then
        handleSlimeAI(entity, dt, player)
    end
end

-- 경비병 AI: 플레이어가 사거리 안에 들어오면 무섭게 쫓아옴
function handleGuardAI(entity, dt, player)
    -- Java의 distanceSq 메서드 다이렉트 호출 가능!
    local distSq = entity:distanceSq(player.x, player.y)
    
    if distSq < 64.0 and distSq > 1.0 then -- 너무 가깝지도 멀지도 않을 때 추적
        local dx = player.x - entity.x
        local dy = player.y - entity.y
        local distance = math.sqrt(distSq)
        
        -- 속도 벡터 지정 및 강제 좌표 이동 (Java 변수 실시간 갱신)
        local speed = 1.8
        entity.x = entity.x + (dx / distance) * speed * dt
        entity.y = entity.y + (dy / distance) * speed * dt
        
        -- 플레이어 바라보도록 회전값 계산
        entity.rotation = math.atan2(dy, dx)
    else
        entity:stop() -- Java 메서드 호출
    end
end

function handleSlimeAI(entity, dt, player)
    -- 제자리에서 통통 튀는 무해한 슬라임 로직 등 구현...
end