-- Animated hybrid maze generator.
-- 방을 일부 배치한 뒤 DFS 미로를 파고, 로딩 화면에서 단계별 진행률을 보여줄 수 있도록 animation 상태를 보관한다.

local MazeGenerator = {}
MazeGenerator.__index = MazeGenerator

MazeGenerator.PATH = 0
MazeGenerator.WALL = 1
-- DFS는 한 번에 두 칸 이동하고 중간 벽을 허물어 미로 통로를 만든다.
MazeGenerator.DIRS = {
    {-2, 0}, -- 상
    { 2, 0}, -- 하
    { 0,-2}, -- 좌
    { 0, 2}  -- 우
}

-- 지정 크기와 seed로 미로 생성기 인스턴스를 만든다.
function MazeGenerator.new(width, height, seed)
    local self = setmetatable({}, MazeGenerator)

    -- 2칸 단위 DFS가 정상 동작하도록 폭/높이는 항상 홀수로 보정한다.
    self.width = (width % 2 == 0) and (width + 1) or width
    self.height = (height % 2 == 0) and (height + 1) or height

    self.map = {}
    self.rooms = {}

    if seed ~= nil then math.randomseed(seed) end

    return self
end

-- 즉시 전체 미로를 생성하는 동기식 경로다.
function MazeGenerator:generate()
    self.rooms = {}
    self:fillWithWalls()
    self:generateRooms(3)              -- 1. 무작위 소형 방 3개 생성
    self:generateMazePaths()           -- 2. 남은 빈 공간을 DFS 미로로 채우기
    self:connectRoomsAndMakeLoops()    -- 3. 방과 미로를 연결하고 낮은 확률로 루프(지름길) 생성
    self:ensureRoomEntrances(2)        -- 4. 각 방은 최소 2개의 입출구를 보장

    -- 플레이어 시작 지점 고정 (안전장치 겸용)
    self.start = { x = 2, y = 2 }

    return self.map
end

-- 로딩 화면에서 한 프레임씩 진행할 수 있는 animation 상태를 초기화한다.
function MazeGenerator:startAnimatedGeneration()
    self.rooms = {}
    self:fillWithWalls()
    self:generateRooms(3)

    self.start = { x = 2, y = 2 }

    local total = 0
    for y = 2, self.height - 1, 2 do
        for x = 2, self.width - 1, 2 do
            if self.map[y][x] == self.WALL then
                -- 진행률 계산을 위해 DFS 시작 후보가 될 수 있는 셀 수를 센다.
                total = total + 1
            end
        end
    end

    self.animation = {
        phase = "scan",
        x = 2,
        y = 2,
        stack = {},
        carved = 0,
        total = math.max(1, total),
        activeX = nil,
        activeY = nil,
        finished = false
    }

    return self.map
end

-- scan 단계에서 아직 파지 않은 벽 셀을 찾아 새 DFS 스택을 시작한다.
function MazeGenerator:advanceAnimationScan()
    local anim = self.animation

    while anim.y <= self.height - 1 do
        while anim.x <= self.width - 1 do
            local x = anim.x
            local y = anim.y
            anim.x = anim.x + 2

            if self.map[y][x] == self.WALL then
                -- 새 DFS 섬의 첫 칸을 길로 만들고 dfs phase로 전환한다.
                self.map[y][x] = self.PATH
                anim.stack = { { x, y } }
                anim.carved = math.min(anim.total, anim.carved + 1)
                anim.activeX = x
                anim.activeY = y
                anim.phase = "dfs"
                return
            end
        end

        anim.x = 2
        anim.y = anim.y + 2
    end

    -- 더 이상 DFS 시작점이 없으면 방 연결과 루프 생성을 후처리로 수행한다.
    self:connectRoomsAndMakeLoops()
    self:ensureRoomEntrances(2)

    anim.carved = anim.total
    anim.activeX = nil
    anim.activeY = nil
    anim.phase = "done"
    anim.finished = true
end

