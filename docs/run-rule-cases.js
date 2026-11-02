// Runs docs/rule-cases.json against hero-utils.js in Node (used by CI; tests.html does the same in a browser).
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const read = f => fs.readFileSync(path.join(__dirname, f), 'utf8');
const sandbox = {};
vm.createContext(sandbox);
vm.runInContext(read('hero-utils.js'), sandbox);
const cases = JSON.parse(read('rule-cases.json'));

let failed = 0;
const check = (name, ok) => { if (!ok) { failed++; console.error('FAIL  ' + name); } };
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);

cases.absorbDamage.forEach(c => {
  const s = c.start, e = c.expect;
  const hero = { currentHp: s.hp, boostHp: s.boostHp, currentArmor: s.armor, currentWard: s.ward, boostArmor: s.boostArmor, boostWard: s.boostWard, inventory: [] };
  sandbox.absorbDamage(hero, c.amount, c.type);
  check(c.name, same([hero.currentHp, hero.boostHp, hero.currentArmor, hero.currentWard, hero.boostArmor, hero.boostWard],
    [e.hp, e.boostHp, e.armor, e.ward, e.boostArmor, e.boostWard]));
});

cases.tickRound.forEach(c => {
  const hero = Object.assign({ currentHp: 0, currentMp: 0, endurance: 0, intelligence: 0, spirit: 0, inventory: [], effects: [] }, JSON.parse(JSON.stringify(c.hero)));
  const r = sandbox.tickRound(hero, hero.endurance * 10, hero.intelligence, hero.spirit, c.diceRolls || {});
  const e = c.expect;
  check(c.name, hero.currentHp === e.hp && (e.mp === undefined || hero.currentMp === e.mp) &&
    (e.boostHp === undefined || hero.boostHp === e.boostHp) &&
    same(hero.effects.map(x => [x.id, x.roundsRemaining || 0]), e.effects.map(x => [x.id, x.roundsRemaining])) &&
    same(r.expired, e.expired));
});


const evil = `x'); alert(1);//" onmouseover="y`;
check('jsArg leaves no raw quote that could end the string or the attribute', !/["']/.test(sandbox.jsArg(evil)));
check('jsArg(null) is an empty string', sandbox.jsArg(null) === '&quot;&quot;');

const total = cases.absorbDamage.length + cases.tickRound.length + 2;
console.log(`${total - failed}/${total} rule cases passed`);
process.exit(failed ? 1 : 0);
