-- HybridMazeGenerator.lua

local HybridMazeGenerator = {}
HybridMazeGenerator.__index = HybridMazeGenerator

-- 상수 정의
HybridMazeGenerator.PATH = 0
HybridMazeGenerator.WALL = 1
HybridMazeGenerator.START = 2
HybridMazeGenerator.EXIT = 3
HybridMazeGenerator.ENEMY_SPIDER = 4
HybridMazeGenerator.ENEMY_BULL = 5
HybridMazeGenerator.ENEMY_GHOST = 6
HybridMazeGenerator.ITEM_HEALTH = 7
HybridMazeGenerator.ITEM_AMMO = 8
HybridMazeGenerator.WEAPON_MELEE = 9
HybridMazeGenerator.WEAPON_GUN = 10
HybridMazeGenerator.KEY = 11
HybridMazeGenerator.LOCKED_DOOR = 12

-- 상하좌우 2칸 점프 배열
HybridMazeGenerator.DIRS = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}

-- 생성자: 다른 스크립트에서 크기와 시드를 전달받습니다.
function HybridMazeGenerator.new(width, height, seed)
    local self = setmetatable({}, HybridMazeGenerator)
    self.width = (width % 2 == 0) and (width + 1) or width
    self.height = (height % 2 == 0) and (height + 1) or height
    self.map = {}
    
    if seed then
        math.randomseed(seed)
    end
    
    return self
end

-- 메인 생성 로직
function HybridMazeGenerator:generate()
    self:generateMap()
    self:generateObjects()
    return self.map, self.objects
end

function HybridMazeGenerator:generateMap()
    self:fillWithWalls()
    self:generateRooms(4)
    self:generateMaze()
    self:connectRoomsAndMakeLoops()
    return self.map
end

function HybridMazeGenerator:generateObjects()
    self.objects = {}
    self.objectOccupied = {}
    self.start = { x = 2, y = 2 }

    self:addObject(self.START, self.start.x, self.start.y)
    self:spawnExit()

    for i = 1, 3 do self:spawnAtRandomPath(self.ENEMY_SPIDER) end
    for i = 1, 2 do self:spawnAtRandomPath(self.ENEMY_BULL) end
    self:spawnAtRandomPath(self.ENEMY_GHOST)

    for i = 1, 4 do self:spawnAtRandomPath(self.ITEM_HEALTH) end
    for i = 1, 5 do self:spawnAtRandomPath(self.ITEM_AMMO) end
    self:spawnAtRandomPath(self.WEAPON_MELEE)
    self:spawnAtRandomPath(self.WEAPON_GUN)

    self:spawnAtRandomPath(self.KEY)
    self:spawnAtRandomPath(self.LOCKED_DOOR)

    return self.objects
end

-- 지정된 폴더 경로에 map.dat 파일로 저장하는 함수
function HybridMazeGenerator:saveToFile(folderPath)
    -- 경로 끝에 슬래시(/)가 없으면 추가해줍니다.
    if folderPath:sub(-1) ~= "/" and folderPath:sub(-1) ~= "\\" then
        folderPath = folderPath .. "/"
    end
    
    local filePath = folderPath .. "map.dat"
    local file, err = io.open(filePath, "w")
    
    if not file then
        print("파일을 열 수 없습니다. 경로를 확인해주세요: " .. tostring(err))
        return false
    end
    
    -- 맵 데이터를 공백으로 구분하여 텍스트 형식으로 저장 (원한다면 쉼표(,) 등으로 변경 가능)
    for y = 1, self.height do
        local rowStr = ""
        for x = 1, self.width do
            rowStr = rowStr .. string.format("%d ", self.map[y][x])
        end
        file:write(rowStr .. "\n")
    end
    
    file:close()
    return true
end

-- 내부 생성 헬퍼 함수들 (기존과 동일)
function HybridMazeGenerator:fillWithWalls()
    for y = 1, self.height do
        self.map[y] = {}
        for x = 1, self.width do
            self.map[y][x] = self.WALL
        end
    end
end

function HybridMazeGenerator:generateRooms(numRooms)
    for i = 1, numRooms do
        local roomWidth = (math.random(1, 3) - 1) * 2 + 5
        local roomHeight = (math.random(1, 3) - 1) * 2 + 5
        
        local startX = (math.random(1, math.floor((self.width - roomWidth) / 2)) - 1) * 2 + 2
        local startY = (math.random(1, math.floor((self.height - roomHeight) / 2)) - 1) * 2 + 2

        for y = startY, startY + roomHeight - 1 do
            for x = startX, startX + roomWidth - 1 do
                self.map[y][x] = self.PATH
            end
        end
    end
end

function HybridMazeGenerator:generateMaze()
    for y = 2, self.height - 1, 2 do
        for x = 2, self.width - 1, 2 do
            if self.map[y][x] == self.WALL then
                self:runDFS(x, y)
            end
        end
    end
end

function HybridMazeGenerator:runDFS(startX, startY)
    local stack = {}
    table.insert(stack, {startX, startY})
    self.map[startY][startX] = self.PATH

    while #stack > 0 do
        local current = stack[#stack]
        local cx, cy = current[1], current[2]

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

            if nx > 1 and nx < self.width and ny > 1 and ny < self.height and self.map[ny][nx] == self.WALL then
                self.map[cy + dir[2] / 2][cx + dir[1] / 2] = self.PATH
                self.map[ny][nx] = self.PATH
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

function HybridMazeGenerator:connectRoomsAndMakeLoops()
    for y = 2, self.height - 1 do
        for x = 2, self.width - 1 do
            if self.map[y][x] == self.WALL then
                local verticalPath = (self.map[y - 1][x] == self.PATH and self.map[y + 1][x] == self.PATH)
                local horizontalPath = (self.map[y][x - 1] == self.PATH and self.map[y][x + 1] == self.PATH)

                if verticalPath ~= horizontalPath and math.random(1, 100) <= 20 then
                    self.map[y][x] = self.PATH
                end
            end
        end
    end
end

function HybridMazeGenerator:spawnObjects()
    return self:generateObjects()
end

function HybridMazeGenerator:getDistance(x1, y1, x2, y2)
    return math.abs(x1 - x2) + math.abs(y1 - y2)
end

function HybridMazeGenerator:spawnExit()
    local x, y, attempts = 0, 0, 0
    local minDistance = math.floor((self.width + self.height) / 2)

    while true do
        x = math.random(1, self.width)
        y = math.random(1, self.height)
        attempts = attempts + 1

        if self.map[y][x] == self.PATH and (self:getDistance(2, 2, x, y) >= minDistance or attempts > 1000) then
            break
        end
    end
    
    self:addObject(self.EXIT, x, y)
end

function HybridMazeGenerator:spawnAtRandomPath(objectType)
    local x, y, attempts = 0, 0, 0

    while true do
        x = math.random(1, self.width)
        y = math.random(1, self.height)
        attempts = attempts + 1

        if attempts > 1000 then
            return
        end

        if self.map[y][x] == self.PATH and not self.objectOccupied[y .. ":" .. x] then
            break
        end
    end
    
    self:addObject(objectType, x, y)
end

function HybridMazeGenerator:addObject(objectType, x, y)
    if not self.objects then self.objects = {} end
    if not self.objectOccupied then self.objectOccupied = {} end

    self.objectOccupied[y .. ":" .. x] = true

    table.insert(self.objects, {
        type = objectType,
        x = x,
        y = y
    })
end

-- 모듈 반환
return HybridMazeGenerator
