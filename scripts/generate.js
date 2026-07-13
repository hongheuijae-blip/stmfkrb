const PROMPT = `너는 스팀펑크 세계관의 게임 디자이너이자 데이터 엔지니어야.

[세계관 설정]
이 세계는 산업 혁명기의 증기 기관 문명과 고대 신앙이 공존한다.
사람들은 톱니바퀴와 증기 속에서도 옛 경전(성경 시편)의 구절을 새기고 암송하며 위안을 얻는다.
낡은 로봇 부품, 무기의 손잡이, 도시의 성당 첨탑, 몬스터의 전승 설화에는 드물게
개역한글판 성경 시편의 구절이 빛바랜 각인이나 인용구로 새겨져 있다.

[작업]
1) 몬스터 10종 생성
2) 무기 10종 생성
3) 로봇 파츠 5종 생성
4) 도시 2개 생성
5) 퀘스트 5개 생성
6) 코어 밸런스 설정 3개 생성

[시편 인용 규칙 - 중요]
- monsters, weapons, robotParts, cities, quests 각 배열마다, 전체 항목 수의 약 20%에만
  "scripture"(인용 구절 원문)와 "scriptureRef"(예: "시편 23:4") 필드를 채워줘.
  예) 몬스터 10종이면 그중 약 2종만, 무기 10종이면 그중 약 2종만.
  나머지 80% 항목은 scripture/scriptureRef를 빈 문자열("")로 둬.
- 인용은 실제 존재하는 시편 구절이어야 하고, 정확한 장:절을 표기해줘. 지어내지 마.
- 인용구는 이야기 맥락에 자연스럽게 어울리는 것으로 골라줘.
  예) 폐허가 된 몬스터의 전승 → "시편 90:5-6" (풀처럼 스러짐)
      수호 무기 → "시편 91:4" (그의 깃으로 너를 덮으시리니)
      증기 성당 도시 → "시편 84:1" (주의 장막이 어찌 그리 사랑스러운지)
- 인용을 넣는 항목은 "lore"나 "description" 본문에도 그 구절이 왜 새겨져 있는지
  한두 문장으로 자연스럽게 녹여줘 (단순 나열 금지).
- 나머지 80% 항목은 시편 인용 없이 순수한 스팀펑크 설정으로만 채워줘 (과하게 종교적으로 만들지 말 것).

[출력 형식]
{
  "monsters": [{"name":"", "level":1, "hp":1, "attack":1, "defense":1, "element":"", "lore":"", "scripture":"", "scriptureRef":""}],
  "weapons": [{"name":"", "type":"", "attack":1, "rarity":"", "description":"", "scripture":"", "scriptureRef":""}],
  "robotParts": [{"name":"", "slot":"", "bonusAttack":1, "bonusDefense":1, "specialEffect":"", "scripture":"", "scriptureRef":""}],
  "cities": [{"name":"", "region":"", "population":1, "description":"", "scripture":"", "scriptureRef":""}],
  "quests": [{"title":"", "description":"", "difficulty":1, "rewardExp":1, "scripture":"", "scriptureRef":""}],
  "coreConfigs": [{"key":"", "value":{}, "description":""}]
}

JSON 이외의 텍스트나 마크다운 코드블록 표시는 절대 출력하지 마.`;