-- animation 상태의 DFS를 한 단계 진행한다.
function MazeGenerator:stepAnimatedDfs()
    local anim = self.animation
    local stack = anim.stack

    if stack == nil or #stack == 0 then
        anim.phase = "scan"
        return
    end

    local current = stack[#stack]
    local cx, cy = current[1], current[2]
    local shuffledDirs = {}

    for i, dir in ipairs(self.DIRS) do
        shuffledDirs[i] = { dir[1], dir[2] }
    end

    self:shuffleList(shuffledDirs)

    for _, dir in ipairs(shuffledDirs) do
        local nx = cx + dir[1]
        local ny = cy + dir[2]

        if nx > 1 and nx < self.width and ny > 1 and ny < self.height and self.map[ny][nx] == self.WALL then
            -- 두 칸 앞 벽을 새 통로로 만들고, 현재 칸과 새 칸 사이의 중간 벽도 허문다.
            self.map[cy + dir[2] / 2][cx + dir[1] / 2] = self.PATH
            self.map[ny][nx] = self.PATH
            table.insert(stack, { nx, ny })

            anim.carved = math.min(anim.total, anim.carved + 1)
            anim.activeX = nx
            anim.activeY = ny
            return
        end
    end

    -- 갈 수 있는 방향이 없으면 DFS backtracking을 수행한다.
    table.remove(stack)

    local top = stack[#stack]
    if top ~= nil then
        anim.activeX = top[1]
        anim.activeY = top[2]
    end
end

-- 한 프레임에 여러 generation step을 진행한다.
function MazeGenerator:stepAnimatedGeneration(maxSteps)
    if self.animation == nil then
        self:startAnimatedGeneration()
    end

    local anim = self.animation
    if anim.finished then
        return true
    end

    maxSteps = maxSteps or 1

    for _ = 1, maxSteps do
        if anim.finished then
            return true
        end

        if anim.phase == "scan" then
            self:advanceAnimationScan()
        elseif anim.phase == "dfs" then
            self:stepAnimatedDfs()
        else
            anim.finished = true
        end
    end

    return anim.finished
end

-- UI 진행률에 사용할 0.0~1.0 값을 반환한다.
function MazeGenerator:getAnimationProgress()
    local anim = self.animation

    if anim == nil then return 0.0 end
    if anim.finished then return 1.0 end

    return math.min(0.99, anim.carved / anim.total)
end

-- animated generation 완료 여부를 반환한다.
function MazeGenerator:isAnimationFinished()
    return self.animation ~= nil and self.animation.finished
end

-- 로딩 미리보기에서 현재 DFS 위치를 강조하기 위한 좌표를 반환한다.
function MazeGenerator:getAnimationCursor()
    if self.animation == nil then return nil end
    if self.animation.activeX == nil or self.animation.activeY == nil then return nil end

    return {
        x = self.animation.activeX,
        y = self.animation.activeY
    }
end

-- 전체 맵 배열을 벽으로 초기화한다.
function MazeGenerator:fillWithWalls()
    for y = 1, self.height do
        self.map[y] = {}
        for x = 1, self.width do
            self.map[y][x] = self.WALL
        end
    end
end

-- Hybrid 방식의 넓은 방 생성 로직
-- DFS 미로만으로는 공간이 단조로워지므로 작은 방을 먼저 배치한다.
function MazeGenerator:generateRooms(numRooms)
    local created = 0
    local attempts = 0
    local maxAttempts = numRooms * 30

    while created < numRooms and attempts < maxAttempts do
        attempts = attempts + 1

        -- 3x3 또는 5x5 정도의 작은 방만 사용해 미로성을 유지합니다.
        local roomWidth = (math.random(1, 2) - 1) * 2 + 3
        local roomHeight = (math.random(1, 2) - 1) * 2 + 3
        
        -- 방 시작 좌표도 2칸 간격으로 맞춰 DFS 격자와 호환되게 한다.
        local startX = (math.random(1, math.floor((self.width - roomWidth) / 2)) - 1) * 2 + 2
        local startY = (math.random(1, math.floor((self.height - roomHeight) / 2)) - 1) * 2 + 2

        if self:canPlaceRoom(startX, startY, roomWidth, roomHeight) then
            local room = {
                x = startX,
                y = startY,
                width = roomWidth,
                height = roomHeight,
                doors = {}
            }

            for y = startY, startY + roomHeight - 1 do
                for x = startX, startX + roomWidth - 1 do
                    self.map[y][x] = self.PATH
                end
            end

            table.insert(self.rooms, room)
            created = created + 1
        end
    end
end

-- 기존 통로와 겹치거나 바로 붙는 방을 막는다.
function MazeGenerator:canPlaceRoom(startX, startY, roomWidth, roomHeight)
    local endX = startX + roomWidth - 1
    local endY = startY + roomHeight - 1

    for y = math.max(1, startY - 1), math.min(self.height, endY + 1) do
        for x = math.max(1, startX - 1), math.min(self.width, endX + 1) do
            if self.map[y][x] == self.PATH then
                return false
            end
        end
    end

    return true
end

-- 맵 전체를 돌며 막혀있는 곳(WALL)마다 DFS 미로 생성 (Hybrid의 특징)
-- 방이 차지하지 않은 모든 영역을 미로 통로로 연결한다.
function MazeGenerator:generateMazePaths()
    for y = 2, self.height - 1, 2 do
        for x = 2, self.width - 1, 2 do
            if self.map[y][x] == self.WALL then
                self:runDFS(x, y)
            end
        end
    end
end

-- 스택을 활용한 DFS (깊이 우선 탐색) 로직
-- 비재귀 스택을 사용해 큰 미로에서도 Lua call stack을 사용하지 않는다.
function MazeGenerator:runDFS(startX, startY)
    local stack = {}
    table.insert(stack, {startX, startY})
    self.map[startY][startX] = self.PATH

    while #stack > 0 do
        local current = stack[#stack]
        local cx, cy = current[1], current[2]

        -- 방향 셔플
        local shuffledDirs = {}
        for i, dir in ipairs(self.DIRS) do
            shuffledDirs[i] = {dir[1], dir[2]}
        end
        for i = #shuffledDirs, 2, -1 do
            local j = math.random(1, i)
            shuffledDirs[i], shuffledDirs[j] = shuffledDirs[j], shuffledDirs[i]
        end

        local moved = false
        for _, dir in ipairs(shuffledDirs) do
            local nx = cx + dir[1]
            local ny = cy + dir[2]

            -- 맵 범위를 벗어나지 않고 2칸 앞이 벽(WALL)인지 확인
            if nx > 1 and nx < self.width and ny > 1 and ny < self.height and self.map[ny][nx] == self.WALL then
                self.map[cy + dir[2] / 2][cx + dir[1] / 2] = self.PATH -- 중간 벽 허물기
                self.map[ny][nx] = self.PATH                           -- 도착지 길 만들기
                table.insert(stack, {nx, ny})
                moved = true
                break
            end
        end

        if not moved then
            table.remove(stack)
        end
    end
end

-- Hybrid 방식의 방/미로 연결 및 지름길 생성
-- 완전한 트리 미로에 일부 루프를 만들어 길찾이가 지나치게 단조롭지 않게 한다.
function MazeGenerator:connectRoomsAndMakeLoops()
    for y = 2, self.height - 1 do
        for x = 2, self.width - 1 do
            if self.map[y][x] == self.WALL then
                -- 위아래가 뚫려있는가?
                local verticalPath = (self.map[y - 1][x] == self.PATH and self.map[y + 1][x] == self.PATH)
                -- 좌우가 뚫려있는가?
                local horizontalPath = (self.map[y][x - 1] == self.PATH and self.map[y][x + 1] == self.PATH)

                -- 십자(+) 모양이 되는 것을 방지하면서, 낮은 확률로만 벽을 허물어 길을 연결함
                if verticalPath ~= horizontalPath and math.random(1, 100) <= 6 then
                    self.map[y][x] = self.PATH
                end
            end
        end
    end
end

-- Fisher-Yates 방식으로 배열을 제자리 셔플한다.
function MazeGenerator:shuffleList(list)
    for i = #list, 2, -1 do
        local j = math.random(1, i)
        list[i], list[j] = list[j], list[i]
    end
end

-- 방 출입구 좌표를 중복 없이 기록한다.
function MazeGenerator:rememberRoomEntrance(room, entrance)
    local key = entrance.y .. ":" .. entrance.x

    if room.doorLookup == nil then
        room.doorLookup = {}
    end

    if room.doorLookup[key] then
        return false
    end

    room.doorLookup[key] = true
    table.insert(room.doors, {
        x = entrance.x,
        y = entrance.y,
        side = entrance.side
    })

    return true
end

-- 후보 벽을 길로 바꾸고 방 출입구로 등록한다.
function MazeGenerator:openRoomEntrance(room, entrance)
    self.map[entrance.y][entrance.x] = self.PATH
    return self:rememberRoomEntrance(room, entrance)
end

-- 방 안쪽, 벽, 바깥쪽이 모두 유효할 때 출입구 후보를 추가한다.
function MazeGenerator:addRoomEntranceCandidate(candidates, side, insideX, insideY, wallX, wallY, outsideX, outsideY)
    if wallX <= 1 or wallX >= self.width or wallY <= 1 or wallY >= self.height then
        return
    end

    if outsideX <= 1 or outsideX >= self.width or outsideY <= 1 or outsideY >= self.height then
        return
    end

    if self.map[insideY][insideX] ~= self.PATH then
        return
    end

    if self.map[outsideY][outsideX] ~= self.PATH then
        return
    end

    table.insert(candidates, {
        x = wallX,
        y = wallY,
        side = side
    })
end

-- 방 네 변을 검사해 열 수 있는 출입구 후보를 수집한다.
function MazeGenerator:collectRoomEntranceCandidates(room)
    local candidates = {}
    local endX = room.x + room.width - 1
    local endY = room.y + room.height - 1

    for y = room.y, endY, 2 do
        self:addRoomEntranceCandidate(candidates, "left", room.x, y, room.x - 1, y, room.x - 2, y)
        self:addRoomEntranceCandidate(candidates, "right", endX, y, endX + 1, y, endX + 2, y)
    end

    for x = room.x, endX, 2 do
        self:addRoomEntranceCandidate(candidates, "top", x, room.y, x, room.y - 1, x, room.y - 2)
        self:addRoomEntranceCandidate(candidates, "bottom", x, endY, x, endY + 1, x, endY + 2)
    end

    return candidates
end

-- 각 방이 최소한의 출입구를 가지도록 후보 벽을 추가로 연다.
function MazeGenerator:ensureRoomEntrances(minEntrances)
    if self.rooms == nil then
        return
    end

    for _, room in ipairs(self.rooms) do
        room.doors = {}
        room.doorLookup = {}

        local candidates = self:collectRoomEntranceCandidates(room)
        local usedSides = {}

        for _, entrance in ipairs(candidates) do
            if self.map[entrance.y][entrance.x] == self.PATH then
                self:rememberRoomEntrance(room, entrance)
                usedSides[entrance.side] = true
            end
        end

        self:shuffleList(candidates)

        for _, entrance in ipairs(candidates) do
            if #room.doors >= minEntrances then
                break
            end

            if not usedSides[entrance.side] then
                if self:openRoomEntrance(room, entrance) then
                    usedSides[entrance.side] = true
                end
            end
        end

        for _, entrance in ipairs(candidates) do
            if #room.doors >= minEntrances then
                break
            end

            self:openRoomEntrance(room, entrance)
        end

        room.doorLookup = nil
    end
end

-- Java ScriptAPI.initScene이 읽을 수 있는 공백 구분 map.dat 파일로 저장한다.
function MazeGenerator:saveToFile(folderPath)
    if folderPath:sub(-1) ~= "/" and folderPath:sub(-1) ~= "\\" then 
        folderPath = folderPath .. "/" 
    end
    
    local filePath = folderPath .. "map.dat"
    local file, err = io.open(filePath, "w")
    
    if not file then 
        print("Failed To Save Map: " .. tostring(err)) 
        return false 
    end

    for y = 1, self.height do
        local row = ""
        for x = 1, self.width do 
            row = row .. tostring(self.map[y][x]) .. " " 
        end
        file:write(row .. "\n")
    end
    
    file:close()
    return true
end

return MazeGenerator
