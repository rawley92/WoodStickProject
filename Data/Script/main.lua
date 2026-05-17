-- 엔진 기동 시 자바 레이어가 DataLoader를 통해 Data.json을 읽고 이미 모든 메모리 로드를 마친 상태입니다.
-- 메인은 단순히 첫 레벨 스크립트로 제어권을 넘깁니다.
dofile("Data/Script/title.lua")