const Ajv = require("ajv");
const ajv = new Ajv();

const schema = {
  type: "object",
  properties: {
    monsters: { type: "array" },
    weapons: { type: "array" },
    robotParts: { type: "array" },
    cities: { type: "array" },
    quests: { type: "array" },
    coreConfigs: { type: "array" },
  },
  required: ["monsters", "weapons", "robotParts", "cities", "quests", "coreConfigs"],
};

const validate = ajv.compile(schema);

function validateGameDataJson(json) {
  const valid = validate(json);
  if (!valid) console.error("[검증 실패]", validate.errors);
  return valid;
}

module.exports = { validateGameDataJson };